package io.github.dreammooncai.util

import io.github.dreammooncai.pvz2tool.pop.core.rsb.Rsb
import java.io.InputStream

/**
 * SMF 压缩容器头解析。
 *
 * ## 容器格式
 * 由 `Zlib.pack(isChineseMode = true)` 与 `RsbPacker.applySmfCompression` 写出：
 * ```
 * [0..3]  小端 int32  SMF_MAGIC (0xDEADFED4)
 * [4..7]  小端 int32  解压后的原始长度
 * [8..]   zlib(DEFLATE) 数据
 * ```
 * ⚠️ 字节序：`CoroutineBinaryStream` 默认 `Endian.Small`（小端），解析必须一致。
 *
 * ## 为什么单独抽出来
 * 「解压判据」需要把 APK 内压缩态的文件长度**归一化到解压后域**（取这里的原始长度），
 * 才能与本地已解压的明文文件长度比较。这部分是纯字节逻辑，抽出来后可以直接单元测试覆盖。
 */
object SmfHeader {

    /** 头部固定长度（magic + 原始长度） */
    const val HEADER_SIZE: Int = 8

    private fun leInt(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 3].toInt() and 0xFF) shl 24)

    /** 给定字节序列是否以 SMF 魔数开头。 */
    fun hasSmfMagic(bytes: ByteArray, offset: Int = 0): Boolean {
        if (bytes.size - offset < HEADER_SIZE) return false
        return leInt(bytes, offset) == Rsb.SMF_MAGIC
    }

    /**
     * 解析已读入内存的头部，返回「解压后原始长度」。
     *
     * @return 原始长度；不是 SMF 容器（魔数不符）或长度非法时返回 `-1`。
     *
     * ⚠️ 合法性上界：写入端是 `writeInt32(inFile.length.toInt())`，能产生的一定是**正 Int**。
     * 因此大于 `Int.MAX_VALUE` 的值（例如 `0xFFFFFFFF`）只可能来自损坏/伪造的头部，一律视为非法，
     * 否则会被无符号解释成 4294967295 这种荒谬长度，让解压判据彻底失真。
     */
    fun readContentSize(bytes: ByteArray, offset: Int = 0): Long {
        if (!hasSmfMagic(bytes, offset)) return -1L
        val original = leInt(bytes, offset + 4).toLong() and 0xFFFFFFFFL
        return if (original in 1L..Int.MAX_VALUE.toLong()) original else -1L
    }

    /**
     * 从流读取头部（最多消耗 [HEADER_SIZE] 字节），返回「解压后原始长度」。
     *
     * @return 原始长度；不是 SMF 容器、流不足 8 字节或长度非法时返回 `-1L`。
     */
    fun readContentSize(input: InputStream): Long {
        val header = ByteArray(HEADER_SIZE)
        var read = 0
        while (read < HEADER_SIZE) {
            val n = input.read(header, read, HEADER_SIZE - read)
            if (n <= 0) return -1L
            read += n
        }
        return readContentSize(header, 0)
    }
}
