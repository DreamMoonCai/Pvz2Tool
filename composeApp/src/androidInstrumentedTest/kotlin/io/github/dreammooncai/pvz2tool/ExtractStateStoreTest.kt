package io.github.dreammooncai.pvz2tool

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.dreammooncai.util.ExtractStateStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [ExtractStateStore] 单元测试：源侧指纹记录的读写。
 *
 * 这套记录是「是否需要重新解压」判据的核心状态：指纹未变 → 保持本地（保护 JS/用户改动）；
 * 指纹变了 → APK 内资源已更新，必须重解压。因此必须保证写入/读回/覆盖/清空都可靠。
 */
@RunWith(AndroidJUnit4::class)
class ExtractStateStoreTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun resetStore() {
        ExtractStateStore.clear(context)
    }

    @Test
    fun missingKeyReturnsNull() {
        assertNull(ExtractStateStore.fingerprint(context, "asset:version/1/smf/a.rsb.smf|content://target/a"))
    }

    @Test
    fun writtenFingerprintCanBeReadBack() {
        val key = "asset:version/1/smf/a.rsb.smf|content://target/a"
        ExtractStateStore.putFingerprint(context, key, "1048576@1700000000000")
        assertEquals("1048576@1700000000000", ExtractStateStore.fingerprint(context, key))
    }

    @Test
    fun fingerprintCanBeOverwritten() {
        val key = "local:content://src/a|content://target/a"
        ExtractStateStore.putFingerprint(context, key, "100@1")
        ExtractStateStore.putFingerprint(context, key, "200@2")
        assertEquals("200@2", ExtractStateStore.fingerprint(context, key))
    }

    @Test
    fun clearRemovesAllRecords() {
        ExtractStateStore.putFingerprint(context, "k1", "v1")
        ExtractStateStore.putFingerprint(context, "k2", "v2")
        ExtractStateStore.clear(context)
        assertNull(ExtractStateStore.fingerprint(context, "k1"))
        assertNull(ExtractStateStore.fingerprint(context, "k2"))
    }

    @Test
    fun recordsAreIsolatedPerKey() {
        ExtractStateStore.putFingerprint(context, "keyA", "a")
        ExtractStateStore.putFingerprint(context, "keyB", "b")
        assertEquals("a", ExtractStateStore.fingerprint(context, "keyA"))
        assertEquals("b", ExtractStateStore.fingerprint(context, "keyB"))
    }
}
