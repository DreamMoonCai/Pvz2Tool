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
import io.github.dreammooncai.pvz2tool.ui.PvzInput
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
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.js
import kotlin.math.roundToInt
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.controller.SoundController
import io.github.dreammooncai.pvz2tool.icon.Gear
import io.github.dreammooncai.pvz2tool.icon.Hook
import io.github.dreammooncai.pvz2tool.icon.HookSelect
import io.github.dreammooncai.pvz2tool.icon.Pvz2Icon
import io.github.dreammooncai.pvz2tool.js.PvzToolJsEngine
import io.github.dreammooncai.pvz2tool.js.JsConsole
import io.github.dreammooncai.pvz2tool.js.code.JsToolContext
import io.github.dreammooncai.pvz2tool.rememberSoundInteractionSource
import io.github.dreammooncai.pvz2tool.view.AsyncImageFromAssets
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.js.FunctionParam
import io.github.alexzhirkevich.keight.js.JSFunction
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.Object
import io.github.dreammooncai.pvz2tool.js.code.JsPopupNavigator
import io.github.dreammooncai.pvz2tool.js.func
import io.github.dreammooncai.pvz2tool.js.orNull
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import java.util.concurrent.ConcurrentHashMap
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupContent
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupItem
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupItemSwitch
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupItemArrow
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupText
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupHost
import io.github.dreammooncai.pvz2tool.ui.popup.MainPopup
import io.github.dreammooncai.pvz2tool.ui.popup.SubPopup
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupNavigator
import kotlinx.coroutines.withContext

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
    val name: String = "", val value: String = "", val danger: Boolean = false // 危险操作 → 红色按钮
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
    val title: String = "", val value: Boolean = false, // 仅 switch 用
    val text: String = "", // 仅 text 用（纯字符串项以 bare 渲染，无额外边距）
    val bare: Boolean = false, // true=纯文本无内边距（由字符串字面量生成）；false=含边距（仿开关行，仅文字）
    val onChange: (suspend (JsAny?) -> Unit)? = null, // switch 状态变化回调(newValue)
    val onClick: (suspend (JsAny?) -> Unit)? = null // arrow 点击回调(接收 nav 对象)
)

/** 通用弹窗中的一个页面（主页面或子页面） */
data class JsPopupPage(
    val title: String = "", val items: List<JsPopupItem> = emptyList(), val bottomText: String? = null
)

/** 通用弹窗状态（设置风格，支持子页面 push/pop） */
data class JsPopupState(
    val isVisible: Boolean = false,
    val title: String = "",
    val items: List<JsPopupItem> = emptyList(),
    val onClose: (suspend (JsAny?) -> Unit)? = null // 弹窗关闭时触发
)

// ======================== 跨弹窗 UI 调用栈（嵌套 backUI 支持） ========================
// 每种弹窗不再只有单一槽位；改为全局有序栈：showXxx 入栈、关闭出栈。
// 仅栈顶渲染；嵌套弹窗通过 当前.backUI(data) 返回上一层并带回数据；
// 若嵌套弹窗未调用 backUI 而直接关闭（按钮/点外部），则向上逐级静默关闭（级联）直到根弹窗，
// 根弹窗关闭后根 JS 通过 await 返回并继续执行剩余逻辑。
// 注意：进度(progress)/解压(extract)为独立单槽，不纳入此调用栈（沿用原单实例语义）。

/** 弹窗帧类型，与 _xxxState 一一对应（MULTISELECT 复用 SELECT） */
enum class UiFrameKind {
    CONFIRM, ALERT, PROMPT, SELECT, MULTISELECT, ACTIONSHEET, SLIDER, LOADING, POPUP
}

/**
 * UI 调用栈中的一帧。
 * @param kind 弹窗类型
 * @param state 具体状态对象（JsXxxState）
 * @param deferred 栈顶 await 返回用 deferred；无则为 null（loading/popup）
 * @param defaultResult 级联关闭时补全父帧 deferred 的默认值
 * @param onResult **本帧自己**的回传回调（打开本帧时由**调用方**注册，本帧调用 backUI 时把 data 交回它）。
 *                 注意归属：backUI 由谁调用，触发的就是谁的 onResult（不是父帧的）。
 * @param onClose 弹窗关闭回调（popup 的 onClose；级联/backUI 触发的关闭均会调用）
 * @param parent 入栈时设为当时的栈顶（即"上一层 UI"）；根帧为 null
 * @param hasBacked 是否已通过 backUI 显式返回（用于区分"显式返回"与"原生关闭"）
 */
data class UiFrame(
    val kind: UiFrameKind,
    val state: Any,
    val deferred: CompletableDeferred<Any?>?,
    val defaultResult: Any?,
    val onResult: (suspend (JsAny?) -> Unit)? = null,
    val onClose: (suspend (JsAny?) -> Unit)? = null,
    var parent: UiFrame? = null,
    var hasBacked: Boolean = false,
    /**
     * 本帧的「交互草稿值」（输入框文本 / 滑块值 / 选择集合）。
     * 跨类型嵌套时父弹窗的 Composable 会短暂离开组合（全局仅栈顶渲染），其 remember 状态随之丢失；
     * 把草稿挂在帧上，等父弹窗重新可见时即可恢复用户已输入/已选择的内容。
     */
    var draft: Any? = null,
    /**
     * 打开本帧的那段 JS 的执行上下文。入栈时（[JsUiManager] 的 pushFrame）自动填充：
     * 优先取「当前正在执行的脚本上下文」（[PvzToolJsEngine.currentJsContext]），
     * 其次继承父帧的同名值（弹窗按钮回调运行在脚本之外，此时前者为 null）。
     *
     * 弹窗内的复合文本（`{{js:...}}` / `{{link|JS:文字}}`）以此作为执行上下文，
     * 因此在弹窗里也能访问 `当前`，进而调用 `当前.backUI(data)` 返回上一层。
     * 若不带上此上下文，链接里的 `当前.xxx` 会因 `当前` 未定义而抛错并被静默吞掉（表现为「点了没反应」）。
     */
    val ownerContext: JsToolContext? = null
) {
    /**
     * 入栈序号（单调自增，由 [JsUiManager.pushFrame] 赋值）。
     *
     * 🔴 用于 `backUI` 在「复合文本求值通道的 uiFrame」与「回调通道的槽位」两个候选之间做**严格**取舍：
     * 取序号更大（更晚入栈 = 更内层）的那个。理由——弹窗只能在栈顶入栈，故后入栈者必是前者的后代，
     * 而「当前这段 JS 逻辑上所属的帧」恒是两者中更内层的那个：
     *   - 链接点击（uiFrame=所在帧）时，槽位可能是某个**尚未结束的祖先回调**留下的外层帧 → 取更内层的 uiFrame；
     *   - 弹窗回调（槽位=该回调所属帧）时，闭包的 uiFrame 是**创建回调时所在的外层帧** → 取更内层的槽位。
     * 该判据在「帧已离场」时依然成立（序号不受出栈影响），因此比按栈内下标比较更稳。
     *
     * 刻意放在**类体**而非构造器：data class 的 equals/hashCode 只认构造器参数，
     * 放进来会让「内容相同但序号不同」的两帧不再相等，破坏既有（本就应当按引用比较的）语义。
     */
    var seq: Long = 0
        internal set
}

/** 通过 CompositionLocal 向下传递"当前弹窗帧"，使弹窗内复合文本的 {{js:}} 能拿到 当前.backUI */
val LocalUiFrame = compositionLocalOf<UiFrame?> { null }

/** 选择弹窗（select / multiSelect）的交互草稿：单项选中下标 + 多项选中集合 */
private data class SelectDraft(val index: Int, val indices: Set<Int>)

// ======================== JS UI 管理器 ========================

object JsUiManager {
    // ===== 跨弹窗 UI 调用栈 =====
    private val uiStack = mutableListOf<UiFrame>()

    /**
     * 与弹窗**组合生命周期解耦**的稳定作用域（主线程，独立于任何 Dialog 的 `rememberCoroutineScope`）。
     *
     * 🔴🔴 所有 JS 回调（onConfirm / onCancel / onResult / onClick / onSelect / onChange …）
     * 以及弹窗按钮的「关闭并触发回调」逻辑，都**必须**跑在本作用域上、绝不能用弹窗 Composable
     * 的 `rememberCoroutineScope()`。否则弹窗在 dismiss 后立刻离开组合，其
     * `rememberCoroutineScope()` 随之被 forget；回调链（尤其是 `onResult`）若在其后又触碰
     * `scope.launch`，就会抛 `ForgottenCoroutineScopeException`（表现：点确定/返回后偶尔报
     * `回调 xxx 执行失败`）。
     *
     * [launchCallback] 内部统一 `withContext(uiScope)` 把回调切换到本作用域，从根上杜绝该问题。
     * 弹窗 Composable 里的 `scope.launch { JsUiManager.xxx }` 也应改用 [uiScope] 以彻底解耦。
     */
    val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * 统一执行「用户 JS」的稳定入口：恒跑在与组合生命周期解耦的 [uiScope] 上。
     *
     * 🔴 为什么需要它：过去主界面 / 悬浮窗的条目把 [PvzToolJsEngine.executeScript] 直接挂在条目的
     * `rememberCoroutineScope()` 上。条目（栏目展开/折叠、悬浮窗消失）一旦离场，该 scope 被 forget，
     * 仍在挂起的 executeScript（keight 的 jsFn.call 必然 suspend）resume 即抛
     * `ForgottenCoroutineScopeException`。本入口切到 [uiScope]，从根上消除该异常——
     * 与弹窗回调经 [launchCallback] 切到同一作用域是同一思路。
     *
     * 🔴 关于「手速过快反复触发同一脚本互相抢占改写 INFO/sectionStates」：
     * 传 [key] 时对「同一 key 的旧在途执行」做【最新胜出】取消（语义等同
     * [kotlinx.coroutines.flow.collectLatest]），保证最终落地的只有最后一次触发的结果，
     * 避免多次执行交错写入同一份共享状态。不传 [key] 则不做去重
     * （例如顶栏图标彼此独立、进入游戏批量脚本只应整体取消）。
     *
     * @param key 去重键；同一 key 的新执行会取消旧在途执行。建议取 `"js:<sectionId>:<itemId>"` 等稳定标识。
     * @param block 实际要执行的挂起块（通常是一次 [PvzToolJsEngine.executeScript] 调用）。
     */
    private val scriptJobs = ConcurrentHashMap<String, Job>()

    fun runUserScript(
        key: String? = null,
        block: suspend CoroutineScope.() -> Unit,
    ) {
        val job = uiScope.launch {
            runCatching { block() }.onFailure { e ->
                if (e !is CancellationException) JsConsole.error("用户脚本执行失败:", e)
            }
        }
        if (key != null) {
            // 取消同一 key 的旧在途执行（最新胜出），避免多次触发交错改写共享状态
            scriptJobs.put(key, job)?.cancel()
            job.invokeOnCompletion {
                // 仅当本 job 仍是该 key 的当前持有者时才移除，避免误删后启动的新 job
                if (scriptJobs[key] === job) scriptJobs.remove(key)
            }
        }
    }

    /** 帧入栈序号发号器（只在 [stackLock] 内自增，出栈不回收）。 */
    private var frameSeq = 0L

    // 🔴 这里**没有**「把运行时的 `当前` 重新绑定到某帧」的方法，是刻意删掉的（曾有两个致命错误）：
    //   1. `runtime.set("当前", ctx, VariableType.Local)` 在同一个作用域里**只能成功一次**，
    //      第二次起抛 SyntaxError: Identifier '当前' (Local) is already declared。
    //      在 runtime 顶层反复重绑 → 首次之后全是空转（异常还被 runCatching 吞掉，极难排查）。
    //      （PvzToolJsEngine.callJsFunction 之所以没事，是因为它把 set 包在每次新建的
    //       root JSFunction 函数体里，每次都是全新作用域。）
    //   2. 即便重绑成功也没用：JS 是**词法作用域**，onConfirm / onSelect / onClick 这些闭包
    //      捕获的是 item 脚本执行时那份 `当前`，事后无论怎么 set 都影响不到已创建的闭包。
    //   结论：回调场景只能靠下面的 [callbackFrame] 槽位，不要试图重绑 `当前`。
    //   复合文本（{{js:}} / {{link:}}）走另一条路：由 PvzText 用 executeScript(uiFrame = 所在帧)
    //   新建上下文，其 uiFrame 天然指向脚本所在的那一帧。

    /**
     * 「当前正在执行 JS 回调的那一帧」槽位栈（LIFO，[currentCallbackFrame] 取栈顶）。
     *
     * 🔴 存在的必要性：JS 的 `onConfirm` / `onSelect` / `onClick` 都是**闭包**，词法捕获了
     * item 脚本里那份 `当前`，其 uiFrame 要么是 null、要么是**父帧**（从第一层 {{link}} 里
     * 打开第二层时就是父帧），都无法指向「回调所属的这一帧」。而 JS 变量是词法作用域，
     * 事后在 runtime 上重新 `set` 影响不到这些已捕获的闭包。
     *
     * 解法：不跟作用域较劲，改为在**执行回调期间**把所属帧挂到本槽位，
     * 由 [JsToolContext] 的 backUI 经 [resolveBackTarget] 与 uiFrame 做严格取舍后取用。
     *
     * 🔴🔴 为什么必须是「按身份入栈 / 出栈」而**不能**用「保存 - 还原」写法：
     * keight 的 `jsFn.call(...)` **即便对同步函数体也会挂起调用协程**（实测见
     * `KeightScopeProbeTest.syncCallYieldsCallingThread`），JS 函数体是跑到别的线程上执行的。
     * 于是必然出现这个交错（弹窗按钮「先同步弹栈、再执行回调」的场景天天发生）：
     *   1) 外层回调协程 A（如 popup 箭头项的 onClick）挂在 `job.join()` 上等待，
     *      它的 `withCallbackFrame(popup)` 一直占着槽位；
     *   2) 用户点「确认」→ `dismissSelfWithCallback` 先补全 deferred（A 的 join 立刻完成）
     *      再 `launchCallback(self)` 起协程 B，B 把槽位置为「本帧」；
     *   3) B 随即调用 `jsFn.call(...)` 挂起，A 被唤醒、跑 `finally`；
     *   4) **若 A 的 finally 是「还原成 prev」就会把 B 刚挂上的帧冲掉**（此处 prev 正是 null）；
     *   5) JS 函数体随后才真正执行 `当前.backUI(...)` → 取到空帧 → 静默无反应。
     * 这正是「点确认只关弹窗、不返回数据 / onConfirm 静默」的根因。
     * 改成按身份移除自己的槽位后：A 的 finally 只会摘掉 A 自己那一项，B 的帧安然无恙。
     */
    private val callbackStack = mutableListOf<CallbackSlot>()

    /** [callbackStack] 中的一项。用独立对象承载，靠**身份**精确移除（不能靠 equals 比内容）。 */
    private class CallbackSlot(val frame: UiFrame?)

    /** 供 [JsToolContext] 的 backUI 在本上下文未绑定帧时回退读取（取最晚挂上的那个回调帧）。 */
    internal fun currentCallbackFrame(): UiFrame? = callbackStack.lastOrNull()?.frame

    /**
     * 在 [block] 执行期间把「当前回调帧」压栈（可重入；退出时**只摘掉自己那一项**）。
     *
     * 🔴 绝不能写成 `val prev = callbackFrame; callbackFrame = frame; finally { callbackFrame = prev }`：
     * 并发回调会互相踩踏，详见 [callbackStack] 的说明。
     */
    internal suspend fun <T> withCallbackFrame(frame: UiFrame?, block: suspend () -> T): T {
        val slot = CallbackSlot(frame)
        callbackStack.add(slot)
        return try {
            block()
        } finally {
            callbackStack.remove(slot)
        }
    }

    /** 当前栈顶帧（快照，仅用于渲染判断） */
    fun topFrame(): UiFrame? = uiStack.lastOrNull()

    /** 当前 popup 类型栈顶帧（供 JsPopupDialog 按帧 identity 重建渲染，避免跨 popup 帧时内容被冻结） */
    fun topPopupFrame(): UiFrame? = uiStack.lastOrNull { it.kind == UiFrameKind.POPUP }

    /** 当前栈顶是否是某类型的帧（供 Composable 渲染判断，避免同屏多弹窗重叠） */
    fun isGlobalTop(kind: UiFrameKind): Boolean = topFrame()?.kind == kind

    /**
     * 入栈一帧（其父帧设为当前栈顶），并刷新渲染（仅栈顶可见）。
     *
     * 同时补全 [UiFrame.ownerContext]（弹窗内复合文本执行 JS 所需的上下文）：
     * - 根弹窗由脚本直接打开 → 取 [PvzToolJsEngine.currentJsContext]（此刻正在执行该脚本）
     * - 嵌套弹窗由「弹窗按钮的 JS 回调」打开 → 回调运行在 executeScript 之外，
     *   currentJsContext 为 null，故继承父帧的上下文
     */
    private fun pushFrame(frame: UiFrame) {
        val parent = uiStack.lastOrNull()
        if (callbackStack.isEmpty())
            frame.ownerContext?.uiFrame = frame
        frame.parent = parent
        frame.seq = ++frameSeq
        uiStack.add(frame)
        sync()
    }

    private fun onDeleteFrame(frame: UiFrame?) {
        if (frame == null) return
        val ctx = frame.ownerContext ?: return
        // 🔴 若 ownerContext.uiFrame 恰为被摘帧本身，或其某个仍在栈上的后代帧，
        // 则把 uiFrame 回退到被摘帧的父帧，避免 当前 指向已离场 / 被重挂接的帧。
        // 原先只处理「uiFrame === frame」；中间帧被 backUIFrame/dismissFrame 摘除、其子帧被
        // reparent 到祖父时，uiFrame 仍停在后代帧上（悬空），此处一并纠偏。
        // 正常流程下 uiFrame 从不等于被摘中间帧的后代，故该逻辑仅在罕见场景激活，不影响已验证的 backUI。
        var f = ctx.uiFrame
        while (f != null && f !== frame) {
            f = f.parent
        }
        if (f === frame) {
            ctx.uiFrame = frame.parent
        }
    }

    /**
     * 在 `backUI` 的两个候选帧中选出**真正应该作用的那一帧**：取入栈序号更大（更内层）的。
     * 任一为 null 时取另一方；两者都 null 时返回 null（调用方会告警并忽略）。
     * 详见 [UiFrame.seq] 的说明。
     *
     * @param evalFrame 复合文本就地求值通道（`{{js:}}` / `{{link}}`）：该脚本所在帧
     * @param callbackFrame 回调通道槽位（`launchCallback` 挂上的「回调所属帧」）
     */
    internal fun resolveBackTarget(evalFrame: UiFrame?, callbackFrame: UiFrame?): UiFrame? = when {
        evalFrame == null -> callbackFrame
        callbackFrame == null -> evalFrame
        callbackFrame.seq >= evalFrame.seq -> callbackFrame
        else -> evalFrame
    }

    /**
     * 在与弹窗生命周期**解耦**的稳定作用域（[uiScope]）中执行 JS 回调。
     *
     * 🔴 为什么必须 `withContext(uiScope)`：
     * 弹窗按钮是「先同步弹栈、再执行回调」。弹栈会让弹窗 Composable 立刻离开组合，
     * 其 `rememberCoroutineScope()` 随之被 forget。若回调体直接跑在弹窗的 scope 上，
     * 回调内部（尤其 `onResult` 里再开弹窗 / 调 `当前.backUI`）一旦再触碰 `scope.launch`，
     * 就会抛 `ForgottenCoroutineScopeException`。
     * 这里把整段回调体切到 [uiScope]（与任何 Dialog 组合无关的 SupervisorJob + Main 作用域），
     * 回调链的两端（触发回调的按钮、被触发的 onResult）都跑在同一稳定作用域上，
     * 弹窗离场不再影响回调执行。
     */
    suspend fun launchCallback(frame: UiFrame? = null, block: suspend () -> Unit) {
        withContext(uiScope.coroutineContext) {
            withCallbackFrame(frame) {
                runCatching { block() }.onFailure { JsConsole.error("JS 回调执行失败:", it) }
            }
        }
    }

    /**
     * 依据栈重建各类型 StateFlow：仅全局栈顶可见，其余类型隐藏。
     * 注：EXTRACT / PROGRESS 不纳入调用栈，保持各自独立单槽语义。
     */
    private fun sync() {
        val top = uiStack.lastOrNull()

        // 支持「多个 kind 共用一个 StateFlow」（如 SELECT / MULTISELECT 共用 _selectState）：
        // 必须一次性在合并后的集合里找最靠上的帧，否则后一次 set 会用空值覆盖前一次写入的可见态。
        @Suppress("UNCHECKED_CAST")
        fun <T> set(flow: MutableStateFlow<T>, empty: T, vararg kinds: UiFrameKind) {
            val f = uiStack.lastOrNull { it.kind in kinds }
            flow.value = if (f != null && f === top) f.state as T else empty
        }
        set(_confirmState, JsConfirmState(), UiFrameKind.CONFIRM)
        set(_alertState, JsAlertState(), UiFrameKind.ALERT)
        set(_promptState, JsPromptState(), UiFrameKind.PROMPT)
        set(_selectState, JsSelectState(), UiFrameKind.SELECT, UiFrameKind.MULTISELECT)
        set(_actionSheetState, JsActionSheetState(), UiFrameKind.ACTIONSHEET)
        set(_sliderState, JsSliderState(), UiFrameKind.SLIDER)
        set(_loadingState, JsLoadingState(), UiFrameKind.LOADING)
        set(_popupState, JsPopupState(), UiFrameKind.POPUP)
    }

    /**
     * 显式返回上一层（当前.backUI）。
     * 弹出当前栈顶帧，补全其 deferred（带 data），并唤醒其父帧：父帧回到可见态，
     * 等待后续交互；同时触发【本帧】的 onResult(data)（即父帧当初打开本帧时传入的回传回调，
     * 用于把 data 交回给父帧后续逻辑）。父帧保持打开。
     * 若当前帧无父帧（已是根），backUI 不会被注册，等价于级联关闭（cascadePopTop）。
     */
    suspend fun backUI(data: JsAny? = null) {
        val frame = uiStack.lastOrNull() ?: return
        backUIFrame(frame, data)
    }

    /**
     * [backUI] 的「按帧引用精确匹配」版本，供 `当前.backUI(data)` 使用。
     *
     * 与直接操作全局栈顶相比更稳健：JS 拿到的 `当前` 绑定的是**发起调用的那一帧**，
     * 即使此刻栈顶已被别的弹窗（loading / alert 等）占据，也只返回自己那一层。
     */
    suspend fun backUIFrame(frame: UiFrame, data: JsAny? = null) {
        if (frame.hasBacked) {
            JsConsole.warn("backUI: ${frame.kind} 帧已返回过，重复调用已忽略（同一帧只应 backUI 一次）")
            return
        }
        val idx = uiStack.indexOfFirst { it === frame }
        if (idx < 0) { // 🔴 本帧已离场：弹窗按钮走的是「先弹栈、再执行回调」，因此 onConfirm / onSelect 里
            // 调用 当前.backUI(data) 时，本帧早已不在栈上（父帧已回到栈顶可见）。
            // 此时只能补发 onResult(data)，**绝不能再弹一次栈**——否则会把刚恢复可见的父帧误关掉
            // （表现为「确认后回到第一层、第三层没打开」）。
            frame.hasBacked = true
            deliverResult(frame, data)
            return
        } // 已是根帧（无上一层）：它的「上一层」是当初打开它的脚本，故先把 data 交回脚本注册的
        // onResult，再级联关闭整个栈（根 JS 的 await 随之返回）。
        // 这样 onResult 才是**跨层级一致**的「带回数据」出口：无论在第几层调用 backUI，
        // 都是「本帧带 data 离场，并把 data 交给打开本帧时注册的那个回调」。
        if (frame.parent == null) {
            frame.hasBacked = true
            cascadePopTop()
            deliverResult(frame, data)
            return
        }
        val grand = frame.parent
        frame.hasBacked = true
        if (frame.deferred != null && !frame.deferred.isCompleted) {
            frame.deferred.complete(data)
        }
        onDeleteFrame(uiStack.removeAt(idx)) // 摘除的可能是中间帧：把子帧的 parent 重新指向祖父，避免悬空父帧
        uiStack.forEach { if (it.parent === frame) it.parent = grand }
        sync()
        deliverResult(frame, data)
    }

    /**
     * 触发「本帧自身的 onResult」——即**调用方当初打开本帧时**注册的回传回调，父帧保持打开。
     *
     * 回调期间的槽位设为**父帧**（onResult 的函数体写在「打开本帧的那段脚本」里，逻辑上属于父帧那一层；
     * 根帧无父则退化为本帧自身），使回调内若再调用 `当前.backUI` 会继续向上回传一层。
     *
     * 🔴 注意这里**没有**也**不能**重绑运行时的 `当前`：JS 是词法作用域，
     * onResult 闭包捕获的是创建它的那份 `当前`，事后 set 无效（且同作用域重复 set 会抛异常）。
     * 槽位只是给 backUI 的取帧判据提供一个「更内层」的候选。
     */
    private suspend fun deliverResult(frame: UiFrame, data: JsAny?) {
        val cb = frame.onResult ?: return
        launchCallback(frame.parent ?: frame) { cb.invoke(data) }
    }

    /**
     * 原生关闭栈顶帧（按钮 / 点外部 / close 均走此路径）。
     * 若栈顶帧未通过 backUI 显式返回，则向上逐级静默关闭（级联），
     * 每级父帧的 deferred 以 defaultResult 补全、onClose 触发，直到根帧。
     * 根帧关闭后根 JS 通过 await 返回并继续执行剩余逻辑。
     */
    suspend fun cascadePopTop() {
        if (uiStack.isEmpty()) return // 弹出栈顶（其 deferred 通常由按钮已补全；若未补全也在此兜底补全）
        val top = uiStack.removeAt(uiStack.lastIndex)
        onDeleteFrame(top)
        if (top.deferred != null && !top.deferred.isCompleted) {
            top.deferred.complete(top.defaultResult)
        }
        withContext(Dispatchers.Main) { top.onClose?.let { cb -> runCatching { cb.invoke(null) } } } // 级联：所有祖先帧静默关闭
        while (uiStack.isNotEmpty()) {
            val p = uiStack.removeAt(uiStack.lastIndex)
            onDeleteFrame(p)
            if (p.deferred != null && !p.deferred.isCompleted) {
                p.deferred.complete(p.defaultResult)
            }
            withContext(Dispatchers.Main) { p.onClose?.let { cb -> runCatching { cb.invoke(null) } } }
        }
        sync()
    }

    /**
     * 原生关闭栈顶帧（按钮 / 点外部 / nav.close 等路径）。
     * 若该帧未通过 backUI 显式返回（hasBacked=false），则等价于「自动触发一个不带参数的 backUI」：
     * 仅弹出自身，触发本帧 onClose，并通知本帧的 onResult(null)（即父帧打开本帧时注册的回传回调，
     * 父帧保持打开，等待后续交互）。
     * 若本帧已是根（无父帧），则弹栈后根 JS 通过 await 以 defaultResult 返回并继续剩余逻辑。
     * （与 cascadePopTop 的区别：此处只处理栈顶一帧，不会把下方所有父帧一并关闭。）
     */
    suspend fun dismissTop() {
        val top = uiStack.lastOrNull() ?: return
        if (top.hasBacked) return
        if (top.deferred != null && !top.deferred.isCompleted) {
            top.deferred.complete(top.defaultResult)
        }
        onDeleteFrame(uiStack.removeAt(uiStack.lastIndex))
        sync() // 本帧关闭事件（如 options.onClose / onDismiss）
        top.onClose?.let { cb -> launchCallback(top.parent ?: top) { cb.invoke(null) } }
    }

    /**
     * 按 state 身份查找对应帧（用于弹窗按钮精确关闭「自身」，避免误关同类型子弹窗）。
     * 每种弹窗类型只有单个 StateFlow 槽位，但调用栈中可同时存在多个同类型帧；
     * 通过 state 身份匹配可唯一定位当前正在交互的那一帧。
     */
    fun frameByState(state: Any): UiFrame? = uiStack.firstOrNull { it.state === state }

    /**
     * 关闭指定帧（按帧引用精确匹配，而非全局栈顶）。
     * 用于弹窗按钮 / 点外部等「关闭当前正在交互的弹窗」场景：即使 onConfirm 已在其间打开了子弹窗，
     * 也只会关闭本帧自身，不会误关新开的子弹窗。
     * 语义等价于「自动触发一个不带参数的 backUI」：弹自身 + onClose + 本帧 onResult(null)（即父帧打开本帧时注册的回传回调），父帧保持打开。
     */
    suspend fun dismissFrame(
        frame: UiFrame, result: Any? = null
    ) { // 必须按引用匹配：UiFrame 是 data class，indexOf 走 equals 可能命中内容相同的另一帧
        val idx = uiStack.indexOfFirst { it === frame }
        if (idx < 0) return
        if (frame.hasBacked) return
        if (frame.deferred != null && !frame.deferred.isCompleted) {
            frame.deferred.complete(result ?: frame.defaultResult)
        }
        val frame = uiStack.removeAt(idx) // 摘除的可能是中间帧（onConfirm 里又开了子弹窗）：把子帧的 parent 重新指向祖父，避免悬空父帧
        onDeleteFrame(frame)
        val grand = frame.parent
        uiStack.forEach { if (it.parent === frame) it.parent = grand }
        sync()
        frame.onClose?.let { cb -> launchCallback(frame.parent ?: frame) { cb.invoke(null) } }
    }

    /** 关闭「state 对应的那一帧」（便捷封装，供 Composable 按钮使用） */
    suspend fun dismissSelf(state: Any) {
        frameByState(state)?.let { dismissFrame(it) }
    }

    /**
     * 关闭「state 对应的那一帧」并执行一个 JS 回调（如 onConfirm / onCancel / onSelect / onConfirm）。
     *
     * 这是消除「取消/确认循环」的关键入口。顺序严格为：
     * 1. 先 [launchCallback] 把回调调度到与弹窗生命周期解耦的 [uiScope]（此刻尚未真正执行）；
     * 2. 再同步把本帧 deferred 补全为 [result]、并 [dismissSelf] 弹栈——
     *    弹窗立即离场、栈顶不再是本帧；
     * 3. 回调随后才执行，其内部若打开子弹窗（如 onCancel 里调 ui.alert），
     *    由于本帧早已离场，子弹窗关闭后不会让本帧"死灰复燃"，从而彻底避免循环重显。
     *
     * 若 [result] 为 `null` 且本帧有 deferred，则由 [dismissSelf] 以 defaultResult 兜底补全
     * （保持与原行为一致）。若回调不需要传值（如纯 onClose），可省略 [callback]。
     */
    suspend fun dismissSelfWithCallback(state: Any, result: Any? = null, callback: (suspend () -> Unit)? = null) {
        val self = frameByState(state) ?: return
        if (self.hasBacked) return // 1) 先同步把本帧弹栈（complete deferred + dismissFrame），确保本帧立即离场；
        //    无论回调以何种调度方式执行，本帧此刻已不在栈上，子弹窗关闭后绝不会"复活"本帧。
        //    —— 必须用 dismissFrame(self) 按帧引用精确移除；切勿传 dismissSelf(self)（其按
        //       it.state === state 匹配，传入 UiFrame 永远匹配不到，导致本帧根本没被移除 → 循环重显）。
        if (self.deferred != null && !self.deferred.isCompleted) {
            self.deferred.complete(result)
        }
        dismissFrame(self, result) // 2) 再在生命周期解耦的 uiScope 中执行回调（如 onConfirm 内再开子弹窗）。
        //    执行期间把「当前回调所属帧」置为 self：回调里若调用 当前.backUI(data)，
        //    会精确返回本层并触发本帧注册给父层的 onResult（此时本帧已从栈上移除，栈顶已是父帧）。
        // 回调帧 = 本帧自身：回调里若调用 当前.backUI(data)，会精确返回本层并触发本帧注册给父层的
        // onResult（此时本帧已从栈上移除，栈顶已是父帧）。
        if (callback != null) launchCallback(self) { callback() }
    }

    /**
     * 关闭「指定类型中最靠上的那一帧」（供 loading/popup 这类**无 deferred、由外部 API 关闭**的弹窗使用）。
     * 与 dismissTop 的区别：即使其上已叠了其它类型的子弹窗，也只会关闭本类型自身的帧，不会误关子帧。
     */
    suspend fun dismissKind(kind: UiFrameKind) {
        val f = uiStack.lastOrNull { it.kind == kind } ?: return
        dismissFrame(f)
    }

    /**
     * 把「交互草稿值」写到 state 所属帧上。
     * 跨类型嵌套时父弹窗会离场（全局仅栈顶渲染），remember 丢失；草稿挂帧后重新可见即可恢复。
     */
    fun saveDraft(state: Any, value: Any?) {
        uiStack.firstOrNull { it.state === state }?.draft = value
    }

    /** 读取 state 所属帧上的「交互草稿值」；帧不存在时返回 null */
    fun readDraft(state: Any): Any? = uiStack.firstOrNull { it.state === state }?.draft

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

    /** 显示通用弹窗（设置风格，支持子页面） */
    fun showPopup(
        ownerContext: JsToolContext?,
        title: String,
        items: List<JsPopupItem>,
        onClose: (suspend (JsAny?) -> Unit)? = null,
        onResult: (suspend (JsAny?) -> Unit)? = null
    ) {
        val state = JsPopupState(
            isVisible = true, title = title, items = items, onClose = onClose
        )
        pushFrame(
            UiFrame(
                UiFrameKind.POPUP,
                state,
                null,
                null,
                onClose = onClose,
                onResult = onResult,
                ownerContext = ownerContext
            )
        )
    }

    /**
     * 隐藏通用弹窗。
     * 语义等价于「自动触发一个不带参数的 backUI」：只关闭 popup 自身那一帧，
     * 触发本帧 onClose 并通知父帧 onResult(null)，父帧保持打开（不会级联关闭下方所有父帧）。
     * 按类型定位而非全局栈顶，因此即使其上叠了子弹窗也不会误关子帧。
     */
    suspend fun hidePopup() {
        dismissKind(UiFrameKind.POPUP)
    }

    // 解压器实例（由 JS 调用时创建）
    private var extractorHolder: AssetExtractorHolder? = null
    private val extractorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** 显示确认弹窗，返回 CompletableDeferred<Boolean> */
    fun showConfirm(
        ownerContext: JsToolContext?,
        title: String,
        message: String,
        confirmText: String = "确认",
        cancelText: String = "取消",
        confirmColor: String = "",
        cancelColor: String = "",
        dismissible: Boolean = false,
        onConfirm: (suspend (JsAny?) -> Unit)? = null,
        onCancel: (suspend (JsAny?) -> Unit)? = null,
        onResult: (suspend (JsAny?) -> Unit)? = null
    ): CompletableDeferred<Boolean> {
        val deferred = CompletableDeferred<Boolean>()
        val state = JsConfirmState(
            isVisible = true,
            title = title,
            message = message,
            confirmText = confirmText,
            cancelText = cancelText,
            confirmColor = confirmColor,
            cancelColor = cancelColor,
            dismissible = dismissible,
            onConfirm = onConfirm,
            onCancel = onCancel,
            deferred = deferred
        )
        @Suppress("UNCHECKED_CAST") pushFrame(
            UiFrame(
                UiFrameKind.CONFIRM,
                state,
                deferred as CompletableDeferred<Any?>,
                false,
                onResult = onResult,
                ownerContext = ownerContext
            )
        )
        return deferred
    }

    /** 隐藏确认弹窗（级联关闭） */
    suspend fun hideConfirm() {
        dismissKind(UiFrameKind.CONFIRM)
    }

    /** 显示提示弹窗（单按钮），返回 CompletableDeferred<Unit> */
    fun showAlert(
        ownerContext: JsToolContext?,
        title: String,
        message: String,
        confirmText: String = "确定",
        confirmColor: String = "",
        dismissible: Boolean = false,
        onConfirm: (suspend (JsAny?) -> Unit)? = null,
        onResult: (suspend (JsAny?) -> Unit)? = null
    ): CompletableDeferred<Unit> {
        val deferred = CompletableDeferred<Unit>()
        val state = JsAlertState(
            isVisible = true,
            title = title,
            message = message,
            confirmText = confirmText,
            confirmColor = confirmColor,
            dismissible = dismissible,
            onConfirm = onConfirm,
            deferred = deferred
        )
        @Suppress("UNCHECKED_CAST") pushFrame(
            UiFrame(
                UiFrameKind.ALERT,
                state,
                deferred as CompletableDeferred<Any?>,
                Unit,
                onResult = onResult,
                ownerContext = ownerContext
            )
        )
        return deferred
    }

    /** 隐藏提示弹窗（级联关闭） */
    suspend fun hideAlert() {
        dismissKind(UiFrameKind.ALERT)
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
        ownerContext: JsToolContext?,
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
        onSelect: (suspend (JsAny?) -> Unit)? = null,
        onResult: (suspend (JsAny?) -> Unit)? = null
    ): CompletableDeferred<String?> {
        val deferred = CompletableDeferred<String?>()
        val state = JsSelectState(
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
        @Suppress("UNCHECKED_CAST") pushFrame(
            UiFrame(
                UiFrameKind.SELECT,
                state,
                deferred as CompletableDeferred<Any?>,
                null,
                onResult = onResult,
                ownerContext = ownerContext
            )
        )
        return deferred
    }

    /**
     * 显示多项选择弹窗，返回 CompletableDeferred<List<String>>（选中项 value 列表）
     * @param defaultValues 默认选中项的 name 或 value 列表
     */
    fun showMultiSelect(
        ownerContext: JsToolContext?,
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
        onSelect: (suspend (JsAny?) -> Unit)? = null,
        onResult: (suspend (JsAny?) -> Unit)? = null
    ): CompletableDeferred<List<String>> {
        val deferred = CompletableDeferred<List<String>>()
        val defaults = defaultValues ?: emptyList()
        val defaultIndices = items.mapIndexedNotNull { i, it ->
            if (it.value in defaults || it.name in defaults) i else null
        }
        val state = JsSelectState(
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
        @Suppress("UNCHECKED_CAST") pushFrame(
            UiFrame(
                UiFrameKind.MULTISELECT,
                state,
                deferred as CompletableDeferred<Any?>,
                emptyList<String>(),
                onResult = onResult,
                ownerContext = ownerContext
            )
        )
        return deferred
    }

    /** 隐藏选择弹窗（级联关闭） */
    suspend fun hideSelect() {
        dismissKind(UiFrameKind.SELECT)
        dismissKind(UiFrameKind.MULTISELECT)
    }

    /** 显示输入弹窗，返回 CompletableDeferred<String?> */
    fun showPrompt(
        ownerContext: JsToolContext?,
        title: String,
        message: String,
        defaultValue: String = "",
        placeholder: String = "",
        confirmText: String = "确定",
        cancelText: String = "取消",
        confirmColor: String = "",
        cancelColor: String = "",
        dismissible: Boolean = false,
        onConfirm: (suspend (JsAny?) -> Unit)? = null,
        onCancel: (suspend (JsAny?) -> Unit)? = null,
        onResult: (suspend (JsAny?) -> Unit)? = null
    ): CompletableDeferred<String?> {
        val deferred = CompletableDeferred<String?>()
        val state = JsPromptState(
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
        @Suppress("UNCHECKED_CAST") pushFrame(
            UiFrame(
                UiFrameKind.PROMPT,
                state,
                deferred as CompletableDeferred<Any?>,
                null,
                onResult = onResult,
                ownerContext = ownerContext
            )
        )
        return deferred
    }

    /** 隐藏输入弹窗（级联关闭） */
    suspend fun hidePrompt() {
        dismissKind(UiFrameKind.PROMPT)
    }

    // ======================== 操作菜单 ========================

    /**
     * 显示操作菜单，返回 CompletableDeferred<String?>（选中项 value，取消返回 null）
     * @param items 动作列表
     * @param cancelable 是否显示底部"取消"按钮（默认 true）
     */
    fun showActionSheet(
        ownerContext: JsToolContext?,
        title: String,
        items: List<JsActionItem>,
        cancelable: Boolean = true,
        cancelText: String = "取消",
        cancelColor: String = "",
        forceMaxForm: Boolean = false,
        onCancel: (suspend (JsAny?) -> Unit)? = null,
        onSelect: (suspend (JsAny?) -> Unit)? = null,
        onResult: (suspend (JsAny?) -> Unit)? = null
    ): CompletableDeferred<String?> {
        val deferred = CompletableDeferred<String?>()
        val state = JsActionSheetState(
            isVisible = true,
            title = title,
            items = items,
            cancelable = cancelable,
            cancelText = cancelText,
            cancelColor = cancelColor,
            forceMaxForm = forceMaxForm,
            onCancel = onCancel,
            onSelect = onSelect,
            deferred = deferred
        )
        @Suppress("UNCHECKED_CAST") pushFrame(
            UiFrame(
                UiFrameKind.ACTIONSHEET,
                state,
                deferred as CompletableDeferred<Any?>,
                null,
                onResult = onResult,
                ownerContext = ownerContext
            )
        )
        return deferred
    }

    /** 隐藏操作菜单（级联关闭） */
    suspend fun hideActionSheet() {
        dismissKind(UiFrameKind.ACTIONSHEET)
    }

    // ======================== 数值滑块 ========================

    /**
     * 显示数值滑块，返回 CompletableDeferred<Double>（确认后的数值，取消返回默认/初始值）
     * @param min 最小值, @param max 最大值, @param step 步长, @param default 初始值, @param unit 单位后缀
     */
    fun showSlider(
        ownerContext: JsToolContext?,
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
        onCancel: (suspend (JsAny?) -> Unit)? = null,
        onResult: (suspend (JsAny?) -> Unit)? = null
    ): CompletableDeferred<Double?> {
        val deferred = CompletableDeferred<Double?>()
        val safeMin = min.coerceAtMost(max)
        val safeMax = max.coerceAtLeast(min)
        val safeDefault = default.coerceIn(safeMin, safeMax)
        val state = JsSliderState(
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
        @Suppress("UNCHECKED_CAST") pushFrame(
            UiFrame(
                UiFrameKind.SLIDER,
                state,
                deferred as CompletableDeferred<Any?>,
                null,
                onResult = onResult,
                ownerContext = ownerContext
            )
        )
        return deferred
    }

    /** 隐藏数值滑块（级联关闭） */
    suspend fun hideSlider() {
        dismissKind(UiFrameKind.SLIDER)
    }

    // ======================== 加载指示 ========================

    /**
     * 显示加载指示（轻量"请稍候"，无按钮，需调用 hideLoading 关闭）
     * @param title 标题, @param message 说明文字
     */
    fun showLoading(
        ownerContext: JsToolContext?,
        title: String,
        message: String = "",
        dismissible: Boolean = false,
        cancelText: String = "取消",
        cancelColor: String = "",
        onDismiss: (suspend (JsAny?) -> Unit)? = null,
        onResult: (suspend (JsAny?) -> Unit)? = null
    ) {
        val state = JsLoadingState(
            isVisible = true,
            title = title,
            message = message,
            dismissible = dismissible,
            cancelText = cancelText,
            cancelColor = cancelColor,
            onDismiss = onDismiss
        )
        pushFrame(
            UiFrame(
                UiFrameKind.LOADING,
                state,
                null,
                null,
                onClose = onDismiss,
                onResult = onResult,
                ownerContext = ownerContext
            )
        )
    }

    /** 实时更新加载指示的文字（供 controller.update 调用，无需重建弹窗） */
    fun updateLoading(message: String) {
        val cur = _loadingState.value
        if (cur.isVisible) _loadingState.value = cur.copy(message = message)
    }

    /**
     * 隐藏加载指示（controller.close() / 取消按钮）。
     * 按类型定位最靠上的 LOADING 帧并只关闭它，即使其上叠了子弹窗也不会误关子帧。
     */
    suspend fun hideLoading() {
        dismissKind(UiFrameKind.LOADING)
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
        CompositionLocalProvider(LocalUiFrame provides JsUiManager.topFrame()) {
            BasicAlertDialog(
                onDismissRequest = { scope.launch { JsUiManager.dismissSelf(state) } },
                properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false)
            ) {
                // 🔴 按「当前 popup 帧的 seq」重建整个 PvzPopupHost：
                // 嵌套 ui.popup（P1→P2→P3 各自是独立帧）不走 nav.push，所以 PvzPopupHost 的
                // backStack/startDestination 在跨帧时不会变，导致 AnimatedContent 的 targetState 恒等、
                // 主页面内容子树不重新组合 —— 表现为内层 arrow 仍绑定外层 handler（点第二层箭头进了 one）。
                // 用 seq 作 key，切换 popup 帧即整体重建，主页面 arrow 的 onClick 闭包重新按本层 items 绑定。
                key(JsUiManager.topPopupFrame()?.seq) {
                PvzPopupHost(
                    startDestination = MainPopup(state.title),
                    onDismiss = { scope.launch { JsUiManager.dismissSelf(state) } }) { route, navigator -> // 捕获 navigator 供 JS 的 nav.push/pop 调用
                    val js = remember(navigator) { JsPopupNavigator(navigator) }
                    when (route) {
                        is MainPopup -> {
                            JsPopupPageContent( // 根页面标题必须跟随当前 popup 帧（_popupState.title），而非 PvzPopupHost
                                // 首次组合时固化的 startDestination 标题——否则同类型嵌套（popup→popup）时，
                                // 新弹窗的内容已切换到第二层，标题却仍停留在第一层。
                                title = state.title,
                                showBackButton = false,
                                onClose = { scope.launch { JsUiManager.dismissSelf(state) } },
                                items = state.items,
                                nav = js
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
                                nav = js,
                                bottomText = page.bottomText
                                )
                            }
                        }
                    }
                }
                } // 关闭 key(topPopupFrame()?.seq) 块
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
    nav: JsPopupNavigator,
    bottomText: String? = null
) { // 本页所属的弹窗帧（JsPopupDialog 提供的是全局栈顶帧 = 本 popup 帧）。
    // 必须在 @Composable 上下文里先取出：下面 arrow 的 onClick 是普通 lambda，不能在里面读 CompositionLocal。
    val frame = LocalUiFrame.current
    PvzPopupContent(
        title = title, showBackButton = showBackButton, onBack = onBack, onClose = onClose, bottomContent = {
            if (bottomText != null) {
                PvzPopupText(
                    bottomText, horizontalArrangement = Arrangement.Center
                )
            }
        }) {
        items.forEach { item ->
            when (item.type) {
                "switch" -> {
                    var checked by remember(item) { mutableStateOf(item.value) }
                    PvzPopupItemSwitch(
                        title = item.title, selected = checked, onCheckedChange = { newVal ->
                            checked = newVal
                            // 经 launchCallback 跑在 JsUiManager.uiScope（与弹窗组合生命周期解耦），
                            // 避免 onChange 里再开弹窗 / 调 backUI 时踩到已 forget 的 rememberCoroutineScope。
                            JsUiManager.uiScope.launch {
                                JsUiManager.launchCallback(frame) { item.onChange?.invoke(newVal.js) }
                            }
                        })
                }

                "arrow" -> PvzPopupItemArrow(title = item.title) { // 必须用与弹窗生命周期解耦的 uiScope：箭头项的 onClick 常用来开启下一层弹窗/对话框，
                    // 一旦本 popup 因「子对话框压栈导致 _popupState 变空」而暂时离场，popup 自身的
                    // rememberCoroutineScope 会被取消，正在执行的 onClick 协程随之中断、下一层弹窗立不住。
                    // 这里直接用 JsUiManager.uiScope.launch，launchCallback 内部同样切到该稳定作用域，
                    // 保证协程在 popup 离场期间依然存活，子对话框稳定压栈。
                    // frame 已在上方 @Composable 上下文中取出（arrow 的 onClick 是普通 lambda，
                    // 无法在其中读取 CompositionLocal）。
                    JsUiManager.uiScope.launch {
                        JsUiManager.launchCallback(frame) { item.onClick?.invoke(nav.js) }
                    }
                }

                "text" -> {
                    val content = item.title.ifBlank { item.text }
                    if (item.bare) { // 纯字符串项：无额外内边距，直接渲染文字
                        PvzRichText(
                            content, defaultStyle = PvzTextStyle(Color(0xFF423F00), null), fontSize = 20.sp
                        )
                    } else { // 含边距文本：仿开关行（相同内边距与分隔线），仅显示文字无控件
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

    if (state.isVisible) {
        CompositionLocalProvider(LocalUiFrame provides JsUiManager.topFrame()) {
            val scope = rememberCoroutineScope()
            PvzStyledDialog(
                isVisible = true,
                titleText = state.title,
                onDismissRequest = {
                    val self = state
                    self.deferred?.complete(Unit)
                    scope.launch { JsUiManager.dismissSelf(self) }
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
                            .height(48.dp),
                        onClick = { // 必须先取快照再操作：onConfirm 可能再开子弹窗，届时 sync() 会把
                            // _alertState 写成空态，若协程内继续读 state 就会拿到空对象，
                            // 导致 deferred 不补全、dismissSelf 定位不到本帧（弹窗关不掉 → 反复重现）。
                            val self =
                                state // 🔴 没有 onConfirm 就**绝不能**起这个回调协程：launchCallback 会把 // 「当前回调帧」槽位压成本 alert 帧，而此刻正 await 本 alert 的上层回调 // （典型是 onResult 里的 `ui.alert(...)`）随时会被 deferred.complete 唤醒并读槽位；
                            // 一旦读到 alert 帧，它的 `当前.backUI` 就会打在**已关闭的 alert** 上 → 静默无反应。
                            self.onConfirm?.let { cb ->
                                JsUiManager.uiScope.launch {
                                    JsUiManager.launchCallback(JsUiManager.frameByState(self)) {
                                        cb.invoke(null)
                                    }
                                }
                            }
                            self.deferred?.complete(Unit)
                            scope.launch { JsUiManager.dismissSelf(self) }
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
}

/**
 * JS 确认弹窗
 * 用法: ui.confirm("标题", "内容").then(ok => { ... })
 */
@Composable
fun JsConfirmDialog() {
    val state by JsUiManager.confirmState.collectAsState()

    if (state.isVisible) {
        CompositionLocalProvider(LocalUiFrame provides JsUiManager.topFrame()) {
            val scope = rememberCoroutineScope()
            PvzStyledDialog(
                isVisible = true,
                titleText = state.title,
                onDismissRequest = { // 先弹栈再执行回调，避免 onCancel 内的子弹窗关闭后本帧"复活"形成循环
                    scope.launch { JsUiManager.dismissSelfWithCallback(state, false) { state.onCancel?.invoke(null) } }
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
                                .height(48.dp),
                            onClick = {
                                scope.launch {
                                    JsUiManager.dismissSelfWithCallback(
                                        state, false
                                    ) { state.onCancel?.invoke(null) }
                                }
                            }) // 确认按钮
                        PvzGreenButton(
                            text = state.confirmText,
                            backgroundColor = parseColorArg(state.confirmColor),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            onClick = {
                                scope.launch {
                                    JsUiManager.dismissSelfWithCallback(
                                        state, true
                                    ) { state.onConfirm?.invoke(true.js) }
                                }
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
}

/**
 * JS 输入弹窗
 * 用法: ui.prompt("标题", "请输入内容", "默认值").then(value => { ... })
 */
@Composable
fun JsPromptDialog() {
    val state by JsUiManager.promptState.collectAsState() // key = deferred：每次新弹窗（含同类型嵌套）都以 defaultValue 重新初始化； // 若 deferred 未变而只是「跨类型嵌套后重新可见」，则从帧上的草稿恢复用户已输入的内容。
    var inputValue by remember(state.deferred) {
        mutableStateOf((JsUiManager.readDraft(state) as? String) ?: state.defaultValue)
    }

    // 输入变化即回写草稿：本弹窗因嵌套短暂离场（remember 丢失）后回来可恢复
    LaunchedEffect(state.deferred) {
        snapshotFlow { inputValue }.distinctUntilChanged().collect { JsUiManager.saveDraft(state, it) }
    }

    if (state.isVisible) {
        CompositionLocalProvider(LocalUiFrame provides JsUiManager.topFrame()) {
            val scope = rememberCoroutineScope()
            PvzStyledDialog(
                isVisible = true,
                titleText = state.title,
                onDismissRequest = {
                    scope.launch { JsUiManager.dismissSelfWithCallback(state, null) { state.onCancel?.invoke(null) } }
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
                                .height(48.dp),
                            onClick = {
                                scope.launch {
                                    JsUiManager.dismissSelfWithCallback(
                                        state, null
                                    ) { state.onCancel?.invoke(null) }
                                }
                            }) // 确认按钮
                        PvzGreenButton(
                            text = state.confirmText,
                            backgroundColor = parseColorArg(state.confirmColor),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            onClick = {
                                val value = inputValue
                                scope.launch {
                                    JsUiManager.dismissSelfWithCallback(
                                        state, value
                                    ) { state.onConfirm?.invoke(value.js) }
                                }
                            })
                    }
                }) {

                // 输入框
                PvzInput(
                    value = inputValue,
                    onValueChange = { newValue -> inputValue = newValue },
                    placeholder = state.placeholder.ifEmpty { "请输入..." },
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .fillMaxWidth(),
                    theme = PvzCollapsiblePanelTheme.GREEN,
                    label = state.message,
                    multiline = true
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

    // 同类型嵌套时 deferred 变化 → 重置为默认值；跨类型嵌套后重新可见（deferred 不变）→ 从草稿恢复。
    val choiceKey: Any? = state.deferredSingle ?: state.deferredMulti
    val initDraft = remember(choiceKey) { JsUiManager.readDraft(state) as? SelectDraft }
    var selectedIndex by remember(choiceKey) { mutableIntStateOf(initDraft?.index ?: -1) }
    var selectedIndices by remember(choiceKey) {
        mutableStateOf(initDraft?.indices ?: state.defaultIndices.toSet())
    }

    // 选中变化即回写草稿：本弹窗因嵌套短暂离场（remember 丢失）后回来可恢复
    LaunchedEffect(choiceKey) {
        snapshotFlow { SelectDraft(selectedIndex, selectedIndices) }.distinctUntilChanged()
            .collect { JsUiManager.saveDraft(state, it) }
    }

    val scope = rememberCoroutineScope()

    // 统一选择处理：单项立即返回，多项切换选中集合
    val onSelect: (Int) -> Unit = { i ->
        if (state.mode == "single") {
            val value = state.items[i].value // 先弹栈再执行 onSelect：避免 onSelect 开的子弹窗关闭后本帧"复活"形成循环
            scope.launch { JsUiManager.dismissSelfWithCallback(state, value) { state.onSelect?.invoke(value.js) } }
        } else {
            selectedIndices = if (i in selectedIndices) selectedIndices - i else selectedIndices + i
            val vals = selectedIndices.sorted().map { state.items[it].value.js }
            JsUiManager.uiScope.launch { JsUiManager.launchCallback(JsUiManager.frameByState(state)) { state.onSelect?.invoke(vals.js) } }
        }
    }

    if (state.isVisible) {
        CompositionLocalProvider(LocalUiFrame provides JsUiManager.topFrame()) {
            PvzStyledDialog(
                isVisible = true,
                titleText = state.title,
                onDismissRequest = {
                    val cancelResult: Any? = if (state.mode == "multi") emptyList<String>() else null
                    scope.launch {
                        JsUiManager.dismissSelfWithCallback(state, cancelResult) {
                            state.onCancel?.invoke(
                                null
                            )
                        }
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
                                scope.launch {
                                    JsUiManager.dismissSelfWithCallback(
                                        state, emptyList<String>()
                                    ) { state.onCancel?.invoke(null) }
                                }
                            }
                            PvzGreenButton(
                                text = state.confirmText,
                                backgroundColor = parseColorArg(state.confirmColor),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                val vals = selectedIndices.sorted().map { state.items[it].value }
                                scope.launch { JsUiManager.dismissSelfWithCallback(state, vals) }
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
                            scope.launch {
                                JsUiManager.dismissSelfWithCallback(state, null) {
                                    state.onCancel?.invoke(
                                        null
                                    )
                                }
                            }
                        }
                    }
                }) {
                if (isGrid) {
                    val columns = state.columns.coerceIn(2, 6)
                    if (state.forceMaxForm) { // 最高形态：选项可能极多，用 LazyVerticalGrid 懒加载（仅组合可见项）
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
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                    if (state.forceMaxForm) { // 最高形态：选项可能极多，用 LazyColumn 懒加载
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()
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
}

/** 网格单元格：图标(或占位矩形)在上，文字在下，整体居中 */
@Composable
private fun GridChoiceCell(
    modifier: Modifier = Modifier,
    item: JsChoiceItem,
    index: Int,
    selected: Boolean,
    isMulti: Boolean,
    showIndex: Boolean,
    showIndexColor: String,
    onSelect: (Int) -> Unit
) {
    val interaction = rememberSoundInteractionSource(
        InitializePvz2.config.ui.sounds.switchClickPress, InitializePvz2.config.ui.sounds.switchClickRelease
    )
    Column(
        modifier = modifier.clickable(interactionSource = interaction, indication = null) {
            SoundController.playSoundFromAssets(InitializePvz2.config.ui.sounds.switchClick)
            onSelect(index)
        }, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            ItemIconOrPlaceholder(
                item, 48.dp, index, item.showIndex ?: showIndex, item.showIndexColor ?: showIndexColor
            )
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
            ItemIconOrPlaceholder(
                item, 36.dp, index, item.showIndex ?: showIndex, item.showIndexColor ?: showIndexColor
            )
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
private fun ItemIconOrPlaceholder(
    item: JsChoiceItem, size: Dp, index: Int = -1, showIndex: Boolean = false, showIndexColor: String = "black"
) {
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
                PvzRichText(
                    item.name,
                    fontSize = if (size > 36.dp) 10.sp else 12.sp,
                    defaultStyle = PvzTextStyle(Color(0xFF5a4a1a)),
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
            onCancel = { JsUiManager.cancelProgress() })
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

    if (state.isVisible) {
        CompositionLocalProvider(LocalUiFrame provides JsUiManager.topFrame()) {
            val scope = rememberCoroutineScope()
            PvzStyledDialog(
                isVisible = true,
                titleText = state.title,
                onDismissRequest = {
                    if (state.cancelable) {
                        scope.launch {
                            JsUiManager.dismissSelfWithCallback(
                                state, null
                            ) { state.onCancel?.invoke(null) }
                        }
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
                                scope.launch {
                                    JsUiManager.dismissSelfWithCallback(
                                        state, null
                                    ) { state.onCancel?.invoke(null) }
                                }
                            })
                    }
                }) {
                if (state.forceMaxForm) { // 最高形态：操作项可能很多，用 LazyColumn 懒加载
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.items) { item ->
                            val value = item.value.ifEmpty { item.name }
                            if (item.danger) {
                                PvzRedButton(
                                    text = item.name, modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp), onClick = {
                                        scope.launch {
                                            JsUiManager.dismissSelfWithCallback(
                                                state, value
                                            ) { state.onSelect?.invoke(value.js) }
                                        }
                                    })
                            } else {
                                PvzGreenButton(
                                    text = item.name, modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp), onClick = {
                                        scope.launch {
                                            JsUiManager.dismissSelfWithCallback(
                                                state, value
                                            ) { state.onSelect?.invoke(value.js) }
                                        }
                                    })
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        state.items.forEach { item ->
                            val value = item.value.ifEmpty { item.name }
                            if (item.danger) {
                                PvzRedButton(
                                    text = item.name, modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp), onClick = {
                                        scope.launch {
                                            JsUiManager.dismissSelfWithCallback(
                                                state, value
                                            ) { state.onSelect?.invoke(value.js) }
                                        }
                                    })
                            } else {
                                PvzGreenButton(
                                    text = item.name, modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp), onClick = {
                                        scope.launch {
                                            JsUiManager.dismissSelfWithCallback(
                                                state, value
                                            ) { state.onSelect?.invoke(value.js) }
                                        }
                                    })
                            }
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
    val state by JsUiManager.sliderState.collectAsState() // key = deferred：每次新弹窗（含同类型嵌套）都以初始值重置； // 若 deferred 未变而只是「跨类型嵌套后重新可见」，则从帧上的草稿恢复用户已拖动的位置。
    var sliderValue by remember(state.deferred) {
        mutableStateOf((JsUiManager.readDraft(state) as? Double) ?: state.value)
    }

    // 拖动变化即回写草稿：本弹窗因嵌套短暂离场（remember 丢失）后回来可恢复
    LaunchedEffect(state.deferred) {
        snapshotFlow { sliderValue }.distinctUntilChanged().collect { JsUiManager.saveDraft(state, it) }
    }

    if (state.isVisible) {
        CompositionLocalProvider(LocalUiFrame provides JsUiManager.topFrame()) {
            val minF = state.min.toFloat()
            val maxF = state.max.toFloat()
            val steps = (((state.max - state.min) / state.step).toInt() - 1).coerceAtLeast(0)
            val unitSuffix = if (state.unit.isNotEmpty()) " ${state.unit}" else ""
            val fmt = "%.${state.decimals}f"
            val scope = rememberCoroutineScope()
            PvzStyledDialog(
                isVisible = true,
                titleText = state.title,
                onDismissRequest = {
                    scope.launch { JsUiManager.dismissSelfWithCallback(state, null) { state.onCancel?.invoke(null) } }
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
                    ) {
                        PvzRedButton(
                            text = state.cancelText,
                            backgroundColor = parseColorArg(state.cancelColor),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            onClick = {
                                scope.launch {
                                    JsUiManager.dismissSelfWithCallback(
                                        state, null
                                    ) { state.onCancel?.invoke(null) }
                                }
                            })
                        PvzGreenButton(
                            text = state.confirmText,
                            backgroundColor = parseColorArg(state.confirmColor),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            onClick = {
                                val value = sliderValue
                                scope.launch {
                                    JsUiManager.dismissSelfWithCallback(
                                        state, value
                                    ) { state.onConfirm?.invoke(value.js) }
                                }
                            })
                    }
                }) {
                Spacer(modifier = Modifier.height(8.dp))
                if (state.showValue) {
                    PvzRichText(
                        text = String.format(fmt, sliderValue) + unitSuffix,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        defaultStyle = PvzTextStyle(Color(0xFF3a4a1a)),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                } // 自定义 PVZ2 齿轮滑块（样式参考 SectionType.SLIDER）
                val activeColor = PvzCollapsiblePanelTheme.GREEN.sliderActiveColor
                val inactiveColor = PvzCollapsiblePanelTheme.GREEN.sliderInactiveColor
                val activeGradientTop = activeColor.copy(
                    red = (activeColor.red * 1.2f).coerceAtMost(1f),
                    green = (activeColor.green * 1.2f).coerceAtMost(1f),
                    blue = (activeColor.blue * 1.2f).coerceAtMost(1f)
                )
                val activeGradientBottom = activeColor.copy(
                    red = (activeColor.red * 0.7f), green = (activeColor.green * 0.7f), blue = (activeColor.blue * 0.7f)
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
                val gearSizePx =
                    with(density) { gearSize.toPx() } // 用 onSizeChanged 捕获宽度，避免 BoxWithConstraints(SubcomposeLayout) 在 Dialog 内被父布局请求 intrinsic 测量而崩溃
                var trackWidthPx by remember { mutableFloatStateOf(0f) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .onSizeChanged { trackWidthPx = it.width.toFloat() }) {
                    Box(
                        modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
                    ) { // 轨道容器（圆角胶囊形状）
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(trackHeight)
                                .clip(RoundedCornerShape(trackHeight / 2))
                        ) { // 轨道背景（内凹感）
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                inactiveGradientTop, inactiveColor, inactiveGradientBottom
                                            )
                                        )
                                    )
                            ) // 选中部分（圆柱立体感）
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(sliderProgress)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                activeGradientTop, activeColor, activeGradientBottom
                                            )
                                        )
                                    )
                            )
                        } // 齿轮作为滑块（中心对准填充色/轨道色交界处）
                        Box(
                            modifier = Modifier
                                .size(gearSize)
                                .align(Alignment.CenterStart)
                                .offset {
                                    IntOffset(
                                        ((trackWidthPx * sliderProgress) - gearSizePx / 2f).roundToInt(), 0
                                    )
                                }) {
                            Image(
                                painter = rememberVectorPainter(Pvz2Icon.Gear),
                                contentDescription = "Slider thumb",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { rotationZ = gearRotation })
                        } // 完全透明的 Slider 用于处理交互
                        Slider(
                            value = sliderValue.toFloat(),
                            onValueChange = {
                                sliderValue = it.toDouble()
                                JsUiManager.uiScope.launch { JsUiManager.launchCallback { state.onChange?.invoke(sliderValue.js) } }
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
                PvzRichText(
                    text = "范围 ${String.format(fmt, state.min)} ~ ${String.format(fmt, state.max)}$unitSuffix",
                    fontSize = 12.sp,
                    defaultStyle = PvzTextStyle(Color(0xAA3a4a1a)),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
        onClose = { JsUiManager.dismissSelf(state) })
}

/**
 * JS 弹窗宿主：一次性挂起全部 JS 驱动的弹窗（alert/confirm/prompt/itemChoice/
 * progress/actionSheet/slider/loading/extractor/popup）。
 *
 * 这样弹窗列表只在一处定义，可被两处复用：
 *  - 主界面 [Pvz2MainScreen]（悬浮窗未启用时）
 *  - 游戏内常驻遮罩 [JsDialogsFloatingHost]（悬浮窗启用时，盖在游戏上方）
 *
 * 所有子弹窗内部均以 `BasicAlertDialog` 实现，空闲时各自 StateFlow 不可见、
 * 不渲染任何内容，因此本宿主在空闲态是空 Composition，不会拦截下层触摸。
 */
@Composable
fun JsDialogsHost() {
    JsAlertDialog()
    JsConfirmDialog()
    JsPromptDialog()
    JsItemChoiceDialog()
    JsProgressDialog()
    JsActionSheetDialog()
    JsSliderDialog()
    JsLoadingDialog()
    JsExtractorDialog()
    JsPopupDialog()
    JsVideoDialog()
}
