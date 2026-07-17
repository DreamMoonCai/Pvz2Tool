package io.github.dreammooncai.pvz2tool.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.controller.SoundController
import io.github.dreammooncai.pvz2tool.icon.Hook
import io.github.dreammooncai.pvz2tool.icon.HookSelect
import io.github.dreammooncai.pvz2tool.icon.Pvz2Icon
import io.github.dreammooncai.pvz2tool.rememberSoundInteractionSource
import io.github.dreammooncai.pvz2tool.view.AsyncImageFromAssets
import io.github.dreammooncai.pvz2tool.view.PvzCollapsiblePanelTheme
import io.github.dreammooncai.pvz2tool.view.PvzGreenButton
import io.github.dreammooncai.pvz2tool.view.PvzProgressBar
import io.github.dreammooncai.pvz2tool.view.PvzRedButton
import io.github.dreammooncai.pvz2tool.view.PvzRichText
import io.github.dreammooncai.pvz2tool.view.PvzSimpleCardBrown
import io.github.dreammooncai.pvz2tool.view.PvzTextOliveStyle
import io.github.dreammooncai.pvz2tool.view.PvzTextStyle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ======================== UI 状态类 ========================

/** 确认弹窗状态 */
data class JsConfirmState(
    val isVisible: Boolean = false,
    val title: String = "",
    val message: String = "",
    val deferred: CompletableDeferred<Boolean>? = null
)

/** 输入弹窗状态 */
data class JsPromptState(
    val isVisible: Boolean = false,
    val title: String = "",
    val message: String = "",
    val defaultValue: String = "",
    val placeholder: String = "",
    val deferred: CompletableDeferred<String?>? = null
)

/** 进度弹窗状态 */
data class JsProgressState(
    val isVisible: Boolean = false,
    val title: String = "",
    val message: String = "",
    val progress: Int = 0,
    val isIndeterminate: Boolean = false,
    val showCancel: Boolean = true,
    val isCancelled: Boolean = false
)

/** 解压根弹窗状态（复用 ExtractorUiState） */
typealias JsExtractorState = ExtractorUiState

/** 提示弹窗状态（单按钮弹窗） */
data class JsAlertState(
    val isVisible: Boolean = false,
    val title: String = "",
    val message: String = "",
    val deferred: CompletableDeferred<Unit>? = null
)

/** 选择弹窗中的单个条目 */
data class JsChoiceItem(
    val name: String = "",
    val icon: String = "",
    val value: String = "",
    val showIndex: Boolean? = null // 单独控制本项是否显示序号；null 时跟随外层 options.showIndex
)

/** 选择弹窗状态（单项 / 多项通用） */
data class JsSelectState(
    val isVisible: Boolean = false,
    val title: String = "",
    val items: List<JsChoiceItem> = emptyList(),
    val mode: String = "single", // "single" | "multi"
    val columns: Int = 4,
    val cancelable: Boolean = false,
    val defaultIndices: List<Int> = emptyList(),
    val showIndex: Boolean = false, // 是否在图标上居中叠加序号（1 开始）
    val deferredSingle: CompletableDeferred<String?>? = null,
    val deferredMulti: CompletableDeferred<List<String>>? = null
)

// ======================== JS UI 管理器 ========================

object JsUiManager {
    // 确认弹窗状态流
    private val _confirmState = MutableStateFlow(JsConfirmState())
    val confirmState: StateFlow<JsConfirmState> = _confirmState.asStateFlow()

    // 输入弹窗状态流
    private val _promptState = MutableStateFlow(JsPromptState())
    val promptState: StateFlow<JsPromptState> = _promptState.asStateFlow()

    // 进度弹窗状态流
    private val _progressState = MutableStateFlow(JsProgressState())
    val progressState: StateFlow<JsProgressState> = _progressState.asStateFlow()

    // 进度取消相关：JS 通过 options.onCancel 注册的回调，点击取消按钮时触发
    private var progressCancelHandler: (suspend () -> Unit)? = null

    // 解压根弹窗状态流
    private val _extractorState = MutableStateFlow(ExtractorUiState())
    val extractorState: StateFlow<ExtractorUiState> = _extractorState.asStateFlow()

    // 提示弹窗状态流
    private val _alertState = MutableStateFlow(JsAlertState())
    val alertState: StateFlow<JsAlertState> = _alertState.asStateFlow()

    // 解压器实例（由 JS 调用时创建）
    private var extractorHolder: AssetExtractorHolder? = null
    private val extractorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** 显示确认弹窗，返回 CompletableDeferred<Boolean> */
    fun showConfirm(title: String, message: String): CompletableDeferred<Boolean> {
        val deferred = CompletableDeferred<Boolean>()
        _confirmState.value = JsConfirmState(
            isVisible = true, title = title, message = message, deferred = deferred
        )
        return deferred
    }

    /** 隐藏确认弹窗 */
    fun hideConfirm() {
        _confirmState.value = JsConfirmState()
    }

    /** 显示提示弹窗（单按钮），返回 CompletableDeferred<Unit> */
    fun showAlert(title: String, message: String): CompletableDeferred<Unit> {
        val deferred = CompletableDeferred<Unit>()
        _alertState.value = JsAlertState(
            isVisible = true, title = title, message = message, deferred = deferred
        )
        return deferred
    }

    /** 隐藏提示弹窗 */
    fun hideAlert() {
        _alertState.value = JsAlertState()
    }

    // 选择弹窗状态流（单项/多项通用）
    private val _selectState = MutableStateFlow(JsSelectState())
    val selectState: StateFlow<JsSelectState> = _selectState.asStateFlow()

    /**
     * 显示单项选择弹窗，返回 CompletableDeferred<String?>（选中项 value，取消返回 null）
     * @param items 待选项
     * @param defaultValue 默认选中项的 name 或 value
     * @param columns 网格模式每排列数（仅条目 >= 8 且有图标时生效）
     */
    fun showSelect(
        title: String,
        items: List<JsChoiceItem>,
        columns: Int = 4,
        cancelable: Boolean = false,
        showIndex: Boolean = false
    ): CompletableDeferred<String?> {
        val deferred = CompletableDeferred<String?>()
        _selectState.value = JsSelectState(
            isVisible = true,
            title = title,
            items = items,
            mode = "single",
            columns = columns,
            cancelable = cancelable,
            showIndex = showIndex,
            deferredSingle = deferred
        )
        return deferred
    }

    /**
     * 显示多项选择弹窗，返回 CompletableDeferred<List<String>>（选中项 value 列表）
     * @param defaultValues 默认选中项的 name 或 value 列表
     */
    fun showMultiSelect(
        title: String,
        items: List<JsChoiceItem>,
        defaultValues: List<String>? = null,
        columns: Int = 4,
        cancelable: Boolean = false,
        showIndex: Boolean = false
    ): CompletableDeferred<List<String>> {
        val deferred = CompletableDeferred<List<String>>()
        val defaults = defaultValues ?: emptyList()
        val defaultIndices = items.mapIndexedNotNull { i, it ->
            if (it.value in defaults || it.name in defaults) i else null
        }
        _selectState.value = JsSelectState(
            isVisible = true,
            title = title,
            items = items,
            mode = "multi",
            columns = columns,
            cancelable = cancelable,
            defaultIndices = defaultIndices,
            showIndex = showIndex,
            deferredMulti = deferred
        )
        return deferred
    }

    /** 隐藏选择弹窗 */
    fun hideSelect() {
        _selectState.value = JsSelectState()
    }

    /** 显示输入弹窗，返回 CompletableDeferred<String?> */
    fun showPrompt(
        title: String, message: String, defaultValue: String = "", placeholder: String = ""
    ): CompletableDeferred<String?> {
        val deferred = CompletableDeferred<String?>()
        _promptState.value = JsPromptState(
            isVisible = true,
            title = title,
            message = message,
            defaultValue = defaultValue,
            placeholder = placeholder,
            deferred = deferred
        )
        return deferred
    }

    /** 隐藏输入弹窗 */
    fun hidePrompt() {
        _promptState.value = JsPromptState()
    }

    /** 显示进度弹窗 */
    fun showProgress(
        title: String, message: String = "", isIndeterminate: Boolean = false, showCancel: Boolean = false
    ) { // 每次显示都重置取消状态与回调（新的进度会话）
        progressCancelHandler = null
        _progressState.value = JsProgressState(
            isVisible = true,
            title = title,
            message = message,
            progress = 0,
            isIndeterminate = isIndeterminate,
            showCancel = showCancel,
            isCancelled = false
        )
    }

    /** 注册进度取消回调（由 JS 的 options.onCancel 提供） */
    fun setProgressCancelHandler(handler: (suspend () -> Unit)?) {
        progressCancelHandler = handler
    }

    /** 关闭进度弹窗（不影响取消标记，供正常完成使用） */
    fun closeProgress() {
        _progressState.value = _progressState.value.copy(isVisible = false)
    }

    /** 是否已取消（供 JS 轮询 controller.isCancelled()） */
    fun isProgressCancelled(): Boolean = _progressState.value.isCancelled

    /**
     * 触发取消：标记已取消、隐藏弹窗，并执行 JS 注册的 onCancel 回调。
     * 由进度弹窗的“取消”按钮调用。
     */
    suspend fun cancelProgress() {
        if (_progressState.value.isCancelled) return
        _progressState.value = _progressState.value.copy(isCancelled = true, isVisible = false)
        runCatching { progressCancelHandler?.invoke() }
        progressCancelHandler = null
    }

    /** 更新进度弹窗 */
    fun updateProgress(message: String? = null, progress: Float? = null) {
        _progressState.value =
            _progressState.value.copy(message = message ?: _progressState.value.message, progress = progress?.let {
                (if (progress > 1f) progress
                else (it * 100)).toInt().coerceIn(0, 100)
            } ?: _progressState.value.progress)
    }

    // ======================== 解压相关方法 ========================

    /**
     * 解压资源到目标目录
     * @param sourcePaths 资源路径列表
     * @param targetDir 目标目录（文件路径）
     * @param sectionName 功能栏名称（用于 UI 显示）
     * @return CompletableDeferred<ExtractorUiState>
     */
    fun extract(
        sourcePaths: List<String>, targetDir: String, sectionName: String = ""
    ): CompletableDeferred<ExtractorUiState> {
        val deferred = CompletableDeferred<ExtractorUiState>()

        // 创建或重用 extractor
        val holder = extractorHolder ?: AssetExtractorHolder(
            AssetResourceExtractor(
                context = InitializePvz2.context, scope = extractorScope
            )
        ).also { extractorHolder = it }

        // 监听 extractor 状态
        holder.extractor.uiState.value = holder.extractor.uiState.value.copy(isVisible = true)

        holder.setOnCompleteListener { state ->
            _extractorState.value = state
            if (!deferred.isCompleted) {
                deferred.complete(state)
            }
        }
        holder.setOnDismissListener { state ->
            _extractorState.value = state
            if (!deferred.isCompleted) {
                deferred.complete(state)
            }
        }

        // 构建 ResourcePair 列表
        val resourcePairs = sourcePaths.mapNotNull { path ->
            try {
                val targetFile = java.io.File(targetDir)
                AssetExtractorHolder.resource(path, targetFile, sectionName = sectionName)
            } catch (e: Exception) {
                null
            }
        }

        if (resourcePairs.isNotEmpty()) {
            holder.extract(*resourcePairs.toTypedArray())
        } else {
            val emptyState = ExtractorUiState(isVisible = false, isComplete = true)
            _extractorState.value = emptyState
            if (!deferred.isCompleted) {
                deferred.complete(emptyState)
            }
        }

        return deferred
    }

    /**
     * 获取解压 UI 状态
     */
    fun getExtractorState(): ExtractorUiState = _extractorState.value

    /**
     * 关闭解压弹窗
     */
    fun closeExtractor() {
        extractorHolder?.extractor?.dismiss()
        _extractorState.value = ExtractorUiState()
    }
}

// ======================== Compose 弹窗组件 ========================

/**
 * JS 提示弹窗（单按钮）
 * 用法: ui.alert("标题", "内容")
 */
@Composable
fun JsAlertDialog() {
    val state by JsUiManager.alertState.collectAsState()

    // 监听 deferred 完成状态，自动隐藏弹窗
    LaunchedEffect(state.deferred) {
        state.deferred?.let { deferred ->
            deferred.invokeOnCompletion {
                JsUiManager.hideAlert()
            }
        }
    }

    if (state.isVisible) {
        PvzStyledDialog(
            isVisible = true,
            titleText = state.title,
            onDismissRequest = {
                state.deferred?.complete(Unit)
            },
            dismissible = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            bottomContent = {
                Spacer(modifier = Modifier.height(16.dp))

                // 确认按钮
                PvzGreenButton(
                    text = "确定", modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp), onClick = {
                        state.deferred?.complete(Unit)
                    })
            }) {
            PvzRichText(
                state.message,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 10.dp),
                defaultStyle = PvzTextOliveStyle.copy(shadowColor = null)
            )
        }
    }
}

/**
 * JS 确认弹窗
 * 用法: ui.confirm("标题", "内容").then(ok => { ... })
 */
@Composable
fun JsConfirmDialog() {
    val state by JsUiManager.confirmState.collectAsState()

    // 监听 deferred 完成状态，自动隐藏弹窗
    LaunchedEffect(state.deferred) {
        state.deferred?.let { deferred ->
            deferred.invokeOnCompletion {
                JsUiManager.hideConfirm()
            }
        }
    }

    if (state.isVisible) {
        PvzStyledDialog(
            isVisible = true,
            titleText = state.title,
            onDismissRequest = {
                state.deferred?.complete(false)
            },
            dismissible = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            bottomContent = {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) { // 取消按钮
                    PvzRedButton(
                        text = "取消", modifier = Modifier
                            .weight(1f)
                            .height(48.dp), onClick = {
                            state.deferred?.complete(false)
                        }) // 确认按钮
                    PvzGreenButton(
                        text = "确认", modifier = Modifier
                            .weight(1f)
                            .height(48.dp), onClick = {
                            state.deferred?.complete(true)
                        })
                }
            }) {
            PvzRichText(
                state.message,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 10.dp),
                defaultStyle = PvzTextOliveStyle.copy(shadowColor = null)
            )
        }
    }
}

/**
 * JS 输入弹窗
 * 用法: ui.prompt("标题", "请输入内容", "默认值").then(value => { ... })
 */
@Composable
fun JsPromptDialog() {
    val state by JsUiManager.promptState.collectAsState()
    var inputValue by remember { mutableStateOf(state.defaultValue) }

    // 监听 deferred 完成状态，自动隐藏弹窗
    LaunchedEffect(state.deferred) {
        state.deferred?.let { deferred ->
            deferred.invokeOnCompletion {
                JsUiManager.hidePrompt()
            }
        }
    }

    // 当弹窗打开时，用 defaultValue 初始化
    LaunchedEffect(state.isVisible, state.defaultValue) {
        if (state.isVisible) {
            inputValue = state.defaultValue
        }
    }

    if (state.isVisible) {
        PvzStyledDialog(
            isVisible = true,
            titleText = state.title,
            onDismissRequest = {
                state.deferred?.complete(null)
            },
            dismissible = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            bottomContent = {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) { // 取消按钮
                    PvzRedButton(
                        text = "取消", modifier = Modifier
                            .weight(1f)
                            .height(48.dp), onClick = {
                            state.deferred?.complete(null)
                        }) // 确认按钮
                    PvzGreenButton(
                        text = "确定", modifier = Modifier
                            .weight(1f)
                            .height(48.dp), onClick = {
                            state.deferred?.complete(inputValue)
                        })
                }
            }) { // 提示文本
            if (state.message.isNotEmpty()) {
                PvzRichText(
                    state.message,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 10.dp),
                    defaultStyle = PvzTextOliveStyle.copy(shadowColor = null)
                )
            }

            // 输入框
            PvzSimpleCardBrown(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                borderColor = PvzCollapsiblePanelTheme.GREEN.sliderInactiveColor,
                backgroundColor = PvzCollapsiblePanelTheme.GREEN.sliderInactiveColor
            ) {
                BasicTextField(
                    value = inputValue,
                    onValueChange = { newValue -> inputValue = newValue },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 14.sp),
                    decorationBox = { innerTextField ->
                        Box {
                            if (inputValue.isEmpty()) {
                                PvzRichText(
                                    text = state.placeholder.ifEmpty { "请输入..." },
                                    fontSize = 14.sp,
                                    defaultStyle = PvzTextStyle(Color(0xCCFFFFFF)),
                                )
                            }
                            innerTextField()
                        }
                    },
                    singleLine = false
                )
            }
        }
    }
}

/**
 * JS 单项 / 多项选择弹窗
 *
 * 布局规则（参考 SectionType.RADIO 样式）：
 * - 任意条目带图标且总数 >= 8 → 网格模式，每排若干，单条目 = 图标(或占位矩形) + 底部文字
 * - 任意条目带图标且总数 < 8 → 列表模式，每条目独占一行，图标(或占位矩形)在文字前
 * - 所有条目均无图标 → 纯文字模式，每条目前带选择标记(单选=圆点/多选=勾)，独占一行
 *
 * 无图标条目：用与图标同尺寸的矩形，内部居中显示与底部相同的文字，超出截断以保持观感。
 *
 * 用法:
 *   ui.select("选择关卡", [{name:"1",icon:"lv1.png",value:"1"}, ...], {defaultValue:"1"})
 *   ui.multiSelect("多选", items, {defaultValues:["a","b"]})
 */
@Composable
fun JsItemChoiceDialog() {
    val state by JsUiManager.selectState.collectAsState()

    val hasAnyIcon = state.items.any { it.icon.isNotBlank() }
    val isGrid = hasAnyIcon && state.items.size >= 8

    var selectedIndex by remember { mutableIntStateOf(-1) }
    var selectedIndices by remember { mutableStateOf(setOf<Int>()) }

    // 打开新弹窗时重置选中状态
    LaunchedEffect(state.deferredSingle, state.deferredMulti) {
        selectedIndex = -1
        selectedIndices = state.defaultIndices.toSet()
    } // 完成时自动隐藏弹窗
    LaunchedEffect(state.deferredSingle, state.deferredMulti) {
        state.deferredSingle?.invokeOnCompletion { JsUiManager.hideSelect() }
        state.deferredMulti?.invokeOnCompletion { JsUiManager.hideSelect() }
    }

    // 统一选择处理：单项立即返回，多项切换选中集合
    val onSelect: (Int) -> Unit = { i ->
        if (state.mode == "single") {
            state.deferredSingle?.complete(state.items[i].value)
        } else {
            selectedIndices = if (i in selectedIndices) selectedIndices - i else selectedIndices + i
        }
    }

    if (state.isVisible) {
        PvzStyledDialog(
            isVisible = true,
            titleText = state.title,
            onDismissRequest = {
                state.deferredSingle?.complete(null)
                state.deferredMulti?.complete(emptyList())
            },
            dismissible = state.cancelable,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            bottomContent = {
                Spacer(modifier = Modifier.height(16.dp))
                if (state.mode == "multi") {
                    Row(
                        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PvzRedButton(
                            text = "取消", modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) { state.deferredMulti?.complete(emptyList()) }
                        PvzGreenButton(
                            text = "确定", modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            val vals = selectedIndices.sorted().map { state.items[it].value }
                            state.deferredMulti?.complete(vals)
                        }
                    }
                } else {
                    PvzRedButton(
                        text = "取消", modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) { state.deferredSingle?.complete(null) }
                }
            }) {
            if (isGrid) {
                val columns = state.columns.coerceIn(2, 6)
                val indexed = state.items.mapIndexed { i, item -> i to item }
                val rows = indexed.chunked(columns)
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()
                ) {
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            row.forEach { (i, item) ->
                                GridChoiceCell(
                                    item = item,
                                    index = i,
                                    selected = if (state.mode == "multi") i in selectedIndices else selectedIndex == i,
                                    isMulti = state.mode == "multi",
                                    showIndex = state.showIndex,
                                    onSelect = onSelect
                                )
                            }
                        }
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()
                ) {
                    state.items.forEachIndexed { i, item ->
                        ListChoiceRow(
                            item = item,
                            index = i,
                            hasAnyIcon = hasAnyIcon,
                            selected = if (state.mode == "multi") i in selectedIndices else selectedIndex == i,
                            isMulti = state.mode == "multi",
                            showIndex = state.showIndex,
                            onSelect = onSelect
                        )
                    }
                }
            }
        }
    }
}

/** 网格单元格：图标(或占位矩形)在上，文字在下，整体居中 */
@Composable
private fun RowScope.GridChoiceCell(
    item: JsChoiceItem, index: Int, selected: Boolean, isMulti: Boolean, showIndex: Boolean, onSelect: (Int) -> Unit
) {
    val interaction = rememberSoundInteractionSource(
        InitializePvz2.config.ui.sounds.switchClickPress, InitializePvz2.config.ui.sounds.switchClickRelease
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(interactionSource = interaction, indication = null) {
                SoundController.playSoundFromAssets(InitializePvz2.config.ui.sounds.switchClick)
                onSelect(index)
            }, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            ItemIconOrPlaceholder(item, 48.dp, index, item.showIndex ?: showIndex)
            if (isMulti && selected) {
                Image(
                    imageVector = Pvz2Icon.HookSelect,
                    contentDescription = "已选中",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        PvzRichText(
            item.name, fontSize = 11.sp, maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
    }
}

/** 列表行：每条目独占一行；有图标时图标(或占位矩形)在文字前，无图标时前带选择标记 */
@Composable
private fun ListChoiceRow(
    item: JsChoiceItem,
    index: Int,
    hasAnyIcon: Boolean,
    selected: Boolean,
    isMulti: Boolean,
    showIndex: Boolean,
    onSelect: (Int) -> Unit
) {
    val interaction = rememberSoundInteractionSource(
        InitializePvz2.config.ui.sounds.switchClickPress, InitializePvz2.config.ui.sounds.switchClickRelease
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp, horizontal = 20.dp)
            .clickable(interactionSource = interaction, indication = null) {
                SoundController.playSoundFromAssets(InitializePvz2.config.ui.sounds.switchClick)
                onSelect(index)
            }, verticalAlignment = Alignment.CenterVertically
    ) {
        if (!hasAnyIcon) { // 纯文字模式：前面带选择标记（单选=圆点，多选=勾）
            Image(
                imageVector = if (selected) Pvz2Icon.HookSelect else Pvz2Icon.Hook,
                contentDescription = if (selected) "已选中" else "未选中",
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            PvzRichText(
                item.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f)
            )
        } else {
            ItemIconOrPlaceholder(item, 36.dp, index, item.showIndex ?: showIndex)
            Spacer(modifier = Modifier.width(10.dp))
            PvzRichText(
                item.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f)
            )
            Image(
                imageVector = if (selected) Pvz2Icon.HookSelect else Pvz2Icon.Hook,
                contentDescription = if (selected) "已选中" else "未选中",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** 图标优先；无图标时渲染与图标同尺寸的占位矩形，内部居中文字（超出截断）。showIndex 时在图标上居中叠加序号(1 开始) */
@Composable
private fun ItemIconOrPlaceholder(item: JsChoiceItem, size: Dp, index: Int = -1, showIndex: Boolean = false) {
    val iconPath = item.icon.takeIf { it.isNotBlank() }?.let { p ->
        if (p.startsWith("/")) p else "images/$p"
    }
    val content: @Composable () -> Unit = {
        if (iconPath != null) {
            AsyncImageFromAssets(
                iconPath,
                modifier = Modifier.size(size),
                contentScale = ContentScale.Fit,
                contentDescription = item.name
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .background(Color(0x33999999), RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0x66999999), RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center
            ) {
                Text(
                    item.name,
                    fontSize = if (size > 36.dp) 10.sp else 12.sp,
                    color = Color(0xFF5a4a1a),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(2.dp)
                )
            }
        }
    }
    if (showIndex && index >= 0 && iconPath != null) { // 仅在「有图标」时居中叠加序号（不遮盖原图，便于图标本身即设计来放数字）
        Box(
            modifier = Modifier.size(size), contentAlignment = Alignment.Center
        ) {
            content()
            Text(
                text = (index + 1).toString(),
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = if (size > 36.dp) 18.sp else 14.sp,
                textAlign = TextAlign.Center
            )
        }
    } else {
        content()
    }
}

/**
 * JS 进度弹窗
 * 用法:
 *   const progress = ui.progress("正在处理...");
 *   progress.update("已完成 50%", 50);
 *   progress.close();
 */
@Composable
fun JsProgressDialog() {
    val state by JsUiManager.progressState.collectAsState()

    if (state.isVisible) {
        val progress = (state.progress / 100f).coerceIn(0f, 1f)
        val scope = rememberCoroutineScope()
        PvzStyledDialog(
            isVisible = true,
            titleText = state.title,
            onDismissRequest = {
                JsUiManager.closeProgress()
            },
            dismissible = progress >= 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            bottomContent = {
                Spacer(modifier = Modifier.height(10.dp))

                // 进度条
                if (state.isIndeterminate) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp), color = Color(0xFF32CD32), strokeWidth = 4.dp
                    )
                }
                PvzProgressBar(
                    progress = progress,
                    label = if (progress >= 1) "完成" else if (state.showCancel) "取消" else null,
                    modifier = Modifier.fillMaxWidth(),
                    onLabelClick = {
                        if (progress >= 1) { // 已完成：点击“完成”直接关闭
                            JsUiManager.closeProgress()
                        } else if (state.showCancel) { // 进行中：点击“取消”触发取消（隐藏弹窗 + 执行 onCancel 回调）
                            scope.launch {
                                JsUiManager.cancelProgress()
                            }
                        }
                    })

                Spacer(modifier = Modifier.height(16.dp))
            }) { // 进度文本
            if (state.message.isNotEmpty() || !state.isIndeterminate) {
                val displayText = if (state.isIndeterminate) {
                    state.message.ifEmpty { "处理中..." }
                } else {
                    "${state.message.ifEmpty { "处理中..." }} (${state.progress}%)"
                }
                PvzRichText(
                    displayText,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 10.dp),
                    defaultStyle = PvzTextOliveStyle.copy(shadowColor = null)
                )
            }
        }
    }
}

/**
 * JS 解压根弹窗
 * 用法:
 *   const result = ui.extract(["path1", "path2"], "/target/dir");
 *   console.log(result.isComplete);
 */
@Composable
fun JsExtractorDialog() {
    val state by JsUiManager.extractorState.collectAsState()

    if (state.isVisible) {
        PvzExtractorDialog(
            uiState = state, isShowNotUpdate = true, onDismissRequest = {
                JsUiManager.closeExtractor()
            })
    }
}
