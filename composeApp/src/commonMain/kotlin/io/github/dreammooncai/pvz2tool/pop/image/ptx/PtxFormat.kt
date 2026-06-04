package io.github.dreammooncai.pvz2tool.pop.image.ptx

enum class PtxFormat(val id: Int) {
    ARGB8888(0), //ABRG8888
    RGBA4444(1),
    RGB565(2),
    RGBA5551(3),
    DXT5_RGBA_MortonBlock(5),
    RGBA4444_Block(21),
    RGB565_Block(22),
    RGBA5551_Block(23),
    PVRTC_4BPP_RGBA(30),
    PVRTC_2BPP_RGBA(31),
    ETC1_RGB(32),
    DXT1_RGB(35),
    DXT3_RGBA(36),
    DXT5_RGBA(37),
    ATC_RGB(38),
    ATC_RGBA4(39),
    ETC1_RGB_A8(147), //ETC1_RGB_A_Palette
    PVRTC_4BPP_RGBA_A8(148),
    ARGB8888_A8(149), //ABGR8888_A8
    ETC1_RGB_A_Palette(150);

    companion object {
        fun form(id: Int) = entries.find { it.id == id } ?: run {
            println("警告: 未实现的PTX格式 $id，使用默认ARGB8888解码器")
            ARGB8888
        }
    }
}