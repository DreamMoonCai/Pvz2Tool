package io.github.dreammooncai.pvz2tool.pop.image

import io.github.dreammooncai.pvz2tool.pop.image.ptx.Ptx
import io.github.dreammooncai.pvz2tool.pop.image.ptx.PtxFormat
import io.github.dreammooncai.pvz2tool.pop.image.ptx.PtxHead
import io.github.dreammooncai.pvz2tool.pop.image.ptx.crypt.*
import io.github.dreammooncai.pvz2tool.pop.plugin.io.Endian
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

/**
 * 纹理解码器 & 编码器基类
 */
abstract class Texture {
    /**
     * 编码整个图像（抽象方法，子类实现）
     * @param bs 二进制流
     * @param bitmap 原始位图数据
     * @return 用于检验大小的 check
     */
    open suspend fun encode(bs: CoroutineBinaryStream, bitmap: YFBitmap): Int = error("[${this::class.simpleName}] Not implemented")

    /**
     * 解码整个图像
     * @param bs 二进制流
     * @param width 图像宽度
     * @param height 图像高度
     * @return YFBitmap对象
     */
    abstract suspend fun decode(bs: CoroutineBinaryStream, width: Int, height: Int): YFBitmap

    companion object {

        fun getEncoder(
            format: PtxFormat,
            endian: Endian,
            chineseMode: Boolean = false
        ): Texture {
            return when (format) {
                // ETC1_RGB_A8 特殊分支：判断是否使用调色板
                PtxFormat.ETC1_RGB_A8 -> {
                    if (chineseMode) {
                        ETC1_RGB_A_Palette()
                    } else {
                        ETC1_RGB_A8()
                    }
                }

                // 标准格式映射
                PtxFormat.ARGB8888 -> {
                    if (Ptx.RsbPtxABGR8888Mode) {
                        ABGR8888()
                    } else if (Ptx.RsbPtxARGB8888PaddingMode) {
                        ARGB8888_Padding()
                    } else {
                        ARGB8888()
                    }
                }

                PtxFormat.RGBA4444 -> RGBA4444()
                PtxFormat.RGB565 -> RGB565()
                PtxFormat.RGBA5551 -> RGBA5551()
                PtxFormat.DXT5_RGBA_MortonBlock -> {
                    if (endian == Endian.Small) {
                        DXT5_RGBA_MortonBlock()
                    } else {
                        DXT5_RGBA()
                    }
                }

                PtxFormat.RGBA4444_Block -> RGBA4444_Block()
                PtxFormat.RGB565_Block -> RGB565_Block()
                PtxFormat.RGBA5551_Block -> RGBA5551_Block()
                PtxFormat.PVRTC_4BPP_RGBA -> PVRTC_4BPP_RGBA()
                PtxFormat.PVRTC_2BPP_RGBA -> PVRTC_2BPP_RGBA()
                PtxFormat.ETC1_RGB -> ETC1_RGB()
                PtxFormat.DXT1_RGB -> DXT1_RGB()
                PtxFormat.DXT3_RGBA -> DXT3_RGBA()
                PtxFormat.DXT5_RGBA -> DXT5_RGBA()
                PtxFormat.ATC_RGB -> ATC_RGB()
                PtxFormat.ATC_RGBA4 -> ATC_RGBA4()
                PtxFormat.PVRTC_4BPP_RGBA_A8 -> PVRTC_4BPP_RGBA_A8()
                PtxFormat.ARGB8888_A8 -> {
                    if (Ptx.RsbPtxABGR8888Mode) {
                        XBGR8888_A8()
                    } else {
                        XRGB8888_A8()
                    }
                }
                PtxFormat.ETC1_RGB_A_Palette -> ETC1_RGB_A_Palette()
            }
        }

        fun getDecoder(format: PtxFormat, endian: Endian, head: PtxHead, fromRsb: Boolean = false): Texture {
            return when (format) {
                PtxFormat.ETC1_RGB_A8 -> if (head.alphaFormat == 0x0) {
                    getDecoder(format, endian, fromRsb)
                } else {
                    getDecoder(PtxFormat.ETC1_RGB_A_Palette, endian, fromRsb)
                }

                else -> getDecoder(format, endian, fromRsb)
            }
        }

        /**
         * 根据格式ID获取对应的解码器
         * @param format 格式ID (PtxFormat枚举值)
         * @return 解码器实例
         */
        fun getDecoder(format: PtxFormat, endian: Endian, fromRsb: Boolean = false): Texture {
            return when (format) {
                PtxFormat.ARGB8888 -> {
                    if ((fromRsb && Ptx.RsbPtxABGR8888Mode) || ((!fromRsb) && Ptx.PtxABGR8888Mode))
                        ABGR8888()
                    else if ((fromRsb && Ptx.RsbPtxARGB8888PaddingMode) || ((!fromRsb) && Ptx.PtxARGB8888PaddingMode))
                        ARGB8888_Padding()
                    else
                        ARGB8888()
                }
                PtxFormat.RGBA4444 -> RGBA4444()
                PtxFormat.RGB565 -> RGB565()
                PtxFormat.RGBA5551 -> RGBA5551()
                PtxFormat.DXT5_RGBA_MortonBlock -> {
                    if (endian == Endian.Small)
                        DXT5_RGBA_MortonBlock()
                    else
                        DXT5_RGBA()
                }
                PtxFormat.RGBA4444_Block -> RGBA4444_Block()
                PtxFormat.RGB565_Block -> RGB565_Block()
                PtxFormat.RGBA5551_Block -> RGBA5551_Block()
                PtxFormat.PVRTC_4BPP_RGBA -> PVRTC_4BPP_RGBA()
                PtxFormat.PVRTC_2BPP_RGBA -> PVRTC_2BPP_RGBA()
                PtxFormat.ETC1_RGB -> ETC1_RGB()
                PtxFormat.DXT1_RGB -> DXT1_RGB()
                PtxFormat.DXT3_RGBA -> DXT3_RGBA()
                PtxFormat.DXT5_RGBA -> DXT5_RGBA()
                PtxFormat.ATC_RGB -> ATC_RGB()
                PtxFormat.ATC_RGBA4 -> ATC_RGBA4()
                PtxFormat.ETC1_RGB_A8 -> ETC1_RGB_A8()
                PtxFormat.PVRTC_4BPP_RGBA_A8 -> PVRTC_4BPP_RGBA_A8()
                PtxFormat.ARGB8888_A8 -> {
                    if ((fromRsb && Ptx.RsbPtxABGR8888Mode) || ((!fromRsb) && Ptx.PtxABGR8888Mode))
                        XBGR8888_A8()
                    else
                        XRGB8888_A8()
                }
                PtxFormat.ETC1_RGB_A_Palette -> ETC1_RGB_A_Palette()
            }
        }
    }
}