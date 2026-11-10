package io.github.dreammooncai.pvz2tool

import io.github.dreammooncai.pvz2tool.ui.stepNumber
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 数量输入框（`PvzNumberInput`）**步进逻辑**的 JVM 单元测试。
 *
 * 组件的 ± 按钮、长按连续步进最终都归约到 [stepNumber] 这一个纯函数，
 * 因此这里不需要真机/模拟器即可覆盖「边界钳制 / 步进 / 异常入参」等关键行为。
 *
 * 运行：`./gradlew :composeApp:testDebugUnitTest --tests "*NumberInputTest*"`
 */
class NumberInputTest {

    @Test
    fun stepsUpAndDownWithinBounds() {
        assertEquals(6, stepNumber(5, 1, 1, 99))
        assertEquals(4, stepNumber(5, -1, 1, 99))
        assertEquals(15, stepNumber(5, 10, 1, 99))   // step 可大于 1
    }

    @Test
    fun clampsAtMinAndMax() {
        assertEquals(1, stepNumber(1, -1, 1, 99), "已到最小值，再减仍为 min")
        assertEquals(99, stepNumber(99, 1, 1, 99), "已到最大值，再加仍为 max")
        assertEquals(99, stepNumber(95, 10, 1, 99), "跨越最大值应钳制到 max")
        assertEquals(1, stepNumber(5, -10, 1, 99), "跨越最小值应钳制到 min")
    }

    @Test
    fun handlesSwappedBounds() {
        // 配置写反（min > max）时不应崩溃或越界
        assertEquals(10, stepNumber(5, 100, 10, 0))
        assertEquals(0, stepNumber(5, -100, 10, 0))
    }

    @Test
    fun largeDeltaDoesNotOverflow() {
        assertEquals(Int.MAX_VALUE, stepNumber(Int.MAX_VALUE - 1, 100, Int.MIN_VALUE, Int.MAX_VALUE))
        assertEquals(Int.MIN_VALUE, stepNumber(Int.MIN_VALUE + 1, -100, Int.MIN_VALUE, Int.MAX_VALUE))
    }

    @Test
    fun zeroDeltaKeepsValueClamped() {
        assertEquals(7, stepNumber(7, 0, 1, 99))
        assertEquals(99, stepNumber(120, 0, 1, 99), "即使不步进，也应把越界值钳制回来")
    }
}
