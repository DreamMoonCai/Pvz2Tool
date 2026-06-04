package io.github.dreammooncai.pvz2tool.pop.core.rsb.model

import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

data class ChildRsgpInfo(
    var index: UInt = 0u,
    var ratio: UInt = 0u,
    var language: String = ""
) {

    // 写入
    suspend fun write(bs: CoroutineBinaryStream) {
        bs.writeUInt32(index)
        bs.writeUInt32(ratio)
        bs.writeString(language, 4, bs.endian)
        bs.writeInt32(0)
    }

    // 读取
    suspend fun read(bs: CoroutineBinaryStream): ChildRsgpInfo {
        index = bs.readUInt32()
        ratio = bs.readUInt32()
        language = bs.readString(4, bs.endian).replace("\u0000", "")
        bs.idInt32(0)
        return this
    }
}
