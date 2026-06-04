package io.github.dreammooncai.pvz2tool.pop.image

import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.File
import java.nio.ByteOrder
import javax.imageio.ImageIO

actual class YFBitmap private actual constructor(
    actual val width: Int,
    actual val height: Int,
    backingStream: CoroutineBinaryStream?
): AutoCloseable {
    @PublishedApi
    internal actual val stream: CoroutineBinaryStream = backingStream ?: CoroutineBinaryStream.allocateBytes(width * height * 4)

    private suspend fun ensureStreamCapacity() {
        val required = width * height * 4L
        if (stream.length < required) {
            stream.setLength(required)
        }
    }

    // ==================== 平台互操作 ====================
    suspend fun loadFromBufferedImage(image: BufferedImage) {
        val w = width.coerceAtMost(image.width)
        val h = height.coerceAtMost(image.height)
        val totalPixels = w * h

        // 1. 确保流容量
        stream.setReadWritePosition(0)

        // 2. 获取 Int 视图
        val dstBuffer = stream.toBuffer().apply {
            limit(totalPixels * 4) // 锁定 limit 到整个位图大小
            order(ByteOrder.nativeOrder())
        }
        val dstIntBuffer = dstBuffer.asIntBuffer()

        // 3. 零拷贝或高性能拷贝
        if (image.type == BufferedImage.TYPE_INT_ARGB || image.type == BufferedImage.TYPE_INT_RGB) {
            // 最佳路径：直接从底层数组拷贝
            val dataBuffer = image.raster.dataBuffer as DataBufferInt
            val srcInts = dataBuffer.data
            // 注意：只有当 image 的 width 等于 YFBitmap 的 width 时才能批量 put
            if (image.width == width) {
                dstIntBuffer.put(srcInts, 0, totalPixels)
            } else {
                // 如果宽度不一致，需要逐行处理，防止错位
                for (i in 0 until h) {
                    dstIntBuffer.put(srcInts, i * image.width, w)
                }
            }
        } else {
            // 🔥 修复后的 Fallback 路径
            // 使用 getRGB 而不是 raster.getPixels
            val tempRow = IntArray(w)
            for (y in 0 until h) {
                // getRGB(startX, startY, w, h, rgbArray, offset, scansize)
                image.getRGB(0, y, w, 1, tempRow, 0, w)
                dstIntBuffer.put(tempRow)
            }
        }
    }

    fun toBufferedImage(): BufferedImage {
        // 强制创建 TYPE_INT_ARGB，保证后续强转安全
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

        stream.readPosition = 0
        val srcBuffer = stream.toBuffer().apply {
            limit(width * height * 4)
            order(ByteOrder.nativeOrder())
        }
        val srcIntBuffer = srcBuffer.asIntBuffer()

        // 🔥 零拷贝：直接写入 BufferedImage 底层数组
        val dataBuffer = image.raster.dataBuffer as DataBufferInt
        val dstInts = dataBuffer.data

        srcIntBuffer.get(dstInts, 0, width * height)

        return image
    }

    actual fun saveToPng(file: File) {
        ImageIO.write(toBufferedImage(), "PNG", file)
    }

    actual fun saveToBmp(file: File) {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        stream.readPosition = 0
        val srcBuffer = stream.toBuffer().apply { limit(width * height * 4) }
        val srcIntBuffer = srcBuffer.asIntBuffer()

        val dstDataBuffer = image.raster.dataBuffer as DataBufferInt
        val dstInts = dstDataBuffer.data

        // 这里依然需要循环处理 Alpha 通道，这是 BMP 格式限制，不是内存限制
        for (i in 0 until width * height) {
            dstInts[i] = srcIntBuffer.get() and 0x00FFFFFF
        }

        ImageIO.write(image, "BMP", file)
    }

    actual suspend fun loadFromFile(file: File) {
        ImageIO.read(file)?.let { loadFromBufferedImage(it) }
    }

    actual suspend fun slice(x: Int, y: Int, width: Int, height: Int): YFBitmap {
        val offset = pixelOffset(x, y)
        val length = width * height * 4L
        val slicedStream = stream.slice(offset, length)
        return invoke(width, height, slicedStream)
    }

    actual fun toCoroutineStream(): CoroutineBinaryStream = stream

    override fun close() {
        stream.close()
    }

    actual companion object {
        actual suspend fun fromFile(file: File): YFBitmap? {
            return ImageIO.read(file)?.let { img ->
                YFBitmap(img.width, img.height).apply {
                    ensureStreamCapacity()
                    loadFromBufferedImage(img)
                }
            }
        }

        actual suspend fun fromCoroutineStream(stream: CoroutineBinaryStream, width: Int, height: Int): YFBitmap {
            return YFBitmap(width, height, stream).apply {
                ensureStreamCapacity()
            }
        }

        actual suspend operator fun invoke(width: Int, height: Int, backingStream: CoroutineBinaryStream?): YFBitmap = YFBitmap(width, height, backingStream).apply {
            ensureStreamCapacity()
        }
    }
}