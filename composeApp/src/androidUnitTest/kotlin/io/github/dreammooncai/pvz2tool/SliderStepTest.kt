package io.github.dreammooncai.pvz2tool

import io.github.dreammooncai.pvz2tool.ui.roundToDecimals
import io.github.dreammooncai.pvz2tool.ui.stepSlider
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 滑块步进（`« − 数值 + »`）纯逻辑单测。
 *
 * 重点：`0.1` 这类步长用 Double 累加会积累浮点误差（`0.30000000000000004`），
 * 界面会显示成「0.30000000000000004」并被 JS 读到脏值——必须按小数位收敛。
 */
class SliderStepTest {

    @Test
    fun stepsUpAndDown() {
        assertEquals(5.0, stepSlider(4.0, 1.0, 0.0, 10.0), 1e-9)
        assertEquals(3.0, stepSlider(4.0, -1.0, 0.0, 10.0), 1e-9)
    }

    @Test
    fun clampsToBounds() {
        assertEquals(10.0, stepSlider(10.0, 5.0, 0.0, 10.0), 1e-9)
        assertEquals(0.0, stepSlider(0.0, -5.0, 0.0, 10.0), 1e-9)
    }

    @Test
    fun reversedBoundsAreNormalized() {
        // min > max 传反了也要能用（自动纠正），不能返回 NaN / 越界
        assertEquals(5.0, stepSlider(4.0, 1.0, 10.0, 0.0), 1e-9)
    }

    @Test
    fun decimalStepDoesNotAccumulateFloatError() {
        // 连续三次 +0.1：直接累加会得到 0.30000000000000004
        var v = 0.0
        repeat(3) { v = stepSlider(v, 0.1, 0.0, 1.0, decimals = 2) }
        assertEquals(0.3, v, 1e-9)
        assertEquals("0.30", "%.2f".format(v))
    }

    @Test
    fun decimalsRoundsResult() {
        assertEquals(0.33, stepSlider(0.3333, 0.0, 0.0, 1.0, decimals = 2), 1e-9)
        assertEquals(0.0, stepSlider(0.4, 0.0, 0.0, 1.0, decimals = 0), 1e-9)
    }

    @Test
    fun roundToDecimalsHandlesZeroAndNegative() {
        assertEquals(3.0, roundToDecimals(2.5, 0), 1e-9)
        // 采用 Kotlin 的 roundToLong（Math.round）语义：.5 恒向正无穷取整，-2.5 → -2
        assertEquals(-2.0, roundToDecimals(-2.5, 0), 1e-9)
        assertEquals(1.23, roundToDecimals(1.2345, 2), 1e-9)
    }

    @Test
    fun largeStepJumpsToBound() {
        assertEquals(100.0, stepSlider(95.0, 50.0, 0.0, 100.0), 1e-9)
    }
}
