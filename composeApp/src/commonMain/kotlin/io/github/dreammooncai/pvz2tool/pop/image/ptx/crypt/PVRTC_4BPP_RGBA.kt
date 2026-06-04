package io.github.dreammooncai.pvz2tool.pop.image.ptx.crypt

import io.github.dreammooncai.pvz2tool.pop.image.Texture
import io.github.dreammooncai.pvz2tool.pop.image.YFBitmap
import io.github.dreammooncai.pvz2tool.pop.image.cut
import io.github.dreammooncai.pvz2tool.pop.plugin.decode.PVRTCDecode
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

/**
 * PVRTC_4BPP_RGBA 解码器（严格复刻 C# PopStudio 原版）
 * 自动补齐为 2 的幂次方，最小 8x8，解码后裁切回原始尺寸
 */
class PVRTC_4BPP_RGBA : Texture() {

    override suspend fun decode(bs: CoroutineBinaryStream, width: Int, height: Int): YFBitmap {
        var needCut = false
        var newWidth = width
        var newHeight = height

        // 最小尺寸 8x8
        if (newWidth < 8) {
            newWidth = 8
            needCut = true
        }
        if (newHeight < 8) {
            newHeight = 8
            needCut = true
        }

        // 补齐到 2 的幂次方 (POT)
        newWidth = getNextPOT(newWidth)
        newHeight = getNextPOT(newHeight)
        if (newWidth != width || newHeight != height) {
            needCut = true
        }

        // 读取 PVRTC 数据包 (4bpp = 宽度*高度/2 字节)
        val packetSize = (newWidth * newHeight) shr 1

        // 解码 PVRTC
        val image = YFBitmap(newWidth, newHeight)
        PVRTCDecode.pvrtcDecompress(bs.readBytes(packetSize), image, newWidth, newHeight, 4)

        // 解码后裁切回原始尺寸
        return if (needCut) {
            image.cut(0, 0, width, height)
        } else {
            image
        }
    }

    /**
     * 获取 >=v 的最小 2 的幂次方
     * 与 C# GetNextPOT 完全一致
     */
    private fun getNextPOT(v: Int): Int {
        var k = 1
        while (k < v) k = k shl 1
        return k
    }
}