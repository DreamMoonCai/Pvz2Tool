package io.github.dreammooncai.pvz2tool.pop.core.rsb.model.compress

import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

abstract class ExtraInfo {
    abstract fun read(bs: CoroutineBinaryStream): ExtraInfo
}