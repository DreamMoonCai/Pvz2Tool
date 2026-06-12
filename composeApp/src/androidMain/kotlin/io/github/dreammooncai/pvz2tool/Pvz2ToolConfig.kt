package io.github.dreammooncai.pvz2tool

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import io.github.dreammooncai.pvz2tool.ui.dialog.AssetExtractorHolder
import io.github.dreammooncai.pvz2tool.view.PvzCollapsiblePanelTheme
import kotlinx.serialization.Serializable
import java.io.File

// 完整的配置根类
@Serializable
data class Pvz2ToolConfig(
    val gameActivity: String,
    val smfDirectory: String,
    val sections: List<DynamicSection>,
    val versions: List<VersionDef>,
    val isExpandedVersions: Boolean = false,
    /**
     * 版本管理面板主题颜色。
     * 可选值同 PvzCollapsiblePanelTheme：
     * "BROWN"（默认）| "BLUE" | "BLUE_BACKGROUND" | "GREEN" | "GREEN_BACKGROUND" |
     * "RED" | "PURPLE" | "PURPLE_BACKGROUND" | "ORANGE" | "TEAL" | "TEAL_BACKGROUND" |
     * "GOLD" | "GRAY" | "GRAY_BACKGROUND"
     */
    val versionsTheme: PvzCollapsiblePanelTheme = PvzCollapsiblePanelTheme.BROWN,
    val announcement: List<Pvz2ToolConfigAnnouncement>,
    val ui: Pvz2ToolConfigUI,
    val localConfigFile: String? = null // 兼容旧的文件路径（逐步淘汰）
) {
    companion object {
        const val PATH_NAME = "pvz2tool"

        lateinit var rootDirectory: File
    }

    init {
        if (smfDirectory.contains("obb") && InitializePvz2.context.obbDir?.exists() != true) {
            InitializePvz2.context.obbDir?.mkdirs()
        }
    }

    fun getSmfDirectoryFile() = rootDirectory.resolve(InitializePvz2.config.smfDirectory)

    /**
     * 改造核心：获取本地工作目录（适配Uri/File）
     * @return Pair(DocumentFile?, Context.() -> Unit)：目录DocumentFile + 权限校验函数
     */
    fun getLocalWorkDir(context: Context): DocumentFile? {
        val uri = InitializePvz2.mLocalConfigDirUri
        // 优先级：Uri > File路径
        val uriStr = localConfigFile

        return try {
            // 1. 处理SAF Uri（content://开头）
            if (uri != null) {
                DocumentFile.fromTreeUri(context, uri)
            }
            // 2. 兼容旧的File路径
            else if (uriStr != null) {
                val file = File(uriStr).parentFile ?: File(uriStr)
                DocumentFile.fromFile(file)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

/**
 * 栏目类型
 */
@Serializable
enum class SectionType {
    /** 单选组（类似版本管理） */
    RADIO,
    /** 多选组（类似功能管理） */
    CHECKBOX,
    /** 纯文本说明 */
    DESCRIPTION,
    /** 按钮组（点击执行 JS 脚本） */
    BUTTON,
    /** 滑动条（数值调节） */
    SLIDER,
    /** 文本输入框 */
    INPUT,
    /** 只读信息展示 */
    INFO
}

@Serializable
data class DynamicSection(
    val id: String,
    val title: String,
    val visibleOnVersionIds: Set<String> = emptySet(),
    val targetPath: String? = null,

    val addItems: Boolean = false,
    val isExpanded: Boolean = false,
    val confirmButtonText: String? = null,
    val items: List<SectionItem> = emptyList(),
    val descriptionContent: String = "",
    /**
     * 栏目主题颜色。
     * 可选值同 PvzCollapsiblePanelTheme：
     * "BROWN"（默认）| "BLUE" | "BLUE_BACKGROUND" | "GREEN" | "GREEN_BACKGROUND" |
     * "RED" | "PURPLE" | "PURPLE_BACKGROUND" | "ORANGE" | "TEAL" | "TEAL_BACKGROUND" |
     * "GOLD" | "GRAY" | "GRAY_BACKGROUND"
     */
    val theme: PvzCollapsiblePanelTheme = PvzCollapsiblePanelTheme.BROWN,
    /**
     * 栏目级 JS 脚本。
     * - 当 confirmButtonText 存在时：点击确认按钮后执行
     * - 当 confirmButtonText 不存在时：进入游戏时执行
     */
    val jsScript: String? = null,
    /**
     * 栏目级 JS 脚本文件路径。
     * 当 jsScript 为空时，系统会从该路径加载 JS 文件执行。
     * 默认路径：`version/版本ID/栏目ID/main.js`。
     * 如果文件不存在且 jsScript 也为空，则不执行任何 JS 代码。
     */
    val jsPath: String? = null
) {
    /**
     * 辅助方法：获取最终的目标目录 File
     */
    fun resolveTargetDirectory(): File {
        return when (val path = this.targetPath) {
            null -> Pvz2ToolConfig.rootDirectory.resolve(InitializePvz2.config.smfDirectory)
            else -> Pvz2ToolConfig.rootDirectory.resolve(path)
        }
    }

    /**
     * 辅助方法：获取最终的 section 级 JS 文件路径。
     * 如果 jsPath 已配置则直接返回；否则返回默认路径 `version/版本ID/栏目ID/main.js`。
     */
    fun resolveJsPath(version: VersionDef): String {
        return jsPath ?: "version/${version.id}/$id/main.js"
    }

    fun readJs(version: VersionDef) = jsScript ?: AssetExtractorHolder.openInputStream(resolveJsPath(version))?.use { inputStream ->
        inputStream.bufferedReader().use { reader -> reader.readText() }
    }
}

@Serializable
data class SectionItem(
    val id: String,
    /** 必填。决定该功能项的渲染类型：RADIO | CHECKBOX | DESCRIPTION | BUTTON | SLIDER | INPUT | INFO */
    val type: SectionType,
    val name: String? = null,
    val desc: String? = null,
    val icon: String? = null,
    /**
     * SMF 资源路径，用于 JS 中 $SMF 占位符解析。
     * 不配置时默认 `version/版本ID/栏目ID/功能项ID`。
     */
    val assetPath: String? = null,
    val default: Boolean = false,
    /** 按钮类型（BUTTON）点击后执行的 JS 脚本，支持 async/await */
    val jsScript: String? = null,
    /**
     * JS 脚本文件路径。
     * 当 jsScript 为空时，系统会尝试从该路径加载 JS 文件执行。
     * 默认值为 `version/版本ID/栏目ID/功能项ID/main.js`。
     * 如果文件不存在且 jsScript 也为空，则不执行任何 JS 代码。
     */
    val jsPath: String? = null,
    /**
     * 仅对 RADIO 类型生效：组件分组 ID。
     * 同一 groupId 的 RADIO 组件互斥（只能选中一个）。
     * 默认为 "root"，表示全局互斥。
     * 设置不同 groupId 可创建多个独立的互斥组。
     */
    val groupId: String = "root",
    /**
     * 仅对 BUTTON 类型生效：按钮上显示的文本。
     * 不配置时默认使用 name。
     */
    val buttonText: String? = null,
    /**
     * 仅对 BUTTON 类型生效：该按钮的颜色风格。
     * 可选值："blue"（默认）| "red" | "green" | "orange" | "purple"
     */
    val buttonColor: String? = null,
    // ========== SLIDER 滑动条字段 ==========
    /**
     * 仅对 SLIDER 类型生效：滑动条的最小值。
     * 默认 0。
     */
    val minValue: Float = 0f,
    /**
     * 仅对 SLIDER 类型生效：滑动条的最大值。
     * 默认 100。
     */
    val maxValue: Float = 100f,
    /**
     * 仅对 SLIDER 类型生效：滑动条的默认值（sliderValue 初始值）。
     * 不配置时取 minValue。
     */
    val defaultValue: Float? = null,
    /**
     * 仅对 SLIDER 类型生效：滑动条每次拖动的步进值。
     * 默认 1。
     */
    val step: Float = 1f,
    /**
     * 仅对 SLIDER 类型生效：滑动条显示的数值后缀。
     * 如 "%"、"倍"、"ms" 等。
     */
    val valueSuffix: String? = null,
    // ========== INPUT 文本输入字段 ==========
    /**
     * 仅对 INPUT 类型生效：输入框的占位提示文字。
     */
    val placeholder: String? = null,
    /**
     * 仅对 INPUT 类型生效：输入框的默认值。
     */
    val inputDefault: String? = null,
    /**
     * 该功能项执行 JS 时需要读写的 SMF 文件名列表（不含扩展名，仅文件基名，如 "dynamic" 对应 dynamic.rsb.smf）。
     *
     * 系统会按顺序尝试以下路径查找 SMF 文件：
     * 1. `pvz2tool/version/版本ID/smf/<名称>.rsb.smf`
     * 2. `pvz2tool/<version.baseAssetPath>/<名称>.rsb.smf`（默认为 `pvz2tool/version/base/smf/`）
     *
     * 缓存目录统一到版本级别：`cacheDir/pvz2tool/js_smf_cache/版本ID/<名称>.rsb.smf/`，
     * 同版本下不同 item 共享同一份解包缓存，修改可正确合并。
     */
    val smfList: List<String> = emptyList(),
    // ========== INFO 只读信息字段 ==========
    /**
     * 仅对 INFO 类型生效：信息展示的当前值（通常由 JS 动态更新）。
     */
    val infoValue: String? = null
) {

    val displayName get() = name ?: buttonText ?: desc ?: id

    /**
     * 辅助方法：获取最终的 Asset 路径。
     * @param section 当前 item 所属的 Section
     */
    fun resolvePath(section: DynamicSection, version: VersionDef): String {
        return this.assetPath ?: "version/${version.id}/${section.id}/${this.id}"
    }

    /**
     * 辅助方法：获取最终的 JS 文件路径。
     * 如果 jsPath 已配置则直接返回；否则返回默认路径 `version/版本ID/栏目ID/功能项ID/main.js`。
     * @param section 当前 item 所属的 Section
     */
    fun resolveJsPath(section: DynamicSection, version: VersionDef): String {
        return this.jsPath ?: "version/${version.id}/${section.id}/${this.id}/main.js"
    }

    fun readJs(section: DynamicSection, version: VersionDef) = jsScript?.takeIf { it.isNotBlank() } ?: AssetExtractorHolder.openInputStream(resolveJsPath(section,version))?.use { inputStream ->
        inputStream.bufferedReader().use { reader -> reader.readText() }
    }
}

@Serializable
data class VersionDef(
    val id: String,
    val name: String,
    val desc: String,
    val icon: String? = null,
    val default: Boolean = false,
    val baseAssetPath: String? = "version/base/smf",
    val assetPath: String? = null,
    val forceOverride: Boolean = false,
    /**
     * 进入游戏前执行的 JS 脚本（版本级）。
     * 仅在选中该版本时触发，优先级高于根级 enterGameScript。
     */
    val enterGameScript: String? = null,
    /**
     * 进入游戏前执行的 JS 脚本文件路径（版本级）。
     * 当 enterGameScript 为空时，系统会从该路径加载 JS 文件执行。
     * 默认路径：`version/版本ID/main.js`。
     * 如果文件不存在且 enterGameScript 也为空，则不执行任何 JS 代码。
     */
    val enterGamePath: String? = null
) {
    companion object {
        /** 版本存档隔离根目录名（在应用私有目录下） */
        const val VERSION_SAVES_DIR = "version_saves"
    }

    fun resolveAssetPath(): String {
        return assetPath ?: "version/$id/smf"
    }

    /**
     * 辅助方法：获取进入游戏 JS 文件路径。
     * 如果 enterGamePath 已配置则直接返回；否则返回默认路径 `version/版本ID/main.js`。
     */
    fun resolveEnterGamePath(): String {
        return enterGamePath ?: "version/$id/main.js"
    }

    fun readJs() = enterGameScript ?: AssetExtractorHolder.openInputStream(resolveEnterGamePath())?.use { inputStream ->
        inputStream.bufferedReader().use { reader -> reader.readText() }
    }

    /**
     * 辅助方法：获取版本专属的游戏存档目录。
     * 放在应用私有目录，确保可写。
     * 返回 `{app_private_dir}/version_saves/<版本ID>/`
     */
    fun resolveGameSaveDirectory(): File {
        // 使用应用私有目录，确保可写
        val privateDir = InitializePvz2.context.getExternalFilesDir(null)
            ?: InitializePvz2.context.filesDir
        val baseDir = privateDir?.parentFile ?: privateDir ?: Pvz2ToolConfig.rootDirectory
        return File(baseDir, "$VERSION_SAVES_DIR/$id")
    }
}

@Serializable
data class Pvz2ToolConfigAnnouncement(
    val title: String,
    val content: String
)

@Serializable
data class Pvz2ToolConfigUI(
    val title: Pvz2ToolConfigUITitle,
    val button: Pvz2ToolConfigUIButton,
    val extractor: Pvz2ToolConfigUIExtractor,
    val noValidDirTip: String,
    val versionLabel: String,
    val uiVersion: String,
    val authorInfo: String,
    val tutorial: String,
    val save: Pvz2ToolConfigUISave, // 原有存档配置
    val settings: Pvz2ToolConfigUISettings, // 设置弹窗
    val log: Pvz2ToolConfigUILog = Pvz2ToolConfigUILog(), // JS 日志面板
    val dialog: Pvz2ToolConfigUIDialog = Pvz2ToolConfigUIDialog(), // 通用对话框按钮
    val error: Pvz2ToolConfigUIError = Pvz2ToolConfigUIError(), // 错误提示文案
    val welcome: Pvz2ToolConfigUIWelcome = Pvz2ToolConfigUIWelcome(), // 欢迎用户组件
    /** 功能属性（背景/音乐/视频/权限等路径与开关）*/
    val assets: Pvz2ToolConfigUIAssets = Pvz2ToolConfigUIAssets(),
    /** 音效文件名映射（相对于 assets/pvz2tool/sound/） */
    val sounds: Pvz2ToolConfigUISounds = Pvz2ToolConfigUISounds(),
)

@Serializable
data class Pvz2ToolConfigUITitle(
    val topAppBar: String,
    val about: String,
    val coreFunction: String,
    val versionManage: String,
)

@Serializable
data class Pvz2ToolConfigUIButton(
    val enterGame: String,
    val isEnterGameDefaultIcon: Boolean = true,
    val tutorial: String,
    val isTutorialDefaultIcon: Boolean = true,
    val resetData: String,
    val isResetDataDefaultIcon: Boolean = true,
    val disconnectTheNetworkAndStart: String = "断网启动",
    val confirmVersion: String
)

@Serializable
data class Pvz2ToolConfigUIExtractor(
    val dialogTitle: String = "戴夫的工具箱 | 资源更新",
    val initialLoadingProgressTip: String = "戴夫正在清点物资清单...",
    val initialProgressTip: String = "戴夫检测到新的版本波动啦，准备更新物资...",
    val noNeedExtractTip: String = "戴夫检查了工具箱，暂时没有新物资需要更新~",
    val singleFileProcessingTip: String = "戴夫正在手忙脚乱整理物资：",
    val multiFileProcessingTip: String = "戴夫正在手忙脚乱整理%d个物资：",
    val waitingTip: String = "戴夫正在整理物资，旅途中稍等片刻~",
    val extractCompleteTip: String = "物资更新完毕！\n准备好再次迎战僵尸了吗，玩家！\n【%s】",
    val extractFailTipPrefix: String = "糟糕！戴夫的工具箱出问题了：",
    val fileSkipTipPrefix: String = "戴夫检查到「%s」无需更新，跳过啦~",
    val continueButtonText: String = "继续物资准备",
    val completeButtonText: String = "重返战场",
    val toastErrorPrefix: String = "戴夫的小提示：更新失败啦 → "
)

@Serializable
data class Pvz2ToolConfigUISave(
    // 弹窗标题
    val presetConfirmTitle: String,
    val presetConfirmMessage: String,
    val deleteConfirmTitle: String,
    val deleteConfirmMessage: String,
    val coverConfirmTitle: String,
    val coverConfirmMessage: String,
    val deleteGameSaveConfirmTitle: String = "删除游玩存档",
    val deleteGameSaveConfirmMessage: String = "此操作将永久清空您当前的游戏进度，删除后无法恢复！确定要继续吗？",
    val saveInfoTitle: String,

    // 输入框标签
    val saveNameLabel: String,
    val saveDescLabel: String,

    // 按钮文字
    val cancelButton: String,
    val confirmButton: String,
    val shareButton: String = "分享所有本地存档",
    val exportButton: String,
    val importButton: String,
    val backupButton: String,
    val coverLocalButton: String,
    val deleteGameSaveButton: String = "删除游玩存档",
    val coverPresetButton: String,

    // 提示文字
    val saveNameEmptyTip: String,
    val noLocalSaveTip: String,
    val selectLocalSaveTip: String,

    // 操作结果提示
    val backupSuccessTip: String,
    val backupFailTipPrefix: String,
    val exportSuccessTip: String,
    val exportFailTipPrefix: String,
    val importSuccessTip: String,
    val importFailTipPrefix: String,
    val deleteSuccessTip: String,
    val deleteFailTipPrefix: String,
    val coverSuccessTip: String,
    val coverFailTipPrefix: String,
    val deleteGameSaveSuccessTip: String = "当前游玩存档已成功删除",
    val deleteGameSaveFailTipPrefix: String = "删除游玩存档失败：%s",
    val defaultImportNamePrefix: String, // 导入_xxx
    val defaultBackupDesc: String,       // 自动备份
    val defaultImportDesc: String,       // 手动导入

    // 新增：重试按钮文字
    val retryButtonText: String,
    val operation: Pvz2ToolConfigOperation, // 新增操作类型
)

// 2. 新增【操作类型中文描述】配置（结果弹窗标题用）
@Serializable
data class Pvz2ToolConfigOperation(
    val backup: String,
    val export: String,
    val import: String,
    val delete: String,
    val deleteGameSave: String = "删除游玩存档",
    val cover: String,
    val saveMeta: String
)

// 3. 新增【设置弹窗】配置
@Serializable
data class Pvz2ToolConfigUISettings(
    val title: String,
    val solidBackgroundMode: String,
    val changeTheProfileReadLocation: String,
    val reloadConfig: String,
    val playBackgroundMusic: String = "播放背景音乐",
    val resetPacketDeepClearing: String = "重置数据包时删除smf目录",
    val showNotUpdate: String = "进入游戏时未检测到更新也进行弹窗",
    val importSmfFile: String = "导入SMF文件",
    /** 是否启用自定义游戏画面设置的文字标签 */
    val customGameDisplay: String = "自定义游戏画面",
    /** 自定义游戏画面子页面标题 */
    val customGameDisplayTitle: String = "游戏画面设置",
    /** 游戏画面配置默认值 */
    val gameDisplay: Pvz2ToolConfigGameDisplay = Pvz2ToolConfigGameDisplay(),
)

/**
 * 游戏画面自定义配置
 * 在"自定义游戏画面"子页面中生效
 */
@Serializable
data class Pvz2ToolConfigGameDisplay(
    /** 是否默认启用自定义游戏画面（总开关）*/
    val isUseCustomGameDisplay: Boolean = false,
    /** 允许随意翻转界面（支持竖屏）的文字标签 */
    val allowRotation: String = "允许随意翻转界面（支持竖屏）",
    /** 是否默认允许随意翻转 */
    val isAllowRotation: Boolean = false,
    /** 自定义窗口尺寸的文字标签 */
    val customWindowSize: String = "自定义窗口宽高",
    /** 自定义窗口比例的文字标签 */
    val customWindowRatio: String = "自定义窗口比例",
    /** 全屏模式的文字标签 */
    val fullscreen: String = "全屏",
    /** 显示模式：fullscreen / ratio / size，默认 fullscreen */
    val displayMode: String = "ratio",
    /** displayMode=size 时的窗口宽度（dp）*/
    val windowWidth: Int = 1280,
    /** displayMode=size 时的窗口高度（dp）*/
    val windowHeight: Int = 720,
    /** displayMode=ratio 时的宽高比（宽/高），例如 1.5 表示 3:2 */
    val windowRatio: Float = 1.5f,
)

// 4. 【JS 日志面板】配置
@Serializable
data class Pvz2ToolConfigUILog(
    /** 日志面板标题 */
    val panelTitle: String = "JS 日志",
    /** 复制日志按钮的无障碍描述 */
    val copyLogDesc: String = "复制日志",
    /** 清空日志按钮的无障碍描述 */
    val clearLogDesc: String = "清空日志",
    /** 无日志时的占位文本 */
    val noLogText: String = "暂无日志",
    /** 预设存档区域标题 */
    val presetSaveLabel: String = "预设存档",
    /** 本地存档区域标题 */
    val localSaveLabel: String = "本地存档",
)

// 5. 【通用对话框按钮】配置
@Serializable
data class Pvz2ToolConfigUIDialog(
    /** 通用对话框取消按钮 */
    val cancel: String = "取消",
    /** 通用对话框确定按钮 */
    val confirm: String = "确定",
    /** 存档列表删除按钮无障碍描述 */
    val deleteSaveDesc: String = "删除存档",
    /** 编辑用户名按钮无障碍描述 */
    val editUserNameDesc: String = "编辑用户名",
    /** 分享 PVZ2 存档时系统分享弹窗标题 */
    val shareSaveChooserTitle: String = "分享 PVZ2 存档",
    /** 分享存档失败提示 */
    val sharePackFailedTip: String = "打包存档失败",
    /** 无可分享存档提示 */
    val noShareableSaveTip: String = "没有可分享的本地存档",
)

// 6. 【错误提示文案】配置
@Serializable
data class Pvz2ToolConfigUIError(
    /** JS 执行出错弹窗标题 */
    val jsExecuteErrorTitle: String = "JS 执行出错",
    /** 游戏 Activity 设置有误提示 */
    val gameActivityInvalid: String = "设置的游戏Activity有误或不存在",
    /** 异常消息为空时的兜底文字 */
    val unknownError: String = "未知错误",
)

// 7. 【欢迎用户组件】配置
@Serializable
data class Pvz2ToolConfigUIWelcome(
    /** 欢迎语模板，%s 替换为用户名 */
    val greetingTemplate: String = "欢迎您，%s",
    /** 编辑用户名弹窗标题 */
    val editUserNameTitle: String = "修改用户名",
    /** 编辑用户名输入框提示文字 */
    val editUserNameHint: String = "请输入新的用户名",
)

// 9. 【功能属性】背景/视频/音乐/权限等路径与开关
@Serializable
data class Pvz2ToolConfigUIAssets(
    val background: String = "bg_main.jpg",
    /** 是否使用纯色背景（false 则使用 background 图片） */
    val isUseSolidColorBackground: Boolean = true,
    /** 背景音乐文件名（相对于 assets/pvz2tool/sound/） */
    val backgroundMusic: String = "bg_music.wav",
    /** 是否默认播放背景音乐 */
    val isPlayBackgroundMusic: Boolean = true,
    /** CG 开场视频文件名（相对于 assets/pvz2tool/video/） */
    val cgVideoPath: String = "opening.mp4",
    /** CG 视频加载超时时间（毫秒），默认 5000ms */
    val cgVideoLoadTimeout: Long = 5000L,
    /** CG 视频失败后的海报图片（相对于 assets/pvz2tool/video/），为空则不显示海报直接跳过 */
    val cgVideoPoster: String? = null,
) {

    /**
     * 检测路径是否为 URL（支持 http:// 和 https://）
     */
    private fun isUrl(path: String): Boolean = path.startsWith("http://") || path.startsWith("https://")

    /**
     * 解析背景音乐路径：
     * - 如果是 URL，直接返回
     * - 否则拼接为 assets 下的 sound 路径
     */
    val resolvedBackgroundMusic: String get() = run {
        val musicPath = backgroundMusic
        if (isUrl(musicPath)) musicPath else "sound/$musicPath"
    }

    /**
     * 解析 CG 视频路径：
     * - 如果是 URL，直接返回
     * - 否则拼接为 video 路径（用于 AssetExtractorHolder.open()）
     */
    val resolvedCgVideoPath: String get() = run {
        val videoPath = cgVideoPath
        if (isUrl(videoPath)) videoPath else "video/$videoPath"
    }

}

// 10. 【音效文件名映射】所有 UI 音效（相对于 assets/pvz2tool/sound/）
@Serializable
data class Pvz2ToolConfigUISounds(
    // ─── 通用点击开关（RADIO/CHECKBOX/列表条目等） ───
    /** 点击/开关 按下音效 */
    val switchClickPress: String = "ui_switch_click_press.wav",
    /** 点击/开关 释放音效 */
    val switchClickRelease: String = "ui_switch_click_release.wav",
    /** 点击/开关 单次点击音效（无 press/release 区分时使用） */
    val switchClick: String = "ui_switch_click.wav",

    // ─── 标准按钮（PvzButton） ───
    /** 普通按钮 按下音效 */
    val buttonClickPress: String = "ui_button_click_press.wav",
    /** 普通按钮 释放音效 */
    val buttonClickRelease: String = "ui_button_click_release.wav",

    // ─── 设置/编辑类按钮（小齿轮图标等） ───
    /** 设置按钮 按下音效 */
    val buttonSettingsPress: String = "ui_button_settings_press.wav",
    /** 设置按钮 释放音效 */
    val buttonSettingsRelease: String = "ui_button_settings_release.wav",

    // ─── 关闭按钮（X 按钮） ───
    /** 关闭按钮 按下音效 */
    val buttonXClosePress: String = "ui_button_x_close_press.wav",
    /** 关闭按钮 释放音效 */
    val buttonXCloseRelease: String = "ui_button_x_close_release.wav",

    // ─── 可折叠面板 ───
    /** 折叠面板标题 按下音效 */
    val collapsiblePanelPress: String = "ui_collapsible_panel_click_press.wav",
    /** 折叠面板标题 释放音效 */
    val collapsiblePanelRelease: String = "ui_collapsible_panel_click_release.wav",
)
