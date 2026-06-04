package io.github.dreammooncai.pvz2tool.pop.image.ptx.crypt

import io.github.dreammooncai.pvz2tool.pop.image.Texture
import io.github.dreammooncai.pvz2tool.pop.image.YFBitmap
import io.github.dreammooncai.pvz2tool.pop.image.cut
import io.github.dreammooncai.pvz2tool.pop.image.getPixelArgb
import io.github.dreammooncai.pvz2tool.pop.image.setPixelArgb
import io.github.dreammooncai.pvz2tool.pop.plugin.decode.PVRTCDecode
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

/**
 * PVRTC_4BPP_RGBA_A8 解码器（严格复刻 C# PopStudio 原版）
 * 格式：PVRTC 4bpp 压缩RGB + 独立8bit Alpha通道
 * 最小尺寸 8x8，自动补齐2的幂，解码后裁切 + 读取Alpha
 */
class PVRTC_4BPP_RGBA_A8 : Texture() {

    override suspend fun decode(bs: CoroutineBinaryStream, width: Int, height: Int): YFBitmap {
        var needCut = false
        var newWidth = width
        var newHeight = height

        // 最小尺寸 8x8（与C#完全一致）
        if (newWidth < 8) {
            newWidth = 8
            needCut = true
        }
        if (newHeight < 8) {
            newHeight = 8
            needCut = true
        }

        // 补齐到2的幂次方 POT
        newWidth = getNextPOT(newWidth)
        newHeight = getNextPOT(newHeight)
        if (newWidth != width || newHeight != height) {
            needCut = true
        }

        // 读取PVRTC数据包 4bpp = width*height/2 字节
        val packetSize = (newWidth * newHeight) shr 1

        // 解码PVRTC
        val image = YFBitmap(newWidth, newHeight)
        PVRTCDecode.pvrtcDecompress(bs.readBytes(packetSize), image, newWidth, newHeight, 4)

        // 裁切回原始尺寸
        val finalImage = if (needCut) {
            image.cut(0, 0, width, height)
        } else {
            image
        }

        // ======================== 关键：读取外置 A8 Alpha 通道 ========================
        val pixelCount = finalImage.width * finalImage.height
        for (i in 0 until pixelCount) {
            val alpha = bs.readUInt8().toInt()
            val x = i % finalImage.width
            val y = i / finalImage.width
            val color = finalImage.getPixelArgb(x, y)
            val newColor = (alpha shl 24) or (color and 0x00FFFFFF)
            finalImage.setPixelArgb(x, y, newColor)
        }

        return finalImage
    }

    /**
     * 最小2的幂次方计算（与C# GetNextPOT完全一致）
     */
    private fun getNextPOT(v: Int): Int {
        var k = 1
        while (k < v) k = k shl 1
        return k
    }
}