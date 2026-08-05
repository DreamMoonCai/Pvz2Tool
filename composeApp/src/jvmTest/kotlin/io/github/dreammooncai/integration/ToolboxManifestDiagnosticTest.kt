package io.github.dreammooncai.integration

import com.reandroid.apk.ApkModule
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import com.reandroid.arsc.chunk.xml.ResXmlElement
import com.reandroid.arsc.chunk.xml.ResXmlAttribute
import com.reandroid.arsc.value.ValueType
import java.io.File
import kotlin.test.Test

/**
 * 诊断：加载【已产出的】集成 APK，校验 manifest 里所有 0x66 引用 resids 是否在产物 0x66 包中真实存在。
 * 不调用 serializeToXml（那一步在重载后引用无法解析成名称会 NPE，但产物二进制本身没问题）。
 * 产物不存在时跳过。
 */
class ToolboxManifestDiagnosticTest {

    private val produced = File(
        "/Users/macbookpro/AndroidStudioProjects/Pvz2Tool/composeApp/build/integrator-out/原版_pvz2tool.apk"
    )

    @Test
    fun `诊断 0x66 引用 resids 是否真实存在`() {
        if (!produced.exists()) {
            println("[诊断] 产物不存在，跳过：${produced.absolutePath}")
            return
        }
        val apk = ApkModule.loadApkFile(produced)
        val manifest: AndroidManifestBlock = apk.androidManifest
        val root = manifest.getManifestElement()

        var refCount = 0
        val bad = mutableListOf<String>()
        val ok66 = mutableListOf<String>()

        fun walk(el: ResXmlElement) {
            val attrs: Iterator<ResXmlAttribute> = el.getAttributes()
            while (attrs.hasNext()) {
                val a = attrs.next()
                if (a.getValueType() == ValueType.REFERENCE) {
                    refCount++
                    val resid = a.getData()
                    val pkg = (resid ushr 24) and 0xFF
                    if (pkg == 0x66) {
                        // 校验该 resid 是否在产物 0x66 包中存在
                        val entry = apk.tableBlock.getResource(resid)
                        val ok = entry != null
                        val info = "0x%08X type=0x%02X entry=0x%04X -> %s".format(
                            resid, (resid ushr 16) and 0xFF, resid and 0xFFFF,
                            entry?.name ?: "NULL"
                        )
                        if (ok) ok66.add(info) else bad.add(info)
                    }
                }
            }
            val children: Iterator<ResXmlElement> = el.getElements()
            while (children.hasNext()) walk(children.next())
        }
        root?.let { walk(it) }

        println("[诊断] 引用属性总数=$refCount")
        println("[诊断] 0x66 引用中【有效】${ok66.size} 个：")
        ok66.forEach { println("   OK: $it") }
        println("[诊断] 0x66 引用中【无效】${bad.size} 个：")
        bad.forEach { println("   BAD: $it") }
        println("[诊断] 完成")
    }
}
