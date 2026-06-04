package io.github.dreammooncai.pvz2tool.pop.image

import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import androidx.core.graphics.createBitmap
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

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

    // ==================== 平台互操作 (零拷贝版) ====================

    suspend fun loadFromAndroidBitmap(bitmap: Bitmap) {
        val w = width.coerceAtMost(bitmap.width)
        val h = height.coerceAtMost(bitmap.height)
        val pixelCount = w * h

        stream.setReadWritePosition(0)

        // 1. 零拷贝：Bitmap原生像素直接写入流的底层Buffer
        val dstBuffer = stream.toBuffer().apply {
            limit(pixelCount * 4)
            order(ByteOrder.nativeOrder())
        }
        bitmap.copyPixelsToBuffer(dstBuffer)

        // 2. 倒带，原地修复通道顺序（无堆分配，仅操作堆外/共享内存）
        dstBuffer.rewind()
        val intView = dstBuffer.asIntBuffer()

        // 核心修复：Android输出RGBA → 转为桌面版期望的BGRA
        // 位运算直接修改Buffer内存，无任何数组拷贝
        for (i in 0 until pixelCount) {
            val rgba = intView[i]
            // RGBA (0xRRGGBBAA) → BGRA (0xBBGGRRAA)，直接交换R和B通道
            val bgra = (rgba and 0xFF00FF00.toInt()) or // 保留G、A通道
                    ((rgba and 0x00FF0000) ushr 16) or    // R → B
                    ((rgba and 0x000000FF) shl 16)         // B → R
            intView.put(i, bgra)
        }
    }

    fun toAndroidBitmap(): Bitmap {
        val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixelCount = width * height

        stream.readPosition = 0
        // 1. 复制Buffer视图，不影响原流的position
        val srcBuffer = stream.toBuffer().duplicate().apply {
            limit(pixelCount * 4)
            order(ByteOrder.nativeOrder())
        }
        val intView = srcBuffer.asIntBuffer()

        // 2. 原地转换回Android期望的RGBA格式
        for (i in 0 until pixelCount) {
            val bgra = intView[i]
            val rgba = (bgra and 0xFF00FF00.toInt()) or
                    ((bgra and 0x00FF0000) ushr 16) or
                    ((bgra and 0x000000FF) shl 16)
            intView.put(i, rgba)
        }

        // 3. 零拷贝：Buffer直接写入Bitmap原生内存
        srcBuffer.rewind()
        bitmap.copyPixelsFromBuffer(srcBuffer)

        return bitmap
    }


    actual fun saveToPng(file: File) {
        FileOutputStream(file).use {
            toAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    actual fun saveToBmp(file: File) {
        val rgbBitmap = createBitmap(width, height, Bitmap.Config.RGB_565)
        val srcBitmap = toAndroidBitmap()

        Canvas(rgbBitmap).apply {
            drawBitmap(srcBitmap, 0f, 0f, null)
        }

        FileOutputStream(file).use {
            rgbBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        rgbBitmap.recycle()
        srcBitmap.recycle()
    }

    actual suspend fun loadFromFile(file: File) {
        BitmapFactory.decodeFile(file.absolutePath)?.let {
            loadFromAndroidBitmap(it)
            it.recycle()
        }
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
            return BitmapFactory.decodeFile(file.absolutePath)?.let { bitmap ->
                val result = YFBitmap(bitmap.width, bitmap.height)
                result.ensureStreamCapacity()
                result.loadFromAndroidBitmap(bitmap)
                bitmap.recycle()
                result
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