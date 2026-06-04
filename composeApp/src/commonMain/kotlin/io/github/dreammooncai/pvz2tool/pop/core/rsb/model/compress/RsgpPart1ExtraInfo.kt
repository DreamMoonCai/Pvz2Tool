package io.github.dreammooncai.pvz2tool.pop.core.rsb.model.compress

import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

data class RsgpPart1ExtraInfo(
    var type: Int,
    var offset: UInt,
    var size: UInt,
    var index: UInt,
    var empty1: Int,
    var empty2: Int,
    var width: UInt,
    var height: UInt
) : RsgpExtraInfo() {

    constructor(): this(0u,0u,0u,0u,0u)

    constructor(offset: UInt,size: UInt,index: UInt,width: UInt,height: UInt): this(
        type = 1,
        offset = offset,
        size = size,
        index = index,
        empty1 = 0,
        empty2 = 0,
        width = width,
        height = height
    )

    override fun read(bs: CoroutineBinaryStream): RsgpPart1ExtraInfo {
        bs.idInt32(type)
        offset = bs.readUInt32()
        size = bs.readUInt32()
        index = bs.readUInt32()
        bs.idInt32(empty1)
        bs.idInt32(empty2)
        width = bs.readUInt32()
        height = bs.readUInt32()
        return this
    }
}