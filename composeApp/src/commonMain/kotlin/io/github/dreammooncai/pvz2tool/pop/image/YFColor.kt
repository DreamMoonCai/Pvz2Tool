package io.github.dreammooncai.pvz2tool.pop.image

/**
 * 【极致性能优化版】ARGB 颜色值
 * 核心优化：
 * 1. inline class：运行时就是 Int，零对象开销
 * 2. 语法完全兼容原有 API
 * 3. 性能 = 直接操作 Int
 */
@JvmInline
value class YFColor(val argb: Int) {

    // ==================== 构造与工厂方法 ====================

    constructor(
        red: Int = 0,
        green: Int = 0,
        blue: Int = 0,
        alpha: Int = 255
    ) : this(
        (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    )

    // ==================== 颜色分量访问（inline，零开销） ====================

    val red: Int
        inline get() = (argb shr 16) and 0xFF

    val green: Int
        inline get() = (argb shr 8) and 0xFF

    val blue: Int
        inline get() = argb and 0xFF

    val alpha: Int
        inline get() = (argb shr 24) and 0xFF

    // ==================== 兼容原有 setArgb 方法 ====================

    fun setArgb(argb: Int): YFColor {
        return YFColor(argb)
    }

    // ==================== 格式转换（inline，零开销） ====================

    /**
     * 将颜色转换为32位ARGB整数
     */
    fun toArgb(): Int = argb

    /**
     * 将颜色转换为32位RGBA整数（用于某些格式）
     */
    fun toRgba(): Int {
        return (red shl 24) or (green shl 16) or (blue shl 8) or alpha
    }

    fun toAbgr(): Int {
        return (alpha shl 24) or (blue shl 16) or (green shl 8) or red
    }

    /**
     * 将颜色转换为16位RGB565格式
     */
    fun toRgb565(): Int {
        val r = (red shr 3) and 0x1F
        val g = (green shr 2) and 0x3F
        val b = (blue shr 3) and 0x1F
        return (r shl 11) or (g shl 5) or b
    }

    /**
     * 将颜色转换为16位RGBA4444格式
     */
    fun toRgba4444(): Int {
        val r = (red shr 4) and 0xF
        val g = (green shr 4) and 0xF
        val b = (blue shr 4) and 0xF
        val a = (alpha shr 4) and 0xF
        return (r shl 12) or (g shl 8) or (b shl 4) or a
    }

    /**
     * 将颜色转换为16位RGBA5551格式
     */
    fun toRgba5551(): Int {
        val r = (red shr 3) and 0x1F
        val g = (green shr 3) and 0x1F
        val b = (blue shr 3) and 0x1F
        val a = if (alpha >= 128) 1 else 0
        return (r shl 11) or (g shl 6) or (b shl 1) or a
    }

    /**
     * 将颜色转换为16位ABGR4444格式（用于某些格式）
     */
    fun toAbgr4444(): Int {
        val a = (alpha shr 4) and 0xF
        val b = (blue shr 4) and 0xF
        val g = (green shr 4) and 0xF
        val r = (red shr 4) and 0xF
        return (a shl 12) or (b shl 8) or (g shl 4) or r
    }

    // ==================== 方便的 copy 方法（类似 data class） ====================

    fun copy(
        red: Int = this.red,
        green: Int = this.green,
        blue: Int = this.blue,
        alpha: Int = this.alpha
    ): YFColor {
        return YFColor(red, green, blue, alpha)
    }

    // ==================== toString 调试 ====================

    override fun toString(): String {
        return "YFColor(A=$alpha, R=$red, G=$green, B=$blue)"
    }

    companion object {
        /**
         * 从32位ARGB整数创建颜色
         */
        fun fromArgb(argb: Int): YFColor {
            return YFColor(argb)
        }

        /**
         * 从32位RGBA整数创建颜色
         */
        fun fromRgba(rgba: Int): YFColor {
            val r = (rgba shr 24) and 0xFF
            val g = (rgba shr 16) and 0xFF
            val b = (rgba shr 8) and 0xFF
            val a = rgba and 0xFF
            return YFColor(r, g, b, a)
        }

        /**
         * 从16位RGB565格式创建颜色
         */
        fun fromRgb565(rgb565: Int): YFColor {
            val r = (rgb565 shr 11) and 0x1F
            val g = (rgb565 shr 5) and 0x3F
            val b = rgb565 and 0x1F

            // 转换为8位值
            val r8 = (r shl 3) or (r shr 2)
            val g8 = (g shl 2) or (g shr 4)
            val b8 = (b shl 3) or (b shr 2)

            return YFColor(r8, g8, b8, 255)
        }

        /**
         * 从16位RGBA4444格式创建颜色
         */
        fun fromRgba4444(rgba4444: Int): YFColor {
            val r = (rgba4444 shr 12) and 0xF
            val g = (rgba4444 shr 8) and 0xF
            val b = (rgba4444 shr 4) and 0xF
            val a = rgba4444 and 0xF

            // 转换为8位值（左移4位复制到低位）
            val r8 = (r shl 4) or r
            val g8 = (g shl 4) or g
            val b8 = (b shl 4) or b
            val a8 = (a shl 4) or a

            return YFColor(r8, g8, b8, a8)
        }

        /**
         * 从16位RGBA5551格式创建颜色
         */
        fun fromRgba5551(rgba5551: Int): YFColor {
            val r = (rgba5551 shr 11) and 0x1F
            val g = (rgba5551 shr 6) and 0x1F
            val b = (rgba5551 shr 1) and 0x1F
            val a = rgba5551 and 0x1

            // 转换为8位值
            val r8 = (r shl 3) or (r shr 2)
            val g8 = (g shl 3) or (g shr 2)
            val b8 = (b shl 3) or (b shr 2)
            val a8 = if (a == 1) 255 else 0

            return YFColor(r8, g8, b8, a8)
        }

        /**
         * 从16位ABGR4444格式创建颜色
         */
        fun fromAbgr4444(abgr4444: Int): YFColor {
            val a = (abgr4444 shr 12) and 0xF
            val b = (abgr4444 shr 8) and 0xF
            val g = (abgr4444 shr 4) and 0xF
            val r = abgr4444 and 0xF

            // 转换为8位值
            val a8 = (a shl 4) or a
            val b8 = (b shl 4) or b
            val g8 = (g shl 4) or g
            val r8 = (r shl 4) or r

            return YFColor(r8, g8, b8, a8)
        }

        /**
         * 从16位ARGB1555格式创建颜色
         */
        fun fromArgb1555(argb1555: Int): YFColor {
            val a = (argb1555 shr 15) and 0x1
            val r = (argb1555 shr 10) and 0x1F
            val g = (argb1555 shr 5) and 0x1F
            val b = argb1555 and 0x1F

            // 转换为8位值
            val a8 = if (a == 1) 255 else 0
            val r8 = (r shl 3) or (r shr 2)
            val g8 = (g shl 3) or (g shr 2)
            val b8 = (b shl 3) or (b shr 2)

            return YFColor(r8, g8, b8, a8)
        }

        /**
         * 从16位L8格式创建颜色（灰度）
         */
        fun fromL8(l8: Int): YFColor {
            val gray = l8 and 0xFF
            return YFColor(gray, gray, gray, 255)
        }

        /**
         * 从16位A8格式创建颜色（仅alpha）
         */
        fun fromA8(a8: Int): YFColor {
            val alpha = a8 and 0xFF
            return YFColor(255, 255, 255, alpha)
        }

        /**
         * 从16位LA8格式创建颜色（灰度+alpha）
         */
        fun fromLa8(la8: Int): YFColor {
            val l = (la8 shr 8) and 0xFF
            val a = la8 and 0xFF
            return YFColor(l, l, l, a)
        }

        /**
         * 从16位L8A8格式创建颜色（灰度+alpha）
         */
        fun fromL8A8(l8a8: Int): YFColor {
            val l = (l8a8 shr 8) and 0xFF
            val a = l8a8 and 0xFF
            return YFColor(l, l, l, a)
        }

        val TRANSPARENT = YFColor(0, 0, 0, 0)
        val BLACK = YFColor(0, 0, 0, 255)
        val WHITE = YFColor(255, 255, 255, 255)
        val RED = YFColor(255, 0, 0, 255)
        val GREEN = YFColor(0, 255, 0, 255)
        val BLUE = YFColor(0, 0, 255, 255)
    }
}