package io.github.dreammooncai.pvz2tool.diagnostic

import android.content.ClipData
import android.content.Context
import android.content.res.Resources
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import com.reandroid.arsc.chunk.xml.ResXmlAttribute
import com.reandroid.arsc.chunk.xml.ResXmlElement
import com.reandroid.arsc.value.ValueType
import io.github.dreammooncai.pvz2tool.ui.dialog.PvzDialogCard
import io.github.dreammooncai.pvz2tool.view.PvzGreenButton
import io.github.dreammooncai.pvz2tool.view.PvzRedButton
import io.github.dreammooncai.pvz2tool.view.PvzRichText
import io.github.dreammooncai.pvz2tool.view.PvzTextOliveStyle
import io.github.dreammooncai.pvz2tool.view.PvzTextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.zip.ZipFile

/**
 * 启动自检：本包 `AndroidManifest.xml` 里是否写入了**当前资源表中不存在**的资源引用。
 *
 * ## 为什么需要它
 * 「手动替换」集成的包（把工具箱的资源/manifest 片段手工搬进目标 APK）极易踩这个坑：
 * manifest 片段是从**另一个版本**的集成包里抄来的，里面是当时那张资源表的**数字 id**
 * （如 `android:theme="@0x6612028f"`），而塞进去的 `resources.arsc` 是新表
 * （`style/Theme.DreamPvzApp` 实际是 `0x6611012f`）。
 *
 * 于是：
 *  - `android:theme` 悬空 → 系统回退默认主题 → Activity 顶部冒出 **ActionBar 黑色标题栏**
 *    （用户反馈的「顶部黑色矩形条」，显示应用名），且标题栏占位把画面整体压下去、点击坐标错位，
 *    表现成「点画面设置卡死」。
 *  - `provider` 的 `android.support.FILE_PROVIDER_PATHS`（`android:resource="@0x66140000"`）悬空
 *    → FileProvider 解析失败 → 导入存档 / 分享文件报错。
 *
 * ## 判定方式
 * 不依赖任何版本号或写死 id：直接读**本 APK 自己的** manifest（二进制 XML，用 ARSCLib 解析），
 * 遍历每一个属性，凡是 `ValueType.REFERENCE` 的 id 都用 [Resources.getResourceName] 试探一次，
 * 探不到（抛 `Resources.NotFoundException`）就是悬空引用。这样无论用户怎么改包、id 怎么变都能查到。
 *
 * ## 只提示、不修复
 * manifest 已经是编译后的二进制 XML 且参与签名，运行时无法自愈；只能把「改哪个 Activity、
 * 哪个属性、改成什么」讲清楚，交给用户用 MT 管理器 / Apktool 改完重签，或改用一键集成重做。
 */
object ManifestRefCheck {

    private const val TAG = "ManifestRefCheck"

    /** 工具箱自身包名（合并后被放进 0x66 共享包，名字仍然是这个） */
    private const val TOOL_PKG_NAME = "io.github.dreammooncai.pvz2tool"

    /**
     * 工具箱资源的包 id。
     *
     * 🔴 只查这个包的引用：0x01 是 Android 框架资源（游戏自己的 @android: 引用与我们无关，
     * 而且框架里被删掉的 style 会让系统按默认主题回退，`Resources.getResourceName` 在真机上
     * 也可能查得到，混进来只会制造噪音）；目标游戏自身的 0x7f 引用同理。
     * 一键集成/手动替换写进来的工具箱引用恒为 0x66（[ToolboxApkMerger.OUR_PKG_ID]），
     * 所以这个过滤既精准又不会漏。
     */
    private const val OUR_PKG_ID = 0x66

    /** 工具箱沉浸式主题的资源名，用于给出「改成什么」的具体建议 */
    private const val OUR_THEME_STYLE_NAME = "Theme.DreamPvzApp"

    /** FileProvider 路径配置资源名 */
    private const val FILE_PATHS_XML_NAME = "file_paths"

    /** 单条悬空引用 */
    data class Issue(
        /** 元素位置，如 `application > activity(io.github.dreammooncai.pvz2tool.Pvz2InitializeActivity)` */
        val elementPath: String,
        /** 元素标签名（activity / provider / meta-data / application …） */
        val elementTag: String,
        /** 属性名，如 `android:theme` */
        val attribute: String,
        /** 写进 manifest 的错误 id，如 `@0x6612028f` */
        val rawValue: String,
        /** 建议改成什么（多行） */
        val hint: String,
        /** 该属性悬空的后果 */
        val impact: String,
    ) {
        /** 供复制/落日志的一行文本 */
        fun toLine(): String = buildString {
            append("位置：").append(elementPath).append('\n')
            append("属性：").append(attribute).append(" = ").append(rawValue).append("（该资源不存在）\n")
            append("后果：").append(impact).append('\n')
            append("改为：").append(hint)
        }
    }

    /**
     * 执行自检；任何异常都吞掉并返回空列表（这是启动诊断，绝不允许它把应用搞崩）。
     */
    fun check(context: Context): List<Issue> {
        val issues = mutableListOf<Issue>()
        try {
            val apkPath = context.applicationInfo?.sourceDir ?: return emptyList()
            ZipFile(apkPath).use { zip ->
                val entry = zip.getEntry("AndroidManifest.xml") ?: return emptyList()
                val doc = AndroidManifestBlock()
                zip.getInputStream(entry).use { doc.readBytes(it) }
                val root = doc.getManifestElement() ?: return emptyList()
                visit(context, root, "", issues)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "检查 AndroidManifest.xml 资源引用失败（已忽略）", t)
            return emptyList()
        }
        if (issues.isNotEmpty()) {
            Log.w(TAG, "发现 ${issues.size} 处悬空资源引用：\n" + issues.joinToString("\n\n") { it.toLine() })
        }
        return issues
    }

    private fun visit(ctx: Context, el: ResXmlElement, parentPath: String, out: MutableList<Issue>) {
        val label = labelOf(el)
        val path = if (parentPath.isEmpty()) label else "$parentPath > $label"

        val attrs = el.getAttributes()
        while (attrs.hasNext()) {
            val a = attrs.next()
            if (a.valueType != ValueType.REFERENCE) continue
            val id = a.data
            if (id == 0) continue
            // 只看工具箱自己的 0x66 引用（框架 0x01 / 游戏 0x7f 的引用不归我们管，见 OUR_PKG_ID 注释）
            if (((id ushr 24) and 0xFF) != OUR_PKG_ID) continue
            if (resourceExists(ctx, id)) continue
            out.add(buildIssue(ctx, path, el, a, id))
        }

        val children = el.getElements()
        while (children.hasNext()) visit(ctx, children.next(), path, out)
    }

    /** `name` = 元素本身名字，带上 `android:name` 便于用户一眼认出是哪个 Activity/Provider */
    private fun labelOf(el: ResXmlElement): String {
        val tag = el.name ?: "?"
        val ident = attrValue(el, "name")
        return if (ident.isNullOrBlank()) tag else "$tag($ident)"
    }

    private fun buildIssue(
        ctx: Context,
        path: String,
        el: ResXmlElement,
        attr: ResXmlAttribute,
        id: Int
    ): Issue {
        // 属性名在二进制 XML 的字符串池里是本地名（theme / resource / name），
        // 但 ARSCLib 若按文本 XML 解析过，可能带 "android:" 前缀 → 统一截掉，避免判据漏掉。
        val local = attr.getName()?.substringAfterLast(':') ?: "?"
        val rawValue = "@0x%08x".format(id)
        val tag = el.name ?: "?"

        return when {
            local == "theme" -> Issue(
                elementPath = path,
                elementTag = tag,
                attribute = "android:theme",
                rawValue = rawValue,
                hint = buildThemeHint(ctx),
                impact = "主题解析失败 → 系统回退默认主题，顶部出现 ActionBar 黑色标题栏（黑条），" +
                    "画面被挤压、点击位置错开。",
            )

            local == "resource" && isFilePathMetaData(el, path) -> Issue(
                elementPath = path,
                elementTag = tag,
                attribute = "android:resource",
                rawValue = rawValue,
                hint = buildFilePathsHint(ctx),
                impact = "FileProvider 读不到路径配置 → 导入存档、分享/发送文件会失败。",
            )

            else -> Issue(
                elementPath = path,
                elementTag = tag,
                attribute = "android:$local",
                rawValue = rawValue,
                hint = "按资源名改成当前包里的正确引用（如 @style/Xxx、@drawable/Xxx、@xml/Xxx），" +
                    "实在没用就直接删掉这条属性。",
                impact = "该属性引用的资源不存在，相关对象（Activity / Provider / 图标 / 标签等）" +
                    "会退回系统默认值或直接抛错。",
            )
        }
    }

    private fun buildThemeHint(ctx: Context): String {
        val real = findResourceId(ctx, "style", OUR_THEME_STYLE_NAME)
        val head = if (real != null) {
            "android:theme=\"@style/$OUR_THEME_STYLE_NAME\"（本包实际 id：0x%08x）".format(real)
        } else {
            "android:theme=\"@style/$OUR_THEME_STYLE_NAME\""
        }
        return head + "\n        不需要沉浸式主题就把这条 android:theme 整行删掉（退回游戏原主题，黑条必然消失）"
    }

    private fun buildFilePathsHint(ctx: Context): String {
        val real = findResourceId(ctx, "xml", FILE_PATHS_XML_NAME)
        return if (real != null) {
            "android:resource=\"@xml/$FILE_PATHS_XML_NAME\"（本包实际 id：0x%08x）".format(real)
        } else {
            "android:resource=\"@xml/$FILE_PATHS_XML_NAME\""
        }
    }

    /** meta-data 的 name 是 FileProvider 的路径配置键（androidx 与 support 两套都认） */
    private fun isFilePathMetaData(el: ResXmlElement, path: String): Boolean {
        if (el.name != "meta-data") return false
        val name = attrValue(el, "name") ?: return false
        return name.endsWith("FILE_PROVIDER_PATHS") || path.contains("FILE_PROVIDER_PATHS")
    }

    private fun attrValue(el: ResXmlElement, localName: String): String? {
        val it = el.getAttributes()
        while (it.hasNext()) {
            val a = it.next()
            if (a.getName() == localName) return a.getValueAsString()
        }
        return null
    }

    private fun resourceExists(ctx: Context, id: Int): Boolean = try {
        ctx.resources.getResourceName(id)
        true
    } catch (_: Resources.NotFoundException) {
        false
    } catch (_: Throwable) {
        // 资源管理器异常（极少数 ROM）时不要误报
        true
    }

    /**
     * 按名字查当前包里的真实 id。
     * 合并包会把工具箱资源放进独立的 0x66 共享包，其包名仍是 [TOOL_PKG_NAME]，
     * 所以两个包名都试一遍；查不到返回 null（此时建议里只给资源名，不再给数字 id）。
     */
    private fun findResourceId(ctx: Context, type: String, name: String): Int? {
        val pkgNames = listOf(ctx.packageName, TOOL_PKG_NAME).distinct()
        for (pkg in pkgNames) {
            try {
                val id = ctx.resources.getIdentifier(name, type, pkg)
                if (id != 0) return id
            } catch (_: Throwable) {
                // ignore
            }
        }
        return null
    }
}

// ==================================================================================
// Compose 提示层
// ==================================================================================

/**
 * 启动时挂一次自检，有问题就弹出一次性提示（本进程内只弹一次）。
 *
 * 放在工具箱主界面组合树里即可；检查在 IO 线程做，失败静默跳过。
 */
@Composable
fun ManifestRefWarningHost() {
    val context = LocalContext.current
    var issues by remember { mutableStateOf<List<ManifestRefCheck.Issue>>(emptyList()) }
    var dismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val found = withContext(Dispatchers.IO) { ManifestRefCheck.check(context) }
        if (found.isNotEmpty()) issues = found
    }

    if (issues.isNotEmpty() && !dismissed) {
        ManifestRefWarningDialog(issues = issues, onDismiss = { dismissed = true })
    }
}

/**
 * 悬空引用提示弹窗。
 *
 * 复用项目统一的 [PvzDialogCard]（奶黄底 + 三层绿框 + 绿色标题栏）和 PVZ 红/绿按钮，
 * 风格与其他弹窗一致。内容区高度由 `fillMaxHeight` 比例 + `weight` 限定为有限值，
 * 内部用 `verticalScroll` 滚动：屏幕小也能看全，且不会把无限高传给滚动容器。
 */
@Composable
fun ManifestRefWarningDialog(
    issues: List<ManifestRefCheck.Issue>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val detail = remember(issues) { issues.joinToString("\n\n") { it.toLine() } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            PvzDialogCard(
                title = "资源引用检查",
                modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.82f)
            ) {
                PvzRichText(
                    text = "AndroidManifest.xml 里写死的 @0x66 资源 id 在当前资源表中不存在" +
                        "（手动替换集成常见：manifest 片段抄自别的版本，资源表却是新的）。" +
                        "下面逐条列出位置、后果与改法。",
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    defaultStyle = PvzTextOliveStyle.copy(shadowColor = null),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
                Spacer(Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        issues.forEachIndexed { index, issue ->
                            PvzIssueBlock(index + 1, issue)
                        }
                        PvzRichText(
                            text = "改法（MT 管理器 / Apktool）：\n" +
                                "1. 打开 APK → 反编译 AndroidManifest.xml（MT 里长按选「反编译」看明文）\n" +
                                "2. 搜索 @0x66，按上面每条把数字 id 换成 @style/Theme.DreamPvzApp 这种名字形式，别用数字 id\n" +
                                "3. 保存 → 重新签名 → 安装\n" +
                                "4. 更省事：直接用工具箱「一键集成」重做，产物不会有这个问题",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            defaultStyle = PvzTextOliveStyle.copy(shadowColor = null),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PvzRedButton(
                        text = "复制详情",
                        modifier = Modifier.weight(1f).height(46.dp),
                        onClick = {
                            scope.launch {
                                clipboard.setClipEntry(
                                    ClipEntry(ClipData.newPlainText("manifest_ref_issues", detail))
                                )
                                Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    PvzGreenButton(
                        text = "我知道了",
                        modifier = Modifier.weight(1f).height(46.dp),
                        onClick = onDismiss
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun PvzIssueBlock(index: Int, issue: ManifestRefCheck.Issue) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE7DDB0), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFAA9A5F), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column {
            PvzRichText(
                text = "$index. ${issue.elementTag}",
                fontSize = 16.sp,
                defaultStyle = PvzTextStyle(Color(0xFF9E2A00), Color(0xFFFFE0C0)),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "位置：${issue.elementPath}",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF423F00)
            )
            Text(
                text = "属性：${issue.attribute} = ${issue.rawValue}  ← 资源不存在",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF9E2A00)
            )
            Text(
                text = "后果：${issue.impact}",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = Color(0xFF5B5410)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "改为：${issue.hint}",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF2E7D32)
            )
        }
    }
}
