@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.dreammooncai.pvz2tool.ui.main

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.runtime.*import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dreammooncai.pvz2tool.DynamicSection
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.Pvz2ToolConfig
import io.github.dreammooncai.pvz2tool.Pvz2ToolTheme
import io.github.dreammooncai.pvz2tool.SectionItem
import io.github.dreammooncai.pvz2tool.SectionType
import io.github.dreammooncai.pvz2tool.bottomRoundedBorder
import io.github.dreammooncai.pvz2tool.icon.*
import io.github.dreammooncai.pvz2tool.topRoundedBorder
import io.github.dreammooncai.pvz2tool.ui.dialog.AssetExtractorHolder
import io.github.dreammooncai.pvz2tool.ui.dialog.PvzConfirmDialog
import io.github.dreammooncai.pvz2tool.ui.dialog.PvzExtractorDialog
import io.github.dreammooncai.pvz2tool.ui.dialog.ResourcePair
import io.github.dreammooncai.pvz2tool.ui.dialog.rememberAssetExtractor
import io.github.dreammooncai.pvz2tool.view.PvzCollapsiblePanel
import io.github.dreammooncai.pvz2tool.view.PvzAnnouncementDialog
import io.github.dreammooncai.pvz2tool.view.PvzSimpleCardBrown
import io.github.dreammooncai.pvz2tool.view.PvzSimpleCardGreen
import io.github.dreammooncai.pvz2tool.view.PvzCardGreen
import io.github.dreammooncai.pvz2tool.view.PvzCardNewYear
import io.github.dreammooncai.pvz2tool.view.PvzRichText
import io.github.dreammooncai.pvz2tool.view.PvzTextGrayStyle
import io.github.dreammooncai.pvz2tool.view.PvzTextRedStyle
import io.github.dreammooncai.pvz2tool.view.PvzTextWhiteStyle
import io.github.dreammooncai.pvz2tool.view.PvzCollapsiblePanelTheme
import io.github.dreammooncai.pvz2tool.view.PvzBlueButton
import io.github.dreammooncai.pvz2tool.view.PvzGreenButton
import io.github.dreammooncai.pvz2tool.view.PvzOrangeButton
import io.github.dreammooncai.pvz2tool.view.PvzPurpleButton
import io.github.dreammooncai.pvz2tool.view.PvzRedButton
import io.github.dreammooncai.pvz2tool.view.ImageSvgButton
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import io.github.dreammooncai.manager.FilePickerManager
import io.github.dreammooncai.pvz2tool.VersionDef
import io.github.dreammooncai.pvz2tool.controller.SoundController
import io.github.dreammooncai.pvz2tool.service.LocalVpnService
import io.github.dreammooncai.pvz2tool.js.PvzToolJsEngine
import io.github.z4kn4fein.semver.Version
import io.github.dreammooncai.pvz2tool.js.JsConsole
import io.github.dreammooncai.pvz2tool.js.JsLogLevel
import io.github.dreammooncai.pvz2tool.js.JsLogger
import io.github.dreammooncai.pvz2tool.pop.rton.RTON
import io.github.dreammooncai.pvz2tool.rememberSoundInteractionSource
import io.github.dreammooncai.pvz2tool.ui.dialog.JsAlertDialog
import io.github.dreammooncai.pvz2tool.ui.dialog.JsConfirmDialog
import io.github.dreammooncai.pvz2tool.ui.dialog.JsExtractorDialog
import io.github.dreammooncai.pvz2tool.ui.dialog.JsProgressDialog
import io.github.dreammooncai.pvz2tool.ui.dialog.JsPromptDialog
import io.github.dreammooncai.pvz2tool.ui.dialog.PvzStyledDialog
import io.github.dreammooncai.pvz2tool.ui.dialog.PvzTutorialDialog
import io.github.dreammooncai.pvz2tool.view.PvzTextStyle
import io.github.dreammooncai.pvz2tool.view.JsExecutionContext
import io.github.dreammooncai.pvz2tool.view.LocalJsExecutionContext
import io.github.dreammooncai.pvz2tool.view.AsyncImageFromAssets
import io.github.dreammooncai.pvz2tool.view.PvzTextOliveStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale
import kotlin.collections.plus
import kotlin.coroutines.resume
import kotlin.math.max

// ======================== 辅助函数 ========================
/**
 * 根据背景颜色自动选择高对比度的文字颜色
 * 浅色背景 → 深色文字
 * 深色背景 → 白色文字
 */
private fun getContrastingTextColor(backgroundColor: Color): Color {
    // 计算亮度：L = 0.299*R + 0.587*G + 0.114*B
    val luminance = 0.299f * backgroundColor.red +
                     0.587f * backgroundColor.green +
                     0.114f * backgroundColor.blue
    return if (luminance > 0.5f) Color(0xFF4A3010) else Color.White
}

// ======================== 1. 常量配置 ========================
object Pvz2Constants {
    object Dimension {
        const val PADDING_SMALL = 8
        const val PADDING_MEDIUM = 16
        const val BUTTON_HEIGHT_MAIN = 48
        const val BUTTON_HEIGHT_SMALL = 40
        const val TOP_BAR_HEIGHT = 56
    }
}

/**
 * 纯视觉 UI 状态单例。
 * 存活于 key(mPvz2MainScreenReloadKey) 的重建周期之外，只影响观感不影响数据，
 * 因此无需在「重置数据」时清空——调用 [resetAll] 即可在确实需要时清空。
 *
 * - [scrollOffset]：主内容区的滚动位置（像素值），重建后恢复到此位置
 * - [sectionExpandedStates]：各栏目展开/折叠状态，key 为 section.id
 */
object Pvz2MainScreenUiState {
    /** 主内容区滚动偏移（px）。重建时用于 animateScrollTo 恢复位置。 */
    var scrollOffset: Int = 0

    /** id -> MutableState<Boolean>（展开状态），跨重建共享同一个 State 对象 */
    private val _sectionExpandedStates = mutableMapOf<String, MutableState<Boolean>>()

    /**
     * 获取指定 id 的展开状态 MutableState。
     * 若不存在则以 [defaultExpanded] 为初始值新建并缓存。
     */
    fun getExpandedState(id: String, defaultExpanded: Boolean): MutableState<Boolean> {
        return _sectionExpandedStates.getOrPut(id) { mutableStateOf(defaultExpanded) }
    }

    /**
     * 在「重置数据」等确实需要恢复默认视觉的场景下调用，清空所有缓存状态。
     */
    fun resetAll() {
        scrollOffset = 0
        _sectionExpandedStates.clear()
    }
}

@Serializable
data class Pvz2ScreenState(
    val selectedVersion: VersionDef? = null,
    val sectionStates: Map<String, DynamicSectionState> = emptyMap(),
)

@Serializable
data class DynamicSectionState(
    /** RADIO 类型的选中项：groupId -> itemId（支持多组互斥） */
    val selectedPresetItemIds: Map<String, String> = emptyMap(),
    val selectedLocalSaveId: String? = null,
    val checkedItemIds: Set<String> = emptySet(),
    /** SLIDER 类型的滑动值：itemId -> value */
    val sliderValues: Map<String, Float> = emptyMap(),
    /** INPUT 类型的输入值：itemId -> value */
    val inputValues: Map<String, String> = emptyMap(),
    /** INFO 类型的动态值：itemId -> displayValue（JS 执行结果） */
    val infoValues: Map<String, String> = emptyMap(),
    /** DESCRIPTION 类型的动态值：itemId -> descValue（JS 执行结果） */
    val descriptionValues: Map<String, String> = emptyMap()
)

// ======================== 3. 通用UI组件 ========================
@Composable
private fun RadioButtonItem(
    label: String?,
    subLabel: String?,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIconPath: String? = null,
    theme: PvzCollapsiblePanelTheme = PvzCollapsiblePanelTheme.BROWN,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = rememberSoundInteractionSource(
                    InitializePvz2.config.ui.sounds.switchClickPress,
                    InitializePvz2.config.ui.sounds.switchClickRelease
                )
            ) {
                SoundController.playSoundFromAssets(InitializePvz2.config.ui.sounds.switchClick)
                onSelect()
            }
            .padding(Pvz2Constants.Dimension.PADDING_SMALL.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            imageVector = if (selected) Pvz2Icon.HookSelect else Pvz2Icon.Hook,
            contentDescription = if (selected) "已选中" else "未选中",
            modifier = Modifier.size(24.dp)
        )

        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIconPath?.let { path ->
                AsyncImageFromAssets(
                    "images/$path",
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.width(Pvz2Constants.Dimension.PADDING_SMALL.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                if (!label.isNullOrBlank())
                    PvzRichText(
                        label,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        defaultStyle = PvzTextStyle(theme.primaryTextColor),
                    )
                if (!subLabel.isNullOrBlank())
                    PvzRichText(
                        subLabel,
                        fontSize = 10.sp,
                        defaultStyle = PvzTextStyle(theme.secondaryTextColor),
                    )
            }
        }
    }
}

@Composable
private fun SectionSwitchItem(
    item: SectionItem,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    theme: PvzCollapsiblePanelTheme = PvzCollapsiblePanelTheme.BROWN,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item.icon?.let { path ->
            AsyncImageFromAssets(
                "images/$path",
                contentDescription = item.name,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.width(Pvz2Constants.Dimension.PADDING_SMALL.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (!item.name.isNullOrBlank())
                PvzRichText(
                    item.name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    defaultStyle = PvzTextStyle(theme.primaryTextColor),
                )
            if (!item.desc.isNullOrBlank())
                PvzRichText(
                    item.desc,
                    fontSize = 10.sp,
                    defaultStyle = PvzTextStyle(theme.secondaryTextColor),
                )
        }
        Image(
            imageVector = if (checked) Pvz2Icon.HookSelect else Pvz2Icon.Hook,
            contentDescription = if (checked) "已选中" else "未选中",
            modifier = Modifier
                .size(24.dp)
                .clickable(
                    interactionSource = rememberSoundInteractionSource(
                        InitializePvz2.config.ui.sounds.switchClickPress,
                        InitializePvz2.config.ui.sounds.switchClickRelease
                    )
                ) {
                    SoundController.playSoundFromAssets(InitializePvz2.config.ui.sounds.switchClick)
                    onCheckedChange(!checked)
                }
        )
    }
}

// ======================== BUTTON 类型专用组件 ========================

/**
 * 单个 JS 按钮项：
 * - 有 name 或 desc 时：左侧名称+描述，右侧执行按钮（可配色）
 * - name 和 desc 均为空时：直接渲染为全宽横条大按钮（同存档按钮风格）
 * 执行中显示 loading 圈。
 * @param section 当前按钮所属的 Section（用于 $GAME_SAVES 路径解析）
 */
@Composable
private fun SectionButtonItem(
    item: SectionItem,
    section: DynamicSection,
    version: VersionDef,
    sectionStates: Map<String, DynamicSectionState>,
    scope: CoroutineScope,
    modifier: Modifier = Modifier,
    theme: PvzCollapsiblePanelTheme = PvzCollapsiblePanelTheme.BROWN,
    updateSectionState: (String, (DynamicSectionState) -> DynamicSectionState) -> Unit
) {
    CompositionLocalProvider(LocalJsExecutionContext provides JsExecutionContext(
        section = section,
        item = item,
        version = version,
        sectionStates = sectionStates,
        updateSectionState = updateSectionState
    )) {
        var isRunning by remember { mutableStateOf(false) }

        // item.buttonColor，不配置则默认蓝色
        val resolvedButtonColor = item.buttonColor ?: "blue"

        val jsExtractor = rememberAssetExtractor(
            context = LocalContext.current
        )

        val uiState by jsExtractor.extractor.uiState
        PvzExtractorDialog(uiState = uiState,SettingsDialogState.isUseShowNotUpdate, onDismissRequest = { jsExtractor.extractor.dismiss() })

        // 构建按钮点击逻辑
        val onClick = {
            val jsScript = item.readJs(section,version)
            if (jsScript != null) {
                isRunning = true
                scope.launch {
                    try {
                        PvzToolJsEngine.executeScript(
                            extractor = jsExtractor,
                            script = jsScript,
                            section = section,
                            item = item,
                            version = version,
                            sectionStates = sectionStates,
                            updateSectionState = updateSectionState
                        )
                    } finally {
                        isRunning = false
                    }
                }
            }
            Unit
        }

        val btnText = item.buttonText ?: item.displayName
        val hasLabel = !item.name.isNullOrBlank() || !item.desc.isNullOrBlank()

        if (!hasLabel) {
            // ── 无名称+无描述：全宽横条大按钮（同存档风格） ──
            if (isRunning) {
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .height(Pvz2Constants.Dimension.BUTTON_HEIGHT_MAIN.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.5.dp,
                        color = theme.primaryTextColor
                    )
                }
            } else {
                val fullModifier = modifier
                    .fillMaxWidth()
                    .height(Pvz2Constants.Dimension.BUTTON_HEIGHT_MAIN.dp)
                RenderColoredButton(resolvedButtonColor, btnText, fullModifier, onClick)
            }
            return@CompositionLocalProvider
        }

        // ── 有名称或描述：左侧文字 + 右侧小按钮 ──
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标（可选）
            item.icon?.let { path ->
                AsyncImageFromAssets(
                    "images/$path",
                    contentDescription = item.name,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.width(Pvz2Constants.Dimension.PADDING_SMALL.dp))

            // 名称 + 描述
            Column(modifier = Modifier.weight(1f)) {
                if (!item.name.isNullOrBlank()) {
                    PvzRichText(
                        item.name,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        defaultStyle = PvzTextStyle(theme.primaryTextColor)
                    )
                }
                if (!item.desc.isNullOrBlank()) {
                    PvzRichText(
                        item.desc,
                        fontSize = 10.sp,
                        defaultStyle = PvzTextStyle(theme.secondaryTextColor)
                    )
                }
            }

            Spacer(modifier = Modifier.width(Pvz2Constants.Dimension.PADDING_SMALL.dp))

            // 执行按钮 / Loading 指示
            if (isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.5.dp,
                    color = theme.primaryTextColor
                )
            } else {
                val buttonModifier = Modifier.height(38.dp)
                RenderColoredButton(resolvedButtonColor, btnText, buttonModifier, onClick)
            }
        }
    }
}

/**
 * 根据颜色字符串渲染对应颜色的 PVZ 按钮，抽出复用。
 */
@Composable
private fun RenderColoredButton(
    color: String,
    text: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    when (color.lowercase()) {
        "red"    -> PvzRedButton(text = text, modifier = modifier, onClick = onClick)
        "green"  -> PvzGreenButton(text = text, modifier = modifier, onClick = onClick)
        "orange" -> PvzOrangeButton(text = text, modifier = modifier, onClick = onClick)
        "purple" -> PvzPurpleButton(text = text, modifier = modifier, onClick = onClick)
        else     -> PvzBlueButton(text = text, modifier = modifier, onClick = onClick)  // "blue" 默认
    }
}

// ======================== 4. 存档相关UI组件 ========================
@Composable
private fun LocalSaveRadioItem(
    entity: PvzLocalSaveEntity,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    theme: PvzCollapsiblePanelTheme = PvzCollapsiblePanelTheme.BROWN
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = rememberSoundInteractionSource(
                    InitializePvz2.config.ui.sounds.switchClickPress,
                    InitializePvz2.config.ui.sounds.switchClickRelease
                )
            ) {
                SoundController.playSoundFromAssets(InitializePvz2.config.ui.sounds.switchClick)
                onSelect()
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            imageVector = if (selected) Pvz2Icon.HookSelect else Pvz2Icon.Hook,
            contentDescription = if (selected) "已选中" else "未选中",
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
        ) {
            PvzRichText(
                text = "${entity.name}(${entity.getFormattedCreateTime()})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                defaultStyle = PvzTextStyle(theme.primaryTextColor)
            )
            PvzRichText(
                text = entity.desc,
                fontSize = 10.sp,
                defaultStyle = PvzTextStyle(theme.secondaryTextColor)
            )
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = InitializePvz2.config.ui.dialog.deleteSaveDesc,
                tint = Color.Red,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ========== 预设存档区域组件（核心修改：增加二次确认 + 统一File操作） ==========
@Composable
private fun PresetSaveSection(
    section: DynamicSection,
    state: DynamicSectionState,
    onStateChange: (DynamicSectionState) -> Unit,
    extractorHolder: AssetExtractorHolder,
    gameSaveDir: File,
    version: VersionDef // 【修复】新增参数：当前版本ID
) {
    val config = InitializePvz2.config
    val saveConfig = config.ui.save

    // 新增：预设存档二次确认弹窗状态
    val showPresetConfirmDialog = remember { mutableStateOf(false) }
    val presetConfirmTitle = remember { mutableStateOf("") }
    val presetConfirmMessage = remember { mutableStateOf("") }
    var onPresetConfirmAction by remember { mutableStateOf({}) }

    // 弹窗显示方法
    fun showPresetConfirmDialog(
        title: String,
        message: String,
        onConfirm: () -> Unit
    ) {
        presetConfirmTitle.value = title
        presetConfirmMessage.value = message
        onPresetConfirmAction = onConfirm
        showPresetConfirmDialog.value = true
    }

    // 执行预设存档提取（统一File操作）
    fun performPresetExtract(item: SectionItem) {
        // 【修复】传入 versionId
        val assetPath = item.resolvePath(section, version)
        // 统一使用AssetExtractorHolder处理File对象
        val resourcePair = AssetExtractorHolder.resource(assetPath, gameSaveDir,true ,sectionName = section.title)
        extractorHolder.setOnDismissListener {
            if (it.isComplete) PvzSaveFileManager.notifyGameSaveChanged()
        }
        extractorHolder.extract(resourcePair)
    }

    PvzRichText(
        text = config.ui.log.presetSaveLabel,
        defaultStyle = PvzTextRedStyle,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )


    section.items.forEach { item ->
        RadioButtonItem(
            label = item.name,
            subLabel = item.desc,
            selected = state.selectedPresetItemIds[item.groupId] == item.id,
            onSelect = {
                val newState = state.copy(
                    selectedPresetItemIds = state.selectedPresetItemIds + (item.groupId to item.id)
                )
                onStateChange(newState)
            },
            leadingIconPath = item.icon,
        )
    }

    val confirmBtnText = section.confirmButtonText ?: saveConfig.coverPresetButton
    // 确认按钮：增加二次确认
    confirmBtnText.let { btnText ->
        Spacer(modifier = Modifier.height(Pvz2Constants.Dimension.PADDING_SMALL.dp))
        PvzRedButton(
            text = btnText,
            modifier = Modifier
                .fillMaxWidth()
                .height(Pvz2Constants.Dimension.BUTTON_HEIGHT_MAIN.dp),
            icon = Icons.Default.Save
        ) {
            // 获取当前 section 中 RADIO 组件的 groupId 对应的选中项
            val radioGroupIds = section.items
                .filter { it.type == SectionType.RADIO }
                .map { it.groupId }
                .distinct()
            
            radioGroupIds.forEach { groupId ->
                state.selectedPresetItemIds[groupId]?.let { itemId ->
                    section.items.find { it.id == itemId && it.type == SectionType.RADIO }?.let { item ->
                        showPresetConfirmDialog(
                            title = saveConfig.presetConfirmTitle,
                            message = String.format(saveConfig.presetConfirmMessage, item.name),
                            onConfirm = { performPresetExtract(item) }
                        )
                    }
                }
            }
        }
    }

    // 预设存档二次确认弹窗
    PvzConfirmDialog(
        isVisible = showPresetConfirmDialog.value,
        title = presetConfirmTitle.value,
        message = presetConfirmMessage.value,
        onConfirm = onPresetConfirmAction,
        onDismiss = { showPresetConfirmDialog.value = false }
    )
}

// ========== 提取的本地存档区域组件 ==========
@Composable
private fun LocalSaveSection(
    context: Context,
    state: DynamicSectionState,
    onStateChange: (DynamicSectionState) -> Unit,
    gameSaveDir: File,
    filePickerManager: FilePickerManager?,
    saveInfoDialogState: PvzSaveInfoDialogState,
    version: VersionDef,
    theme: PvzCollapsiblePanelTheme = PvzCollapsiblePanelTheme.BROWN
) {
    val config = InitializePvz2.config
    val saveConfig = config.ui.save

    val operationState = remember { PvzSaveOperationState() }

    // 重试需要记住的目标ID
    val lastDeleteId = remember { mutableStateOf<String?>(null) }
    val lastCoverId = remember { mutableStateOf<String?>(null) }

    // ==========================
    // 二次确认弹窗（原有逻辑保留）
    // ==========================
    val showConfirmDialog = remember { mutableStateOf(false) }
    val confirmTitle = remember { mutableStateOf("") }
    val confirmMessage = remember { mutableStateOf("") }
    var onConfirmAction by remember { mutableStateOf<() -> Unit>({}) }

    fun showConfirmDialog(
        title: String,
        message: String,
        onConfirm: () -> Unit
    ) {
        confirmTitle.value = title
        confirmMessage.value = message
        onConfirmAction = onConfirm
        showConfirmDialog.value = true
    }

    val refreshTrigger = remember { mutableLongStateOf(0L) }

    DisposableEffect(context) {
        val listener = { id: Long -> refreshTrigger.longValue = id }
        PvzLocalSaveManager.addRefreshListener(listener)
        onDispose { PvzLocalSaveManager.removeRefreshListener(listener) }
    }

    val localSaves by remember(refreshTrigger.longValue) {
        derivedStateOf { PvzLocalSaveManager.getAllLocalSaves(context) }
    }

    // ==========================
    // 操作函数（原有逻辑保留）
    // ==========================
    val scope = rememberCoroutineScope()

    fun performShareAllSaves() {
        if (localSaves.isEmpty()) {
            operationState.postResult(
                PvzSaveOperationResult(
                    type = PvzSaveOperationType.EXPORT, // 复用 EXPORT 类型
                    isSuccess = false,
                    message = config.ui.dialog.noShareableSaveTip
                )
            )
            return
        }

        scope.launch {
            // 1. 打包所有存档
            val shareUri = PvzSaveFileManager.packAllLocalSavesForShare(context, localSaves)

            if (shareUri == null) {
                operationState.postResult(
                    PvzSaveOperationResult(
                        type = PvzSaveOperationType.EXPORT,
                        isSuccess = false,
                        message = config.ui.dialog.sharePackFailedTip
                    )
                )
                return@launch
            }

            // 2. 调用系统分享
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = PvzSaveFileManager.SHARE_FILE_MIME_TYPE
                putExtra(Intent.EXTRA_STREAM, shareUri)
                // 给接收方临时读取权限
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, config.ui.dialog.shareSaveChooserTitle))
        }
    }
    fun performDelete(saveId: String) {
        lastDeleteId.value = saveId
        val result = PvzLocalSaveManager.deleteLocalSave(context, saveId)
        operationState.postResult(result)
    }

    fun performExport() {
        filePickerManager?.launch { uri, doc ->
            if (uri != null && doc != null) {
                PvzSaveFileManager.exportGameSaveToDocumentFile(
                    context = context,
                    sourceDir = gameSaveDir,
                    targetDoc = doc, onResult = operationState::postResult
                )
            } else {
                operationState.postResult(PvzSaveOperationType.EXPORT failed config.ui.noValidDirTip)
            }
        }
    }

    fun performImport() {
        filePickerManager?.launch { uri, doc ->
            if (uri != null && doc != null) {
                PvzSaveFileManager.batchImportAllSaves(
                    context = context,
                    sourceDoc = doc,
                    defaultImportDesc = saveConfig.defaultImportDesc,
                    defaultImportNamePrefix = saveConfig.defaultImportNamePrefix,
                    onSingleResult = operationState::postResult,
                    // 新增：等待操作结果弹窗关闭
                    onWaitForResultDismiss = operationState::awaitDismiss,
                    onEachSaveConfig = { defaultName, defaultDesc, importSuccessTip ->
                        suspendCancellableCoroutine { cont ->
                            // 在配置弹窗标题里显示导入成功提示（如果有的话）
                            val dialogTitle = importSuccessTip ?: saveConfig.saveInfoTitle


                            saveInfoDialogState.show(
                                title = dialogTitle, // 传入动态标题
                                name = defaultName,
                                desc = defaultDesc,
                                onDismiss = {
                                    if (cont.isActive) cont.resume(null)
                                }
                            ) { name, desc ->
                                if (cont.isActive) cont.resume(name to desc)
                            }
                        }
                    },
                    onFinalResult = { finalResult ->
                        // 全部完成后，显示最终结果
                        operationState.postResult(finalResult)
                    }
                )
            } else {
                operationState.postResult(PvzSaveOperationType.IMPORT failed config.ui.noValidDirTip)
            }
        }
    }


    fun performBackup() {
        // 异步获取存档用户名，构造 "版本名 - 用户名 (时间)" 格式的默认名称
        scope.launch {
            val userName = loadUserNameFromSave(context)
            val timePrefix = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date())
            val defaultName = if (userName != null) {
                "${version.name} - $userName ($timePrefix)"
            } else {
                "${version.name} ($timePrefix)"
            }
            saveInfoDialogState.show(
                title = saveConfig.saveInfoTitle,
                name = defaultName,
                desc = saveConfig.defaultBackupDesc,
            ) { name, desc ->
                val newId = PvzLocalSaveManager.generateSaveId()
                val backupDir = File(PvzLocalSaveManager.getLocalSaveRootDir(context), newId)
                val result = PvzSaveFileManager.backupCurrentSave(
                    context = context,
                    gameSaveDir = gameSaveDir,
                    backupDir = backupDir
                )
                if (!result.isSuccess) {
                    operationState.postResult(result)
                    return@show
                }
                handleSaveLocalSaveMeta(context, newId, backupDir, name, desc)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (InitializePvz2.hasVersionChanges() && InitializePvz2.mSfmVersion != Version.min) {
            val backupDir = File(PvzLocalSaveManager.getLocalSaveRootDir(context), InitializePvz2.mSfmVersion.toString())
            if (backupDir.exists()) return@LaunchedEffect
            val result = PvzSaveFileManager.backupCurrentSave(
                context = context,
                gameSaveDir = gameSaveDir,
                backupDir = backupDir
            )
            if (!result.isSuccess) {
                operationState.postResult(result)
                return@LaunchedEffect
            }
            val userName = loadUserNameFromSave(context)
            val timePrefix = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date())
            val defaultName = if (userName != null) {
                "${version.name} - $userName ($timePrefix)"
            } else {
                "${version.name} ($timePrefix)"
            }
            handleSaveLocalSaveMeta(context, InitializePvz2.mSfmVersion.toString(), backupDir, defaultName, "检测到版本切换自动备份${InitializePvz2.mSfmVersion}版本存档")
        }
    }

    fun performCover(saveId: String) {
        lastCoverId.value = saveId
        val save = localSaves.find { it.id == saveId } ?: return
        val result = PvzSaveFileManager.coverGameSaveWithLocalSave(
            localSaveDir = File(save.savePath),
            gameSaveDir = gameSaveDir
        )
        operationState.postResult(result)
    }

    // ==========================
    // 弹窗
    // ==========================
    // 结果弹窗（原有逻辑保留）
    PvzSaveOperationResultDialog(
        operationState = operationState,
        onRetry = when (operationState.result?.type) {
            PvzSaveOperationType.DELETE if lastDeleteId.value == null -> null
            PvzSaveOperationType.COVER if lastCoverId.value == null -> null
            else -> { result ->
                when (result.type) {
                    PvzSaveOperationType.DELETE -> lastDeleteId.value?.let { performDelete(it) }
                    PvzSaveOperationType.EXPORT -> performExport()
                    PvzSaveOperationType.IMPORT -> performImport()
                    PvzSaveOperationType.BACKUP -> performBackup()
                    PvzSaveOperationType.COVER -> lastCoverId.value?.let { performCover(it) }
                    else -> operationState.reset()
                }
            }
        }
    )

    // 二次确认弹窗（原有逻辑保留）
    PvzConfirmDialog(
        isVisible = showConfirmDialog.value,
        title = confirmTitle.value,
        message = confirmMessage.value,
        onConfirm = onConfirmAction,
        onDismiss = { showConfirmDialog.value = false }
    )

    // ==========================
    // UI（原有逻辑保留）
    // ==========================

    PvzRichText(
        text = config.ui.log.localSaveLabel,
        defaultStyle = PvzTextRedStyle,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    if (localSaves.isNotEmpty()) {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            localSaves.forEach { entity ->
                LocalSaveRadioItem(
                    entity = entity,
                    selected = state.selectedLocalSaveId == entity.id,
                    onSelect = {
                        onStateChange(state.copy(selectedLocalSaveId = entity.id))
                    },
                    onDelete = {
                        showConfirmDialog(
                            title = saveConfig.deleteConfirmTitle, // 读取配置
                            message = String.format(saveConfig.deleteConfirmMessage, entity.name), // 替换占位符
                            onConfirm = { performDelete(entity.id) }
                        )
                    },
                    theme = theme
                )
            }
        }
    } else {
        Row(
            Modifier
                .padding(bottom = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PvzRichText(text = saveConfig.noLocalSaveTip, fontSize = 18.sp, defaultStyle = PvzTextGrayStyle)
        }
    }

    val isFileProviderAvailable = remember {
        mutableStateOf(PvzSaveFileManager.isFileProviderRegistered(context))
    }
    if (localSaves.isNotEmpty() && isFileProviderAvailable.value)
        PvzGreenButton(
            text = saveConfig.shareButton,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Pvz2Constants.Dimension.PADDING_SMALL.dp)
                .height(Pvz2Constants.Dimension.BUTTON_HEIGHT_SMALL.dp),
            icon = Icons.Default.Share,
            onClick = ::performShareAllSaves
        )

    PvzBlueButton(
        text = saveConfig.exportButton,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .height(Pvz2Constants.Dimension.BUTTON_HEIGHT_SMALL.dp),
        icon = Icons.Default.Upload,
        onClick = ::performExport
    )

    PvzOrangeButton(
        text = saveConfig.importButton,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .height(Pvz2Constants.Dimension.BUTTON_HEIGHT_SMALL.dp),
        icon = Icons.Default.Download,
        onClick = ::performImport
    )

    PvzPurpleButton(
        text = saveConfig.backupButton,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .height(Pvz2Constants.Dimension.BUTTON_HEIGHT_SMALL.dp),
        Icons.Default.Backup,
        onClick = ::performBackup
    )

    PvzRedButton(
        text = saveConfig.deleteGameSaveButton,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .height(Pvz2Constants.Dimension.BUTTON_HEIGHT_SMALL.dp),
        icon = Icons.Default.DeleteForever,
        onClick = {
            // 高风险操作必须弹出二次确认
            showConfirmDialog(
                title = saveConfig.deleteGameSaveConfirmTitle,
                message = saveConfig.deleteGameSaveConfirmMessage,
                onConfirm = {
                    val result = PvzSaveFileManager.deleteCurrentGameSave(gameSaveDir = gameSaveDir)
                    operationState.postResult(result)
                }
            )
        }
    )

    PvzRedButton(
        text = saveConfig.coverLocalButton,
        modifier = Modifier
            .fillMaxWidth()
            .height(Pvz2Constants.Dimension.BUTTON_HEIGHT_SMALL.dp),
        icon = Icons.Default.Save,
        onClick = {
            val selectedId = state.selectedLocalSaveId
            if (selectedId == null) {
                // 读取配置中的提示语
                operationState.postResult(PvzSaveOperationType.COVER failed saveConfig.selectLocalSaveTip)
                return@PvzRedButton
            }
            showConfirmDialog(
                title = saveConfig.coverConfirmTitle, // 读取配置
                message = saveConfig.coverConfirmMessage, // 读取配置
                onConfirm = { performCover(selectedId) }
            )
        }
    )
}

// 语法糖（原有逻辑保留）
private infix fun PvzSaveOperationType.failed(msg: String) =
    PvzSaveOperationResult(this, false, msg)

// ======================== 7. 动态栏目渲染组件（优化存档逻辑） ========================
private fun handleSaveLocalSaveMeta(
    context: Context,
    saveId: String,
    saveDir: File,
    name: String,
    desc: String
) {
    val saveEntity = PvzLocalSaveEntity(
        id = saveId,
        name = name,
        desc = desc,
        savePath = saveDir.absolutePath,
        createTime = System.currentTimeMillis().toString()
    )
    PvzLocalSaveManager.saveLocalSaveMeta(context, saveEntity)
}

@Composable
private fun DynamicSectionComponent(
    section: DynamicSection,
    state: DynamicSectionState,
    onStateChange: (DynamicSectionState) -> Unit,
    updateSectionState: (String, (DynamicSectionState) -> DynamicSectionState) -> Unit,
    extractorHolder: AssetExtractorHolder,
    filePickerManager: FilePickerManager?,
    version: VersionDef,
    sectionStates: Map<String, DynamicSectionState>
) {
    // 从栏目配置获取主题
    val theme = section.theme

    // 从单例取展开状态：跨 key(mPvz2MainScreenReloadKey) 重建后保持用户展开/折叠操作
    val expandedState = Pvz2MainScreenUiState.getExpandedState(section.id, section.isExpanded)

    PvzCollapsiblePanel(
        title = section.title,
        isExpandedInit = section.isExpanded,
        expandedState = expandedState,
        theme = theme
    ) {
        // =====================================================
        // 特殊处理：saves 栏 - 存档专属 UI
        // =====================================================
        if (section.id == "saves") {
            val context = LocalContext.current
            val gameSaveDir = section.resolveTargetDirectory()

            // 统一的弹窗状态类
            val saveInfoDialogState = remember { PvzSaveInfoDialogState() }
            PvzSaveInfoDialog(dialogState = saveInfoDialogState)

            // 预设存档区域
            if (section.items.isNotEmpty()) {
                PresetSaveSection(
                    section = section,
                    state = state,
                    onStateChange = onStateChange,
                    extractorHolder = extractorHolder,
                    gameSaveDir = gameSaveDir,
                    version = version
                )

                // 分割线
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    thickness = 1.dp,
                    color = Color(0xFFA1510D)
                )
            }

            // 本地存档区域
            LocalSaveSection(
                context = context,
                state = state,
                onStateChange = onStateChange,
                gameSaveDir = gameSaveDir,
                filePickerManager = filePickerManager,
                saveInfoDialogState = saveInfoDialogState,
                version = version,
                theme = theme
            )
        } else {
            section.items.forEach { item ->
                CompositionLocalProvider(LocalJsExecutionContext provides JsExecutionContext(
                    section = section,
                    item = item,
                    version = version,
                    sectionStates = sectionStates,
                    updateSectionState = updateSectionState
                )) {
                    when (item.type) {
                        SectionType.RADIO -> {
                            val isSelected = state.selectedPresetItemIds[item.groupId] == item.id
                            val buttonScope = rememberCoroutineScope()

                            val jsExtractor = rememberAssetExtractor(
                                context = LocalContext.current
                            )

                            val uiState by jsExtractor.extractor.uiState
                            PvzExtractorDialog(uiState = uiState,SettingsDialogState.isUseShowNotUpdate, onDismissRequest = { jsExtractor.extractor.dismiss() })

                            RadioButtonItem(
                                label = item.name,
                                subLabel = item.desc,
                                selected = isSelected,
                                leadingIconPath = item.icon,
                                theme = theme,
                                onSelect = {
                                    val newState = state.copy(
                                        selectedPresetItemIds = state.selectedPresetItemIds + (item.groupId to item.id)
                                    )

                                    onStateChange(newState)

                                    val jsScript = item.readJs(section,version)
                                    if (jsScript != null) {
                                        buttonScope.launch {
                                            PvzToolJsEngine.executeScript(
                                                extractor = jsExtractor,
                                                script = jsScript,
                                                section = section,
                                                item = item,
                                                version = version,
                                                sectionStates = sectionStates,
                                                updateSectionState = updateSectionState
                                            )
                                        }
                                    }
                                }
                            )
                        }

                        SectionType.CHECKBOX -> {
                            val isChecked = state.checkedItemIds.contains(item.id)
                            val checkboxScope = rememberCoroutineScope()

                            val jsExtractor = rememberAssetExtractor(
                                context = LocalContext.current
                            )

                            val uiState by jsExtractor.extractor.uiState
                            PvzExtractorDialog(uiState = uiState,SettingsDialogState.isUseShowNotUpdate, onDismissRequest = { jsExtractor.extractor.dismiss() })

                            SectionSwitchItem(
                                item = item,
                                checked = isChecked,
                                theme = theme,
                                onCheckedChange = { checked ->
                                    val newIds = if (checked) state.checkedItemIds + item.id else state.checkedItemIds - item.id
                                    val newState = state.copy(checkedItemIds = newIds)

                                    onStateChange(newState)
                                    Log.d("CheckboxSave", "功能[${item.name}]状态变更，立即部分保存")

                                    val jsScript = item.readJs(section,version)
                                    // 如果有 jsScript 或 jsPath，执行 JS 并传入选中状态
                                    if (jsScript != null) {
                                        checkboxScope.launch {
                                            PvzToolJsEngine.executeScript(
                                                extractor = jsExtractor,
                                                script = jsScript,
                                                section = section,
                                                item = item,
                                                version = version,
                                                sectionStates = sectionStates,
                                                updateSectionState = updateSectionState
                                            )
                                        }
                                    }
                                },
                            )
                        }

                        SectionType.DESCRIPTION -> {
                            // 读取 JS 脚本
                            val jsScript = item.readJs(section, version)

                            val currentDescriptionValue = state.descriptionValues[item.id] ?: item.desc.orEmpty().ifEmpty { item.name }

                            // 创建 extractor
                            val jsExtractor = rememberAssetExtractor(
                                context = LocalContext.current
                            )

                            val uiState by jsExtractor.extractor.uiState
                            PvzExtractorDialog(uiState = uiState, SettingsDialogState.isUseShowNotUpdate, onDismissRequest = { jsExtractor.extractor.dismiss() })

                            // 首次加载或重新加载时执行 JS
                            LaunchedEffect(item.id, version.id, jsScript) {
                                if (jsScript != null) {
                                    PvzToolJsEngine.executeScript(
                                        extractor = jsExtractor,
                                        script = jsScript,
                                        section = section,
                                        item = item,
                                        version = version,
                                        sectionStates = sectionStates,
                                        updateSectionState = updateSectionState
                                    )
                                }
                            }

                            if (!currentDescriptionValue.isNullOrBlank()) {
                                PvzRichText(
                                    text = currentDescriptionValue,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    defaultStyle = PvzTextStyle(section.theme.secondaryTextColor),
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }

                        SectionType.BUTTON -> {
                            val buttonScope = rememberCoroutineScope()

                            SectionButtonItem(
                                item = item,
                                section = section,
                                version = version,
                                sectionStates = sectionStates,
                                scope = buttonScope,
                                theme = theme,
                                updateSectionState = updateSectionState
                            )
                        }

                        SectionType.SLIDER -> {
                            val currentValue = state.sliderValues[item.id] ?: item.defaultValue ?: item.minValue
                            val displayValue = item.valueSuffix?.let { "${"%.2f".format(currentValue)}$it" } ?: currentValue.toString()
                            val sliderSteps = ((item.maxValue - item.minValue) / item.step).toInt() - 1
                            val sliderScope = rememberCoroutineScope()

                            // 从主题获取 Slider 颜色和文字颜色
                            val activeColor = section.theme.sliderActiveColor
                            val inactiveColor = section.theme.sliderInactiveColor
                            val primaryTextStyle = PvzTextStyle(section.theme.primaryTextColor)
                            val secondaryTextStyle = PvzTextStyle(section.theme.secondaryTextColor)

                            // 计算填充色的渐变版本（上亮下暗）
                            val activeGradientTop = activeColor.copy(
                                red = (activeColor.red * 1.2f).coerceAtMost(1f),
                                green = (activeColor.green * 1.2f).coerceAtMost(1f),
                                blue = (activeColor.blue * 1.2f).coerceAtMost(1f)
                            )
                            val activeGradientBottom = activeColor.copy(
                                red = (activeColor.red * 0.7f),
                                green = (activeColor.green * 0.7f),
                                blue = (activeColor.blue * 0.7f)
                            )

                            // 计算轨道背景的渐变版本（内凹感）
                            val inactiveGradientTop = inactiveColor.copy(
                                red = (inactiveColor.red * 1.2f).coerceAtMost(1f),
                                green = (inactiveColor.green * 1.2f).coerceAtMost(1f),
                                blue = (inactiveColor.blue * 1.2f).coerceAtMost(1f)
                            )
                            val inactiveGradientBottom = inactiveColor.copy(
                                red = (inactiveColor.red * 0.7f),
                                green = (inactiveColor.green * 0.7f),
                                blue = (inactiveColor.blue * 0.7f)
                            )

                            // 齿轮旋转角度 = 进度 * 360度
                            val rotationAngle = ((currentValue - item.minValue) / (item.maxValue - item.minValue)) * 360f

                            // 水平布局：标题(当前值) | Slider
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 左侧：标题+当前值
                                Column(
                                    modifier = Modifier.padding(end = 12.dp)
                                ) {
                                    item.name?.let {
                                        PvzRichText(
                                            text = "$it ($displayValue)",
                                            fontSize = 14.sp,
                                            lineHeight = 18.sp,
                                            defaultStyle = primaryTextStyle,
                                        )
                                    }
                                    item.desc?.let {
                                        PvzRichText(
                                            text = "[${"%.2f".format(item.minValue)}~${"%.2f".format(item.maxValue)}${item.valueSuffix ?: ""}] $it",
                                            fontSize = 11.sp,
                                            lineHeight = 14.sp,
                                            defaultStyle = secondaryTextStyle,
                                        )
                                    }
                                }

                                // 右侧：自定义 PVZ2 齿轮 Slider
                                val density = LocalDensity.current
                                BoxWithConstraints(
                                    modifier = Modifier.weight(1f).padding(end = 10.dp)
                                ) {
                                    // 轨道高度和齿轮大小
                                    val trackHeight = 20.dp
                                    val gearSize = 32.dp

                                    // 进度比例
                                    val progress = ((currentValue - item.minValue) / (item.maxValue - item.minValue)).coerceIn(0f, 1f)

                                    // 计算齿轮中心位置（齿轮中心正好在填充色/轨道色交界处）
                                    val trackWidthPx = constraints.maxWidth.toFloat()
                                    val gearCenterPx = progress * trackWidthPx
                                    val gearLeftOffset = with(density) { gearCenterPx.toDp() } - gearSize / 2

                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // 轨道容器（圆角胶囊形状）
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(trackHeight)
                                                .clip(RoundedCornerShape(trackHeight / 2))
                                        ) {
                                            // 轨道背景（内凹感）
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(
                                                                inactiveGradientTop,
                                                                inactiveColor,
                                                                inactiveGradientBottom
                                                            )
                                                        )
                                                    )
                                            )

                                            // 选中部分（圆柱立体感）
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(progress)
                                                    .fillMaxHeight()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(
                                                                activeGradientTop,
                                                                activeColor,
                                                                activeGradientBottom
                                                            )
                                                        )
                                                    )
                                            )
                                        }

                                        // 齿轮作为滑块（中心对准填充色/轨道色交界处）
                                        Box(
                                            modifier = Modifier
                                                .size(gearSize)
                                                .align(Alignment.CenterStart)
                                                .offset(x = gearLeftOffset)
                                        ) {
                                            Image(
                                                painter = rememberVectorPainter(Pvz2Icon.Gear),
                                                contentDescription = "Slider thumb",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .graphicsLayer {
                                                        rotationZ = rotationAngle
                                                    }
                                            )
                                        }

                                        val jsExtractor = rememberAssetExtractor(
                                            context = LocalContext.current
                                        )

                                        val uiState by jsExtractor.extractor.uiState
                                        PvzExtractorDialog(uiState = uiState,SettingsDialogState.isUseShowNotUpdate, onDismissRequest = { jsExtractor.extractor.dismiss() })

                                        // 完全透明的 Slider 用于处理交互
                                        Slider(
                                            value = currentValue,
                                            onValueChange = { newValue ->
                                                val clampedValue = (newValue / item.step).toInt() * item.step
                                                val finalValue = clampedValue.coerceIn(item.minValue, item.maxValue)
                                                val newState = state.copy(sliderValues = state.sliderValues + (item.id to finalValue))
                                                onStateChange(newState)
                                            },
                                            onValueChangeFinished = {
                                                val finalState = state.copy(sliderValues = state.sliderValues + (item.id to currentValue))
                                                onStateChange(finalState)

                                                val jsScript = item.readJs(section,version)
                                                // 如果有 jsScript 或 jsPath，拖动结束后执行
                                                if (jsScript != null) {
                                                    sliderScope.launch {
                                                        PvzToolJsEngine.executeScript(
                                                            extractor = jsExtractor,
                                                            script = jsScript,
                                                            section = section,
                                                            item = item,
                                                            version = version,
                                                            sectionStates = sectionStates,
                                                            updateSectionState = updateSectionState
                                                        )
                                                    }
                                                }
                                            },
                                            valueRange = item.minValue..item.maxValue,
                                            steps = sliderSteps.coerceAtLeast(0),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp),
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color.Transparent,
                                                activeTrackColor = Color.Transparent,
                                                inactiveTrackColor = Color.Transparent,
                                                activeTickColor = Color.Transparent,
                                                inactiveTickColor = Color.Transparent
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        SectionType.INPUT -> {
                            val currentValue = state.inputValues[item.id] ?: item.inputDefault ?: ""
                            val primaryTextStyle = PvzTextStyle(section.theme.primaryTextColor)
                            val secondaryTextStyle = PvzTextStyle(section.theme.secondaryTextColor)

                            Column(modifier = Modifier.fillMaxWidth()) {
                                // 标签
                                item.name?.let {
                                    PvzRichText(
                                        text = it,
                                        fontSize = 14.sp,
                                        lineHeight = 18.sp,
                                        defaultStyle = primaryTextStyle
                                    )
                                }
                                item.desc?.let {
                                    PvzRichText(
                                        text = it,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        defaultStyle = secondaryTextStyle,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }

                                // 输入框（使用主题的 sliderInactiveColor 作为背景和边框）
                                // 卡片内文字使用白色以保证对比度
                                PvzSimpleCardBrown(
                                    modifier = Modifier.fillMaxWidth(),
                                    borderColor = section.theme.sliderInactiveColor,
                                    backgroundColor = section.theme.sliderInactiveColor
                                ) {
                                    BasicTextField(
                                        value = currentValue,
                                        onValueChange = { newValue ->
                                            val newState = state.copy(inputValues = state.inputValues + (item.id to newValue))
                                            onStateChange(newState)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        textStyle = TextStyle(
                                            color = Color.White, // 深色背景上使用白色文字
                                            fontSize = 14.sp
                                        ),
                                        decorationBox = { innerTextField ->
                                            Box {
                                                if (currentValue.isEmpty()) {
                                                PvzRichText(
                                                        text = item.placeholder ?: "请输入...",
                                                        fontSize = 14.sp,
                                                        defaultStyle = PvzTextStyle(Color(0xCCFFFFFF)),
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        },
                                        singleLine = true
                                    )
                                }
                            }

                            // 输入完成时保存
                            LaunchedEffect(currentValue) {
                                onStateChange(state.copy(inputValues = state.inputValues + (item.id to currentValue)))
                            }
                        }

                        SectionType.INFO -> {
                            val primaryTextStyle = PvzTextStyle(section.theme.primaryTextColor)
                            val secondaryTextStyle = PvzTextStyle(section.theme.secondaryTextColor)
                            // 信息框使用浅色背景（sliderActiveColor），与编辑框的深色背景形成对比
                            val infoBackgroundColor = section.theme.sliderActiveColor
                            // 根据背景亮度自动选择文字颜色，保证对比度
                            val infoTextColor = getContrastingTextColor(infoBackgroundColor)

                            // 从 state 中读取 info 值，支持 JS 动态修改
                            val currentInfoValue = state.infoValues[item.id] ?: item.infoValue ?: "—"

                            val jsExtractor = rememberAssetExtractor(
                                context = LocalContext.current
                            )

                            val uiState by jsExtractor.extractor.uiState
                            PvzExtractorDialog(uiState = uiState, SettingsDialogState.isUseShowNotUpdate, onDismissRequest = { jsExtractor.extractor.dismiss() })

                            // 读取 JS 脚本
                            val jsScript = item.readJs(section, version)

                            // 首次加载或重新加载时执行 JS
                            LaunchedEffect(item.id, version.id, jsScript) {
                                if (jsScript != null) {
                                    PvzToolJsEngine.executeScript(
                                        extractor = jsExtractor,
                                        script = jsScript,
                                        section = section,
                                        item = item,
                                        version = version,
                                        sectionStates = sectionStates,
                                        updateSectionState = updateSectionState
                                    )
                                }
                            }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                // 标签
                                item.name?.let {
                                    PvzRichText(
                                        text = it,
                                        fontSize = 14.sp,
                                        lineHeight = 18.sp,
                                        defaultStyle = primaryTextStyle
                                    )
                                }
                                item.desc?.let {
                                    PvzRichText(
                                        text = it,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        defaultStyle = secondaryTextStyle,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }

                                // 信息值（使用浅色背景 sliderActiveColor，与编辑框的深色背景形成对比）
                                PvzSimpleCardGreen(
                                    modifier = Modifier.fillMaxWidth(),
                                    borderColor = infoBackgroundColor,
                                    backgroundColor = infoBackgroundColor
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PvzRichText(
                                            text = currentInfoValue,
                                            fontSize = 14.sp,
                                            lineHeight = 18.sp,
                                            defaultStyle = PvzTextStyle(infoTextColor),
                                            modifier = Modifier.weight(1f)
                                        )
                                        item.icon?.let { iconPath ->
                                            AsyncImageFromAssets(
                                                "images/$iconPath",
                                                contentDescription = item.name,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .padding(start = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // item 之间的分隔线（最后一个 item 不加分隔线）
                if (item != section.items.last()) {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        thickness = 0.5.dp,
                        color = theme.dividerColor.copy(alpha = theme.dividerAlpha)
                    )
                }
            }

            // 保存按钮（如果有配置）
            section.confirmButtonText?.let { btnText ->
                val confirmScope = rememberCoroutineScope()

                Spacer(modifier = Modifier.height(Pvz2Constants.Dimension.PADDING_SMALL.dp))
                PvzRedButton(
                    text = btnText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Pvz2Constants.Dimension.BUTTON_HEIGHT_MAIN.dp),
                    icon = Icons.Default.Save
                ) {
                    // 执行 section 级 JS（confirmButtonText 存在时由此处触发）
                    val sectionJs = section.readJs(version)
                    if (sectionJs != null) {
                        confirmScope.launch {
                            PvzToolJsEngine.executeScript(
                                script = sectionJs,
                                section = section,
                                item = null,
                                version = version,
                                sectionStates = sectionStates,
                                updateSectionState = updateSectionState
                            )
                        }
                    }

                    onStateChange(state)
                }
            }
        }
    }
}

// ======================== 8. 业务组件 ========================
@Composable
private fun CoreFunctionSection(
    onEnterGame: () -> Unit,
    onTutorial: () -> Unit,
    onResetData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = InitializePvz2.config

    PvzCardGreen(
        title = config.ui.title.coreFunction,
        modifier = modifier,
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            PvzPurpleButton(
                config.ui.button.enterGame,
                modifier = Modifier
                    .weight(1f)
                    .height(Pvz2Constants.Dimension.BUTTON_HEIGHT_MAIN.dp),
                if (config.ui.button.isEnterGameDefaultIcon) Icons.Default.PlayArrow else null
            ) { onEnterGame() }
            PvzRichText(config.ui.button.disconnectTheNetworkAndStart,modifier = Modifier.padding(horizontal = 8.dp), defaultStyle = PvzTextRedStyle)
            Image(
                imageVector = if (SettingsDialogState.isUseDisconnectTheNetworkAndStart) Pvz2Icon.HookSelect else Pvz2Icon.Hook,
                contentDescription = if (SettingsDialogState.isUseDisconnectTheNetworkAndStart) "已选中" else "未选中",
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = rememberSoundInteractionSource(
                            InitializePvz2.config.ui.sounds.switchClickPress,
                            InitializePvz2.config.ui.sounds.switchClickRelease
                        )
                    ) {
                        SoundController.playSoundFromAssets(InitializePvz2.config.ui.sounds.switchClick)
                        SettingsDialogState.toggleDisconnectTheNetworkAndStart()
                    }
            )
        }

        Spacer(modifier = Modifier.height(Pvz2Constants.Dimension.PADDING_SMALL.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            PvzPurpleButton(
                config.ui.button.tutorial,
                modifier = Modifier
                    .weight(1f)
                    .height(Pvz2Constants.Dimension.BUTTON_HEIGHT_SMALL.dp)
                    .padding(end = Pvz2Constants.Dimension.PADDING_SMALL.dp),
                if (config.ui.button.isTutorialDefaultIcon) Icons.AutoMirrored.Filled.LibraryBooks else null,
                onClick = onTutorial
            )
            PvzOrangeButton(
                config.ui.button.resetData,
                modifier = Modifier
                    .weight(1f)
                    .height(Pvz2Constants.Dimension.BUTTON_HEIGHT_SMALL.dp),
                if (config.ui.button.isResetDataDefaultIcon) Icons.Default.Archive else null,
                onClick = onResetData
            )
        }
    }
}

// ======================== JS 日志面板 ========================
@Composable
private fun JsLogPanel(
    modifier: Modifier = Modifier,
    theme: PvzCollapsiblePanelTheme = PvzCollapsiblePanelTheme.GREEN_BACKGROUND,
    maxHeight: Dp = 200.dp
) {
    val logs by JsLogger.logs.collectAsState()
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()

    // 每次有新日志自动滚到底部
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    PvzCollapsiblePanel(
        title = InitializePvz2.config.ui.log.panelTitle,
        modifier = modifier,
        isExpandedInit = true,
        theme = theme,
        headerTrailingContent = {
            if (logs.isNotEmpty()) {
                PvzRichText(
                    text = "${logs.size}",
                    fontSize = 10.sp,
                    defaultStyle = PvzTextStyle(Color.White.copy(alpha = 0.7f)),
                    modifier = Modifier.padding(end = 4.dp)
                )
                IconButton(
                    onClick = {
                        val text = logs.joinToString("\n") { "[${it.timestamp}] ${it.message}" }
                        scope.launch {
                            clipboardManager.setClipEntry(ClipEntry(ClipData.newPlainText("js_log", text)))
                        }
                    },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = InitializePvz2.config.ui.log.copyLogDesc,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp)
                    )
                }
                IconButton(
                    onClick = { JsLogger.clearLogs() },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = InitializePvz2.config.ui.log.clearLogDesc,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp, max = maxHeight)
        ) {
            if (logs.isEmpty()) {
                PvzRichText(
                    text = InitializePvz2.config.ui.log.noLogText,
                    fontSize = 11.sp,
                    defaultStyle = PvzTextStyle(theme.secondaryTextColor.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(8.dp)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(logs) { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            // 时间戳 — 跟随主题次要色
                            PvzRichText(
                                text = entry.timestamp,
                                fontSize = 9.sp,
                                defaultStyle = PvzTextStyle(theme.secondaryTextColor.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .width(42.dp),
                                maxLines = 1
                            )
                            // 日志内容 — ERROR/WARN/SUCCESS 保留语义色，INFO 跟随主题主色
                            PvzRichText(
                                text = entry.message,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                defaultStyle = PvzTextStyle(
                                    when (entry.level) {
                                        JsLogLevel.ERROR   -> Color(0xFFFF6B6B)
                                        JsLogLevel.WARN    -> Color(0xFFFFD93D)
                                        JsLogLevel.SUCCESS -> Color(0xFF6BCB77)
                                        JsLogLevel.INFO    -> theme.primaryTextColor
                                    }
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ======================== 修改：欢迎用户组件（添加高度动画） ========================
/**
 * 欢迎用户组件
 * 显示从存档读取的用户名，右侧有编辑按钮
 */
@Composable
private fun WelcomeUserSection(
    modifier: Modifier = Modifier,
    theme: PvzCollapsiblePanelTheme = PvzCollapsiblePanelTheme.BROWN
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 用户名状态
    var userName by remember { mutableStateOf<String?>(null) }

    // 编辑对话框状态
    var showEditDialog by remember { mutableStateOf(false) }

    // 刷新触发器（当存档变化时更新）
    val refreshTrigger = remember { mutableLongStateOf(0L) }

    // 监听存档变化
    DisposableEffect(context) {
        val gameSaveChangeListener = { _: Long ->
            refreshTrigger.longValue = System.currentTimeMillis()
        }

        // 注册两个监听
        PvzSaveFileManager.addGameSaveChangeListener(gameSaveChangeListener)

        onDispose {
            PvzSaveFileManager.removeGameSaveChangeListener(gameSaveChangeListener)
        }
    }

    // 从存档读取用户名
    LaunchedEffect(refreshTrigger.longValue) {
        scope.launch {
            userName = loadUserNameFromSave(context)
        }
    }

    // 编辑用户名对话框
    if (showEditDialog && userName != null) {
        EditUserNameDialog(
            currentUserName = userName!!,
            onDismiss = { showEditDialog = false },
            onConfirm = { newName ->
                scope.launch {
                    val success = saveUserNameToSave(context, newName)
                    if (success) {
                        // 刷新显示
                        refreshTrigger.longValue = System.currentTimeMillis()
                    }
                    showEditDialog = false
                }
            }
        )
    }

    // 使用 AnimatedVisibility 实现流畅的高度变化+内容动画
    AnimatedVisibility(
        visible = userName != null,
        modifier = modifier,
        // 入场：高度从0展开 + 内容从上方滑入 + 淡入
        enter = expandVertically(
            expandFrom = Alignment.Top,
            animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
        ) + slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
        ) + fadeIn(
            animationSpec = tween(durationMillis = 300)
        ),
        // 退场：高度收缩到0 + 内容向上滑出 + 淡出
        exit = shrinkVertically(
            shrinkTowards = Alignment.Top,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        ) + slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        ) + fadeOut(
            animationSpec = tween(durationMillis = 250)
        )
    ) {
        // 带背景的卡片
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF5E6D3),
                            Color(0xFFE8D4B8)
                        )
                    )
                )
                .then(
                    // 使用和其他卡片类似的边框
                    Modifier.topRoundedBorder(
                        width = 3.dp,
                        color = Color(0xFF96826A),
                        topCornerRadius = 8.dp
                    )
                )
                .then(
                    Modifier.bottomRoundedBorder(
                        width = 3.dp,
                        color = Color(0xFF96826A),
                        bottomCornerRadius = 8.dp
                    )
                )
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 欢迎语文本
                PvzRichText(
                    text = String.format(InitializePvz2.config.ui.welcome.greetingTemplate, userName),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    defaultStyle = PvzTextStyle(theme.primaryTextColor)
                )

                // 编辑按钮
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = InitializePvz2.config.ui.welcome.editUserNameTitle,
                    tint = theme.secondaryTextColor,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(
                            interactionSource = rememberSoundInteractionSource(
                                InitializePvz2.config.ui.sounds.buttonSettingsPress,
                                InitializePvz2.config.ui.sounds.buttonSettingsRelease
                            ),
                            indication = null
                        ) {
                            SoundController.playSoundFromAssets(InitializePvz2.config.ui.sounds.buttonSettingsPress)
                            showEditDialog = true
                        }
                )
            }
        }
    }
}

// ======================== 新增：编辑用户名对话框 ========================
/**
 * 编辑用户名对话框
 */
@Composable
private fun EditUserNameDialog(
    currentUserName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var inputValue by remember { mutableStateOf(currentUserName) }

    PvzStyledDialog(
        isVisible = true,
        titleText = InitializePvz2.config.ui.welcome.editUserNameTitle,
        onDismissRequest = onDismiss,
        dismissible = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        bottomContent = {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 取消按钮
                PvzRedButton(
                    text = InitializePvz2.config.ui.dialog.cancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    onClick = onDismiss
                )
                // 确认按钮
                PvzGreenButton(
                    text = InitializePvz2.config.ui.dialog.confirm,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    onClick = {
                        if (inputValue.isNotBlank()) {
                            onConfirm(inputValue)
                        }
                    }
                )
            }
        }
    ) {
        // 提示文本
        PvzRichText(
            InitializePvz2.config.ui.welcome.editUserNameHint,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 10.dp),
            defaultStyle = PvzTextOliveStyle.copy(shadowColor = null)
        )

        // 输入框
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    Brush.verticalGradient(colors = listOf(Color(0xFFFFFFFF), Color(0xFFF5F5F5))),
                    RoundedCornerShape(6.dp)
                )
                .border(2.dp, Color(0xFF8ED229), RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            BasicTextField(
                value = inputValue,
                onValueChange = { inputValue = it },
                textStyle = TextStyle(
                    color = Color(0xFF423F00),
                    fontSize = 14.sp
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

// ======================== 新增：保存用户名到存档 ========================
/**
 * 将新用户名保存到 pp.dat
 */
private suspend fun saveUserNameToSave(context: Context, newUserName: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            // 1. 获取游戏存档目录
            val config = InitializePvz2.config
            val saveSection = config.sections.find { it.id == "saves" } ?: return@withContext false
            val gameSaveDir = saveSection.resolveTargetDirectory()

            // 2. 检查 pp.dat 是否存在
            val ppDatFile = File(gameSaveDir, "pp.dat")
            if (!ppDatFile.exists() || ppDatFile.length() == 0L) {
                Log.e("EditUser", "pp.dat 不存在或为空")
                return@withContext false
            }

            // 3. 创建临时文件
            val tempJsonFile = File(context.cacheDir, "temp_edit_pp_${System.currentTimeMillis()}.json")
            val tempRtonFile = File(context.cacheDir, "temp_edit_pp_rton_${System.currentTimeMillis()}.dat")

            try {
                // 4. 解码 RTON -> JSON
                RTON.decodeAuto(ppDatFile.absolutePath, tempJsonFile.absolutePath)

                // 5. 解析并修改 JSON
                val jsonString = tempJsonFile.readText()
                val json = Json.parseToJsonElement(jsonString).jsonObject

                // 创建可变的 JSON 对象
                val mutableObjects = json.getValue("objects").jsonArray.toMutableList()
                val firstObject = mutableObjects[0].jsonObject
                val objData = firstObject.getValue("objdata").jsonObject

                // 修改用户名字段 "n"
                val mutableObjData = objData.toMutableMap().apply {
                    put("n", JsonPrimitive(newUserName))
                }

                // 重新构建完整的 JSON
                val modifiedFirstObject = firstObject.toMutableMap().apply {
                    put("objdata", JsonObject(mutableObjData))
                }
                mutableObjects[0] = JsonObject(modifiedFirstObject)

                val finalJson = JsonObject(json.toMutableMap().apply {
                    put("objects", JsonArray(mutableObjects))
                })

                // 6. 将修改后的 JSON 写回临时文件
                tempJsonFile.writeText(finalJson.toString())

                // 7. 编码 JSON -> RTON
                RTON.encodeAuto(tempJsonFile.absolutePath, tempRtonFile.absolutePath)

                // 8. 验证生成的文件
                if (!tempRtonFile.exists() || tempRtonFile.length() == 0L) {
                    Log.e("EditUser", "生成的 RTON 文件无效")
                    return@withContext false
                }

                // 9. 备份原文件（可选，安全起见）
                val backupFile = File(gameSaveDir, "pp.dat.backup_${System.currentTimeMillis()}")
                if (ppDatFile.exists()) {
                    ppDatFile.copyTo(backupFile, overwrite = true)
                }

                // 10. 用新文件覆盖原文件
                tempRtonFile.copyTo(ppDatFile, overwrite = true)

                // 11. 删除备份（如果成功）
                if (backupFile.exists()) {
                    backupFile.delete()
                }

                Log.d("EditUser", "用户名修改成功: $newUserName")
                true
            } catch (e: Exception) {
                Log.e("EditUser", "修改用户名失败", e)
                false
            } finally {
                // 清理临时文件
                if (tempJsonFile.exists()) tempJsonFile.delete()
                if (tempRtonFile.exists()) tempRtonFile.delete()
            }
        } catch (e: Exception) {
            Log.e("EditUser", "修改用户名异常", e)
            false
        }
    }
}

/**
 * 从当前游戏存档中读取用户名
 */
private suspend fun loadUserNameFromSave(context: Context): String? {
    return withContext(Dispatchers.IO) {
        try {
            // 1. 获取游戏存档目录
            val config = InitializePvz2.config
            val saveSection = config.sections.find { it.id == "saves" } ?: return@withContext null
            val gameSaveDir = saveSection.resolveTargetDirectory()

            // 2. 检查 pp.dat 是否存在
            val ppDatFile = File(gameSaveDir, "pp.dat")
            if (!ppDatFile.exists() || ppDatFile.length() == 0L) {
                return@withContext null
            }

            // 3. 创建临时文件用于存放解码后的 JSON
            val tempJsonFile = File(context.cacheDir, "temp_pp_${System.currentTimeMillis()}.json")

            try {
                // 4. 使用 RTON.decodeAuto 解码
                RTON.decodeAuto(ppDatFile.absolutePath, tempJsonFile.absolutePath)

                val json = Json.parseToJsonElement(tempJsonFile.readText())

                json.jsonObject
                    .getValue("objects")
                    .jsonArray[0]
                    .jsonObject
                    .getValue("objdata")
                    .jsonObject
                    .getValue("n")
                    .jsonPrimitive.content
            } catch (e: Exception) {
                Log.e("WelcomeUser", "读取存档用户名失败", e)
                null
            } finally {
                // 清理临时文件
                if (tempJsonFile.exists()) {
                    tempJsonFile.delete()
                }
            }
        } catch (e: Exception) {
            Log.e("WelcomeUser", "读取存档异常", e)
            null
        }
    }
}

@Composable
private fun AboutSection() {
    val config = InitializePvz2.config
    PvzCardNewYear(config.ui.title.about) {
        PvzRichText(config.ui.authorInfo)
        Spacer(modifier = Modifier.height(5.dp))
        PvzRichText("${config.ui.versionLabel}${config.ui.uiVersion}", fontSize = 14.sp, fontWeight = FontWeight.Medium, defaultStyle = PvzTextGrayStyle)
    }
}

// ======================== 9. 主界面（核心优化） ========================
@Composable
fun Pvz2MainScreen(
    onGotoGameClick: () -> Unit,
    onResetDataClick: () -> Unit,
    onCloseToolbox: () -> Unit,
    rootDirectory: File = InitializePvz2.context.getExternalFilesDir(null)!!.parentFile!!,
    onStateChanged: (Pvz2ScreenState) -> Unit,
    filePickerManager: FilePickerManager? = null // 传入提前注册的PickerManager
) {
    val config = InitializePvz2.config

    Pvz2ToolConfig.rootDirectory = rootDirectory

    val currentState by InitializePvz2.mPvz2ScreenStateFlow.collectAsState()

    var showTutorial by remember { mutableStateOf(false) }
    if (showTutorial) PvzTutorialDialog(onDismiss = { showTutorial = false })

    var showAnnouncement by remember { mutableStateOf(false) }
    if (showAnnouncement) PvzAnnouncementDialog(onDismiss = { showAnnouncement = false })

    JsAlertDialog()
    JsConfirmDialog()
    JsPromptDialog()
    JsProgressDialog()
    JsExtractorDialog()

    // 2. 显示设置弹窗（仅传状态）
    PvzSettingsDialog(filePickerManager = filePickerManager)

    if (SettingsDialogState.isUseDisconnectTheNetworkAndStart) {
        LocalVpnService.RequestPermissionsVpn({
            SettingsDialogState.toggleDisconnectTheNetworkAndStart()
            Toast.makeText(InitializePvz2.context, "未同意VPN权限，无法开启此功能.", Toast.LENGTH_SHORT).show()
        })
    }

    // 直接用单例里保存的偏移量作为初始值，首帧就渲染在正确位置，不会闪跳到顶部
    val scrollState = rememberScrollState(initial = Pvz2MainScreenUiState.scrollOffset)
    val scope = rememberCoroutineScope()

    LaunchedEffect(scrollState) {
        // 监听滚动值变化，不会触发重组
        snapshotFlow { scrollState.value }
            .collect { offset ->
                Pvz2MainScreenUiState.scrollOffset = offset
            }
    }

    // 版本状态管理（使用 LaunchedEffect 避免 remember(localScreenState) 引用变化导致重置）
    val defaultVersion = config.versions.find { it.default } ?: config.versions.first()
    val selectedVersion by remember(currentState) { mutableStateOf(currentState.selectedVersion ?: defaultVersion) }

    // 版本选择器临时状态
    var tempVersion by remember(selectedVersion) { mutableStateOf(selectedVersion) }

    // 动态栏目状态管理（版本隔离：每个版本的 section 状态独立存储）
    val sectionStates = remember(currentState) {
        // 1. 从持久化状态中只提取属于当前版本的状态
        val initMap = currentState.sectionStates.toMutableMap()

        config.sections.forEach { section ->
            if (!initMap.containsKey(section.id)) {
                // per-item 模式：收集所有类型的默认值
                // RADIO 默认值（按 groupId 分组）
                val defaultRadioItems = section.items.filter { it.type == SectionType.RADIO }
                val defaultRadioItemIds = defaultRadioItems
                    .groupBy { it.groupId }
                    .mapValues { (_, items) ->
                        items.find { it.default }?.id ?: items.firstOrNull()?.id
                    }
                    .filterValues { it != null }
                    .mapValues { it.value!! }

                val defaultCheckboxIds = section.items.filter { it.type == SectionType.CHECKBOX && it.default }.map { it.id }.toSet()

                // SLIDER 默认值
                val defaultSliderValues = section.items
                    .filter { it.type == SectionType.SLIDER }
                    .associate { it.id to (it.defaultValue ?: it.minValue) }

                // INPUT 默认值
                val defaultInputValues = section.items
                    .filter { it.type == SectionType.INPUT }
                    .associate { it.id to (it.inputDefault ?: "") }

                val defaultState = DynamicSectionState(
                    selectedPresetItemIds = defaultRadioItemIds,
                    selectedLocalSaveId = null,
                    checkedItemIds = defaultCheckboxIds,
                    sliderValues = defaultSliderValues,
                    inputValues = defaultInputValues
                )
                initMap[section.id] = defaultState
            }
        }
        mutableStateMapOf<String, DynamicSectionState>().apply { putAll(initMap) }
    }


    fun isSectionVisible(section: DynamicSection): Boolean {
        if (section.visibleOnVersionIds.isEmpty()) return true
        return section.visibleOnVersionIds.contains(selectedVersion.id)
    }

    val extractorHolder = rememberAssetExtractor(
        context = LocalContext.current
    )

    val uiState by extractorHolder.extractor.uiState
    PvzExtractorDialog(uiState = uiState,SettingsDialogState.isUseShowNotUpdate, onDismissRequest = { extractorHolder.extractor.dismiss() })

    // 辅助方法（版本隔离）
    fun updateSectionState(sectionId: String, updater: (DynamicSectionState) -> DynamicSectionState) {
        // 仅更新内存中的状态，不触发持久化
        sectionStates[sectionId] = updater(sectionStates[sectionId] ?: DynamicSectionState())
    }

    // 新增：全量保存状态（版本隔离）
    fun saveFullState() {
        // 将内存中的状态转换为版本化 key 后保存
        val fullState = Pvz2ScreenState(
            selectedVersion = selectedVersion,
            sectionStates = sectionStates
        )
        InitializePvz2.updateScreenState { fullState }
        onStateChanged(fullState)
        Log.d("FullSave", "进入游戏：全量保存所有状态")
    }

    CompositionLocalProvider(LocalJsExecutionContext provides JsExecutionContext(
        section = null,
        item = null,
        version = selectedVersion,
        sectionStates = sectionStates,
        updateSectionState = ::updateSectionState
    )) {
        // 背景与UI设置
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImageFromAssets(
                "images/${config.ui.assets.background}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            // ======================== 新增：悬浮面板状态 ========================
            val density = LocalDensity.current

            // 占位符的布局信息
            var placeholderWidth by remember { mutableIntStateOf(0) }
            // 占位符相对于【父级 Row】的 Y 坐标
            var placeholderYInRow by remember { mutableFloatStateOf(Float.POSITIVE_INFINITY) }
            // ======================== 关键修复 1：使用 derivedStateOf 处理频繁变化的滚动 ========================
            // 这样只有当 finalY 真正改变时，才会触发悬浮层的重绘
            val panelOffsetY by remember {
                derivedStateOf {
                    if (placeholderYInRow == Float.POSITIVE_INFINITY) return@derivedStateOf 0

                    // 原始位置 = 占位符在静止时的Y - 滚动偏移量
                    val rawCurrentY = placeholderYInRow - scrollState.value

                    // 挂起逻辑：不能小于 0 (因为 Scaffold 的 innerPadding 已经把 TopAppBar 的位置让出来了)
                    // 如果你想让它正好挂在 TopAppBar 下面，可以把 0f 换成 topBarHeightPx 相关的值
                    max(rawCurrentY, 0f).toInt()
                }
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp) // 建议只留左右padding，上下留给内容去居中
                            ) {
                                // 1. 加大比例系数
                                val availableHeight = maxHeight
                                val imageSize = availableHeight * 0.8f // 图片几乎占满高度
                                val textSize = with(density) { availableHeight.toSp() } * 0.4f // 文字也加大

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp), // 2. 图标之间增加固定间距
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    PvzRichText(
                                        config.ui.title.topAppBar,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = textSize,
                                        modifier = Modifier.weight(1f),
                                        defaultStyle = PvzTextWhiteStyle
                                    )

                                    // 图标现在会更大，且中间有间隔
                                    ImageSvgButton(
                                        imageVector = Pvz2Icon.Settings,
                                        imageVectorPress = Pvz2Icon.SettingsPress,
                                        contentDescription = "打开设置",
                                        modifier = Modifier.size(imageSize),
                                        // 【新增】传入按下的音效
                                        pressSound = InitializePvz2.config.ui.sounds.buttonSettingsPress,
                                        // 【新增】传入释放的音效
                                        releaseSound = InitializePvz2.config.ui.sounds.buttonSettingsRelease
                                    ) {
                                        SettingsDialogState.isShow = true
                                    }

                                    ImageSvgButton(
                                        Pvz2Icon.Announcement,
                                        Pvz2Icon.AnnouncementPress,
                                        "打开公告",
                                        Modifier.size(imageSize),
                                        // 【新增】传入按下的音效
                                        pressSound = InitializePvz2.config.ui.sounds.buttonSettingsPress,
                                        // 【新增】传入释放的音效
                                        releaseSound = InitializePvz2.config.ui.sounds.buttonSettingsRelease
                                    ) { showAnnouncement = true }

                                    ImageSvgButton(
                                        Pvz2Icon.Close,
                                        Pvz2Icon.ClosePress,
                                        "关闭工具箱",
                                        Modifier.size(imageSize * 0.9f),
                                        // 【新增】传入按下的音效
                                        pressSound = InitializePvz2.config.ui.sounds.buttonXClosePress,
                                        // 【新增】传入释放的音效
                                        releaseSound = InitializePvz2.config.ui.sounds.buttonXCloseRelease
                                    ) { onCloseToolbox() } // 这个保持稍微小一点点，或者也改成 imageSize

                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = Color.White
                        ),
                        modifier = Modifier
                            .height(Pvz2Constants.Dimension.TOP_BAR_HEIGHT.dp)
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
                },
                contentColor = Color.Transparent,
                containerColor = Color.Transparent,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (SettingsDialogState.isUseSolidColorBackground) Modifier.drawBehind {
                        drawRoundRect(
                            brush = Brush.verticalGradient(colors = listOf(Color(0xffEEE5C5), Color(0xffEEE5C5))),
                            cornerRadius = CornerRadius(0.dp.toPx(), 15.dp.toPx()),
                            size = size
                        )
                    } else Modifier)
                    .clip(RoundedCornerShape(bottomStart = 15.dp, bottomEnd = 15.dp))
                    .bottomRoundedBorder(width = 5.dp, color = Color(0xff96826A), bottomCornerRadius = 15.dp)
            ) { innerPadding ->
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    val maxHeight = maxHeight

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(Pvz2Constants.Dimension.PADDING_MEDIUM.dp)
                                .widthIn(max = 900.dp)
                                .align(Alignment.TopCenter),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            // 左侧
                            Column(
                                modifier = Modifier
                                    .weight(0.45f)
                                    .padding(end = 6.dp),
                                verticalArrangement = Arrangement.Top,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CoreFunctionSection(
                                    onEnterGame = {
                                        val resourcesToExtract = mutableListOf<ResourcePair>()

                                        val version = selectedVersion
                                        val targetDir = config.getSmfDirectoryFile()

                                        // 【修复】这里使用 version.forceOverride
                                        if (version.baseAssetPath != null) {
                                            resourcesToExtract.add(
                                                AssetExtractorHolder.resource(
                                                    internalPath = version.baseAssetPath,
                                                    targetDir = targetDir,
                                                    sectionName = "通用资源"
                                                )
                                            )
                                        }
                                        resourcesToExtract.add(
                                            AssetExtractorHolder.resource(
                                                internalPath = version.resolveAssetPath(),
                                                targetDir = targetDir,
                                                forceOverride = version.forceOverride,
                                                sectionName = "版本特有资源"
                                            )
                                        )
                                        // 注意：功能项解压已移除，仅保留版本级和BASE级解压
                                        fun runGotoGame() = scope.launch {
                                            // 1. section 级 JS（仅无 confirmButtonText 的栏目）
                                            config.sections
                                                .filter { isSectionVisible(it) && it.confirmButtonText == null }
                                                .forEach { section ->
                                                    val sectionJs = section.readJs(version)
                                                    if (sectionJs != null) {
                                                        try {
                                                            PvzToolJsEngine.executeScript(
                                                                script = sectionJs,
                                                                section = section,
                                                                item = null,
                                                                version = selectedVersion,
                                                                sectionStates = sectionStates,
                                                                updateSectionState = ::updateSectionState
                                                            )
                                                        } catch (e: Exception) {
                                                            JsConsole.error("section[${section.id}] JS 执行失败:",e)
                                                        }
                                                    }
                                                }
                                            // 2. 版本级 enterGameScript / enterGamePath
                                            val versionEnterScript = version.readJs()
                                            if (versionEnterScript != null) {
                                                try {
                                                    PvzToolJsEngine.executeScript(
                                                        script = versionEnterScript,
                                                        section = null,
                                                        item = null,
                                                        version = selectedVersion,
                                                        sectionStates = sectionStates,
                                                        updateSectionState = ::updateSectionState
                                                    )
                                                } catch (e: Exception) {
                                                    JsConsole.error("版本 enterGameScript 执行失败:",e)
                                                }
                                            }
                                            InitializePvz2.mSfmVersion = InitializePvz2.versionName
                                            // 关键：进入游戏时全量保存
                                            saveFullState()
                                            onGotoGameClick()
                                        }

                                        scope.launch {
                                            // 统一打包所有 SMF 修改
                                            PvzToolJsEngine.flushPendingSmfPacks()
                                            if (resourcesToExtract.isNotEmpty()) {
                                                extractorHolder.setOnDismissListener {
                                                    if (it.isComplete) runGotoGame()
                                                }
                                                extractorHolder.extract(*resourcesToExtract.toTypedArray())
                                            } else {
                                                runGotoGame()
                                            }
                                        }
                                    },
                                    onTutorial = { showTutorial = true },
                                    onResetData = { onResetDataClick() }
                                )
                                Spacer(modifier = Modifier.height(Pvz2Constants.Dimension.PADDING_MEDIUM.dp))
                                AboutSection()
                                Spacer(modifier = Modifier.height(Pvz2Constants.Dimension.PADDING_MEDIUM.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onPlaced { coordinates ->
                                            placeholderWidth = coordinates.size.width
                                            // 注意：这里获取的是相对于父 Row 的坐标
                                            placeholderYInRow = coordinates.positionInParent().y
                                        }
                                ) {}
                            }

                            // 右侧
                            Column(
                                modifier = Modifier
                                    .weight(0.55f)
                                    .padding(start = 6.dp),
                                verticalArrangement = Arrangement.Top
                            ) {
                                WelcomeUserSection()

                                val expandedState = Pvz2MainScreenUiState.getExpandedState("_versions", config.isExpandedVersions)

                                // 版本管理
                                PvzCollapsiblePanel(
                                    title = config.ui.title.versionManage,
                                    isExpandedInit = config.isExpandedVersions,
                                    theme = config.versionsTheme,
                                    expandedState = expandedState
                                ) {
                                    config.versions.forEach { version ->
                                        RadioButtonItem(
                                            label = version.name,
                                            subLabel = version.desc,
                                            selected = tempVersion == version,
                                            onSelect = { tempVersion = version },
                                            leadingIconPath = version.icon
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(Pvz2Constants.Dimension.PADDING_SMALL.dp))
                                    PvzRedButton(
                                        text = config.ui.button.confirmVersion,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(Pvz2Constants.Dimension.BUTTON_HEIGHT_MAIN.dp),
                                        icon = Icons.Default.Save
                                    ) {
                                        // 游戏存档版本隔离处理
                                        val currentVersion = selectedVersion
                                        val newVersion = tempVersion


                                        // 获取当前游戏存档目录（saves section 的目标目录）
                                        val saveSection = config.sections.find { it.id == "saves" }
                                        val gameSaveDir = saveSection?.resolveTargetDirectory()

                                        // 仅当版本真正改变时才处理存档隔离
                                        if (currentVersion.id != newVersion.id && gameSaveDir != null) {

                                            // 获取目标 SMF 目录（用于清理旧版本资源文件）
                                            val targetSmfDir = config.getSmfDirectoryFile()

                                            // 切换版本前，先清空当前版本的 SMF 修改（避免跨版本污染）
                                            PvzToolJsEngine.closePrepareSmf()

                                            // 执行版本存档隔离切换
                                            val (backupSuccess, hasNewSave) = PvzSaveFileManager.switchVersionSaveIsolation(
                                                currentGameSaveDir = gameSaveDir,
                                                currentVersion = currentVersion,
                                                newVersion = newVersion,
                                                targetSmfDir = targetSmfDir
                                            )

                                            Log.d("VersionSwitch", "版本切换为[${newVersion.name}]，存档隔离：备份${if (backupSuccess) "成功" else "失败"}，新版本${if (hasNewSave) "有" else "无"}存档")

                                            // 触发存档变化刷新（用户名等会重新加载）
                                            PvzSaveFileManager.notifyGameSaveChanged()
                                        }

                                        val fullState = Pvz2ScreenState(
                                            selectedVersion = tempVersion,
                                            sectionStates = emptyMap()
                                        )
                                        InitializePvz2.updateScreenState { fullState }
                                        onStateChanged(fullState)
                                        Log.d("VersionSwitch", "版本切换为[$tempVersion]")
                                    }
                                }

                                // 动态栏目列表
                                config.sections.forEach { section ->
                                    if (isSectionVisible(section)) {
                                        CompositionLocalProvider(LocalJsExecutionContext provides JsExecutionContext(
                                            section = section,
                                            item = null,
                                            version = selectedVersion,
                                            sectionStates = sectionStates,
                                            updateSectionState = ::updateSectionState
                                        )) {
                                            DynamicSectionComponent(
                                                section = section,
                                                state = sectionStates[section.id] ?: DynamicSectionState(),
                                                onStateChange = { newSectionState ->
                                                    updateSectionState(section.id) { newSectionState }
                                                    onStateChanged(currentState)
                                                },
                                                updateSectionState = ::updateSectionState,
                                                extractorHolder = extractorHolder,
                                                filePickerManager = filePickerManager,
                                                version = selectedVersion,
                                                sectionStates = sectionStates
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (placeholderYInRow != Float.POSITIVE_INFINITY) {

                        // ======================== 关键修复 2：结构对齐 ========================
                        // 这里复制一套和上面一模一样的 Row 结构，确保对齐
                        Row(
                            modifier = Modifier
                                .padding(Pvz2Constants.Dimension.PADDING_MEDIUM.dp)
                                .widthIn(max = 900.dp)
                                .align(Alignment.TopCenter),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(0.45f)
                                    .padding(end = 6.dp)
                                    .offset { IntOffset(0, panelOffsetY) },
                                contentAlignment = Alignment.TopCenter
                            ) {
                                // 这里才是真正的 JsLogPanel
                                JsLogPanel(maxHeight = maxHeight)
                            }

                            // 右侧占位，保持结构平衡
                            Spacer(modifier = Modifier.weight(0.55f))
                        }
                    }
                }
            }
        }
    }
}

// ======================== 10. 预览函数 ========================
@Preview(
    device = Devices.PHONE,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    widthDp = 900,
    heightDp = 600
)
@Composable
private fun Pvz2MainScreenPreview() {
    InitializePvz2.init(LocalContext.current)
    Pvz2ToolTheme {
        Pvz2MainScreen(
            rootDirectory = File(""),
            onGotoGameClick = { println("进入游戏") },
            onResetDataClick = { println("重置数据包") },
            onCloseToolbox = {},
            onStateChanged = { println("状态变更") }
        )
    }
}
