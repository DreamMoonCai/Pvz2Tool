### 配置文件属性说明

#### 根级配置
| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `gameActivity` | String | 是 | 游戏主 Activity 完整类名，如 `com.popcap.pvz2cmhd.SexyAppActivity` |
| `smfDirectory` | String | 否 | 游戏默认 SMF 存放目录，默认 `files/`；当 section 不填写 `targetPath` 时使用此目录 |
| `versions` | Array | 是 | 版本配置数组，至少配置 1 个版本 |
| `sections` | Array | 否 | 功能栏目配置数组，不配置则无功能项展示 |
| `isExpandedVersions` | Boolean | 否 | 版本管理面板是否默认展开，默认 `false` |
| `versionsTheme` | String | 否 | 版本管理面板主题颜色，默认 `BROWN`；可选值见下方主题颜色列表 |
| `announcement` | Array | 否 | 公告列表，每项包含 `title` 和 `content` |
| `ui` | Object | 否 | UI 文案配置（详见"UI 配置"章节） |
| `localConfigFile` | String | 否 | 本地配置文件路径（逐步淘汰，不推荐使用） |

**主题颜色可选值（BROWN | BLUE | BLUE_BACKGROUND | GREEN | GREEN_BACKGROUND | RED | PURPLE | PURPLE_BACKGROUND | ORANGE | TEAL | TEAL_BACKGROUND | GOLD | GRAY | GRAY_BACKGROUND）**：
- 带 `_BACKGROUND` 后缀 = 深色背景 + 亮色文字（适合深色风格）
- 不带后缀 = 浅色背景 + 深色文字（默认）

---

#### 版本配置（versions）
| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `versions[n].id` | String | 是 | 版本唯一标识 ID，不可重复 |
| `versions[n].name` | String | 是 | 版本展示名称 |
| `versions[n].desc` | String | 是 | 版本描述文本 |
| `versions[n].icon` | String | 否 | 版本图标路径，基于 `pvz2tool/images/` 目录 |
| `versions[n].default` | Boolean | 否 | 是否为默认选中版本，仅第一个设为 `true` 的版本生效，默认 `false` |
| `versions[n].baseAssetPath` | String | 否 | 基础资源路径，默认 `version/base/smf`；无需基础资源时填写 `null`（不带引号） |
| `versions[n].assetPath` | String | 否 | 该版本的核心资源路径，默认 `version/版本ID/smf` |
| `versions[n].forceOverride` | Boolean | 否 | 版本级强制覆盖开关，开启后该版本所有资源解压时会强制覆盖目标文件，可突破"即时模式文件保护"逻辑 |
| `versions[n].enterGameScript` | String | 否 | 进入游戏前执行的 JS 脚本（版本级）；仅在选中该版本时触发，优先级高于根级 `enterGameScript` |
| `versions[n].enterGamePath` | String | 否 | 进入游戏前执行的 JS 脚本文件路径（版本级）；`enterGameScript` 为空时从该路径加载；默认路径 `version/版本ID/main.js`；文件不存在且 `enterGameScript` 也为空则不执行 |

**游戏存档版本隔离**：
- 不同版本的游戏存档完全隔离，互不影响
- 存档存储在应用私有目录下的 `version_saves/版本ID/` 目录下（如 `Android/data/包名/files/version_saves/版本ID/`）
- 切换版本时的行为：
  1. 将当前游戏存档备份到旧版本隔离目录
  2. 从新版本隔离目录恢复存档（如果存在）
  3. 如果新版本没有存档，则删除当前游戏存档
- 本地存档（工具自己保存的存档）不受影响，始终在 `saves/` 目录下

---

#### 功能栏目配置（sections）
| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `sections[n].id` | String | 是 | 功能栏唯一标识 ID，不可重复；**特殊值：`saves` 自动渲染本地存档+预设存档一体化功能** |
| `sections[n].title` | String | 是 | 功能栏展示标题 |
| `sections[n].visibleOnVersionIds` | Array | 否 | 配置该栏目在哪些版本下显示，不配置则所有版本可见，如 `["old","new"]` |
| `sections[n].targetPath` | String | 否 | 资源目标目录路径，不配置则默认使用 `smfDirectory` |
| `sections[n].addItems` | Boolean | 否 | **本地配置专属**：是否追加模式；`true` 追加到 APK 内置配置同 ID 栏目；`false`（默认）替换 |
| `sections[n].isExpanded` | Boolean | 否 | 是否默认展开该功能栏，默认 `false` |
| `sections[n].confirmButtonText` | String | 否 | 保存按钮文字，默认 `null`（不显示按钮） |
| `sections[n].theme` | String | 否 | 栏目主题颜色，默认 `BROWN`；可选值见主题颜色列表 |
| `sections[n].jsScript` | String | 否 | 栏目级 JS 脚本；`confirmButtonText` 存在时点击按钮后执行，否则进入游戏时执行 |
| `sections[n].jsPath` | String | 否 | 栏目级 JS 脚本文件路径；`jsScript` 为空时从该路径加载；默认路径 `version/版本ID/栏目ID/main.js`；文件不存在且 `jsScript` 也为空则不执行 |

**功能栏类型（type）**：
| 类型 | 说明 |
|------|------|
| `RADIO` | 单选栏，该栏下所有功能项仅能选中 1 个 |
| `CHECKBOX` | 多选栏，该栏下功能项可独立开关 |
| `DESCRIPTION` | 描述栏，展示文本内容，支持 JS 执行（JS 执行返回值作为显示内容），无交互功能 |
| `BUTTON` | 按钮栏，每个功能项渲染为按钮，点击后执行 JS 脚本 |
| `SLIDER` | 滑动条栏，用于数值调节（需配合 jsScript 使用） |
| `INPUT` | 文本输入栏，用于文本输入（需配合 jsScript 使用） |
| `INFO` | 只读信息栏，用于展示动态信息（JS 执行返回值显示在信息框中） |

---

#### 功能项配置（sections[0].items）
| 属性 | 类型 | 适用类型 | 说明 |
|------|------|----------|------|
| `items[n].id` | String | 全部 | 功能项唯一标识 ID，不可重复 |
| `items[n].type` | String | 全部 | **必填**，决定渲染类型：`RADIO` \| `CHECKBOX` \| `DESCRIPTION` \| `BUTTON` \| `SLIDER` \| `INPUT` \| `INFO` |
| `items[n].name` | String | 全部 | 功能项展示标题 |
| `items[n].desc` | String | 全部 | 功能项描述文本 |
| `items[n].icon` | String | 全部 | 图标路径，基于 `pvz2tool/images/` 目录 |
| `items[n].assetPath` | String | 全部 | 功能项的资源路径；不配置默认 `version/版本ID/栏目ID/功能项ID`（**注意**：`$SMF` 占位符指向版本目录，而非此字段） |
| `items[n].default` | Boolean | RADIO/CHECKBOX | 是否默认选中/开启 |
| `items[n].jsScript` | String | BUTTON/RADIO/CHECKBOX/SLIDER/INFO | 点击/切换时执行的 JS 脚本，支持 async/await；**当 assetPath 目录下存在 SMF 文件时，脚本可通过 `this.data` 访问并修改 SMF 数据** |
| `items[n].jsPath` | String | BUTTON/RADIO/CHECKBOX/SLIDER/INFO | JS 脚本文件路径；当 `jsScript` 为空时从该路径加载 JS 文件；默认 `version/版本ID/栏目ID/功能项ID/main.js` |
| `items[n].smfList` | Array | BUTTON/RADIO/CHECKBOX | SMF 基名列表，如 `["dynamic"]`；用于 JS 中 `$SMF` 占位符解析和 SMF 数据修改；不配置则不加载 SMF；查找顺序：1. `pvz2tool/version/版本ID/smf/<名称>(.rsb.smf/.obb)` 2. `pvz2tool/<baseAssetPath>/<名称>(.rsb.smf/.obb)`,对于obb文件内置对象名为data.obb |

**按钮类型专属字段（BUTTON）**：
| 属性 | 类型 | 说明 |
|------|------|------|
| `buttonText` | String | 按钮显示文本，不配置时默认使用 `name` |
| `buttonColor` | String | 按钮颜色风格，默认 `blue`；可选值：`blue` \| `red` \| `green` \| `orange` \| `purple` |

**RADIO 类型专属字段**：
| 属性 | 类型 | 说明 |
|------|------|------|
| `groupId` | String | 组件分组 ID，同一 groupId 的 RADIO 互斥；默认为 `root`（全局互斥），设置不同 groupId 可创建多个独立的互斥组 |
| `jsScript` | String | 点击后执行的 JS 脚本 |

**CHECKBOX 类型专属字段**：
| 属性 | 类型 | 说明 |
|------|------|------|
| `jsScript` | String | 切换时执行的 JS 脚本，可通过 `this.checked` 获取当前选中状态 |

**SLIDER 滑动条专属字段**：
| 属性 | 类型 | 说明 |
|------|------|------|
| `minValue` | Float | 滑动条最小值，默认 `0` |
| `maxValue` | Float | 滑动条最大值，默认 `100` |
| `defaultValue` | Float | 滑动条默认值，不配置时取 `minValue` |
| `step` | Float | 滑动条步进值，默认 `1` |
| `valueSuffix` | String | 数值后缀，如 `%`、`倍`、`ms` 等 |
| `jsScript` | String | 拖动结束后执行的 JS 脚本，可通过 `this.value` 获取当前值 |

**INPUT 文本输入专属字段**：
| 属性 | 类型 | 说明 |
|------|------|------|
| `placeholder` | String | 输入框占位提示文字 |
| `inputDefault` | String | 输入框默认值 |

**INFO 只读信息专属字段**：
| 属性 | 类型 | 说明 |
|------|------|------|
| `infoValue` | String | 信息展示的默认值，当 JS 脚本执行后返回空值时显示 |
| `jsScript` | String | **INFO 专属**：首次加载或重新加载时执行的 JS 脚本，返回值显示在信息框中 |
| `jsPath` | String | JS 脚本文件路径；当 `jsScript` 为空时从该路径加载 JS 文件；默认 `version/版本ID/栏目ID/功能项ID/main.js` |

> **INFO 类型 JS 执行时机**：首次加载页面或版本切换时自动执行 JS，脚本返回值作为信息框显示内容。

> **DESCRIPTION 类型 JS 执行时机**：首次加载页面或版本切换时自动执行 JS，脚本返回值作为描述文本显示内容。同时支持在文本中使用 `{{js:表达式}}` 和 `{{js:文件.js}}` 标签动态执行 JS 表达式。

---

#### JS 脚本与组件联动

**JS 脚本执行逻辑**：

**BUTTON/RADIO/CHECKBOX/SLIDER 类型**：点击/切换时执行 JS 脚本。

**INFO 类型**：首次加载页面或版本切换时自动执行 JS，返回值显示在信息框中。

所有类型均按以下优先级执行 JS 脚本：

1. **优先使用 `jsScript`**：如果配置项中填写了 `jsScript`，直接执行该脚本
2. **回退到 `jsPath`**：如果 `jsScript` 为空，尝试从 `jsPath` 指定的文件加载脚本
3. **默认路径**：如果 `jsPath` 也未配置，尝试加载 `version/版本ID/栏目ID/功能项ID/main.js`
4. **静默跳过**：如果文件不存在且 `jsScript` 也为空，则不执行任何 JS 代码

---

**进入游戏时 JS 执行顺序**：

点击"进入游戏"按钮时，按以下顺序依次执行 JS：

1. **版本级 enterGame JS**：执行当前选中版本的 `enterGameScript`；若为空则尝试加载 `enterGamePath`（默认 `version/版本ID/main.js`）
2. **section 级 JS（无 confirmButtonText 的栏目）**：遍历所有可见栏目，对**未配置 confirmButtonText** 的栏目执行其 `jsScript`；若为空则尝试加载 `jsPath`（默认 `version/版本ID/栏目ID/main.js`）

---

**confirmButtonText 按钮的 section 级 JS**：

对于配置了 `confirmButtonText` 的栏目，其 `jsScript`/`jsPath` **不在进入游戏时执行**，而是在用户点击确认按钮后立即执行：

```yaml
sections:
  - id: "my_section"
    title: "我的配置栏"
    confirmButtonText: "应用设置"
    jsScript: |
      // 点击"应用设置"按钮后执行
      console.log(this.all.my_section.some_item.selected)
    # 或者使用文件路径
    # jsPath: "scripts/apply_settings.js"
```

---

**示例**：
```yaml
# 版本配置（版本级 enterGame JS）
versions:
  - id: "v2"
    name: "国际版 2.0"
    desc: "..."
    # 方式1：内联脚本
    enterGameScript: |
      console.log("进入游戏，版本：" + this.version.id)
    # 方式2：文件路径（enterGameScript 为空时自动加载）
    # enterGamePath: "version/v2/main.js"
    # 方式3：不配置，自动尝试加载 version/v2/main.js

# 栏目配置（section 级 JS）
sections:
  # confirmButtonText 存在 → 点击按钮时执行 JS
  - id: "speed"
    title: "速度设置"
    confirmButtonText: "应用速度"
    jsScript: "console.log('已应用速度设置')"

  # confirmButtonText 不存在 → 进入游戏时执行 JS
  - id: "init_data"
    title: "初始化数据"
    # 不配置 jsScript，自动尝试加载 version/v2/init_data/main.js
```

**RADIO/CHECKBOX/SLIDER 组件可配合 `jsScript` 实现状态联动**：

| 组件类型 | JS 中可访问的状态 | 说明 |
|----------|-------------------|------|
| `RADIO` | 无额外状态 | 仅有点击事件，脚本可读取 `this.all` 获取全局状态 |
| `CHECKBOX` | `this.checked` | Boolean 类型，表示当前复选框选中状态 |
| `SLIDER` | `this.value` | Float 类型，表示当前滑动条值 |

**完整 JS 上下文（`jsScript` 中可用）**：
```javascript
// 1. 组件状态（由调用时传入）
this.checked  // CHECKBOX 选中状态 (true/false)
this.value    // SLIDER 当前值 (Float)

// 2. 版本信息
this.version.id              // 版本 ID
this.version.name            // 版本名称
this.version.baseAssetPath    // 基础资源路径
this.version.assetPath       // 资源路径（解析后）
this.version.forceOverride    // 强制覆盖标志

// 3. 所有栏目状态
this.all.栏目ID              // 获取栏目信息
this.all.栏目ID.栏目项ID     // 获取栏目项信息和状态
this.all.栏目ID.targetPath   // 目标目录绝对路径

// 4. 快捷查找
this.findById("栏目ID", "栏目项ID")  // 查找指定栏目项
this.findById.section("栏目ID")      // 查找栏目
this.findById.item("栏目项ID")       // 全局查找栏目项

// 5. 游戏 Activity
this.gameActivity  // 当前配置的 gameActivity 字符串
```

**栏目项可用属性**：
- `id`, `name`, `desc`, `type`, `icon`, `assetPath`, `groupId`
- `selected`（RADIO 选中状态）、`checked`（CHECKBOX 选中状态）、`value`（SLIDER/INPUT 当前值）
- `sectionId`, `sectionTitle`, `resolvedPath`

---

#### SMF 数据修改（高级功能）

当功能项配置了 `jsScript` 或 `jsPath` 时，JS 脚本可以访问并修改 SMF 文件中的数据。

**触发条件**：仅当 item 配置了 `jsScript` 或 `jsPath` 时触发。

**前置流程**：
1. 从版本级 asset 路径 `pvz2tool/version/<versionId>/smf/<名称>(.rsb.smf/.obb)` 或 `pvz2tool/<baseAssetPath>/<名称>(.rsb.smf/.obb)` 扫描所有 `.smf` 文件（由 item 的 `smfList` 指定基名）
2. 提取 SMF 文件到缓存目录并解包
3. 构建 `this.data` 对象，暴露解包后的文件结构

**`this.data` 对象结构**：
```javascript
// 假设 assetPath 下有 dynamic.rsb.smf，解包到 extracted/dynamic/
// 文件结构：extracted/dynamic/PROPERTIES/RESOURCESUICOMMON.JSON

// RTON/JSON 文件：可调用 .load() 同步加载
var obj = this.data.dynamic.properties.resourcesuicommon.load();
obj.someField = "modified";
obj.save();  // 保存修改

// 普通文件：路径字符串
var path = this.data.dynamic.properties.resourcesuicommon2;
// path = "/data/user/0/.../cache/pvz2tool/js_smf_cache/<versionId>/<smfFileName>/extracted/dynamic/PROPERTIES/RESOURCESUICOMMON2.WAM"
```

**文件路径映射规则**：
- SMF 文件名 `dynamic.rsb.smf` → `this.data.dynamic`
- 解包目录内的子目录保持层级
- 文件名（不含扩展名）转为小写作为属性名
- `.rton` / `.json` 后缀文件：注入 `load()` 和 `save()` 方法
- 其他文件：直接暴露为路径字符串

**执行流程**：
1. 提取并解包 SMF → 构建 `this.data` 对象
2. 执行 JS 脚本（脚本可通过 `this.data` 读写 SMF 数据）
3. 对比修改前后的文件差异
4. 若有修改：重新打包 SMF → 替换进入游戏时的解压源文件
5. 若无修改：跳过打包，保持原 SMF 不变

**缓存目录**：`context.cacheDir/pvz2tool/js_smf_cache/<versionId>/<smfFileName>/`
- `original/`：原始 SMF 文件（版本级共享，同版本不同 item 共用）
- `extracted/`：解包后的目录结构
- `modified/`：JS 修改后重新打包的 SMF 文件

**配置示例**：
```yaml
items:
  - id: "modify_coin"
    type: "RADIO"
    groupId: "coin_mode"
    default: true
    # 指定需要的 SMF 基名列表（对应 <名称>.rsb.smf）
    smfList:
      - "dynamic"
    jsScript: |
      // 读取 SMF 中的 RTON 文件
      var coinData = this.data.dynamic.packages.npcs.load();
      coinData.coin_amount = 9999;
      coinData.save();
      "金币修改完成"
```

**`smfList` 说明**：
- 填写需要的 SMF 基名列表，如 `["dynamic"]`
- 系统自动处理：查找 `pvz2tool/version/<versionId>/smf/<名称>(.rsb.smf/.obb)`，若不存在则回退 `pvz2tool/<baseAssetPath>/<名称>(.rsb.smf/.obb)`
- 同版本不同 item 共享同一份解包缓存（按 SMF 基名隔离）

---

#### UI 配置（ui）

UI 文案配置结构：
```yaml
ui:
  title:           # 标题配置
  button:          # 按钮配置
  extractor:       # 解压进度弹窗配置
  save:            # 存档操作弹窗配置
  settings:        # 设置弹窗配置
  log:             # JS 日志面板配置
  dialog:          # 通用对话框按钮配置
  error:           # 错误提示文案配置
  welcome:         # 欢迎用户组件配置
  assets:          # 功能属性（背景/视频/音乐等，支持 URL）
  sounds:          # 音效文件名映射（支持 URL）
  noValidDirTip:   # 未选择有效目录提示
  versionLabel:    # 版本标签前缀
  uiVersion:       # UI 版本号
  authorInfo:      # 作者信息
  tutorial:        # 教程内容
```

**资源路径 URL 支持说明：**
以下配置字段支持 URL（`http://` 或 `https://`），检测到 URL 则直接使用远程资源：
- `ui.assets.background` — 背景图片
- `ui.assets.backgroundMusic` — 背景音乐
- `ui.assets.cgVideoPath` — CG 开场视频
- `ui.assets.cgVideoLoadTimeout` — CG 开场视频加载超时时间
- `ui.assets.cgVideoPoster` — CG 开场视频加载超时或失败时的海报图片
- `ui.sounds.*` — 所有音效文件

**title 配置详解**：
| 属性 | 类型 | 说明 |
|------|------|------|
| `topAppBar` | String | 顶部应用栏标题 |
| `about` | String | 关于页面标题 |
| `coreFunction` | String | 核心功能页面标题 |
| `versionManage` | String | 版本管理页面标题 |

**button 配置详解**：
| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enterGame` | String | - | 进入游戏按钮文字 |
| `isEnterGameDefaultIcon` | Boolean | `true` | 进入游戏按钮是否使用默认图标 |
| `tutorial` | String | - | 教程按钮文字 |
| `isTutorialDefaultIcon` | Boolean | `true` | 教程按钮是否使用默认图标 |
| `resetData` | String | - | 重置数据按钮文字 |
| `isResetDataDefaultIcon` | Boolean | `true` | 重置数据按钮是否使用默认图标 |
| `disconnectTheNetworkAndStart` | String | `断网启动` | 断网启动按钮文字 |
| `confirmVersion` | String | - | 确认版本按钮文字 |

**extractor 配置详解（资源解压弹窗）**：
| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `dialogTitle` | String | `戴夫的工具箱 | 资源更新` | 解压弹窗标题 |
| `initialLoadingProgressTip` | String | `戴夫正在清点物资清单...` | 初始加载进度提示 |
| `initialProgressTip` | String | `戴夫检测到新的版本波动啦...` | 开始解压进度提示 |
| `noNeedExtractTip` | String | `戴夫检查了工具箱...` | 无需解压提示 |
| `singleFileProcessingTip` | String | `戴夫正在手忙脚乱整理物资：` | 单文件处理提示 |
| `multiFileProcessingTip` | String | `戴夫正在手忙脚乱整理%d个物资：` | 多文件处理提示 |
| `waitingTip` | String | `戴夫正在整理物资...` | 等待提示 |
| `extractCompleteTip` | String | `物资更新完毕！...` | 解压完成提示 |
| `extractFailTipPrefix` | String | `糟糕！戴夫的工具箱出问题了：` | 解压失败提示前缀 |
| `fileSkipTipPrefix` | String | `戴夫检查到「%s」无需更新...` | 文件跳过提示前缀 |
| `continueButtonText` | String | `继续物资准备` | 继续按钮文字 |
| `completeButtonText` | String | `重返战场` | 完成按钮文字 |
| `toastErrorPrefix` | String | `戴夫的小提示：更新失败啦 → ` | Toast 错误前缀 |

**save 配置详解（存档操作）**：
| 属性 | 类型 | 说明 |
|------|------|------|
| `presetConfirmTitle` | String | 预设存档确认弹窗标题 |
| `presetConfirmMessage` | String | 预设存档确认弹窗内容 |
| `deleteConfirmTitle` | String | 删除存档确认弹窗标题 |
| `deleteConfirmMessage` | String | 删除存档确认弹窗内容 |
| `coverConfirmTitle` | String | 覆盖存档确认弹窗标题 |
| `coverConfirmMessage` | String | 覆盖存档确认弹窗内容 |
| `deleteGameSaveConfirmTitle` | String | 删除游玩存档确认弹窗标题 |
| `deleteGameSaveConfirmMessage` | String | 删除游玩存档确认弹窗内容 |
| `saveInfoTitle` | String | 存档信息标题 |
| `saveNameLabel` | String | 存档名称标签 |
| `saveDescLabel` | String | 存档描述标签 |
| `cancelButton` | String | 取消按钮 |
| `confirmButton` | String | 确认按钮 |
| `shareButton` | String | 分享按钮 |
| `exportButton` | String | 导出按钮 |
| `importButton` | String | 导入按钮 |
| `backupButton` | String | 备份按钮 |
| `coverLocalButton` | String | 覆盖本地按钮 |
| `deleteGameSaveButton` | String | 删除游玩存档按钮 |
| `coverPresetButton` | String | 覆盖预设按钮 |
| `retryButtonText` | String | 重试按钮文字 |
| `saveNameEmptyTip` | String | 存档名称为空提示 |
| `noLocalSaveTip` | String | 无本地存档提示 |
| `selectLocalSaveTip` | String | 选择本地存档提示 |
| `backupSuccessTip` | String | 备份成功提示 |
| `backupFailTipPrefix` | String | 备份失败提示前缀 |
| `exportSuccessTip` | String | 导出成功提示 |
| `exportFailTipPrefix` | String | 导出失败提示前缀 |
| `importSuccessTip` | String | 导入成功提示 |
| `importFailTipPrefix` | String | 导入失败提示前缀 |
| `deleteSuccessTip` | String | 删除成功提示 |
| `deleteFailTipPrefix` | String | 删除失败提示前缀 |
| `coverSuccessTip` | String | 覆盖成功提示 |
| `coverFailTipPrefix` | String | 覆盖失败提示前缀 |
| `deleteGameSaveSuccessTip` | String | 删除游玩存档成功提示 |
| `deleteGameSaveFailTipPrefix` | String | 删除游玩存档失败提示前缀 |
| `defaultImportNamePrefix` | String | 导入默认名称前缀 |
| `defaultBackupDesc` | String | 自动备份描述 |
| `defaultImportDesc` | String | 手动导入描述 |
| `operation` | Object | 操作类型配置 |

**operation 配置详解**：
| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `backup` | String | - | 备份操作名称 |
| `export` | String | - | 导出操作名称 |
| `import` | String | - | 导入操作名称 |
| `delete` | String | - | 删除操作名称 |
| `deleteGameSave` | String | `删除游玩存档` | 删除游玩存档操作名称 |
| `cover` | String | - | 覆盖操作名称 |
| `saveMeta` | String | - | 保存元数据操作名称 |

**assets 配置详解：**
| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `background` | String | `bg_main.jpg` | 背景图片路径（支持 URL） |
| `isUseSolidColorBackground` | Boolean | `true` | 是否使用纯色背景（false 则使用 background 图片） |
| `backgroundMusic` | String | `bg_music.wav` | 背景音乐文件名（支持 URL） |
| `isPlayBackgroundMusic` | Boolean | `true` | 是否默认播放背景音乐 |
| `cgVideoPath` | String | `opening.mp4` | CG 开场视频路径，相对于 `video/` 目录（支持 URL） |
| `cgVideoLoadTimeout` | Long | `5000` | CG 开场视频超时时间，默认5秒超时 |
| `cgVideoPoster` | String | null | CG 开场视频加载超时或失败时的海报图片 |

**sounds 配置详解：**
| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `switchClickPress` | String | `ui_switch_click_press.wav` | 开关按下音效 |
| `switchClickRelease` | String | `ui_switch_click_release.wav` | 开关释放音效 |
| `switchClick` | String | `ui_switch_click.wav` | 开关单次点击音效 |
| `buttonClickPress` | String | `ui_button_click_press.wav` | 普通按钮按下音效 |
| `buttonClickRelease` | String | `ui_button_click_release.wav` | 普通按钮释放音效 |
| `buttonSettingsPress` | String | `ui_button_settings_press.wav` | 设置按钮按下音效 |
| `buttonSettingsRelease` | String | `ui_button_settings_release.wav` | 设置按钮释放音效 |
| `buttonXClosePress` | String | `ui_button_x_close_press.wav` | 关闭按钮按下音效 |
| `buttonXCloseRelease` | String | `ui_button_x_close_release.wav` | 关闭按钮释放音效 |
| `collapsiblePanelPress` | String | `ui_collapsible_panel_click_press.wav` | 可折叠面板按下音效 |
| `collapsiblePanelRelease` | String | `ui_collapsible_panel_click_release.wav` | 可折叠面板释放音效 |

**log 配置详解（JS 日志面板）：**
| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `panelTitle` | String | `JS 日志` | 日志面板标题 |
| `copyLogDesc` | String | `复制日志` | 复制日志按钮的无障碍描述 |
| `clearLogDesc` | String | `清空日志` | 清空日志按钮的无障碍描述 |
| `noLogText` | String | `暂无日志` | 无日志时的占位文本 |
| `presetSaveLabel` | String | `预设存档` | 预设存档区域标题 |
| `localSaveLabel` | String | `本地存档` | 本地存档区域标题 |

**dialog 配置详解（通用对话框按钮）：**
| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `cancel` | String | `取消` | 通用对话框取消按钮 |
| `confirm` | String | `确定` | 通用对话框确定按钮 |
| `deleteSaveDesc` | String | `删除存档` | 存档列表删除按钮无障碍描述 |
| `editUserNameDesc` | String | `编辑用户名` | 编辑用户名按钮无障碍描述 |
| `shareSaveChooserTitle` | String | `分享 PVZ2 存档` | 分享存档时系统分享弹窗标题 |
| `sharePackFailedTip` | String | `打包存档失败` | 分享存档失败提示 |
| `noShareableSaveTip` | String | `没有可分享的本地存档` | 无可分享存档提示 |

**error 配置详解（错误提示文案）：**
| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `jsExecuteErrorTitle` | String | `JS 执行出错` | JS 执行出错弹窗标题 |
| `gameActivityInvalid` | String | `设置的游戏Activity有误或不存在` | 游戏 Activity 设置有误提示 |
| `unknownError` | String | `未知错误` | 异常消息为空时的兜底文字 |

**welcome 配置详解（欢迎用户组件）：**
| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `greetingTemplate` | String | `欢迎您，%s` | 欢迎语模板，%s 替换为用户名 |
| `editUserNameTitle` | String | `修改用户名` | 编辑用户名弹窗标题 |
| `editUserNameHint` | String | `请输入新的用户名` | 编辑用户名输入框提示文字 |

**settings 配置详解（设置弹窗）：**
| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `title` | String | - | 设置弹窗标题 |
| `solidBackgroundMode` | String | - | 纯色背景模式 |
| `changeTheProfileReadLocation` | String | - | 更改配置读取位置 |
| `reloadConfig` | String | - | 重新加载配置 |
| `playBackgroundMusic` | String | `播放背景音乐` | 播放背景音乐 |
| `resetPacketDeepClearing` | String | `重置数据包时删除smf目录` | 重置数据包时删除smf目录 |
| `showNotUpdate` | String | `进入游戏时未检测到更新也进行弹窗` | 进入游戏时未检测到更新也弹窗 |
| `importSmfFile` | String | `导入SMF文件` | 导入SMF文件按钮 |

所有文本类配置均支持**复合颜色样式**，语法：

- **颜色与阴影**：
  - 格式：`{{colorName:content}}` 或 `{{colorName-shadow:content}}`
  - 示例：`{{green-shadow:松间烬雪}}` 会显示为带深绿阴影的绿色文字。

- **交互式链接 (Link)**：
  - **基础格式**：`{{link|URL:显示文本}}`。
  - **样式化链接**：`{{link-样式名|URL:显示文本}}`。
  - **实现逻辑**：系统会提取 `link-` 后的样式名并应用对应的颜色和阴影，同时保留下划线和点击跳转功能。
  - 示例：`{{link-gold-shadow|https://pvz2.com:前往官网}}`。

- **行内图标 (Icon)**：
  - **格式**：`{{icon:文件名|width=宽度|height=高度}}`（`width`/`height` 可选，单位 dp）。
  - **参数**：
    - `文件名`：`pvz2tool/images/` 下的图片文件名。
    - `width=宽度`（可选）：图片宽度，单位 dp，默认 `fontSize * 1.2`。
    - `height=高度`（可选）：图片高度，单位 dp，默认 `fontSize * 1.2`。
  - **自动化处理**：组件会自动扫描文本中的 `icon` 标签，并尝试从 `${Pvz2ToolConfig.PATH_NAME}/images/文件名` 加载图片。
  - **视觉对齐**：图标大小会随字体大小（fontSize）自动缩放（约 1.2 倍），并保持垂直居中。
  - **示例**：
    - `消耗 {{icon:sun.png}} 50 点阳光`（默认尺寸）。
    - `{{icon:coin.png|width=32|height=32}}`（32dp 正方形图标）。

- **JS 表达式 (JS)**：
  - **作用**：执行一段 JavaScript 表达式，并将结果插入到文本中。
  - **格式**：`{{js:表达式}}`/`{{js:JS路径}}`。
  - **参数**：
    - `JS路径`：`pvz2tool/js/` 下的图片文件名。
  - **递归解析**：JS 表达式的返回值如果包含 `{{...}}` 复合文本标签，会自动递归解析（支持颜色标签、链接标签、图标标签、嵌套 `{{js:...}}` 标签等）。
  - **示例**：
    - `{{js:test.js}}` → 执行`pvz2tool/js/test.js`并显示返回结果。
    - `{{js:1 + 2}}` → 显示为 `3`。
    - `{{js:new Date().getFullYear()}}` → 显示为当前年份。
    - `{{js: '状态：' + (hp > 0 ? '{{green:存活}}' : '{{red:阵亡}}') }}` → JS 返回的字符串中含有 `{{green:...}}` / `{{red:...}}` 标签，会被继续解析为对应颜色的文本。
    - `{{js: '道具：' + itemIcon }}`（其中 `itemIcon = '{{icon:sword.png}}'`）→ JS 返回的字符串中含有 `{{icon:...}}` 标签，会被继续解析并渲染为图标。

---

### JS 脚本内置对象

`BUTTON` 类型栏目的 `jsScript` 脚本运行在 KeightJS 沙箱环境中，可使用以下全局对象。

---

#### `path` — 路径工具

工作模式与微信小程序一致：使用**占位符前缀**代替硬编码绝对路径，运行时统一解析。

所有路径属性在 JS 读取时**动态解析**为实际可读写的 File 绝对路径。

##### 快捷路径子对象

可直接读取的子对象，属性值均为通过 `JsFileResolver` 动态解析的**实际绝对路径字符串**：

```js
path.app.data   // → /data/data/io.github.dreammooncai.pvz2tool
path.app.files   // → /data/data/io.github.dreammooncai.pvz2tool/files
path.app.cache   // → /data/data/io.github.dreammooncai.pvz2tool/cache

path.android.data   // → /sdcard/Android/data
path.android.files  // → /sdcard/Android/data/io.github.dreammooncai.pvz2tool/files
path.android.cache  // → /sdcard/Android/data/io.github.dreammooncai.pvz2tool/cache

path.game.saves  // → <WORK_DIR>/<sections[id="saves"].targetPath>（通过 SAF 解析）

path.pvz2tool.files  // → <WORK_DIR>（工具箱 SAF 配置根目录）
path.pvz2tool.smf    // → <WORK_DIR>/<version.assetPath>（当前选中版本的 SMF 目录）
path.pvz2tool.item    // → <WORK_DIR>/<item.assetPath/version.assetPath>（当前功能所在的 资源 目录）
path.pvz2tool.jsDir    // → <WORK_DIR>/<item.jsPath.dir/section.jsPath.dir>（当前JS的所在目录）
```

> **注意**：`$SMF` 和 `path.pvz2tool.smf` 指向当前选中版本的 SMF 目录（`version.assetPath`），在任何 JS 脚本执行时均可访问。

##### 文件访问机制

所有文件操作（`rton.decode/encode/load/save`、`rsb.pack/unpack`、`ptx.decode/encode`）都通过统一的文件访问层处理：

| 场景 | 处理方式 |
|------|----------|
| **工作目录未选择** | 自动回退到 `asset` 目录读取 |
| **输入文件** | DocumentFile → 尝试转 File；如果是 asset/无权限 → 复制到缓存 → 使用缓存 File |
| **输出文件** | DocumentFile 无写入权限 → 先输出到缓存 → 再自动复制回 DocumentFile |

**`$SMF` 的降级规则**：当版本目录不存在时，自动回退到 `baseAssetPath`：
1. 优先查找 `pvz2tool/<version.assetPath>`（版本专属目录）
2. 版本目录不存在时，回退到 `pvz2tool/<version.baseAssetPath>`（基础资源目录，默认为 `pvz2tool/version/base/smf`）

当 workDir 未选择时，`$SMF` 自动从 asset 目录解析，asset 目录会按需复制到缓存。

这意味着你可以直接使用占位符路径，无需关心底层是 SAF DocumentFile、普通 File 还是 asset：

```js
// 即使工作目录未选择，也能从 asset 读取
rton.load("$SMF/default.rton");

// 即使目标目录无写入权限，也能正常保存
obj.save();  // 自动处理缓存和复制
```

##### 路径解析函数

```js
// 将占位符路径解析为实际绝对路径字符串
// $GAME_SAVES 从 sections[id="saves"].targetPath 解析
// $SMF 从当前选中版本的 assetPath 解析
var absPath = path.resolve("$GAME_SAVES/UserData.rton");
var smfRton = path.resolve("$SMF/main.rton");  // 指向版本 SMF 目录

// 解析为 DocumentFile URI 字符串（用于 SAF 操作）
var uri = path.resolveUri("$WORK_DIR/config.json");
// → "content://com.android.externalstorage.documents/tree/primary:Android/data/..."
```

##### 占位符前缀说明

| 占位符 | 说明 | 解析结果 |
|--------|------|----------|
| `$WORK_DIR` | 用户选择的本地工作目录（pvz2tool 配置根目录） | SAF URI 解析后的 File 路径 |
| `$GAME_DATA` | 游戏包外部存储根目录 | `/sdcard/Android/data/<package>` |
| `$GAME_FILES` | 游戏包外部存储 files 目录 | `<package>/files/` |
| `$GAME_CACHE` | 游戏包外部存储 cache 目录 | `<package>/cache/` |
| `$GAME_SAVES` | 游戏存档目录（从 `sections[id="saves"].targetPath` 动态读取） | `<WORK_DIR>/<targetPath>` |
| `$GAME_SMF` | 游戏目录 SMF（rootDirectory + smfDirectory） | `<rootDirectory>/<smfDirectory>` |
| `$SMF` | 当前选中版本的 SMF 目录（基于 version.assetPath） | `<WORK_DIR>/<version.assetPath>` |
| `$ITEM` | 栏目下的 item assetPath，降级到 `$SMF` | `<WORK_DIR>/<item.assetPath>` |
| `$JS_DIR` | 栏目下的 item jsPath 所在目录，逐级递升 | `<WORK_DIR>/<item.jsPath.dir>` |
| `$APP_FILES` | 工具箱应用外部 files 目录 | `/sdcard/Android/data/<app_package>/files` |


