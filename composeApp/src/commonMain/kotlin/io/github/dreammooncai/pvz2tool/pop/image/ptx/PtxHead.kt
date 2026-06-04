package io.github.dreammooncai.pvz2tool.pop.image.ptx

import io.github.dreammooncai.pvz2tool.pop.plugin.io.Endian
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

data class PtxHead(
    var width: Int = 0,
    var height: Int = 0,
    var check: Int = 0,
    var format: PtxFormat = PtxFormat.ARGB8888,
    var alphaSize: Int = 0,
    var alphaFormat: Int = 0
) {

    fun read(bs: CoroutineBinaryStream): PtxHead {
        val thisMagic = bs.readInt32()
        if (thisMagic == 829977712) { // 1xtp
            bs.endian = if (bs.endian == Endian.Small) Endian.Big else Endian.Small
        } else if (thisMagic != magic) {
            error("数据不匹配")
        }
        bs.idInt32(version)
        width = bs.readInt32()
        height = bs.readInt32()
        check = bs.readInt32()
        format = PtxFormat.form(bs.readInt32())
        alphaSize = bs.readInt32()
        alphaFormat = bs.readInt32()
        return this
    }

    suspend fun write(bs: CoroutineBinaryStream) {
        bs.writeInt32(magic)
        bs.writeInt32(version)
        bs.writeInt32(width)
        bs.writeInt32(height)
        bs.writeInt32(check)
        bs.writeInt32(format.id)
        bs.writeInt32(alphaSize)
        bs.writeInt32(alphaFormat)
    }

    companion object {
        const val magic = 1886681137 // ptx1
        const val version = 1
    }
}