package io.github.dreammooncai.pvz2tool.pop.image

import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream
import java.io.File

/**
 * 【极致性能 + 零 OOM】跨平台位图
 * 底层存储：基于 CoroutineBinaryStream (内存/文件自动切换)
 * 核心优势：支持 GB 级超大图，永不 OOM
 */
expect class YFBitmap private constructor(
    width: Int,
    height: Int,
    backingStream: CoroutineBinaryStream? = null
): AutoCloseable {
    val width: Int
    val height: Int

    // 内部流，暴露给通用扩展函数使用
    @PublishedApi
    internal val stream: CoroutineBinaryStream

    // ==================== 平台相关 API ====================
    fun saveToPng(file: File)
    fun saveToBmp(file: File)
    suspend fun loadFromFile(file: File)

    // ==================== 流操作 (高级 API) ====================
    suspend fun slice(x: Int, y: Int, width: Int, height: Int): YFBitmap
    fun toCoroutineStream(): CoroutineBinaryStream

    companion object {
        suspend fun fromFile(file: File): YFBitmap?
        suspend fun fromCoroutineStream(stream: CoroutineBinaryStream, width: Int, height: Int): YFBitmap

        suspend operator fun invoke(width: Int, height: Int, backingStream: CoroutineBinaryStream? = null): YFBitmap
    }
}

// ==================== 通用扩展实现 (全平台共用) ====================

// 计算像素在流中的偏移量 (每个像素 4 字节: ARGB)
internal fun YFBitmap.pixelOffset(x: Int, y: Int): Long {
    return (y * width + x) * 4L
}

// 核心像素读写
suspend fun YFBitmap.setPixel(x: Int, y: Int, color: YFColor) = setPixelArgb(x, y, color.toArgb())
suspend fun YFBitmap.setPixelArgb(x: Int, y: Int, argb: Int) {
    if (x !in 0 until width || y !in 0 until height) return
    stream.setWritePosition(pixelOffset(x, y))
    stream.writeInt(argb)
}

fun YFBitmap.getPixel(x: Int, y: Int): YFColor = YFColor(getPixelArgb(x, y))
fun YFBitmap.getPixelArgb(x: Int, y: Int): Int {
    if (x !in 0 until width || y !in 0 until height) return 0
    stream.readPosition = pixelOffset(x, y)
    return stream.readInt()
}

// 批量遍历 (顺序读取，性能最高)
inline fun YFBitmap.forEachPosition(action: (x: Int, y: Int) -> Unit) {
    val total = width * height
    for (i in 0 until total) {
        action(i % width, i / width)
    }
}

inline fun YFBitmap.forEachArgb(action: (argb: Int) -> Unit) {
    stream.readPosition = 0
    val total = width * height
    for (i in 0 until total) {
        action(stream.readInt())
    }
}

inline fun YFBitmap.forEachArgbIndexed(action: (i: Int, argb: Int) -> Unit) {
    stream.readPosition = 0
    val total = width * height
    for (i in 0 until total) {
        action(i, stream.readInt())
    }
}

inline fun YFBitmap.forEachArgb(action: (x: Int, y: Int, argb: Int) -> Unit) {
    stream.readPosition = 0
    val total = width * height
    for (i in 0 until total) {
        action(i % width, i / width, stream.readInt())
    }
}

inline fun YFBitmap.forEachPixel(action: (color: YFColor) -> Unit) =
    forEachArgb { argb -> action(YFColor(argb)) }

inline fun YFBitmap.forEachPixelIndexed(action: (i: Int, color: YFColor) -> Unit) =
    forEachArgbIndexed { i, argb -> action(i, YFColor(argb)) }

inline fun YFBitmap.forEachPixel(action: (x: Int, y: Int, color: YFColor) -> Unit) =
    forEachArgb { x, y, argb -> action(x, y, YFColor(argb)) }

inline fun YFBitmap.forEachPixelIndexed(action: (i: Int, x: Int, y: Int, color: YFColor) -> Unit) {
    forEachArgbIndexed { i, argb ->
        action(i, i % width, i / width, YFColor(argb))
    }
}

// 图像移动
suspend fun YFBitmap.moveTo(decImg: YFBitmap, mX: Int, mY: Int) {
    for (y in 0 until height) {
        val dstY = y + mY
        if (dstY < 0 || dstY >= decImg.height) continue
        for (x in 0 until width) {
            val dstX = x + mX
            if (dstX < 0 || dstX >= decImg.width) continue
            decImg.setPixelArgb(dstX, dstY, getPixelArgb(x, y))
        }
    }
}

// 图像裁剪
suspend fun YFBitmap.cut(mX: Int, mY: Int, mWidth: Int, mHeight: Int): YFBitmap {
    val result = YFBitmap(mWidth, mHeight)
    for (y in 0 until mHeight) {
        val srcY = y + mY
        if (srcY !in 0..<height) continue
        for (x in 0 until mWidth) {
            val srcX = x + mX
            if (srcX !in 0..<width) continue
            result.setPixelArgb(x, y, getPixelArgb(srcX, srcY))
        }
    }
    return result
}

// 旋转 (优化版：利用流批量读写)
suspend fun YFBitmap.rotate0(): YFBitmap {
    val result = YFBitmap(width, height)
    this.stream.readPosition = 0
    // 直接复制流 (零拷贝，如果是内存后端则极快)
    this.stream.copyTo(result.stream, (width * height * 4L).coerceAtMost(this.stream.length))
    return result
}

suspend fun YFBitmap.rotate90(): YFBitmap {
    val result = YFBitmap(height, width)
    for (y in 0 until height) {
        for (x in 0 until width) {
            result.setPixelArgb(height - y - 1, x, getPixelArgb(x, y))
        }
    }
    return result
}

suspend fun YFBitmap.rotate180(): YFBitmap {
    val result = YFBitmap(width, height)
    val total = width * height
    for (i in 0 until total) {
        val srcIdx = total - 1 - i
        this.stream.readPosition = srcIdx * 4L
        val argb = this.stream.readInt()
        result.stream.setWritePosition(i * 4L)
        result.stream.writeInt(argb)
    }
    return result
}

suspend fun YFBitmap.rotate270(): YFBitmap {
    val result = YFBitmap(height, width)
    for (y in 0 until height) {
        for (x in 0 until width) {
            result.setPixelArgb(y, width - x - 1, getPixelArgb(x, y))
        }
    }
    return result
}

// 便捷方法
fun YFBitmap.saveToPng(path: String) = saveToPng(File(path))
fun YFBitmap.saveToBmp(path: String) = saveToBmp(File(path))
suspend fun YFBitmap.loadFromFile(path: String) = loadFromFile(File(path))
suspend fun YFBitmap.Companion.fromFile(path: String): YFBitmap? = fromFile(File(path))