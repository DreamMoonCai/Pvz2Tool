package io.github.dreammooncai.pvz2tool.pop.core.rsb.model

import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

data class RsbCompositeInfo(
    var ID: String = "",
    var child_Number: UInt = 0u
) {
    // 固定 0x40 长度数组（64个）
    var child_Info: Array<ChildRsgpInfo> = Array(0x40) { ChildRsgpInfo() }

    // 写入
    suspend fun write(bs: CoroutineBinaryStream) {
        bs.writeString(ID, 0x80)
        for (i in 0 until 0x40) {
            child_Info[i].write(bs)
        }
        bs.writeUInt32(child_Number)
    }

    suspend fun read(bs: CoroutineBinaryStream): RsbCompositeInfo {
        ID = bs.readString(0x80).replace("\u0000", "")
        for (i in 0 until 0x40) {
            child_Info[i].read(bs)
        }
        child_Number = bs.readUInt32()
        return this
    }
}