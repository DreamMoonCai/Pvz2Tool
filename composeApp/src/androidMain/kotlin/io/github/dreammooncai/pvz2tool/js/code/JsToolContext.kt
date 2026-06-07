package io.github.dreammooncai.pvz2tool.js.code

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.JsPropertyAccessor
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.ObjectScope
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.js
import io.github.dreammooncai.pvz2tool.DynamicSection
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.SectionItem
import io.github.dreammooncai.pvz2tool.SectionType
import io.github.dreammooncai.pvz2tool.VersionDef
import io.github.dreammooncai.pvz2tool.js.JsConsole
import io.github.dreammooncai.pvz2tool.js.eq
import io.github.dreammooncai.pvz2tool.js.func
import io.github.dreammooncai.pvz2tool.ui.main.DynamicSectionState
import io.github.dreammooncai.pvz2tool.ui.main.PvzLocalSaveManager
import kotlin.collections.iterator

private val config get() = InitializePvz2.config

/**
 * JS 工具上下文，构建完整的 this 对象供 JS 脚本使用。
 *
 * ## 可用的 this 属性
 * - `this.findById(id)` - 快捷查找（先找 item，再找 section）
 * - `this.version` - 版本信息 { id, name, baseAssetPath, assetPath, forceOverride }
 * - `this.all` - 所有栏目及其项的状态
 * - `this.gameActivity` - 当前配置的 gameActivity
 * - `this.checked` - CHECKBOX 当前状态（可读写）
 * - `this.value` - SLIDER 当前值（可读写）
 *
 * ## 可用的 this 方法
 * - `this.setValue(itemId, value)` - 设置栏目项的值（CHECKBOX/INPUT/SLIDER）
 *
 * 详细 API 文档见: assets/pvz2tool/js_documentation.md
 */
class JsToolContext(
    private val sectionStates: Map<String, DynamicSectionState>,
    private val version: VersionDef,
    private val section: DynamicSection?,
    private val item: SectionItem?,
    /**
     * 状态变更回调：(sectionId, itemId, newValue) -> Unit
     * 用于将 JS 中的修改同步回 Compose 状态
     */
    private val onStateChange: ((section: DynamicSection, item: SectionItem, newValue: Any) -> Unit)? = null,
    /**
     * call 方法的回调（用于执行其他 item 的 JS 效果）
     * 在 runtime 内部执行，需要正确的上下文
     */
    private val onCallJs: (suspend ScriptRuntime.(targetItemId: String) -> JsAny?)? = null,
) {

    val js = Object {
        listOf("findById".js, "查找".js).func("id") { args ->
            val id = toString(args[0])
            findItemGlobally(id) ?: findSectionById(id)
        }
        listOf("version".js, "版本".js) eq buildVersion()
        listOf("all".js, "全部".js) eq buildAll()
        listOf("gameActivity".js, "游戏界面".js) eq (config.gameActivity ?: "").js

        buildItemProperty(item,section,sectionStates[section?.id])
        
        // 设置组件值的方法
        listOf("setValue".js, "设置值".js).func("itemId", "value") { args ->
            val itemId = toString(args[0])
            val value = args[1]
            setItemValue(itemId, value)
            Undefined
        }

        // 调用另一个栏目项的 JS 效果（类似于点击按钮）
        listOf("call".js, "调用".js).func("itemId") { args ->
            val itemId = toString(args[0])
            onCallJs?.invoke(this,itemId)
        }

        // 刷新界面
        listOf("refresh".js, "刷新".js).func {
            PvzLocalSaveManager.triggerRefresh()
            Undefined
        }
    }
    
    /**
     * 设置栏目项的值
     */
    private fun ScriptRuntime.setItemValue(itemId: String, value: JsAny?): Boolean {
        if (value == null) {
            JsConsole.warn("setValue: 未传入值或Null无法设置")
            return false
        }
        // 先查找 item
        val itemPair = findItemById(itemId)
        if (itemPair == null) {
            JsConsole.warn("setValue: 找不到 item: $itemId")
            return false
        }
        val (section, item) = itemPair

        // 调用回调同步状态
        onStateChange?.invoke(section, item, value.toKotlin(this))
        return true
    }
    
    /**
     * 查找指定栏目
     */
    private fun findSectionById(sectionId: String): JsAny? {
        val section = config.sections.find { it.id == sectionId } ?: return null
        val state = sectionStates[section.id]

        return buildSectionObject(section, state)
    }

    /**
     * 全局查找指定项（通过 itemId）
     */
    private fun findItemGlobally(itemId: String): JsAny? {
        for (section in config.sections) {
            val item = section.items.find { it.id == itemId }
            if (item != null) {
                val state = sectionStates[section.id]
                return buildItemObject(section, item, state)
            }
        }
        return null
    }

    /**
     * 构建版本对象
     */
    private fun buildVersion(): JsObject {
        return Object("version") {
            listOf("id".js, "编号".js) eq version.id.js
            listOf("name".js, "名称".js) eq version.name.js
            listOf("baseAssetPath".js, "基础资源路径".js) eq version.baseAssetPath?.js
            listOf("assetPath".js, "资源路径".js) eq version.resolveAssetPath().js
            listOf("forceOverride".js, "强制覆盖".js) eq version.forceOverride.js
        }
    }

    /**
     * 构建 all 对象（所有栏目）
     */
    private fun buildAll(): JsObject = Object {
        for (section in config.sections) {
            val state = sectionStates[section.id]
            section.id.js eq buildSectionObject(section, state)
        }
    }
    
    /**
     * 构建栏目对象
     */
    private fun buildSectionObject(section: DynamicSection, state: DynamicSectionState?): JsObject {
        val items = section.items.map { item -> item.id.js to buildItemObject(section, item, state) }
        return Object(section.id) {
            // 栏目基本信息
            listOf("id".js, "编号".js) eq section.id.js
            listOf("title".js, "标题".js) eq section.title.js
            listOf("theme".js, "主题".js) eq section.theme.name.js
            listOf("targetPath".js, "目标路径".js) eq (section.resolveTargetDirectory().absolutePath).js
            
            // 栏目下所有项
            listOf("items".js, "项目".js) eq Object {
                items.forEach { (key, value) -> key eq value }
            }

            items.forEach { (key, value) -> key eq value }
            
            // 栏目的整体状态
            listOf("checkedItems".js, "选中项".js) eq Object("checkedItems") {
                for (itemId in state?.checkedItemIds ?: emptySet()) {
                    val item = section.items.find { it.id == itemId }
                    if (item != null) {
                        item.id.js eq true.js
                    }
                }
            }
            
            listOf("sliderValues".js, "滑块值".js) eq Object("sliderValues") {
                for ((itemId, value) in state?.sliderValues ?: emptyMap()) {
                    itemId.js eq value.js
                }
            }
            
            listOf("inputValues".js, "输入值".js) eq Object("inputValues") {
                for ((itemId, value) in state?.inputValues ?: emptyMap()) {
                    itemId.js eq value.js
                }
            }

            listOf("infoValues".js, "信息值".js) eq Object("infoValues") {
                for ((itemId, value) in state?.infoValues ?: emptyMap()) {
                    itemId.js eq value.js
                }
            }

            listOf("descriptionValues".js, "描述值".js) eq Object("descriptionValues") {
                for ((itemId, value) in state?.descriptionValues ?: emptyMap()) {
                    itemId.js eq value.js
                }
            }
        }
    }
    
    /**
     * 构建栏目项对象
     */
    private fun buildItemObject(
        section: DynamicSection,
        item: SectionItem,
        state: DynamicSectionState?
    ): JsObject {
        val obj = Object(item.id) {
            // 基本配置信息
            listOf("id".js, "编号".js) eq item.id.js
            listOf("name".js, "名称".js) eq (item.name ?: "").js
            listOf("desc".js, "描述".js) eq (item.desc ?: "").js
            listOf("type".js, "类型".js) eq item.type.name.js
            listOf("icon".js, "图标".js) eq (item.icon ?: "").js
            listOf("assetPath".js, "资源路径".js) eq (item.assetPath ?: "").js
            listOf("groupId".js, "分组".js) eq item.groupId.js
            listOf("displayName".js, "显示名".js) eq item.displayName.js
            
            // 类型特定的配置（可读写属性）
            buildItemProperty(item,section,state)
            listOf("call".js, "执行".js).func {
                onCallJs?.invoke(this,item.id)
            }
            
            // 父栏目引用
            listOf("sectionId".js, "栏目编号".js) eq section.id.js
            listOf("sectionTitle".js, "栏目标题".js) eq section.title.js
            
            // 解析后的完整 assetPath
            listOf("resolvedPath".js, "解析后路径".js) eq item.resolvePath(section, version).js
        }
        return obj
    }

    private fun ObjectScope.buildItemProperty(item: SectionItem?,section: DynamicSection?,state: DynamicSectionState?) {
        if (item != null && section != null && state != null) when (item.type) {
            SectionType.CHECKBOX -> listOf("checked".js, "选中".js,"勾选".js) eq JsPropertyAccessor.BackedField(
                getter = Callable { state.checkedItemIds.contains(item.id).js },
                setter = Callable { args ->
                    val jsValue = args.getOrNull(0) ?: return@Callable Undefined
                    val newChecked = jsValue.toKotlin(this) as Boolean
                    onStateChange?.invoke(section, item, newChecked)
                    Undefined
                }
            )

            SectionType.RADIO -> {
                listOf("selected".js,"checked".js, "选中".js) eq JsPropertyAccessor.BackedField(
                    getter = Callable { (state.selectedPresetItemIds[item.groupId] == item.id).js },
                    setter = Callable { args ->
                        val jsValue = args.getOrNull(0) ?: return@Callable Undefined
                        val newValue = toNumber(jsValue).toFloat()
                        onStateChange?.invoke(section, item, newValue)
                        Undefined
                    }
                )
            }
            SectionType.SLIDER -> {
                // value 属性支持 getter/setter
                listOf("value".js, "值".js) eq JsPropertyAccessor.BackedField(
                    getter = Callable { (state.sliderValues[item.id] ?: (item.defaultValue ?: item.minValue)).js },
                    setter = Callable { args ->
                        val jsValue = args.getOrNull(0) ?: return@Callable Undefined
                        val newValue = toNumber(jsValue).toFloat()
                        onStateChange?.invoke(section, item, newValue)
                        Undefined
                    }
                )
                listOf("minValue".js, "最小值".js) eq item.minValue.js
                listOf("maxValue".js, "最大值".js) eq item.maxValue.js
                listOf("step".js, "步长".js) eq item.step.js
                listOf("valueSuffix".js, "值后缀".js) eq (item.valueSuffix ?: "").js
            }
            SectionType.INPUT -> {
                // value 属性支持 getter/setter
                listOf("value".js, "值".js) eq JsPropertyAccessor.BackedField(
                    getter = Callable { (state.inputValues[item.id] ?: item.inputDefault ?: "").js },
                    setter = Callable { args ->
                        val jsValue = args.getOrNull(0) ?: return@Callable Undefined
                        val newValue = toString(jsValue)
                        onStateChange?.invoke(section, item, newValue)
                        Undefined
                    }
                )
                listOf("inputDefault".js, "默认输入".js) eq (item.inputDefault ?: "").js
                listOf("placeholder".js, "占位符".js) eq (item.placeholder ?: "").js
            }

            SectionType.INFO -> {
                listOf("value".js, "值".js) eq JsPropertyAccessor.BackedField(
                    getter = Callable { (state.infoValues[item.id] ?: item.infoValue ?: "-").js },
                    setter = Callable { args ->
                        val jsValue = args.getOrNull(0) ?: return@Callable Undefined
                        val newValue = toString(jsValue)
                        onStateChange?.invoke(section, item, newValue)
                        Undefined
                    }
                )
            }

            SectionType.DESCRIPTION -> {
                listOf("value".js, "值".js) eq JsPropertyAccessor.BackedField(
                    getter = Callable { (state.descriptionValues[item.id] ?: item.desc.orEmpty().ifEmpty { item.name } ?: "-").js },
                    setter = Callable { args ->
                        val jsValue = args.getOrNull(0) ?: return@Callable Undefined
                        val newValue = toString(jsValue)
                        onStateChange?.invoke(section, item, newValue)
                        Undefined
                    }
                )
            }

            else -> {}
        }
    }
    
    companion object {
        /**
         * 查找指定 ID 的栏目
         */
        fun findSectionById(sectionId: String): DynamicSection? {
            return InitializePvz2.config.sections.find { it.id == sectionId }
        }
        
        /**
         * 全局查找指定 ID 的栏目项
         */
        fun findItemById(itemId: String): Pair<DynamicSection, SectionItem>? {
            for (section in InitializePvz2.config.sections) {
                val item = section.items.find { it.id == itemId }
                if (item != null) {
                    return section to item
                }
            }
            return null
        }
    }
}