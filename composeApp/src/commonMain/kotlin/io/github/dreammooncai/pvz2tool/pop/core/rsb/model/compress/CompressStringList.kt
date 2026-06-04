@file:Suppress("JavaDefaultMethodsNotOverriddenByDelegation")

package io.github.dreammooncai.pvz2tool.pop.core.rsb.model.compress

import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.charset.StandardCharsets

private data class PrefixInfo(
    var prefix: ByteArray,
    var location: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PrefixInfo
        if (!prefix.contentEquals(other.prefix)) return false
        if (location != other.location) return false
        return true
    }

    override fun hashCode(): Int {
        var result = prefix.contentHashCode()
        result = 31 * result + location
        return result
    }
}

class CompressStringList(private val type: Int): MutableList<CompressString> by mutableListOf() {

    val mutex = Mutex()

    suspend fun addCompress(compress: CompressString) = mutex.withLock {
        add(compress)
    }

    suspend fun write(): ByteArray {
        // 1. 排序 (与C#一致: string.CompareOrdinal)
        mutex.withLock {
            sortBy { it.name }
        }

        val prefixList = mutableListOf<PrefixInfo>()
        prefixList.add(PrefixInfo(ByteArray(0), -1)) // 初始前缀
        var finishedBytes = ByteArray(0)

        // 2. 遍历处理每一个字符串
        for (tls in this) {
            val fullName = tls.name ?: ""
            val awaitList = mutableListOf<PrefixInfo>()

            // 注意：这里不添加 0x00，C#是在后面逻辑中加的
            var thisRest = fullName.toByteArray(StandardCharsets.UTF_8)

            var removeStart = false // 恢复 C# 的原始逻辑标志

            var i = 0
            // 3. 遍历前缀列表 (使用 while 循环配合 i-- 来模拟 C# 的 for 循环修改)
            while (i < prefixList.size) {
                val pi = prefixList[i]

                if (removeStart) {
                    // 逻辑：一旦开始移除，后续全部移除，并将其 cover 设为 0
                    writeInt24(finishedBytes, pi.location, 0)
                    prefixList.removeAt(i)
                    // 这里 i 不递增，因为列表元素前移了
                    continue
                }

                val prefix = pi.prefix
                var j = 0 // 匹配长度

                // 计算最长公共前缀 j
                while (j < prefix.size && j < thisRest.size) {
                    if (prefix[j] != thisRest[j]) break
                    j++
                }

                when {
                    // 情况 A: 完全匹配 (该前缀仍然有效)
                    j == prefix.size && prefix.isNotEmpty() -> {
                        awaitList.add(pi)
                        // 截断 thisRest (去掉已匹配的前缀)
                        thisRest = thisRest.copyOfRange(j, thisRest.size)
                        i++ // 检查下一个前缀
                    }

                    // 情况 B: 部分匹配 (需要拆分前缀)
                    j > 0 -> {
                        // 1. 修改被拆分的后半段前缀的 Cover (在文件中的 offset)
                        val location3 = pi.location + j * 4
                        writeInt24(finishedBytes, location3, finishedBytes.size / 4)

                        // 2. 更新当前前缀为匹配的前半段
                        pi.prefix = prefix.copyOfRange(0, j)
                        awaitList.add(pi)

                        // 3. 截断 thisRest
                        thisRest = thisRest.copyOfRange(j, thisRest.size)

                        // 4. 标记：后面的前缀全部需要移除
                        removeStart = true
                        i++ // 移动到下一个，以便在下一轮循环中被 remove
                    }

                    // 情况 C: 完全不匹配 (移除该前缀及后续)
                    else -> {
                        prefixList.removeAt(i)
                        // i 不递增，因为 remove 导致元素前移
                        removeStart = true
                    }
                }
            }

            // 4. 添加 0x00 终止符 (C# 逻辑：将剩余名字加个0再加入)
            val tmpThisBytes2 = ByteArray(thisRest.size + 1)
            System.arraycopy(thisRest, 0, tmpThisBytes2, 0, thisRest.size)
            thisRest = tmpThisBytes2 // 现在 thisRest 包含 0x00

            // 5. 将新的剩余名字加入 prefixList 和 awaitList
            val newPrefixInfo = PrefixInfo(thisRest, finishedBytes.size)
            prefixList.add(newPrefixInfo)
            awaitList.add(newPrefixInfo)

            // 6. 生成写入的字节块 (名字部分 + Info部分)
            val infoLength = when {
                type == 0 -> 4
                tls.type == 1 -> 12
                else -> 32
            }

            // 注意：这里使用 thisRest (包含0x00) 来计算长度
            val thisFinishedBytes = ByteArray(thisRest.size * 4 + infoLength)

            // 写入名字 (每字节扩为4字节，低8位有效)
            for (k in thisRest.indices) {
                thisFinishedBytes[k * 4] = thisRest[k]
            }

            // 写入 ExtraInfo
            when (infoLength) {
                4 -> {
                    val info = tls.extraInfo as RsbExtraInfo
                    writeInt32(thisFinishedBytes, thisFinishedBytes.size - 4, info.index.toInt())
                }
                12 -> {
                    val info = tls.extraInfo as RsgpPart0ExtraInfo
                    writeInt32(thisFinishedBytes, thisFinishedBytes.size - 12, info.type)
                    writeInt32(thisFinishedBytes, thisFinishedBytes.size - 8, info.offset.toInt())
                    writeInt32(thisFinishedBytes, thisFinishedBytes.size - 4, info.size.toInt())
                }
                32 -> {
                    val info = tls.extraInfo as RsgpPart1ExtraInfo
                    writeInt32(thisFinishedBytes, thisFinishedBytes.size - 32, info.type)
                    writeInt32(thisFinishedBytes, thisFinishedBytes.size - 28, info.offset.toInt())
                    writeInt32(thisFinishedBytes, thisFinishedBytes.size - 24, info.size.toInt())
                    writeInt32(thisFinishedBytes, thisFinishedBytes.size - 20, info.index.toInt())
                    writeInt32(thisFinishedBytes, thisFinishedBytes.size - 16, info.empty1)
                    writeInt32(thisFinishedBytes, thisFinishedBytes.size - 12, info.empty2)
                    writeInt32(thisFinishedBytes, thisFinishedBytes.size - 8, info.width.toInt())
                    writeInt32(thisFinishedBytes, thisFinishedBytes.size - 4, info.height.toInt())
                }
            }

            // 7. 拼接到总数据
            val l = finishedBytes.size
            finishedBytes = finishedBytes.copyOf(l + thisFinishedBytes.size)
            System.arraycopy(thisFinishedBytes, 0, finishedBytes, l, thisFinishedBytes.size)

            // 8. 更新所有 awaitList 中前缀的 Cover (作用范围)
            for (pi in awaitList) {
                writeInt24(finishedBytes, pi.location, finishedBytes.size / 4)
            }
        }

        // 9. 收尾：将剩余所有前缀的 Cover 设为 0
        for (pi in prefixList) {
            if (pi.prefix.isNotEmpty()) {
                writeInt24(finishedBytes, pi.location, 0)
            }
        }

        return finishedBytes
    }

    suspend fun read(bs: CoroutineBinaryStream): CompressStringList {
        mutex.withLock { clear() }
        val endOffset = bs.length
        val defaults = mutableListOf<Default>()
        defaults.add(Default("", bs.length.toInt()))

        while (bs.readPosition < endOffset) {
            var temp = ""
            var tempHead = ""

            var i = 0
            while (i < defaults.size) {
                val d = defaults[i]
                if (bs.readPosition < d.offset * 4) {
                    tempHead += d.name
                } else {
                    defaults.removeAt(i)
                    i--
                }
                i++
            }

            var startIndex = 0
            var tpendOffset = defaults.last().offset
            var tmpEndOffset: Int

            while (bs.peekByte() != 0.toByte()) {
                temp += bs.readByte().toInt().toChar()
                tmpEndOffset = bs.readUInt24().toInt()
                if (tmpEndOffset != 0) {
                    if (temp.length != 1) {
                        defaults.add(
                            Default(
                                temp.substring(startIndex, temp.length - 1),
                                tpendOffset
                            )
                        )
                    }
                    startIndex = temp.length - 1
                    tpendOffset = tmpEndOffset
                }
            }

            bs.readPosition++
            tmpEndOffset = bs.readUInt24().toInt()
            if (tmpEndOffset != 0 && temp.length != 1) {
                defaults.add(
                    Default(
                        temp.substring(startIndex, temp.length),
                        tpendOffset
                    )
                )
            }

            val fullName = tempHead + temp
            val extra = if (type == 0) {
                RsbExtraInfo().read(bs)
            } else {
                if (bs.peekInt32() == 0) {
                    RsgpPart0ExtraInfo().read(bs)
                } else {
                    RsgpPart1ExtraInfo().read(bs)
                }
            }
            addCompress(CompressString(fullName, extra))
        }
        return this
    }

    private fun writeInt24(bytes: ByteArray, location: Int, cover: Int) {
        bytes[location + 1] = (cover and 0xFF).toByte()
        bytes[location + 2] = ((cover shr 8) and 0xFF).toByte()
        bytes[location + 3] = ((cover shr 16) and 0xFF).toByte()
    }

    private fun writeInt32(bytes: ByteArray, location: Int, cover: Int) {
        writeInt32(bytes, location, cover.toUInt())
    }

    private fun writeInt32(bytes: ByteArray, location: Int, cover: UInt) {
        bytes[location] = (cover and 0xFFu).toByte()
        bytes[location + 1] = ((cover shr 8) and 0xFFu).toByte()
        bytes[location + 2] = ((cover shr 16) and 0xFFu).toByte()
        bytes[location + 3] = ((cover shr 24) and 0xFFu).toByte()
    }

    data class Default(
        var name: String = "",
        var offset: Int = 0,
        var bsOffset: Long = 0
    )
}

