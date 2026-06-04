package io.github.dreammooncai.pvz2tool.pop.core.rsb.model

import io.github.dreammooncai.pvz2tool.pop.image.ptx.PtxFormat
import io.github.dreammooncai.pvz2tool.pop.image.ptx.PtxHead
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

class RsbPtxInfo {
    private val Ptx_EachLength: UInt

    var width: UInt = 0u
    var height: UInt = 0u
    var check: UInt = 0u
    var format: UInt = 0u
    var alphaSize: UInt = 0u
    var alphaFormat: UInt = 0u

    // 无参构造
    constructor() {
        Ptx_EachLength = 0x10u
    }

    // 带长度构造 + 校验
    constructor(ptxEachLength: UInt) {
        if (ptxEachLength != 0x10u && ptxEachLength != 0x14u && ptxEachLength != 0x18u) {
            throw Exception("Invalid Ptx_EachLength")
        }
        this.Ptx_EachLength = ptxEachLength
    }

    fun toPtxHead(): PtxHead = PtxHead(width.toInt(),height.toInt(),check.toInt(), PtxFormat.form(format.toInt()),alphaSize.toInt(),alphaFormat.toInt())

    // 写入
    suspend fun write(bs: CoroutineBinaryStream) {
        bs.writeUInt32(width)
        bs.writeUInt32(height)
        bs.writeUInt32(check)
        bs.writeUInt32(format)

        if (Ptx_EachLength != 0x10u) {
            bs.writeUInt32(alphaSize)
        }
        if (Ptx_EachLength == 0x18u) {
            bs.writeUInt32(alphaFormat)
        }
    }

    // 读取
    fun read(bs: CoroutineBinaryStream): RsbPtxInfo {
        width = bs.readUInt32()
        height = bs.readUInt32()
        check = bs.readUInt32()
        format = bs.readUInt32()

        if (Ptx_EachLength >= 0x14u) {
            alphaSize = bs.readUInt32()
            alphaFormat = if (Ptx_EachLength == 0x18u) {
                bs.readUInt32()
            } else {
                if (alphaSize == 0u) 0u else 0x64u
            }
        }
        return this
    }
}