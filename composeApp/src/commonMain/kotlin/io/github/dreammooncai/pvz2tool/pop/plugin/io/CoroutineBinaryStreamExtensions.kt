// CoroutineBinaryStreamExtensions.kt
package io.github.dreammooncai.pvz2tool.pop.plugin.io

import java.io.File
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.DoubleBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.LongBuffer
import java.nio.ShortBuffer

// ====================== 范围操作 ======================

/**
 * 使用 [ClosedRange] 进行切片
 * stream.slice(0L..100L)
 */
fun CoroutineBinaryStream.slice(range: ClosedRange<Long>): CoroutineBinaryStream {
    return slice(range.start, range.endInclusive - range.start + 1)
}

/**
 * 使用 until 语法切片
 * stream.slice(0L until 100L)
 */
fun CoroutineBinaryStream.slice(range: LongRange): CoroutineBinaryStream {
    return slice(range.first, range.last - range.first)
}

// ====================== 文件快捷操作 ======================

/**
 * 挂起保存并关闭（一站式）
 */
suspend fun CoroutineBinaryStream.saveAndClose(target: File) {
    use { it.saveFile(target) }
}

suspend fun CoroutineBinaryStream.saveAndClose(path: String) {
    saveAndClose(File(path))
}

// ====================== 集合批量读写 ======================

suspend fun CoroutineBinaryStream.writeIntList(list: List<Int>, endian: Endian = Endian.Null) {
    list.forEach { writeInt(it, endian) }
}

fun CoroutineBinaryStream.readIntList(count: Int, endian: Endian = Endian.Null): List<Int> {
    return List(count) { readInt(endian) }
}

// ====================== 带进度的大文件拷贝 ======================

suspend fun CoroutineBinaryStream.copyToWithProgress(
    target: CoroutineBinaryStream,
    length: Long = this.length - readPosition,
    bufferSize: Int = 8192,
    onProgress: (copied: Long, total: Long) -> Unit
) {
    var copied = 0L
    val buffer = ByteArray(bufferSize)
    while (copied < length) {
        val readCount = minOf(bufferSize, (length - copied).toInt())
        read(buffer, 0, readCount)
        target.write(buffer, count = readCount)
        copied += readCount
        onProgress(copied, length)
    }
}

/**
 * 全版本兼容的 Buffer.duplicate() 扩展方法
 * 自动判断类型，调用对应子类的 duplicate()，避免 API 34 限制
 */
@Suppress("UNCHECKED_CAST", "REDUNDANT_ELSE_IN_WHEN")
fun <T: Buffer> T.duplicateCompat(): T {
    return when (this) {
        is ByteBuffer -> this.duplicate()
        is CharBuffer -> this.duplicate()
        is ShortBuffer -> this.duplicate()
        is IntBuffer -> this.duplicate()
        is LongBuffer -> this.duplicate()
        is FloatBuffer -> this.duplicate()
        is DoubleBuffer -> this.duplicate()
        else -> error("Unsupported buffer type: ${this::class.simpleName}")
    } as T
}