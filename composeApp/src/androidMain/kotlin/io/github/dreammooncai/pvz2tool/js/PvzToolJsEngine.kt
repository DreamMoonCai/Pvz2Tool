package io.github.dreammooncai.pvz2tool.js

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.JSEngine
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.js.JSFunction
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.js
import io.github.dreammooncai.pvz2tool.DynamicSection
import io.github.dreammooncai.pvz2tool.SectionItem
import io.github.dreammooncai.pvz2tool.SectionType
import io.github.dreammooncai.pvz2tool.VersionDef
import io.github.dreammooncai.pvz2tool.js.code.JsSmfDataBuilder
import io.github.dreammooncai.pvz2tool.js.code.JsStorage
import io.github.dreammooncai.pvz2tool.js.code.JsToolContext
import io.github.dreammooncai.pvz2tool.js.code.PvzToolContexts
import io.github.dreammooncai.pvz2tool.js.code.PvzToolGlobals
import io.github.dreammooncai.pvz2tool.ui.dialog.AssetExtractorHolder
import io.github.dreammooncai.pvz2tool.ui.dialog.UiFrame
import io.github.dreammooncai.pvz2tool.ui.main.DynamicSectionState
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

private var jSEngine: JSEngine<JSRuntime>? = null

private val jSEngineMutex = Mutex()

/**
 * 需要映射到 globalThis 的常见全局标识符。
 * keight 只内置了 `globalThis`，浏览器 / Node 生态脚本里的这些名字若不存在会直接 ReferenceError。
 * 与浏览器一致：`window === self === frames === top === parent === globalThis`。
 */
private val GLOBAL_THIS_ALIASES = listOf("globalThis", "global", "self", "window", "frames", "top", "parent")
private const val GLOBAL_THIS_EXPR = "globalThis"

/** 等待进入游戏时统一打包的 SMF manager 列表 */
private val pendingSmfPacks = mutableListOf<JsSmfDataManager>()

object PvzToolJsEngine {
    suspend fun getJSEngine() = jSEngineMutex.withLock {
        jSEngine ?: createEngine().also {
            PvzToolContexts(it.runtime, JsFileAccess).attached()
            jSEngine = it
        }
    }

    /**
     * 创建一个新的 keight 引擎，注入所有全局对象。
     */
    private suspend fun createEngine(): JSEngine<JSRuntime> {
        val runtime = JSRuntime(
            context = Dispatchers.Default + SupervisorJob(),
            console = JsConsole
        )

        PvzToolGlobals.attached(runtime)

        val engine = JSEngine(runtime)

        // keight 只内置了 globalThis。浏览器 / Node 脚本里常见的 self / window / global 等标识符
        // 直接读取会 ReferenceError 导致脚本崩溃，这里统一把它们指向 globalThis。
        // 用 runCatching 兜底：即使 globalThis 取不到也只是少了别名，不会让引擎创建失败。
        runCatching {
            val global = engine.compile(GLOBAL_THIS_EXPR).invoke()
            GLOBAL_THIS_ALIASES.forEach { alias ->
                runtime.set(alias.js, global, VariableType.Global)
            }
        }.onFailure {
            JsConsole.warn("全局属性映射失败（self/window 等别名不可用）:", it)
        }

        return engine
    }

    suspend fun ScriptRuntime.getJsToolContext(): JsToolContext? = getJSEngine().compile("当前._context").invoke(this)?.toKotlin() as? JsToolContext?

    suspend fun stringify(obj: JsAny,space: Int? = null): String {
        val stringifyFunc = getJSEngine().compile("JSON.stringify").invoke() as? JSFunction ?: return getJSEngine().runtime.toString(obj)
        return getJSEngine().runtime.toString(
            stringifyFunc.invoke(listOf(obj,null,space?.js), getJSEngine().runtime)
        )
    }

    suspend fun parse(data: String?): JsAny? {
        if (data == null) return null
        val parseFunc = getJSEngine().compile("JSON.parse").invoke() as? JSFunction ?: return null
        return parseFunc.invoke(listOf(data.js), getJSEngine().runtime)
    }

    private suspend fun ScriptRuntime.callJsFunction(
        targetItemId: String,
        version: VersionDef,
        sectionStates: Map<String, DynamicSectionState>,
        onRunJsListener: (suspend ScriptRuntime.(section: DynamicSection?,item: SectionItem?) -> Unit)? = null,
        onStateChange: ((section: DynamicSection, item: SectionItem, newValue: Any) -> Unit)?,
        uiFrame: UiFrame? = null,
    ): JsAny? {
        val itemPair = JsToolContext.findItemById(targetItemId)
        if (itemPair == null) {
            JsConsole.warn("call: 找不到 item: $targetItemId")
            return null
        }
        val (targetSection, targetItem) = itemPair

        // 获取目标 JS 脚本
        val targetScript = targetItem.readJs(targetSection,version)

        if (targetScript.isNullOrBlank()) {
            JsConsole.warn("call: item $targetItemId 没有配置 JS 脚本")
            return null
        }
        return callJsFunction(targetScript,targetSection,targetItem,version,sectionStates,false,onRunJsListener, onStateChange = onStateChange, uiFrame = uiFrame)
    }

    private suspend fun ScriptRuntime.callJsFunction(
        targetScript: String,
        targetSection: DynamicSection?,
        targetItem: SectionItem?,
        version: VersionDef,
        sectionStates: Map<String, DynamicSectionState>,
        isRichText: Boolean = false,
        onRunJsListener: (suspend ScriptRuntime.(section: DynamicSection?,item: SectionItem?) -> Unit)? = null,
        text: String? = null,
        onTextChange: ((String) -> Unit)? = null,
        onStateChange: ((section: DynamicSection, item: SectionItem, newValue: Any) -> Unit)? = null,
        uiFrame: UiFrame? = null,
    ): JsAny? = callJsFunction(JsToolContext(
        sectionStates = sectionStates,
        version = version,
        section = targetSection,
        item = targetItem,
        onStateChange = onStateChange,
        onCallJs = { targetItemId ->
            callJsFunction(targetItemId,version,sectionStates,onRunJsListener,onStateChange, uiFrame = uiFrame)
        },
        text = text,
        onTextChange = onTextChange,
        uiFrame = uiFrame,
        onRunJsListener = onRunJsListener
    ),targetScript, isRichText)

    private suspend fun ScriptRuntime.callJsFunction(
        targetContext: JsToolContext,
        targetScript: String,
        isRichText: Boolean = false
    ): JsAny? = try {
        val targetFileAccess = JsFileAccess(targetContext.section,targetContext.item,targetContext.version)
        val job = JSFunction(
            name = "root",
            isAsync = true,
            body = Expression { rt ->
                // 注入工具上下文（path, rton, rsb, ptx 等）
                PvzToolContexts(rt, targetFileAccess).attached()
                rt.set("当前".js, targetContext.js, VariableType.Local)
                listOf("storage".js, "存储".js).forEach { key ->
                    rt.set(key, JsStorage(targetContext.version.id).js, VariableType.Local)
                }
                targetContext.onRunJsListener?.invoke(rt,targetContext.section,targetContext.item)
                getJSEngine().compile(targetScript).invoke(rt)
            }
        ).call(targetContext.js, listOf(), this)?.toKotlin(this) as Job

        val result = if (job is Deferred<*>) {
            job.await() as JsAny?
        } else {
            job.join()
            if (job.isCancelled) {
                @OptIn(InternalCoroutinesApi::class)
                val err = try {
                    job.getCancellationException()
                } catch (t: Throwable) {
                    throw CancellationException("Promise was rejected or cancelled", t)
                }
                throw err.cause ?: err
            }
            null
        }
        val resultStr = result?.toString() ?: ""
        if (resultStr.isNotBlank() && resultStr != "null" && resultStr != "undefined") {
            JsConsole.success("完成: $resultStr")
        } else {
            JsConsole.success("完成")
        }
        if (!isRichText && targetContext.section != null && resultStr.isNotBlank() && (targetContext.item?.type == SectionType.INFO || targetContext.item?.type == SectionType.DESCRIPTION)) {
            targetContext.onStateChange?.invoke(targetContext.section, targetContext.item, resultStr)
        }
        result
        } catch (e: Exception) {
            // 取消类异常（CancellationException 及其子类 ChildCancelledException）通常是良性的：
            // 来自可见性 collectLatest 超集取消、runUserScript 同一 key 最新胜出取消、组合/弹窗离场取消等。
            // 它们被吞掉/恢复后即视为正常流程，不应记成「✖ 错误」刷屏；只有真正的脚本错误（ReferenceError 等）才记 error。
            if (e !is CancellationException) JsConsole.error("错误:", e)
            throw e
        }

    /**
     * 执行 JS 脚本，注入 this 对象（包含版本、栏目状态、组件状态等信息）
     *
     * @param script JS 脚本内容；如果为空且 jsPath 不为空，则从 jsPath 加载脚本
     * @param section 当前执行的 Section（用于路径解析）
     * @param item 当前执行的 SectionItem
     * @param version 版本 ID
     * @param sectionStates 所有栏目的状态（用于构建 this.all）
     * @param updateSectionState 状态变更回调（JS 中调用 setValue 时触发）
     */
    suspend fun executeScript(
        script: String,
        section: DynamicSection?,
        item: SectionItem?,
        version: VersionDef,
        sectionStates: Map<String, DynamicSectionState> = emptyMap(),
        isRichText: Boolean = false,
        onRunJsListener: (suspend ScriptRuntime.(section: DynamicSection?,item: SectionItem?) -> Unit)? = null,
        updateSectionState: ((String, (DynamicSectionState) -> DynamicSectionState) -> Unit)? = null,
        source: String? = null,
        text: String? = null,
        onTextChange: ((String) -> Unit)? = null,
        uiFrame: UiFrame? = null,
    ): String {
        JsConsole.info("执行: ${item?.displayName ?: section?.title ?: source ?: "动态代码"}")
        return try {
            runCatching {
                getJSEngine().runtime.callJsFunction(script,section,item,version,sectionStates,isRichText,onRunJsListener, text, onTextChange, uiFrame = uiFrame, onStateChange = { section, item, newValue ->
                    updateSectionState?.invoke(section.id) { s: DynamicSectionState ->
                        when (item.type) {
                            SectionType.INPUT -> s.copy(inputValues = s.inputValues + (item.id to newValue.toString()))
                            SectionType.RADIO -> s.copy(
                                selectedPresetItemIds = if (newValue as? Boolean ?: newValue.toString().toBoolean()) s.selectedPresetItemIds + (item.groupId to item.id)
                                else section.items.filter { it.type == SectionType.RADIO }
                                    .groupBy { it.groupId }
                                    .mapValues { (_, items) ->
                                        items.find { it.default }?.id ?: items.firstOrNull()?.id
                                    }
                                    .filterValues { it != null }
                                    .mapValues { it.value!! }
                            )
                            SectionType.CHECKBOX -> s.copy(
                                checkedItemIds = if (newValue as? Boolean ?: newValue.toString().toBoolean()) s.checkedItemIds + item.id
                                else s.checkedItemIds - item.id
                            )

                            SectionType.SLIDER -> s.copy(sliderValues = s.sliderValues + (item.id to (newValue as? Float ?: newValue.toString().toFloat())))
                            // NUMBER（数量输入框）：统一存 Int
                            SectionType.NUMBER -> s.copy(
                                numberValues = s.numberValues + (item.id to (
                                        when (newValue) {
                                            is Int -> newValue
                                            is Number -> newValue.toInt()
                                            else -> newValue.toString().toDoubleOrNull()?.toInt() ?: 0
                                        }
                                        ))
                            )
                            SectionType.INFO ->  s.copy(infoValues = s.infoValues + (item.id to newValue.toString()))
                            SectionType.DESCRIPTION ->  s.copy(descriptionValues = s.descriptionValues + (item.id to newValue.toString()))
                            else -> s
                        }
                    }
                })?.toString() ?: ""
            }.getOrDefault("")
        } finally {
            JsFileAccess.cacheFileList.toList().forEach { file -> file.close() }
            // 非复合文本求值（即 BUTTON / CHECKBOX / SLIDER 等组件触发的脚本）执行完毕后，
            // 通知所有 {{js:...}} 复合文本重新计算，使 name / desc / buttonText 能反映最新的 JS 状态。
            // isRichText = true 的调用是复合文本自身求值，跳过以避免「求值 → 刷新 → 再求值」死循环。
            if (!isRichText) JsRichTextRefresher.refresh()
        }
    }

    /**
     * 执行一段 JS 脚本字符串（无上下文，用于通用脚本执行）。
     *
     * @param source 执行环境标识（如「悬浮窗按钮」「顶栏图标」「可见性求值」「复合文本」），
     *   用于日志「执行:」输出所在位置；为空时回退为「动态代码」。
     *
     * 注意：该重载同时被复合文本的无上下文降级求值复用，因此**不会**自动触发
     * [JsRichTextRefresher.refresh]。若调用方是用户交互（如悬浮窗按钮），请在执行后手动调用。
     */
    suspend fun executeScript(script: String, source: String? = null): String {
        JsConsole.info("无上下文执行: ${source ?: "动态代码"}")
        val engine = getJSEngine()
        val result = engine.evaluate(script)
        return result?.toString() ?: ""
    }

    suspend fun executeScript(
        context: JsToolContext,
        script: String,
        isRichText: Boolean = false,
        source: String? = null,
    ): String {
        JsConsole.info("执行: ${context.item?.displayName ?: context.section?.title ?: source ?: "动态代码"}")
        return try {
            runCatching {
                getJSEngine().runtime.callJsFunction(context,script,isRichText)?.toString() ?: ""
            }.getOrDefault("")
        } finally {
            JsFileAccess.cacheFileList.toList().forEach { file -> file.close() }
            // 非复合文本求值（即 BUTTON / CHECKBOX / SLIDER 等组件触发的脚本）执行完毕后，
            // 通知所有 {{js:...}} 复合文本重新计算，使 name / desc / buttonText 能反映最新的 JS 状态。
            // isRichText = true 的调用是复合文本自身求值，跳过以避免「求值 → 刷新 → 再求值」死循环。
            if (!isRichText) JsRichTextRefresher.refresh()
        }
    }

    suspend fun executeScript(
        extractor: AssetExtractorHolder,
        script: String,
        section: DynamicSection?,
        item: SectionItem?,
        version: VersionDef,
        sectionStates: Map<String, DynamicSectionState> = emptyMap(),
        updateSectionState: ((String, (DynamicSectionState) -> DynamicSectionState) -> Unit)? = null,
        smfListOverride: List<String>? = null,
        source: String? = null,
        uiFrame: UiFrame? = null,
    ): String {
        return executeScript(
            script = script,
            section = section,
            item = item,
            version = version,
            sectionStates = sectionStates,
            uiFrame = uiFrame,
            onRunJsListener = { section,item ->
                // 优先用调用方显式指定的 smfList（顶栏图标/悬浮窗自身配置的），否则回退到 item 的 smfList。
                // 这样即使 item 为空（顶栏图标渲染时 jsContext.item 为 null）也能独立加载并注入 data。
                val effectiveSmfList = smfListOverride ?: item?.smfList
                if (effectiveSmfList.isNullOrEmpty()) return@executeScript

                val manager = JsSmfDataManager(
                    version = version,
                    sectionName = source ?: item?.displayName,
                    smfList = effectiveSmfList,
                    holder = extractor,
                    targetSmfDir = section?.resolveTargetDirectory()
                )

                manager.prepareSmf()
                pendingSmfPacks.add(manager)

                val builder = JsSmfDataBuilder(manager)
                val data = builder.buildFromExtractedDir()
                listOf("data".js,"数据".js).forEach {
                    set(it, data, VariableType.Local)
                }
            },
            updateSectionState = updateSectionState,
            source = source,
        )
    }

    /**
     * 统一打包所有等待中的 SMF 修改。
     * 应在进入游戏前调用。
     */
    suspend fun flushPendingSmfPacks() {
        if (pendingSmfPacks.isEmpty()) return
        pendingSmfPacks.forEach { it.packSmf() }
        pendingSmfPacks.clear()
    }

    fun closePrepareSmf() {
        if (pendingSmfPacks.isEmpty()) return
        pendingSmfPacks.forEach { it.closePrepareSmf() }
        pendingSmfPacks.clear()
    }
}
