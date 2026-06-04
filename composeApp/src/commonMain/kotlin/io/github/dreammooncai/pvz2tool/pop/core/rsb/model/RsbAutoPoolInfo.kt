package io.github.dreammooncai.pvz2tool.pop.core.rsb.model

import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

data class RsbAutoPoolInfo(
    var ID: String = "",
    var part1_MaxOffset_InDecompress: UInt = 0u,
    var part1_MaxSize: UInt = 0u,
    var type: Int = 0x1
) {
    // 写入
    suspend fun write(bs: CoroutineBinaryStream) {
        bs.writeString(ID, 0x80)
        bs.writeUInt32(part1_MaxOffset_InDecompress)
        bs.writeUInt32(part1_MaxSize)
        bs.writeInt32(type)
        bs.writeInt32(0)
        bs.writeInt32(0)
        bs.writeInt32(0)
    }

    // 读取
    suspend fun read(bs: CoroutineBinaryStream): RsbAutoPoolInfo {
        ID = bs.readString(0x80).replace("\u0000", "")
        part1_MaxOffset_InDecompress = bs.readUInt32()
        part1_MaxSize = bs.readUInt32()
        type = bs.readInt32()
        bs.idInt32(0)
        bs.idInt32(0)
        bs.idInt32(0)
        return this
    }
}