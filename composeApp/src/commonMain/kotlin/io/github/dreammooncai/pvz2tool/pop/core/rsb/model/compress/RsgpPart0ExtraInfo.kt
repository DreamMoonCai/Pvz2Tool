package io.github.dreammooncai.pvz2tool.pop.core.rsb.model.compress

import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

data class RsgpPart0ExtraInfo(
    var type: Int,
    var offset: UInt,
    var size: UInt
) : RsgpExtraInfo() {

    constructor(): this(0u, 0u)

    constructor(offset: UInt, size: UInt) : this(0,offset, size)

    override fun read(bs: CoroutineBinaryStream): RsgpPart0ExtraInfo {
        bs.idInt32(type)
        offset = bs.readUInt32()
        size = bs.readUInt32()
        return this
    }
}