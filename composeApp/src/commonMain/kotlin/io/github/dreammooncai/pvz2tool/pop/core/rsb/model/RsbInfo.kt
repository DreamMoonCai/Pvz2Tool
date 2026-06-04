package io.github.dreammooncai.pvz2tool.pop.core.rsb.model

import io.github.dreammooncai.pvz2tool.pop.core.rsb.model.compress.CompressStringList
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream


data class RsbInfo(
    var head: RsbHeadInfo = RsbHeadInfo(),
    var fileList: CompressStringList = CompressStringList(0),
    var rsgpList: CompressStringList = CompressStringList(0),
    var compositeInfo: MutableList<RsbCompositeInfo> = mutableListOf(),
    var compositeList: CompressStringList = CompressStringList(0),
    var rsgpInfo: MutableList<RsbRsgpInfo> = mutableListOf(),
    var autopoolInfo: MutableList<RsbAutoPoolInfo> = mutableListOf(),
    var rsgp: MutableList<RsgpInfo> = mutableListOf()
) {

    suspend fun read(bs: CoroutineBinaryStream): RsbInfo {
        head.read(bs)
        val countComposite = head.composite_Number.toInt()
        val countRsgp = head.rsgp_Number.toInt()
        val countAutopool = head.autopool_Number.toInt()

        // ✅ 直接在初始化时 read，不需要再写循环！
        bs.readPosition = head.compositeInfo_BeginOffset.toLong()
        compositeInfo = MutableList(countComposite) {
            RsbCompositeInfo().read(bs)
        }

        bs.readPosition = head.rsgpInfo_BeginOffset.toLong()
        rsgpInfo = MutableList(countRsgp) {
            RsbRsgpInfo().read(bs)
        }

        bs.readPosition = head.autopoolInfo_BeginOffset.toLong()
        autopoolInfo = MutableList(countAutopool) {
            RsbAutoPoolInfo().read(bs)
        }

        return this
    }

    fun getPtxInfo(bs: CoroutineBinaryStream,index: Int): RsbPtxInfo {
        val eachLen = head.ptxInfo_EachLength
        val absoluteOffset = head.ptxInfo_BeginOffset.toLong() + (index.toLong() * eachLen.toLong())

        // 3. 额外安全检查：防止计算出的偏移超出流范围（可选）
        if (absoluteOffset >= bs.length) {
            throw IndexOutOfBoundsException(
                "Ptx index $index is out of bounds. " +
                        "Calculated offset: $absoluteOffset, Stream length: ${bs.length}"
            )
        }

        return bs.slice(absoluteOffset, eachLen.toLong()).use { stream ->
            RsbPtxInfo(eachLen).read(stream)
        }
    }
}