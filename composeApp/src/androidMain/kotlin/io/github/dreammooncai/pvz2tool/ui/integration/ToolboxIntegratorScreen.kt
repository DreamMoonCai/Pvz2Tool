package io.github.dreammooncai.pvz2tool.ui.integration

import android.content.ClipData
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.documentfile.provider.DocumentFile
import androidx.core.content.FileProvider
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dreammooncai.integration.ToolboxApkMerger
import io.github.dreammooncai.integration.ToolboxApkMerger.DexStrategy
import io.github.dreammooncai.integration.ToolboxApkMerger.IntegrateReport
import io.github.dreammooncai.integration.ToolboxApkMerger.MergeResult
import io.github.dreammooncai.manager.FilePickerManager
import com.reandroid.apk.ApkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.dreammooncai.pvz2tool.DynamicSection
import io.github.dreammooncai.pvz2tool.FloatingWindowItem
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.Pvz2ToolConfig
import io.github.dreammooncai.pvz2tool.Pvz2ToolConfigGameDisplay
import io.github.dreammooncai.pvz2tool.Pvz2ToolConfigOperation
import io.github.dreammooncai.pvz2tool.Pvz2ToolConfigUISave
import io.github.dreammooncai.pvz2tool.Pvz2ToolConfigAnnouncement
import io.github.dreammooncai.pvz2tool.ScheduleDef
import io.github.dreammooncai.pvz2tool.Pvz2ToolSimpleConfig
import io.github.dreammooncai.pvz2tool.SectionItem
import io.github.dreammooncai.pvz2tool.SectionType
import io.github.dreammooncai.pvz2tool.TopBarIconItem
import io.github.dreammooncai.pvz2tool.VersionDef
import io.github.dreammooncai.pvz2tool.icon.ArrowRight
import io.github.dreammooncai.pvz2tool.icon.ArrowRightPress
import io.github.dreammooncai.pvz2tool.icon.Hook
import io.github.dreammooncai.pvz2tool.icon.HookSelect
import io.github.dreammooncai.pvz2tool.icon.Pvz2Icon
import io.github.dreammooncai.pvz2tool.topRoundedBorder
import io.github.dreammooncai.pvz2tool.ui.dialog.PvzDialogCard
import io.github.dreammooncai.pvz2tool.ui.main.Pvz2MainScreen
import io.github.dreammooncai.pvz2tool.view.AsyncImageFromAssets
import io.github.dreammooncai.pvz2tool.ui.dialog.JsLoadingDialog
import io.github.dreammooncai.pvz2tool.ui.dialog.JsProgressDialog
import io.github.dreammooncai.pvz2tool.ui.dialog.JsUiManager
import io.github.dreammooncai.pvz2tool.ui.dialog.PvzStyledDialog
import io.github.dreammooncai.pvz2tool.view.ImageSvgButton
import io.github.dreammooncai.pvz2tool.view.PvzBlueButton
import io.github.dreammooncai.pvz2tool.view.PvzCollapsiblePanelTheme
import io.github.dreammooncai.pvz2tool.view.PvzGreenButton
import io.github.dreammooncai.pvz2tool.view.PvzRedButton
import android.content.Context
import android.content.Intent
import coil3.compose.AsyncImage
import io.github.dreammooncai.pvz2tool.R
import io.github.dreammooncai.pvz2tool.view.PvzSimpleCardBrown
import io.github.dreammooncai.pvz2tool.view.PvzSimpleCardGreen
import io.github.dreammooncai.pvz2tool.view.PvzRichText
import io.github.dreammooncai.pvz2tool.view.PvzTextOliveStyle
import io.github.dreammooncai.pvz2tool.view.PvzTextStyle
import io.github.dreammooncai.pvz2tool.view.PvzTextWhiteStyle
import com.charleskorn.kaml.MultiLineStringStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlScalar
import android.net.Uri
import io.github.dreammooncai.pvz2tool.ui.dialog.AssetExtractorHolder
import kotlinx.coroutines.launch
import java.io.File

// ── PVZ 主题色板（集中定义，避免在列表中散落大量魔数） ──────────
private val PvzGreen = Color(0xFF689F38)        // PVZ 主绿（边框/文字）
private val PvzGreenSurface = Color(0xFFE8F5D0) // 浅绿面（选中态/卡片底）
private val PvzGreenBright = Color(0xFF8ED229)  // 高亮绿（选中态）
private val PvzBorderBrown = Color(0xFFAA9A5F)  // 卡片描边棕
private val PvzCreamCard = Color(0xFFF0ECD0)    // 奶油卡片底
private val PvzCream = Color(0xFFFCF9E8)        // 奶油面

// 列表中单条 item 的奶油色圆角卡片容器（统一边框/内边距），替代散落的重复布局
@Composable
private fun PvzItemCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PvzCreamCard)
            .border(1.dp, PvzBorderBrown, RoundedCornerShape(8.dp))
            .padding(10.dp),
        content = content
    )
}

// 打包完成后：把合并产物 APK 分享到其他软件（复制进 cacheDir 走 FileProvider，避免裸 file:// 在 7.0+ 被拦）
private fun shareMergedApk(context: Context, apk: File) {
    try {
        val shareFile = File(context.cacheDir, "pvz2tool_share_${System.currentTimeMillis()}.apk")
        apk.inputStream().use { input -> shareFile.outputStream().use { input.copyTo(it) } }
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, shareFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享 APK"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "分享失败：${e.message}", Toast.LENGTH_LONG).show()
    }
}

// 打包完成后：把合并产物 APK 导出到本地（通过 SAF 选择目标目录）
private fun exportMergedApkToLocal(context: Context, filePickerManager: FilePickerManager, apk: File) {
    filePickerManager.launch(isDirectory = true, fileMimeType = "*/*") { uri, doc ->
        val dir = doc?.takeIf { it.isDirectory }
        if (uri != null && dir != null) {
            try {
                val name = "${apk.nameWithoutExtension}_${System.currentTimeMillis()}.apk"
                val target = dir.createFile("application/vnd.android.package-archive", name)
                if (target != null) {
                    context.contentResolver.openOutputStream(target.uri)?.use { out ->
                        apk.inputStream().use { it.copyTo(out) }
                    }
                    Toast.makeText(context, "已导出到所选目录：$name", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "导出失败：目标目录不可写", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "导出失败：${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "未选择有效目录", Toast.LENGTH_SHORT).show()
        }
    }
}

// ── 子页面数据模型 ──────────────────────────────────────────────

data class AnnouncementDraft(val title: String = "", val content: String = "")

data class ScheduleDraft(
    val id: String = "",
    val name: String = "",
    val cron: String = "",
    val jsScript: String = "",
    val jsPath: String = "",
    val enabled: Boolean = true,
)

data class FwItemDraft(
    val id: String = "",
    val name: String = "",
    val buttonText: String = "",
    val buttonColor: String = "blue",
    val jsScript: String = "",
    val jsPath: String = "",
    val isShowFromJs: String = "",
    val isShowFromJsPath: String = "",
    val desc: String = "",
    val icon: String = "",
    val smfList: List<String> = emptyList(),
)

data class TbiItemDraft(
    val id: String = "",
    val icon: String = "",
    val iconPress: String = "",
    val contentDescription: String = "",
    val jsScript: String = "",
    val jsPath: String = "",
    val isShowFromJs: String = "",
    val isShowFromJsPath: String = "",
    val pressSound: String = "",
    val releaseSound: String = "",
    val smfList: List<String> = emptyList(),
)

// ── 列表排序辅助 ─────────────────────────────────────────
// 列表元素移动到新位置（保序），用于集成器列表的上移 / 下移 / 置顶 / 置底。
// 越界或同位置返回原列表；沿用本项目「拷贝-修改-整体回写」的列表编辑范式。
private fun <T> List<T>.moveTo(from: Int, to: Int): List<T> {
    if (from == to || from !in indices || to !in indices) return this
    return toMutableList().also { it.add(to, it.removeAt(from)) }
}

// 上移 / 下移 / 置顶 / 置底 按钮组（位于边界时对应按钮自动禁用）
@Composable
private fun <T> ReorderButtons(
    list: List<T>,
    index: Int,
    onUpdate: (List<T>) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier, Arrangement.spacedBy(4.dp, Alignment.End), Alignment.CenterVertically) {
        PvzBlueButton("上移", Modifier.height(32.dp), enabled = index > 0) {
            onUpdate(list.moveTo(index, index - 1))
        }
        PvzBlueButton("下移", Modifier.height(32.dp), enabled = index < list.lastIndex) {
            onUpdate(list.moveTo(index, index + 1))
        }
        PvzBlueButton("置顶", Modifier.height(32.dp), enabled = index > 0) {
            onUpdate(list.moveTo(index, 0))
        }
        PvzBlueButton("置底", Modifier.height(32.dp), enabled = index < list.lastIndex) {
            onUpdate(list.moveTo(index, list.lastIndex))
        }
    }
}

// ── 版本 / 栏目 / 功能项 数据模型 ────────────────────────────

data class VersionDraft(
    val id: String = "",
    val name: String = "",
    val desc: String = "",
    val icon: String = "",
    val default: Boolean = false,
    val assetPath: String = "",
    val baseAssetPath: String = "",
    val forceOverride: Boolean = false,
    val enterGameScript: String = "",
    val enterGamePath: String = ""
)

data class SectionDraft(
    val id: String = "",
    val title: String = "",
    val theme: String = "BROWN",
    val isExpanded: Boolean = false,
    val confirmButtonText: String = "",
    val visibleOnVersionIds: String = "",       // 逗号分隔，空=全部可见
    val targetPath: String = "",
    val addItems: Boolean = false,
    val descriptionContent: String = "",
    val jsScript: String = "",
    val jsPath: String = "",
    val items: List<SectionItemDraft> = emptyList()
)

data class SectionItemDraft(
    val id: String = "",
    val type: String = "DESCRIPTION",           // RADIO/CHECKBOX/SLIDER/BUTTON/INPUT/INFO/DESCRIPTION
    val name: String = "",
    val desc: String = "",
    val icon: String = "",
    val assetPath: String = "",
    val jsScript: String = "",
    val jsPath: String = "",
    val isShowFromJs: String = "",
    val isShowFromJsPath: String = "",
    // RADIO
    val groupId: String = "root",
    val radioDefault: Boolean = false,
    // CHECKBOX
    val checkboxDefault: Boolean = false,
    val smfList: String = "",                   // 逗号分隔
    // SLIDER
    val minValue: String = "0",
    val maxValue: String = "100",
    val defaultValue: String = "",
    val step: String = "1",
    val valueSuffix: String = "",
    // BUTTON
    val buttonText: String = "",
    val buttonColor: String = "blue",
    // INPUT
    val placeholder: String = "",
    val inputDefault: String = "",
    // INFO
    val infoValue: String = ""
)

// ── 从 dream.yml 读取默认值 ───────────────────────────────

// 读取目标 APK 旧版 dream.yml 时用「非严格」解析：遇到新版本已改名/删除的字段直接忽略（取默认值），
// 而非抛 UnknownPropertyException 导致整套向导默认值回退为内置默认、buildYamlFromWizard 丢弃向导编辑。
// 结构错误（语法/缩进）仍会抛异常并被 runCatching 捕获，不影响健壮性。
private val lenientYaml = Yaml(configuration = YamlConfiguration(strictMode = false, encodeDefaults = false, multiLineStringStyle = MultiLineStringStyle.Literal))

fun loadDefaultsFromDreamYml(raw: String): DreamDefaults {
    val config = runCatching {
        lenientYaml.decodeFromString(Pvz2ToolConfig.serializer(), raw)
    }.getOrNull()
    return DreamDefaults(
        smfDirectory = config?.smfDirectory ?: "files/",
        versions = config?.versions?.map { v ->
            VersionDraft(v.id, v.name, v.desc, v.icon ?: "", v.default,
                v.assetPath ?: "", v.baseAssetPath ?: "", v.forceOverride,
                v.enterGameScript ?: "", v.enterGamePath ?: "")
        } ?: emptyList(),
        sections = config?.sections?.map { s ->
            SectionDraft(s.id, s.title, s.theme.name, s.isExpanded,
                s.confirmButtonText ?: "", s.visibleOnVersionIds.joinToString(","),
                s.targetPath ?: "", s.addItems, s.descriptionContent,
                s.jsScript ?: "", s.jsPath ?: "",
                s.items.map { i ->
                    SectionItemDraft(
                        id = i.id, type = i.type.name, name = i.name ?: "", desc = i.desc ?: "",
                        icon = i.icon ?: "", assetPath = i.assetPath ?: "",
                        jsScript = i.jsScript ?: "", jsPath = i.jsPath ?: "",
                        isShowFromJs = i.isShowFromJs ?: "", isShowFromJsPath = i.isShowFromJsPath ?: "",
                        groupId = i.groupId, radioDefault = i.default,
                        checkboxDefault = i.default, smfList = i.smfList.joinToString(","),
                        minValue = i.minValue.toString(), maxValue = i.maxValue.toString(),
                        defaultValue = i.defaultValue?.toString() ?: "", step = i.step.toString(),
                        valueSuffix = i.valueSuffix ?: "",
                        buttonText = i.buttonText ?: "", buttonColor = i.buttonColor ?: "blue",
                        placeholder = i.placeholder ?: "", inputDefault = i.inputDefault ?: "",
                        infoValue = i.infoValue ?: ""
                    )
                }
            )
        } ?: emptyList(),
        announcements = config?.announcement?.map { AnnouncementDraft(it.title, it.content) } ?: emptyList(),
        isExpandedVersions = config?.isExpandedVersions ?: false,
        versionsTheme = config?.versionsTheme?.name ?: "BROWN",
        baseAssetPath = config?.baseAssetPath ?: "",
        // ui.assets
        bgImage = config?.ui?.assets?.background ?: "",
        isUseSolidColorBg = config?.ui?.assets?.isUseSolidColorBackground ?: true,
        bgMusic = config?.ui?.assets?.backgroundMusic ?: "",
        isPlayBgMusic = config?.ui?.assets?.isPlayBackgroundMusic ?: true,
        sideBgImage = config?.ui?.assets?.sideBgImage ?: "",
        floatingBallIcon = config?.ui?.assets?.floatingBallIcon ?: "",
        // ui.settings
        showFloatingWindowLabel = config?.ui?.settings?.showFloatingWindow ?: "是否开启悬浮窗",
        isShowFloatingWindowDefault = config?.ui?.settings?.isShowFloatingWindow ?: true,
        fwEmptyTip = config?.ui?.floatingWindow?.emptyTip ?: "",
        fwAllHiddenTip = config?.ui?.floatingWindow?.allHiddenTip ?: "",
        exitConfirmTitle = config?.ui?.settings?.exitConfirmTitle ?: "",
        exitConfirmMessage = config?.ui?.settings?.exitConfirmMessage ?: "",
        isUseExitConfirm = config?.ui?.settings?.isUseExitConfirm ?: true,
        exitConfirmButtonText = config?.ui?.settings?.exitConfirmButtonText ?: "",
        floatingExitConfirmTitle = config?.ui?.settings?.floatingExitConfirmTitle ?: "",
        floatingExitConfirmMessage = config?.ui?.settings?.floatingExitConfirmMessage ?: "",
        floatingExitConfirmButtonText = config?.ui?.settings?.floatingExitConfirmButtonText ?: "",
        // ui.floatingWindow.items
        fwItems = config?.ui?.floatingWindow?.items?.map { fw ->
            FwItemDraft(fw.id, fw.name ?: "", fw.buttonText ?: "",
                fw.buttonColor ?: "blue", fw.jsScript ?: "", fw.jsPath ?: "",
                fw.isShowFromJs ?: "", fw.isShowFromJsPath ?: "",
                fw.desc ?: "", fw.icon ?: "")
        } ?: emptyList(),
        // ui.topBarIcons.items
        tbiItems = config?.ui?.topBarIcons?.items?.map { tbi ->
            TbiItemDraft(tbi.id, tbi.icon, tbi.iconPress ?: "", tbi.contentDescription ?: "",
                tbi.jsScript ?: "", tbi.jsPath ?: "", tbi.isShowFromJs ?: "",
                tbi.isShowFromJsPath ?: "", tbi.pressSound ?: "", tbi.releaseSound ?: "")
        } ?: emptyList(),
        // UI 高级文本默认值
        uiVersionLabel = config?.ui?.versionLabel ?: "版本号：",
        uiUiVersion = config?.ui?.uiVersion ?: "V2.5.1",
        uiAuthorInfo = config?.ui?.authorInfo ?: "作者：{{green-shadow:松间烬雪}}",
        uiTutorial = config?.ui?.tutorial ?: "欢迎使用本应用！",
        uiNoValidDirTip = config?.ui?.noValidDirTip ?: "未选择有效目录",
        uiTitleTopAppBar = config?.ui?.title?.topAppBar ?: "{{icon|width=200|height=40:egame_sdk_game_logo.png}}·迷宫拓展版",
        uiTitleAbout = config?.ui?.title?.about ?: "关于版本",
        uiTitleCoreFunction = config?.ui?.title?.coreFunction ?: "核心功能",
        uiTitleVersionManage = config?.ui?.title?.versionManage ?: "版本管理",
        uiBtnEnterGame = config?.ui?.button?.enterGame ?: "进入游戏",
        uiBtnTutorial = config?.ui?.button?.tutorial ?: "教程",
        uiBtnResetData = config?.ui?.button?.resetData ?: "重置数据包",
        uiBtnShowFW = config?.ui?.button?.showFloatingWindow ?: "工具悬窗",
        uiBtnConfirmVersion = config?.ui?.button?.confirmVersion ?: "选定版本",
        uiLogPanelTitle = config?.ui?.log?.panelTitle ?: "JS 日志",
        uiDialogConfirm = config?.ui?.dialog?.confirm ?: "确定",
        uiDialogCancel = config?.ui?.dialog?.cancel ?: "取消",
        uiWelcomeGreeting = config?.ui?.welcome?.greetingTemplate ?: "欢迎您，%s",
        // Extractor
        uiExDialogTitle = config?.ui?.extractor?.dialogTitle ?: "戴夫的工具箱 | 资源更新",
        uiExInitLoadTip = config?.ui?.extractor?.initialLoadingProgressTip ?: "戴夫正在清点物资清单...",
        uiExInitProgTip = config?.ui?.extractor?.initialProgressTip ?: "戴夫检测到新的版本波动啦，准备更新物资...",
        uiExNoNeedTip = config?.ui?.extractor?.noNeedExtractTip ?: "戴夫检查了工具箱，暂时没有新物资需要更新~",
        uiExSingleFileTip = config?.ui?.extractor?.singleFileProcessingTip ?: "戴夫正在手忙脚乱整理物资：",
        uiExMultiFileTip = config?.ui?.extractor?.multiFileProcessingTip ?: "戴夫正在手忙脚乱整理%d个物资：",
        uiExWaitingTip = config?.ui?.extractor?.waitingTip ?: "戴夫正在整理物资，旅途中稍等片刻~",
        uiExCompleteTip = config?.ui?.extractor?.extractCompleteTip ?: "物资更新完毕！\n准备好再次迎战僵尸了吗，玩家！\n【%s】",
        uiExFailPrefix = config?.ui?.extractor?.extractFailTipPrefix ?: "糟糕！戴夫的工具箱出问题了：",
        uiExSkipPrefix = config?.ui?.extractor?.fileSkipTipPrefix ?: "戴夫检查到「%s」无需更新，跳过啦~",
        uiExContinueBtn = config?.ui?.extractor?.continueButtonText ?: "继续物资准备",
        uiExCompleteBtn = config?.ui?.extractor?.completeButtonText ?: "重返战场",
        uiExToastErr = config?.ui?.extractor?.toastErrorPrefix ?: "戴夫的小提示：更新失败啦 → ",
        // Sounds
        uiSndSwitchPress = config?.ui?.sounds?.switchClickPress ?: "ui_switch_click_press.wav",
        uiSndSwitchRelease = config?.ui?.sounds?.switchClickRelease ?: "ui_switch_click_release.wav",
        uiSndBtnPress = config?.ui?.sounds?.buttonClickPress ?: "ui_button_click_press.wav",
        uiSndBtnRelease = config?.ui?.sounds?.buttonClickRelease ?: "ui_button_click_release.wav",
        uiSndSettingsPress = config?.ui?.sounds?.buttonSettingsPress ?: "ui_button_settings_press.wav",
        uiSndSettingsRelease = config?.ui?.sounds?.buttonSettingsRelease ?: "ui_button_settings_release.wav",
        uiSndXClosePress = config?.ui?.sounds?.buttonXClosePress ?: "ui_button_x_close_press.wav",
        uiSndXCloseRelease = config?.ui?.sounds?.buttonXCloseRelease ?: "ui_button_x_close_release.wav",
        uiSndPanelPress = config?.ui?.sounds?.collapsiblePanelPress ?: "ui_collapsible_panel_click_press.wav",
        uiSndPanelRelease = config?.ui?.sounds?.collapsiblePanelRelease ?: "ui_collapsible_panel_click_release.wav",
        // Button icons
        uiBtnEnterGameIcon = config?.ui?.button?.isEnterGameDefaultIcon ?: true,
        uiBtnTutorialIcon = config?.ui?.button?.isTutorialDefaultIcon ?: true,
        uiBtnResetDataIcon = config?.ui?.button?.isResetDataDefaultIcon ?: true,
        // Settings extra
        uiSetTitle = config?.ui?.settings?.title ?: "戴夫工具箱·设置",
        uiSetSolidBg = config?.ui?.settings?.solidBackgroundMode ?: "纯色背景模式",
        uiSetPlayMusic = config?.ui?.settings?.playBackgroundMusic ?: "播放背景音乐",
        uiSetImportSmf = config?.ui?.settings?.importSmfFile ?: "导入SMF文件",
        uiSetReload = config?.ui?.settings?.reloadConfig ?: "重新读取配置文件",
        uiSetResetSmf = config?.ui?.settings?.resetPacketDeepClearing ?: "重置数据包时删除smf目录",
        uiSetCustomDisplay = config?.ui?.settings?.customGameDisplay ?: "自定义游戏画面",
        uiSetDisplayTitle = config?.ui?.settings?.customGameDisplayTitle ?: "游戏画面设置",
        uiSetApplyBtn = config?.ui?.settings?.applyButtonText ?: "应 用",
        // Error/Log/Dialog/Welcome extras
        uiErrJsTitle = config?.ui?.error?.jsExecuteErrorTitle ?: "JS 执行出错",
        uiErrUnknown = config?.ui?.error?.unknownError ?: "未知错误",
        uiLogCopyDesc = config?.ui?.log?.copyLogDesc ?: "复制日志",
        uiLogClearDesc = config?.ui?.log?.clearLogDesc ?: "清空日志",
        uiLogNoLogText = config?.ui?.log?.noLogText ?: "暂无日志",
        uiLogPresetSaveLabel = config?.ui?.log?.presetSaveLabel ?: "预设存档",
        uiLogLocalSaveLabel = config?.ui?.log?.localSaveLabel ?: "本地存档",
        uiDlgDelSave = config?.ui?.dialog?.deleteSaveDesc ?: "删除存档",
        uiDlgEditUser = config?.ui?.dialog?.editUserNameDesc ?: "编辑用户名",
        uiDlgShareTitle = config?.ui?.dialog?.shareSaveChooserTitle ?: "分享 PVZ2 存档",
        uiDlgPackFail = config?.ui?.dialog?.sharePackFailedTip ?: "打包存档失败",
        uiDlgNoShare = config?.ui?.dialog?.noShareableSaveTip ?: "没有可分享的本地存档",
        uiWelcomeEditTitle = config?.ui?.welcome?.editUserNameTitle ?: "修改用户名",
        uiWelcomeEditHint = config?.ui?.welcome?.editUserNameHint ?: "请输入新的用户名",
        // 存档模块
        save = SaveDraft(
            presetConfirmTitle = config?.ui?.save?.presetConfirmTitle ?: "",
            presetConfirmMessage = config?.ui?.save?.presetConfirmMessage ?: "",
            deleteConfirmTitle = config?.ui?.save?.deleteConfirmTitle ?: "",
            deleteConfirmMessage = config?.ui?.save?.deleteConfirmMessage ?: "",
            coverConfirmTitle = config?.ui?.save?.coverConfirmTitle ?: "",
            coverConfirmMessage = config?.ui?.save?.coverConfirmMessage ?: "",
            deleteGameSaveConfirmTitle = config?.ui?.save?.deleteGameSaveConfirmTitle ?: "",
            deleteGameSaveConfirmMessage = config?.ui?.save?.deleteGameSaveConfirmMessage ?: "",
            saveInfoTitle = config?.ui?.save?.saveInfoTitle ?: "",
            saveNameLabel = config?.ui?.save?.saveNameLabel ?: "",
            saveDescLabel = config?.ui?.save?.saveDescLabel ?: "",
            cancelButton = config?.ui?.save?.cancelButton ?: "",
            confirmButton = config?.ui?.save?.confirmButton ?: "",
            shareButton = config?.ui?.save?.shareButton ?: "",
            exportButton = config?.ui?.save?.exportButton ?: "",
            importButton = config?.ui?.save?.importButton ?: "",
            backupButton = config?.ui?.save?.backupButton ?: "",
            coverLocalButton = config?.ui?.save?.coverLocalButton ?: "",
            deleteGameSaveButton = config?.ui?.save?.deleteGameSaveButton ?: "",
            coverPresetButton = config?.ui?.save?.coverPresetButton ?: "",
            saveNameEmptyTip = config?.ui?.save?.saveNameEmptyTip ?: "",
            noLocalSaveTip = config?.ui?.save?.noLocalSaveTip ?: "",
            selectLocalSaveTip = config?.ui?.save?.selectLocalSaveTip ?: "",
            backupSuccessTip = config?.ui?.save?.backupSuccessTip ?: "",
            backupFailTipPrefix = config?.ui?.save?.backupFailTipPrefix ?: "",
            exportSuccessTip = config?.ui?.save?.exportSuccessTip ?: "",
            exportFailTipPrefix = config?.ui?.save?.exportFailTipPrefix ?: "",
            importSuccessTip = config?.ui?.save?.importSuccessTip ?: "",
            importFailTipPrefix = config?.ui?.save?.importFailTipPrefix ?: "",
            deleteSuccessTip = config?.ui?.save?.deleteSuccessTip ?: "",
            deleteFailTipPrefix = config?.ui?.save?.deleteFailTipPrefix ?: "",
            coverSuccessTip = config?.ui?.save?.coverSuccessTip ?: "",
            coverFailTipPrefix = config?.ui?.save?.coverFailTipPrefix ?: "",
            deleteGameSaveSuccessTip = config?.ui?.save?.deleteGameSaveSuccessTip ?: "",
            deleteGameSaveFailTipPrefix = config?.ui?.save?.deleteGameSaveFailTipPrefix ?: "",
            defaultImportNamePrefix = config?.ui?.save?.defaultImportNamePrefix ?: "",
            defaultBackupDesc = config?.ui?.save?.defaultBackupDesc ?: "",
            defaultImportDesc = config?.ui?.save?.defaultImportDesc ?: "",
            exportOptionTitle = config?.ui?.save?.exportOptionTitle ?: "",
            exportToFolderOption = config?.ui?.save?.exportToFolderOption ?: "",
            shareAsPackageOption = config?.ui?.save?.shareAsPackageOption ?: "",
            gameSaveLabel = config?.ui?.save?.gameSaveLabel ?: "",
            gameSaveInfoTemplate = config?.ui?.save?.gameSaveInfoTemplate ?: "",
            gameSaveUnknownUser = config?.ui?.save?.gameSaveUnknownUser ?: "",
            gameSaveNotExistTip = config?.ui?.save?.gameSaveNotExistTip ?: "",
            retryButtonText = config?.ui?.save?.retryButtonText ?: "",
            opBackup = config?.ui?.save?.operation?.backup ?: "",
            opExport = config?.ui?.save?.operation?.export ?: "",
            opImport = config?.ui?.save?.operation?.import ?: "",
            opDelete = config?.ui?.save?.operation?.delete ?: "",
            opDeleteGameSave = config?.ui?.save?.operation?.deleteGameSave ?: "",
            opCover = config?.ui?.save?.operation?.cover ?: "",
            opSaveMeta = config?.ui?.save?.operation?.saveMeta ?: ""
        ),
        gameDisplay = config?.ui?.settings?.gameDisplay ?: Pvz2ToolConfigGameDisplay(),
        uiSetChangeProfile = config?.ui?.settings?.changeTheProfileReadLocation ?: "",
        uiSetShowNotUpdate = config?.ui?.settings?.showNotUpdate ?: "",
        uiSetExitConfirm = config?.ui?.settings?.exitConfirm ?: "",
        uiSndSwitchClick = config?.ui?.sounds?.switchClick ?: "ui_switch_click.wav",
        cgVideoPath = config?.ui?.assets?.cgVideoPath ?: "opening.mp4",
        cgVideoPoster = config?.ui?.assets?.cgVideoPoster ?: "",
        cgVideoLoadTimeout = config?.ui?.assets?.cgVideoLoadTimeout?.toString() ?: "5000"
    )
}

/**
 * 更新模式：将「目标 APK 旧 dream.yml」深度合并到「当前工具箱内置模板 dream.yml」之上。
 * - 目标 yml 已有的字段 → 用目标值（保留用户/旧版的真实配置）。
 * - 目标 yml 缺失的字段（如新版新增功能）→ 用内置模板的默认值补齐。
 * 这样更新流程既能保留目标现有配置，又能让新版字段自动带上当前工具箱的默认配置（而非写死的兜底常量）。
 *
 * @param baseRaw 内置模板（当前工具箱 APK 自带），作为默认值来源
 * @param overrideRaw 目标 APK 旧 yml，作为优先覆盖来源
 */
private fun mergeDreamYml(baseRaw: String, overrideRaw: String): String {
    val baseNode = runCatching { lenientYaml.parseToYamlNode(baseRaw) }.getOrNull() ?: return overrideRaw
    val overrideNode = runCatching { lenientYaml.parseToYamlNode(overrideRaw) }.getOrNull() ?: return baseRaw
    val mergedNode = deepMergeYaml(baseNode, overrideNode)
    // 合并后重新解析为 Pvz2ToolConfig 再序列化，确保结构合法、缺省值正确回填
    val mergedConfig = runCatching {
        lenientYaml.decodeFromYamlNode(Pvz2ToolConfig.serializer(), mergedNode)
    }.getOrNull() ?: return overrideRaw
    return Yaml(configuration = YamlConfiguration(
        encodeDefaults = false, multiLineStringStyle = MultiLineStringStyle.Literal
    )).encodeToString(Pvz2ToolConfig.serializer(), mergedConfig)
}

/**
 * YAML 节点级深度合并：override 覆盖 base，仅在两侧均为映射(YamlMap)时递归合并子键；
 * 标量 / 列表 / null 一律以 override 为准（列表整体替换，符合「目标配置优先」语义）。
 *
 * 注意：YamlScalar 的 equals 包含 path，不能用「标量引用相等」去重同名 key，
 * 必须以标量文本内容(content)作为合并依据，否则同名 key 会被当成两个键导致重复键异常。
 */
private fun deepMergeYaml(base: YamlNode, override: YamlNode): YamlNode {
    if (base is YamlMap && override is YamlMap) {
        val merged = LinkedHashMap<String, YamlNode>()
        val keyScalar = LinkedHashMap<String, YamlScalar>()
        for ((k, v) in base.entries) {
            merged[k.content] = v
            keyScalar[k.content] = k
        }
        for ((k, v) in override.entries) {
            val key = k.content
            val existing = merged[key]
            merged[key] = if (existing != null) deepMergeYaml(existing, v) else v
            keyScalar[key] = k
        }
        val result = LinkedHashMap<YamlScalar, YamlNode>()
        for ((key, node) in merged) result[keyScalar[key]!!] = node
        return YamlMap(result, base.path)
    }
    return override
}

/**
 * 计算向导实际使用的 dream.yml 原始文本：
 * - 更新模式且已读取到目标 yml → 目标旧 yml 深度合并到内置模板之上（缺失字段取当前工具箱默认）。
 * - 其余情况 → 直接用内置模板。
 */
private fun effectiveDreamYmlRaw(sourceMode: String, targetDreamYmlRaw: String?, dreamYmlRaw: String): String {
    return if (sourceMode == "update" && !targetDreamYmlRaw.isNullOrEmpty()) {
        mergeDreamYml(dreamYmlRaw, targetDreamYmlRaw)
    } else dreamYmlRaw
}

data class DreamDefaults(
    val smfDirectory: String,
    val versions: List<VersionDraft>,
    val sections: List<SectionDraft>,
    val announcements: List<AnnouncementDraft>,
    val isExpandedVersions: Boolean,
    val versionsTheme: String,
    val baseAssetPath: String,
    val bgImage: String, val isUseSolidColorBg: Boolean,
    val bgMusic: String, val isPlayBgMusic: Boolean,
    val sideBgImage: String, val floatingBallIcon: String,
    val showFloatingWindowLabel: String, val isShowFloatingWindowDefault: Boolean,
    val fwEmptyTip: String, val fwAllHiddenTip: String,
    val exitConfirmTitle: String, val exitConfirmMessage: String, val isUseExitConfirm: Boolean,
    val exitConfirmButtonText: String, val floatingExitConfirmTitle: String,
    val floatingExitConfirmMessage: String, val floatingExitConfirmButtonText: String,
    val fwItems: List<FwItemDraft>, val tbiItems: List<TbiItemDraft>,
    // UI 高级文本
    val uiVersionLabel: String, val uiUiVersion: String, val uiAuthorInfo: String, val uiTutorial: String, val uiNoValidDirTip: String,
    val uiTitleTopAppBar: String, val uiTitleAbout: String, val uiTitleCoreFunction: String, val uiTitleVersionManage: String,
    val uiBtnEnterGame: String, val uiBtnTutorial: String, val uiBtnResetData: String, val uiBtnShowFW: String, val uiBtnConfirmVersion: String,
    val uiLogPanelTitle: String, val uiDialogConfirm: String, val uiDialogCancel: String, val uiWelcomeGreeting: String,
    // Extractor
    val uiExDialogTitle: String, val uiExInitLoadTip: String, val uiExInitProgTip: String, val uiExNoNeedTip: String,
    val uiExSingleFileTip: String, val uiExMultiFileTip: String, val uiExWaitingTip: String, val uiExCompleteTip: String,
    val uiExFailPrefix: String, val uiExSkipPrefix: String, val uiExContinueBtn: String, val uiExCompleteBtn: String, val uiExToastErr: String,
    // Sounds
    val uiSndSwitchPress: String, val uiSndSwitchRelease: String, val uiSndBtnPress: String, val uiSndBtnRelease: String,
    val uiSndSettingsPress: String, val uiSndSettingsRelease: String, val uiSndXClosePress: String, val uiSndXCloseRelease: String,
    val uiSndPanelPress: String, val uiSndPanelRelease: String,
    // Button icons
    val uiBtnEnterGameIcon: Boolean, val uiBtnTutorialIcon: Boolean, val uiBtnResetDataIcon: Boolean,
    // Settings extra
    val uiSetTitle: String, val uiSetSolidBg: String, val uiSetPlayMusic: String, val uiSetImportSmf: String, val uiSetReload: String,
    val uiSetResetSmf: String, val uiSetCustomDisplay: String, val uiSetDisplayTitle: String, val uiSetApplyBtn: String,
    // Error/Log/Dialog/Welcome extras
    val uiErrJsTitle: String, val uiErrUnknown: String,
    val uiLogCopyDesc: String, val uiLogClearDesc: String, val uiLogNoLogText: String, val uiLogPresetSaveLabel: String, val uiLogLocalSaveLabel: String,
    val uiDlgDelSave: String, val uiDlgEditUser: String, val uiDlgShareTitle: String, val uiDlgPackFail: String, val uiDlgNoShare: String,
    val uiWelcomeEditTitle: String, val uiWelcomeEditHint: String,
    // 存档模块 + 游戏画面 + settings/sounds 补充项
    val save: SaveDraft,
    val gameDisplay: Pvz2ToolConfigGameDisplay,
    val uiSetChangeProfile: String, val uiSetShowNotUpdate: String, val uiSetExitConfirm: String,
    val uiSndSwitchClick: String,
    val cgVideoPath: String, val cgVideoPoster: String, val cgVideoLoadTimeout: String,
    val schedules: List<ScheduleDraft> = emptyList(),
)

// 存档模块草稿（完整对齐 Pvz2ToolConfigUISave + operation）
data class SaveDraft(
    val presetConfirmTitle: String = "", val presetConfirmMessage: String = "",
    val deleteConfirmTitle: String = "", val deleteConfirmMessage: String = "",
    val coverConfirmTitle: String = "", val coverConfirmMessage: String = "",
    val deleteGameSaveConfirmTitle: String = "", val deleteGameSaveConfirmMessage: String = "",
    val saveInfoTitle: String = "", val saveNameLabel: String = "",
    val saveDescLabel: String = "", val cancelButton: String = "",
    val confirmButton: String = "", val shareButton: String = "",
    val exportButton: String = "", val importButton: String = "",
    val backupButton: String = "", val coverLocalButton: String = "",
    val deleteGameSaveButton: String = "", val coverPresetButton: String = "",
    val saveNameEmptyTip: String = "", val noLocalSaveTip: String = "",
    val selectLocalSaveTip: String = "", val backupSuccessTip: String = "",
    val backupFailTipPrefix: String = "", val exportSuccessTip: String = "",
    val exportFailTipPrefix: String = "", val importSuccessTip: String = "",
    val importFailTipPrefix: String = "", val deleteSuccessTip: String = "",
    val deleteFailTipPrefix: String = "", val coverSuccessTip: String = "",
    val coverFailTipPrefix: String = "", val deleteGameSaveSuccessTip: String = "",
    val deleteGameSaveFailTipPrefix: String = "", val defaultImportNamePrefix: String = "",
    val defaultBackupDesc: String = "", val defaultImportDesc: String = "",
    val exportOptionTitle: String = "", val exportToFolderOption: String = "",
    val shareAsPackageOption: String = "", val gameSaveLabel: String = "",
    val gameSaveInfoTemplate: String = "", val gameSaveUnknownUser: String = "",
    val gameSaveNotExistTip: String = "", val retryButtonText: String = "",
    val opBackup: String = "", val opExport: String = "", val opImport: String = "",
    val opDelete: String = "", val opDeleteGameSave: String = "", val opCover: String = "", val opSaveMeta: String = ""
)

fun SaveDraft.toConfigSave() = Pvz2ToolConfigUISave(
    presetConfirmTitle = presetConfirmTitle, presetConfirmMessage = presetConfirmMessage,
    deleteConfirmTitle = deleteConfirmTitle, deleteConfirmMessage = deleteConfirmMessage,
    coverConfirmTitle = coverConfirmTitle, coverConfirmMessage = coverConfirmMessage,
    deleteGameSaveConfirmTitle = deleteGameSaveConfirmTitle, deleteGameSaveConfirmMessage = deleteGameSaveConfirmMessage,
    saveInfoTitle = saveInfoTitle, saveNameLabel = saveNameLabel, saveDescLabel = saveDescLabel,
    cancelButton = cancelButton, confirmButton = confirmButton, shareButton = shareButton,
    exportButton = exportButton, importButton = importButton, backupButton = backupButton,
    coverLocalButton = coverLocalButton, deleteGameSaveButton = deleteGameSaveButton, coverPresetButton = coverPresetButton,
    saveNameEmptyTip = saveNameEmptyTip, noLocalSaveTip = noLocalSaveTip, selectLocalSaveTip = selectLocalSaveTip,
    backupSuccessTip = backupSuccessTip, backupFailTipPrefix = backupFailTipPrefix, exportSuccessTip = exportSuccessTip,
    exportFailTipPrefix = exportFailTipPrefix, importSuccessTip = importSuccessTip, importFailTipPrefix = importFailTipPrefix,
    deleteSuccessTip = deleteSuccessTip, deleteFailTipPrefix = deleteFailTipPrefix, coverSuccessTip = coverSuccessTip,
    coverFailTipPrefix = coverFailTipPrefix, deleteGameSaveSuccessTip = deleteGameSaveSuccessTip, deleteGameSaveFailTipPrefix = deleteGameSaveFailTipPrefix,
    defaultImportNamePrefix = defaultImportNamePrefix, defaultBackupDesc = defaultBackupDesc, defaultImportDesc = defaultImportDesc,
    exportOptionTitle = exportOptionTitle, exportToFolderOption = exportToFolderOption, shareAsPackageOption = shareAsPackageOption,
    gameSaveLabel = gameSaveLabel, gameSaveInfoTemplate = gameSaveInfoTemplate, gameSaveUnknownUser = gameSaveUnknownUser,
    gameSaveNotExistTip = gameSaveNotExistTip, retryButtonText = retryButtonText,
    operation = Pvz2ToolConfigOperation(backup = opBackup, export = opExport, import = opImport, delete = opDelete, deleteGameSave = opDeleteGameSave, cover = opCover, saveMeta = opSaveMeta)
)

// ── 从向导状态构建完整 dream.yml ──────────────────────────

/**
 * 将脚本类字段（jsPath / enterGamePath / isShowFromJsPath）的用户输入值转换为「APK assets 内的相对路径」。
 *
 * 该路径必须与 [fieldKeyToApkSubDir] 决定的打包落盘位置保持一致：
 * 打包器会把脚本类字段统一放到 `assets/pvz2tool/js/<用户输入相对路径>`，
 * 运行时 [AssetExtractorHolder.openInputStream] 会补 `pvz2tool/` 前缀按此路径查找。
 * 因此 yml 里写的 jsPath 直接使用用户输入值，不做自动补前缀。
 */
private fun packScriptAssetPath(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return raw
}

private fun buildYamlFromWizard(
    templateYml: String,
    gameActivity: String, smfDirectory: String, baseAssetPath: String, simplifiedLaunch: Boolean,
    cgVideoPath: String, cgVideoPoster: String, cgVideoLoadTimeout: String,
    gameActivityInvalid: String,
    versions: List<VersionDraft>, sections: List<SectionDraft>,
    announcements: List<AnnouncementDraft>,
    isExpandedVersions: Boolean, versionsTheme: String,
    fwItems: List<FwItemDraft>, tbiItems: List<TbiItemDraft>,
    bgImage: String, isUseSolidColorBg: Boolean, bgMusic: String, isPlayBgMusic: Boolean,
    sideBgImage: String, floatingBallIcon: String,
    showFloatingWindowLabel: String, isShowFloatingWindowDefault: Boolean,
    fwEmptyTip: String, fwAllHiddenTip: String,
    exitConfirmTitle: String, exitConfirmMessage: String, isUseExitConfirm: Boolean,
    exitConfirmButtonText: String, floatingExitConfirmTitle: String,
    floatingExitConfirmMessage: String, floatingExitConfirmButtonText: String,
    // UI 高级文本
    uiVersionLabel: String, uiUiVersion: String, uiAuthorInfo: String, uiTutorial: String, uiNoValidDirTip: String,
    uiTitleTopAppBar: String, uiTitleAbout: String, uiTitleCoreFunction: String, uiTitleVersionManage: String,
    uiBtnEnterGame: String, uiBtnTutorial: String, uiBtnResetData: String, uiBtnShowFW: String, uiBtnConfirmVersion: String,
    uiLogPanelTitle: String, uiDialogConfirm: String, uiDialogCancel: String, uiWelcomeGreeting: String,
    // Extractor
    uiExDialogTitle: String, uiExInitLoadTip: String, uiExInitProgTip: String, uiExNoNeedTip: String,
    uiExSingleFileTip: String, uiExMultiFileTip: String, uiExWaitingTip: String, uiExCompleteTip: String,
    uiExFailPrefix: String, uiExSkipPrefix: String, uiExContinueBtn: String, uiExCompleteBtn: String, uiExToastErr: String,
    // Sounds
    uiSndSwitchPress: String, uiSndSwitchRelease: String, uiSndBtnPress: String, uiSndBtnRelease: String,
    uiSndSettingsPress: String, uiSndSettingsRelease: String, uiSndXClosePress: String, uiSndXCloseRelease: String,
    uiSndPanelPress: String, uiSndPanelRelease: String,
    // Button icons
    uiBtnEnterGameIcon: Boolean, uiBtnTutorialIcon: Boolean, uiBtnResetDataIcon: Boolean,
    // Settings extra
    uiSetTitle: String, uiSetSolidBg: String, uiSetPlayMusic: String, uiSetImportSmf: String, uiSetReload: String,
    uiSetResetSmf: String, uiSetCustomDisplay: String, uiSetDisplayTitle: String, uiSetApplyBtn: String,
    // Error/Log/Dialog/Welcome extras
    uiErrJsTitle: String, uiErrUnknown: String,
    uiLogCopyDesc: String, uiLogClearDesc: String, uiLogNoLogText: String, uiLogPresetSaveLabel: String, uiLogLocalSaveLabel: String,
    uiDlgDelSave: String, uiDlgEditUser: String, uiDlgShareTitle: String, uiDlgPackFail: String, uiDlgNoShare: String,
    uiWelcomeEditTitle: String, uiWelcomeEditHint: String,
    saveDraft: SaveDraft, gameDisplay: Pvz2ToolConfigGameDisplay,
    uiSetChangeProfile: String, uiSetShowNotUpdate: String, uiSetExitConfirm: String, uiSndSwitchClick: String,
    schedules: List<ScheduleDraft>,
    /** 更新模式：gameActivity 直接沿用目标 APK 之前的配置（template.gameActivity 经深度合并后已是目标原值），不取向导状态，避免被空值/重新探测覆盖 */
    isUpdateMode: Boolean = false
): String {
    // 简易模式：直接构建精简 config 对象
    if (simplifiedLaunch) {
        val cfg = Pvz2ToolSimpleConfig(
            gameActivity = gameActivity,
            smfDirectory = smfDirectory,
            baseAssetPath = baseAssetPath.ifBlank { "version/base/smf" },
            simplifiedLaunch = true,
            cgVideoPath = cgVideoPath,
            cgVideoPoster = cgVideoPoster.ifBlank { null },
            cgVideoLoadTimeout = cgVideoLoadTimeout.toLongOrNull() ?: 5000L,
            isShowFloatingWindow = isShowFloatingWindowDefault,
            isUseExitConfirm = isUseExitConfirm,
            isUseCustomGameDisplay = gameDisplay.isUseCustomGameDisplay,
            displayMode = gameDisplay.displayMode,
            windowRatio = gameDisplay.windowRatio,
            windowWidth = gameDisplay.windowWidth,
            windowHeight = gameDisplay.windowHeight,
            isAllowRotation = gameDisplay.isAllowRotation,
            floatingWindow = fwItems.map { fw ->
                FloatingWindowItem(
                    id = fw.id,
                    name = fw.name.ifBlank { null },
                    desc = fw.desc.ifBlank { null },
                    icon = fw.icon.ifBlank { null },
                    buttonText = fw.buttonText.ifBlank { null },
                    buttonColor = fw.buttonColor.ifBlank { null },
                    jsScript = fw.jsScript.ifBlank { null },
                    jsPath = packScriptAssetPath(fw.jsPath),
                    isShowFromJs = fw.isShowFromJs.ifBlank { null },
                    isShowFromJsPath = packScriptAssetPath(fw.isShowFromJsPath),
                    smfList = fw.smfList
                )
            },
        )
        val yml = Yaml(configuration = YamlConfiguration(encodeDefaults = false, multiLineStringStyle = MultiLineStringStyle.Literal)).encodeToString(Pvz2ToolSimpleConfig.serializer(), cfg)
        return "# Pvz2Tool 简易模式配置（由集成器生成）\n" + yml.removePrefix("---\n")
    }

    // 完整模式：解析模板 YAML 保留未编辑字段（title/button/extractor 等），只覆盖向导编辑的部分
    val template = runCatching {
        lenientYaml.decodeFromString(Pvz2ToolConfig.serializer(), templateYml)
    }.getOrNull() ?: return templateYml

    val cfg = template.copy(
        // 更新模式：直接用目标 APK 之前记录的 gameActivity（template.gameActivity 已深度合并目标 yml），不取向导状态
        gameActivity = if (isUpdateMode) template.gameActivity else gameActivity,
        smfDirectory = smfDirectory,
        baseAssetPath = baseAssetPath.ifBlank { "version/base/smf" },
        simplifiedLaunch = false,
        versions = versions.map { v ->
            VersionDef(v.id, v.name, v.desc, v.icon.ifBlank { null },
                v.default, v.baseAssetPath.ifBlank { null }, v.assetPath.ifBlank { null },
                v.forceOverride, v.enterGameScript.ifBlank { null }, packScriptAssetPath(v.enterGamePath))
        },
        sections = sections.map { s ->
            DynamicSection(s.id, s.title,
                visibleOnVersionIds = s.visibleOnVersionIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
                targetPath = s.targetPath.ifBlank { null },
                addItems = s.addItems, isExpanded = s.isExpanded,
                confirmButtonText = s.confirmButtonText.ifBlank { null },
                items = s.items.map { item ->
                    SectionItem(item.id, SectionType.valueOf(item.type),
                        name = item.name.ifBlank { null }, desc = item.desc.ifBlank { null },
                        icon = item.icon.ifBlank { null }, assetPath = item.assetPath.ifBlank { null },
                        default = when (item.type) {
                            "CHECKBOX" -> item.checkboxDefault
                            "RADIO" -> item.radioDefault
                            else -> false
                        },
                        jsScript = item.jsScript.ifBlank { null }, jsPath = packScriptAssetPath(item.jsPath),
                        isShowFromJs = item.isShowFromJs.ifBlank { null },
                        isShowFromJsPath = packScriptAssetPath(item.isShowFromJsPath),
                        groupId = item.groupId,
                        smfList = item.smfList.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                        minValue = if (item.type == "SLIDER") item.minValue.toFloatOrNull() ?: 0f else 0f,
                        maxValue = if (item.type == "SLIDER") item.maxValue.toFloatOrNull() ?: 100f else 100f,
                        defaultValue = if (item.type == "SLIDER") item.defaultValue.toFloatOrNull() else null,
                        step = if (item.type == "SLIDER") item.step.toFloatOrNull() ?: 1f else 1f,
                        valueSuffix = if (item.type == "SLIDER") item.valueSuffix.ifBlank { null } else null,
                        buttonText = if (item.type == "BUTTON") item.buttonText.ifBlank { null } else null,
                        buttonColor = if (item.type == "BUTTON") item.buttonColor.ifBlank { null } else null,
                        placeholder = if (item.type == "INPUT") item.placeholder.ifBlank { null } else null,
                        inputDefault = if (item.type == "INPUT") item.inputDefault.ifBlank { null } else null,
                        infoValue = if (item.type == "INFO") item.infoValue.ifBlank { null } else null
                    )
                },
                descriptionContent = s.descriptionContent.ifBlank { "" },
                theme = PvzCollapsiblePanelTheme.valueOf(s.theme),
                jsScript = s.jsScript.ifBlank { null }, jsPath = packScriptAssetPath(s.jsPath)
            )
        },
        isExpandedVersions = isExpandedVersions,
        versionsTheme = PvzCollapsiblePanelTheme.valueOf(versionsTheme),
        announcement = announcements.map {
            Pvz2ToolConfigAnnouncement(it.title, it.content)
        },
        schedules = schedules.map {
            ScheduleDef(it.id, it.name, it.cron, it.jsScript.ifBlank { null }, it.jsPath.ifBlank { null }, it.enabled)
        },
        ui = template.ui.copy(
            save = saveDraft.toConfigSave(),
            assets = template.ui.assets.copy(
                background = bgImage.ifBlank { "bg_main.jpg" },
                isUseSolidColorBackground = isUseSolidColorBg,
                backgroundMusic = bgMusic.ifBlank { "bg_music.wav" },
                isPlayBackgroundMusic = isPlayBgMusic,
                sideBgImage = sideBgImage.ifBlank { "game_side_bg.jpg" },
                floatingBallIcon = floatingBallIcon.ifBlank {  "ic_floating_dave.png" },
                cgVideoPath = cgVideoPath.ifBlank { "opening.mp4" },
                cgVideoPoster = cgVideoPoster.ifBlank { null },
                cgVideoLoadTimeout = cgVideoLoadTimeout.toLongOrNull() ?: 5000L
            ),
            settings = template.ui.settings.copy(
                showFloatingWindow = showFloatingWindowLabel,
                isShowFloatingWindow = isShowFloatingWindowDefault,
                exitConfirmTitle = exitConfirmTitle.ifBlank { "退出游戏" },
                exitConfirmMessage = exitConfirmMessage.ifBlank { "确定要退出游戏吗？" },
                isUseExitConfirm = isUseExitConfirm,
                exitConfirmButtonText = exitConfirmButtonText.ifBlank { "确认退出" },
                floatingExitConfirmTitle = floatingExitConfirmTitle.ifBlank { "确认退出" },
                floatingExitConfirmMessage = floatingExitConfirmMessage.ifBlank { "确定要退出悬浮窗吗(直至重启游戏后显示)？" },
                floatingExitConfirmButtonText = floatingExitConfirmButtonText.ifBlank { "确认" },
                title = uiSetTitle.ifBlank { "戴夫工具箱·设置" },
                solidBackgroundMode = uiSetSolidBg.ifBlank { "纯色背景模式" },
                playBackgroundMusic = uiSetPlayMusic.ifBlank { "播放背景音乐" },
                importSmfFile = uiSetImportSmf.ifBlank { "导入SMF文件" },
                reloadConfig = uiSetReload.ifBlank { "重新读取配置文件" },
                resetPacketDeepClearing = uiSetResetSmf.ifBlank { "重置数据包时删除smf目录" },
                customGameDisplay = uiSetCustomDisplay.ifBlank { "自定义游戏画面" },
                customGameDisplayTitle = uiSetDisplayTitle.ifBlank { "游戏画面设置" },
                applyButtonText = uiSetApplyBtn.ifBlank { "应 用" },
                changeTheProfileReadLocation = uiSetChangeProfile.ifBlank { "切换存档读取位置" },
                showNotUpdate = uiSetShowNotUpdate.ifBlank { "进入游戏时未检测到更新也进行弹窗" },
                exitConfirm = uiSetExitConfirm.ifBlank { "退出游戏二次确认" },
                gameDisplay = gameDisplay
            ),
            floatingWindow = template.ui.floatingWindow.copy(
                emptyTip = fwEmptyTip,
                allHiddenTip = fwAllHiddenTip,
                items = fwItems.map {
                    FloatingWindowItem(it.id, it.name.ifBlank { null }, it.desc.ifBlank { null },
                        it.icon.ifBlank { null }, it.buttonText.ifBlank { null },
                        it.buttonColor.ifBlank { null },
                        it.jsScript.ifBlank { null }, packScriptAssetPath(it.jsPath),
                        it.isShowFromJs.ifBlank { null }, packScriptAssetPath(it.isShowFromJsPath),
                        smfList = it.smfList)
                }
            ),
            topBarIcons = template.ui.topBarIcons.copy(
                items = tbiItems.map {
                    TopBarIconItem(it.id, it.icon, it.iconPress.ifBlank { null },
                        it.contentDescription.ifBlank { null },
                        it.jsScript.ifBlank { null }, packScriptAssetPath(it.jsPath),
                        it.isShowFromJs.ifBlank { null }, packScriptAssetPath(it.isShowFromJsPath),
                        it.pressSound.ifBlank { null }, it.releaseSound.ifBlank { null },
                        smfList = it.smfList)
                }
            ),
            versionLabel = uiVersionLabel,
            uiVersion = uiUiVersion,
            authorInfo = uiAuthorInfo,
            tutorial = uiTutorial,
            noValidDirTip = uiNoValidDirTip,
            title = template.ui.title.copy(
                topAppBar = uiTitleTopAppBar,
                about = uiTitleAbout,
                coreFunction = uiTitleCoreFunction,
                versionManage = uiTitleVersionManage
            ),
            button = template.ui.button.copy(
                enterGame = uiBtnEnterGame,
                isEnterGameDefaultIcon = uiBtnEnterGameIcon,
                tutorial = uiBtnTutorial,
                isTutorialDefaultIcon = uiBtnTutorialIcon,
                resetData = uiBtnResetData,
                isResetDataDefaultIcon = uiBtnResetDataIcon,
                showFloatingWindow = uiBtnShowFW,
                confirmVersion = uiBtnConfirmVersion
            ),
            extractor = template.ui.extractor.copy(
                dialogTitle = uiExDialogTitle, initialLoadingProgressTip = uiExInitLoadTip,
                initialProgressTip = uiExInitProgTip, noNeedExtractTip = uiExNoNeedTip,
                singleFileProcessingTip = uiExSingleFileTip, multiFileProcessingTip = uiExMultiFileTip,
                waitingTip = uiExWaitingTip, extractCompleteTip = uiExCompleteTip,
                extractFailTipPrefix = uiExFailPrefix, fileSkipTipPrefix = uiExSkipPrefix,
                continueButtonText = uiExContinueBtn, completeButtonText = uiExCompleteBtn,
                toastErrorPrefix = uiExToastErr
            ),
            sounds = template.ui.sounds.copy(
                switchClickPress = uiSndSwitchPress, switchClickRelease = uiSndSwitchRelease,
                buttonClickPress = uiSndBtnPress, buttonClickRelease = uiSndBtnRelease,
                buttonSettingsPress = uiSndSettingsPress, buttonSettingsRelease = uiSndSettingsRelease,
                buttonXClosePress = uiSndXClosePress, buttonXCloseRelease = uiSndXCloseRelease,
                collapsiblePanelPress = uiSndPanelPress, collapsiblePanelRelease = uiSndPanelRelease,
                switchClick = uiSndSwitchClick.ifBlank { "ui_switch_click.wav" }
            ),
            error = template.ui.error.copy(
                jsExecuteErrorTitle = uiErrJsTitle, unknownError = uiErrUnknown,
                gameActivityInvalid = gameActivityInvalid
            ),
            log = template.ui.log.copy(
                panelTitle = uiLogPanelTitle, copyLogDesc = uiLogCopyDesc, clearLogDesc = uiLogClearDesc,
                noLogText = uiLogNoLogText, presetSaveLabel = uiLogPresetSaveLabel, localSaveLabel = uiLogLocalSaveLabel
            ),
            dialog = template.ui.dialog.copy(
                confirm = uiDialogConfirm, cancel = uiDialogCancel,
                deleteSaveDesc = uiDlgDelSave, editUserNameDesc = uiDlgEditUser,
                shareSaveChooserTitle = uiDlgShareTitle, sharePackFailedTip = uiDlgPackFail, noShareableSaveTip = uiDlgNoShare
            ),
            welcome = template.ui.welcome.copy(
                greetingTemplate = uiWelcomeGreeting, editUserNameTitle = uiWelcomeEditTitle, editUserNameHint = uiWelcomeEditHint
            )
        )
    )
    // 1. kaml 序列化（encodeDefaults=false 跳过 null/空/默认值，multiLineStringStyle=Literal 块标量）
    val yamlWriter = Yaml(configuration = YamlConfiguration(encodeDefaults = false, multiLineStringStyle = MultiLineStringStyle.Literal))
    var yml = yamlWriter.encodeToString(Pvz2ToolConfig.serializer(), cfg).removePrefix("---\n")
    // 2. 从模板注入注释
    yml = injectCommentsFromTemplate(yml, templateYml)
    return "# Pvz2Tool 完整配置（由集成器生成）\n" + yml
}

// ── YAML 后处理 ────────────────────────────────────────────


/**
 * 将模板中的注释块注入到生成的 YAML 中。
 * 按 (缩进, 键名) 配对匹配，避免注释注入到错误缩进级别的同名键上。
 * 每个 (indent, key) 组合只注入一次（取第一个匹配位置）。
 */
private fun injectCommentsFromTemplate(generated: String, template: String): String {
    data class CKey(val indent: Int, val name: String)
    val commentMap = LinkedHashMap<CKey, String>() // 保留顺序
    var pending = mutableListOf<String>()
    for (line in template.lines()) {
        val trimmed = line.trimStart()
        if (trimmed.startsWith("#")) {
            pending.add(line)
        } else if (trimmed.isNotEmpty()) {
            val indent = line.length - trimmed.length
            val name = extractYamlKey(trimmed)
            if (name.isNotEmpty() && pending.isNotEmpty()) {
                val ck = CKey(indent, name)
                if (ck !in commentMap) commentMap[ck] = pending.joinToString("\n")
            }
            pending.clear()
        } else pending.clear()
    }
    if (commentMap.isEmpty()) return generated
    val gLines = generated.lines().toMutableList()
    val inserted = mutableSetOf<CKey>()
    var i = 0
    while (i < gLines.size) {
        val line = gLines[i]
        val trimmed = line.trimStart()
        val indent = line.length - trimmed.length
        val name = extractYamlKey(trimmed)
        if (name.isNotEmpty()) {
            val ck = CKey(indent, name)
            if (ck in commentMap && ck !in inserted) {
                gLines.add(i, commentMap[ck]!!)
                i += commentMap[ck]!!.lines().size
                inserted.add(ck)
            }
        }
        i++
    }
    return gLines.joinToString("\n")
}

/** 从 YAML 行提取键名（"gameActivity:", "- id:" → "gameActivity"/"id"） */
private fun extractYamlKey(line: String): String {
    val trimmed = line.trimStart().trimStart('-').trimStart()
    val ci = trimmed.indexOf(':')
    if (ci <= 0) return ""
    return trimmed.substring(0, ci).trim()
}

// ── 主界面 ──────────────────────────────────────────────────────

/**
 * 工具箱集成器 —— 整体视觉对标 [Pvz2MainScreen]（背景图 + Scaffold + 绿色渐变 TopAppBar）。
 *
 * 第 3 步的「界面预览」会读取 dream.yml 生成临时 [Pvz2ToolConfig]，临时替换
 * [InitializePvz2.config] 后直接渲染 [Pvz2MainScreen]()——所见即所得。
 */
/**
 * 选择文件时的「覆盖风险」检测回调，经 CompositionLocal 注入 FileInputRow，
 * 避免在每个调用点逐个透传。回调返回被占用的 APK 完整路径（用于提示），无风险返回 null。
 * 占用判定：① 该字段的 APK 目标路径已被其他已选字段复用（多次使用）；
 *          ② 该路径已存在于基础 APK（工具箱自身资源，非本次选择造成）。
 */
val LocalOverwriteChecker = staticCompositionLocalOf<(String) -> String?> { { _ -> null } }

/** 扫描目标 APK 中第一个超过阈值的 DEX 序号（1-based）。
 *  直接通过 ZipFile 读取条目大小——ARSCLib 的 InputSource.getLength() 对 APK 内未解压的
 *  DEX 条目常返回不准确/抛异常，不能用于精确大小判断。 */
private fun findLargeDexIndex(apk: File, thresholdBytes: Long = 20L * 1024 * 1024): Int {
    return runCatching {
        java.util.zip.ZipFile(apk).use { zip ->
            val dexEntries = zip.entries().asSequence()
                .filter { !it.isDirectory && it.name.endsWith(".dex") }
                .sortedBy { it.name }
                .toList()
            val idx = dexEntries.indexOfFirst { it.size > thresholdBytes }
            if (idx >= 0) idx + 1 else 1
        }
    }.getOrDefault(1)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolboxIntegratorScreen(
    filePickerManager: FilePickerManager,
    configReady: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cfg = LocalConfiguration.current
    val smallest = minOf(cfg.screenWidthDp, cfg.screenHeightDp)
    val topBarHeightDp = when {
        smallest >= 840 -> 72
        smallest >= 600 -> 64
        else -> 56
    }.dp

    val sourceApk = remember { File(context.applicationInfo.sourceDir) }
    // 基础 APK（工具箱自身）已存在的资源条目名集合，用于「选择文件时检测是否覆盖基础资源」
    var baseApkEntries by remember { mutableStateOf<Set<String>>(emptySet()) }
    // 源 APK（工具箱自身）的 DEX 数量（更新模式结束序号默认值计算用，需在引用它的 LaunchedEffect 之前声明）
    var sourceDexCount by remember { mutableStateOf(0) }
    LaunchedEffect(sourceApk) {
        withContext(Dispatchers.IO) {
            val set = runCatching {
                val m = ApkModule.loadApkFile(sourceApk)
                m.listInputSources().map { it.name }.toSet()
            }.getOrDefault(emptySet())
            baseApkEntries = set
        }
    }
    LaunchedEffect(sourceApk) {
        withContext(Dispatchers.IO) {
            sourceDexCount = runCatching { ApkModule.loadApkFile(sourceApk).listDexFiles().size }.getOrDefault(0)
        }
    }
    val sourceVersion = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrDefault("?") ?: "?"
    }

    // ── 从 dream.yml 读取默认值 ──
    val dreamYmlRaw = remember {
        runCatching {
            context.assets.open("pvz2tool/dream.yml").bufferedReader().use { it.readText() }
        }.getOrDefault("")
    }

    // ── 向导步骤状态 ──
    var step by remember { mutableStateOf(1) }
    var targetApk by remember { mutableStateOf<File?>(null) }
    var dexStrategy by remember { mutableStateOf(DexStrategy.INSERT_BEFORE) }
    var includeExamples by remember { mutableStateOf(true) }
    var report by remember { mutableStateOf<IntegrateReport?>(null) }
    var result by remember { mutableStateOf<MergeResult?>(null) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // ── 集成模式：内置到未集成的 APK / 更新已集成的 APK ──
    // "integrate" = 首次内置到未集成的目标 APK；"update" = 在已集成工具箱的 APK 上更新（替换旧版 DEX）
    var sourceMode by remember { mutableStateOf("integrate") }
    // 更新模式下的 DEX 替换参数
    var updDexStart by remember { mutableStateOf("1") }      // 旧版工具箱 DEX 起始序号（1 起）
    var updDexEnd by remember { mutableStateOf("") }         // 结束序号（留空=覆盖到本版本所有 DEX）
    var updInsertMode by remember { mutableStateOf(DexStrategy.INSERT_BEFORE) }
    // 更新模式：是否附加源 APK 中目标未包含的 pvz2tool 文件（如默认图/示例）。默认开，无描述文件时关
    var appendUnreferencedAssets by remember { mutableStateOf(false) }
    // 选完目标 APK 后从描述文件检测到的历史集成信息（null=未检测到）
    var detectedInfo by remember { mutableStateOf<ToolboxApkMerger.IntegratorInfo?>(null) }
    // 更新模式下，从目标 APK 读取的 dream.yml（作为向导模板，保留目标现有配置）
    var targetDreamYmlRaw by remember { mutableStateOf<String?>(null) }

    // ── 从 dream.yml 读取默认值（更新模式优先取目标 APK 当前配置）──
    // 更新模式：以「内置模板 dream.yml」为 base、「目标旧 dream.yml」为 override 做深度合并——
    // 目标有的字段用目标的（保留用户/旧版真实配置），目标缺失的字段（如新版新增功能）用内置模板默认补齐。
    // 普通集成直接用内置模板。
    val effectiveDefaultsRaw = effectiveDreamYmlRaw(sourceMode, targetDreamYmlRaw, dreamYmlRaw)
    val defaults = remember(sourceMode, targetDreamYmlRaw, dreamYmlRaw) { loadDefaultsFromDreamYml(effectiveDefaultsRaw) }

    // ── 集成选项 ──
    var gameActivity by remember { mutableStateOf("") }
    var smfDirectory by remember { mutableStateOf(defaults.smfDirectory) }
    // 全局基础资源包路径（onEnterGame 解压的通用资源目录，SMF/资源设置页可编辑，需与打包资源路径一致）
    var baseAssetPath by remember { mutableStateOf(defaults.baseAssetPath.ifBlank { "version/base/smf" }) }
    var simplifiedLaunch by remember { mutableStateOf(false) }
    // CG 设定（简易模式=根属性，非简易=ui.assets）
    var cgVideoPath by remember { mutableStateOf("opening.mp4") }
    var cgVideoPoster by remember { mutableStateOf("bg_main.jpg") }
    var cgVideoLoadTimeout by remember { mutableStateOf("5000") }
    // 错误提示
    var gameActivityInvalid by remember { mutableStateOf("设置的游戏Activity有误或不存在") }

    // ── 子页面导航 ──
    var showUiSettings by remember { mutableStateOf(false) }
    var showAnnouncementSettings by remember { mutableStateOf(false) }
    var showFloatingWindowSettings by remember { mutableStateOf(false) }
    var showTopBarIconSettings by remember { mutableStateOf(false) }
    var showUiAdvancedSettings by remember { mutableStateOf(false) }
    var showVersionSettings by remember { mutableStateOf(false) }
    var showSectionSettings by remember { mutableStateOf(false) }
    var showCompositeTextTool by remember { mutableStateOf(false) }
    // 栏目→功能项导航：选中的栏目索引，-1 表示返回栏目列表
    var editingSectionIndex by remember { mutableStateOf(-1) }

    // 选中的文件
    var selectedFiles by remember { mutableStateOf<Map<String, File>>(emptyMap()) }
    var pendingFileNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    // 选中的文件夹：fieldKey -> 已落到本地缓存的文件夹（其 tree 会按 value 的父目录名重命名打包）
    var selectedFolders by remember { mutableStateOf<Map<String, File>>(emptyMap()) }

    // ── SMF/资源设置 ──
    // 子页面导航
    var showSmfResourceSettings by remember { mutableStateOf(false) }
    var showScheduleSettings by remember { mutableStateOf(false) }
    // 被用户排除（不打包）的 SMF/资源条目，相对 assets/pvz2tool/ 的路径集合（如 version/base/smf/foo.dat）
    var excludedSmfAssets by remember { mutableStateOf<Set<String>>(emptySet()) }
    // 追加的文件：APK 内相对 assets/pvz2tool/ 的路径 → 本地文件
    var addedSmfFiles by remember { mutableStateOf<Map<String, File>>(emptyMap()) }
    // 追加的文件夹：scope（相对 assets/pvz2tool/ 的资源目录，如 "version/base/smf"）→ 已落到本地缓存的文件夹
    var addedSmfFolders by remember { mutableStateOf<Map<String, File>>(emptyMap()) }
    // 「选择后删除」总开关（仅作用于 SMF/资源设置页的资源选择）：
    //   来自本地文件/文件夹 → 复制进缓存后删除本地原件（SAF delete）
    //   来自目标 APK       → 登记进 removedTargetEntries，打包时从产物中移除
    var smfDeleteAfterPick by remember { mutableStateOf(false) }
    // 需要从目标 APK 产物中删除的原始条目（APK 内完整路径，如 assets/foo.rsb）
    var removedTargetEntries by remember { mutableStateOf<Set<String>>(emptySet()) }
    // 目标 APK 条目浏览器：非 null 时弹窗打开，值 = 要追加到的 scope
    var targetApkBrowserScope by remember { mutableStateOf<String?>(null) }
    // PopCap 原版 APK 警告弹窗：非 null 时显示，值为待删除的缓存文件
    var popCapWarningFile by remember { mutableStateOf<File?>(null) }
    // 工具箱模式不匹配警告："update_wrong"=更新模式选了无工具箱APK / "integrate_wrong"=普通模式选了有工具箱APK
    var toolboxModeWarning by remember { mutableStateOf<String?>(null) }
    // 从目标 APK 选择的条目：addedSmfFiles rel → APK 内完整路径（如 assets/beach.rsb.smf）
    // 用于「先选择后开删除开关」时追溯登记到 removedTargetEntries
    var targetApkMapping by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    // 本地文件选择：addedSmfFiles rel → SAF URI 字符串（用于追溯注册待删除）
    var localFileUris by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    // 本地文件夹选择：scope → SAF URI 字符串（用于追溯注册待删除）
    var localFolderUris by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    // 待打包完成后删除的本地文件/文件夹列表
    var pendingLocalDeletions by remember { mutableStateOf<List<PendingLocalDeletion>>(emptyList()) }

    // 版本列表
    var versions by remember { mutableStateOf(defaults.versions.ifEmpty { listOf(VersionDraft("new", "正式服", "全新体验", "new_version_icon.png", default = true), VersionDraft("old", "怀旧服", "经典怀旧", "old_version_icon.png")) }) }
    var isExpandedVersions by remember { mutableStateOf(defaults.isExpandedVersions) }
    var versionsTheme by remember { mutableStateOf(defaults.versionsTheme) }

    // 栏目列表
    var sections by remember { mutableStateOf(defaults.sections.filter { includeExamples || !it.id.startsWith("example_") }) }

    // 公告
    var announcements by remember { mutableStateOf(defaults.announcements.ifEmpty { listOf(AnnouncementDraft("拓展2.5.1(最新版)", "内容{{red:演示的红色内容}}内容"), AnnouncementDraft("拓展2.0.0(怀旧版)", "内容")) }) }

    // 定时任务
    var schedules by remember { mutableStateOf(defaults.schedules) }

    // 悬浮窗设置
    var showFloatingWindowLabel by remember { mutableStateOf(defaults.showFloatingWindowLabel.ifBlank { "是否开启悬浮窗" }) }
    var isShowFloatingWindowDefault by remember { mutableStateOf(defaults.isShowFloatingWindowDefault) }
    var fwItems by remember { mutableStateOf(defaults.fwItems.ifEmpty { listOf(FwItemDraft(id = "vpn_toggle", name = "{{js:vpn.isActive() ? '恢复网络' : '断开网络'}}", buttonColor = "red", isShowFromJs = "vpn.isPrepared()", jsScript = "vpn.isActive() ? vpn.restore() : vpn.disconnect();"), FwItemDraft(id = "game_display", name = "画面设置", buttonColor = "green", isShowFromJs = "ui.isCustomGameDisplayEnabled()", jsScript = "ui.showGameDisplay();")) }) }
    var fwEmptyTip by remember { mutableStateOf(defaults.fwEmptyTip.ifBlank { "（悬浮窗暂无内容，请在 dream.yml 的 ui.floatingWindow.items 中配置）" }) }
    var fwAllHiddenTip by remember { mutableStateOf(defaults.fwAllHiddenTip.ifBlank { "（当前没有可用的功能）" }) }
    var exitConfirmTitle by remember { mutableStateOf(defaults.exitConfirmTitle.ifBlank { "退出游戏" }) }
    var exitConfirmMessage by remember { mutableStateOf(defaults.exitConfirmMessage.ifBlank { "确定要退出游戏吗？" }) }
    var isUseExitConfirm by remember { mutableStateOf(defaults.isUseExitConfirm) }
    var exitConfirmButtonText by remember { mutableStateOf(defaults.exitConfirmButtonText.ifBlank { "确认退出" }) }
    var floatingExitConfirmTitle by remember { mutableStateOf(defaults.floatingExitConfirmTitle.ifBlank { "确认退出" }) }
    var floatingExitConfirmMessage by remember { mutableStateOf(defaults.floatingExitConfirmMessage.ifBlank { "确定要退出悬浮窗吗(直至重启游戏后显示)？" }) }
    var floatingExitConfirmButtonText by remember { mutableStateOf(defaults.floatingExitConfirmButtonText.ifBlank { "确认" }) }

    // 顶栏图标
    var tbiItems by remember { mutableStateOf(defaults.tbiItems.ifEmpty { listOf(TbiItemDraft(id = "refresh_top", icon = "images/new_version_icon.png", iconPress = "images/new_version_icon.png", contentDescription = "刷新", isShowFromJs = "ui.isCustomGameDisplayEnabled()", jsScript = "ui.refreshAll();"), TbiItemDraft(id = "help_top", icon = "images/new_version_icon.png", contentDescription = "帮助", jsPath = "topbar/help.js")) }) }

    // 非简易模式额外选项
    var bgImage by remember { mutableStateOf(defaults.bgImage.ifBlank { "bg_main.jpg" }) }
    var isUseSolidColorBg by remember { mutableStateOf(defaults.isUseSolidColorBg) }
    var bgMusic by remember { mutableStateOf(defaults.bgMusic.ifBlank { "bg_music.wav" }) }
    var isPlayBgMusic by remember { mutableStateOf(defaults.isPlayBgMusic) }

    // ── UI 高级文本设置 ──
    var uiVersionLabel by remember { mutableStateOf(defaults.uiVersionLabel) }
    var uiUiVersion by remember { mutableStateOf(defaults.uiUiVersion) }
    var uiAuthorInfo by remember { mutableStateOf(defaults.uiAuthorInfo) }
    var uiTutorial by remember { mutableStateOf(defaults.uiTutorial) }
    var uiNoValidDirTip by remember { mutableStateOf(defaults.uiNoValidDirTip) }
    var uiTitleTopAppBar by remember { mutableStateOf(defaults.uiTitleTopAppBar) }
    var uiTitleAbout by remember { mutableStateOf(defaults.uiTitleAbout) }
    var uiTitleCoreFunction by remember { mutableStateOf(defaults.uiTitleCoreFunction) }
    var uiTitleVersionManage by remember { mutableStateOf(defaults.uiTitleVersionManage) }
    var uiBtnEnterGame by remember { mutableStateOf(defaults.uiBtnEnterGame) }
    var uiBtnTutorial by remember { mutableStateOf(defaults.uiBtnTutorial) }
    var uiBtnResetData by remember { mutableStateOf(defaults.uiBtnResetData) }
    var uiBtnShowFW by remember { mutableStateOf(defaults.uiBtnShowFW) }
    var uiBtnConfirmVersion by remember { mutableStateOf(defaults.uiBtnConfirmVersion) }
    var uiLogPanelTitle by remember { mutableStateOf(defaults.uiLogPanelTitle) }
    var uiLogCopyDesc by remember { mutableStateOf(defaults.uiLogCopyDesc) }
    var uiLogClearDesc by remember { mutableStateOf(defaults.uiLogClearDesc) }
    var uiLogNoLogText by remember { mutableStateOf(defaults.uiLogNoLogText) }
    var uiLogPresetSaveLabel by remember { mutableStateOf(defaults.uiLogPresetSaveLabel) }
    var uiLogLocalSaveLabel by remember { mutableStateOf(defaults.uiLogLocalSaveLabel) }
    var uiDialogConfirm by remember { mutableStateOf(defaults.uiDialogConfirm) }
    var uiDialogCancel by remember { mutableStateOf(defaults.uiDialogCancel) }
    var uiWelcomeGreeting by remember { mutableStateOf(defaults.uiWelcomeGreeting) }
    var uiWelcomeEditTitle by remember { mutableStateOf(defaults.uiWelcomeEditTitle) }
    var uiWelcomeEditHint by remember { mutableStateOf(defaults.uiWelcomeEditHint) }
    // Extractor
    var uiExDialogTitle by remember { mutableStateOf(defaults.uiExDialogTitle) }; var uiExInitLoadTip by remember { mutableStateOf(defaults.uiExInitLoadTip) }
    var uiExInitProgTip by remember { mutableStateOf(defaults.uiExInitProgTip) }; var uiExNoNeedTip by remember { mutableStateOf(defaults.uiExNoNeedTip) }
    var uiExSingleFileTip by remember { mutableStateOf(defaults.uiExSingleFileTip) }; var uiExMultiFileTip by remember { mutableStateOf(defaults.uiExMultiFileTip) }
    var uiExWaitingTip by remember { mutableStateOf(defaults.uiExWaitingTip) }; var uiExCompleteTip by remember { mutableStateOf(defaults.uiExCompleteTip) }
    var uiExFailPrefix by remember { mutableStateOf(defaults.uiExFailPrefix) }; var uiExSkipPrefix by remember { mutableStateOf(defaults.uiExSkipPrefix) }
    var uiExContinueBtn by remember { mutableStateOf(defaults.uiExContinueBtn) }; var uiExCompleteBtn by remember { mutableStateOf(defaults.uiExCompleteBtn) }
    var uiExToastErr by remember { mutableStateOf(defaults.uiExToastErr) }
    // Sounds
    var uiSndSwitchPress by remember { mutableStateOf(defaults.uiSndSwitchPress) }; var uiSndSwitchRelease by remember { mutableStateOf(defaults.uiSndSwitchRelease) }
    var uiSndBtnPress by remember { mutableStateOf(defaults.uiSndBtnPress) }; var uiSndBtnRelease by remember { mutableStateOf(defaults.uiSndBtnRelease) }
    var uiSndSettingsPress by remember { mutableStateOf(defaults.uiSndSettingsPress) }; var uiSndSettingsRelease by remember { mutableStateOf(defaults.uiSndSettingsRelease) }
    var uiSndXClosePress by remember { mutableStateOf(defaults.uiSndXClosePress) }; var uiSndXCloseRelease by remember { mutableStateOf(defaults.uiSndXCloseRelease) }
    var uiSndPanelPress by remember { mutableStateOf(defaults.uiSndPanelPress) }; var uiSndPanelRelease by remember { mutableStateOf(defaults.uiSndPanelRelease) }
    // Button icons
    var uiBtnEnterGameIcon by remember { mutableStateOf(defaults.uiBtnEnterGameIcon) }; var uiBtnTutorialIcon by remember { mutableStateOf(defaults.uiBtnTutorialIcon) }
    var uiBtnResetDataIcon by remember { mutableStateOf(defaults.uiBtnResetDataIcon) }
    // Settings
    var uiSetTitle by remember { mutableStateOf(defaults.uiSetTitle) }; var uiSetSolidBg by remember { mutableStateOf(defaults.uiSetSolidBg) }
    var uiSetPlayMusic by remember { mutableStateOf(defaults.uiSetPlayMusic) }; var uiSetImportSmf by remember { mutableStateOf(defaults.uiSetImportSmf) }
    var uiSetReload by remember { mutableStateOf(defaults.uiSetReload) }; var uiSetResetSmf by remember { mutableStateOf(defaults.uiSetResetSmf) }
    var uiSetCustomDisplay by remember { mutableStateOf(defaults.uiSetCustomDisplay) }; var uiSetDisplayTitle by remember { mutableStateOf(defaults.uiSetDisplayTitle) }
    var uiSetApplyBtn by remember { mutableStateOf(defaults.uiSetApplyBtn) }
    // Error/Dialog/Welcome extras
    var uiErrJsTitle by remember { mutableStateOf(defaults.uiErrJsTitle) }; var uiErrUnknown by remember { mutableStateOf(defaults.uiErrUnknown) }
    var uiDlgDelSave by remember { mutableStateOf(defaults.uiDlgDelSave) }; var uiDlgEditUser by remember { mutableStateOf(defaults.uiDlgEditUser) }
    var uiDlgShareTitle by remember { mutableStateOf(defaults.uiDlgShareTitle) }; var uiDlgPackFail by remember { mutableStateOf(defaults.uiDlgPackFail) }; var uiDlgNoShare by remember { mutableStateOf(defaults.uiDlgNoShare) }
    // 存档模块 + 游戏画面 + settings/sounds 补充
    var saveDraft by remember { mutableStateOf(defaults.save) }
    var gameDisplay by remember { mutableStateOf(defaults.gameDisplay) }
    var uiSetChangeProfile by remember { mutableStateOf(defaults.uiSetChangeProfile) }
    var uiSetShowNotUpdate by remember { mutableStateOf(defaults.uiSetShowNotUpdate) }
    var uiSetExitConfirm by remember { mutableStateOf(defaults.uiSetExitConfirm) }
    var uiSndSwitchClick by remember { mutableStateOf(defaults.uiSndSwitchClick) }
    var sideBgImage by remember { mutableStateOf(defaults.sideBgImage.ifBlank { "game_side_bg.jpg" }) }
    var floatingBallIcon by remember { mutableStateOf(defaults.floatingBallIcon.ifBlank { "ic_floating_dave.png" }) }
    // 窗口填充背景图（@mipmap/bg_fill_image）：替换 App 启动窗口背景，无需输入路径，直接选图替换默认图
    var bgFillImageFile by remember { mutableStateOf<File?>(null) }
    // 更新模式：目标 APK 当前生效的 bg_fill_image（未显式覆盖时默认取自目标，而非工具箱内置图）
    var targetBgFillImage by remember { mutableStateOf<File?>(null) }
    // 更新模式：未显式覆盖背景图时，解出目标 APK 当前生效的 bg_fill_image 供预览（否则沿用工具箱内置默认图会误导）
    LaunchedEffect(sourceMode, targetApk, bgFillImageFile) {
        targetBgFillImage = null
        if (sourceMode != "update" || targetApk == null || bgFillImageFile != null) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            runCatching {
                val cacheDir = context.cacheDir ?: context.filesDir ?: return@withContext
                val fileDir = File(cacheDir, "integrator_target_bg").apply { mkdirs() }
                val out = File(fileDir, "bg_fill_image.jpg")
                val module = ApkModule.loadApkFile(targetApk)
                val ins = module.getInputSource("res/mipmap-hdpi-v4/bg_fill_image.jpg") ?: return@withContext
                ins.openStream().use { inp -> out.outputStream().use { inp.copyTo(it) } }
                targetBgFillImage = out
            }
        }
    }

    /** 读取当前 APK 内置的 anti-distribution.txt 内容作为默认值（即打包时若无修改会落盘的声明文本）。 */
    fun readDefaultAntiDistribution(ctx: Context): String = runCatching {
        ctx.assets.open("pvz2tool/anti-distribution.txt").bufferedReader().use { it.readText() }
    }.getOrDefault("")

    // 进游戏声明文本（anti-distribution.txt）：进入游戏时逐行 Toast，默认预填当前 APK 内置内容，可直接修改
    var antiDistributionText by remember { mutableStateOf(readDefaultAntiDistribution(context)) }

    /** 把全部「来自 dream.yml 默认值的向导字段」重置为给定默认值 d。
     *  不含游戏 Activity、不含流程/UI 导航态/SMF 选择态。 */
    fun applyConfigDefaults(d: DreamDefaults, isUpdateMode: Boolean = false) {
        smfDirectory = d.smfDirectory
        baseAssetPath = d.baseAssetPath.ifBlank { "version/base/smf" }
        simplifiedLaunch = false
        cgVideoPath = d.cgVideoPath.ifBlank { "opening.mp4" }
        cgVideoPoster = d.cgVideoPoster.ifBlank { "bg_main.jpg" }
        cgVideoLoadTimeout = d.cgVideoLoadTimeout.ifBlank { "5000" }
        gameActivityInvalid = "设置的游戏Activity有误或不存在"
        bgImage = d.bgImage.ifBlank { "bg_main.jpg" }; isUseSolidColorBg = d.isUseSolidColorBg
        bgMusic = d.bgMusic.ifBlank { "bg_music.wav" }; isPlayBgMusic = d.isPlayBgMusic
        sideBgImage = d.sideBgImage.ifBlank { "game_side_bg.jpg" }
        floatingBallIcon = d.floatingBallIcon.ifBlank { "ic_floating_dave.png" }
        bgFillImageFile = null
        // 更新模式：进游戏声明由 pickApk 从目标 APK 读取，不在 applyConfigDefaults 中覆盖
        if (!isUpdateMode) antiDistributionText = readDefaultAntiDistribution(context)
        versions = d.versions.ifEmpty { listOf(VersionDraft("new", "正式服", "全新体验", "new_version_icon.png", default = true), VersionDraft("old", "怀旧服", "经典怀旧", "old_version_icon.png")) }
        isExpandedVersions = d.isExpandedVersions; versionsTheme = d.versionsTheme
        sections = d.sections.filter { includeExamples || !it.id.startsWith("example_") }
        announcements = d.announcements.ifEmpty { listOf(AnnouncementDraft("拓展2.5.1(最新版)", "内容{{red:演示的红色内容}}内容"), AnnouncementDraft("拓展2.0.0(怀旧版)", "内容")) }
        showFloatingWindowLabel = d.showFloatingWindowLabel.ifBlank { "是否开启悬浮窗" }
        isShowFloatingWindowDefault = d.isShowFloatingWindowDefault
        fwItems = d.fwItems.ifEmpty { listOf(FwItemDraft(id = "vpn_toggle", name = "{{js:vpn.isActive() ? '恢复网络' : '断开网络'}}", buttonColor = "red", isShowFromJs = "vpn.isPrepared()", jsScript = "vpn.isActive() ? vpn.restore() : vpn.disconnect();"), FwItemDraft(id = "game_display", name = "画面设置", buttonColor = "green", isShowFromJs = "ui.isCustomGameDisplayEnabled()", jsScript = "ui.showGameDisplay();")) }
        fwEmptyTip = d.fwEmptyTip.ifBlank { "（悬浮窗暂无内容，请在 dream.yml 的 ui.floatingWindow.items 中配置）" }
        fwAllHiddenTip = d.fwAllHiddenTip.ifBlank { "（当前没有可用的功能）" }
        exitConfirmTitle = d.exitConfirmTitle.ifBlank { "退出游戏" }
        exitConfirmMessage = d.exitConfirmMessage.ifBlank { "确定要退出游戏吗？" }; isUseExitConfirm = d.isUseExitConfirm
        exitConfirmButtonText = d.exitConfirmButtonText.ifBlank { "确认退出" }
        floatingExitConfirmTitle = d.floatingExitConfirmTitle.ifBlank { "确认退出" }
        floatingExitConfirmMessage = d.floatingExitConfirmMessage.ifBlank { "确定要退出悬浮窗吗(直至重启游戏后显示)？" }
        floatingExitConfirmButtonText = d.floatingExitConfirmButtonText.ifBlank { "确认" }
        tbiItems = d.tbiItems.ifEmpty { listOf(TbiItemDraft(id = "refresh_top", icon = "images/new_version_icon.png", iconPress = "images/new_version_icon.png", contentDescription = "刷新", isShowFromJs = "ui.isCustomGameDisplayEnabled()", jsScript = "ui.refreshAll();"), TbiItemDraft(id = "help_top", icon = "images/new_version_icon.png", contentDescription = "帮助", jsPath = "topbar/help.js")) }
        uiVersionLabel = d.uiVersionLabel; uiUiVersion = d.uiUiVersion; uiAuthorInfo = d.uiAuthorInfo
        uiTutorial = d.uiTutorial; uiNoValidDirTip = d.uiNoValidDirTip
        uiTitleTopAppBar = d.uiTitleTopAppBar; uiTitleAbout = d.uiTitleAbout
        uiTitleCoreFunction = d.uiTitleCoreFunction; uiTitleVersionManage = d.uiTitleVersionManage
        uiBtnEnterGame = d.uiBtnEnterGame; uiBtnTutorial = d.uiBtnTutorial
        uiBtnResetData = d.uiBtnResetData; uiBtnShowFW = d.uiBtnShowFW; uiBtnConfirmVersion = d.uiBtnConfirmVersion
        uiLogPanelTitle = d.uiLogPanelTitle; uiLogCopyDesc = d.uiLogCopyDesc; uiLogClearDesc = d.uiLogClearDesc
        uiLogNoLogText = d.uiLogNoLogText; uiLogPresetSaveLabel = d.uiLogPresetSaveLabel; uiLogLocalSaveLabel = d.uiLogLocalSaveLabel
        uiDialogConfirm = d.uiDialogConfirm; uiDialogCancel = d.uiDialogCancel
        uiWelcomeGreeting = d.uiWelcomeGreeting; uiWelcomeEditTitle = d.uiWelcomeEditTitle; uiWelcomeEditHint = d.uiWelcomeEditHint
        uiExDialogTitle = d.uiExDialogTitle; uiExInitLoadTip = d.uiExInitLoadTip; uiExInitProgTip = d.uiExInitProgTip
        uiExNoNeedTip = d.uiExNoNeedTip; uiExSingleFileTip = d.uiExSingleFileTip; uiExMultiFileTip = d.uiExMultiFileTip
        uiExWaitingTip = d.uiExWaitingTip; uiExCompleteTip = d.uiExCompleteTip
        uiExFailPrefix = d.uiExFailPrefix; uiExSkipPrefix = d.uiExSkipPrefix
        uiExContinueBtn = d.uiExContinueBtn; uiExCompleteBtn = d.uiExCompleteBtn; uiExToastErr = d.uiExToastErr
        uiSndSwitchPress = d.uiSndSwitchPress; uiSndSwitchRelease = d.uiSndSwitchRelease
        uiSndBtnPress = d.uiSndBtnPress; uiSndBtnRelease = d.uiSndBtnRelease
        uiSndSettingsPress = d.uiSndSettingsPress; uiSndSettingsRelease = d.uiSndSettingsRelease
        uiSndXClosePress = d.uiSndXClosePress; uiSndXCloseRelease = d.uiSndXCloseRelease
        uiSndPanelPress = d.uiSndPanelPress; uiSndPanelRelease = d.uiSndPanelRelease
        uiSndSwitchClick = d.uiSndSwitchClick
        uiBtnEnterGameIcon = d.uiBtnEnterGameIcon; uiBtnTutorialIcon = d.uiBtnTutorialIcon; uiBtnResetDataIcon = d.uiBtnResetDataIcon
        uiSetTitle = d.uiSetTitle; uiSetSolidBg = d.uiSetSolidBg; uiSetPlayMusic = d.uiSetPlayMusic
        uiSetImportSmf = d.uiSetImportSmf; uiSetReload = d.uiSetReload; uiSetResetSmf = d.uiSetResetSmf
        uiSetCustomDisplay = d.uiSetCustomDisplay; uiSetDisplayTitle = d.uiSetDisplayTitle; uiSetApplyBtn = d.uiSetApplyBtn
        uiErrJsTitle = d.uiErrJsTitle; uiErrUnknown = d.uiErrUnknown
        uiDlgDelSave = d.uiDlgDelSave; uiDlgEditUser = d.uiDlgEditUser; uiDlgShareTitle = d.uiDlgShareTitle
        uiDlgPackFail = d.uiDlgPackFail; uiDlgNoShare = d.uiDlgNoShare
        uiSetChangeProfile = d.uiSetChangeProfile; uiSetShowNotUpdate = d.uiSetShowNotUpdate; uiSetExitConfirm = d.uiSetExitConfirm
        saveDraft = d.save; gameDisplay = d.gameDisplay
        schedules = d.schedules
    }

    // 更新模式：选完目标 APK 后，用目标 dream.yml 作为向导默认值（覆盖内置模板）；切回内置模式则恢复内置默认。
    // 仅在进入向导填写前触发（pickApk 之后），不会清空用户在向导内已编辑的内容。
    LaunchedEffect(sourceMode, targetDreamYmlRaw) {
        applyConfigDefaults(loadDefaultsFromDreamYml(effectiveDefaultsRaw), isUpdateMode = sourceMode == "update")
    }

    // 全屏图片预览浮层状态：点缩略图/背景图预览后设置（Coil model），置 null 关闭
    var previewModel by remember { mutableStateOf<Any?>(null) }
    val openImagePreview: (Any) -> Unit = { previewModel = it }
    val openImagePreviewFromResource: () -> Unit = { previewModel = R.mipmap.bg_fill_image }

    // ── 预览模式状态 ──
    var isPreviewing by remember { mutableStateOf(false) }
    // ── 解析 dream.yml 为 Pvz2ToolConfig（预览用）──
    // 基于向导状态实时构建，随任何编辑即时更新
    val previewYml by remember {
        derivedStateOf {
            if (dreamYmlRaw.isEmpty() && targetDreamYmlRaw.isNullOrEmpty()) null
            else buildYamlFromWizard(
                effectiveDefaultsRaw,
                gameActivity, smfDirectory, baseAssetPath, simplifiedLaunch,
                cgVideoPath, cgVideoPoster, cgVideoLoadTimeout, gameActivityInvalid,
                versions, sections, announcements,
                isExpandedVersions, versionsTheme,
                fwItems, tbiItems,
                bgImage, isUseSolidColorBg, bgMusic, isPlayBgMusic,
                sideBgImage, floatingBallIcon,
                showFloatingWindowLabel, isShowFloatingWindowDefault,
                fwEmptyTip, fwAllHiddenTip,
                exitConfirmTitle, exitConfirmMessage, isUseExitConfirm,
                exitConfirmButtonText, floatingExitConfirmTitle,
                floatingExitConfirmMessage, floatingExitConfirmButtonText,
                uiVersionLabel, uiUiVersion, uiAuthorInfo, uiTutorial, uiNoValidDirTip,
                uiTitleTopAppBar, uiTitleAbout, uiTitleCoreFunction, uiTitleVersionManage,
                uiBtnEnterGame, uiBtnTutorial, uiBtnResetData, uiBtnShowFW, uiBtnConfirmVersion,
                uiLogPanelTitle, uiDialogConfirm, uiDialogCancel, uiWelcomeGreeting,
                uiExDialogTitle, uiExInitLoadTip, uiExInitProgTip, uiExNoNeedTip,
                uiExSingleFileTip, uiExMultiFileTip, uiExWaitingTip, uiExCompleteTip,
                uiExFailPrefix, uiExSkipPrefix, uiExContinueBtn, uiExCompleteBtn, uiExToastErr,
                uiSndSwitchPress, uiSndSwitchRelease, uiSndBtnPress, uiSndBtnRelease,
                uiSndSettingsPress, uiSndSettingsRelease, uiSndXClosePress, uiSndXCloseRelease,
                uiSndPanelPress, uiSndPanelRelease,
                uiBtnEnterGameIcon, uiBtnTutorialIcon, uiBtnResetDataIcon,
                uiSetTitle, uiSetSolidBg, uiSetPlayMusic, uiSetImportSmf, uiSetReload,
                uiSetResetSmf, uiSetCustomDisplay, uiSetDisplayTitle, uiSetApplyBtn,
                uiErrJsTitle, uiErrUnknown,
                uiLogCopyDesc, uiLogClearDesc, uiLogNoLogText, uiLogPresetSaveLabel, uiLogLocalSaveLabel,
                uiDlgDelSave, uiDlgEditUser, uiDlgShareTitle, uiDlgPackFail, uiDlgNoShare,
                uiWelcomeEditTitle, uiWelcomeEditHint,
                saveDraft, gameDisplay, uiSetChangeProfile, uiSetShowNotUpdate, uiSetExitConfirm, uiSndSwitchClick,
                schedules = schedules,
                isUpdateMode = sourceMode == "update"
            )
        }
    }
    val parsedConfig = remember(previewYml) {
        previewYml?.let {
            runCatching {
                lenientYaml.decodeFromString(Pvz2ToolConfig.serializer(), it)
            }.getOrNull()
        }
    }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    fun pickApk() {
        filePickerManager.launch(isDirectory = false, fileMimeType = "application/vnd.android.package-archive") { uri, doc ->
            if (uri == null) { toast("未选择文件"); return@launch }
            val name = doc?.name ?: "target.apk"
            scope.launch {
                JsUiManager.showLoading("正在读取 APK", "请稍候，正在复制所选 APK 到工作目录…")
                val res = withContext(Dispatchers.IO) {
                    runCatching {
                        val cacheDir = context.cacheDir ?: context.filesDir ?: throw IllegalStateException("无可用缓存目录")
                        val cache = File(cacheDir, "integrator_apk").apply { mkdirs() }
                        val out = File(cache, name)
                        context.contentResolver.openInputStream(uri)?.use { inp -> out.outputStream().use { inp.copyTo(it) } }
                        // 一键检测：游戏入口 Activity + 是否含 kotlin（决定 dex 默认策略）
                        val info = ToolboxApkMerger.detectTarget(out)
                        out to info
                    }
                }
                res.onFailure { e -> toast("读取 APK 失败：${e.message}") }
                res.onSuccess { (out, info) ->
                    // 签名检测：拒绝 PopCap Games 原版 APK
                    val certDN = withContext(Dispatchers.IO) {
                        runCatching {
                            java.util.zip.ZipFile(out).use { zip ->
                                val sigEntry = zip.entries().asSequence()
                                    .firstOrNull { it.name.startsWith("META-INF/") && (it.name.endsWith(".RSA") || it.name.endsWith(".DSA")) }
                                    ?: return@runCatching ""
                                val certBytes = zip.getInputStream(sigEntry).use { it.readBytes() }
                                val cert = java.security.cert.CertificateFactory.getInstance("X.509")
                                    .generateCertificates(java.io.ByteArrayInputStream(certBytes))
                                    .firstOrNull()
                                (cert as? java.security.cert.X509Certificate)?.subjectX500Principal?.name ?: ""
                            }
                        }.getOrDefault("")
                    }
                    if ("OU=PopCap Games" in certDN || "OU=PopCap" in certDN) {
                        JsUiManager.hideLoading()
                        popCapWarningFile = out
                        return@onSuccess
                    }

                    // 工具箱特征检测：检查 APK 是否包含 assets/pvz2tool/ 目录
                    val hasToolbox = withContext(Dispatchers.IO) {
                        runCatching {
                            java.util.zip.ZipFile(out).use { zip ->
                                zip.entries().asSequence().any { it.name.startsWith("assets/pvz2tool/") }
                            }
                        }.getOrDefault(false)
                    }
                    if (sourceMode == "update" && !hasToolbox) {
                        JsUiManager.hideLoading()
                        toolboxModeWarning = "update_wrong"
                        return@onSuccess
                    }
                    if (sourceMode == "integrate" && hasToolbox) {
                        JsUiManager.hideLoading()
                        toolboxModeWarning = "integrate_wrong"
                        return@onSuccess
                    }

                    targetApk = out
                    report = null; result = null
                    gameActivity = info.gameActivity
                    // 默认选择：目标含 kotlin → 插入之前（新版推荐）；否则 → 追加之后（老版推荐）
                    dexStrategy = if (info.hasKotlin) DexStrategy.INSERT_BEFORE else DexStrategy.APPEND

                    // 更新模式：检测目标 APK 内的集成描述文件，并读取其 dream.yml 作为向导模板
                    if (sourceMode == "update") {
                        detectedInfo = ToolboxApkMerger.detectIntegratorInfo(out)
                        targetDreamYmlRaw = runCatching {
                            ApkModule.loadApkFile(out)
                                .getInputSource("assets/pvz2tool/dream.yml")
                                ?.openStream()?.readBytes()?.toString(Charsets.UTF_8)
                        }.getOrNull()
                        // 同时读取目标 APK 的进游戏声明文本（anti-distribution.txt）
                        runCatching {
                            ApkModule.loadApkFile(out)
                                .getInputSource("assets/pvz2tool/anti-distribution.txt")
                                ?.openStream()?.readBytes()?.toString(Charsets.UTF_8)
                        }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { antiDistributionText = it }
                        detectedInfo?.let { di ->
                            // 描述文件内部的 dexStart/dexEnd 是 0-based，UI 使用 1-based 序号
                            updDexStart = (di.dexStart + 1).toString()
                            updDexEnd = (di.dexEnd + 1).toString()
                            updInsertMode = if (di.insertMode == "after") DexStrategy.APPEND else DexStrategy.INSERT_BEFORE
                            includeExamples = di.includeExamples
                            simplifiedLaunch = di.simplifiedLaunch
                        } ?: run {
                            // 无描述文件：扫描目标 APK 第一个 >20MB 的 DEX 序号作为默认值
                            val idx = findLargeDexIndex(out)
                            updDexStart = idx.toString()
                            updDexEnd = ""
                            // 起始序号=1（第一个 DEX）→ 插之前；否则 → 追之后
                            updInsertMode = if (idx <= 1) DexStrategy.INSERT_BEFORE else DexStrategy.APPEND
                            // 无描述文件视为旧版集成，示例项目默认关闭，pvz2tool 仅替换已有文件
                            includeExamples = false
                            appendUnreferencedAssets = false
                        }
                    } else {
                        detectedInfo = null
                        targetDreamYmlRaw = null
                    }
                }
                JsUiManager.hideLoading()
            }
        }
    }

    /** 根据 fieldKey 推断文件在 APK 中的子目录 */
    fun fieldKeyToApkSubDir(fieldKey: String): String = when {
        fieldKey == "antiDistribution" -> ""                  // anti-distribution.txt 直接落在 assets/pvz2tool/ 下
        fieldKey.contains("icon") || fieldKey.contains("Icon") ||
        fieldKey in setOf("bgImage", "sideBgImage", "floatingBallIcon") -> "images/"
        fieldKey.contains("sound") || fieldKey.contains("Sound") ||
        fieldKey.contains("Music") || fieldKey == "bgMusic" -> "sound/"
        fieldKey.startsWith("cgVideo") -> "video/"
        else -> ""
    }

    /** 从向导状态解析 fieldKey 对应的目标文件名（= 用户输入的字段值，保留用户输入的父目录如 tool/a.js） */
    fun resolveFieldFileName(fieldKey: String): String {
        // 仅剥除与 fieldKey 对应的标准子目录前缀（如 images/、js/），保留用户输入的额外父目录层级
        val subdir = fieldKeyToApkSubDir(fieldKey)
        fun strip(raw: String): String =
            if (subdir.isNotEmpty() && raw.startsWith(subdir)) raw.removePrefix(subdir) else raw
        // 版本级字段: ver_N_field
        val verMatch = Regex("^ver_(\\d+)_(.+)$").matchEntire(fieldKey)
        if (verMatch != null) {
            val idx = verMatch.groupValues[1].toIntOrNull() ?: -1
            val field = verMatch.groupValues[2]
            if (idx in versions.indices) {
                val v = versions[idx]
                return when (field) {
                    "icon" -> strip(v.icon)
                    "enterGamePath" -> strip(v.enterGamePath)
                    else -> ""
                }
            }
        }
        // 栏目级字段: section_N_field
        val secMatch = Regex("^section_(\\d+)_(.+)$").matchEntire(fieldKey)
        if (secMatch != null) {
            val idx = secMatch.groupValues[1].toIntOrNull() ?: -1
            val field = secMatch.groupValues[2]
            if (idx in sections.indices) {
                return when (field) {
                    "jsPath" -> strip(sections[idx].jsPath)
                    else -> ""
                }
            }
        }
        // 功能项级字段: secitem_SI_I_field
        val itemMatch = Regex("^secitem_(\\d+)_(\\d+)_(.+)$").matchEntire(fieldKey)
        if (itemMatch != null) {
            val si = itemMatch.groupValues[1].toIntOrNull() ?: -1
            val ii = itemMatch.groupValues[2].toIntOrNull() ?: -1
            val field = itemMatch.groupValues[3]
            if (si in sections.indices && ii in sections[si].items.indices) {
                val item = sections[si].items[ii]
                return when (field) {
                    "icon" -> strip(item.icon)
                    "jsPath" -> strip(item.jsPath)
                    "isShowFromJsPath" -> strip(item.isShowFromJsPath)
                    else -> ""
                }
            }
        }
        // 悬浮窗项: fw_item_N_field
        val fwMatch = Regex("^fw_item_(\\d+)_(.+)$").matchEntire(fieldKey)
        if (fwMatch != null) {
            val idx = fwMatch.groupValues[1].toIntOrNull() ?: -1
            val field = fwMatch.groupValues[2]
            if (idx in fwItems.indices) {
                return when (field) {
                    "icon" -> strip(fwItems[idx].icon)
                    "jsPath" -> strip(fwItems[idx].jsPath)
                    "isShowFromJsPath" -> strip(fwItems[idx].isShowFromJsPath)
                    "desc" -> strip(fwItems[idx].desc)
                    else -> ""
                }
            }
        }
        // 顶栏图标项: tbi_item_N_field
        val tbiMatch = Regex("^tbi_item_(\\d+)_(.+)$").matchEntire(fieldKey)
        if (tbiMatch != null) {
            val idx = tbiMatch.groupValues[1].toIntOrNull() ?: -1
            val field = tbiMatch.groupValues[2]
            if (idx in tbiItems.indices) {
                val item = tbiItems[idx]
                return when (field) {
                    "icon" -> strip(item.icon)
                    "iconPress" -> strip(item.iconPress)
                    "jsPath" -> strip(item.jsPath)
                    "isShowFromJsPath" -> strip(item.isShowFromJsPath)
                    "pressSound" -> strip(item.pressSound)
                    "releaseSound" -> strip(item.releaseSound)
                    else -> ""
                }
            }
        }
        // 顶层简单字段
        val raw = when (fieldKey) {
            "antiDistribution" -> "anti-distribution.txt"   // 固定文件名
            "cgVideoPath" -> cgVideoPath
            "cgVideoPoster" -> cgVideoPoster
            "bgImage" -> bgImage
            "sideBgImage" -> sideBgImage
            "floatingBallIcon" -> floatingBallIcon
            "bgMusic" -> bgMusic
            else -> ""
        }
        return strip(raw)
    }

    /**
     * 计算某字段在 APK 中的完整目标路径（与 computeExtraResources / doApply 落盘路径一致），用于覆盖检测。
     * 字段未填写文件名时返回 null（此时无法判定路径，不做覆盖检测）。
     */
    fun resolveApkTargetPath(fieldKey: String): String? {
        val name = resolveFieldFileName(fieldKey).trim()
        if (name.isBlank()) return null
        val sub = fieldKeyToApkSubDir(fieldKey)
        return "assets/pvz2tool/$sub$name"
    }

    /**
     * 选择文件时的覆盖风险检测：该字段的 APK 目标路径是否已被占用。
     * 占用条件：① 被其他已选字段复用（同一路径多次使用）；② 已存在于基础 APK（非本次选择造成）。
     * 返回被占用的完整路径（用于二次确认提示），无风险返回 null。
     */
    fun overwriteTargetPath(fieldKey: String): String? {
        val current = resolveApkTargetPath(fieldKey) ?: return null
        val occupiedByOther = (selectedFiles.keys + selectedFolders.keys)
            .asSequence()
            .filter { it != fieldKey }
            .any { resolveApkTargetPath(it) == current }
        val occupiedInBase = baseApkEntries.contains(current)
        return if (occupiedByOther || occupiedInBase) current else null
    }

    /** 将图片文件转成 JPEG（@mipmap/bg_fill_image 资源表写死 .jpg 扩展名，必须保证是有效 JPEG）。解码失败则返回原文件。 */
    fun convertImageToJpeg(file: File): File {
        val bmp = BitmapFactory.decodeFile(file.absolutePath) ?: return file
        val out = File(file.parentFile, "${file.nameWithoutExtension}_bgfill.jpg")
        runCatching { out.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) } }
        bmp.recycle()
        return if (out.exists() && out.length() > 0) out else file
    }

    /** 字段对应的「config 原值文本」（与 buildYamlFromWizard 写入一致），用于本地目录落地 */
    fun resolveFieldRawText(fieldKey: String): String {
        Regex("""^ver_(\d+)_icon$""").matchEntire(fieldKey)?.let { m ->
            val idx = m.groupValues[1].toIntOrNull() ?: -1
            if (idx in versions.indices) return versions[idx].icon
        }
        Regex("""^secitem_(\d+)_(\d+)_icon$""").matchEntire(fieldKey)?.let { m ->
            val si = m.groupValues[1].toIntOrNull() ?: -1
            val ii = m.groupValues[2].toIntOrNull() ?: -1
            if (si in sections.indices && ii in sections[si].items.indices) return sections[si].items[ii].icon
        }
        Regex("""^tbi_item_(\d+)_icon$""").matchEntire(fieldKey)?.let { m ->
            val idx = m.groupValues[1].toIntOrNull() ?: -1
            if (idx in tbiItems.indices) return tbiItems[idx].icon
        }
        Regex("""^tbi_item_(\d+)_iconPress$""").matchEntire(fieldKey)?.let { m ->
            val idx = m.groupValues[1].toIntOrNull() ?: -1
            if (idx in tbiItems.indices) return tbiItems[idx].iconPress
        }
        Regex("""^fw_item_(\d+)_icon$""").matchEntire(fieldKey)?.let { m ->
            val idx = m.groupValues[1].toIntOrNull() ?: -1
            if (idx in fwItems.indices) return fwItems[idx].icon
        }
        return when (fieldKey) {
            "bgImage" -> bgImage
            "sideBgImage" -> sideBgImage
            "floatingBallIcon" -> floatingBallIcon
            "cgVideoPoster" -> cgVideoPoster
            else -> ""
        }
    }

    /** 计算文件夹选择的目标基准路径：标准子目录 + value 中的父目录名（如 scripts/tool） */
    fun folderTargetBase(fieldKey: String): String {
        val subdir = fieldKeyToApkSubDir(fieldKey)
        val raw = resolveFieldRawText(fieldKey)
        val rawStripped = if (subdir.isNotEmpty() && raw.startsWith(subdir)) raw.removePrefix(subdir) else raw
        val parentDir = if (rawStripped.contains("/")) rawStripped.substringBeforeLast("/") else ""
        return (subdir + parentDir).trimEnd('/')
    }

    /**
     * 汇总「即将注入 APK 的追加文件」，区分 assets 与 res 两条通道。
     * 供 doApply（实际打包）与 computePreview（差异预览）共用，保证预览清单与实际注入一致。
     * 返回：(assets/pvz2tool/ 下相对路径 → 本地文件, res/ 下相对路径 → 本地文件)
     */
    fun computeExtraResources(): Pair<Map<String, File>, Map<String, File>> {
        val baseFiles = selectedFiles.toMutableMap()
        // anti-distribution.txt：由文本框内容生成临时文件，注入 assets/pvz2tool/anti-distribution.txt
        if (antiDistributionText.isNotBlank()) {
            val dir = context.cacheDir ?: context.filesDir ?: context.cacheDir!!
            val tmp = File(dir, "integrator_anti_distribution.txt").apply {
                parentFile?.mkdirs(); writeText(antiDistributionText)
            }
            baseFiles["antiDistribution"] = tmp
        }
        val extraAssets = mutableMapOf<String, File>()
        val extraRes = mutableMapOf<String, File>()
        for ((k, file) in baseFiles) {
            val dir = fieldKeyToApkSubDir(k)
            var name = resolveFieldFileName(k)
            // 含运行时占位符（$WORK_DIR / $SMF 等）的字段：占位符路径运行时解析自文件系统，
            // 不应打包到 APK 内；降级使用选择器返回的原始文件名，并跳过以 $ 开头的。
            if (name.startsWith("$")) name = pendingFileNames[k] ?: ""
            if (name.isEmpty() || name.startsWith("$")) continue
            val apkPath = dir + name
            if (apkPath.startsWith("res/")) extraRes[apkPath.removePrefix("res/")] = file
            else extraAssets[apkPath] = file
        }
        // 文件夹选择：展开整个 tree 注入（按 value 父目录名重命名，如 scripts/tool/...）
        for ((fieldKey, folder) in selectedFolders) {
            val base = folderTargetBase(fieldKey)
            folder.walkTopDown().filter { it.isFile }.forEach { f ->
                val rel = f.relativeTo(folder).path.replace(File.separatorChar, '/')
                val apkPath = "$base/$rel"
                if (apkPath.startsWith("res/")) extraRes[apkPath.removePrefix("res/")] = f
                else extraAssets[apkPath] = f
            }
        }
        // @mipmap/bg_fill_image：资源表写死 .jpg，把用户所选图片转成 JPEG 再注入 res/mipmap-hdpi-v4/bg_fill_image.jpg（未选则沿用 APK 内置默认）
        bgFillImageFile?.let {
            extraRes["mipmap-hdpi-v4/bg_fill_image.jpg"] = convertImageToJpeg(it)
        }
        // SMF/资源设置：追加的文件（key 已是相对 assets/pvz2tool/ 的路径）→ 直接注入
        for ((rel, file) in addedSmfFiles) {
            extraAssets[rel] = file
        }
        // SMF/资源设置：追加的文件夹（key=scope 资源目录）→ 展开整个 tree 注入到该 scope 下
        for ((scope, folder) in addedSmfFolders) {
            folder.walkTopDown().filter { it.isFile }.forEach { f ->
                val rel = f.relativeTo(folder).path.replace(File.separatorChar, '/')
                val apkPath = "$scope/$rel".trimEnd('/')
                extraAssets[apkPath] = f
            }
        }
        return extraAssets to extraRes
    }

    fun computePreview() {
        val t = targetApk ?: return
        scope.launch {
            loading = true; errorMsg = null
            JsUiManager.showLoading("正在计算差异", "请稍候，正在分析 APK 差异…")
            val res = withContext(Dispatchers.IO) {
                val (extraAssets, extraRes) = computeExtraResources()
                val isUpdateMode = sourceMode == "update"
                val rawStart = updDexStart.toIntOrNull() ?: 1     // 1-based UI 值
                val effStart = rawStart - 1                         // 内部 0-based
                val effEnd = run {
                    val rawEnd = updDexEnd.toIntOrNull()
                    if (rawEnd == null) effStart + sourceDexCount - 1   // 留空 = 覆盖到本版本所有 DEX
                    else (rawEnd - 1).coerceAtLeast(effStart)           // 用户填写（1-based → 0-based）
                }
                runCatching {
                    // 更新模式下 bg_fill_image 的保留由合并引擎统一处理（res 全量替换、仅跳过 bg_fill_image），此处无需重复计算
                    val preserveTargetRes = emptySet<String>()
                    ToolboxApkMerger.preview(
                        sourceApk, t, dexStrategy, extraAssets, extraRes, excludedSmfAssets, removedTargetEntries,
                        version = sourceVersion, updateMode = isUpdateMode, dexStart = effStart, dexEnd = effEnd,
                        overrideDreamYml = previewYml, preserveTargetResEntries = preserveTargetRes,
                        preserveTargetAssets = isUpdateMode, appendUnreferenced = !isUpdateMode || appendUnreferencedAssets
                    )
                }
            }
            res.onFailure { errorMsg = "计算差异失败：${it.message}" }
            report = res.getOrNull()
            loading = false
            JsUiManager.hideLoading()
        }
    }

    fun doApply() {
        val t = targetApk ?: return
        scope.launch {
            loading = true; errorMsg = null
            JsUiManager.showLoading("正在打包", "请稍候，正在合并 APK…")
            val res = withContext(Dispatchers.IO) {
                runCatching {
                    val outName = t.name.removeSuffix(".apk") + "_pvz2tool.apk"
                    val base = context.cacheDir ?: context.filesDir ?: throw IllegalStateException("无可用缓存目录")
                    val out = File(base, "integrator_out/$outName")
                    val overrideYml = buildYamlFromWizard(
                        effectiveDefaultsRaw,
                        gameActivity, smfDirectory, baseAssetPath, simplifiedLaunch,
                        cgVideoPath, cgVideoPoster, cgVideoLoadTimeout, gameActivityInvalid,
                        versions, sections, announcements,
                        isExpandedVersions, versionsTheme,
                        fwItems, tbiItems,
                        bgImage, isUseSolidColorBg, bgMusic, isPlayBgMusic,
                        sideBgImage, floatingBallIcon,
                        showFloatingWindowLabel, isShowFloatingWindowDefault,
                        fwEmptyTip, fwAllHiddenTip,
                        exitConfirmTitle, exitConfirmMessage, isUseExitConfirm,
                        exitConfirmButtonText, floatingExitConfirmTitle,
                        floatingExitConfirmMessage, floatingExitConfirmButtonText,
                        uiVersionLabel, uiUiVersion, uiAuthorInfo, uiTutorial, uiNoValidDirTip,
                        uiTitleTopAppBar, uiTitleAbout, uiTitleCoreFunction, uiTitleVersionManage,
                        uiBtnEnterGame, uiBtnTutorial, uiBtnResetData, uiBtnShowFW, uiBtnConfirmVersion,
                        uiLogPanelTitle, uiDialogConfirm, uiDialogCancel, uiWelcomeGreeting,
                        uiExDialogTitle, uiExInitLoadTip, uiExInitProgTip, uiExNoNeedTip,
                        uiExSingleFileTip, uiExMultiFileTip, uiExWaitingTip, uiExCompleteTip,
                        uiExFailPrefix, uiExSkipPrefix, uiExContinueBtn, uiExCompleteBtn, uiExToastErr,
                        uiSndSwitchPress, uiSndSwitchRelease, uiSndBtnPress, uiSndBtnRelease,
                        uiSndSettingsPress, uiSndSettingsRelease, uiSndXClosePress, uiSndXCloseRelease,
                        uiSndPanelPress, uiSndPanelRelease,
                        uiBtnEnterGameIcon, uiBtnTutorialIcon, uiBtnResetDataIcon,
                        uiSetTitle, uiSetSolidBg, uiSetPlayMusic, uiSetImportSmf, uiSetReload,
                        uiSetResetSmf, uiSetCustomDisplay, uiSetDisplayTitle, uiSetApplyBtn,
                        uiErrJsTitle, uiErrUnknown,
                        uiLogCopyDesc, uiLogClearDesc, uiLogNoLogText, uiLogPresetSaveLabel, uiLogLocalSaveLabel,
                        uiDlgDelSave, uiDlgEditUser, uiDlgShareTitle, uiDlgPackFail, uiDlgNoShare,
                        uiWelcomeEditTitle, uiWelcomeEditHint,
                        saveDraft, gameDisplay, uiSetChangeProfile, uiSetShowNotUpdate, uiSetExitConfirm, uiSndSwitchClick,
                        schedules = schedules,
                        isUpdateMode = sourceMode == "update"
                    )
                    // 组装额外资源：区分 assets 与 res 两条注入通道（与预览共用同一逻辑，保证清单一致）
                    val (extraAssets, extraRes) = computeExtraResources()
                    val isUpdateMode = sourceMode == "update"
                    val rawStart = updDexStart.toIntOrNull() ?: 1     // 1-based UI 值
                    val effStart = rawStart - 1                         // 内部 0-based
                    val effEnd = run {
                        val rawEnd = updDexEnd.toIntOrNull()
                        if (rawEnd == null) effStart + sourceDexCount - 1 else (rawEnd - 1).coerceAtLeast(effStart)
                    }
                    // 更新模式且用户未显式覆盖背景图时，保留目标 APK 现有 bg_fill_image（跳过源 APK 覆盖）
                    val preserveTargetRes = if (isUpdateMode && !extraRes.containsKey("mipmap-hdpi-v4/bg_fill_image.jpg"))
                        setOf("res/mipmap-hdpi-v4/bg_fill_image.jpg") else emptySet()
                    ToolboxApkMerger.apply(sourceApk, t, dexStrategy, out, includeExamples,
                        overrideDreamYml = overrideYml,
                        extraResources = extraAssets,
                        extraResResources = extraRes,
                        excludedSmfAssets = excludedSmfAssets,
                        removedTargetEntries = removedTargetEntries,
                        version = sourceVersion, updateMode = isUpdateMode, dexStart = effStart, dexEnd = effEnd, insertMode = updInsertMode,
                        preserveTargetResEntries = preserveTargetRes,
                        simplifiedLaunch = simplifiedLaunch,
                        preserveTargetAssets = isUpdateMode,
                        appendUnreferenced = !isUpdateMode || appendUnreferencedAssets)
                }
            }
            res.onFailure { errorMsg = "合并失败：${it.message}"; it.printStackTrace() }
            result = res.getOrNull()
            loading = false
            JsUiManager.hideLoading()
            if (res.isSuccess) {
                toast("打包完成：${t.name.removeSuffix(".apk")}_pvz2tool.apk")
                // 打包成功后删除已登记的本地文件/文件夹（SAF）
                val deletions = pendingLocalDeletions
                if (deletions.isNotEmpty()) {
                    pendingLocalDeletions = emptyList()
                    withContext(Dispatchers.IO) {
                        deletions.forEach { (uriStr, label) ->
                            val doc = runCatching { DocumentFile.fromSingleUri(context, android.net.Uri.parse(uriStr)) }.getOrNull()
                            if (doc != null) {
                                val ok = runCatching { doc.delete() }.getOrDefault(false)
                                if (ok) Toast.makeText(context, "已删除原文件：$label", Toast.LENGTH_SHORT).show()
                                else Toast.makeText(context, "原文件删除失败（来源不可写）：$label", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    /** 通用文件选择器。选中文件复制到缓存，后续合并时打包到 APK 对应路径。 */
    fun pickAnyFile(fieldKey: String, mimeType: String) {
        filePickerManager.launch(isDirectory = false, fileMimeType = mimeType) { uri, doc ->
            if (uri == null || doc == null) return@launch
            val name = doc.name ?: "file"
            val cacheDir = context.cacheDir ?: context.filesDir ?: return@launch
            val fileDir = File(cacheDir, "integrator_files").apply { mkdirs() }
            val out = File(fileDir, "${fieldKey}_${name}")
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { inp -> out.outputStream().use { inp.copyTo(it) } }
            }.onSuccess {
                selectedFiles = selectedFiles.toMutableMap().apply { put(fieldKey, out) }
                pendingFileNames = pendingFileNames.toMutableMap().apply { put(fieldKey, name) }
                selectedFolders = selectedFolders.minus(fieldKey)
                Toast.makeText(context, "已选择：$name", Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                Toast.makeText(context, "读取文件失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** 清除某字段的文件/文件夹选择（供 FileInputRow 在用户输入变化导致选择失效时调用） */
    fun clearFieldSelection(fieldKey: String) {
        selectedFiles = selectedFiles.minus(fieldKey)
        selectedFolders = selectedFolders.minus(fieldKey)
    }

    /** 将 SAF 目录树递归拷贝到本地 File 目录（用于文件夹选择） */
    fun copyDocumentTreeToDir(doc: DocumentFile, dir: File) {
        if (!doc.isDirectory) return
        dir.mkdirs()
        doc.listFiles().forEach { child ->
            val name = child.name ?: return@forEach
            if (child.isDirectory) {
                copyDocumentTreeToDir(child, File(dir, name))
            } else {
                runCatching {
                    context.contentResolver.openInputStream(child.uri)?.use { inp ->
                        File(dir, name).outputStream().use { inp.copyTo(it) }
                    }
                }
            }
        }
    }

    /** 选择文件夹：选中整个目录树，按 value 中父目录名（如 tool）重命名后打包到 APK 工作目录；同时清除该字段的单文件选择。 */
    fun pickFolder(fieldKey: String) {
        filePickerManager.launch(isDirectory = true, fileMimeType = "*/*") { uri, doc ->
            if (uri == null || doc == null) return@launch
            val folderName = doc.name ?: "folder"
            val cacheDir = context.cacheDir ?: context.filesDir ?: return@launch
            val out = File(cacheDir, "integrator_folders/${fieldKey}_$folderName").apply { deleteRecursively(); mkdirs() }
            copyDocumentTreeToDir(doc, out)
            selectedFolders = selectedFolders.toMutableMap().apply { put(fieldKey, out) }
            selectedFiles = selectedFiles.minus(fieldKey)
            Toast.makeText(context, "已选择文件夹：$folderName", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 「选择后删除」：删除 SAF 选中的本地原件（文件或整个目录）。
     * 内容已完整复制进缓存后才调用。删除依赖 provider 支持 FLAG_SUPPORTS_DELETE，
     * 失败（如只读来源、云盘文档）时仅提示，不影响已追加的资源。
     */
    fun deletePickedSource(doc: DocumentFile, label: String) {
        val ok = runCatching { doc.delete() }.getOrDefault(false)
        if (ok) Toast.makeText(context, "已删除原文件：$label", Toast.LENGTH_SHORT).show()
        else Toast.makeText(context, "原文件删除失败（来源不可写）：$label", Toast.LENGTH_LONG).show()
    }

    /** SMF/资源设置：追加单个文件到某 scope 资源目录（key = 相对 assets/pvz2tool/ 的路径）。 */
    fun pickSmfFile(scope: String) {
        filePickerManager.launch(isDirectory = false, fileMimeType = "*/*") { uri, doc ->
            if (uri == null || doc == null) return@launch
            val name = doc.name ?: "file"
            val cacheDir = context.cacheDir ?: context.filesDir ?: return@launch
            val fileDir = File(cacheDir, "integrator_smf_files").apply { mkdirs() }
            val out = File(fileDir, "${scope.replace('/', '_')}_$name")
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { inp -> out.outputStream().use { inp.copyTo(it) } }
            }.onSuccess {
                val rel = "$scope/$name".trimEnd('/')
                addedSmfFiles = addedSmfFiles.toMutableMap().apply { put(rel, out) }
                localFileUris = localFileUris + (rel to uri.toString())
                if (smfDeleteAfterPick) {
                    pendingLocalDeletions = pendingLocalDeletions + PendingLocalDeletion(uri.toString(), name)
                    Toast.makeText(context, "已追加文件：$rel（打包后删除原文件）", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "已追加文件：$rel", Toast.LENGTH_SHORT).show()
                }
            }.onFailure { e ->
                Toast.makeText(context, "读取文件失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** SMF/资源设置：追加整个文件夹到某 scope 资源目录（key = scope，多个文件夹合并进同一缓存目录）。 */
    fun pickSmfFolder(scope: String) {
        filePickerManager.launch(isDirectory = true, fileMimeType = "*/*") { uri, doc ->
            if (uri == null || doc == null) return@launch
            val folderName = doc.name ?: "folder"
            val cacheDir = context.cacheDir ?: context.filesDir ?: return@launch
            val target = addedSmfFolders[scope]
                ?: File(cacheDir, "integrator_smf_folders/${scope.replace('/', '_')}_$folderName").apply { deleteRecursively(); mkdirs() }
            copyDocumentTreeToDir(doc, target)
            addedSmfFolders = addedSmfFolders.toMutableMap().apply { put(scope, target) }
            localFolderUris = localFolderUris + (scope to uri.toString())
            if (smfDeleteAfterPick) {
                pendingLocalDeletions = pendingLocalDeletions + PendingLocalDeletion(uri.toString(), folderName)
                Toast.makeText(context, "已追加文件夹到：$scope（打包后删除原目录）", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "已追加文件夹到：$scope", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * SMF/资源设置：把目标 APK 内的一个条目复制为 SMF 资源。
     * 内容解出到缓存后注册进 addedSmfFiles（落到 assets/pvz2tool/<scope>/<文件名>）；
     * 若「选择后删除」开启且该条目非受保护条目，同时登记删除 —— 打包时从产物中移除原条目。
     */
    fun addEntryFromTargetApk(assetScope: String, entryName: String) {
        val t = targetApk ?: return
        val fileName = entryName.substringAfterLast('/')
        val cacheDir = context.cacheDir ?: context.filesDir ?: return
        scope.launch {
            val fileDir = File(cacheDir, "integrator_smf_files").apply { mkdirs() }
            val out = File(fileDir, "${assetScope.replace('/', '_')}_$fileName")
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    ApkModule.loadApkFile(t).use { module ->
                        val ins = module.getInputSource(entryName) ?: error("条目不存在：$entryName")
                        ins.openStream().use { inp -> out.outputStream().use { inp.copyTo(it) } }
                    }
                }.isSuccess
            }
            if (!ok) {
                toast("解出失败：$entryName")
                return@launch
            }
            val rel = "$assetScope/$fileName".trimEnd('/')
            addedSmfFiles = addedSmfFiles.toMutableMap().apply { put(rel, out) }
            // 记录映射（无论开关状态），供「后开开关」时追溯登记
            targetApkMapping = targetApkMapping + (rel to entryName)
            if (smfDeleteAfterPick && !ToolboxApkMerger.isProtectedTargetEntry(entryName)) {
                removedTargetEntries = removedTargetEntries + entryName
                toast("已追加：$rel（打包时删除原条目）")
            } else if (smfDeleteAfterPick) {
                toast("已追加：$rel（$entryName 受保护，不会删除）")
            } else {
                toast("已追加：$rel")
            }
        }
    }

    /** 选择 App 启动窗口背景图（@mipmap/bg_fill_image）。选中后覆盖默认背景，不做路径/文件名输入。 */
    fun pickBgFillImage() {
        filePickerManager.launch(isDirectory = false, fileMimeType = "image/*") { uri, doc ->
            if (uri == null || doc == null) return@launch
            val name = doc.name ?: "file"
            val cacheDir = context.cacheDir ?: context.filesDir ?: return@launch
            val fileDir = File(cacheDir, "integrator_files").apply { mkdirs() }
            val out = File(fileDir, "bgFill_$name")
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { inp -> out.outputStream().use { inp.copyTo(it) } }
            }.onSuccess {
                bgFillImageFile = out
                Toast.makeText(context, "已选择背景图：$name", Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                Toast.makeText(context, "读取文件失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** 恢复 App 启动窗口默认背景图（取消自定义）。 */
    fun resetBgFillImage() { bgFillImageFile = null }

    fun restart() {
        step = 1; targetApk = null; report = null; result = null
        dexStrategy = DexStrategy.INSERT_BEFORE; includeExamples = true
        applyConfigDefaults(defaults)   // 配置字段恢复为当前模式默认值（更新模式=目标 APK 配置，内置模式=内置模板）
        gameActivity = ""               // 游戏 Activity 由下一步 pickApk 重新探测，此处清空
        showUiSettings = false; showAnnouncementSettings = false; showFloatingWindowSettings = false
        showTopBarIconSettings = false; showVersionSettings = false; showSectionSettings = false; showUiAdvancedSettings = false
        showSmfResourceSettings = false
        editingSectionIndex = -1
        selectedFiles = emptyMap()
        pendingFileNames = emptyMap()
        selectedFolders = emptyMap()
        excludedSmfAssets = emptySet()
        addedSmfFiles = emptyMap()
        addedSmfFolders = emptyMap()
        smfDeleteAfterPick = false
        removedTargetEntries = emptySet()
        targetApkMapping = emptyMap()
        localFileUris = emptyMap()
        localFolderUris = emptyMap()
        pendingLocalDeletions = emptyList()
        targetApkBrowserScope = null
    }

    // ── 预览模式切换 ──
    // ── 预览：进入时把选中资源落地到本地 cache 目录，并把本地工作目录指向它 ──
    // 如此 Pvz2MainScreen 经 AssetExtractorHolder 解析资源时，本地目录优先于 APK assets，
    // 预览即可显示用户所选图片、执行所选 js 等（与最终打包效果一致）。
    var savedConfig by remember { mutableStateOf<Pvz2ToolConfig?>(null) }
    var savedLocalConfigUri by remember { mutableStateOf<Uri?>(null) }

    fun isPreviewImageField(fieldKey: String): Boolean =
        fieldKey.contains("icon", ignoreCase = true) || fieldKey in setOf("bgImage", "sideBgImage", "floatingBallIcon", "cgVideoPoster")

    /** 把选中文件按 config 实际引用路径复制到 previewDir（含多候选路径，兼容不同前缀约定） */
    fun stagePreviewAssets(dir: File) {
        fun copyTo(file: File, rel: String) {
            if (rel.isBlank() || rel.startsWith("/") || rel.startsWith("http://") || rel.startsWith("https://")) return
            val out = File(dir, rel)
            out.parentFile?.mkdirs()
            runCatching { file.copyTo(out, overwrite = true) }
        }
        for ((fieldKey, file) in selectedFiles) {
            val p1 = fieldKeyToApkSubDir(fieldKey) + resolveFieldFileName(fieldKey)
            copyTo(file, p1)
            val raw = resolveFieldRawText(fieldKey)
            if (raw.isNotBlank()) {
                copyTo(file, raw) // config 原值作为相对路径（兼容脚本等不加前缀的情况）
                if (isPreviewImageField(fieldKey)) {
                    copyTo(file, "images/$raw") // 主屏图片加载器统一加 images/ 前缀，覆盖带/不带前缀两种写法
                }
            }
        }
        // 文件夹选择：把整个 tree 按 value 父目录名重命名复制到 previewDir（与打包逻辑一致）
        for ((fieldKey, folder) in selectedFolders) {
            val base = folderTargetBase(fieldKey)
            folder.walkTopDown().filter { it.isFile }.forEach { f ->
                val rel = f.relativeTo(folder).path.replace(File.separatorChar, '/')
                copyTo(f, "$base/$rel")
            }
        }
        if (antiDistributionText.isNotBlank()) {
            val tmp = File(context.cacheDir, "integrator_anti_distribution.txt").apply {
                parentFile?.mkdirs(); writeText(antiDistributionText)
            }
            copyTo(tmp, "anti-distribution.txt")
        }
    }

    fun enterPreview() {
        val yml = previewYml ?: run { toast("配置尚未就绪，无法预览"); return }
        val cfg = parsedConfig ?: run { toast("配置尚未就绪，无法预览"); return }
        val previewDir = File(context.cacheDir, "integrator_preview").apply { deleteRecursively(); mkdirs() }
        // 更新模式：先把目标 APK 的 assets/pvz2tool/ 整目录复制到预览目录作为底包（不含 dream.yml）
        if (sourceMode == "update" && targetApk != null) {
            runCatching {
                ApkModule.loadApkFile(targetApk!!).use { module ->
                    module.listInputSources().filter {
                        it.name.startsWith("assets/pvz2tool/") && it.name != "assets/pvz2tool/dream.yml"
                    }.forEach { ins ->
                        val rel = ins.name.removePrefix("assets/pvz2tool/")
                        if (rel.isNotEmpty() && !rel.contains("..")) {
                            ins.openStream().use { inp ->
                                File(previewDir, rel).apply { parentFile?.mkdirs() }.outputStream().use { inp.copyTo(it) }
                            }
                        }
                    }
                }
            }
        }
        // 第二阶段：用户选择/修改的文件覆盖底包
        stagePreviewAssets(previewDir)
        // 写入预览用 dream.yml（仅作为本地配置目录入口文件，其 parent 即预览工作目录）
        val localYml = File(previewDir, "dream.yml").apply { writeText(yml) }
        savedConfig = if (InitializePvz2.isConfigReady()) InitializePvz2.config else null
        savedLocalConfigUri = InitializePvz2.mLocalConfigDirUri
        // 关键：让预览从本地目录读取图片/js——localConfigFile 指向预览目录，并清空 SAF 树 Uri 以走该分支
        InitializePvz2.mLocalConfigDirUri = null
        InitializePvz2.config = cfg.copy(localConfigFile = localYml.absolutePath)
        AssetExtractorHolder.clearResourceCaches()
        InitializePvz2.mPvz2MainScreenReloadKey++
        isPreviewing = true
    }

    fun exitPreview() {
        if (savedConfig != null) {
            InitializePvz2.config = savedConfig!!
            savedConfig = null
        }
        InitializePvz2.mLocalConfigDirUri = savedLocalConfigUri
        AssetExtractorHolder.clearResourceCaches()
        InitializePvz2.mPvz2MainScreenReloadKey++
        isPreviewing = false
    }

    // ── 构建向导标题栏 ──
    @Composable
    fun WizardTopBar() {
        val stepTitle = when {
            editingSectionIndex >= 0 -> "功能项设置 · ${sections[editingSectionIndex].title}"
            showVersionSettings -> "版本设置"
            showSectionSettings -> "栏目设置"
            showAnnouncementSettings -> "公告设置"
            showFloatingWindowSettings -> "悬浮窗设置"
            showTopBarIconSettings -> "顶栏图标设置"
            showUiAdvancedSettings -> "UI高级设置"
            showSmfResourceSettings -> "SMF/资源设置"
            showScheduleSettings -> "定时任务设置"
            showUiSettings -> "UI设置"
            else -> when (step) {
                1 -> "第1步·选择源APK"
                2 -> "第2步·目标设置"
                3 -> "第3步·预览与合并"
                else -> "工具箱集成器"
            }
        }
        TopAppBar(
            title = {
                PvzRichText(
                    "工具箱集成器 · $stepTitle",
                    fontWeight = FontWeight.Bold,
                    defaultStyle = PvzTextWhiteStyle
                )
            },
            navigationIcon = {
                Box(
                    Modifier
                        .padding(start = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable {
                            when {
                                editingSectionIndex >= 0 -> editingSectionIndex = -1
                                showVersionSettings -> showVersionSettings = false
                                showSectionSettings -> showSectionSettings = false
                                showAnnouncementSettings -> showAnnouncementSettings = false
                                showFloatingWindowSettings -> showFloatingWindowSettings = false
                                showTopBarIconSettings -> showTopBarIconSettings = false
                                showUiAdvancedSettings -> showUiAdvancedSettings = false
                                showSmfResourceSettings -> showSmfResourceSettings = false
                                showScheduleSettings -> showScheduleSettings = false
                                showUiSettings -> showUiSettings = false
                                result != null -> onBack()      // 打包完成后退出
                                step > 1 -> step--              // 正常：上一步
                                else -> onBack()                // 第 1 步：返回
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        when {
                            editingSectionIndex >= 0 || showVersionSettings || showSectionSettings || showAnnouncementSettings || showFloatingWindowSettings || showTopBarIconSettings || showUiSettings || showUiAdvancedSettings || showSmfResourceSettings || showScheduleSettings -> "← 返回"
                            result != null -> "← 退出"
                            step > 1 -> "← 上一步"
                            else -> "← 退出"
                        },
                        fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium
                    )
                }
            },
            actions = {
                // 复合文本工具入口（右上角）
                Box(
                    Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { showCompositeTextTool = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "复合文本工具",
                        fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium
                    )
                }
                if (showUiSettings && !showUiAdvancedSettings) {
                    Box(
                        Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable {
                                showUiSettings = true
                                showUiAdvancedSettings = true
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "高级设置",
                            fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White
            ),
            modifier = Modifier
                .height(topBarHeightDp)
                .drawBehind {
                    drawRoundRect(
                        brush = Brush.verticalGradient(colors = listOf(Color(0xff7BC400), Color(0xff4A9A00))),
                        cornerRadius = CornerRadius(15.dp.toPx(), 0.dp.toPx()),
                        size = size
                    )
                }
                .clip(RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp))
                .topRoundedBorder(width = 5.dp, color = Color(0xff96826A), topCornerRadius = 15.dp)
        )
    }

    // ── 预览模式：直接渲染 Pvz2MainScreen（顶栏自带取消预览按钮）──
    // 注意：这里的提前 return 必须位于 CompositionLocalProvider（非 inline 函数）之外，
    // 否则 Kotlin 会报 "'return' is prohibited here"。
    if (isPreviewing && parsedConfig != null) {
        Box(modifier = Modifier.fillMaxSize()) {
            key(InitializePvz2.mPvz2MainScreenReloadKey) {
                Pvz2MainScreen(
                    onGotoGameClick = {},
                    onResetDataClick = {},
                    onCloseToolbox = { exitPreview() },
                    onStateChanged = {},
                    isPreviewMode = true,
                    onCancelPreview = { exitPreview() }
                )
            }
        }
        return
    }

    // ── 整体布局：对标 Pvz2MainScreen（背景图 + Scaffold + TopAppBar）──
    CompositionLocalProvider(LocalOverwriteChecker provides { overwriteTargetPath(it) }) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 背景图（复用 Pvz2MainScreen 同款）
        val bgPath = configReady.takeIf { it }?.let { InitializePvz2.config.ui.assets.background }
            ?: "bg_main.jpg"
        AsyncImageFromAssets(
            if (bgPath.startsWith("/")) bgPath else "images/$bgPath",
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        // ── 向导模式 ──
        // 拦截系统返回键，与左上角按钮行为一致
        val inSubPage = editingSectionIndex >= 0 || showVersionSettings || showSectionSettings
                || showAnnouncementSettings || showFloatingWindowSettings
                || showTopBarIconSettings || showUiSettings || showUiAdvancedSettings || showSmfResourceSettings
                || showScheduleSettings
        BackHandler(enabled = inSubPage || step > 1 || result != null) {
            when {
                editingSectionIndex >= 0 -> editingSectionIndex = -1
                showVersionSettings -> showVersionSettings = false
                showSectionSettings -> showSectionSettings = false
                showAnnouncementSettings -> showAnnouncementSettings = false
                showFloatingWindowSettings -> showFloatingWindowSettings = false
                showTopBarIconSettings -> showTopBarIconSettings = false
                showUiAdvancedSettings -> showUiAdvancedSettings = false
                showSmfResourceSettings -> showSmfResourceSettings = false
                showScheduleSettings -> showScheduleSettings = false
                showUiSettings -> showUiSettings = false
                result != null -> onBack()      // 打包完成后物理返回键 = 退出
                step > 1 -> step--              // 正常：上一步
                else -> onBack()                // 第 1 步：返回
            }
        }
        Scaffold(
            topBar = { WizardTopBar() },
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            modifier = Modifier
                .fillMaxSize()
                .then(if (isUseSolidColorBg) Modifier.drawBehind {
                    drawRoundRect(
                        brush = Brush.verticalGradient(colors = listOf(Color(0xffEEE5C5), Color(0xffEEE5C5))),
                        cornerRadius = CornerRadius(0.dp.toPx(), 15.dp.toPx()),
                        size = size
                    )
                } else Modifier)
        ) { innerPadding ->
            Box(Modifier
                .padding(innerPadding)
                .fillMaxSize()) {
            when {
                // ── 功能项设置子页面（栏目下） ──
                editingSectionIndex >= 0 && step == 2 -> {
                    ItemSettingsContent(
                        sectionIndex = editingSectionIndex,
                        section = sections[editingSectionIndex],
                        onUpdate = { updated -> sections = sections.toMutableList().also { it[editingSectionIndex] = updated } },
                        onPickFile = ::pickAnyFile,
                        selectedFiles = selectedFiles,
                        onImagePreview = openImagePreview,
                        selectedFolders = selectedFolders,
                        onPickFolder = { pickFolder(it) },
                        onClearFieldSelection = { clearFieldSelection(it) }
                    )
                }
                // ── 版本设置子页面 ──
                showVersionSettings && step == 2 -> {
                    VersionSettingsContent(
                        versions = versions,
                        onUpdate = { versions = it },
                        isExpandedVersions = isExpandedVersions,
                        onIsExpandedVersions = { isExpandedVersions = it },
                        versionsTheme = versionsTheme,
                        onVersionsTheme = { versionsTheme = it },
                        onPickFile = ::pickAnyFile,
                        selectedFiles = selectedFiles,
                        onImagePreview = openImagePreview,
                        selectedFolders = selectedFolders,
                        onPickFolder = { pickFolder(it) },
                        onClearFieldSelection = { clearFieldSelection(it) }
                    )
                }
                // ── 栏目设置子页面 ──
                showSectionSettings && step == 2 -> {
                    SectionSettingsContent(
                        sections = sections,
                        onUpdate = { sections = it },
                        onEditItems = { idx -> editingSectionIndex = idx },
                        onPickFile = ::pickAnyFile,
                        selectedFolders = selectedFolders,
                        onPickFolder = { pickFolder(it) },
                        onClearFieldSelection = { clearFieldSelection(it) }
                    )
                }
                // ── 公告设置子页面 ──
                showAnnouncementSettings && step == 2 -> {
                    AnnouncementSettingsContent(
                        announcements = announcements,
                        onUpdate = { announcements = it }
                    )
                }
                // ── 悬浮窗设置子页面 ──
                showFloatingWindowSettings && step == 2 -> {
                    FloatingWindowSettingsContent(
                        showFloatingWindowLabel = showFloatingWindowLabel,
                        onShowFloatingWindowLabel = { showFloatingWindowLabel = it },
                        isShowFloatingWindowDefault = isShowFloatingWindowDefault,
                        onIsShowFloatingWindowDefault = { isShowFloatingWindowDefault = it },
                        fwItems = fwItems,
                        onFwItems = { fwItems = it },
                        fwEmptyTip = fwEmptyTip,
                        onFwEmptyTip = { fwEmptyTip = it },
                        fwAllHiddenTip = fwAllHiddenTip,
                        onFwAllHiddenTip = { fwAllHiddenTip = it },
                        exitConfirmTitle = exitConfirmTitle,
                        onExitConfirmTitle = { exitConfirmTitle = it },
                        exitConfirmMessage = exitConfirmMessage,
                        onExitConfirmMessage = { exitConfirmMessage = it },
                        isUseExitConfirm = isUseExitConfirm,
                        onIsUseExitConfirm = { isUseExitConfirm = it },
                        exitConfirmButtonText = exitConfirmButtonText,
                        onExitConfirmButtonText = { exitConfirmButtonText = it },
                        floatingExitConfirmTitle = floatingExitConfirmTitle,
                        onFloatingExitConfirmTitle = { floatingExitConfirmTitle = it },
                        floatingExitConfirmMessage = floatingExitConfirmMessage,
                        onFloatingExitConfirmMessage = { floatingExitConfirmMessage = it },
                        floatingExitConfirmButtonText = floatingExitConfirmButtonText,
                        onFloatingExitConfirmButtonText = { floatingExitConfirmButtonText = it },
                        onPickFile = ::pickAnyFile,
                        selectedFolders = selectedFolders,
                        onPickFolder = { pickFolder(it) },
                        onClearFieldSelection = { clearFieldSelection(it) },
                        simplifiedLaunch = simplifiedLaunch
                    )
                }
                // ── UI高级设置子页面 ──
                showUiAdvancedSettings && step == 2 -> {
                    UiAdvancedSettingsContent(
                        simplifiedLaunch = simplifiedLaunch,
                        onPickFile = ::pickAnyFile,
                        selectedFolders = selectedFolders,
                        onPickFolder = { pickFolder(it) },
                        uiExDialogTitle = uiExDialogTitle, uiExInitLoadTip = uiExInitLoadTip, uiExInitProgTip = uiExInitProgTip, uiExNoNeedTip = uiExNoNeedTip,
                        uiExSingleFileTip = uiExSingleFileTip, uiExMultiFileTip = uiExMultiFileTip, uiExWaitingTip = uiExWaitingTip, uiExCompleteTip = uiExCompleteTip,
                        uiExFailPrefix = uiExFailPrefix, uiExSkipPrefix = uiExSkipPrefix, uiExContinueBtn = uiExContinueBtn, uiExCompleteBtn = uiExCompleteBtn, uiExToastErr = uiExToastErr,
                        uiSndSwitchPress = uiSndSwitchPress, uiSndSwitchRelease = uiSndSwitchRelease, uiSndBtnPress = uiSndBtnPress, uiSndBtnRelease = uiSndBtnRelease,
                        uiSndSettingsPress = uiSndSettingsPress, uiSndSettingsRelease = uiSndSettingsRelease, uiSndXClosePress = uiSndXClosePress, uiSndXCloseRelease = uiSndXCloseRelease,
                        uiSndPanelPress = uiSndPanelPress, uiSndPanelRelease = uiSndPanelRelease,
                        uiBtnEnterGameIcon = uiBtnEnterGameIcon, uiBtnTutorialIcon = uiBtnTutorialIcon, uiBtnResetDataIcon = uiBtnResetDataIcon,
                        uiSetTitle = uiSetTitle, uiSetSolidBg = uiSetSolidBg, uiSetPlayMusic = uiSetPlayMusic, uiSetImportSmf = uiSetImportSmf, uiSetReload = uiSetReload,
                        uiSetResetSmf = uiSetResetSmf, uiSetCustomDisplay = uiSetCustomDisplay, uiSetDisplayTitle = uiSetDisplayTitle, uiSetApplyBtn = uiSetApplyBtn,
                        uiErrJsTitle = uiErrJsTitle, uiErrUnknown = uiErrUnknown,
                        uiLogCopyDesc = uiLogCopyDesc, uiLogClearDesc = uiLogClearDesc, uiLogNoLogText = uiLogNoLogText, uiLogPresetSaveLabel = uiLogPresetSaveLabel, uiLogLocalSaveLabel = uiLogLocalSaveLabel,
                        uiDlgDelSave = uiDlgDelSave, uiDlgEditUser = uiDlgEditUser, uiDlgShareTitle = uiDlgShareTitle, uiDlgPackFail = uiDlgPackFail, uiDlgNoShare = uiDlgNoShare,
                        uiWelcomeEditTitle = uiWelcomeEditTitle, uiWelcomeEditHint = uiWelcomeEditHint,
                        saveDraft = saveDraft, gameDisplay = gameDisplay,
                        uiSetChangeProfile = uiSetChangeProfile, uiSetShowNotUpdate = uiSetShowNotUpdate, uiSetExitConfirm = uiSetExitConfirm, uiSndSwitchClick = uiSndSwitchClick,
                        onUiExDialogTitle = { uiExDialogTitle = it }, onUiExInitLoadTip = { uiExInitLoadTip = it }, onUiExInitProgTip = { uiExInitProgTip = it }, onUiExNoNeedTip = { uiExNoNeedTip = it },
                        onUiExSingleFileTip = { uiExSingleFileTip = it }, onUiExMultiFileTip = { uiExMultiFileTip = it }, onUiExWaitingTip = { uiExWaitingTip = it }, onUiExCompleteTip = { uiExCompleteTip = it },
                        onUiExFailPrefix = { uiExFailPrefix = it }, onUiExSkipPrefix = { uiExSkipPrefix = it }, onUiExContinueBtn = { uiExContinueBtn = it }, onUiExCompleteBtn = { uiExCompleteBtn = it }, onUiExToastErr = { uiExToastErr = it },
                        onUiSndSwitchPress = { uiSndSwitchPress = it }, onUiSndSwitchRelease = { uiSndSwitchRelease = it }, onUiSndBtnPress = { uiSndBtnPress = it }, onUiSndBtnRelease = { uiSndBtnRelease = it },
                        onUiSndSettingsPress = { uiSndSettingsPress = it }, onUiSndSettingsRelease = { uiSndSettingsRelease = it }, onUiSndXClosePress = { uiSndXClosePress = it }, onUiSndXCloseRelease = { uiSndXCloseRelease = it },
                        onUiSndPanelPress = { uiSndPanelPress = it }, onUiSndPanelRelease = { uiSndPanelRelease = it },
                        onUiBtnEnterGameIcon = { uiBtnEnterGameIcon = it }, onUiBtnTutorialIcon = { uiBtnTutorialIcon = it }, onUiBtnResetDataIcon = { uiBtnResetDataIcon = it },
                        onUiSetTitle = { uiSetTitle = it }, onUiSetSolidBg = { uiSetSolidBg = it }, onUiSetPlayMusic = { uiSetPlayMusic = it },
                        onUiSetImportSmf = { uiSetImportSmf = it }, onUiSetReload = { uiSetReload = it }, onUiSetResetSmf = { uiSetResetSmf = it },
                        onUiSetCustomDisplay = { uiSetCustomDisplay = it }, onUiSetDisplayTitle = { uiSetDisplayTitle = it }, onUiSetApplyBtn = { uiSetApplyBtn = it },
                        onUiErrJsTitle = { uiErrJsTitle = it }, onUiErrUnknown = { uiErrUnknown = it },
                        onUiLogCopyDesc = { uiLogCopyDesc = it }, onUiLogClearDesc = { uiLogClearDesc = it }, onUiLogNoLogText = { uiLogNoLogText = it },
                        onUiLogPresetSaveLabel = { uiLogPresetSaveLabel = it }, onUiLogLocalSaveLabel = { uiLogLocalSaveLabel = it },
                        onUiDlgDelSave = { uiDlgDelSave = it }, onUiDlgEditUser = { uiDlgEditUser = it }, onUiDlgShareTitle = { uiDlgShareTitle = it },
                        onUiDlgPackFail = { uiDlgPackFail = it }, onUiDlgNoShare = { uiDlgNoShare = it },
                        onUiWelcomeEditTitle = { uiWelcomeEditTitle = it }, onUiWelcomeEditHint = { uiWelcomeEditHint = it },
                        onSaveDraft = { saveDraft = it }, onGameDisplay = { gameDisplay = it },
                        onUiSetChangeProfile = { uiSetChangeProfile = it }, onUiSetShowNotUpdate = { uiSetShowNotUpdate = it }, onUiSetExitConfirm = { uiSetExitConfirm = it }, onUiSndSwitchClick = { uiSndSwitchClick = it },
                        onClearFieldSelection = { clearFieldSelection(it) }
                    )
                }
                // ── 定时任务设置子页面 ──
                showScheduleSettings && step == 2 -> {
                    ScheduleSettingsContent(schedules, { schedules = it }) { showScheduleSettings = false }
                }
                // ── SMF/资源设置子页面 ──
                showSmfResourceSettings && step == 2 -> {
                    SmfResourceSettingsContent(
                        // 更新模式：目标 APK 已有旧版工具箱资源，列出目标内资源让用户决定保留/替换
                        sourceApk = if (sourceMode == "update") targetApk ?: sourceApk else sourceApk,
                        baseAssetPath = baseAssetPath,
                        onBaseAssetPath = { baseAssetPath = it },
                        versions = versions,
                        onUpdateVersions = { versions = it },
                        simplifiedLaunch = simplifiedLaunch,
                        excludedSmfAssets = excludedSmfAssets,
                        onToggleExclude = { rel ->
                            excludedSmfAssets = if (rel in excludedSmfAssets) excludedSmfAssets - rel else excludedSmfAssets + rel
                        },
                        addedSmfFiles = addedSmfFiles,
                        onAddSmfFile = { rel, f -> addedSmfFiles = addedSmfFiles.toMutableMap().apply { put(rel, f) } },
                        addedSmfFolders = addedSmfFolders,
                        onAddSmfFolder = { scope, f -> addedSmfFolders = addedSmfFolders.toMutableMap().apply { put(scope, f) } },
                        onRemoveAdded = { key ->
                            addedSmfFiles = addedSmfFiles.minus(key)
                            addedSmfFolders = addedSmfFolders.minus(key)
                            // 清理追踪映射与待删除登记
                            val removedApkEntry = targetApkMapping[key]
                            if (removedApkEntry != null) {
                                removedTargetEntries = removedTargetEntries - removedApkEntry
                                targetApkMapping = targetApkMapping - key
                            }
                            val removedLocalUri = localFileUris[key] ?: localFolderUris[key]
                            localFileUris = localFileUris - key
                            localFolderUris = localFolderUris - key
                            if (removedLocalUri != null) {
                                pendingLocalDeletions = pendingLocalDeletions.filter { it.uri != removedLocalUri }
                            }
                        },
                        onPickSmfFile = { scope -> pickSmfFile(scope) },
                        onPickSmfFolder = { scope -> pickSmfFolder(scope) },
                        deleteAfterPick = smfDeleteAfterPick,
                        onDeleteAfterPick = { newValue ->
                            smfDeleteAfterPick = newValue
                            if (newValue) {
                                // 追溯：已从目标 APK 选择的条目 → 登记到 removedTargetEntries
                                val apkToAdd = targetApkMapping.values.filter { !ToolboxApkMerger.isProtectedTargetEntry(it) }.toSet()
                                if (apkToAdd.isNotEmpty()) removedTargetEntries = removedTargetEntries + apkToAdd
                                // 追溯：已从本地选择的文件 → 注册到待删除列表
                                val fileDeletions = localFileUris.map { (rel, u) -> PendingLocalDeletion(u, rel.substringAfterLast('/')) }
                                val folderDeletions = localFolderUris.map { (_, u) -> PendingLocalDeletion(u, "folder") }
                                if (fileDeletions.isNotEmpty() || folderDeletions.isNotEmpty()) {
                                    pendingLocalDeletions = pendingLocalDeletions + fileDeletions + folderDeletions
                                }
                                localFileUris = emptyMap()
                                localFolderUris = emptyMap()
                            }
                        },
                        hasTargetApk = targetApk != null,
                        onPickFromTargetApk = { targetApkBrowserScope = it },
                        removedTargetEntries = removedTargetEntries,
                        onUndoRemoveTarget = { removedTargetEntries = removedTargetEntries - it },
                        onBack = { showSmfResourceSettings = false }
                    )
                }
                // ── 顶栏图标设置子页面 ──
                showTopBarIconSettings && step == 2 -> {
                    TopBarIconSettingsContent(
                        tbiItems = tbiItems,
                        onUpdate = { tbiItems = it },
                        onPickFile = ::pickAnyFile,
                        selectedFiles = selectedFiles,
                        onImagePreview = openImagePreview,
                        selectedFolders = selectedFolders,
                        onPickFolder = { pickFolder(it) },
                        onClearFieldSelection = { clearFieldSelection(it) }
                    )
                }
                // ── UI设置子页面 ──
                showUiSettings && step == 2 -> {
                val ymlAssets = if (simplifiedLaunch) "" else "ui.assets."

                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                    // ═══ 左栏：CG/视频 ═══
                    Column(
                        Modifier
                            .weight(0.5f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        PvzDialogCard(title = null) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                UiSectionHeader("CG/视频 (${ymlAssets})")
                                UiInputCard("${ymlAssets}cgVideoPath", "开场 CG 视频文件名。选择文件时，将复制到 APK 对应路径。") {
                                    FileInputRow(cgVideoPath, "如 opening.mp4", "*/*", "cgVideoPath", { cgVideoPath = it }, selectedFolder = selectedFolders["cgVideoPath"], onPickFile = { label, mime -> pickAnyFile(label, mime) }, onPickFolder = { fk -> pickFolder(fk) }, onClearSelection = { clearFieldSelection("cgVideoPath") })
                                }
                                UiInputCard("${ymlAssets}cgVideoPoster", "CG 加载超时或出错时的占位海报图。") {
                                    FileInputRow(cgVideoPoster, "如 bg_main.jpg", "*/*", "cgVideoPoster", { cgVideoPoster = it },
                                        selectedFile = selectedFiles["cgVideoPoster"], selectedFolder = selectedFolders["cgVideoPoster"],
                                        onImagePreview = openImagePreview,
                                        onPickFile = { label, mime -> pickAnyFile(label, mime) },
                                        onPickFolder = { fk -> pickFolder(fk) },
                                        onClearSelection = { clearFieldSelection("cgVideoPoster") },
                                        targetApk = if (sourceMode == "update") targetApk else null)
                                }
                                UiInputCard("${ymlAssets}cgVideoLoadTimeout", "CG 视频加载超时时间（毫秒），默认 5000。") {
                                    IntegratorInputField(cgVideoLoadTimeout, "如 5000") { cgVideoLoadTimeout = it }
                                }
                            }
                        }

                        // § 版本/作者/标题（仅非简易）
                        if (!simplifiedLaunch) {
                        Spacer(Modifier.height(10.dp))
                        PvzDialogCard(title = null) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                UiSectionHeader("版本/作者/标题")
                                UiInputCard("ui.versionLabel", "版本号标签前缀") { IntegratorInputField(uiVersionLabel, "如 版本号：") { uiVersionLabel = it } }
                                UiInputCard("ui.uiVersion", "UI 版本号") { IntegratorInputField(uiUiVersion, "如 V2.5.1") { uiUiVersion = it } }
                                UiInputCard("ui.authorInfo", "作者信息") { IntegratorInputField(uiAuthorInfo, "作者信息", multiline = true) { uiAuthorInfo = it } }
                                UiInputCard("ui.noValidDirTip", "无有效目录提示") { IntegratorInputField(uiNoValidDirTip, "如 未选择有效目录") { uiNoValidDirTip = it } }
                                UiInputCard("ui.title.topAppBar", "顶栏标题") { IntegratorInputField(uiTitleTopAppBar, "顶栏标题") { uiTitleTopAppBar = it } }
                                UiInputCard("ui.title.about", "关于版本标签") { IntegratorInputField(uiTitleAbout, "如 关于版本") { uiTitleAbout = it } }
                                UiInputCard("ui.title.coreFunction", "核心功能标签") { IntegratorInputField(uiTitleCoreFunction, "如 核心功能") { uiTitleCoreFunction = it } }
                                UiInputCard("ui.title.versionManage", "版本管理标签") { IntegratorInputField(uiTitleVersionManage, "如 版本管理") { uiTitleVersionManage = it } }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        } // end if (!simplifiedLaunch) - 版本/作者/标题
                    }

                    // ═══ 右栏：背景/图标 + 音频 + 错误提示 ═══
                    Column(
                        Modifier
                            .weight(0.5f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // §3 背景/图标（仅非简易）
                        PvzDialogCard(title = null) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                UiSectionHeader("背景/图标 (ui.assets)")
                                if (!simplifiedLaunch) {
                                    UiInputCard("ui.assets.bgImage", "主界面背景图文件名或 URL。") {
                                        FileInputRow(
                                            bgImage,
                                            "如 bg_main.jpg",
                                            "*/*",
                                            "bgImage",
                                            { bgImage = it },
                                            selectedFile = selectedFiles["bgImage"],
                                            selectedFolder = selectedFolders["bgImage"],
                                            onImagePreview = openImagePreview,
                                            onPickFile = { label, mime -> pickAnyFile(label, mime) },
                                            onPickFolder = { fk -> pickFolder(fk) },
                                            onClearSelection = { clearFieldSelection("bgImage") },
                                            targetApk = if (sourceMode == "update") targetApk else null)
                                    }
                                    UiSwitchCard(
                                        "ui.assets.isUseSolidColorBackground",
                                        "是否使用纯色背景（替代背景图）。"
                                    ) {
                                        PvzCheckRow("纯色背景模式", isUseSolidColorBg) {
                                            isUseSolidColorBg = !isUseSolidColorBg
                                        }
                                    }
                                    UiInputCard("ui.assets.sideBgImage", "侧边背景图文件名或 URL。") {
                                        FileInputRow(
                                            sideBgImage,
                                            "如 game_side_bg.jpg",
                                            "*/*",
                                            "sideBgImage",
                                            { sideBgImage = it },
                                            selectedFile = selectedFiles["sideBgImage"],
                                            selectedFolder = selectedFolders["sideBgImage"],
                                            onImagePreview = openImagePreview,
                                            onPickFile = { label, mime -> pickAnyFile(label, mime) },
                                            onPickFolder = { fk -> pickFolder(fk) },
                                            onClearSelection = { clearFieldSelection("sideBgImage") },
                                            targetApk = if (sourceMode == "update") targetApk else null)
                                    }
                                    UiInputCard("ui.assets.floatingBallIcon", "悬浮球图标文件名。") {
                                        FileInputRow(
                                            floatingBallIcon,
                                            "如 ic_floating_dave.png",
                                            "*/*",
                                            "floatingBallIcon",
                                            { floatingBallIcon = it },
                                            selectedFile = selectedFiles["floatingBallIcon"],
                                            selectedFolder = selectedFolders["floatingBallIcon"],
                                            onImagePreview = openImagePreview,
                                            onPickFile = { label, mime -> pickAnyFile(label, mime) },
                                            onPickFolder = { fk -> pickFolder(fk) },
                                            onClearSelection = { clearFieldSelection("floatingBallIcon") },
                                            targetApk = if (sourceMode == "update") targetApk else null)
                                    }
                                }
                                UiInputCard("ui.assets.bgFillImage (@mipmap/bg_fill_image)", "App 启动窗口背景图（windowBackground）。下方为当前默认背景，点「修改背景图」选本地图片替换即可；资源表固定为 .jpg，所选图片打包时会自动转为 JPEG。更新模式下未单独修改则保留目标 APK 现有背景。") {
                                    val fillModel = bgFillImageFile ?: targetBgFillImage ?: R.mipmap.bg_fill_image
                                    AsyncImage(
                                        model = fillModel,
                                        contentDescription = if (bgFillImageFile != null) "已选背景图预览（点击大屏预览）" else "目标 APK 当前背景图预览（点击大屏预览）",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { openImagePreview(fillModel) },
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                                        PvzBlueButton("修改背景图", Modifier.height(36.dp)) { pickBgFillImage() }
                                        if (bgFillImageFile != null) {
                                            Spacer(Modifier.width(8.dp))
                                            PvzRedButton("恢复默认", Modifier.height(36.dp)) { resetBgFillImage() }
                                        }
                                    }
                                }
                            }
                        }

                        // §4 音频（仅非简易）
                        if (!simplifiedLaunch) {
                            Spacer(Modifier.height(10.dp))
                            PvzDialogCard(title = null) {
                                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    UiSectionHeader("音频 (ui.assets)")
                                    UiInputCard("ui.assets.bgMusic", "背景音乐文件名（相对于 assets/pvz2tool/sound/）。") {
                                        FileInputRow(bgMusic, "如 bg_music.wav", "*/*", "bgMusic", { bgMusic = it }, selectedFolder = selectedFolders["bgMusic"], onPickFile = { label, mime -> pickAnyFile(label, mime) }, onPickFolder = { fk -> pickFolder(fk) }, onClearSelection = { clearFieldSelection("bgMusic") })
                                    }
                                    UiSwitchCard("ui.assets.isPlayBackgroundMusic", "是否默认播放背景音乐。") {
                                        PvzCheckRow("默认播放背景音乐", isPlayBgMusic) { isPlayBgMusic = !isPlayBgMusic }
                                    }
                                }
                            }
                        }

                        // §5 进游戏声明（anti-distribution.txt，所有模式均生效）
                        Spacer(Modifier.height(10.dp))
                        PvzDialogCard(title = null) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                UiSectionHeader("进游戏声明 (anti-distribution.txt)")
                                UiInputCard("anti-distribution.txt", "进入游戏时（本地工作目录优先，回退 APK 内置）逐行弹出的声明/提示文本，每行一条 Toast。已默认填入当前 APK 内置内容，可直接修改；清空则沿用内置默认。") {
                                    IntegratorInputField(antiDistributionText, "每行一条提示文本", multiline = true) { antiDistributionText = it }
                                }
                            }
                        }

                        // §6 错误提示
                        if (!simplifiedLaunch) {
                            Spacer(Modifier.height(10.dp))
                            PvzDialogCard(title = null) {
                                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    UiSectionHeader("错误提示 (ui.error.)")
                                    UiInputCard("ui.error.gameActivityInvalid", "游戏 Activity 设置有误时的提示文案。") {
                                        IntegratorInputField(
                                            gameActivityInvalid,
                                            "如：设置的游戏Activity有误或不存在"
                                        ) { gameActivityInvalid = it }
                                    }
                                }
                            }
                        }

                        // § 教程/按钮/对话框（仅非简易）
                        if (!simplifiedLaunch) {
                        Spacer(Modifier.height(10.dp))
                        PvzDialogCard(title = null) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                UiSectionHeader("教程/按钮/对话框")
                                UiInputCard("ui.tutorial", "教程文本") { IntegratorInputField(uiTutorial, "教程内容", multiline = true) { uiTutorial = it } }
                                UiInputCard("ui.button.enterGame", "进入游戏按钮") { IntegratorInputField(uiBtnEnterGame, "如 进入游戏") { uiBtnEnterGame = it } }
                                UiInputCard("ui.button.tutorial", "教程按钮") { IntegratorInputField(uiBtnTutorial, "如 教程") { uiBtnTutorial = it } }
                                UiInputCard("ui.button.resetData", "重置数据按钮") { IntegratorInputField(uiBtnResetData, "如 重置数据包") { uiBtnResetData = it } }
                                UiInputCard("ui.button.showFloatingWindow", "悬浮窗按钮") { IntegratorInputField(uiBtnShowFW, "如 工具悬窗") { uiBtnShowFW = it } }
                                UiInputCard("ui.button.confirmVersion", "选定版本按钮") { IntegratorInputField(uiBtnConfirmVersion, "如 选定版本") { uiBtnConfirmVersion = it } }
                                UiInputCard("ui.log.panelTitle", "日志面板标题") { IntegratorInputField(uiLogPanelTitle, "如 JS 日志") { uiLogPanelTitle = it } }
                                UiInputCard("ui.dialog.confirm", "通用确认按钮") { IntegratorInputField(uiDialogConfirm, "如 确定") { uiDialogConfirm = it } }
                                UiInputCard("ui.dialog.cancel", "通用取消按钮") { IntegratorInputField(uiDialogCancel, "如 取消") { uiDialogCancel = it } }
                                UiInputCard("ui.welcome.greeting", "欢迎语模板") { IntegratorInputField(uiWelcomeGreeting, "如 欢迎您，%s") { uiWelcomeGreeting = it } }
                            }
                        }
                    }
                    } // end if (!simplifiedLaunch) - 教程/按钮/对话框
                }
                }
                }
                // ── 正常步骤内容 ──
                else -> {
                // ── 两列布局，内容均分 ──
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 左侧：步骤信息 / 配置
                    Column(Modifier.weight(0.48f)) {
                        PvzDialogCard(title = null) {
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                when (step) {
                                    1 -> StepSourceLeft(
                                        sourceName = sourceApk.name,
                                        sourceVersion = sourceVersion,
                                        sourceMode = sourceMode,
                                        onSourceMode = { sourceMode = it }
                                    )
                                    2 -> StepTargetLeft(
                                        targetApk = targetApk,
                                        gameActivity = gameActivity,
                                        dexStrategy = dexStrategy,
                                        smfDirectory = smfDirectory,
                                        onStrategy = { dexStrategy = it },
                                        onSmfDirectory = { smfDirectory = it },
                                        onPickApk = { pickApk() },
                                        sourceMode = sourceMode,
                                        detectedInfo = detectedInfo,
                                        sourceDexCount = sourceDexCount,
                                        updDexStart = updDexStart,
                                        updDexEnd = updDexEnd,
                                        updInsertMode = updInsertMode,
                                        onUpdDexStart = {
                                            updDexStart = it
                                            updInsertMode = if ((it.toIntOrNull() ?: 1) <= 1) DexStrategy.INSERT_BEFORE else DexStrategy.APPEND
                                        },
                                        onUpdDexEnd = { updDexEnd = it },
                                        onUpdInsertMode = { updInsertMode = it },
                                        appendUnreferencedAssets = appendUnreferencedAssets,
                                        onAppendUnreferencedAssets = { appendUnreferencedAssets = it }
                                    )
                                    3 -> StepPreviewLeft(
                                        report = report, result = result, loading = loading,
                                        onRecompute = { computePreview() },
                                        onApply = { doApply() }
                                    )
                                }
                            }
                        }
                    }

                    // 右侧：操作 / 结果（可滚动）+ 底部导航（固定）
                    Column(Modifier.weight(0.52f)) {
                        PvzDialogCard(
                            title = null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                when (step) {
                                    1 -> StepSourceRight()
                                    2 -> StepTargetRight(
                                        simplifiedLaunch = simplifiedLaunch,
                                        includeExamples = includeExamples,
                                        onSimplifiedLaunch = { simplifiedLaunch = it; if (it) includeExamples = false },
                                        onIncludeExamples = { includeExamples = it; if (!it) sections = sections.filter { s -> !s.id.startsWith("example_") } },
                                        onOpenUiSettings = { showUiSettings = true },
                                        onOpenAnnouncementSettings = { showAnnouncementSettings = true },
                                        onOpenFloatingWindowSettings = { showFloatingWindowSettings = true },
                                        onOpenTopBarIconSettings = { showTopBarIconSettings = true },
                                        onOpenVersionSettings = { showVersionSettings = true },
                                        onOpenSectionSettings = { showSectionSettings = true; editingSectionIndex = -1 },
                                        onOpenSmfResourceSettings = { showSmfResourceSettings = true },
                                        onOpenScheduleSettings = { showScheduleSettings = true },
                                        // 更新模式无描述文件时隐藏简易模式/示例项目开关（沿用目标 APK 已有状态）
                                        showLaunchOptions = !(sourceMode == "update" && detectedInfo == null)
                                    )
                                    3 -> StepPreviewRight(
                                        report = report, result = result,
                                        includeExamples = includeExamples,
                                        configReady = configReady,
                                        dreamYmlFull = dreamYmlRaw,
                                        simplifiedLaunch = simplifiedLaunch,
                                        errorMsg = errorMsg,
                                        onPreviewUi = { enterPreview() },
                                        onRestart = { restart() }
                                    )
                                }
                            }
                        }
                        // 底部导航按钮（固定在右栏底部，不随内容滚动）
                        BottomNavRow(
                            step = step,
                            targetApk = targetApk,
                            result = result,
                            onBack = onBack,
                            onPrev = { step-- },
                            onNext = {
                                when (step) {
                                    1 -> step = 2
                                    2 -> if (targetApk != null) { step = 3; computePreview() } else toast("请先选择目标 APK")
                                }
                            },
                            onApply = { doApply() },
                            onRecompute = { computePreview() },
                            onRestart = { restart() },
                            filePickerManager = filePickerManager
                        )
                    }
                }
                }
            }
            } // Box end
        }

        // 全屏图片预览浮层：点缩略图/背景图预览后展示，点任意处关闭
        if (previewModel != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { previewModel = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = previewModel!!,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
                Text(
                    "点击任意处关闭",
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    color = Color.White,
                    fontSize = 13.sp
                )
            }
        }
    }

    // 复合文本工具弹窗（右上角入口）
    if (showCompositeTextTool) {
        CompositeTextToolDialog(onDismiss = { showCompositeTextTool = false })
    }

    // 处理耗时任务（差异计算 / APK 合并）时的加载与进度弹窗
    JsLoadingDialog()
    JsProgressDialog()

    // 目标 APK 条目浏览器（从目标 APK 选择条目追加为 SMF 资源）
    targetApkBrowserScope?.let { assetScope ->
        // 当前 scope 下已从目标 APK 选过的条目（APK 内完整路径），传入对话框以隐藏其「选择」按钮
        val selectedTargets = targetApkMapping
            .filter { (rel, _) -> rel.startsWith("$assetScope/") }
            .values
            .toSet()
        TargetApkBrowserDialog(
            targetApk = targetApk,
            assetScope = assetScope,
            onDismiss = { targetApkBrowserScope = null },
            onPickEntry = { entry -> addEntryFromTargetApk(assetScope, entry) },
            selectedEntries = selectedTargets
        )
    }
    // PopCap 原版 APK 警告弹窗
    popCapWarningFile?.let { f ->
        PvzStyledDialog(
            isVisible = true,
            titleText = "原版 APK 警告",
            onDismissRequest = { f.delete(); popCapWarningFile = null },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            bottomContent = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PvzBodyText("检测到此 APK 为 PopCap Games 原版，签名校验未去除。")
                    PvzBodyText("请先在 MT 管理器使用「去除签名校验」功能，并关闭上面的「使用原版过签」开关后处理该 APK，处理完成后再重新选择。")
                    PvzRedButton("我知道了", Modifier
                        .fillMaxWidth()
                        .height(42.dp)) {
                        f.delete(); popCapWarningFile = null
                    }
                }
            }
        ) {}
    }
    // 工具箱模式不匹配警告弹窗
    toolboxModeWarning?.let { mode ->
        val (title, desc) = if (mode == "update_wrong")
            "该 APK 不含工具箱" to "此 APK 未检测到工具箱特征（assets/pvz2tool/），无法执行更新。\n请选择已集成工具箱的 APK，或在第一步切换为「内置到未集成的 APK」。"
        else
            "该 APK 已含工具箱" to "此 APK 已包含工具箱特征，请使用更新模式。\n请在第一步切换为「更新已集成的 APK」后重新选择。"
        PvzStyledDialog(
            isVisible = true,
            titleText = title,
            onDismissRequest = { toolboxModeWarning = null },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            bottomContent = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PvzBodyText(desc)
                    PvzRedButton("我知道了", Modifier.fillMaxWidth().height(42.dp)) {
                        toolboxModeWarning = null
                    }
                }
            }
        ) {}
    }
    } // ← CompositionLocalProvider(LocalOverwriteChecker)
}

// ── 底部按钮行 ──────────────────────────────────────────────────

private val BUTTON_HEIGHT = 48.dp

@Composable
private fun BottomNavRow(
    step: Int,
    targetApk: File?,
    result: MergeResult?,
    onBack: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onApply: () -> Unit,
    onRecompute: () -> Unit,
    onRestart: () -> Unit,
    filePickerManager: FilePickerManager
) {
    val ctx = LocalContext.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 打包完成后：导出 / 分享按钮置于「集成 / 退出」上方
        result?.let { merged ->
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                PvzGreenButton(
                    "导出到本地",
                    Modifier.weight(1f).height(BUTTON_HEIGHT),
                    onClick = { exportMergedApkToLocal(ctx, filePickerManager, merged.outputApk) }
                )
                PvzBlueButton(
                    "分享到其他软件",
                    Modifier.weight(1f).height(BUTTON_HEIGHT),
                    onClick = { shareMergedApk(ctx, merged.outputApk) }
                )
            }
        }
        if (result != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PvzGreenButton("再集成一个", Modifier
                    .weight(1f)
                    .height(BUTTON_HEIGHT), onClick = onRestart)
                PvzRedButton("退出", Modifier
                    .weight(1f)
                    .height(BUTTON_HEIGHT), onClick = onBack)
            }
        } else when (step) {
            1 -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PvzRedButton("退出", Modifier
                        .weight(1f)
                        .height(BUTTON_HEIGHT), onClick = onBack)
                    PvzGreenButton("下一步 →", Modifier
                        .weight(1f)
                        .height(BUTTON_HEIGHT), onClick = onNext)
                }
            }
            2 -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PvzRedButton("← 上一步", Modifier
                        .weight(1f)
                        .height(BUTTON_HEIGHT), onClick = onPrev)
                    PvzGreenButton(
                        if (targetApk != null) "预览差异 →" else "请选择目标 APK",
                        Modifier
                            .weight(1f)
                            .height(BUTTON_HEIGHT),
                        onClick = onNext
                    )
                }
            }
            3 -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PvzRedButton("← 上一步", Modifier
                        .weight(1f)
                        .height(BUTTON_HEIGHT), onClick = onPrev)
                    PvzGreenButton("重新计算", Modifier
                        .weight(1f)
                        .height(BUTTON_HEIGHT), onClick = onRecompute)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PvzGreenButton(
                        "执行合并",
                        Modifier
                            .weight(1f)
                            .height(BUTTON_HEIGHT),
                        backgroundColor = Color(0xFF2E7D32),
                        onClick = onApply
                    )
                }
            }
        }
    }
}

// ── 第 1 步：源确认（左右分栏）──────────────────────────────────

@Composable
private fun StepSourceLeft(
    sourceName: String,
    sourceVersion: String,
    sourceMode: String,
    onSourceMode: (String) -> Unit
) {
    PvzSectionTitle("第 1 步 · 源确认")

    PvzInfoCard("源 = 当前安装的工具箱 APK（自身）") {
        PvzBodyText("即本应用自身 APK，自带资源包 id 0x66，将作为「新版本」集成到目标游戏 APK。")
        PvzHighlightText("文件：$sourceName　版本：$sourceVersion")
    }

    Spacer(Modifier.height(8.dp))
    PvzInfoCard("集成模式") {
        PvzBodyText("选择本次集成的目标类型。")
        Spacer(Modifier.height(8.dp))
        PvzChoiceRow(
            label = "内置到未集成的 APK 中",
            selected = sourceMode == "integrate",
            onClick = { onSourceMode("integrate") }
        )
        PvzChoiceRow(
            label = "更新已集成的 APK",
            selected = sourceMode == "update",
            onClick = { onSourceMode("update") }
        )
    }
}

@Composable
private fun StepSourceRight() {
    PvzSectionTitle("合并流水线")
    PvzInfoCard("合并步骤") {
        PvzBodyText("① assets/kotlin/org/META-INF/根文件 合并（dream.yml 按差异追加）")
        PvzBodyText("② arsc：搬移 0x66 包（删旧加新，字符串池合并）")
        PvzBodyText("③ dex：追加/插入到目标 dex（可选策略）")
        PvzBodyText("④ manifest：删游戏 LAUNCHER + 追加启动器组件 + targetSdk≥21")
        PvzBodyText("⑤ res 覆盖替换　⑥ lib 按 ABI 合并")
    }
    PvzWarningCard("产出为未签名 APK，合并完成后请用 MT 管理器签名安装。")
}

// ── 第 2 步：选择目标 + 策略配置（左右分栏）─────────────────────

@Composable
private fun StepTargetLeft(
    targetApk: File?,
    gameActivity: String,
    dexStrategy: DexStrategy,
    smfDirectory: String,
    onStrategy: (DexStrategy) -> Unit,
    onSmfDirectory: (String) -> Unit,
    onPickApk: () -> Unit,
    // 更新模式相关
    sourceMode: String = "integrate",
    detectedInfo: ToolboxApkMerger.IntegratorInfo? = null,
    sourceDexCount: Int = 0,
    updDexStart: String = "0",
    updDexEnd: String = "",
    updInsertMode: DexStrategy = DexStrategy.INSERT_BEFORE,
    onUpdDexStart: (String) -> Unit = {},
    onUpdDexEnd: (String) -> Unit = {},
    onUpdInsertMode: (DexStrategy) -> Unit = {},
    // 更新模式：「附加未包含 pvz2tool 内容」开关
    appendUnreferencedAssets: Boolean = true,
    onAppendUnreferencedAssets: (Boolean) -> Unit = {}
) {
    PvzSectionTitle("第 2 步 · 目标设置")

    PvzInfoCard("选择目标游戏 APK") {
        PvzBodyText(
            if (sourceMode == "update")
                "选择一个已集成工具箱的 APK。集成器将替换其中的旧版工具箱 DEX（依据描述文件或手动指定的范围）。"
            else
                "选择一个未集成的目标游戏 APK。集成器将把工具箱注入其中。"
        )
        Spacer(Modifier.height(8.dp))
        PvzGreenButton("选择 APK 文件", Modifier
            .fillMaxWidth()
            .height(BUTTON_HEIGHT), onClick = onPickApk)
        targetApk?.let {
            Spacer(Modifier.height(8.dp))
            PvzHighlightText("✓ 已选：${it.name}")
        }
    }

    // gameActivity 展示（只读，自动检测）—— Info 风格（浅绿卡片）
    if ((gameActivity.isNotEmpty() || targetApk != null) && sourceMode != "update") {
        Spacer(Modifier.height(6.dp))
        PvzInfoCard("目标游戏活动 (gameActivity)") {
            PvzBodyText("自动检测到的游戏入口 Activity，将写入 dream.yml。")
            Spacer(Modifier.height(4.dp))
            val bgColor = Color(0xFFD4E8A0)
            PvzSimpleCardGreen(borderColor = bgColor, backgroundColor = bgColor) {
                PvzRichText(
                    if (gameActivity.isNotEmpty()) gameActivity else "（检测中...）",
                    fontSize = 13.sp, defaultStyle = PvzTextStyle(Color(0xFF33691E)),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }

    if (sourceMode == "update") {
        // ── 更新模式：DEX 策略为「替换旧版工具箱 DEX」，不再自由选插入/追加 ──
        val defaultEnd = (updDexStart.toIntOrNull() ?: 1) + (sourceDexCount - 1).coerceAtLeast(0)
        PvzInfoCard("DEX 合并策略（更新模式）") {
            PvzBodyText("更新模式：将替换目标 APK 中旧版工具箱 DEX，而非自由插入。")
            Spacer(Modifier.height(8.dp))
            detectedInfo?.let { di ->
                PvzHighlightText("已检测到描述文件：版本 ${di.version}，旧工具箱 DEX 范围 [${di.dexStart + 1}..${di.dexEnd + 1}]")
                Spacer(Modifier.height(6.dp))
                PvzBodyText("新工具箱 DEX 将依据描述文件插入到剩余目标 DEX 的：")
            } ?: run {
                PvzBodyText("未检测到描述文件，请手动指定旧工具箱 DEX 所在范围：")
                Spacer(Modifier.height(6.dp))
                PvzBodyText("起始序号（1 起）：")
                IntegratorInputField(updDexStart, "如 1") { onUpdDexStart(it) }
                Spacer(Modifier.height(4.dp))
                PvzBodyText("结束序号（留空 = 覆盖到本版本所有 DEX，默认 $defaultEnd）：")
                IntegratorInputField(updDexEnd, "留空自动计算") { onUpdDexEnd(it) }
                Spacer(Modifier.height(6.dp))
            }
            PvzChoiceRow(
                label = "插入到剩余目标 DEX 之前（新版推荐）",
                selected = updInsertMode == DexStrategy.INSERT_BEFORE,
                onClick = { onUpdInsertMode(DexStrategy.INSERT_BEFORE) }
            )
            PvzChoiceRow(
                label = "追加到剩余目标 DEX 之后（老版推荐）",
                selected = updInsertMode == DexStrategy.APPEND,
                onClick = { onUpdInsertMode(DexStrategy.APPEND) }
            )
        }
        // 更新模式：附加未包含内容的开关
        Spacer(Modifier.height(6.dp))
        PvzInfoCard("附加未包含的 pvz2tool 内容") {
            PvzBodyText("开启：源 APK 中目标没有的文件（如默认图/示例/新功能 JS）也会写入。关闭：仅替换目标已有文件。文档类（js_documentation.md / config_documentation.md）始终覆盖。")
            Spacer(Modifier.height(6.dp))
            PvzCheckRow("附加未包含内容", appendUnreferencedAssets) { onAppendUnreferencedAssets(!appendUnreferencedAssets) }
        }
    } else {
        // ── 普通集成模式：DEX 插入/追加策略 ──
        PvzInfoCard("DEX 合并策略") {
            PvzBodyText("决定工具箱的 dex 放在目标 dex 之前还是之后。")
            Spacer(Modifier.height(8.dp))
            PvzChoiceRow(
                label = "插入到目标所有 dex 之前（新版推荐）",
                selected = dexStrategy == DexStrategy.INSERT_BEFORE,
                onClick = { onStrategy(DexStrategy.INSERT_BEFORE) }
            )
            PvzChoiceRow(
                label = "追加到目标 dex 之后（老版推荐）",
                selected = dexStrategy == DexStrategy.APPEND,
                onClick = { onStrategy(DexStrategy.APPEND) }
            )
        }
    }

    Spacer(Modifier.height(6.dp))
    PvzInfoCard("SMF 存放目录 (smfDirectory)") {
        PvzBodyText("栏目默认的 SMF 资源解压目标路径（相对于游戏 dataDir）。")
        Spacer(Modifier.height(4.dp))
        IntegratorInputField(smfDirectory, "如 files/") { onSmfDirectory(it) }
    }
}

@Composable
private fun StepTargetRight(
    simplifiedLaunch: Boolean,
    includeExamples: Boolean,
    onSimplifiedLaunch: (Boolean) -> Unit,
    onIncludeExamples: (Boolean) -> Unit,
    onOpenUiSettings: () -> Unit,
    onOpenAnnouncementSettings: () -> Unit,
    onOpenFloatingWindowSettings: () -> Unit,
    onOpenTopBarIconSettings: () -> Unit,
    onOpenVersionSettings: () -> Unit,
    onOpenSectionSettings: () -> Unit,
    onOpenSmfResourceSettings: () -> Unit,
    onOpenScheduleSettings: () -> Unit,
    /** 更新模式无描述文件时隐藏简易模式/示例项目开关（沿用目标 APK 已有状态） */
    showLaunchOptions: Boolean = true
) {
    PvzSectionTitle("集成选项")

    // 简易模式开关（更新模式无描述文件时隐藏，沿用目标 APK 已有状态）
    if (showLaunchOptions) {
        PvzInfoCard("简易模式 (simplifiedLaunch)") {
            PvzBodyText("开启后跳过完整主界面，只解压基础资源后直接进入游戏。")
            Spacer(Modifier.height(6.dp))
            PvzCheckRow("启用简易模式", simplifiedLaunch) { onSimplifiedLaunch(!simplifiedLaunch) }
        }
    }

    if (!simplifiedLaunch) {
        if (showLaunchOptions) {
            PvzInfoCard("示例栏目") {
                PvzCheckRow("保留示例栏目", includeExamples) { onIncludeExamples(!includeExamples) }
            }
        }
        PvzRowLink("版本设置 →") { onOpenVersionSettings() }
        PvzRowLink("栏目设置 →") { onOpenSectionSettings() }
        PvzRowLink("公告设置 →") { onOpenAnnouncementSettings() }
        PvzRowLink("顶栏图标设置 →") { onOpenTopBarIconSettings() }
        PvzRowLink("定时任务设置 →") { onOpenScheduleSettings() }
    }
    PvzRowLink("悬浮窗设置 →") { onOpenFloatingWindowSettings() }
    PvzRowLink("SMF/资源设置 →") { onOpenSmfResourceSettings() }

    // UI设置入口
    PvzRowLink("UI 设置 →") { onOpenUiSettings() }
}

// ── 第 3 步：预览与执行（左右分栏）─────────────────────────────

@Composable
private fun StepPreviewLeft(
    report: IntegrateReport?,
    result: MergeResult?,
    loading: Boolean,
    onRecompute: () -> Unit,
    onApply: () -> Unit
) {
    PvzSectionTitle("第 3 步 · 差异报告")

    // 合并成功 → 显示快捷摘要
    result?.let {
        PvzInfoCard("✓ 合并已完成") {
            PvzHighlightText("输出：${it.outputApk.name}")
            PvzBodyText("大小：${"%.2f".format(it.outputApk.length() / 1024f / 1024f)} MB")
        }
        return
    }

    // 未计算
    if (report == null && !loading) {
        PvzBodyText("点击右侧「重新计算」按钮生成差异报告。")
        return
    }

    // 差异报告
    report?.let { r ->
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PvzStatChip("dex ${r.sourceDexCount}+${r.targetDexCount}=${r.resultDexCount}")
            PvzStatChip("res +${r.resAdded}/覆${r.resOverwritten}")
            PvzStatChip("assets +${r.assetsAdded}")
        }
        PvzStatChipRow(
            "lib ABI：" + r.libAbis.joinToString(),
            "dream.yml 新增${r.dreamYmlAdded}/跳过${r.dreamYmlSkipped}"
        )

        PvzInfoCard("manifest 变更") {
            r.manifestChanges.forEach { PvzBodyText("• $it") }
            PvzHighlightText("目标包名：${r.targetPackage}")
            PvzHighlightText("arsc 包（合并后）：${r.arscTargetPackagesAfter.joinToString()}")
        }

        PvzInfoCard("即将注入的追加文件") {
            if (r.extraFiles.isEmpty()) {
                PvzBodyText("（无，全部使用工具箱内置资源）")
            } else {
                r.extraFiles.forEach { PvzBodyText("• $it") }
            }
        }

        // 「选择后删除」：从目标 APK 中移除的原始条目
        if (r.targetRemoved.isNotEmpty()) {
            PvzInfoCard("将从目标 APK 删除的条目（${r.targetRemoved.size}）") {
                r.targetRemoved.forEach { PvzBodyText("• $it") }
            }
        }
    }
}

@Composable
private fun StepPreviewRight(
    report: IntegrateReport?,
    result: MergeResult?,
    includeExamples: Boolean,
    configReady: Boolean,
    dreamYmlFull: String,
    simplifiedLaunch: Boolean,
    errorMsg: String?,
    onPreviewUi: () -> Unit,
    onRestart: () -> Unit
) {
    PvzSectionTitle("操作与预览")

    // 错误信息
    errorMsg?.let {
        PvzWarningCard("⚠ $it")
        Spacer(Modifier.height(8.dp))
    }

    // 合并成功
    result?.let {
        // 需用户手动介入的告警优先于成功卡展示：产物虽已生成，但不照做可能导致运行异常，必须先看到。
        // 当前合并管线不产生告警（列表恒空），此处保留为扩展点。
        it.report.warnings.forEach { w ->
            PvzWarningCard("⚠ $w")
            Spacer(Modifier.height(8.dp))
        }
        PvzSuccessCard(
            path = it.outputApk.absolutePath,
            sizeMb = "%.2f".format(it.outputApk.length() / 1024f / 1024f),
            notes = it.report.notes
        )
        return
    }

    // 界面预览（简易模式没有界面，不显示）
    if (report != null && !simplifiedLaunch) {
        PvzInfoCard("预览界面") {
            PvzGreenButton(
                "预览界面效果",
                Modifier
                    .fillMaxWidth()
                    .height(BUTTON_HEIGHT),
                onClick = onPreviewUi
            )
            PvzBodyText("点击预览 dream.yml 在合并后的实际界面效果。")
        }
    }
}

// ── PVZ 风格小组件 ──────────────────────────────────────────────

private val PvzTextOliveStyleNoShadow = PvzTextOliveStyle.copy(shadowColor = null)

@Composable
private fun PvzSectionTitle(text: String) {
    PvzRichText(
        text,
        defaultStyle = PvzTextOliveStyleNoShadow,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun PvzInfoCard(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF8F4D5))
            .border(1.dp, PvzBorderBrown, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        PvzRichText(title, defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@Composable
private fun PvzWarningCard(text: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFFF3E0))
            .border(1.dp, Color(0xFFD32F2F), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        PvzRichText(text, defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 13.sp)
    }
}

@Composable
private fun PvzBodyText(text: String) {
    PvzRichText(
        text,
        defaultStyle = PvzTextOliveStyle.copy(shadowColor = null),
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
}

@Composable
private fun PvzHighlightText(text: String) {
    PvzRichText(
        text,
        defaultStyle = PvzTextOliveStyleNoShadow,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 2.dp)
    )
}

@Composable
private fun PvzChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) PvzGreenSurface else Color.Transparent)
            .border(1.dp, if (selected) PvzGreen else Color(0xFFD5CFA0), RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (selected) "●" else "○",
            fontSize = 14.sp,
            color = if (selected) PvzGreen else PvzBorderBrown
        )
        Spacer(Modifier.width(8.dp))
        PvzRichText(label, defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 13.sp)
    }
}

@Composable
private fun PvzCheckRow(title: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PvzRichText(
            title,
            defaultStyle = PvzTextOliveStyleNoShadow,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Image(
            imageVector = if (checked) Pvz2Icon.HookSelect else Pvz2Icon.Hook,
            contentDescription = if (checked) "已选中" else "未选中",
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun PvzStatChip(text: String) {
    Box(
        Modifier
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(PvzGreenSurface)
            .border(1.dp, PvzGreen, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 11.sp, color = Color(0xFF33691E))
    }
}

@Composable
private fun PvzStatChipRow(vararg texts: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        texts.forEach { PvzStatChip(it) }
    }
}

@Composable
private fun PvzSuccessCard(path: String, sizeMb: String, notes: List<String>) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(PvzGreenSurface)
            .border(1.5.dp, Color(0xFF558B2F), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        PvzRichText(
            "✓ 合并完成",
            defaultStyle = PvzTextOliveStyleNoShadow,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        PvzHighlightText("输出：$path")
        PvzHighlightText("大小：$sizeMb MB")
        notes.forEach { PvzBodyText("• $it") }
        Spacer(Modifier.height(4.dp))
        PvzWarningCard("请用 MT 管理器对该 APK 签名后安装。")
    }
}

// ── 集成器小组件：文本输入 ─────────────────────────────────────

@Composable
private fun IntegratorInputField(value: String, placeholder: String, multiline: Boolean = false, onValue: (String) -> Unit) {
    PvzSimpleCardBrown(
        modifier = Modifier.fillMaxWidth(),
        borderColor = PvzCollapsiblePanelTheme.GREEN.sliderInactiveColor,
        backgroundColor = PvzCollapsiblePanelTheme.GREEN.sliderInactiveColor
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            singleLine = !multiline,
            textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 13.sp),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 13.sp,
                            color = Color(0xCCFFFFFF)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

// ── 复合文本工具（右上角入口弹窗）─────────────────────────────

/** 复合文本类型 */
private enum class CtType(val label: String, val mainLabel: String) {
    PLAIN("普通文字", "文字内容"),
    COLOR("颜色文本", "文字内容"),
    JS("JS表达式", "JS表达式"),
    ICON("图标", "图标路径"),
    LINK("链接", "显示文字")
}

/** 颜色选取模式 */
private enum class CtColorMode(val label: String) {
    PRESET("预设色"),
    CUSTOM("自定义色")
}

/** 预设色（与 PvzText.DefaultPvzTagStyles 命名一致，含 -shadow 变体） */
private val CT_PRESET_COLORS = listOf(
    "green", "purple", "red", "gold", "gray", "white", "olive",
    "black", "grey", "blue", "yellow", "orange", "cyan", "pink",
    "green-shadow", "purple-shadow", "red-shadow", "gold-shadow",
    "gray-shadow", "white-shadow", "olive-shadow"
)

/** 链接样式后缀（对应 link-<style>） */
private val CT_LINK_STYLES = listOf("", "green", "purple", "red", "gold", "gray", "white", "olive", "blue", "cyan", "pink")

/**
 * 根据所选类型与参数拼接出最终的复合文本标记。
 * - 普通文字：原样返回
 * - 颜色文本：{{color:内容}} 或 {{#RRGGBB:内容}}
 * - JS表达式：{{js:表达式}}
 * - 图标：{{icon:path}}（宽高留空则省略，由字号自适应）或 {{icon|width=w|height=h:path}}（可选 x/y/z：带 x 或 y 即进入浮层模式，不占位、按文本起点偏移覆盖）
 * - 链接：{{link:url:显示}} 或 {{link-green:url:显示}}（url 可为网址——点击用浏览器打开，或 JS 代码/.js 文件——点击执行）
 */
private fun buildCompositeText(
    type: CtType,
    text: String,
    colorMode: CtColorMode,
    presetColor: String,
    customHex: String,
    iconWidth: String,
    iconHeight: String,
    iconX: String,
    iconY: String,
    iconZ: String,
    linkUrl: String,
    linkStyle: String
): String {
    return when (type) {
        CtType.PLAIN -> text
        CtType.COLOR -> {
            val c = if (colorMode == CtColorMode.PRESET) presetColor else customHex.trim()
            if (c.isBlank()) text else "{{$c:$text}}"
        }
        CtType.JS -> if (text.isBlank()) "" else "{{js:$text}}"
        CtType.ICON -> {
            // 仅当用户填写才追加对应参数：宽/高留空即省略（解析器按 fontSize*1.2f 自适应）；
            // x 或 y 任一存在即触发浮层模式；z 控制层级（>0 在文字之上、<0 之下）。
            val parts = mutableListOf<String>()
            if (iconWidth.isNotBlank()) parts += "width=$iconWidth"
            if (iconHeight.isNotBlank()) parts += "height=$iconHeight"
            if (iconX.isNotBlank()) parts += "x=$iconX"
            if (iconY.isNotBlank()) parts += "y=$iconY"
            if (iconZ.isNotBlank()) parts += "z=$iconZ"
            if (parts.isEmpty()) "{{icon:$text}}"
            else "{{icon|${parts.joinToString("|")}:$text}}"
        }
        CtType.LINK -> {
            val tag = "link" + if (linkStyle.isNotBlank()) "-$linkStyle" else ""
            "{{$tag|$linkUrl:$text}}"
        }
    }
}

/** 类型选择器（分段按钮） */
@Composable
private fun CtTypeSelector(selected: CtType, onSelect: (CtType) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CtType.values().forEach { t ->
            val sel = t == selected
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (sel) PvzGreenBright else PvzGreenSurface)
                    .border(1.dp, PvzGreen, RoundedCornerShape(8.dp))
                    .clickable { onSelect(t) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                PvzRichText(
                    t.label,
                    defaultStyle = if (sel) PvzTextWhiteStyle.copy(shadowColor = null) else PvzTextOliveStyleNoShadow,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** 下拉选择器（点击展开内联列表，避免引入额外依赖） */
@Composable
private fun CtSpinner(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        PvzRichText(label, defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(PvzGreenSurface)
                .border(1.dp, PvzGreen, RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
                .padding(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                PvzRichText(
                    if (selected.isBlank()) "（默认/无）" else selected,
                    defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 13.sp
                )
                PvzRichText("▼", defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 12.sp)
            }
        }
        if (expanded) {
            Spacer(Modifier.height(4.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(1.dp, PvzGreen, RoundedCornerShape(8.dp))
            ) {
                options.forEach { opt ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(opt); expanded = false }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        PvzRichText(
                            if (opt.isBlank()) "（默认/无）" else opt,
                            defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 复合文本工具弹窗：输入文字 → 选择类型 → 按类型显示更多自定义参数 → 实时预览 → 复制按钮复制最终复合文本。
 */
@Composable
private fun CompositeTextToolDialog(onDismiss: () -> Unit) {
    var ctType by remember { mutableStateOf(CtType.PLAIN) }
    var ctText by remember { mutableStateOf("") }
    var ctColorMode by remember { mutableStateOf(CtColorMode.PRESET) }
    var ctPresetColor by remember { mutableStateOf("green") }
    var ctCustomHex by remember { mutableStateOf("#FF0000") }
    var ctIconWidth by remember { mutableStateOf("") }
    var ctIconHeight by remember { mutableStateOf("") }
    var ctIconX by remember { mutableStateOf("") }
    var ctIconY by remember { mutableStateOf("") }
    var ctIconZ by remember { mutableStateOf("") }
    var ctLinkUrl by remember { mutableStateOf("") }
    var ctLinkStyle by remember { mutableStateOf("") }

    val generated = remember(ctType, ctText, ctColorMode, ctPresetColor, ctCustomHex, ctIconWidth, ctIconHeight, ctIconX, ctIconY, ctIconZ, ctLinkUrl, ctLinkStyle) {
        buildCompositeText(ctType, ctText, ctColorMode, ctPresetColor, ctCustomHex, ctIconWidth, ctIconHeight, ctIconX, ctIconY, ctIconZ, ctLinkUrl, ctLinkStyle)
    }

    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    PvzStyledDialog(
        isVisible = true,
        titleText = "复合文本工具",
        onDismissRequest = onDismiss,
        dismissible = true,
        horizontalAlignment = Alignment.CenterHorizontally,
        bottomContent = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PvzRedButton(
                    text = "取消",
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    onClick = onDismiss
                )
                PvzGreenButton(
                    text = "复制",
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    onClick = {
                        scope.launch {
                            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("composite_text", generated)))
                        }
                        Toast.makeText(context, "已复制：$generated", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                )
            }
        }
    ) {
        // 内容整体加边距（包住全部，避免类型以下顶格）
        Column(Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)) {
            // 文字内容
            Column(Modifier.fillMaxWidth()) {
                PvzRichText(ctType.mainLabel, defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                IntegratorInputField(ctText, ctType.mainLabel, multiline = true) { ctText = it }
            }

        Spacer(Modifier.height(12.dp))

        // 类型选择
        PvzRichText("类型", defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        CtTypeSelector(ctType) { ctType = it }

        Spacer(Modifier.height(12.dp))

        // 类型相关自定义参数
        when (ctType) {
            CtType.COLOR -> {
                // 颜色模式切换
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CtColorMode.values().forEach { m ->
                        val sel = m == ctColorMode
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) PvzGreenBright else PvzGreenSurface)
                                .border(1.dp, PvzGreen, RoundedCornerShape(8.dp))
                                .clickable { ctColorMode = m }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            PvzRichText(m.label, defaultStyle = if (sel) PvzTextWhiteStyle.copy(shadowColor = null) else PvzTextOliveStyleNoShadow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (ctColorMode == CtColorMode.PRESET) {
                    CtSpinner("预设色", CT_PRESET_COLORS, ctPresetColor) { ctPresetColor = it }
                } else {
                    Column(Modifier.fillMaxWidth()) {
                        PvzRichText("十六进制色（如 #FF0000 / #AARRGGBB）", defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        IntegratorInputField(ctCustomHex, "#FF0000") { ctCustomHex = it }
                    }
                }
            }
            CtType.ICON -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        PvzRichText("宽度(留空自适应)", defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        IntegratorInputField(ctIconWidth, "留空自适应") { ctIconWidth = it }
                    }
                    Column(Modifier.weight(1f)) {
                        PvzRichText("高度(留空自适应)", defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        IntegratorInputField(ctIconHeight, "留空自适应") { ctIconHeight = it }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        PvzRichText("X偏移(dp,可选)", defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        IntegratorInputField(ctIconX, "如 10") { ctIconX = it }
                    }
                    Column(Modifier.weight(1f)) {
                        PvzRichText("Y偏移(dp,可选)", defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        IntegratorInputField(ctIconY, "-6") { ctIconY = it }
                    }
                    Column(Modifier.weight(1f)) {
                        PvzRichText("层级z(可选)", defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        IntegratorInputField(ctIconZ, "2") { ctIconZ = it }
                    }
                }
                Spacer(Modifier.height(4.dp))
                PvzRichText("提示：图标相对 images/ 目录，如 auto_collect.png", defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 12.sp)
                Spacer(Modifier.height(2.dp))
                PvzRichText("填写 X 或 Y 即进入浮层模式：图标不再占位，从整段文本起点按偏移覆盖其他文字；z>0 在文字之上、<0 在下、可分层。", defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 12.sp)
            }
            CtType.LINK -> {
                Column(Modifier.fillMaxWidth()) {
                    PvzRichText("链接地址 / JS代码(点击执行)", defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    IntegratorInputField(ctLinkUrl, "https://... 或 JS代码") { ctLinkUrl = it }
                }
                Spacer(Modifier.height(8.dp))
                CtSpinner("链接样式(可选)", CT_LINK_STYLES, ctLinkStyle) { ctLinkStyle = it }
                Spacer(Modifier.height(4.dp))
                PvzRichText(
                    "提示：地址可为网址（点击用浏览器打开），也可填 JS 代码或 .js 文件（点击即执行，相当于点击式 JS）。",
                    defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 12.sp
                )
            }
            else -> {
                PvzRichText(
                    if (ctType == CtType.JS) "提示：表达式将在运行时求值，如 vpn.isActive() ? '开' : '关'" else "无需额外参数",
                    defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // 实时预览（图标两侧加示例文字，便于查看图标在文字中的内联效果；
        // 若使用 X/Y 坐标，需有周围文字作为锚点，否则图标因无文本起点可能不显示）
        PvzRichText("预览", defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        PvzSimpleCardGreen(borderColor = Color(0xFFD4E8A0), backgroundColor = Color(0xFFD4E8A0)) {
            Box(Modifier
                .fillMaxWidth()
                .padding(12.dp), contentAlignment = Alignment.Center) {
                PvzRichText(
                    if (generated.isBlank()) "（输出为空）" else "示例文字 $generated 示例文字",
                    defaultStyle = PvzTextOliveStyleNoShadow,
                    fontSize = 15.sp
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        PvzRichText(
            "提示：使用 X/Y 坐标时，图标漂浮在文字之上/下且不占位，需要周围有文字提供起点锚点；若整段只有图标本身，坐标模式可能无法定位而不显示。",
            defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 12.sp
        )

        Spacer(Modifier.height(10.dp))

        // 生成的原始文本
        PvzRichText("生成结果", defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        UiInfoValue(generated.ifBlank { "（输出为空）" })
        }
    }
}

@Composable
private fun PvzRowLink(label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(PvzGreenSurface)
            .border(1.dp, PvzGreen, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        PvzRichText(label, defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        ImageSvgButton(
            Pvz2Icon.ArrowRight,
            Pvz2Icon.ArrowRightPress,
            "前往",
            Modifier.size(25.dp),
            onClick = onClick
        )
    }
}

// ── UI设置子页面的小组件（对标 Pvz2MainScreen 的 SectionType 组件）──

/** 区段标题 —— 对应 DESCRIPTION 区段外的大标题 */
@Composable
private fun UiSectionHeader(text: String) {
    PvzRichText(
        text, defaultStyle = PvzTextOliveStyleNoShadow,
        fontSize = 17.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

/** Info 卡片（对标 Pvz2MainScreen 的 INFO 类型：浅绿底 + 自动对比度文字） */
@Composable
private fun UiInfoCard(label: String, content: @Composable () -> Unit) {
    Column(Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        PvzRichText(label, defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        content()
    }
}

/** Info 值（PvzSimpleCardGreen 浅色背景 + 自动对比文字 — 对标 Pvz2MainScreen INFO）。使用纯 Text 避免解析复合文本标记。 */
@Composable
private fun UiInfoValue(text: String) {
    val bgColor = Color(0xFFD4E8A0) // sliderActiveColor 风格
    val textColor = Color(0xFF33691E) // 深绿文字保证对比度
    PvzSimpleCardGreen(borderColor = bgColor, backgroundColor = bgColor) {
        Text(
            text, fontSize = 14.sp, lineHeight = 18.sp,
            color = textColor,
            modifier = Modifier.padding(12.dp)
        )
    }
}

/** 描述文字（对标 Pvz2MainScreen 的 DESCRIPTION 类型）。使用纯 Text 避免 PvzRichText 解析掉 {{red:}} 等复合文本标记。 */
@Composable
private fun UiDescription(text: String) {
    Text(text, fontSize = 12.sp, lineHeight = 16.sp, color = Color(0xFF5D4E37))
}

/** Input 卡片（对标 Pvz2MainScreen 的 INPUT 类型） */
@Composable
private fun UiInputCard(label: String, desc: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(PvzCream)
            .border(1.dp, Color(0xFFD5CFA0), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        PvzRichText(label, defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        UiDescription(desc)
        Spacer(Modifier.height(4.dp))
        content()
    }
}

/** Switch 卡片 */
@Composable
private fun UiSwitchCard(label: String, desc: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(PvzCream)
            .border(1.dp, Color(0xFFD5CFA0), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        PvzRichText(label, defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        UiDescription(desc)
        Spacer(Modifier.height(2.dp))
        content()
    }
}

/** 文件输入行：输入框 + 选择按钮。仅当路径为相对路径（非空、非绝对路径、非 URL）时显示选择按钮。
 * 当 selectedFile 为可解码图片时，额外显示可点击缩略图（点开全屏预览）。非图片字段不传 selectedFile 即可。 */
/**
 * 从 App 自带 assets（pvz2tool/...）解析 value 指向的默认打包图片，用于向导未手动选图时也能预览默认图标。
 * value 形如 `new_version_icon.png` 或 `images/new_version_icon.png`，统一归一到 `pvz2tool/images/<name>`。
 * 非图片字段（js/音频路径）open 失败自然返回 null，不会显示缩略图。
 */

// ── SMF/资源设置子页面 ─────────────────────────────────────────────

private data class SmfSourceEntry(val relName: String, val fullRel: String)

/** 待打包完成后删除的本地文件/文件夹（SAF 来源）。 */
private data class PendingLocalDeletion(val uri: String, val label: String)

/** 列出源 APK 中某 scope 资源目录下所有文件（fullRel 为相对 assets/pvz2tool/ 的路径），用于 SMF/资源设置页。 */
private fun listSmfSourceEntries(apk: File, scope: String): List<SmfSourceEntry> {
    val prefix = "assets/pvz2tool/$scope/"
    return runCatching {
        val module = ApkModule.loadApkFile(apk)
        module.listInputSources().mapNotNull { ins ->
            val name = ins.name
            if (name.startsWith(prefix)) {
                val rel = name.removePrefix(prefix)
                if (rel.isEmpty()) null
                else SmfSourceEntry(relName = rel, fullRel = name.removePrefix("assets/pvz2tool/").trimStart('/'))
            } else null
        }.sortedBy { it.relName }
    }.getOrDefault(emptyList())
}

@Composable
private fun SmfScopeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(end = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) PvzGreen else PvzGreenSurface)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 12.sp, color = if (selected) Color.White else Color(0xFF33691E))
    }
}

@Composable
private fun SmfResourceSettingsContent(
    sourceApk: File,
    baseAssetPath: String,
    onBaseAssetPath: (String) -> Unit,
    versions: List<VersionDraft>,
    onUpdateVersions: (List<VersionDraft>) -> Unit,
    simplifiedLaunch: Boolean = false,
    excludedSmfAssets: Set<String>,
    onToggleExclude: (String) -> Unit,
    addedSmfFiles: Map<String, File>,
    onAddSmfFile: (String, File) -> Unit,
    addedSmfFolders: Map<String, File>,
    onAddSmfFolder: (String, File) -> Unit,
    onRemoveAdded: (String) -> Unit,
    onPickSmfFile: (String) -> Unit,
    onPickSmfFolder: (String) -> Unit,
    /** 「选择后删除」总开关：仅作用于本页的资源选择 */
    deleteAfterPick: Boolean,
    onDeleteAfterPick: (Boolean) -> Unit,
    /** 目标 APK 是否已选（未选时「从目标 APK 选择」不可用） */
    hasTargetApk: Boolean,
    /** 打开目标 APK 条目浏览器（参数 = 追加到的 scope） */
    onPickFromTargetApk: (String) -> Unit,
    /** 已登记「打包时从目标 APK 删除」的原始条目 */
    removedTargetEntries: Set<String>,
    onUndoRemoveTarget: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scopeIndex by remember { mutableStateOf(-1) } // -1 = 通用 base
    val scopePath = if (scopeIndex < 0) {
        baseAssetPath.trim().trimEnd('/')
    } else {
        versions.getOrNull(scopeIndex)?.let { v ->
            (v.assetPath.ifBlank { "version/${v.id}/smf" }).trim().trimEnd('/')
        } ?: "version/unknown/smf"
    }

    // 源 APK 真实文件列表（随 scopePath 变化重新枚举，不写死）
    var entries by remember { mutableStateOf<List<SmfSourceEntry>>(emptyList()) }
    var loadingEntries by remember { mutableStateOf(false) }
    LaunchedEffect(scopePath, sourceApk) {
        loadingEntries = true
        val list = withContext(Dispatchers.IO) { listSmfSourceEntries(sourceApk, scopePath) }
        entries = list
        loadingEntries = false
    }

    Column(modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 10.dp, vertical = 10.dp)) {
        PvzDialogCard(title = null) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                UiSectionHeader("SMF/资源设置")
                UiDescription("管理 CoreFunctionSection.onEnterGame 解压的通用资源与版本特有资源。删除仅排除即将打包 APK 中的该文件；追加的文件/文件夹只进入即将打包的 APK。列表随下方资源目录变化（改目录名后重新枚举，不重置选择）。")

                Spacer(Modifier.height(8.dp))
                // 「选择后删除」总开关：追加本地文件删除原文件；从目标 APK 选择则登记打包时删除原条目
                PvzCheckRow(
                    "选择后删除原件（追加后删除来源）",
                    deleteAfterPick
                ) { onDeleteAfterPick(!deleteAfterPick) }
                Text(
                    "开启后：追加本地文件会删除原文件；从目标 APK 选择会在打包时删除目标 APK 中的原条目（不改动已安装 APK）。Manifest/arsc/dex/res/META-INF/assets/pvz2tool 受保护条目不会被删除。",
                    fontSize = 11.sp, color = Color.Gray
                )

                Spacer(Modifier.height(8.dp))
                if (!simplifiedLaunch) {
                Text("资源范围", fontSize = 13.sp, color = Color(0xFF33691E), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    SmfScopeChip("通用 base", selected = scopeIndex < 0) { scopeIndex = -1 }
                    versions.forEachIndexed { i, v ->
                        SmfScopeChip("版本：${v.name}", selected = scopeIndex == i) { scopeIndex = i }
                    }
                }
                }

                Spacer(Modifier.height(8.dp))
                val isBase = scopeIndex < 0
                IntegratorInputField(
                    if (isBase) baseAssetPath else (versions.getOrNull(scopeIndex)?.assetPath ?: ""),
                    "资源目录（相对 assets/pvz2tool/，如 version/base/smf）"
                ) { newValue ->
                    if (isBase) onBaseAssetPath(newValue) else {
                        versions.getOrNull(scopeIndex)?.let { v ->
                            onUpdateVersions(versions.toMutableList().also { it[scopeIndex] = it[scopeIndex].copy(assetPath = newValue) })
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("源 APK 内资源（${entries.size}）", fontSize = 13.sp, color = Color(0xFF33691E), fontWeight = FontWeight.Bold)
                    if (loadingEntries) {
                        Spacer(Modifier.width(8.dp))
                        Text("加载中…", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Spacer(Modifier.height(4.dp))
                if (entries.isEmpty() && !loadingEntries) {
                    PvzInfoCard("该目录在源 APK 中不存在") {
                        PvzBodyText("源 APK（工具箱自身）assets/pvz2tool/$scopePath/ 下没有文件。可直接「追加文件/文件夹」向即将打包的 APK 注入资源。")
                    }
                } else {
                    entries.forEach { e ->
                        val excluded = e.fullRel in excludedSmfAssets
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .background(
                                    if (excluded) Color(0xFFFFEBEE) else Color.Transparent, RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(e.relName, fontSize = 13.sp, color = if (excluded) Color.Gray else Color(0xFF1B5E20))
                                if (excluded) Text("已排除（不会打包）", fontSize = 11.sp, color = Color(0xFFC62828))
                            }
                            PvzRedButton(if (excluded) "恢复" else "排除", Modifier.height(32.dp)) {
                                onToggleExclude(e.fullRel)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text("追加到即将打包的 APK（scope = $scopePath）", fontSize = 13.sp, color = Color(0xFF33691E), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PvzGreenButton("追加文件", Modifier.height(36.dp)) { onPickSmfFile(scopePath) }
                    PvzGreenButton("追加文件夹", Modifier.height(36.dp)) { onPickSmfFolder(scopePath) }
                    if (hasTargetApk) {
                        PvzGreenButton("从目标 APK 选择", Modifier.height(36.dp)) { onPickFromTargetApk(scopePath) }
                    } else {
                        Box(
                            Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFBDBDBD))
                                .border(1.dp, Color(0xFF9E9E9E), RoundedCornerShape(6.dp))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("从目标 APK 选择（请先选目标 APK）", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }

                // 已追加文件（属于当前 scope）
                val addedFilesHere = addedSmfFiles.filterKeys { it.startsWith("$scopePath/") }
                if (addedFilesHere.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("已追加文件（${addedFilesHere.size}）", fontSize = 12.sp, color = Color(0xFF33691E))
                    addedFilesHere.forEach { (key, _) ->
                        val name = key.removePrefix("$scopePath/")
                        Row(Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(name, fontSize = 13.sp, color = Color(0xFF1B5E20), modifier = Modifier.weight(1f))
                            PvzRedButton("移除", Modifier.height(30.dp)) { onRemoveAdded(key) }
                        }
                    }
                }
                // 已追加文件夹（属于当前 scope）
                val addedFolderHere = addedSmfFolders.filterKeys { it == scopePath }
                if (addedFolderHere.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("已追加文件夹（${addedFolderHere.size}）", fontSize = 12.sp, color = Color(0xFF33691E))
                    addedFolderHere.forEach { (key, folder) ->
                        Row(Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${folder.name}（${folder.walkTopDown().count { it.isFile }} 个文件）", fontSize = 13.sp, color = Color(0xFF1B5E20), modifier = Modifier.weight(1f))
                            PvzRedButton("移除", Modifier.height(30.dp)) { onRemoveAdded(key) }
                        }
                    }
                }

                // 已登记「打包时从目标 APK 删除」的条目（全局，跨 scope 展示）
                if (removedTargetEntries.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("打包时将删除的目标 APK 原条目（${removedTargetEntries.size}）", fontSize = 12.sp, color = Color(0xFFB71C1C), fontWeight = FontWeight.Bold)
                    removedTargetEntries.sorted().forEach { name ->
                        Row(Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(name, fontSize = 11.sp, color = Color(0xFF8D4E00), modifier = Modifier.weight(1f))
                            PvzRedButton("撤销", Modifier.height(30.dp)) { onUndoRemoveTarget(name) }
                        }
                    }
                }
            }
        }
    }
}

// ── 目标 APK 条目浏览器（从目标 APK 选择条目追加为 SMF 资源） ──────────

private class ApkTreeNode(
    val name: String,
    val fullPath: String,
    val isDir: Boolean,
    val children: MutableList<ApkTreeNode> = mutableListOf()
)

/** 文件名含 smf/rsb/obb 视为特殊资源（高亮 + 置顶）。 */
private fun isSpecialResourceName(name: String): Boolean {
    val n = name.lowercase()
    return n.contains("smf") || n.contains("rsb") || n.contains("obb")
}

/** 排序：文件夹在前 → 特殊文件置顶 → 其余文件；组内按名称。 */
private val apkNodeComparator = compareBy<ApkTreeNode>(
    { !it.isDir },
    { !(it.isDir || isSpecialResourceName(it.name)) },
    { it.name.lowercase() }
)

/** 由 APK 全量条目名构建目录树（已排序）。 */
private fun buildApkEntryTree(entries: List<String>): ApkTreeNode {
    val root = ApkTreeNode("", "", true)
    for (e in entries) {
        val parts = e.split('/').filter { it.isNotEmpty() }
        if (parts.isEmpty()) continue
        var cur = root
        for (i in parts.indices) {
            val seg = parts[i]
            val path = parts.subList(0, i + 1).joinToString("/")
            val isLast = i == parts.size - 1
            if (isLast) {
                cur.children.add(ApkTreeNode(seg, path, isDir = false))
            } else {
                val existing = cur.children.firstOrNull { it.isDir && it.name == seg }
                cur = existing ?: ApkTreeNode(seg, path, true).also { cur.children.add(it) }
            }
        }
    }
    fun sortNode(node: ApkTreeNode) {
        node.children.sortWith(apkNodeComparator)
        node.children.forEach { if (it.isDir) sortNode(it) }
    }
    sortNode(root)
    return root
}

/** 搜索模式：扁平收集匹配文件节点（按特殊优先、名称排序）。 */
private fun flattenMatching(root: ApkTreeNode, query: String): List<ApkTreeNode> {
    val q = query.lowercase().trim()
    val out = mutableListOf<ApkTreeNode>()
    fun walk(node: ApkTreeNode) {
        if (!node.isDir && node.name.lowercase().contains(q)) out.add(node)
        node.children.forEach { walk(it) }
    }
    walk(root)
    return out.sortedWith(apkNodeComparator)
}

@Composable
private fun TargetApkBrowserDialog(
    targetApk: File?,
    assetScope: String,
    onDismiss: () -> Unit,
    onPickEntry: (String) -> Unit,
    /** 已选条目（APK 内完整路径集合）；命中时该条目不再显示「选择」按钮，改为显示「✓ 已选」 */
    selectedEntries: Set<String> = emptySet()
) {
    var loading by remember { mutableStateOf(true) }
    var root by remember { mutableStateOf<ApkTreeNode?>(null) }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(targetApk) {
        loading = true
        root = null
        val apk = targetApk
        if (apk == null) { loading = false; return@LaunchedEffect }
        val entries = withContext(Dispatchers.IO) {
            runCatching {
                ApkModule.loadApkFile(apk).listInputSources().map { it.name }
            }.getOrDefault(emptyList())
        }
        root = buildApkEntryTree(entries)
        loading = false
    }

    PvzStyledDialog(
        isVisible = true,
        titleText = "从目标 APK 选择（追加到 $assetScope）",
        onDismissRequest = onDismiss,
        dismissible = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        bottomContent = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PvzRedButton("关闭", Modifier
                    .weight(1f)
                    .height(40.dp)) { onDismiss() }
            }
        }
    ) {
        Column(Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)) {
            if (loading) {
                Text("正在读取目标 APK 条目…", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
            } else {
                val r = root
                if (r == null) {
                    Text("无法读取目标 APK 条目。", fontSize = 13.sp, color = Color(0xFFC62828), modifier = Modifier.padding(vertical = 8.dp))
                } else {
                    IntegratorInputField(query, "过滤文件名（smf / rsb / obb…）") { query = it }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "文件夹在前；含 smf/rsb/obb 的文件高亮并置顶。点文件夹展开，点「选择」追加该条目到 $assetScope。",
                        fontSize = 11.sp, color = Color.Gray
                    )
                    Spacer(Modifier.height(6.dp))
                    if (query.isBlank()) {
                        ApkTreeNodes(r.children, 0, expanded, onPickEntry, selectedEntries)
                    } else {
                        val matches = remember(r, query) { flattenMatching(r, query) }
                        if (matches.isEmpty()) {
                            Text("无匹配条目。", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                        } else {
                            matches.forEach { node ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val special = isSpecialResourceName(node.name)
                                    Text(
                                        node.fullPath,
                                        fontSize = 12.sp,
                                        color = if (special) Color(0xFF6A1B9A) else Color(0xFF1B5E20),
                                        fontWeight = if (special) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (node.fullPath in selectedEntries) {
                                        Text("✓ 已选", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(end = 8.dp))
                                    } else {
                                        PvzGreenButton("选择", Modifier.height(28.dp)) { onPickEntry(node.fullPath) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApkTreeNodes(
    nodes: List<ApkTreeNode>,
    depth: Int,
    expanded: SnapshotStateMap<String, Boolean>,
    onPickEntry: (String) -> Unit,
    selectedEntries: Set<String> = emptySet()
) {
    nodes.forEach { node ->
        val indent = (depth * 14 + 4).dp
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = indent, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (node.isDir) {
                val isOpen = expanded[node.fullPath] == true
                Row(
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { expanded[node.fullPath] = !isOpen }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isOpen) "▼" else "▶", fontSize = 10.sp, color = Color(0xFF558B2F))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${node.name}/",
                        fontSize = 13.sp,
                        color = Color(0xFF33691E),
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                val special = isSpecialResourceName(node.name)
                Text(
                    node.name,
                    fontSize = 13.sp,
                    color = if (special) Color(0xFF6A1B9A) else Color(0xFF1B5E20),
                    fontWeight = if (special) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                if (node.fullPath in selectedEntries) {
                    Text("✓ 已选", fontSize = 12.sp, color = Color.Gray)
                } else {
                    PvzGreenButton("选择", Modifier.height(28.dp)) { onPickEntry(node.fullPath) }
                }
            }
        }
        if (node.isDir && expanded[node.fullPath] == true) {
            ApkTreeNodes(node.children, depth + 1, expanded, onPickEntry, selectedEntries)
        }
    }
}

/** 解析默认图路径（用于 Coil AsyncImage）。更新模式下先从目标 APK 提取到临时文件。 */
@Composable
private fun rememberDefaultImagePath(context: Context, value: String, targetApk: File?): Any? {
    return remember(value, targetApk) {
        if (value.isBlank() || value.startsWith("/") || value.startsWith("http://") || value.startsWith("https://")) return@remember null
        val base = value.removePrefix("images/").removePrefix("pvz2tool/").removePrefix("pvz2tool")
        val candidates = listOf("pvz2tool/images/$base", "pvz2tool/$base")
        // 更新模式：从目标 APK 提取到临时文件
        if (targetApk != null) {
            for (c in candidates) {
                val apkPath = "assets/$c"
                runCatching {
                    ApkModule.loadApkFile(targetApk).use { module ->
                        module.getInputSource(apkPath)?.openStream()?.use { inp ->
                            val tmp = File(context.cacheDir, "thumb_${base.hashCode()}")
                            tmp.outputStream().use { inp.copyTo(it) }
                            return@remember tmp
                        }
                    }
                }
            }
        }
        // 回退：源 APK assets 路径（Coil 支持 android_asset URI）
        for (c in candidates) {
            try { context.assets.open(c).close(); return@remember "file:///android_asset/$c" } catch (_: Exception) {}
        }
        null
    }
}

@Composable
private fun FileInputRow(
    value: String, placeholder: String, mimeType: String,
    fieldKey: String, onValue: (String) -> Unit,
    onPickFile: (label: String, mimeType: String) -> Unit,
    onPickFolder: (fieldKey: String) -> Unit = {},
    selectedFile: File? = null,
    selectedFolder: File? = null,
    onImagePreview: (Any) -> Unit = {},
    /** 用户输入变化且需重置已选文件/文件夹时回调（仅当曾选过文件/文件夹时触发） */
    onClearSelection: () -> Unit = {},
    /** 更新模式：目标 APK（优先从此读取图片预览，而非源 APK 内置资源） */
    targetApk: File? = null
) {
    // 仅相对路径可打包进 APK：非空 && 非 / 开头 && 非 http/https 开头
    val isPackable = value.isNotBlank()
            && !value.startsWith("/")
            && !value.startsWith("http://")
            && !value.startsWith("https://")
    // 含父目录（如 tool/a.js）时，点击「选择」弹出 文件/文件夹 二选一（与导出选项弹窗同款）
    val hasParent = value.contains("/")
    var showChoice by remember { mutableStateOf(false) }
    // 已选过文件/文件夹时，按钮文案改为「重新选择」（行为不变，无需二次确认）
    val hasSelection = selectedFile != null || selectedFolder != null
    val pickLabel = if (hasSelection) "重新选择" else "选择"
    val pickFileLabel = if (hasSelection) "重新选择文件" else "选择文件"
    val pickFolderLabel = if (hasSelection) "重新选择文件夹" else "选择文件夹"

    // 覆盖风险检测（经 CompositionLocal 注入）：选择时若 APK 目标路径已被占用，需二次确认
    val onOverwritePath = LocalOverwriteChecker.current
    var showOverwriteConfirm by remember { mutableStateOf(false) }
    var confirmPath by remember { mutableStateOf("") }
    var pendingPick by remember { mutableStateOf<(() -> Unit)?>(null) }
    val requestPick: (() -> Unit) -> Unit = { action ->
        val p = onOverwritePath(fieldKey)
        if (p != null) {
            confirmPath = p
            pendingPick = action
            showOverwriteConfirm = true
        } else action()
    }

    // 记录选择生效时 value 的父目录，用于判断「仅改文件名未改目录」的例外（文件夹场景）
    val selectionActiveParentDir = remember(selectedFile, selectedFolder) {
        value.substringBeforeLast('/', "")
    }
    // 包装 onValue：输入变化导致选择失效时先重置选择
    val onValueWrapped: (String) -> Unit = { newVal ->
        val folder = selectedFolder
        val file = selectedFile
        if (folder != null) {
            // 文件夹：仅当父目录（文件所在目录名）变化时才重置；仅改文件名不重置
            if (newVal.substringBeforeLast('/', "") != selectionActiveParentDir) onClearSelection()
        } else if (file != null) {
            // 文件：任何输入变化都重置选择
            onClearSelection()
        }
        onValue(newVal)
    }

    Column(Modifier.fillMaxWidth()) {
        // 预览图放在输入框上方（与 bgFillImage 一致）。优先显示已选文件缩略图；未选文件时尝试解析 value 指向的默认打包图片
        val ctx = LocalContext.current
        val defaultModel = rememberDefaultImagePath(ctx, value, targetApk)
        val model: Any? = selectedFile ?: defaultModel
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = "图片预览",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onImagePreview(model) },
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(6.dp))
        } else if (selectedFolder != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PvzGreenSurface)
                    .border(1.dp, PvzGreen, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("已选文件夹：${selectedFolder.name}", fontSize = 13.sp, color = Color(0xFF33691E))
            }
            Spacer(Modifier.height(6.dp))
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                IntegratorInputField(value, placeholder, onValue = onValueWrapped)
            }
            if (isPackable) {
                Spacer(Modifier.width(6.dp))
                PvzBlueButton(pickLabel, Modifier.height(36.dp)) {
                    if (hasParent) showChoice = true else requestPick { onPickFile(fieldKey, mimeType) }
                }
            }
        }
    }

    // 含父目录时：选择 文件 / 文件夹（样式与导出选项弹窗一致）
    if (showChoice) {
        PvzStyledDialog(
            isVisible = showChoice,
            titleText = "选择打包内容",
            onDismissRequest = { showChoice = false },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            bottomContent = {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PvzGreenButton(
                        text = pickFolderLabel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        onClick = { showChoice = false; requestPick { onPickFolder(fieldKey) } }
                    )
                    PvzBlueButton(
                        text = pickFileLabel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        onClick = { showChoice = false; requestPick { onPickFile(fieldKey, mimeType) } }
                    )
                }
            }
        ) {}
    }

    // 覆盖风险二次确认：该 APK 目标路径已被其他字段复用或已存在于基础资源，继续将覆盖并可能影响其他引用
    if (showOverwriteConfirm) {
        PvzStyledDialog(
            isVisible = showOverwriteConfirm,
            titleText = "覆盖确认",
            onDismissRequest = { showOverwriteConfirm = false; pendingPick = null },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            bottomContent = {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "路径 $confirmPath 已被其他位置使用或已存在于基础资源中，继续将覆盖并可能影响其他引用。是否继续？",
                        fontSize = 13.sp, color = Color(0xFF8D4E00)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PvzRedButton("取消", Modifier
                            .weight(1f)
                            .height(44.dp)) {
                            showOverwriteConfirm = false
                            pendingPick = null
                        }
                        PvzGreenButton("继续", Modifier
                            .weight(1f)
                            .height(44.dp)) {
                            showOverwriteConfirm = false
                            pendingPick?.invoke()
                            pendingPick = null
                        }
                    }
                }
            }
        ) {}
    }
}

// ── 公告设置子页面 ─────────────────────────────────────────────

@Composable
private fun AnnouncementSettingsContent(
    announcements: List<AnnouncementDraft>,
    onUpdate: (List<AnnouncementDraft>) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 左栏：公告列表
        Column(Modifier
            .weight(0.5f)
            .verticalScroll(rememberScrollState())) {
            PvzDialogCard(title = null) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    UiSectionHeader("公告列表 (announcement)")
                    UiDescription("公告将在工具箱启动时以弹窗形式展示给用户。支持多条公告轮播。")

                    announcements.forEachIndexed { i, a ->
                        Spacer(Modifier.height(8.dp))
                        // 每条公告卡片
                        PvzItemCard {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                PvzRichText("公告 #${i + 1}", defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                PvzRedButton("删除", Modifier.height(32.dp)) {
                                    onUpdate(announcements.toMutableList().also { it.removeAt(i) })
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            ReorderButtons(announcements, i, onUpdate, Modifier.fillMaxWidth())
                            Spacer(Modifier.height(6.dp))
                            PvzRichText("标题", defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 12.sp)
                            IntegratorInputField(a.title, "公告标题") { v ->
                                onUpdate(announcements.toMutableList().also { it[i] = it[i].copy(title = v) })
                            }
                            Spacer(Modifier.height(4.dp))
                            PvzRichText("内容", defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 12.sp)
                            IntegratorInputField(a.content, "公告正文（支持 {{red:}} 等复合颜色文本）", multiline = true) { v ->
                                onUpdate(announcements.toMutableList().also { it[i] = it[i].copy(content = v) })
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    PvzGreenButton("＋ 添加公告", Modifier
                        .fillMaxWidth()
                        .height(42.dp)) {
                        onUpdate(announcements + AnnouncementDraft())
                    }
                }
            }
        }

        // 右栏：预览与说明
        Column(Modifier
            .weight(0.5f)
            .verticalScroll(rememberScrollState())) {
            PvzDialogCard(title = null) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    UiSectionHeader("配置说明")
                    UiDescription("对应 dream.yml 路径：announcement")
                    Spacer(Modifier.height(8.dp))
                    UiDescription("• title：公告标题")
                    UiDescription("• content：公告正文，支持复合颜色文本")
                    UiDescription("如 {{red:红色文本}}、{{green:绿色文本}} 等")
                    Spacer(Modifier.height(12.dp))
                    UiDescription("提示：多条公告将以列表形式存储，工具箱启动时按序展示。")
                }
            }
        }
    }
}

// ── 悬浮窗设置子页面 ────────────────────────────────────────────

private val FW_COLORS = listOf("blue", "red", "green", "orange", "purple")

@Composable
private fun FloatingWindowSettingsContent(
    simplifiedLaunch: Boolean,
    showFloatingWindowLabel: String,
    onShowFloatingWindowLabel: (String) -> Unit,
    isShowFloatingWindowDefault: Boolean,
    onIsShowFloatingWindowDefault: (Boolean) -> Unit,
    fwItems: List<FwItemDraft>,
    onFwItems: (List<FwItemDraft>) -> Unit,
    fwEmptyTip: String,
    onFwEmptyTip: (String) -> Unit,
    fwAllHiddenTip: String,
    onFwAllHiddenTip: (String) -> Unit,
    exitConfirmTitle: String,
    onExitConfirmTitle: (String) -> Unit,
    exitConfirmMessage: String,
    onExitConfirmMessage: (String) -> Unit,
    isUseExitConfirm: Boolean,
    onIsUseExitConfirm: (Boolean) -> Unit,
    exitConfirmButtonText: String,
    onExitConfirmButtonText: (String) -> Unit,
    floatingExitConfirmTitle: String,
    onFloatingExitConfirmTitle: (String) -> Unit,
    floatingExitConfirmMessage: String,
    onFloatingExitConfirmMessage: (String) -> Unit,
    floatingExitConfirmButtonText: String,
    onFloatingExitConfirmButtonText: (String) -> Unit,
    onPickFile: (fieldKey: String, mimeType: String) -> Unit,
    selectedFolders: Map<String, File> = emptyMap(),
    onPickFolder: (fieldKey: String) -> Unit = {},
    onClearFieldSelection: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        fun clearFieldSelection(fieldKey: String) = onClearFieldSelection(fieldKey)
        // 左栏：基础设置 + 按钮项列表
        Column(Modifier
            .weight(0.5f)
            .verticalScroll(rememberScrollState())) {
            // 基础设置
            PvzDialogCard(title = null) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    UiSectionHeader("悬浮窗基础设置 (ui.settings)")
                    if (!simplifiedLaunch)
                        UiInputCard("showFloatingWindow", "「开启悬浮窗」开关标签文本。") {
                            IntegratorInputField(showFloatingWindowLabel, "如：是否开启悬浮窗") { onShowFloatingWindowLabel(it) }
                        }
                    UiSwitchCard("isShowFloatingWindow", "默认是否开启悬浮窗（仅影响首次启动）。") {
                        PvzCheckRow("默认开启悬浮窗", isShowFloatingWindowDefault) { onIsShowFloatingWindowDefault(!isShowFloatingWindowDefault) }
                    }
                }
            }

            // 退出确认
            Spacer(Modifier.height(10.dp))
            PvzDialogCard(title = null) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    UiSectionHeader("退出确认 (ui.settings)")
                    UiSwitchCard("isUseExitConfirm", "默认是否启用退出确认（仅影响首次启动）。") {
                        PvzCheckRow("默认启用退出确认", isUseExitConfirm) { onIsUseExitConfirm(!isUseExitConfirm) }
                    }
                    if (!simplifiedLaunch) {
                        UiInputCard("exitConfirmTitle", "退出确认弹窗标题。") {
                            IntegratorInputField(exitConfirmTitle, "如：退出游戏") { onExitConfirmTitle(it) }
                        }
                        UiInputCard("exitConfirmMessage", "退出确认弹窗内容。") {
                            IntegratorInputField(exitConfirmMessage, "如：确定要退出游戏吗？") { onExitConfirmMessage(it) }
                        }
                        UiInputCard("exitConfirmButtonText", "退出确认按钮文字。") {
                            IntegratorInputField(exitConfirmButtonText, "如：确认退出") { onExitConfirmButtonText(it) }
                        }
                    }
                }
            }

            // 悬浮球退出确认
            if (!simplifiedLaunch) {
                Spacer(Modifier.height(10.dp))
                PvzDialogCard(title = null) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        UiSectionHeader("悬浮球退出确认 (ui.settings)")
                        UiInputCard("floatingExitConfirmTitle", "关闭悬浮球时确认弹窗标题。") {
                            IntegratorInputField(floatingExitConfirmTitle, "如：确认退出") {
                                onFloatingExitConfirmTitle(
                                    it
                                )
                            }
                        }
                        UiInputCard("floatingExitConfirmMessage", "关闭悬浮球时确认弹窗内容。") {
                            IntegratorInputField(
                                floatingExitConfirmMessage,
                                "如：确定要退出悬浮窗吗..."
                            ) { onFloatingExitConfirmMessage(it) }
                        }
                        UiInputCard("floatingExitConfirmButtonText", "关闭悬浮球时确认按钮文字。") {
                            IntegratorInputField(
                                floatingExitConfirmButtonText,
                                "如：确认"
                            ) { onFloatingExitConfirmButtonText(it) }
                        }
                    }
                }
            }

            // 占位提示文案
            if (!simplifiedLaunch) {
                Spacer(Modifier.height(10.dp))
                PvzDialogCard(title = null) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        UiSectionHeader("占位提示文案 (ui.floatingWindow)")
                        UiInputCard("emptyTip", "悬浮窗无内容时的占位提示。") {
                            IntegratorInputField(fwEmptyTip, "如：（悬浮窗暂无内容...）") { onFwEmptyTip(it) }
                        }
                        UiInputCard("allHiddenTip", "悬浮窗所有项被隐藏时的提示。") {
                            IntegratorInputField(fwAllHiddenTip, "如：（当前没有可用的功能）") { onFwAllHiddenTip(it) }
                        }
                    }
                }
            }
            // 按钮项列表
            Spacer(Modifier.height(10.dp))
            PvzDialogCard(title = null) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    UiSectionHeader("悬浮窗按钮项 (items)")
                    UiDescription("配置悬浮窗面板内的功能按钮。每条对应一个按钮。")

                    fwItems.forEachIndexed { i, item ->
                        Spacer(Modifier.height(8.dp))
                        FwItemEditor(i, item, onFwItems, fwItems, onPickFile, selectedFolders = selectedFolders, onPickFolder = onPickFolder, onClearFieldSelection = onClearFieldSelection)
                    }

                    Spacer(Modifier.height(10.dp))
                    PvzGreenButton("＋ 添加按钮项", Modifier
                        .fillMaxWidth()
                        .height(42.dp)) {
                        onFwItems(fwItems + FwItemDraft())
                    }
                }
            }
        }

        // 右栏：说明
        Column(Modifier
            .weight(0.5f)
            .verticalScroll(rememberScrollState())) {
            PvzDialogCard(title = null) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    UiSectionHeader("悬浮窗配置说明")
                    UiDescription("对应 dream.yml 路径：")
                    UiDescription("• ui.settings.showFloatingWindow / isShowFloatingWindow")
                    UiDescription("• ui.floatingWindow.emptyTip / allHiddenTip")
                    UiDescription("• ui.floatingWindow.items[]")
                    Spacer(Modifier.height(8.dp))
                    UiDescription("按钮项字段说明：")
                    UiDescription("• id：唯一标识（必填）")
                    UiDescription("• name / buttonText：按钮文字")
                    UiDescription("• buttonColor：blue|red|green|orange|purple")
                    UiDescription("• jsScript / jsPath：点击执行的脚本")
                    UiDescription("• isShowFromJs / isShowFromJsPath：可见性判定")
                    Spacer(Modifier.height(8.dp))
                    UiDescription("常用 JS API：")
                    UiDescription("• vpn.isPrepared() / vpn.isActive()")
                    UiDescription("• ui.isCustomGameDisplayEnabled()")
                    UiDescription("• ui.showGameDisplay()")
                }
            }
        }
    }
}

@Composable
private fun FwItemEditor(
    index: Int,
    item: FwItemDraft,
    onFwItems: (List<FwItemDraft>) -> Unit,
    fwItems: List<FwItemDraft>,
    onPickFile: (fieldKey: String, mimeType: String) -> Unit,
    selectedFolders: Map<String, File> = emptyMap(),
    onPickFolder: (fieldKey: String) -> Unit = {},
    onClearFieldSelection: (String) -> Unit = {}
) {
    fun clearFieldSelection(fieldKey: String) = onClearFieldSelection(fieldKey)
    val colorOptions = FW_COLORS

    PvzItemCard {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            PvzRichText("按钮项 #${index + 1}", defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            PvzRedButton("删除", Modifier.height(32.dp)) {
                onFwItems(fwItems.toMutableList().also { it.removeAt(index) })
            }
        }
        Spacer(Modifier.height(4.dp))
        ReorderButtons(fwItems, index, onFwItems, Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))

        fun update(fw: (FwItemDraft) -> FwItemDraft) {
            onFwItems(fwItems.toMutableList().also { it[index] = fw(it[index]) })
        }

        UiInputCard("id", "唯一标识（必填）") {
            IntegratorInputField(item.id, "如 vpn_toggle") { v -> update { it.copy(id = v) } }
        }
        UiInputCard("name", "按钮文字（优先级低于 buttonText）") {
            IntegratorInputField(item.name, "如 VPN 开关") { v -> update { it.copy(name = v) } }
        }
        UiInputCard("buttonText", "按钮文字（优先级高于 name）") {
            IntegratorInputField(item.buttonText, "如 断开VPN") { v -> update { it.copy(buttonText = v) } }
        }
        UiInputCard("icon", "左侧图标资源名（可选，相对于 assets/pvz2tool/images/）") {
            IntegratorInputField(item.icon, "如 icons/fw_vpn.png") { v -> update { it.copy(icon = v) } }
        }
        UiInputCard("desc", "按钮下方的描述文字（可选）") {
            IntegratorInputField(item.desc, "如 VPN 开关说明", multiline = true) { v -> update { it.copy(desc = v) } }
        }
        // buttonColor 选择
        Column(Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)) {
            PvzRichText("buttonColor", defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp)) {
                colorOptions.forEach { c ->
                    val sel = item.buttonColor == c
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (sel) PvzGreenSurface else Color.Transparent)
                            .border(1.dp, if (sel) PvzGreen else Color(0xFFD5CFA0), RoundedCornerShape(6.dp))
                            .clickable { update { it.copy(buttonColor = c) } }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(c, fontSize = 11.sp, color = if (sel) Color(0xFF33691E) else PvzBorderBrown)
                    }
                }
            }
        }
        UiInputCard("jsScript", "点击执行的 JS 脚本（裸表达式）") {
            IntegratorInputField(item.jsScript, "如 vpn.disconnect()", multiline = true) { v -> update { it.copy(jsScript = v) } }
        }
        UiInputCard("jsPath", "脚本文件路径（jsScript 为空时生效）") {
            FileInputRow(item.jsPath, "如 script/fw_vpn.js", "*/*", "fw_item_${item.id}_jsPath", { v -> update { it.copy(jsPath = v) } }, onPickFile, selectedFolder = selectedFolders["fw_item_${item.id}_jsPath"], onPickFolder = onPickFolder, onClearSelection = { clearFieldSelection("fw_item_${item.id}_jsPath") })
        }
        UiInputCard("smfList", "关联的 SMF 资源列表（逗号分隔）") {
            IntegratorInputField(item.smfList.joinToString(", "), "如 activityconfig, dailyreward") { v ->
                update { it.copy(smfList = v.split(",", "，").map { s -> s.trim() }.filter { s -> s.isNotEmpty() }) }
            }
        }
        UiInputCard("isShowFromJs", "可见性判定 JS 表达式") {
            IntegratorInputField(item.isShowFromJs, "如 vpn.isPrepared()", multiline = true) { v -> update { it.copy(isShowFromJs = v) } }
        }
        UiInputCard("isShowFromJsPath", "可见性判定脚本路径") {
            FileInputRow(item.isShowFromJsPath, "如 script/fw_show.js", "*/*", "fw_item_${item.id}_isShowFromJsPath", { v -> update { it.copy(isShowFromJsPath = v) } }, onPickFile, selectedFolder = selectedFolders["fw_item_${item.id}_isShowFromJsPath"], onPickFolder = onPickFolder, onClearSelection = { clearFieldSelection("fw_item_${item.id}_isShowFromJsPath") })
        }
    }
}

// ── 顶栏图标设置子页面 ──────────────────────────────────────────

@Composable
private fun TopBarIconSettingsContent(
    tbiItems: List<TbiItemDraft>,
    onUpdate: (List<TbiItemDraft>) -> Unit,
    onPickFile: (fieldKey: String, mimeType: String) -> Unit,
    selectedFiles: Map<String, File> = emptyMap(),
    onImagePreview: (Any) -> Unit = {},
    selectedFolders: Map<String, File> = emptyMap(),
    onPickFolder: (fieldKey: String) -> Unit = {},
    onClearFieldSelection: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        fun clearFieldSelection(fieldKey: String) = onClearFieldSelection(fieldKey)
        // 左栏：图标项列表
        Column(Modifier
            .weight(0.5f)
            .verticalScroll(rememberScrollState())) {
            PvzDialogCard(title = null) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    UiSectionHeader("顶栏图标列表 (ui.topBarIcons.items)")
                    UiDescription("配置顶栏设置图标左侧的可点击图标组，按数组顺序从左到右排列。")

                    tbiItems.forEachIndexed { i, item ->
                        Spacer(Modifier.height(8.dp))
                        TbiItemEditor(i, item, onUpdate, tbiItems, onPickFile, selectedFiles = selectedFiles, onImagePreview = onImagePreview, selectedFolders = selectedFolders, onPickFolder = onPickFolder, onClearFieldSelection = onClearFieldSelection)
                    }

                    Spacer(Modifier.height(10.dp))
                    PvzGreenButton("＋ 添加图标项", Modifier
                        .fillMaxWidth()
                        .height(42.dp)) {
                        onUpdate(tbiItems + TbiItemDraft())
                    }
                }
            }
        }

        // 右栏：说明
        Column(Modifier
            .weight(0.5f)
            .verticalScroll(rememberScrollState())) {
            PvzDialogCard(title = null) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    UiSectionHeader("顶栏图标配置说明")
                    UiDescription("对应 dream.yml 路径：ui.topBarIcons.items[]")
                    Spacer(Modifier.height(8.dp))
                    UiDescription("字段说明：")
                    UiDescription("• id：唯一标识（必填）")
                    UiDescription("• icon：正常态图标资源路径")
                    UiDescription("• iconPress：按下态图标（可选，回退 icon）")
                    UiDescription("• contentDescription：无障碍描述（可选）")
                    UiDescription("• jsScript / jsPath：点击执行的脚本")
                    UiDescription("• isShowFromJs / isShowFromJsPath：可见性判定")
                    UiDescription("• pressSound / releaseSound：自定义音效（可选）")
                    Spacer(Modifier.height(8.dp))
                    UiDescription("图标资源支持：相对工作目录 / 绝对路径 / URL / APK Assets")
                }
            }
        }
    }
}

@Composable
private fun TbiItemEditor(
    index: Int,
    item: TbiItemDraft,
    onUpdate: (List<TbiItemDraft>) -> Unit,
    tbiItems: List<TbiItemDraft>,
    onPickFile: (fieldKey: String, mimeType: String) -> Unit,
    selectedFiles: Map<String, File> = emptyMap(),
    onImagePreview: (Any) -> Unit = {},
    selectedFolders: Map<String, File> = emptyMap(),
    onPickFolder: (fieldKey: String) -> Unit = {},
    onClearFieldSelection: (String) -> Unit = {}
) {
    fun clearFieldSelection(fieldKey: String) = onClearFieldSelection(fieldKey)
    PvzItemCard {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            PvzRichText("图标项 #${index + 1}", defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            PvzRedButton("删除", Modifier.height(32.dp)) {
                onUpdate(tbiItems.toMutableList().also { it.removeAt(index) })
            }
        }
        Spacer(Modifier.height(4.dp))
        ReorderButtons(tbiItems, index, onUpdate, Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))

        fun update(fw: (TbiItemDraft) -> TbiItemDraft) {
            onUpdate(tbiItems.toMutableList().also { it[index] = fw(it[index]) })
        }

        UiInputCard("id", "唯一标识（必填）") {
            IntegratorInputField(item.id, "如 refresh_top") { v -> update { it.copy(id = v) } }
        }
        UiInputCard("icon", "正常态图标资源路径") {
            FileInputRow(item.icon, "如 icons/refresh.png", "*/*", "tbi_item_${item.id}_icon", { v -> update { it.copy(icon = v) } }, selectedFile = selectedFiles["tbi_item_${item.id}_icon"], onImagePreview = onImagePreview, onPickFile = onPickFile, selectedFolder = selectedFolders["tbi_item_${item.id}_icon"], onPickFolder = onPickFolder, onClearSelection = { clearFieldSelection("tbi_item_${item.id}_icon") })
        }
        UiInputCard("iconPress", "按下态图标（可选，回退到 icon）") {
            FileInputRow(item.iconPress, "如 icons/refresh_press.png", "*/*", "tbi_item_${item.id}_iconPress", { v -> update { it.copy(iconPress = v) } }, selectedFile = selectedFiles["tbi_item_${item.id}_iconPress"], onImagePreview = onImagePreview, onPickFile = onPickFile, selectedFolder = selectedFolders["tbi_item_${item.id}_iconPress"], onPickFolder = onPickFolder, onClearSelection = { clearFieldSelection("tbi_item_${item.id}_iconPress") })
        }
        UiInputCard("contentDescription", "无障碍描述（可选）") {
            IntegratorInputField(item.contentDescription, "如 刷新配置") { v -> update { it.copy(contentDescription = v) } }
        }
        UiInputCard("jsScript", "点击执行的 JS 脚本（裸表达式）") {
            IntegratorInputField(item.jsScript, "如 native.reloadConfig()", multiline = true) { v -> update { it.copy(jsScript = v) } }
        }
        UiInputCard("jsPath", "脚本文件路径（jsScript 为空时生效）") {
            FileInputRow(item.jsPath, "如 script/topbar_refresh.js", "*/*", "tbi_item_${item.id}_jsPath", { v -> update { it.copy(jsPath = v) } }, onPickFile, selectedFolder = selectedFolders["tbi_item_${item.id}_jsPath"], onPickFolder = onPickFolder, onClearSelection = { clearFieldSelection("tbi_item_${item.id}_jsPath") })
        }
        UiInputCard("isShowFromJs", "可见性判定 JS 表达式") {
            IntegratorInputField(item.isShowFromJs, "如 !!prepareVpn", multiline = true) { v -> update { it.copy(isShowFromJs = v) } }
        }
        UiInputCard("isShowFromJsPath", "可见性判定脚本路径") {
            FileInputRow(item.isShowFromJsPath, "如 script/topbar_show.js", "*/*", "tbi_item_${item.id}_isShowFromJsPath", { v -> update { it.copy(isShowFromJsPath = v) } }, onPickFile, selectedFolder = selectedFolders["tbi_item_${item.id}_isShowFromJsPath"], onPickFolder = onPickFolder, onClearSelection = { clearFieldSelection("tbi_item_${item.id}_isShowFromJsPath") })
        }
        UiInputCard("pressSound", "按下音效文件名（可选）") {
            FileInputRow(item.pressSound, "如 ui_click_press.wav", "*/*", "tbi_item_${item.id}_pressSound", { v -> update { it.copy(pressSound = v) } }, onPickFile, selectedFolder = selectedFolders["tbi_item_${item.id}_pressSound"], onPickFolder = onPickFolder, onClearSelection = { clearFieldSelection("tbi_item_${item.id}_pressSound") })
        }
        UiInputCard("releaseSound", "释放音效文件名（可选）") {
            FileInputRow(item.releaseSound, "如 ui_click_release.wav", "*/*", "tbi_item_${item.id}_releaseSound", { v -> update { it.copy(releaseSound = v) } }, onPickFile, selectedFolder = selectedFolders["tbi_item_${item.id}_releaseSound"], onPickFolder = onPickFolder, onClearSelection = { clearFieldSelection("tbi_item_${item.id}_releaseSound") })
        }
        UiInputCard("smfList", "关联的 SMF 资源列表（逗号分隔）") {
            IntegratorInputField(item.smfList.joinToString(", "), "如 activityconfig, dailyreward") { v ->
                update { it.copy(smfList = v.split(",", "，").map { s -> s.trim() }.filter { s -> s.isNotEmpty() }) }
            }
        }
    }
}

// ── 版本设置子页面 ─────────────────────────────────────────

@Composable
private fun VersionSettingsContent(
    versions: List<VersionDraft>,
    onUpdate: (List<VersionDraft>) -> Unit,
    isExpandedVersions: Boolean,
    onIsExpandedVersions: (Boolean) -> Unit,
    versionsTheme: String,
    onVersionsTheme: (String) -> Unit,
    onPickFile: (label: String, mimeType: String) -> Unit,
    selectedFiles: Map<String, File> = emptyMap(),
    onImagePreview: (Any) -> Unit = {},
    selectedFolders: Map<String, File> = emptyMap(),
    onPickFolder: (fieldKey: String) -> Unit = {},
    onClearFieldSelection: (String) -> Unit = {}
) {
    Row(Modifier
        .fillMaxSize()
        .padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        fun clearFieldSelection(fieldKey: String) = onClearFieldSelection(fieldKey)
        // 左栏：版本列表
        Column(Modifier
            .weight(0.6f)
            .verticalScroll(rememberScrollState())) {
            PvzDialogCard(title = null) {
                Column(Modifier.padding(12.dp)) {
                    PvzSectionTitle("版本列表")
                    Text("管理游戏版本（正式服/怀旧服等）", fontSize = 12.sp, color = Color(0xFF5D4E37))
                    Spacer(Modifier.height(8.dp))

                    versions.forEachIndexed { i, v ->
                        PvzInfoCard("版本 #${i + 1}") {
                            IntegratorInputField(v.id, "版本ID（必填）") { v2 ->
                                onUpdate(versions.toMutableList().also { it[i] = it[i].copy(id = v2) })
                            }
                            Spacer(Modifier.height(4.dp))
                            IntegratorInputField(v.name, "版本名称") { v2 ->
                                onUpdate(versions.toMutableList().also { it[i] = it[i].copy(name = v2) })
                            }
                            Spacer(Modifier.height(4.dp))
                            IntegratorInputField(v.desc, "版本描述", multiline = true) { v2 ->
                                onUpdate(versions.toMutableList().also { it[i] = it[i].copy(desc = v2) })
                            }
                            Spacer(Modifier.height(4.dp))
                            FileInputRow(v.icon, "图标路径", "*/*", "ver_${i}_icon", { v2 ->
                                onUpdate(versions.toMutableList().also { it[i] = it[i].copy(icon = v2) })
                            }, selectedFile = selectedFiles["ver_${i}_icon"], onImagePreview = onImagePreview, onPickFile = onPickFile, selectedFolder = selectedFolders["ver_${i}_icon"], onPickFolder = onPickFolder, onClearSelection = { clearFieldSelection("ver_${i}_icon") })
                            Spacer(Modifier.height(4.dp))
                            PvzCheckRow("默认版本", v.default) {
                                onUpdate(versions.mapIndexed { j, w -> w.copy(default = j == i) })
                            }
                            Spacer(Modifier.height(4.dp))
                            IntegratorInputField(v.assetPath, "资源路径（留空=默认 version/<id>/smf）") { v2 ->
                                onUpdate(versions.toMutableList().also { it[i] = it[i].copy(assetPath = v2) })
                            }
                            Spacer(Modifier.height(4.dp))
                            IntegratorInputField(v.baseAssetPath, "基础包路径（留空=全局）") { v2 ->
                                onUpdate(versions.toMutableList().also { it[i] = it[i].copy(baseAssetPath = v2) })
                            }
                            Spacer(Modifier.height(4.dp))
                            PvzCheckRow("强制覆盖 (forceOverride)", v.forceOverride) {
                                onUpdate(versions.toMutableList().also { it[i] = it[i].copy(forceOverride = !v.forceOverride) })
                            }
                            Spacer(Modifier.height(4.dp))
                            IntegratorInputField(v.enterGameScript, "进入游戏 JS脚本（可选）", multiline = true) { v2 ->
                                onUpdate(versions.toMutableList().also { it[i] = it[i].copy(enterGameScript = v2) })
                            }
                            Spacer(Modifier.height(4.dp))
                            FileInputRow(v.enterGamePath, "进入游戏 JS 文件路径（可选）", "*/*", "ver_${i}_enterGamePath", { v2 ->
                                onUpdate(versions.toMutableList().also { it[i] = it[i].copy(enterGamePath = v2) })
                            }, onPickFile, selectedFolder = selectedFolders["ver_${i}_enterGamePath"], onPickFolder = onPickFolder, onClearSelection = { clearFieldSelection("ver_${i}_enterGamePath") })
                            Spacer(Modifier.height(4.dp))
                            ReorderButtons(versions, i, onUpdate, Modifier.fillMaxWidth())
                            Spacer(Modifier.height(4.dp))
                            PvzRedButton("删除", Modifier
                                .fillMaxWidth()
                                .height(36.dp)) {
                                onUpdate(versions.toMutableList().also { it.removeAt(i) })
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }

                    PvzGreenButton("+ 添加版本", Modifier
                        .fillMaxWidth()
                        .height(BUTTON_HEIGHT)) {
                        onUpdate(versions + VersionDraft(id = "v${versions.size + 1}", name = "版本 ${versions.size + 1}"))
                    }
                }
            }
        }

        // 右栏：说明 + 外观设置
        Column(Modifier
            .weight(0.4f)
            .verticalScroll(rememberScrollState())) {
            PvzDialogCard(title = null) {
                Column(Modifier.padding(12.dp)) {
                    PvzSectionTitle("版本说明")
                    Text("版本列表中的第一个版本为默认版本。用户可在版本管理面板中切换不同版本。", fontSize = 12.sp, color = Color(0xFF5D4E37))
                    Text("每个版本可拥有独立的资源包（SMF）、进入脚本等。", fontSize = 12.sp, color = Color(0xFF5D4E37))
                    Spacer(Modifier.height(12.dp))

                    PvzSectionTitle("外观设置")
                    PvzCheckRow("默认展开版本面板 (isExpandedVersions)", isExpandedVersions) { onIsExpandedVersions(!isExpandedVersions) }
                    Spacer(Modifier.height(6.dp))
                    Text("版本面板主题色 (versionsTheme):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D4E37))
                    listOf("BROWN", "BLUE", "GREEN", "RED", "PURPLE", "ORANGE", "TEAL", "GOLD", "GRAY").forEach { theme ->
                        PvzChoiceRow(theme, versionsTheme == theme) { onVersionsTheme(theme) }
                    }
                }
            }
        }
    }
}

// ── 栏目设置子页面 ─────────────────────────────────────────

@Composable
private fun SectionSettingsContent(
    sections: List<SectionDraft>,
    onUpdate: (List<SectionDraft>) -> Unit,
    onEditItems: (Int) -> Unit,
    onPickFile: (label: String, mimeType: String) -> Unit,
    selectedFolders: Map<String, File> = emptyMap(),
    onPickFolder: (fieldKey: String) -> Unit = {},
    onClearFieldSelection: (String) -> Unit = {}
) {
    Row(Modifier
        .fillMaxSize()
        .padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        fun clearFieldSelection(fieldKey: String) = onClearFieldSelection(fieldKey)
        // 左栏：栏目列表
        Column(Modifier
            .weight(0.6f)
            .verticalScroll(rememberScrollState())) {
            PvzDialogCard(title = null) {
                Column(Modifier.padding(12.dp)) {
                    PvzSectionTitle("栏目列表")
                    Text("管理主界面的动态栏目", fontSize = 12.sp, color = Color(0xFF5D4E37))
                    Spacer(Modifier.height(8.dp))

                    sections.forEachIndexed { i, s ->
                        PvzInfoCard("栏目 · ${s.title.ifBlank { "(未命名)" }}") {
                            IntegratorInputField(s.id, "栏目ID（必填）") { v ->
                                onUpdate(sections.toMutableList().also { it[i] = it[i].copy(id = v) })
                            }
                            Spacer(Modifier.height(4.dp))
                            IntegratorInputField(s.title, "栏目标题") { v ->
                                onUpdate(sections.toMutableList().also { it[i] = it[i].copy(title = v) })
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("栏目面板主题色 (theme):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D4E37))
                            listOf("BROWN", "BLUE", "GREEN", "RED", "PURPLE", "ORANGE", "TEAL", "GOLD", "GRAY").forEach { theme ->
                                PvzChoiceRow(theme, s.theme == theme) {
                                    onUpdate(sections.toMutableList().also { it[i] = it[i].copy(theme = theme) })
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            PvzCheckRow("默认展开 (isExpanded)", s.isExpanded) {
                                onUpdate(sections.toMutableList().also { it[i] = it[i].copy(isExpanded = !s.isExpanded) })
                            }
                            Spacer(Modifier.height(4.dp))
                            PvzCheckRow("允许添加功能项 (addItems)", s.addItems) {
                                onUpdate(sections.toMutableList().also { it[i] = it[i].copy(addItems = !s.addItems) })
                            }
                            Spacer(Modifier.height(4.dp))
                            IntegratorInputField(s.confirmButtonText, "确认按钮文字（可选）") { v ->
                                onUpdate(sections.toMutableList().also { it[i] = it[i].copy(confirmButtonText = v) })
                            }
                            Spacer(Modifier.height(4.dp))
                            IntegratorInputField(s.visibleOnVersionIds, "版本可见性（逗号分隔，空=全部）") { v ->
                                onUpdate(sections.toMutableList().also { it[i] = it[i].copy(visibleOnVersionIds = v) })
                            }
                            Spacer(Modifier.height(4.dp))
                            IntegratorInputField(s.targetPath, "资源解压路径（可选）") { v ->
                                onUpdate(sections.toMutableList().also { it[i] = it[i].copy(targetPath = v) })
                            }
                            Spacer(Modifier.height(4.dp))
                            IntegratorInputField(s.descriptionContent, "描述内容 (descriptionContent)", multiline = true) { v ->
                                onUpdate(sections.toMutableList().also { it[i] = it[i].copy(descriptionContent = v) })
                            }
                            Spacer(Modifier.height(4.dp))
                            IntegratorInputField(s.jsScript, "栏目级 JS 脚本", multiline = true) { v ->
                                onUpdate(sections.toMutableList().also { it[i] = it[i].copy(jsScript = v) })
                            }
                            Spacer(Modifier.height(4.dp))
                            FileInputRow(s.jsPath, "栏目级 JS 文件路径", "*/*", "section_${i}_jsPath", { v ->
                                onUpdate(sections.toMutableList().also { it[i] = it[i].copy(jsPath = v) })
                            }, onPickFile, selectedFolder = selectedFolders["section_${i}_jsPath"], onPickFolder = onPickFolder, onClearSelection = { clearFieldSelection("section_${i}_jsPath") })
                            Spacer(Modifier.height(6.dp))
                            ReorderButtons(sections, i, onUpdate, Modifier.fillMaxWidth())
                            Spacer(Modifier.height(6.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PvzGreenButton("编辑功能项 →", Modifier
                                    .weight(1f)
                                    .height(36.dp)) { onEditItems(i) }
                                PvzRedButton("删除", Modifier
                                    .weight(1f)
                                    .height(36.dp)) {
                                    onUpdate(sections.toMutableList().also { it.removeAt(i) })
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }

                    PvzGreenButton("+ 添加栏目", Modifier
                        .fillMaxWidth()
                        .height(BUTTON_HEIGHT)) {
                        onUpdate(sections + SectionDraft(id = "section_${sections.size + 1}", isExpanded = true))
                    }
                }
            }
        }

        // 右栏：说明
        Column(Modifier
            .weight(0.4f)
            .verticalScroll(rememberScrollState())) {
            PvzDialogCard(title = null) {
                Column(Modifier.padding(12.dp)) {
                    PvzSectionTitle("栏目说明")
                    Text("栏目是主界面中的折叠面板，每个栏目包含若干功能项（RADIO / CHECKBOX / SLIDER / BUTTON / INPUT / INFO / DESCRIPTION）。", fontSize = 12.sp, color = Color(0xFF5D4E37))
                    Text("id 以 example_ 开头的栏目为示例栏目，可通过「保留示例栏目」开关控制是否保留。", fontSize = 12.sp, color = Color(0xFF5D4E37))
                    Spacer(Modifier.height(12.dp))
                    PvzSectionTitle("栏目属性列表")
                    listOf("id (必填)", "title (必填)", "theme (默认 BROWN)", "isExpanded", "confirmButtonText", "visibleOnVersionIds", "targetPath", "addItems", "descriptionContent", "jsScript / jsPath").forEach {
                        Text("  • $it", fontSize = 12.sp, color = Color(0xFF5D4E37))
                    }
                }
            }
        }
    }
}

// ── 功能项设置子页面（栏目下的功能项编辑器）─────────────────

@Composable
private fun ItemSettingsContent(
    sectionIndex: Int,
    section: SectionDraft,
    onUpdate: (SectionDraft) -> Unit,
    onPickFile: (label: String, mimeType: String) -> Unit,
    selectedFiles: Map<String, File> = emptyMap(),
    onImagePreview: (Any) -> Unit = {},
    selectedFolders: Map<String, File> = emptyMap(),
    onPickFolder: (fieldKey: String) -> Unit = {},
    onClearFieldSelection: (String) -> Unit = {}
) {
    fun clearFieldSelection(fieldKey: String) = onClearFieldSelection(fieldKey)
    val items = section.items
    Row(Modifier
        .fillMaxSize()
        .padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // 左栏：功能项列表
        Column(Modifier
            .weight(0.6f)
            .verticalScroll(rememberScrollState())) {
            PvzDialogCard(title = null) {
                Column(Modifier.padding(12.dp)) {
                    PvzSectionTitle("「${section.title.ifBlank { section.id }}」的功能项")
                    Text("管理此栏目下的所有功能项", fontSize = 12.sp, color = Color(0xFF5D4E37))
                    Spacer(Modifier.height(8.dp))

                    items.forEachIndexed { i, item ->
                        val itemLabel = item.name.ifBlank { item.id.ifBlank { "#${i + 1}" } }
                        PvzInfoCard("[$item.type] $itemLabel") {
                            ReorderButtons(items, i, onUpdate = { newItems -> onUpdate(section.copy(items = newItems)) }, Modifier.fillMaxWidth())
                            Spacer(Modifier.height(6.dp))
                            IntegratorInputField(item.id, "ID（必填）") { v ->
                                onUpdate(section.copy(items = items.toMutableList().also { it[i] = it[i].copy(id = v) }))
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("类型 (type):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D4E37))
                            val types = listOf("DESCRIPTION", "BUTTON", "CHECKBOX", "RADIO", "SLIDER", "INPUT", "INFO")
                            types.forEach { t ->
                                PvzChoiceRow(t, item.type == t) {
                                    onUpdate(section.copy(items = items.toMutableList().also { it[i] = it[i].copy(type = t) }))
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            // 通用字段
                            IntegratorInputField(item.name, "name（支持 {{red:}} 复合文本）") { v ->
                                onUpdate(section.copy(items = items.toMutableList().also { it[i] = it[i].copy(name = v) }))
                            }
                            Spacer(Modifier.height(4.dp))
                            IntegratorInputField(item.desc, "desc", multiline = true) { v ->
                                onUpdate(section.copy(items = items.toMutableList().also { it[i] = it[i].copy(desc = v) }))
                            }
                            Spacer(Modifier.height(4.dp))
                            FileInputRow(item.icon, "icon 路径（可选）", "*/*", "secitem_${sectionIndex}_${i}_icon", { v ->
                                onUpdate(section.copy(items = items.toMutableList().also { it[i] = it[i].copy(icon = v) }))
                            }, selectedFile = selectedFiles["secitem_${sectionIndex}_${i}_icon"], onImagePreview = onImagePreview, onPickFile = onPickFile, selectedFolder = selectedFolders["secitem_${sectionIndex}_${i}_icon"], onPickFolder = onPickFolder, onClearSelection = { clearFieldSelection("secitem_${sectionIndex}_${i}_icon") })
                            Spacer(Modifier.height(4.dp))
                            IntegratorInputField(item.assetPath, "assetPath（可选）") { v ->
                                onUpdate(section.copy(items = items.toMutableList().also { it[i] = it[i].copy(assetPath = v) }))
                            }
                            Spacer(Modifier.height(4.dp))
                            IntegratorInputField(item.jsScript, "jsScript（可选）", multiline = true) { v ->
                                onUpdate(section.copy(items = items.toMutableList().also { it[i] = it[i].copy(jsScript = v) }))
                            }
                            Spacer(Modifier.height(4.dp))
                            FileInputRow(item.jsPath, "jsPath（可选）", "*/*", "secitem_${sectionIndex}_${i}_jsPath", { v ->
                                onUpdate(section.copy(items = items.toMutableList().also { it[i] = it[i].copy(jsPath = v) }))
                            }, onPickFile, selectedFolder = selectedFolders["secitem_${sectionIndex}_${i}_jsPath"], onPickFolder = onPickFolder, onClearSelection = { clearFieldSelection("secitem_${sectionIndex}_${i}_jsPath") })
                            Spacer(Modifier.height(4.dp))
                            IntegratorInputField(item.isShowFromJs, "isShowFromJs（可选）", multiline = true) { v ->
                                onUpdate(section.copy(items = items.toMutableList().also { it[i] = it[i].copy(isShowFromJs = v) }))
                            }
                            Spacer(Modifier.height(4.dp))
                            FileInputRow(item.isShowFromJsPath, "isShowFromJsPath（可选）", "*/*", "secitem_${sectionIndex}_${i}_isShowFromJsPath", { v ->
                                onUpdate(section.copy(items = items.toMutableList().also { it[i] = it[i].copy(isShowFromJsPath = v) }))
                            }, onPickFile, selectedFolder = selectedFolders["secitem_${sectionIndex}_${i}_isShowFromJsPath"], onPickFolder = onPickFolder, onClearSelection = { clearFieldSelection("secitem_${sectionIndex}_${i}_isShowFromJsPath") })

                            // 类型特定字段
                            when (item.type) {
                                "RADIO" -> {
                                    Spacer(Modifier.height(4.dp))
                                    IntegratorInputField(item.groupId, "groupId（互斥组）") { v ->
                                        onUpdate(section.copy(items = items.toMutableList().also { it[i] = it[i].copy(groupId = v) }))
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    PvzCheckRow("默认选中", item.radioDefault) {
                                        onUpdate(section.copy(items = items.mapIndexed { j, w ->
                                            w.copy(radioDefault = if (w.groupId == item.groupId) j == i else w.radioDefault)
                                        }))
                                    }
                                }
                                "CHECKBOX" -> {
                                    Spacer(Modifier.height(4.dp))
                                    PvzCheckRow("默认勾选", item.checkboxDefault) {
                                        onUpdate(section.copy(items = items.toMutableList().also { it[i] = it[i].copy(checkboxDefault = !item.checkboxDefault) }))
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    IntegratorInputField(item.smfList, "smfList（逗号分隔）") { v ->
                                        onUpdate(section.copy(items = items.toMutableList().also { it[i] = it[i].copy(smfList = v) }))
                                    }
                                }
                                "SLIDER" -> {
                                    Spacer(Modifier.height(4.dp))
                                    IntegratorInputField(item.minValue, "minValue") { v ->
                                        onUpdate(section.copy(items = items.toMutableList().also { it[i] = it[i].copy(minValue = v) }))
                                    }
                                    IntegratorInputField(item.maxValue, "maxValue") { v ->
                                        onUpdate(section.copy(items = items.toMutableList().also { it[i] = it[i].copy(maxValue = v) }))
                                    }
                                    IntegratorInputField(item.defaultValue, "defaultValue") { v ->
                                        onUpdate(section.copy(items = items.toMutableList().also { it[i] = it[i].copy(defaultValue = v) }))
                                    }
                                    IntegratorInputField(item.step, "step") { v ->
                                        onUpdate(section.copy(items = items.toMutableList().also { it[i] = it[i].copy(step = v) }))
                                    }
                                    IntegratorInputField(item.valueSuffix, "valueSuffix") { v ->
                                        onUpdate(section.copy(items = items.toMutableList().also { it[i] = it[i].copy(valueSuffix = v) }))
                                    }
                                }
                                "BUTTON" -> {
                                    Spacer(Modifier.height(4.dp))
                                    IntegratorInputField(item.buttonText, "buttonText（按钮文字）") { v ->
                                        onUpdate(section.copy(items = items.toMutableList().also { it[i] = it[i].copy(buttonText = v) }))
                                    }
                                    Text("buttonColor:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D4E37))
                                    listOf("blue", "red", "green", "orange", "purple").forEach { c ->
                                        PvzChoiceRow(c, item.buttonColor == c) {
                                            onUpdate(section.copy(items = items.toMutableList().also { it[i] = it[i].copy(buttonColor = c) }))
                                        }
                                    }
                                }
                                "INPUT" -> {
                                    Spacer(Modifier.height(4.dp))
                                    IntegratorInputField(item.placeholder, "placeholder") { v ->
                                        onUpdate(section.copy(items = items.toMutableList().also { it[i] = it[i].copy(placeholder = v) }))
                                    }
                                    IntegratorInputField(item.inputDefault, "inputDefault（初始值）") { v ->
                                        onUpdate(section.copy(items = items.toMutableList().also { it[i] = it[i].copy(inputDefault = v) }))
                                    }
                                }
                                "INFO" -> {
                                    Spacer(Modifier.height(4.dp))
                                    IntegratorInputField(item.infoValue, "infoValue（静态默认值）", multiline = true) { v ->
                                        onUpdate(section.copy(items = items.toMutableList().also { it[i] = it[i].copy(infoValue = v) }))
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            PvzRedButton("删除", Modifier
                                .fillMaxWidth()
                                .height(36.dp)) {
                                onUpdate(section.copy(items = items.toMutableList().also { it.removeAt(i) }))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }

                    PvzGreenButton("+ 添加功能项", Modifier
                        .fillMaxWidth()
                        .height(BUTTON_HEIGHT)) {
                        onUpdate(section.copy(items = items + SectionItemDraft(id = "item_${items.size + 1}", type = "DESCRIPTION")))
                    }
                }
            }
        }

        // 右栏：类型说明
        Column(Modifier
            .weight(0.4f)
            .verticalScroll(rememberScrollState())) {
            PvzDialogCard(title = null) {
                Column(Modifier.padding(12.dp)) {
                    PvzSectionTitle("功能项类型")
                    Text("DESCRIPTION - 纯文本描述（name 留空）或带标题描述", fontSize = 12.sp, color = Color(0xFF5D4E37))
                    Text("BUTTON - 按钮，左文右钮或全宽横条", fontSize = 12.sp, color = Color(0xFF5D4E37))
                    Text("CHECKBOX - 勾选框", fontSize = 12.sp, color = Color(0xFF5D4E37))
                    Text("RADIO - 单选项（同 groupId 为一组）", fontSize = 12.sp, color = Color(0xFF5D4E37))
                    Text("SLIDER - 滑块（min/max/step/valueSuffix）", fontSize = 12.sp, color = Color(0xFF5D4E37))
                    Text("INPUT - 文本输入框", fontSize = 12.sp, color = Color(0xFF5D4E37))
                    Text("INFO - 信息展示（infoValue + jsScript 动态值）", fontSize = 12.sp, color = Color(0xFF5D4E37))
                    Spacer(Modifier.height(12.dp))
                    Text("name 和 desc 字段支持 {{red:}} {{green:}} 等复合颜色文本标记。", fontSize = 11.sp, color = PvzBorderBrown)
                }
            }
        }
    }
}
@Composable
private fun UiAdvancedSettingsContent(
    simplifiedLaunch: Boolean,
    onPickFile: (String, String) -> Unit,
    selectedFolders: Map<String, File> = emptyMap(),
    onPickFolder: (fieldKey: String) -> Unit = {},
    onClearFieldSelection: (String) -> Unit = {},
    uiExDialogTitle: String, uiExInitLoadTip: String, uiExInitProgTip: String, uiExNoNeedTip: String,
    uiExSingleFileTip: String, uiExMultiFileTip: String, uiExWaitingTip: String, uiExCompleteTip: String,
    uiExFailPrefix: String, uiExSkipPrefix: String, uiExContinueBtn: String, uiExCompleteBtn: String, uiExToastErr: String,
    uiSndSwitchPress: String, uiSndSwitchRelease: String, uiSndBtnPress: String, uiSndBtnRelease: String,
    uiSndSettingsPress: String, uiSndSettingsRelease: String, uiSndXClosePress: String, uiSndXCloseRelease: String,
    uiSndPanelPress: String, uiSndPanelRelease: String,
    uiBtnEnterGameIcon: Boolean, uiBtnTutorialIcon: Boolean, uiBtnResetDataIcon: Boolean,
    uiSetTitle: String, uiSetSolidBg: String, uiSetPlayMusic: String, uiSetImportSmf: String, uiSetReload: String,
    uiSetResetSmf: String, uiSetCustomDisplay: String, uiSetDisplayTitle: String, uiSetApplyBtn: String,
    uiErrJsTitle: String, uiErrUnknown: String,
    uiLogCopyDesc: String, uiLogClearDesc: String, uiLogNoLogText: String, uiLogPresetSaveLabel: String, uiLogLocalSaveLabel: String,
    uiDlgDelSave: String, uiDlgEditUser: String, uiDlgShareTitle: String, uiDlgPackFail: String, uiDlgNoShare: String,
    uiWelcomeEditTitle: String, uiWelcomeEditHint: String,
    saveDraft: SaveDraft, gameDisplay: Pvz2ToolConfigGameDisplay,
    uiSetChangeProfile: String, uiSetShowNotUpdate: String, uiSetExitConfirm: String, uiSndSwitchClick: String,
    onUiExDialogTitle: (String) -> Unit, onUiExInitLoadTip: (String) -> Unit, onUiExInitProgTip: (String) -> Unit,
    onUiExNoNeedTip: (String) -> Unit, onUiExSingleFileTip: (String) -> Unit, onUiExMultiFileTip: (String) -> Unit,
    onUiExWaitingTip: (String) -> Unit, onUiExCompleteTip: (String) -> Unit, onUiExFailPrefix: (String) -> Unit,
    onUiExSkipPrefix: (String) -> Unit, onUiExContinueBtn: (String) -> Unit, onUiExCompleteBtn: (String) -> Unit, onUiExToastErr: (String) -> Unit,
    onUiSndSwitchPress: (String) -> Unit, onUiSndSwitchRelease: (String) -> Unit, onUiSndBtnPress: (String) -> Unit, onUiSndBtnRelease: (String) -> Unit,
    onUiSndSettingsPress: (String) -> Unit, onUiSndSettingsRelease: (String) -> Unit, onUiSndXClosePress: (String) -> Unit, onUiSndXCloseRelease: (String) -> Unit,
    onUiSndPanelPress: (String) -> Unit, onUiSndPanelRelease: (String) -> Unit,
    onUiBtnEnterGameIcon: (Boolean) -> Unit, onUiBtnTutorialIcon: (Boolean) -> Unit, onUiBtnResetDataIcon: (Boolean) -> Unit,
    onUiSetTitle: (String) -> Unit, onUiSetSolidBg: (String) -> Unit, onUiSetPlayMusic: (String) -> Unit,
    onUiSetImportSmf: (String) -> Unit, onUiSetReload: (String) -> Unit, onUiSetResetSmf: (String) -> Unit,
    onUiSetCustomDisplay: (String) -> Unit, onUiSetDisplayTitle: (String) -> Unit, onUiSetApplyBtn: (String) -> Unit,
    onUiErrJsTitle: (String) -> Unit, onUiErrUnknown: (String) -> Unit,
    onUiLogCopyDesc: (String) -> Unit, onUiLogClearDesc: (String) -> Unit, onUiLogNoLogText: (String) -> Unit,
    onUiLogPresetSaveLabel: (String) -> Unit, onUiLogLocalSaveLabel: (String) -> Unit,
    onUiDlgDelSave: (String) -> Unit, onUiDlgEditUser: (String) -> Unit, onUiDlgShareTitle: (String) -> Unit,
    onUiDlgPackFail: (String) -> Unit, onUiDlgNoShare: (String) -> Unit,
    onUiWelcomeEditTitle: (String) -> Unit, onUiWelcomeEditHint: (String) -> Unit,
    onSaveDraft: (SaveDraft) -> Unit, onGameDisplay: (Pvz2ToolConfigGameDisplay) -> Unit,
    onUiSetChangeProfile: (String) -> Unit, onUiSetShowNotUpdate: (String) -> Unit, onUiSetExitConfirm: (String) -> Unit,     onUiSndSwitchClick: (String) -> Unit
) {
    fun clearFieldSelection(fieldKey: String) = onClearFieldSelection(fieldKey)
    Row(Modifier
        .fillMaxSize()
        .padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier
            .weight(0.5f)
            .verticalScroll(rememberScrollState())) {
        if (!simplifiedLaunch) {
        PvzDialogCard(title = null) {
            Column(Modifier.padding(12.dp)) {
                PvzSectionTitle("资源更新弹窗文本 (ui.extractor)")
                UiInputCard("ui.extractor.dialogTitle", "弹窗标题") { IntegratorInputField(uiExDialogTitle, "弹窗标题") { onUiExDialogTitle(it) } }
                UiInputCard("ui.extractor.initialLoadingProgressTip", "初始加载提示") { IntegratorInputField(uiExInitLoadTip, "清点物资") { onUiExInitLoadTip(it) } }
                UiInputCard("ui.extractor.initialProgressTip", "初始进度提示") { IntegratorInputField(uiExInitProgTip, "检测到更新") { onUiExInitProgTip(it) } }
                UiInputCard("ui.extractor.noNeedExtractTip", "无需更新提示") { IntegratorInputField(uiExNoNeedTip, "暂无更新") { onUiExNoNeedTip(it) } }
                UiInputCard("ui.extractor.singleFileProcessingTip", "单文件处理提示") { IntegratorInputField(uiExSingleFileTip, "整理物资") { onUiExSingleFileTip(it) } }
                UiInputCard("ui.extractor.multiFileProcessingTip", "多文件处理提示(%d=数量)") { IntegratorInputField(uiExMultiFileTip, "整理%d个物资") { onUiExMultiFileTip(it) } }
                UiInputCard("ui.extractor.waitingTip", "等待提示") { IntegratorInputField(uiExWaitingTip, "稍等片刻") { onUiExWaitingTip(it) } }
                UiInputCard("ui.extractor.extractCompleteTip", "完成提示(%s=版本)") { IntegratorInputField(uiExCompleteTip, "物资更新完毕") { onUiExCompleteTip(it) } }
                UiInputCard("ui.extractor.extractFailTipPrefix", "失败提示前缀") { IntegratorInputField(uiExFailPrefix, "出问题了") { onUiExFailPrefix(it) } }
                UiInputCard("ui.extractor.fileSkipTipPrefix", "跳过提示前缀") { IntegratorInputField(uiExSkipPrefix, "无需更新") { onUiExSkipPrefix(it) } }
                UiInputCard("ui.extractor.continueButtonText", "继续按钮") { IntegratorInputField(uiExContinueBtn, "继续") { onUiExContinueBtn(it) } }
                UiInputCard("ui.extractor.completeButtonText", "完成按钮") { IntegratorInputField(uiExCompleteBtn, "重返战场") { onUiExCompleteBtn(it) } }
                UiInputCard("ui.extractor.toastErrorPrefix", "Toast错误前缀") { IntegratorInputField(uiExToastErr, "更新失败") { onUiExToastErr(it) } }
            }
        }

        Spacer(Modifier.height(10.dp))
        PvzDialogCard(title = null) {
            Column(Modifier.padding(12.dp)) {
                PvzSectionTitle("音效文件 (ui.sounds)")
                UiInputCard("ui.sounds.switchClickPress", "开关按下音效") { FileInputRow(uiSndSwitchPress, "*.wav", "audio/*", "uiSndSwitchPress", { onUiSndSwitchPress(it) }, onPickFile, selectedFolder = selectedFolders["uiSndSwitchPress"], onPickFolder = onPickFolder, onClearSelection = { clearFieldSelection("uiSndSwitchPress") }) }
                UiInputCard("ui.sounds.switchClickRelease", "开关释放音效") { FileInputRow(uiSndSwitchRelease, "*.wav", "audio/*", "uiSndSwitchRelease", { onUiSndSwitchRelease(it) }, onPickFile, selectedFolder = selectedFolders["uiSndSwitchRelease"], onPickFolder = onPickFolder, onClearSelection = { clearFieldSelection("uiSndSwitchRelease") }) }
                UiInputCard("ui.sounds.buttonClickPress", "按钮按下音效") { FileInputRow(uiSndBtnPress, "*.wav", "audio/*", "uiSndBtnPress", { onUiSndBtnPress(it) }, onPickFile, selectedFolder = selectedFolders["uiSndBtnPress"], onPickFolder = onPickFolder, onClearSelection = { clearFieldSelection("uiSndBtnPress") }) }
                UiInputCard("ui.sounds.buttonClickRelease", "按钮释放音效") { FileInputRow(uiSndBtnRelease, "*.wav", "audio/*", "uiSndBtnRelease", { onUiSndBtnRelease(it) }, onPickFile, selectedFolder = selectedFolders["uiSndBtnRelease"], onPickFolder = onPickFolder, onClearSelection = { clearFieldSelection("uiSndBtnRelease") }) }
                UiInputCard("ui.sounds.buttonSettingsPress", "设置按钮按下音效") { FileInputRow(uiSndSettingsPress, "*.wav", "audio/*", "uiSndSettingsPress", { onUiSndSettingsPress(it) }, onPickFile, selectedFolder = selectedFolders["uiSndSettingsPress"], onPickFolder = onPickFolder, onClearSelection = { clearFieldSelection("uiSndSettingsPress") }) }
                UiInputCard("ui.sounds.buttonSettingsRelease", "设置按钮释放音效") { FileInputRow(uiSndSettingsRelease, "*.wav", "audio/*", "uiSndSettingsRelease", { onUiSndSettingsRelease(it) }, onPickFile, selectedFolder = selectedFolders["uiSndSettingsRelease"], onPickFolder = onPickFolder, onClearSelection = { clearFieldSelection("uiSndSettingsRelease") }) }
                UiInputCard("ui.sounds.buttonXClosePress", "关闭按钮按下音效") { FileInputRow(uiSndXClosePress, "*.wav", "audio/*", "uiSndXClosePress", { onUiSndXClosePress(it) }, onPickFile, selectedFolder = selectedFolders["uiSndXClosePress"], onPickFolder = onPickFolder, onClearSelection = { clearFieldSelection("uiSndXClosePress") }) }
                UiInputCard("ui.sounds.buttonXCloseRelease", "关闭按钮释放音效") { FileInputRow(uiSndXCloseRelease, "*.wav", "audio/*", "uiSndXCloseRelease", { onUiSndXCloseRelease(it) }, onPickFile, selectedFolder = selectedFolders["uiSndXCloseRelease"], onPickFolder = onPickFolder, onClearSelection = { clearFieldSelection("uiSndXCloseRelease") }) }
                UiInputCard("ui.sounds.collapsiblePanelPress", "折叠面板按下音效") { FileInputRow(uiSndPanelPress, "*.wav", "audio/*", "uiSndPanelPress", { onUiSndPanelPress(it) }, onPickFile, selectedFolder = selectedFolders["uiSndPanelPress"], onPickFolder = onPickFolder, onClearSelection = { clearFieldSelection("uiSndPanelPress") }) }
                UiInputCard("ui.sounds.collapsiblePanelRelease", "折叠面板释放音效") { FileInputRow(uiSndPanelRelease, "*.wav", "audio/*", "uiSndPanelRelease", { onUiSndPanelRelease(it) }, onPickFile, selectedFolder = selectedFolders["uiSndPanelRelease"], onPickFolder = onPickFolder, onClearSelection = { clearFieldSelection("uiSndPanelRelease") }) }
            }
        }

        Spacer(Modifier.height(10.dp))
        PvzDialogCard(title = null) {
            Column(Modifier.padding(12.dp)) {
                PvzSectionTitle("按钮图标开关 (ui.button)")
                UiSwitchCard("ui.button.isEnterGameDefaultIcon", "进入游戏使用默认图标") { PvzCheckRow("使用默认图标", uiBtnEnterGameIcon) { onUiBtnEnterGameIcon(!uiBtnEnterGameIcon) } }
                UiSwitchCard("ui.button.isTutorialDefaultIcon", "教程使用默认图标") { PvzCheckRow("使用默认图标", uiBtnTutorialIcon) { onUiBtnTutorialIcon(!uiBtnTutorialIcon) } }
                UiSwitchCard("ui.button.isResetDataDefaultIcon", "重置数据使用默认图标") { PvzCheckRow("使用默认图标", uiBtnResetDataIcon) { onUiBtnResetDataIcon(!uiBtnResetDataIcon) } }
            }
        }

        Spacer(Modifier.height(10.dp))
        PvzDialogCard(title = null) {
            Column(Modifier.padding(12.dp)) {
                PvzSectionTitle("设置扩展 (ui.settings)")
                UiInputCard("ui.settings.title", "设置弹窗标题") { IntegratorInputField(uiSetTitle, "设置") { onUiSetTitle(it) } }
                UiInputCard("ui.settings.solidBackgroundMode", "纯色背景模式标签") { IntegratorInputField(uiSetSolidBg, "纯色背景") { onUiSetSolidBg(it) } }
                UiInputCard("ui.settings.playBackgroundMusic", "播放背景音乐标签") { IntegratorInputField(uiSetPlayMusic, "播放背景音乐") { onUiSetPlayMusic(it) } }
                UiInputCard("ui.settings.importSmfFile", "导入SMF文件标签") { IntegratorInputField(uiSetImportSmf, "导入SMF") { onUiSetImportSmf(it) } }
                UiInputCard("ui.settings.reloadConfig", "重新读取配置标签") { IntegratorInputField(uiSetReload, "重新读取") { onUiSetReload(it) } }
                UiInputCard("ui.settings.resetPacketDeepClearing", "重置时删除SMF标签") { IntegratorInputField(uiSetResetSmf, "删除SMF") { onUiSetResetSmf(it) } }
                UiInputCard("ui.settings.customGameDisplay", "自定义游戏画面标签") { IntegratorInputField(uiSetCustomDisplay, "自定义画面") { onUiSetCustomDisplay(it) } }
                UiInputCard("ui.settings.customGameDisplayTitle", "游戏画面设置标题") { IntegratorInputField(uiSetDisplayTitle, "画面设置") { onUiSetDisplayTitle(it) } }
                UiInputCard("ui.settings.applyButtonText", "应用按钮文字") { IntegratorInputField(uiSetApplyBtn, "应用") { onUiSetApplyBtn(it) } }
            }
        }

        Spacer(Modifier.height(10.dp))
        PvzDialogCard(title = null) {
            Column(Modifier.padding(12.dp)) {
                PvzSectionTitle("错误/日志/对话框/欢迎 (ui.error/log/dialog/welcome)")
                UiInputCard("ui.error.jsExecuteErrorTitle", "JS错误标题") { IntegratorInputField(uiErrJsTitle, "JS执行出错") { onUiErrJsTitle(it) } }
                UiInputCard("ui.error.unknownError", "未知错误") { IntegratorInputField(uiErrUnknown, "未知错误") { onUiErrUnknown(it) } }
                UiInputCard("ui.log.copyLogDesc", "复制日志描述") { IntegratorInputField(uiLogCopyDesc, "复制日志") { onUiLogCopyDesc(it) } }
                UiInputCard("ui.log.clearLogDesc", "清空日志描述") { IntegratorInputField(uiLogClearDesc, "清空日志") { onUiLogClearDesc(it) } }
                UiInputCard("ui.log.noLogText", "无日志文本") { IntegratorInputField(uiLogNoLogText, "暂无日志") { onUiLogNoLogText(it) } }
                UiInputCard("ui.log.presetSaveLabel", "预设存档标签") { IntegratorInputField(uiLogPresetSaveLabel, "预设存档") { onUiLogPresetSaveLabel(it) } }
                UiInputCard("ui.log.localSaveLabel", "本地存档标签") { IntegratorInputField(uiLogLocalSaveLabel, "本地存档") { onUiLogLocalSaveLabel(it) } }
                UiInputCard("ui.dialog.deleteSaveDesc", "删除存档描述") { IntegratorInputField(uiDlgDelSave, "删除存档") { onUiDlgDelSave(it) } }
                UiInputCard("ui.dialog.editUserNameDesc", "编辑用户名描述") { IntegratorInputField(uiDlgEditUser, "编辑用户名") { onUiDlgEditUser(it) } }
                UiInputCard("ui.dialog.shareSaveChooserTitle", "分享存档选择器标题") { IntegratorInputField(uiDlgShareTitle, "分享存档") { onUiDlgShareTitle(it) } }
                UiInputCard("ui.dialog.sharePackFailedTip", "打包失败提示") { IntegratorInputField(uiDlgPackFail, "打包失败") { onUiDlgPackFail(it) } }
                UiInputCard("ui.dialog.noShareableSaveTip", "无可分享存档提示") { IntegratorInputField(uiDlgNoShare, "没有可分享的存档") { onUiDlgNoShare(it) } }
                UiInputCard("ui.welcome.editUserNameTitle", "修改用户名标题") { IntegratorInputField(uiWelcomeEditTitle, "修改用户名") { onUiWelcomeEditTitle(it) } }
                UiInputCard("ui.welcome.editUserNameHint", "修改用户名提示") { IntegratorInputField(uiWelcomeEditHint, "请输入新的用户名") { onUiWelcomeEditHint(it) } }
            }
        }

        Spacer(Modifier.height(10.dp))
        PvzDialogCard(title = null) {
            Column(Modifier.padding(12.dp)) {
                PvzSectionTitle("设置补充 (ui.settings / ui.sounds)")
                UiInputCard("ui.settings.changeTheProfileReadLocation", "切换存档读取位置标签") { IntegratorInputField(uiSetChangeProfile, "切换存档位置") { onUiSetChangeProfile(it) } }
                UiInputCard("ui.settings.showNotUpdate", "未检测更新也弹窗标签") { IntegratorInputField(uiSetShowNotUpdate, "未更新也弹窗") { onUiSetShowNotUpdate(it) } }
                UiInputCard("ui.settings.exitConfirm", "退出游戏二次确认标签") { IntegratorInputField(uiSetExitConfirm, "退出确认") { onUiSetExitConfirm(it) } }
                UiInputCard("ui.sounds.switchClick", "单击音效(无按下/释放区分)") { FileInputRow(uiSndSwitchClick, "*.wav", "audio/*", "uiSndSwitchClick", { onUiSndSwitchClick(it) }, onPickFile) }
            }
        }

        }
        }
        Column(Modifier
            .weight(0.5f)
            .verticalScroll(rememberScrollState())) {
        if (!simplifiedLaunch) {
        Spacer(Modifier.height(10.dp))
        PvzDialogCard(title = null) {
            Column(Modifier.padding(12.dp)) {
                PvzSectionTitle("存档管理 (ui.save)")
                UiInputCard("ui.save.presetConfirmTitle", "预设覆盖确认标题") { IntegratorInputField(saveDraft.presetConfirmTitle, "预设覆盖确认标题") { onSaveDraft(saveDraft.copy(presetConfirmTitle = it)) } }
                UiInputCard("ui.save.presetConfirmMessage", "预设覆盖确认内容") { IntegratorInputField(saveDraft.presetConfirmMessage, "预设覆盖确认内容") { onSaveDraft(saveDraft.copy(presetConfirmMessage = it)) } }
                UiInputCard("ui.save.deleteConfirmTitle", "删除确认标题") { IntegratorInputField(saveDraft.deleteConfirmTitle, "删除确认标题") { onSaveDraft(saveDraft.copy(deleteConfirmTitle = it)) } }
                UiInputCard("ui.save.deleteConfirmMessage", "删除确认内容") { IntegratorInputField(saveDraft.deleteConfirmMessage, "删除确认内容") { onSaveDraft(saveDraft.copy(deleteConfirmMessage = it)) } }
                UiInputCard("ui.save.coverConfirmTitle", "覆盖确认标题") { IntegratorInputField(saveDraft.coverConfirmTitle, "覆盖确认标题") { onSaveDraft(saveDraft.copy(coverConfirmTitle = it)) } }
                UiInputCard("ui.save.coverConfirmMessage", "覆盖确认内容") { IntegratorInputField(saveDraft.coverConfirmMessage, "覆盖确认内容") { onSaveDraft(saveDraft.copy(coverConfirmMessage = it)) } }
                UiInputCard("ui.save.deleteGameSaveConfirmTitle", "删除游玩存档确认标题") { IntegratorInputField(saveDraft.deleteGameSaveConfirmTitle, "删除游玩存档确认标题") { onSaveDraft(saveDraft.copy(deleteGameSaveConfirmTitle = it)) } }
                UiInputCard("ui.save.deleteGameSaveConfirmMessage", "删除游玩存档确认内容") { IntegratorInputField(saveDraft.deleteGameSaveConfirmMessage, "删除游玩存档确认内容") { onSaveDraft(saveDraft.copy(deleteGameSaveConfirmMessage = it)) } }
                UiInputCard("ui.save.saveInfoTitle", "存档信息标题") { IntegratorInputField(saveDraft.saveInfoTitle, "存档信息标题") { onSaveDraft(saveDraft.copy(saveInfoTitle = it)) } }
                UiInputCard("ui.save.saveNameLabel", "存档名称标签") { IntegratorInputField(saveDraft.saveNameLabel, "存档名称标签") { onSaveDraft(saveDraft.copy(saveNameLabel = it)) } }
                UiInputCard("ui.save.saveDescLabel", "存档描述标签") { IntegratorInputField(saveDraft.saveDescLabel, "存档描述标签") { onSaveDraft(saveDraft.copy(saveDescLabel = it)) } }
                UiInputCard("ui.save.cancelButton", "取消按钮") { IntegratorInputField(saveDraft.cancelButton, "取消按钮") { onSaveDraft(saveDraft.copy(cancelButton = it)) } }
                UiInputCard("ui.save.confirmButton", "确认按钮") { IntegratorInputField(saveDraft.confirmButton, "确认按钮") { onSaveDraft(saveDraft.copy(confirmButton = it)) } }
                UiInputCard("ui.save.shareButton", "分享按钮") { IntegratorInputField(saveDraft.shareButton, "分享按钮") { onSaveDraft(saveDraft.copy(shareButton = it)) } }
                UiInputCard("ui.save.exportButton", "导出按钮") { IntegratorInputField(saveDraft.exportButton, "导出按钮") { onSaveDraft(saveDraft.copy(exportButton = it)) } }
                UiInputCard("ui.save.importButton", "导入按钮") { IntegratorInputField(saveDraft.importButton, "导入按钮") { onSaveDraft(saveDraft.copy(importButton = it)) } }
                UiInputCard("ui.save.backupButton", "备份按钮") { IntegratorInputField(saveDraft.backupButton, "备份按钮") { onSaveDraft(saveDraft.copy(backupButton = it)) } }
                UiInputCard("ui.save.coverLocalButton", "覆盖本地按钮") { IntegratorInputField(saveDraft.coverLocalButton, "覆盖本地按钮") { onSaveDraft(saveDraft.copy(coverLocalButton = it)) } }
                UiInputCard("ui.save.deleteGameSaveButton", "删除游玩存档按钮") { IntegratorInputField(saveDraft.deleteGameSaveButton, "删除游玩存档按钮") { onSaveDraft(saveDraft.copy(deleteGameSaveButton = it)) } }
                UiInputCard("ui.save.coverPresetButton", "覆盖预设按钮") { IntegratorInputField(saveDraft.coverPresetButton, "覆盖预设按钮") { onSaveDraft(saveDraft.copy(coverPresetButton = it)) } }
                UiInputCard("ui.save.saveNameEmptyTip", "存档名为空提示") { IntegratorInputField(saveDraft.saveNameEmptyTip, "存档名为空提示") { onSaveDraft(saveDraft.copy(saveNameEmptyTip = it)) } }
                UiInputCard("ui.save.noLocalSaveTip", "无本地存档提示") { IntegratorInputField(saveDraft.noLocalSaveTip, "无本地存档提示") { onSaveDraft(saveDraft.copy(noLocalSaveTip = it)) } }
                UiInputCard("ui.save.selectLocalSaveTip", "选择本地存档提示") { IntegratorInputField(saveDraft.selectLocalSaveTip, "选择本地存档提示") { onSaveDraft(saveDraft.copy(selectLocalSaveTip = it)) } }
                UiInputCard("ui.save.backupSuccessTip", "备份成功提示") { IntegratorInputField(saveDraft.backupSuccessTip, "备份成功提示") { onSaveDraft(saveDraft.copy(backupSuccessTip = it)) } }
                UiInputCard("ui.save.backupFailTipPrefix", "备份失败前缀") { IntegratorInputField(saveDraft.backupFailTipPrefix, "备份失败前缀") { onSaveDraft(saveDraft.copy(backupFailTipPrefix = it)) } }
                UiInputCard("ui.save.exportSuccessTip", "导出成功提示") { IntegratorInputField(saveDraft.exportSuccessTip, "导出成功提示") { onSaveDraft(saveDraft.copy(exportSuccessTip = it)) } }
                UiInputCard("ui.save.exportFailTipPrefix", "导出失败前缀") { IntegratorInputField(saveDraft.exportFailTipPrefix, "导出失败前缀") { onSaveDraft(saveDraft.copy(exportFailTipPrefix = it)) } }
                UiInputCard("ui.save.importSuccessTip", "导入成功提示") { IntegratorInputField(saveDraft.importSuccessTip, "导入成功提示") { onSaveDraft(saveDraft.copy(importSuccessTip = it)) } }
                UiInputCard("ui.save.importFailTipPrefix", "导入失败前缀") { IntegratorInputField(saveDraft.importFailTipPrefix, "导入失败前缀") { onSaveDraft(saveDraft.copy(importFailTipPrefix = it)) } }
                UiInputCard("ui.save.deleteSuccessTip", "删除成功提示") { IntegratorInputField(saveDraft.deleteSuccessTip, "删除成功提示") { onSaveDraft(saveDraft.copy(deleteSuccessTip = it)) } }
                UiInputCard("ui.save.deleteFailTipPrefix", "删除失败前缀") { IntegratorInputField(saveDraft.deleteFailTipPrefix, "删除失败前缀") { onSaveDraft(saveDraft.copy(deleteFailTipPrefix = it)) } }
                UiInputCard("ui.save.coverSuccessTip", "覆盖成功提示") { IntegratorInputField(saveDraft.coverSuccessTip, "覆盖成功提示") { onSaveDraft(saveDraft.copy(coverSuccessTip = it)) } }
                UiInputCard("ui.save.coverFailTipPrefix", "覆盖失败前缀") { IntegratorInputField(saveDraft.coverFailTipPrefix, "覆盖失败前缀") { onSaveDraft(saveDraft.copy(coverFailTipPrefix = it)) } }
                UiInputCard("ui.save.deleteGameSaveSuccessTip", "删除游玩存档成功提示") { IntegratorInputField(saveDraft.deleteGameSaveSuccessTip, "删除游玩存档成功提示") { onSaveDraft(saveDraft.copy(deleteGameSaveSuccessTip = it)) } }
                UiInputCard("ui.save.deleteGameSaveFailTipPrefix", "删除游玩存档失败前缀") { IntegratorInputField(saveDraft.deleteGameSaveFailTipPrefix, "删除游玩存档失败前缀") { onSaveDraft(saveDraft.copy(deleteGameSaveFailTipPrefix = it)) } }
                UiInputCard("ui.save.defaultImportNamePrefix", "默认导入名前缀") { IntegratorInputField(saveDraft.defaultImportNamePrefix, "默认导入名前缀") { onSaveDraft(saveDraft.copy(defaultImportNamePrefix = it)) } }
                UiInputCard("ui.save.defaultBackupDesc", "默认备份描述") { IntegratorInputField(saveDraft.defaultBackupDesc, "默认备份描述") { onSaveDraft(saveDraft.copy(defaultBackupDesc = it)) } }
                UiInputCard("ui.save.defaultImportDesc", "默认导入描述") { IntegratorInputField(saveDraft.defaultImportDesc, "默认导入描述") { onSaveDraft(saveDraft.copy(defaultImportDesc = it)) } }
                UiInputCard("ui.save.exportOptionTitle", "导出选项标题") { IntegratorInputField(saveDraft.exportOptionTitle, "导出选项标题") { onSaveDraft(saveDraft.copy(exportOptionTitle = it)) } }
                UiInputCard("ui.save.exportToFolderOption", "导出到文件夹选项") { IntegratorInputField(saveDraft.exportToFolderOption, "导出到文件夹选项") { onSaveDraft(saveDraft.copy(exportToFolderOption = it)) } }
                UiInputCard("ui.save.shareAsPackageOption", "分享为存档包选项") { IntegratorInputField(saveDraft.shareAsPackageOption, "分享为存档包选项") { onSaveDraft(saveDraft.copy(shareAsPackageOption = it)) } }
                UiInputCard("ui.save.gameSaveLabel", "游玩存档区域标题") { IntegratorInputField(saveDraft.gameSaveLabel, "游玩存档区域标题") { onSaveDraft(saveDraft.copy(gameSaveLabel = it)) } }
                UiInputCard("ui.save.gameSaveInfoTemplate", "游玩存档信息模板(%s用户/%t时间)") { IntegratorInputField(saveDraft.gameSaveInfoTemplate, "游玩存档信息模板(%s用户/%t时间)") { onSaveDraft(saveDraft.copy(gameSaveInfoTemplate = it)) } }
                UiInputCard("ui.save.gameSaveUnknownUser", "游玩存档未知用户") { IntegratorInputField(saveDraft.gameSaveUnknownUser, "游玩存档未知用户") { onSaveDraft(saveDraft.copy(gameSaveUnknownUser = it)) } }
                UiInputCard("ui.save.gameSaveNotExistTip", "游玩存档不存在提示") { IntegratorInputField(saveDraft.gameSaveNotExistTip, "游玩存档不存在提示") { onSaveDraft(saveDraft.copy(gameSaveNotExistTip = it)) } }
                UiInputCard("ui.save.retryButtonText", "重试按钮") { IntegratorInputField(saveDraft.retryButtonText, "重试按钮") { onSaveDraft(saveDraft.copy(retryButtonText = it)) } }
                UiInputCard("ui.save.opBackup", "操作-备份") { IntegratorInputField(saveDraft.opBackup, "操作-备份") { onSaveDraft(saveDraft.copy(opBackup = it)) } }
                UiInputCard("ui.save.opExport", "操作-导出") { IntegratorInputField(saveDraft.opExport, "操作-导出") { onSaveDraft(saveDraft.copy(opExport = it)) } }
                UiInputCard("ui.save.opImport", "操作-导入") { IntegratorInputField(saveDraft.opImport, "操作-导入") { onSaveDraft(saveDraft.copy(opImport = it)) } }
                UiInputCard("ui.save.opDelete", "操作-删除") { IntegratorInputField(saveDraft.opDelete, "操作-删除") { onSaveDraft(saveDraft.copy(opDelete = it)) } }
                UiInputCard("ui.save.opDeleteGameSave", "操作-删除游玩存档") { IntegratorInputField(saveDraft.opDeleteGameSave, "操作-删除游玩存档") { onSaveDraft(saveDraft.copy(opDeleteGameSave = it)) } }
                UiInputCard("ui.save.opCover", "操作-覆盖") { IntegratorInputField(saveDraft.opCover, "操作-覆盖") { onSaveDraft(saveDraft.copy(opCover = it)) } }
                UiInputCard("ui.save.opSaveMeta", "操作-存档元数据") { IntegratorInputField(saveDraft.opSaveMeta, "操作-存档元数据") { onSaveDraft(saveDraft.copy(opSaveMeta = it)) } }
            }
        }
        }

        Spacer(Modifier.height(10.dp))
        PvzDialogCard(title = null) {
            Column(Modifier.padding(12.dp)) {
                PvzSectionTitle("游戏画面设置 (ui.settings.gameDisplay)")
                UiSwitchCard("ui.settings.gameDisplay.isUseCustomGameDisplay", "默认启用自定义画面总开关") { PvzCheckRow("默认启用自定义画面总开关", gameDisplay.isUseCustomGameDisplay) { onGameDisplay(gameDisplay.copy(isUseCustomGameDisplay = !gameDisplay.isUseCustomGameDisplay)) } }
                if (!simplifiedLaunch) { UiInputCard("ui.settings.gameDisplay.allowRotation", "允许翻转界面标签") { IntegratorInputField(gameDisplay.allowRotation, "允许翻转界面标签") { onGameDisplay(gameDisplay.copy(allowRotation = it)) } } }
                UiSwitchCard("ui.settings.gameDisplay.isAllowRotation", "默认允许翻转") { PvzCheckRow("默认允许翻转", gameDisplay.isAllowRotation) { onGameDisplay(gameDisplay.copy(isAllowRotation = !gameDisplay.isAllowRotation)) } }
                if (!simplifiedLaunch) { UiInputCard("ui.settings.gameDisplay.customWindowSize", "自定义窗口尺寸标签") { IntegratorInputField(gameDisplay.customWindowSize, "自定义窗口尺寸标签") { onGameDisplay(gameDisplay.copy(customWindowSize = it)) } } }
                if (!simplifiedLaunch) { UiInputCard("ui.settings.gameDisplay.customWindowRatio", "自定义窗口比例标签") { IntegratorInputField(gameDisplay.customWindowRatio, "自定义窗口比例标签") { onGameDisplay(gameDisplay.copy(customWindowRatio = it)) } } }
                if (!simplifiedLaunch) { UiInputCard("ui.settings.gameDisplay.fullscreen", "全屏标签") { IntegratorInputField(gameDisplay.fullscreen, "全屏标签") { onGameDisplay(gameDisplay.copy(fullscreen = it)) } } }
                UiInputCard("ui.settings.gameDisplay.displayMode", "显示模式(ratio/size/fullscreen)") { IntegratorInputField(gameDisplay.displayMode, "显示模式(ratio/size/fullscreen)") { onGameDisplay(gameDisplay.copy(displayMode = it)) } }
                UiInputCard("ui.settings.gameDisplay.windowWidth", "窗口宽度(px,0=屏幕宽)") { IntegratorInputField(gameDisplay.windowWidth.toString(), "窗口宽度(px,0=屏幕宽)") { onGameDisplay(gameDisplay.copy(windowWidth = it.toIntOrNull() ?: 0)) } }
                UiInputCard("ui.settings.gameDisplay.windowHeight", "窗口高度(px,0=屏幕高)") { IntegratorInputField(gameDisplay.windowHeight.toString(), "窗口高度(px,0=屏幕高)") { onGameDisplay(gameDisplay.copy(windowHeight = it.toIntOrNull() ?: 0)) } }
                UiInputCard("ui.settings.gameDisplay.windowRatio", "窗口比例(宽/高)") { IntegratorInputField(gameDisplay.windowRatio.toString(), "窗口比例(宽/高)") { onGameDisplay(gameDisplay.copy(windowRatio = it.toFloatOrNull() ?: 1.5f)) } }
                if (!simplifiedLaunch) { UiInputCard("ui.settings.gameDisplay.ratioHint", "比例输入提示") { IntegratorInputField(gameDisplay.ratioHint, "比例输入提示") { onGameDisplay(gameDisplay.copy(ratioHint = it)) } } }
                if (!simplifiedLaunch) { UiInputCard("ui.settings.gameDisplay.widthHint", "宽度输入提示") { IntegratorInputField(gameDisplay.widthHint, "宽度输入提示") { onGameDisplay(gameDisplay.copy(widthHint = it)) } } }
                if (!simplifiedLaunch) { UiInputCard("ui.settings.gameDisplay.heightHint", "高度输入提示") { IntegratorInputField(gameDisplay.heightHint, "高度输入提示") { onGameDisplay(gameDisplay.copy(heightHint = it)) } } }
            }
        }

        Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ScheduleSettingsContent(
    items: List<ScheduleDraft>,
    onUpdate: (List<ScheduleDraft>) -> Unit,
    onBack: () -> Unit
) {
    val cronPresets = listOf(
        "每天 10:00" to "0 10 * * *",
        "每天 18:00" to "0 18 * * *",
        "每天 08:00" to "0 8 * * *",
        "每 30 分钟" to "every 30m",
        "每 1 小时" to "every 1h",
        "每 2 小时" to "every 2h",
        "每 6 小时" to "every 6h",
        "每天一次" to "every 1d",
    )

    Row(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 左栏：定时任务列表
        Column(Modifier
            .weight(0.55f)
            .verticalScroll(rememberScrollState())) {
            PvzDialogCard(title = null) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    UiSectionHeader("定时任务列表 (schedules)")
                    UiDescription("配置后台定时执行的 JS 脚本，底层基于 AlarmManager。脚本末尾必须调用 timer.nextTrigger() 续期。")

                    items.forEachIndexed { i, item ->
                        Spacer(Modifier.height(8.dp))
                        PvzItemCard {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                PvzRichText(item.name.ifBlank { item.id }.ifBlank { "定时任务 #${i + 1}" },
                                    defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    PvzGreenButton(if (item.enabled) "启用" else "禁用", Modifier.height(30.dp)) {
                                        onUpdate(items.toMutableList().also { it[i] = it[i].copy(enabled = !it[i].enabled) })
                                    }
                                    PvzRedButton("删除", Modifier.height(30.dp)) {
                                        onUpdate(items.toMutableList().also { it.removeAt(i) })
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            ReorderButtons(items, i, onUpdate, Modifier.fillMaxWidth())
                            Spacer(Modifier.height(6.dp))
                            fun update(fn: (ScheduleDraft) -> ScheduleDraft) {
                                onUpdate(items.toMutableList().also { it[i] = fn(it[i]) })
                            }
                            UiInputCard("id", "唯一标识（必填）") {
                                IntegratorInputField(item.id, "如 daily_sign") { v -> update { it.copy(id = v) } }
                            }
                            UiInputCard("name", "显示名称") {
                                IntegratorInputField(item.name, "如 每日签到提醒") { v -> update { it.copy(name = v) } }
                            }
                            // cron 预设选择
                            Column(Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)) {
                                PvzRichText("触发规则 (cron)", defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(2.dp))
                                val preset = cronPresets.firstOrNull { it.second == item.cron }
                                val showCustom = item.cron.isNotBlank() && preset == null
                                Row(Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()), Arrangement.spacedBy(4.dp)) {
                                    cronPresets.take(4).forEach { (label, value) ->
                                        val sel = item.cron == value
                                        Box(
                                            Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (sel) PvzGreenSurface else Color.Transparent)
                                                .border(
                                                    1.dp,
                                                    if (sel) PvzGreen else Color(0xFFD5CFA0),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .clickable { update { it.copy(cron = if (sel) "" else value) } }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) { Text(label, fontSize = 11.sp, color = if (sel) Color(0xFF33691E) else PvzBorderBrown) }
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()), Arrangement.spacedBy(4.dp)) {
                                    cronPresets.drop(4).forEach { (label, value) ->
                                        val sel = item.cron == value
                                        Box(
                                            Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (sel) PvzGreenSurface else Color.Transparent)
                                                .border(
                                                    1.dp,
                                                    if (sel) PvzGreen else Color(0xFFD5CFA0),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .clickable { update { it.copy(cron = if (sel) "" else value) } }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) { Text(label, fontSize = 11.sp, color = if (sel) Color(0xFF33691E) else PvzBorderBrown) }
                                    }
                                    // 自定义入口
                                    val customSel = showCustom || item.cron.isBlank()
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (customSel) PvzGreenSurface else Color.Transparent)
                                            .border(
                                                1.dp,
                                                if (customSel) PvzGreen else Color(0xFFD5CFA0),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable { update { it.copy(cron = "") } }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) { Text("自定义", fontSize = 11.sp, color = if (customSel) Color(0xFF33691E) else PvzBorderBrown) }
                                }
                                if (showCustom || item.cron.isBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    IntegratorInputField(item.cron, "如 0 10 * * * 或 every 30m") { v -> update { it.copy(cron = v) } }
                                }
                            }
                            UiInputCard("jsScript", "执行的 JS 脚本（内联代码，优先于 jsPath）") {
                                IntegratorInputField(item.jsScript,
                                    "notifications.show('签到', '记得签到！');\ntimer.nextTrigger();",
                                    multiline = true
                                ) { v -> update { it.copy(jsScript = v) } }
                            }
                            UiInputCard("jsPath", "JS 文件路径（jsScript 为空时生效）") {
                                IntegratorInputField(item.jsPath, "如 js/daily_check.js") { v -> update { it.copy(jsPath = v) } }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    PvzGreenButton("＋ 添加定时任务", Modifier
                        .fillMaxWidth()
                        .height(42.dp)) {
                        onUpdate(items + ScheduleDraft(id = "timer_${items.size + 1}"))
                    }
                }
            }
        }

        // 右栏：说明
        Column(Modifier
            .weight(0.45f)
            .verticalScroll(rememberScrollState())) {
            PvzDialogCard(title = null) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    UiSectionHeader("定时任务说明")
                    UiDescription("对应 dream.yml 路径：schedules[]")
                    Spacer(Modifier.height(8.dp))
                    UiDescription("字段说明：")
                    UiDescription("• id：唯一标识（必填，用于 timer.cancel(id)）")
                    UiDescription("• name：显示名称（timer.list() 中可见）")
                    UiDescription("• cron：触发规则，选择预设或输入自定义 cron 表达式")
                    UiDescription("• jsScript：定时执行的 JS 内联代码（优先于 jsPath）")
                    UiDescription("• jsPath：JS 文件路径（jsScript 为空时生效，如 js/daily_check.js）")
                    UiDescription("• enabled：是否启用")
                    Spacer(Modifier.height(8.dp))
                    PvzRichText("cron 格式：", defaultStyle = PvzTextOliveStyleNoShadow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    UiDescription("\"0 10 * * *\" — 每天 10:00")
                    UiDescription("\"every 30m\" — 每 30 分钟")
                    UiDescription("\"every 2h\" — 每 2 小时")
                    UiDescription("\"every 1d\" — 每天一次")
                    Spacer(Modifier.height(8.dp))
                    Text("重要提醒：", color = Color(0xFFB71C1C), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    UiDescription("脚本末尾必须调用 timer.nextTrigger() 注册下次闹钟，否则只触发一次。后台无法弹 UI 弹窗，请使用 notifications.show() 发送通知。")
                }
            }
        }
    }
}
