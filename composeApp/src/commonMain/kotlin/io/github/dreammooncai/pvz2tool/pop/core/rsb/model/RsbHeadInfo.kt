package io.github.dreammooncai.pvz2tool.pop.core.rsb.model

import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

data class RsbHeadInfo(
    var version: Int = 3,
    var headLength: UInt = 0u,
    var fileList_Length: UInt = 0u,
    var fileList_BeginOffset: UInt = 0u,
    var rsgpList_Length: UInt = 0u,
    var rsgpList_BeginOffset: UInt = 0u,
    var rsgp_Number: UInt = 0u,
    var rsgpInfo_BeginOffset: UInt = 0u,
    var composite_Number: UInt = 0u,
    var compositeInfo_BeginOffset: UInt = 0u,
    var compositeList_Length: UInt = 0u,
    var compositeList_BeginOffset: UInt = 0u,
    var autopool_Number: UInt = 0u,
    var autopoolInfo_BeginOffset: UInt = 0u,
    var ptx_Number: UInt = 0u,
    var ptxInfo_BeginOffset: UInt = 0u,
    var ptxInfo_EachLength: UInt = 0x10u, // 中文版二代rsb是0x18
    var xmlPart1_BeginOffset: UInt = 0u,
    var xmlPart2_BeginOffset: UInt = 0u,
    var xmlPart3_BeginOffset: UInt = 0u,
    var rsbInfo_Length: UInt = 0u
) {
    companion object {
        const val magic: Int = 1920164401
        const val maxVersion: Int = 4
        const val rsgpInfo_EachLength: UInt = 0xCCu
        const val compositeInfo_EachLength: UInt = 0x484u
        const val autopoolInfo_EachLength: UInt = 0x98u
    }

    // 写入头部到流
    suspend fun write(bs: CoroutineBinaryStream) {
        bs.writeInt32(magic)
        bs.writeInt32(version)
        bs.writeInt32(0)
        bs.writeUInt32(headLength)
        bs.writeUInt32(fileList_Length)
        bs.writeUInt32(fileList_BeginOffset)
        bs.writeInt32(0)
        bs.writeInt32(0)
        bs.writeUInt32(rsgpList_Length)
        bs.writeUInt32(rsgpList_BeginOffset)
        bs.writeUInt32(rsgp_Number)
        bs.writeUInt32(rsgpInfo_BeginOffset)
        bs.writeUInt32(rsgpInfo_EachLength)
        bs.writeUInt32(composite_Number)
        bs.writeUInt32(compositeInfo_BeginOffset)
        bs.writeUInt32(compositeInfo_EachLength)
        bs.writeUInt32(compositeList_Length)
        bs.writeUInt32(compositeList_BeginOffset)
        bs.writeUInt32(autopool_Number)
        bs.writeUInt32(autopoolInfo_BeginOffset)
        bs.writeUInt32(autopoolInfo_EachLength)
        bs.writeUInt32(ptx_Number)
        bs.writeUInt32(ptxInfo_BeginOffset)
        bs.writeUInt32(ptxInfo_EachLength)
        bs.writeUInt32(xmlPart1_BeginOffset)
        bs.writeUInt32(xmlPart2_BeginOffset)
        bs.writeUInt32(xmlPart3_BeginOffset)
        if (version > maxVersion) {
            error("不支持的资源版本")
        }
        if (version == 4) {
            val value = if (rsbInfo_Length == 0u) {
                if (xmlPart1_BeginOffset == 0u) headLength else xmlPart1_BeginOffset
            } else {
                rsbInfo_Length
            }
            bs.writeUInt32(value)
        }
    }

    fun read(bs: CoroutineBinaryStream): RsbHeadInfo {
        bs.idInt32(magic)
        version = bs.readInt32()
        bs.idInt32(0)
        headLength = bs.readUInt32()
        fileList_Length = bs.readUInt32()
        fileList_BeginOffset = bs.readUInt32()
        bs.idInt32(0)
        bs.idInt32(0)
        rsgpList_Length = bs.readUInt32()
        rsgpList_BeginOffset = bs.readUInt32()
        rsgp_Number = bs.readUInt32()
        rsgpInfo_BeginOffset = bs.readUInt32()
        bs.idUInt32(rsgpInfo_EachLength)
        composite_Number = bs.readUInt32()
        compositeInfo_BeginOffset = bs.readUInt32()
        bs.idUInt32(compositeInfo_EachLength)
        compositeList_Length = bs.readUInt32()
        compositeList_BeginOffset = bs.readUInt32()
        autopool_Number = bs.readUInt32()
        autopoolInfo_BeginOffset = bs.readUInt32()
        bs.idUInt32(autopoolInfo_EachLength)
        ptx_Number = bs.readUInt32()
        ptxInfo_BeginOffset = bs.readUInt32()
        ptxInfo_EachLength = bs.readUInt32()
        xmlPart1_BeginOffset = bs.readUInt32()
        xmlPart2_BeginOffset = bs.readUInt32()
        xmlPart3_BeginOffset = bs.readUInt32()
        if (version > maxVersion) {
            error("不支持的资源版本")
        }
        if (version == 4) {
            rsbInfo_Length = bs.readUInt32()
        }
        return this
    }
}