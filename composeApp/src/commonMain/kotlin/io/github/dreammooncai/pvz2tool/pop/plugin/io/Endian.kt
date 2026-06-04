package io.github.dreammooncai.pvz2tool.pop.plugin.io

import java.nio.ByteOrder

enum class Endian(val byteOrder: ByteOrder) {
    Null(ByteOrder.nativeOrder()), Small(ByteOrder.LITTLE_ENDIAN), Big(ByteOrder.BIG_ENDIAN);

    val isNull: Boolean
        inline get() = this == Null
}