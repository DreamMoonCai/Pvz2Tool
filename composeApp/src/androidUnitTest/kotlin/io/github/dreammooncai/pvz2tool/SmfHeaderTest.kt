package io.github.dreammooncai.pvz2tool

import io.github.dreammooncai.pvz2tool.pop.core.rsb.Rsb
import io.github.dreammooncai.util.SmfHeader
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [SmfHeader] 单元测试：SMF 压缩容器头的解析（**纯 JVM 逻辑，无需 Android API**）。
 *
 * 这层逻辑决定了「APK 内 SMF 更新后是否会被重新解压」——解压判据需要把 APK 内
 * 压缩态文件的长度**归一化到解压后域**（取头里的原始长度）才能与本地明文比较。
 * 因此这里对魔数、字节序、非法输入逐项覆盖。
 *
 * 运行：./gradlew :composeApp:testDebugUnitTest --tests "*SmfHeaderTest*"
 */
class SmfHeaderTest {

    /** 按引擎写入顺序（`Endian.Small` 小端）拼出 SMF 头。 */
    private fun buildHeader(magic: Int, originalLength: Int): ByteArray {
        val buf = ByteArray(SmfHeader.HEADER_SIZE)
        putLittleEndian(buf, 0, magic)
        putLittleEndian(buf, 4, originalLength)
        return buf
    }

    private fun putLittleEndian(buf: ByteArray, offset: Int, value: Int) {
        buf[offset] = value.toByte()
        buf[offset + 1] = (value shr 8).toByte()
        buf[offset + 2] = (value shr 16).toByte()
        buf[offset + 3] = (value shr 24).toByte()
    }

    @Test
    fun smfContainerExposesOriginalLength() {
        val header = buildHeader(Rsb.SMF_MAGIC, 123_456)
        assertTrue(SmfHeader.hasSmfMagic(header))
        assertEquals(123_456L, SmfHeader.readContentSize(header))
    }

    @Test
    fun plainRsbBytesAreNotSmf() {
        // RSB 明文（未做 SMF 压缩优化）不会带 SMF 魔数
        val plain = ByteArray(16)
        assertFalse(SmfHeader.hasSmfMagic(plain))
        assertEquals(-1L, SmfHeader.readContentSize(plain))
    }

    @Test
    fun shortInputIsRejected() {
        val tooShort = ByteArray(SmfHeader.HEADER_SIZE - 1)
        assertFalse(SmfHeader.hasSmfMagic(tooShort))
        assertEquals(-1L, SmfHeader.readContentSize(tooShort))
    }

    @Test
    fun zeroOrNegativeOriginalLengthIsRejected() {
        assertEquals(-1L, SmfHeader.readContentSize(buildHeader(Rsb.SMF_MAGIC, 0)))
        // -1 -> 0xFFFFFFFF：写入端不可能产生，属损坏头部，必须拒绝而不是解释成 4294967295
        assertEquals(-1L, SmfHeader.readContentSize(buildHeader(Rsb.SMF_MAGIC, -1)))
        // 0x80000000 超出 Int.MAX_VALUE，同样拒绝
        assertEquals(-1L, SmfHeader.readContentSize(buildHeader(Rsb.SMF_MAGIC, Int.MIN_VALUE)))
    }

    @Test
    fun maxIntLengthIsAccepted() {
        assertEquals(
            Int.MAX_VALUE.toLong(),
            SmfHeader.readContentSize(buildHeader(Rsb.SMF_MAGIC, Int.MAX_VALUE))
        )
    }

    @Test
    fun streamOverloadConsumesOnlyHeader() {
        val payload = byteArrayOf(0x78, 0x9C.toByte(), 0x01, 0x02, 0x03)
        val stream = (buildHeader(Rsb.SMF_MAGIC, 4_096) + payload).inputStream()
        assertEquals(4_096L, SmfHeader.readContentSize(stream))
        // 头部已被消费，剩余应恰为 zlib 数据
        assertTrue(payload.contentEquals(stream.readBytes()))
    }

    @Test
    fun streamShorterThanHeaderReturnsMinusOne() {
        assertEquals(-1L, SmfHeader.readContentSize(byteArrayOf(0xD4.toByte(), 0xFE.toByte()).inputStream()))
    }

    @Test
    fun emptyStreamReturnsMinusOne() {
        assertEquals(-1L, SmfHeader.readContentSize(ByteArray(0).inputStream()))
    }
}
