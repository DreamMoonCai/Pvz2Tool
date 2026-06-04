package io.github.dreammooncai.pvz2tool.pop.core.rsb.model

import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

data class RsgpHeadInfo(
    var magic_t: Int = magic,
    var version: Int = 0x3,
    var flags: UInt = 0b1u,
    var fileOffset: UInt = 0u,
    var part0_Offset: UInt = 0u,
    var part0_ZSize: UInt = 0u,
    var part0_Size: UInt = 0u,
    var part1_Offset: UInt = 0u,
    var part1_ZSize: UInt = 0u,
    var part1_Size: UInt = 0u,
    var fileList_Length: UInt = 0u,
    var fileList_BeginOffset: UInt = 0x5Cu
) {
    companion object {
        const val magic: Int = 1920165744
        const val maxVersion: Int = 4
    }

    suspend fun write(bs: CoroutineBinaryStream) {
        bs.writeInt32(magic_t)
        bs.writeInt32(version)
        bs.writeInt32(0)
        bs.writeInt32(0)
        bs.writeUInt32(flags)
        bs.writeUInt32(fileOffset)
        bs.writeUInt32(part0_Offset)
        bs.writeUInt32(part0_ZSize)
        bs.writeUInt32(part0_Size)
        bs.writeInt32(0)
        bs.writeUInt32(part1_Offset)
        bs.writeUInt32(part1_ZSize)
        bs.writeUInt32(part1_Size)
        bs.writeInt32(0)
        bs.writeInt32(0)
        bs.writeInt32(0)
        bs.writeInt32(0)
        bs.writeInt32(0)
        bs.writeUInt32(fileList_Length)
        bs.writeUInt32(fileList_BeginOffset)
        bs.writeInt32(0)
        bs.writeInt32(0)
        bs.writeInt32(0)
    }

    fun read(bs: CoroutineBinaryStream): RsgpHeadInfo {
        magic_t = bs.readInt32()
        version = bs.readInt32()
        bs.readInt32()
        bs.readInt32()
        flags = bs.readUInt32()
        fileOffset = bs.readUInt32()
        part0_Offset = bs.readUInt32()
        part0_ZSize = bs.readUInt32()
        part0_Size = bs.readUInt32()
        bs.readInt32()
        part1_Offset = bs.readUInt32()
        part1_ZSize = bs.readUInt32()
        part1_Size = bs.readUInt32()
        bs.readInt32()
        bs.readInt32()
        bs.readInt32()
        bs.readInt32()
        bs.readInt32()
        fileList_Length = bs.readUInt32()
        fileList_BeginOffset = bs.readUInt32()
        bs.readInt32()
        bs.readInt32()
        bs.readInt32()
        return this
    }
}