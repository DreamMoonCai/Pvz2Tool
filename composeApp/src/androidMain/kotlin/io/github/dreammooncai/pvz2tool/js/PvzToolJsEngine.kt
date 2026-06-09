package io.github.dreammooncai.pvz2tool.js

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.JSEngine
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.js.JSFunction
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.Undefined
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

        return JSEngine(runtime)
    }

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
        return callJsFunction(targetScript,targetSection,targetItem,version,sectionStates,false,onRunJsListener,onStateChange)
    }

    private suspend fun ScriptRuntime.callJsFunction(
        targetScript: String,
        targetSection: DynamicSection?,
        targetItem: SectionItem?,
        version: VersionDef,
        sectionStates: Map<String, DynamicSectionState>,
        isRichText: Boolean = false,
        onRunJsListener: (suspend ScriptRuntime.(section: DynamicSection?,item: SectionItem?) -> Unit)? = null,
        onStateChange: ((section: DynamicSection, item: SectionItem, newValue: Any) -> Unit)?,
    ): JsAny? = try {
        // 为目标 item 创建上下文
        val targetAccess = JsFileAccess(targetSection, targetItem, version)
        val targetContext = JsToolContext(
            sectionStates = sectionStates,
            version = version,
            section = targetSection,
            item = targetItem,
            onStateChange = onStateChange,
            onCallJs = { targetItemId ->
                callJsFunction(targetItemId,version,sectionStates,onRunJsListener,onStateChange)
            },
        )

        val job = JSFunction(
            isAsync = true,
            body = Expression { rt ->
                // 注入工具上下文（path, rton, rsb, ptx 等）
                PvzToolContexts(rt, targetAccess).attached()
                rt.set("当前".js, targetContext.js, VariableType.Local)
                listOf("storage".js, "存储".js).forEach { key ->
                    rt.set(key, JsStorage(version.id).js, VariableType.Local)
                }
                onRunJsListener?.invoke(rt,targetSection,targetItem)
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
            Undefined
        }
        val resultStr = result?.toString() ?: ""
        if (resultStr.isNotBlank() && resultStr != "null" && resultStr != "undefined") {
            JsConsole.success("完成: $resultStr")
        } else {
            JsConsole.success("完成")
        }
        if (!isRichText && targetSection != null && resultStr.isNotBlank() && (targetItem?.type == SectionType.INFO || targetItem?.type == SectionType.DESCRIPTION)) {
            onStateChange?.invoke(targetSection, targetItem, resultStr)
        }
        result
    } catch (e: Exception) {
        JsConsole.error("错误:",e)
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
    ): String {
        JsConsole.info("执行: ${item?.displayName ?: section?.title}")
        return try {
            runCatching {
                getJSEngine().runtime.callJsFunction(script,section,item,version,sectionStates,isRichText,onRunJsListener) { section, item, newValue ->
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
                            SectionType.INFO ->  s.copy(infoValues = s.infoValues + (item.id to newValue.toString()))
                            SectionType.DESCRIPTION ->  s.copy(descriptionValues = s.descriptionValues + (item.id to newValue.toString()))
                            else -> s
                        }
                    }
                }?.toString() ?: ""
            }.getOrDefault("")
        } finally {
            JsFileAccess.cacheFileList.toList().forEach { file -> file.close() }
        }
    }

    /**
     * 执行一段 JS 脚本字符串（无上下文，用于通用脚本执行）。
     */
    suspend fun executeScript(script: String): String {
        val engine = getJSEngine()
        val result = engine.evaluate(script)
        return result?.toString() ?: ""
    }

    suspend fun executeScript(
        extractor: AssetExtractorHolder,
        script: String,
        section: DynamicSection,
        item: SectionItem,
        version: VersionDef,
        sectionStates: Map<String, DynamicSectionState> = emptyMap(),
        updateSectionState: ((String, (DynamicSectionState) -> DynamicSectionState) -> Unit)? = null,
    ): String {
        return executeScript(
            script = script,
            section = section,
            item = item,
            version = version,
            sectionStates = sectionStates,
            onRunJsListener = { section,item ->
                val manager = JsSmfDataManager(version, item ?: return@executeScript, extractor,section?.resolveTargetDirectory() ?: return@executeScript)

                // 仅当 smfList 非空时才走准备/打包流程
                if (item.smfList.isNotEmpty()) {
                    manager.prepareSmf()
                    pendingSmfPacks.add(manager)
                }

                val builder = JsSmfDataBuilder(manager)
                val data = builder.buildFromExtractedDir()
                listOf("data".js,"数据".js).forEach {
                    set(it, data, VariableType.Local)
                }
            },
            updateSectionState = updateSectionState,
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
