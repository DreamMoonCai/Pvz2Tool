package io.github.dreammooncai.pvz2tool.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.DialogProperties
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.js
import kotlin.math.roundToInt
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.controller.SoundController
import io.github.dreammooncai.pvz2tool.icon.Gear
import io.github.dreammooncai.pvz2tool.icon.Hook
import io.github.dreammooncai.pvz2tool.icon.HookSelect
import io.github.dreammooncai.pvz2tool.icon.Pvz2Icon
import io.github.dreammooncai.pvz2tool.rememberSoundInteractionSource
import io.github.dreammooncai.pvz2tool.view.AsyncImageFromAssets
import io.github.dreammooncai.pvz2tool.view.PvzCollapsiblePanelTheme
import io.github.dreammooncai.pvz2tool.view.PvzGreenButton
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
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupContent
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupItem
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupItemSwitch
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupItemArrow
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupText
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupHost
import io.github.dreammooncai.pvz2tool.ui.popup.MainPopup
import io.github.dreammooncai.pvz2tool.ui.popup.SubPopup
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupNavigator

// ======================== 颜色解析辅助 ========================
// 将 JS 传入的颜色字符串解析为 Compose Color；支持命名色与十六进制；非法值返回 null（沿用默认主题色）。
private val NAMED_COLORS = mapOf(
    "black" to Color.Black,
    "white" to Color.White,
    "red" to Color(0xFFC62828),
    "green" to Color(0xFF558B2F),
    "blue" to Color(0xFF2196F3),
    "yellow" to Color(0xFFFBC02D),
    "orange" to Color(0xFFF57C00),
    "purple" to Color(0xFF8E24AA),
    "gray" to Color(0xFF9E9E9E),
    "grey" to Color(0xFF9E9E9E),
    "gold" to Color(0xFFFBC501),
    "cyan" to Color(0xFF00ACC1),
    "pink" to Color(0xFFE91E63)
)

private fun parseColorArg(raw: String?): Color? {
    if (raw.isNullOrBlank()) return null
    val s = raw.trim().lowercase()
    NAMED_COLORS[s]?.let { return it }
    if (s.startsWith("#")) {
        val hex = s.removePrefix("#")
        val argb = when (hex.length) {
            3 -> "FF" + hex.map { "$it$it" }.joinToString("")
            6 -> "FF$hex"
            8 -> hex
            else -> return null
        }
        return runCatching { Color(argb.toLong(16)) }.getOrNull()
    }
    return null
}

// ======================== UI 状态类 ========================

/** 确认弹窗状态 */
data class JsConfirmState(
    val isVisible: Boolean = false,
    val title: String = "",
    val message: String = "",
    val confirmText: String = "确认",
    val cancelText: String = "取消",
    val confirmColor: String = "",
    val cancelColor: String = "",
    val dismissible: Boolean = false,
    val onConfirm: (suspend (JsAny?) -> Unit)? = null,
    val onCancel: (suspend (JsAny?) -> Unit)? = null,
    val deferred: CompletableDeferred<Boolean>? = null
)

/** 输入弹窗状态 */
data class JsPromptState(
    val isVisible: Boolean = false,
    val title: String = "",
    val message: String = "",
    val defaultValue: String = "",
    val placeholder: String = "",
    val confirmText: String = "确定",
    val cancelText: String = "取消",
    val confirmColor: String = "",
    val cancelColor: String = "",
    val dismissible: Boolean = false,
    val onConfirm: (suspend (JsAny?) -> Unit)? = null,
    val onCancel: (suspend (JsAny?) -> Unit)? = null,
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
    val confirmText: String = "确定",
    val confirmColor: String = "",
    val dismissible: Boolean = false,
    val onConfirm: (suspend (JsAny?) -> Unit)? = null,
    val deferred: CompletableDeferred<Unit>? = null
)

/** 选择弹窗中的单个条目 */
data class JsChoiceItem(
    val name: String = "",
    val icon: String = "",
    val value: String = "",
    val showIndex: Boolean? = null, // 单独控制本项是否显示序号；null 时跟随外层 options.showIndex
    val showIndexColor: String? = null // 单独控制序号颜色 "black"/"white"；null 时跟随外层 options.showIndexColor
)

/** 选择弹窗状态（单项 / 多项通用） */
data class JsSelectState(
    val isVisible: Boolean = false,
    val title: String = "",
    val items: List<JsChoiceItem> = emptyList(),
    val mode: String = "single", // "single" | "multi"
    val columns: Int = 4,
    val cancelable: Boolean = false,
    val confirmText: String = "确定",
    val cancelText: String = "取消",
    val confirmColor: String = "",
    val cancelColor: String = "",
    val defaultIndices: List<Int> = emptyList(),
    val showIndex: Boolean = false, // 是否在图标上居中叠加序号（1 开始）
    val showIndexColor: String = "black", // 序号颜色：外层统一控制，"black" | "white"
    val forceMaxForm: Boolean = false, // 开启后以最高形态展示（内容区固定上限高度，跳过探测重测）
    val onCancel: (suspend (JsAny?) -> Unit)? = null,
    val onSelect: (suspend (JsAny?) -> Unit)? = null, // 单选：选中某项(value)即触发；多选：选中集合(values)变化时触发
    val deferredSingle: CompletableDeferred<String?>? = null,
    val deferredMulti: CompletableDeferred<List<String>>? = null
)

/** 操作菜单中的单个动作 */
data class JsActionItem(
    val name: String = "",
    val value: String = "",
    val danger: Boolean = false // 危险操作 → 红色按钮
)

/** 操作菜单状态（底部动作列表，点击某项即返回其 value） */
data class JsActionSheetState(
    val isVisible: Boolean = false,
    val title: String = "",
    val items: List<JsActionItem> = emptyList(),
    val cancelable: Boolean = true,
    val cancelText: String = "取消",
    val cancelColor: String = "",
    val forceMaxForm: Boolean = false, // 开启后以最高形态展示（内容区固定上限高度，跳过探测重测）
    val onCancel: (suspend (JsAny?) -> Unit)? = null,
    val onSelect: (suspend (JsAny?) -> Unit)? = null, // 点击某项(value)即触发
    val deferred: CompletableDeferred<String?>? = null
)

/** 数值滑块状态 */
data class JsSliderState(
    val isVisible: Boolean = false,
    val title: String = "",
    val min: Double = 0.0,
    val max: Double = 100.0,
    val step: Double = 1.0,
    val value: Double = 0.0,
    val unit: String = "",
    val decimals: Int = 2, // 显示小数位
    val showValue: Boolean = true, // 是否显示当前数值大字体
    val confirmText: String = "确定",
    val cancelText: String = "取消",
    val confirmColor: String = "",
    val cancelColor: String = "",
    val dismissible: Boolean = false,
    val onChange: (suspend (JsAny?) -> Unit)? = null, // 拖动实时回调(value)
    val onConfirm: (suspend (JsAny?) -> Unit)? = null,
    val onCancel: (suspend (JsAny?) -> Unit)? = null,
    val deferred: CompletableDeferred<Double?>? = null
)

/** 加载指示状态（轻量"请稍候"遮罩，无按钮，需手动关闭） */
data class JsLoadingState(
    val isVisible: Boolean = false,
    val title: String = "",
    val message: String = "",
    val dismissible: Boolean = false,
    val cancelText: String = "取消",
    val cancelColor: String = "",
    val onDismiss: (suspend (JsAny?) -> Unit)? = null
)

// ======================== 通用弹窗（设置风格，支持子页面）数据结构 ========================

/** 通用弹窗（设置风格，支持子页面）中的单个项 */
data class JsPopupItem(
    val type: String = "text", // "switch" | "arrow" | "text" | "spacer"
    val title: String = "",
    val value: Boolean = false, // 仅 switch 用
    val text: String = "", // 仅 text 用（纯字符串项以 bare 渲染，无额外边距）
    val bare: Boolean = false, // true=纯文本无内边距（由字符串字面量生成）；false=含边距（仿开关行，仅文字）
    val onChange: (suspend (JsAny?) -> Unit)? = null, // switch 状态变化回调(newValue)
    val onClick: (suspend (JsAny?) -> Unit)? = null // arrow 点击回调(接收 nav 对象)
)

/** 通用弹窗中的一个页面（主页面或子页面） */
data class JsPopupPage(
    val title: String = "",
    val items: List<JsPopupItem> = emptyList(),
    val bottomText: String? = null
)

/** 通用弹窗状态（设置风格，支持子页面 push/pop） */
data class JsPopupState(
    val isVisible: Boolean = false,
    val title: String = "",
    val items: List<JsPopupItem> = emptyList(),
    val navObj: JsAny? = null, // 传给 JS 回调的 nav 对象（push/pop/close）
    val onClose: (suspend (JsAny?) -> Unit)? = null // 弹窗关闭时触发
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

    // 操作菜单状态流
    private val _actionSheetState = MutableStateFlow(JsActionSheetState())
    val actionSheetState: StateFlow<JsActionSheetState> = _actionSheetState.asStateFlow()

    // 数值滑块状态流
    private val _sliderState = MutableStateFlow(JsSliderState())
    val sliderState: StateFlow<JsSliderState> = _sliderState.asStateFlow()

    // 加载指示状态流
    private val _loadingState = MutableStateFlow(JsLoadingState())
    val loadingState: StateFlow<JsLoadingState> = _loadingState.asStateFlow()

    // 通用弹窗（设置风格，支持子页面）状态流
    private val _popupState = MutableStateFlow(JsPopupState())
    val popupState: StateFlow<JsPopupState> = _popupState.asStateFlow()
    // 弹窗内 PvzPopupHost 的 navigator 引用（由 JsPopupDialog 渲染时捕获，供 JS 的 nav.push/pop 调用）
    var popupNavigatorRef: PvzPopupNavigator? = null

    /** 显示通用弹窗（设置风格，支持子页面） */
    fun showPopup(
        title: String,
        items: List<JsPopupItem>,
        navObj: JsAny?,
        onClose: (suspend (JsAny?) -> Unit)? = null
    ) {
        _popupState.value = JsPopupState(
            isVisible = true,
            title = title,
            items = items,
            navObj = navObj,
            onClose = onClose
        )
    }

    /** 隐藏通用弹窗（并异步触发 onClose 回调） */
    fun hidePopup() {
        val cb = _popupState.value.onClose
        _popupState.value = JsPopupState()
        popupNavigatorRef = null
        extractorScope.launch { runCatching { cb?.invoke(null) } }
    }

    // 解压器实例（由 JS 调用时创建）
    private var extractorHolder: AssetExtractorHolder? = null
    private val extractorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** 显示确认弹窗，返回 CompletableDeferred<Boolean> */
    fun showConfirm(
        title: String,
        message: String,
        confirmText: String = "确认",
        cancelText: String = "取消",
        confirmColor: String = "",
        cancelColor: String = "",
        dismissible: Boolean = false,
        onConfirm: (suspend (JsAny?) -> Unit)? = null,
        onCancel: (suspend (JsAny?) -> Unit)? = null
    ): CompletableDeferred<Boolean> {
        val deferred = CompletableDeferred<Boolean>()
        _confirmState.value = JsConfirmState(
            isVisible = true, title = title, message = message,
            confirmText = confirmText, cancelText = cancelText,
            confirmColor = confirmColor, cancelColor = cancelColor,
            dismissible = dismissible, onConfirm = onConfirm, onCancel = onCancel,
            deferred = deferred
        )
        return deferred
    }

    /** 隐藏确认弹窗 */
    fun hideConfirm() {
        _confirmState.value = JsConfirmState()
    }

    /** 显示提示弹窗（单按钮），返回 CompletableDeferred<Unit> */
    fun showAlert(
        title: String,
        message: String,
        confirmText: String = "确定",
        confirmColor: String = "",
        dismissible: Boolean = false,
        onConfirm: (suspend (JsAny?) -> Unit)? = null
    ): CompletableDeferred<Unit> {
        val deferred = CompletableDeferred<Unit>()
        _alertState.value = JsAlertState(
            isVisible = true, title = title, message = message,
            confirmText = confirmText, confirmColor = confirmColor,
            dismissible = dismissible, onConfirm = onConfirm,
            deferred = deferred
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
        showIndex: Boolean = false,
        showIndexColor: String = "black",
        confirmText: String = "确定",
        cancelText: String = "取消",
        confirmColor: String = "",
        cancelColor: String = "",
        forceMaxForm: Boolean = false,
        onCancel: (suspend (JsAny?) -> Unit)? = null,
        onSelect: (suspend (JsAny?) -> Unit)? = null
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
            showIndexColor = showIndexColor,
            confirmText = confirmText,
            cancelText = cancelText,
            confirmColor = confirmColor,
            cancelColor = cancelColor,
            forceMaxForm = forceMaxForm,
            onCancel = onCancel,
            onSelect = onSelect,
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
        showIndex: Boolean = false,
        showIndexColor: String = "black",
        confirmText: String = "确定",
        cancelText: String = "取消",
        confirmColor: String = "",
        cancelColor: String = "",
        forceMaxForm: Boolean = false,
        onCancel: (suspend (JsAny?) -> Unit)? = null,
        onSelect: (suspend (JsAny?) -> Unit)? = null
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
            showIndexColor = showIndexColor,
            confirmText = confirmText,
            cancelText = cancelText,
            confirmColor = confirmColor,
            cancelColor = cancelColor,
            forceMaxForm = forceMaxForm,
            onCancel = onCancel,
            onSelect = onSelect,
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
        title: String, message: String, defaultValue: String = "", placeholder: String = "",
        confirmText: String = "确定",
        cancelText: String = "取消",
        confirmColor: String = "",
        cancelColor: String = "",
        dismissible: Boolean = false,
        onConfirm: (suspend (JsAny?) -> Unit)? = null,
        onCancel: (suspend (JsAny?) -> Unit)? = null
    ): CompletableDeferred<String?> {
        val deferred = CompletableDeferred<String?>()
        _promptState.value = JsPromptState(
            isVisible = true,
            title = title,
            message = message,
            defaultValue = defaultValue,
            placeholder = placeholder,
            confirmText = confirmText,
            cancelText = cancelText,
            confirmColor = confirmColor,
            cancelColor = cancelColor,
            dismissible = dismissible,
            onConfirm = onConfirm,
            onCancel = onCancel,
            deferred = deferred
        )
        return deferred
    }

    /** 隐藏输入弹窗 */
    fun hidePrompt() {
        _promptState.value = JsPromptState()
    }

    // ======================== 操作菜单 ========================

    /**
     * 显示操作菜单，返回 CompletableDeferred<String?>（选中项 value，取消返回 null）
     * @param items 动作列表
     * @param cancelable 是否显示底部"取消"按钮（默认 true）
     */
    fun showActionSheet(
        title: String,
        items: List<JsActionItem>,
        cancelable: Boolean = true,
        cancelText: String = "取消",
        cancelColor: String = "",
        forceMaxForm: Boolean = false,
        onCancel: (suspend (JsAny?) -> Unit)? = null,
        onSelect: (suspend (JsAny?) -> Unit)? = null
    ): CompletableDeferred<String?> {
        val deferred = CompletableDeferred<String?>()
        _actionSheetState.value = JsActionSheetState(
            isVisible = true, title = title, items = items, cancelable = cancelable,
            cancelText = cancelText, cancelColor = cancelColor, forceMaxForm = forceMaxForm,
            onCancel = onCancel, onSelect = onSelect, deferred = deferred
        )
        return deferred
    }

    /** 隐藏操作菜单 */
    fun hideActionSheet() {
        _actionSheetState.value = JsActionSheetState()
    }

    // ======================== 数值滑块 ========================

    /**
     * 显示数值滑块，返回 CompletableDeferred<Double>（确认后的数值，取消返回默认/初始值）
     * @param min 最小值, @param max 最大值, @param step 步长, @param default 初始值, @param unit 单位后缀
     */
    fun showSlider(
        title: String,
        min: Double = 0.0,
        max: Double = 100.0,
        step: Double = 1.0,
        default: Double = 0.0,
        unit: String = "",
        decimals: Int = 2,
        showValue: Boolean = true,
        confirmText: String = "确定",
        cancelText: String = "取消",
        confirmColor: String = "",
        cancelColor: String = "",
        dismissible: Boolean = false,
        onChange: (suspend (JsAny?) -> Unit)? = null,
        onConfirm: (suspend (JsAny?) -> Unit)? = null,
        onCancel: (suspend (JsAny?) -> Unit)? = null
    ): CompletableDeferred<Double?> {
        val deferred = CompletableDeferred<Double?>()
        val safeMin = min.coerceAtMost(max)
        val safeMax = max.coerceAtLeast(min)
        val safeDefault = default.coerceIn(safeMin, safeMax)
        _sliderState.value = JsSliderState(
            isVisible = true,
            title = title,
            min = safeMin,
            max = safeMax,
            step = if (step <= 0.0) 1.0 else step,
            value = safeDefault,
            unit = unit,
            decimals = decimals.coerceAtLeast(0),
            showValue = showValue,
            confirmText = confirmText,
            cancelText = cancelText,
            confirmColor = confirmColor,
            cancelColor = cancelColor,
            dismissible = dismissible,
            onChange = onChange,
            onConfirm = onConfirm,
            onCancel = onCancel,
            deferred = deferred
        )
        return deferred
    }

    /** 隐藏数值滑块 */
    fun hideSlider() {
        _sliderState.value = JsSliderState()
    }

    // ======================== 加载指示 ========================

    /**
     * 显示加载指示（轻量"请稍候"，无按钮，需调用 hideLoading 关闭）
     * @param title 标题, @param message 说明文字
     */
    fun showLoading(
        title: String,
        message: String = "",
        dismissible: Boolean = false,
        cancelText: String = "取消",
        cancelColor: String = "",
        onDismiss: (suspend (JsAny?) -> Unit)? = null
    ) {
        _loadingState.value = JsLoadingState(
            isVisible = true, title = title, message = message,
            dismissible = dismissible, cancelText = cancelText,
            cancelColor = cancelColor, onDismiss = onDismiss
        )
    }

    /** 实时更新加载指示的文字（供 controller.update 调用，无需重建弹窗） */
    fun updateLoading(message: String) {
        val cur = _loadingState.value
        if (cur.isVisible) _loadingState.value = cur.copy(message = message)
    }

    /** 隐藏加载指示 */
    fun hideLoading() {
        _loadingState.value = JsLoadingState()
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
 * JS 通用弹窗（设置风格，支持子页面）
 * 通过 ui.popup(title, items, options) 触发；items 声明式描述主页面项，
 * 项的回调里通过 nav 对象的 push/pop/close 进入/返回/关闭子页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JsPopupDialog() {
    val state by JsUiManager.popupState.collectAsState()
    val scope = rememberCoroutineScope()

    if (state.isVisible) {
        BasicAlertDialog(
            onDismissRequest = { JsUiManager.hidePopup() },
            properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false)
        ) {
            PvzPopupHost(
                startDestination = MainPopup(state.title),
                onDismiss = { JsUiManager.hidePopup() }
            ) { route, navigator ->
                // 捕获 navigator 供 JS 的 nav.push/pop 调用
                JsUiManager.popupNavigatorRef = navigator
                when (route) {
                    is MainPopup -> {
                        JsPopupPageContent(
                            title = route.title,
                            showBackButton = false,
                            onClose = { JsUiManager.hidePopup() },
                            items = state.items,
                            scope = scope,
                            navObj = state.navObj
                        )
                    }
                    is SubPopup -> {
                        val page = route.data as? JsPopupPage
                        if (page != null) {
                            JsPopupPageContent(
                                title = route.title,
                                showBackButton = true,
                                onBack = { navigator.pop() },
                                items = page.items,
                                scope = scope,
                                navObj = state.navObj,
                                bottomText = page.bottomText
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JsPopupPageContent(
    title: String,
    showBackButton: Boolean,
    onClose: () -> Unit = {},
    onBack: () -> Unit = {},
    items: List<JsPopupItem>,
    scope: CoroutineScope,
    navObj: JsAny?,
    bottomText: String? = null
) {
    PvzPopupContent(
        title = title,
        showBackButton = showBackButton,
        onBack = onBack,
        onClose = onClose,
        bottomContent = {
            if (bottomText != null) {
                PvzPopupText(
                    bottomText,
                    horizontalArrangement = Arrangement.Center
                )
            }
        }
    ) {
        items.forEach { item ->
            when (item.type) {
                "switch" -> {
                    var checked by remember(item) { mutableStateOf(item.value) }
                    PvzPopupItemSwitch(
                        title = item.title,
                        selected = checked,
                        onCheckedChange = { newVal ->
                            checked = newVal
                            scope.launch { item.onChange?.invoke(newVal.js) }
                        }
                    )
                }
                "arrow" -> PvzPopupItemArrow(title = item.title) {
                    scope.launch { item.onClick?.invoke(navObj) }
                }
                "text" -> {
                    val content = item.title.ifBlank { item.text }
                    if (item.bare) {
                        // 纯字符串项：无额外内边距，直接渲染文字
                        PvzRichText(
                            content,
                            defaultStyle = PvzTextStyle(Color(0xFF423F00), null),
                            fontSize = 20.sp
                        )
                    } else {
                        // 含边距文本：仿开关行（相同内边距与分隔线），仅显示文字无控件
                        PvzPopupItem(content, isSpacer = true) {}
                    }
                }
                "spacer" -> Spacer(Modifier.height(12.dp))
            }
        }
    }
}

/**
 * JS 提示弹窗（单按钮）
 * 用法: ui.alert("标题", "内容")
 */
@Composable
fun JsAlertDialog() {
    val state by JsUiManager.alertState.collectAsState()
    val scope = rememberCoroutineScope()

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
            dismissible = state.dismissible,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            bottomContent = {
                Spacer(modifier = Modifier.height(16.dp))

                // 确认按钮
                PvzGreenButton(
                    text = state.confirmText,
                    backgroundColor = parseColorArg(state.confirmColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp), onClick = {
                        scope.launch { state.onConfirm?.invoke(null) }
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
    val scope = rememberCoroutineScope()

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
                scope.launch { state.onCancel?.invoke(null) }
                state.deferred?.complete(false)
            },
            dismissible = state.dismissible,
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
                        text = state.cancelText,
                        backgroundColor = parseColorArg(state.cancelColor),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp), onClick = {
                            scope.launch { state.onCancel?.invoke(null) }
                            state.deferred?.complete(false)
                        }) // 确认按钮
                    PvzGreenButton(
                        text = state.confirmText,
                        backgroundColor = parseColorArg(state.confirmColor),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp), onClick = {
                            scope.launch { state.onConfirm?.invoke(true.js) }
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
    val scope = rememberCoroutineScope()
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
                scope.launch { state.onCancel?.invoke(null) }
                state.deferred?.complete(null)
            },
            dismissible = state.dismissible,
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
                        text = state.cancelText,
                        backgroundColor = parseColorArg(state.cancelColor),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp), onClick = {
                            scope.launch { state.onCancel?.invoke(null) }
                            state.deferred?.complete(null)
                        }) // 确认按钮
                    PvzGreenButton(
                        text = state.confirmText,
                        backgroundColor = parseColorArg(state.confirmColor),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp), onClick = {
                            scope.launch { state.onConfirm?.invoke(inputValue.js) }
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
    val scope = rememberCoroutineScope()

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
            val value = state.items[i].value
            scope.launch { state.onSelect?.invoke(value.js) }
            state.deferredSingle?.complete(value)
        } else {
            selectedIndices = if (i in selectedIndices) selectedIndices - i else selectedIndices + i
            val vals = selectedIndices.sorted().map { state.items[it].value.js }
            scope.launch { state.onSelect?.invoke(vals.js) }
        }
    }

    if (state.isVisible) {
        PvzStyledDialog(
            isVisible = true,
            titleText = state.title,
            onDismissRequest = {
                scope.launch { state.onCancel?.invoke(null) }
                state.deferredSingle?.complete(null)
                state.deferredMulti?.complete(emptyList())
            },
            dismissible = state.cancelable,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            forceMaxForm = state.forceMaxForm,
            bottomContent = {
                Spacer(modifier = Modifier.height(16.dp))
                if (state.mode == "multi") {
                    Row(
                        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PvzRedButton(
                            text = state.cancelText,
                            backgroundColor = parseColorArg(state.cancelColor),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            scope.launch { state.onCancel?.invoke(null) }
                            state.deferredMulti?.complete(emptyList())
                        }
                        PvzGreenButton(
                            text = state.confirmText,
                            backgroundColor = parseColorArg(state.confirmColor),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            val vals = selectedIndices.sorted().map { state.items[it].value }
                            state.deferredMulti?.complete(vals)
                        }
                    }
                } else {
                    PvzRedButton(
                        text = state.cancelText,
                        backgroundColor = parseColorArg(state.cancelColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        scope.launch { state.onCancel?.invoke(null) }
                        state.deferredSingle?.complete(null)
                    }
                }
            }) {
            if (isGrid) {
                val columns = state.columns.coerceIn(2, 6)
                if (state.forceMaxForm) {
                    // 最高形态：选项可能极多，用 LazyVerticalGrid 懒加载（仅组合可见项）
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        itemsIndexed(state.items) { i, item ->
                            GridChoiceCell(
                                modifier = Modifier.fillMaxWidth(),
                                item = item,
                                index = i,
                                selected = if (state.mode == "multi") i in selectedIndices else selectedIndex == i,
                                isMulti = state.mode == "multi",
                                showIndex = state.showIndex,
                                showIndexColor = state.showIndexColor,
                                onSelect = onSelect
                            )
                        }
                    }
                } else {
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
                                        modifier = Modifier.weight(1f),
                                        item = item,
                                        index = i,
                                        selected = if (state.mode == "multi") i in selectedIndices else selectedIndex == i,
                                        isMulti = state.mode == "multi",
                                        showIndex = state.showIndex,
                                        showIndexColor = state.showIndexColor,
                                        onSelect = onSelect
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                if (state.forceMaxForm) {
                    // 最高形态：选项可能极多，用 LazyColumn 懒加载
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(state.items) { i, item ->
                            ListChoiceRow(
                                item = item,
                                index = i,
                                hasAnyIcon = hasAnyIcon,
                                selected = if (state.mode == "multi") i in selectedIndices else selectedIndex == i,
                                isMulti = state.mode == "multi",
                                showIndex = state.showIndex,
                                showIndexColor = state.showIndexColor,
                                onSelect = onSelect
                            )
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
                                showIndexColor = state.showIndexColor,
                                onSelect = onSelect
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 网格单元格：图标(或占位矩形)在上，文字在下，整体居中 */
@Composable
private fun GridChoiceCell(
    modifier: Modifier = Modifier,
    item: JsChoiceItem, index: Int, selected: Boolean, isMulti: Boolean, showIndex: Boolean, showIndexColor: String, onSelect: (Int) -> Unit
) {
    val interaction = rememberSoundInteractionSource(
        InitializePvz2.config.ui.sounds.switchClickPress, InitializePvz2.config.ui.sounds.switchClickRelease
    )
    Column(
        modifier = modifier
            .clickable(interactionSource = interaction, indication = null) {
                SoundController.playSoundFromAssets(InitializePvz2.config.ui.sounds.switchClick)
                onSelect(index)
            }, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            ItemIconOrPlaceholder(item, 48.dp, index, item.showIndex ?: showIndex, item.showIndexColor ?: showIndexColor)
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
    showIndexColor: String,
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
            ItemIconOrPlaceholder(item, 36.dp, index, item.showIndex ?: showIndex, item.showIndexColor ?: showIndexColor)
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
private fun ItemIconOrPlaceholder(item: JsChoiceItem, size: Dp, index: Int = -1, showIndex: Boolean = false, showIndexColor: String = "black") {
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
            PvzRichText(
                text = "{{$showIndexColor:${index + 1}}}",
                defaultStyle = PvzTextStyle(Color.Black),
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
        PvzProgressDialog(
            isVisible = true,
            title = state.title,
            message = state.message,
            progress = progress,
            isIndeterminate = state.isIndeterminate,
            dismissible = progress >= 1f,
            showCancel = state.showCancel,
            onComplete = { JsUiManager.closeProgress() },
            onCancel = { JsUiManager.cancelProgress() }
        )
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

/**
 * JS 操作菜单弹窗（底部动作列表，点击某项即返回其 value）
 * 用法:
 *   ui.actionSheet("选择操作", ["复制", "重命名"])
 *   ui.actionSheet("危险操作", [{name:"删除", value:"del", danger:true}], {cancelable:false})
 */
@Composable
fun JsActionSheetDialog() {
    val state by JsUiManager.actionSheetState.collectAsState()
    val scope = rememberCoroutineScope()

    // 监听 deferred 完成状态，自动隐藏弹窗
    LaunchedEffect(state.deferred) {
        state.deferred?.let { deferred ->
            deferred.invokeOnCompletion { JsUiManager.hideActionSheet() }
        }
    }

    if (state.isVisible) {
        PvzStyledDialog(
            isVisible = true,
            titleText = state.title,
            onDismissRequest = {
                if (state.cancelable) {
                    scope.launch { state.onCancel?.invoke(null) }
                    state.deferred?.complete(null)
                }
            },
            dismissible = state.cancelable,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            forceMaxForm = state.forceMaxForm,
            bottomContent = {
                Spacer(modifier = Modifier.height(16.dp))
                if (state.cancelable) {
                    PvzRedButton(
                        text = state.cancelText,
                        backgroundColor = parseColorArg(state.cancelColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        onClick = {
                            scope.launch { state.onCancel?.invoke(null) }
                            state.deferred?.complete(null)
                        }
                    )
                }
            }
        ) {
            if (state.forceMaxForm) {
                // 最高形态：操作项可能很多，用 LazyColumn 懒加载
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.items) { item ->
                        val value = item.value.ifEmpty { item.name }
                        if (item.danger) {
                            PvzRedButton(
                                text = item.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                onClick = {
                                    scope.launch { state.onSelect?.invoke(value.js) }
                                    state.deferred?.complete(value)
                                }
                            )
                        } else {
                            PvzGreenButton(
                                text = item.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                onClick = {
                                    scope.launch { state.onSelect?.invoke(value.js) }
                                    state.deferred?.complete(value)
                                }
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    state.items.forEach { item ->
                        val value = item.value.ifEmpty { item.name }
                        if (item.danger) {
                            PvzRedButton(
                                text = item.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                onClick = {
                                    scope.launch { state.onSelect?.invoke(value.js) }
                                    state.deferred?.complete(value)
                                }
                            )
                        } else {
                            PvzGreenButton(
                                text = item.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                onClick = {
                                    scope.launch { state.onSelect?.invoke(value.js) }
                                    state.deferred?.complete(value)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * JS 数值滑块弹窗
 * 用法: ui.slider("速度", { min:0, max:200, step:5, default:60, unit:"%" })
 * 返回 number；点击"取消"返回 null。
 */
@Composable
fun JsSliderDialog() {
    val state by JsUiManager.sliderState.collectAsState()
    val scope = rememberCoroutineScope()
    var sliderValue by remember { mutableStateOf(state.value) }

    LaunchedEffect(state.deferred) {
        state.deferred?.let { deferred ->
            deferred.invokeOnCompletion { JsUiManager.hideSlider() }
        }
    }

    // 当弹窗打开时，用初始值重置滑块
    LaunchedEffect(state.isVisible, state.value) {
        if (state.isVisible) sliderValue = state.value
    }

    if (state.isVisible) {
        val minF = state.min.toFloat()
        val maxF = state.max.toFloat()
        val steps = (((state.max - state.min) / state.step).toInt() - 1).coerceAtLeast(0)
        val unitSuffix = if (state.unit.isNotEmpty()) " ${state.unit}" else ""
        val fmt = "%.${state.decimals}f"
        PvzStyledDialog(
            isVisible = true,
            titleText = state.title,
            onDismissRequest = {
                scope.launch { state.onCancel?.invoke(null) }
                state.deferred?.complete(null)
            },
            dismissible = state.dismissible,
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
                    PvzRedButton(
                        text = state.cancelText,
                        backgroundColor = parseColorArg(state.cancelColor),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        onClick = {
                            scope.launch { state.onCancel?.invoke(null) }
                            state.deferred?.complete(null)
                        }
                    )
                    PvzGreenButton(
                        text = state.confirmText,
                        backgroundColor = parseColorArg(state.confirmColor),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        onClick = {
                            scope.launch { state.onConfirm?.invoke(sliderValue.js) }
                            state.deferred?.complete(sliderValue)
                        }
                    )
                }
            }
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            if (state.showValue) {
                Text(
                    text = String.format(fmt, sliderValue) + unitSuffix,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3a4a1a),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            // 自定义 PVZ2 齿轮滑块（样式参考 SectionType.SLIDER）
            val activeColor = PvzCollapsiblePanelTheme.GREEN.sliderActiveColor
            val inactiveColor = PvzCollapsiblePanelTheme.GREEN.sliderInactiveColor
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
            val sliderProgress = ((sliderValue - state.min) / (state.max - state.min)).toFloat().coerceIn(0f, 1f)
            val gearRotation = sliderProgress * 360f
            val density = LocalDensity.current
            val trackHeight = 20.dp
            val gearSize = 32.dp
            val gearSizePx = with(density) { gearSize.toPx() }
            // 用 onSizeChanged 捕获宽度，避免 BoxWithConstraints(SubcomposeLayout) 在 Dialog 内被父布局请求 intrinsic 测量而崩溃
            var trackWidthPx by remember { mutableFloatStateOf(0f) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .onSizeChanged { trackWidthPx = it.width.toFloat() }
            ) {
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
                                .fillMaxWidth(sliderProgress)
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
                            .offset {
                                IntOffset(
                                    ((trackWidthPx * sliderProgress) - gearSizePx / 2f).roundToInt(),
                                    0
                                )
                            }
                    ) {
                        Image(
                            painter = rememberVectorPainter(Pvz2Icon.Gear),
                            contentDescription = "Slider thumb",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { rotationZ = gearRotation }
                        )
                    }
                    // 完全透明的 Slider 用于处理交互
                    Slider(
                        value = sliderValue.toFloat(),
                        onValueChange = {
                            sliderValue = it.toDouble()
                            scope.launch { state.onChange?.invoke(sliderValue.js) }
                        },
                        valueRange = minF..maxF,
                        steps = steps,
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
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "范围 ${String.format(fmt, state.min)} ~ ${String.format(fmt, state.max)}$unitSuffix",
                fontSize = 12.sp,
                color = Color(0xAA3a4a1a),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * JS 加载指示弹窗（轻量"请稍候"，无按钮，需调用 ui.loading 返回的 controller.close() 关闭）
 * 用法:
 *   var ctrl = ui.loading("处理中", { message:"请稍候..." });
 *   // ... 耗时任务 ...
 *   ctrl.close();
 */
@Composable
fun JsLoadingDialog() {
    val state by JsUiManager.loadingState.collectAsState()
    PvzLoadingDialog(
        isVisible = state.isVisible,
        title = state.title,
        message = state.message,
        dismissible = state.dismissible,
        cancelText = state.cancelText,
        cancelColor = parseColorArg(state.cancelColor),
        onDismiss = state.onDismiss?.let { cb ->
            suspend { cb.invoke(null) }
        },
        onClose = { JsUiManager.hideLoading() }
    )
}
