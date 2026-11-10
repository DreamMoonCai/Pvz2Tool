package io.github.dreammooncai.pvz2tool

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.dreammooncai.pvz2tool.ui.dialog.ChoiceOrderStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 多选弹窗「拖动排序」持久化的设备侧单测（SharedPreferences + JSON 往返）。
 *
 * 为什么不放 JVM：`ChoiceOrderStore` 依赖 `InitializePvz2.context` 拿 SharedPreferences，
 * JVM 单测没有真实 Context。这里只验证存取本身；顺序合并逻辑由 `ItemOrderTest` 覆盖。
 */
@RunWith(AndroidJUnit4::class)
class ChoiceOrderStoreTest {

    private val key = "test_order_key"

    @Before
    fun setUp() {
        InitializePvz2.context = InstrumentationRegistry.getInstrumentation().targetContext
        ChoiceOrderStore.clear(key)
    }

    @After
    fun tearDown() {
        ChoiceOrderStore.clear(key)
    }

    @Test
    fun loadReturnsEmptyWhenNothingSaved() {
        assertTrue(ChoiceOrderStore.load(key).isEmpty())
    }

    @Test
    fun saveThenLoadRoundTrip() {
        val order = listOf("cool", "sun", "auto")
        ChoiceOrderStore.save(key, order)
        assertEquals(order, ChoiceOrderStore.load(key))
    }

    @Test
    fun saveOverwritesPreviousOrder() {
        ChoiceOrderStore.save(key, listOf("a", "b"))
        ChoiceOrderStore.save(key, listOf("b", "a"))
        assertEquals(listOf("b", "a"), ChoiceOrderStore.load(key))
    }

    @Test
    fun valuesWithSpecialCharsSurviveJsonRoundTrip() {
        // 条目 value 可能含中文/引号/逗号：JSON 序列化必须原样还原
        val order = listOf("阳光,无限", "带\"引号\"", "换行\n值")
        ChoiceOrderStore.save(key, order)
        assertEquals(order, ChoiceOrderStore.load(key))
    }

    @Test
    fun clearRemovesOrder() {
        ChoiceOrderStore.save(key, listOf("a"))
        ChoiceOrderStore.clear(key)
        assertTrue(ChoiceOrderStore.load(key).isEmpty())
    }
}
