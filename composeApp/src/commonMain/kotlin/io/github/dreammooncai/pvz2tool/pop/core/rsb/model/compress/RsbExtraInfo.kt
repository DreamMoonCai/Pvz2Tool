package io.github.dreammooncai.pvz2tool.pop.core.rsb.model.compress

import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

data class RsbExtraInfo(
    var index: UInt = 0u
) : ExtraInfo() {
    override fun read(bs: CoroutineBinaryStream): RsbExtraInfo {
        index = bs.readUInt32()
        return this
    }
}