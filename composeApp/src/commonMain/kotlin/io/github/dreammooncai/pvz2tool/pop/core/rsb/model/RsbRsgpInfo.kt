package io.github.dreammooncai.pvz2tool.pop.core.rsb.model

import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

data class RsbRsgpInfo(
    var ID: String = "",
    var offset: UInt = 0u,
    var size: UInt = 0u,
    var pool_Index: UInt = 0u,
    var flags: UInt = 0b1u,
    var fileOffset: UInt = 0u,
    var part0_Offset: UInt = 0u,
    var part0_ZSize: UInt = 0u,
    var part0_Size: UInt = 0u,
    var part0_Size2: UInt = 0u,
    var part1_Offset: UInt = 0u,
    var part1_ZSize: UInt = 0u,
    var part1_Size: UInt = 0u,
    var ptx_Number: UInt = 0u,
    var ptx_BeforeNumber: UInt = 0u
) {
    // 写入逻辑
    suspend fun write(bs: CoroutineBinaryStream) {
        bs.writeString(ID, 0x80)
        bs.writeUInt32(offset)
        bs.writeUInt32(size)
        bs.writeUInt32(pool_Index)
        bs.writeUInt32(flags)
        bs.writeUInt32(fileOffset)
        bs.writeUInt32(part0_Offset)
        bs.writeUInt32(part0_ZSize)
        bs.writeUInt32(part0_Size)
        bs.writeUInt32(if (part0_Size2 == 0u) part0_Size else part0_Size2)
        bs.writeUInt32(part1_Offset)
        bs.writeUInt32(part1_ZSize)
        bs.writeUInt32(part1_Size)
        bs.writeInt32(0)
        bs.writeInt32(0)
        bs.writeInt32(0)
        bs.writeInt32(0)
        bs.writeInt32(0)
        bs.writeUInt32(ptx_Number)
        bs.writeUInt32(ptx_BeforeNumber)
    }

    // 读取逻辑
    suspend fun read(bs: CoroutineBinaryStream): RsbRsgpInfo {
        ID = bs.readString(0x80).replace("\u0000", "")
        offset = bs.readUInt32()
        size = bs.readUInt32()
        pool_Index = bs.readUInt32()
        flags = bs.readUInt32()
        fileOffset = bs.readUInt32()
        part0_Offset = bs.readUInt32()
        part0_ZSize = bs.readUInt32()
        part0_Size = bs.readUInt32()
        part0_Size2 = bs.readUInt32()
        part1_Offset = bs.readUInt32()
        part1_ZSize = bs.readUInt32()
        part1_Size = bs.readUInt32()
        bs.idInt32(0)
        bs.idInt32(0)
        bs.idInt32(0)
        bs.idInt32(0)
        bs.idInt32(0)
        ptx_Number = bs.readUInt32()
        ptx_BeforeNumber = bs.readUInt32()
        return this
    }
}