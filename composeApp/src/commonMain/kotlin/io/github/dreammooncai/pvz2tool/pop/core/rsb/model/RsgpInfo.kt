package io.github.dreammooncai.pvz2tool.pop.core.rsb.model

import io.github.dreammooncai.pvz2tool.pop.core.rsb.model.compress.CompressStringList
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

data class RsgpInfo(
    var head: RsgpHeadInfo = RsgpHeadInfo(),
    var fileList: CompressStringList = CompressStringList(1)
) {
    suspend fun read(bs: CoroutineBinaryStream): RsgpInfo {
        val back = bs.readPosition
        head.read(bs)

        // 跳转到 fileList 偏移
        bs.readPosition = back + head.fileList_BeginOffset.toLong()

        val bFileStream = bs.slice(
            offset = back + head.fileList_BeginOffset.toLong(),
            length = head.fileList_Length.toLong()
        )

        fileList.read(bFileStream)
        return this
    }
}