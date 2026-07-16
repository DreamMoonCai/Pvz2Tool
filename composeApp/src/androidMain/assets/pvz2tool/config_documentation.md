### 配置文件属性说明

#### 根级配置
| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `gameActivity` | String | 是 | 游戏主 Activity 完整类名，如 `com.popcap.pvz2cmhd.SexyAppActivity` |
| `smfDirectory` | String | 是 | 游戏默认 SMF 存放目录，如 `files/`；当 section 不填写 `targetPath` 时使用此目录 |
| `versions` | Array | 是 | 版本配置数组，至少配置 1 个版本 |
| `sections` | Array | 是 | 功能栏目配置数组（精简模式下可为空列表 `[]`） |
| `isExpandedVersions` | Boolean | 否 | 版本管理面板是否默认展开，默认 `false` |
| `versionsTheme` | String | 否 | 版本管理面板主题颜色，默认 `BROWN`；可选值见下方主题颜色列表 |
| `announcement` | Array | 是 | 公告列表（精简模式下可为空列表 `[]`），每项包含 `title` 和 `content` |
| `ui` | Object | 是 | UI 文案配置（精简模式下可省略，详见"UI 配置"章节） |
| `localConfigFile` | String | 否 | 本地配置文件路径（逐步淘汰，不推荐使用） |
| `simplifiedLaunch` | Boolean | 否 | 精简模式开关，默认 `false`；开启后跳过完整主界面，CG 开场视频正常播放，之后只解压 base 资源并直接进入游戏 |

#### 精简配置模式

当 `simplifiedLaunch: true` 时，系统使用精简配置类进行解析，**只需以下字段**，其余字段（`versions`、`sections`、`ui.title/button/save/settings`、`announcement` 等）均不需要：

| 属性 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `gameActivity` | String | 是 | — | 游戏主 Activity 完整类名 |
| `simplifiedLaunch` | Boolean | 是 | — | 必须为 `true` |
| `smfDirectory` | String | 否 | `files/` | 游戏 SMF 存放目录 |
| `baseAssetPath` | String | 否 | `version/base/smf` | base 资源路径 |
| `cgVideoPath` | String | 否 | `opening.mp4` | CG 开场视频路径 |
| `cgVideoPoster` | String | 否 | `null` | CG 视频超时海报图 |
| `cgVideoLoadTimeout` | Long | 否 | `5000` | CG 视频超时毫秒 |
| `gameActivityInvalid` | String | 否 | `设置的游戏Activity有误或不存在` | Activity 错误提示 |

**精简配置示例**：
```yaml
gameActivity: com.popcap.pvz2cmhd.SexyAppActivity
simplifiedLaunch: true
# baseAssetPath: version/base/smf  # 可选，自定义 base 资源路径
# smfDirectory: files/              # 可选，自定义 SMF 目录
# cgVideoPath: opening.mp4          # 可选，CG 视频路径
# cgVideoPoster: poster.jpg         # 可选，CG 超时海报图
# cgVideoLoadTimeout: 5000          # 可选，CG 超时毫秒
# gameActivityInvalid: "..."        # 可选，Activity 错误提示
```

> **注意**：精简模式下 CG 开场视频仍会正常播放（如有版本变化），之后直接跳过主界面进入游戏。

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
| `versions[n].icon` | String | 否 | 版本图标路径，基于 `pvz2tool/images/` 目录；支持绝对路径（`/` 开头）和占位符变量 |
| `versions[n].default` | Boolean | 否 | 是否为默认选中版本，仅第一个设为 `true` 的版本生效，默认 `false` |
| `versions[n].baseAssetPath` | String | 否 | 基础资源路径，默认 `version/base/smf`；无需基础资源时填写 `null`（不带引号） |
| `versions[n].assetPath` | String | 否 | 该版本的核心资源路径，默认 `version/版本ID/smf`；支持占位符变量 |
| `versions[n].forceOverride` | Boolean | 否 | 版本级强制覆盖开关，开启后该版本所有资源解压时会强制覆盖目标文件，可突破"即时模式文件保护"逻辑 |
| `versions[n].enterGameScript` | String | 否 | 进入游戏前执行的 JS 脚本（版本级）；仅在选中该版本时触发，优先级高于根级 `enterGameScript` |
| `versions[n].enterGamePath` | String | 否 | 进入游戏前执行的 JS 脚本文件路径（版本级）；`enterGameScript` 为空时从该路径加载；默认路径 `version/版本ID/main.js`；文件不存在且 `enterGameScript` 也为空则不执行；支持占位符变量 |

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
| `sections[n].targetPath` | String | 否 | 资源目标目录路径，不配置则默认使用 `smfDirectory`；支持占位符变量（如 `$ANDROID_FILES/pvz2_tools`） |
| `sections[n].addItems` | Boolean | 否 | **本地配置专属**：是否追加模式；`true` 追加到 APK 内置配置同 ID 栏目；`false`（默认）替换 |
| `sections[n].isExpanded` | Boolean | 否 | 是否默认展开该功能栏，默认 `false` |
| `sections[n].confirmButtonText` | String | 否 | 保存按钮文字，默认 `null`（不显示按钮） |
| `sections[n].descriptionContent` | String | 否 | DESCRIPTION 类型栏目的默认显示文本，默认空；JS 脚本返回值会覆盖此内容 |
| `sections[n].theme` | String | 否 | 栏目主题颜色，默认 `BROWN`；可选值见主题颜色列表 |
| `sections[n].jsScript` | String | 否 | 栏目级 JS 脚本；`confirmButtonText` 存在时点击按钮后执行，否则进入游戏时执行 |
| `sections[n].jsPath` | String | 否 | 栏目级 JS 脚本文件路径；`jsScript` 为空时从该路径加载；默认路径 `version/版本ID/栏目ID/main.js`；文件不存在且 `jsScript` 也为空则不执行；支持占位符变量 |

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
| `items[n].icon` | String | 全部 | 图标路径，基于 `pvz2tool/images/` 目录；支持绝对路径（`/` 开头）和占位符变量 |
| `items[n].assetPath` | String | 全部 | 功能项的资源路径；不配置默认 `version/版本ID/栏目ID/功能项ID`（**注意**：`$SMF` 占位符指向版本目录，而非此字段）；支持占位符变量 |
| `items[n].default` | Boolean | RADIO/CHECKBOX | 是否默认选中/开启 |
| `items[n].jsScript` | String | BUTTON/RADIO/CHECKBOX/SLIDER/INFO | 点击/切换时执行的 JS 脚本，支持 async/await；**当 assetPath 目录下存在 SMF 文件时，脚本可通过 `this.data` 访问并修改 SMF 数据** |
| `items[n].jsPath` | String | BUTTON/RADIO/CHECKBOX/SLIDER/INFO | JS 脚本文件路径；当 `jsScript` 为空时从该路径加载 JS 文件；默认 `version/版本ID/栏目ID/功能项ID/main.js`；支持占位符变量 |
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
| `jsPath` | String | JS 脚本文件路径；当 `jsScript` 为空时从该路径加载 JS 文件；默认 `version/版本ID/栏目ID/功能项ID/main.js`；支持占位符变量 |

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

**资源路径 URL、绝对路径与占位符支持说明：**
以下配置字段支持 URL（`http://` 或 `https://`）、绝对路径（以 `/` 开头）和占位符变量，检测到 URL 则直接使用远程资源，检测到绝对路径则直接使用本地文件系统，占位符会自动展开为实际路径：
- `ui.assets.background` — 背景图片
- `ui.assets.backgroundMusic` — 背景音乐
- `ui.assets.cgVideoPath` — CG 开场视频
- `ui.assets.cgVideoLoadTimeout` — CG 开场视频加载超时时间
- `ui.assets.cgVideoPoster` — CG 开场视频加载超时或失败时的海报图片
- `ui.sounds.*` — 所有音效文件
- `versions[n].icon` / `items[n].icon` — 图标路径（以 `/` 开头时为绝对路径）
- `versions[n].assetPath` / `versions[n].enterGamePath` — 资源路径 / JS 脚本路径
- `sections[n].targetPath` — 资源目标目录路径
- `sections[n].jsPath` / `items[n].jsPath` — JS 脚本文件路径
- `items[n].assetPath` — 功能项资源路径

**支持的占位符变量**（详见 `js_documentation.md` 占位符路径章节）：
| 占位符 | 说明 |
|--------|------|
| `$WORK_DIR` | 用户 SAF 工作目录 |
| `$GAME_SAVES` | 游戏存档目录 |
| `$GAME_SMF` | 游戏 SMF 目录 |
| `$APP_DATA` | 应用内部数据目录 |
| `$APP_FILES` | 应用内部文件目录 |
| `$APP_CACHE` | 应用内部缓存目录 |
| `$ANDROID_DATA` | 应用外部数据根目录 |
| `$ANDROID_FILES` | 应用外部文件目录 |
| `$ANDROID_CACHE` | 应用外部缓存目录 |

> **注意**：`$SMF`、`$ITEM`、`$JS_DIR` 依赖版本/栏目上下文，仅在 JS 环境中可用，配置文件中无法使用。

**配置中使用占位符示例**：
```yaml
versions:
  - id: "v2"
    icon: "$APP_FILES/icons/v2_icon.png"    # 应用内部文件目录下的图标
    assetPath: "$WORK_DIR/custom_assets"     # SAF 工作目录下的自定义资源
    enterGamePath: "$ANDROID_FILES/scripts/init.js"  # 外部存储下的 JS 脚本

sections:
  - id: "tools"
    targetPath: "$ANDROID_FILES/pvz2_tools"  # 外部存储下的工具目录
    jsPath: "$APP_FILES/js/tools.js"         # 应用内部的 JS 脚本
    items:
      - id: "export"
        type: BUTTON
        icon: "$APP_FILES/icons/export.png"
        assetPath: "$WORK_DIR/export_data"
        jsPath: "$ANDROID_CACHE/scripts/export.js"
```

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
| `showFloatingWindow` | String | `工具悬窗` | 工具悬窗按钮文字 |
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
| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `presetConfirmTitle` | String | - | 使用预设存档时的二次确认弹窗标题 |
| `presetConfirmMessage` | String | - | 使用预设存档时的二次确认弹窗内容 |
| `deleteConfirmTitle` | String | - | 删除本地存档时的二次确认弹窗标题 |
| `deleteConfirmMessage` | String | - | 删除本地存档时的二次确认弹窗内容（支持 `%s` 替换为存档名） |
| `coverConfirmTitle` | String | - | 用本地存档覆盖游玩存档时的二次确认弹窗标题 |
| `coverConfirmMessage` | String | - | 用本地存档覆盖游玩存档时的二次确认弹窗内容 |
| `deleteGameSaveConfirmTitle` | String | `删除游玩存档` | 删除游玩存档时的二次确认弹窗标题 |
| `deleteGameSaveConfirmMessage` | String | `此操作将永久清空...` | 删除游玩存档时的二次确认弹窗内容 |
| `saveInfoTitle` | String | - | 备份/导入存档时的命名弹窗标题 |
| `saveNameLabel` | String | - | 命名弹窗中存档名称输入框的标签文字 |
| `saveDescLabel` | String | - | 命名弹窗中存档描述输入框的标签文字 |
| `cancelButton` | String | - | 命名弹窗中的取消按钮文字 |
| `confirmButton` | String | - | 命名弹窗中的确认按钮文字 |
| `shareButton` | String | `导出所有本地存档` | 本地存档区域的导出按钮文字（点击弹出导出选项弹窗或直接导出文件夹） |
| `exportButton` | String | - | 游玩存档区域的导出按钮文字（已迁移到 `exportGameSaveButton`，保留兼容） |
| `importButton` | String | - | 导入存档按钮文字 |
| `backupButton` | String | - | 备份游玩存档按钮文字（已迁移到 `backupGameSaveButton`，保留兼容） |
| `coverLocalButton` | String | - | 用选中的本地存档覆盖当前游玩存档的按钮文字 |
| `deleteGameSaveButton` | String | `删除游玩存档` | 删除当前游玩存档的按钮文字 |
| `coverPresetButton` | String | - | 使用预设存档覆盖当前游玩存档的按钮文字 |
| `gameSaveLabel` | String | `游玩存档` | 游玩存档信息区域的标题文字 |
| `gameSaveInfoTemplate` | String | `存档用户：%s · 最后游玩：%t` | 游玩存档信息展示模板；`%s` 替换为存档内用户名（读取 pp.dat），`%t` 替换为 pp.dat 最后修改时间 |
| `gameSaveUnknownUser` | String | `未知用户` | 无法从 pp.dat 读取用户名时的占位文本 |
| `gameSaveNotExistTip` | String | `暂无游玩存档` | 游戏存档目录为空或不存在时的提示文字 |
| `exportOptionTitle` | String | `选择导出方式` | 导出选项弹窗的标题（FileProvider 可用时显示） |
| `exportToFolderOption` | String | `导出到指定文件夹` | 导出选项弹窗中「导出到文件夹」按钮文字 |
| `shareAsPackageOption` | String | `分享为存档包` | 导出选项弹窗中「分享为 .pvz2saves 存档包」按钮文字 |
| `backupGameSaveButton` | String | `备份游玩存档` | 游玩存档区域的备份按钮文字 |
| `exportGameSaveButton` | String | `导出游玩存档` | 游玩存档区域的导出按钮文字 |
| `retryButtonText` | String | - | 操作失败结果弹窗中的重试按钮文字 |
| `saveNameEmptyTip` | String | - | 存档名称输入为空时的提示文字 |
| `noLocalSaveTip` | String | - | 本地存档列表为空时的占位提示文字 |
| `selectLocalSaveTip` | String | - | 未选中本地存档就点击覆盖按钮时的提示文字 |
| `backupSuccessTip` | String | - | 备份本地存档成功的结果提示 |
| `backupFailTipPrefix` | String | - | 备份本地存档失败的结果提示前缀（后接异常信息） |
| `exportSuccessTip` | String | - | 导出游玩存档到文件夹成功的结果提示 |
| `exportFailTipPrefix` | String | - | 导出游玩存档失败的结果提示前缀（后接异常信息） |
| `importSuccessTip` | String | - | 导入存档成功的结果提示 |
| `importFailTipPrefix` | String | - | 导入存档失败的结果提示前缀（后接异常信息） |
| `deleteSuccessTip` | String | - | 删除本地存档成功的结果提示 |
| `deleteFailTipPrefix` | String | - | 删除本地存档失败的结果提示前缀（后接异常信息） |
| `coverSuccessTip` | String | - | 用本地存档覆盖游玩存档成功的结果提示 |
| `coverFailTipPrefix` | String | - | 覆盖游玩存档失败的结果提示前缀（后接异常信息） |
| `deleteGameSaveSuccessTip` | String | `当前游玩存档已成功删除` | 删除游玩存档成功的结果提示 |
| `deleteGameSaveFailTipPrefix` | String | `删除游玩存档失败：%s` | 删除游玩存档失败的结果提示前缀（`%s` 替换为异常信息） |
| `defaultImportNamePrefix` | String | - | 批量导入存档时自动生成的名称前缀（如 `导入_`，后接序号） |
| `defaultBackupDesc` | String | - | 备份存档时默认填入的描述文字（如 `自动备份`） |
| `defaultImportDesc` | String | - | 导入存档时默认填入的描述文字（如 `手动导入`） |
| `operation` | Object | - | 操作类型中文描述配置（结果弹窗标题用，详见下方） |

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
| `background` | String | `bg_main.jpg` | 背景图片路径（支持 URL、绝对路径和占位符） |
| `isUseSolidColorBackground` | Boolean | `true` | 是否使用纯色背景（false 则使用 background 图片） |
| `backgroundMusic` | String | `bg_music.wav` | 背景音乐文件名（支持 URL、绝对路径和占位符） |
| `isPlayBackgroundMusic` | Boolean | `true` | 是否默认播放背景音乐 |
| `cgVideoPath` | String | `opening.mp4` | CG 开场视频路径，相对于 `video/` 目录（支持 URL、绝对路径和占位符） |
| `cgVideoLoadTimeout` | Long | `5000` | CG 开场视频超时时间，默认5秒超时 |
| `cgVideoPoster` | String | null | CG 开场视频加载超时或失败时的海报图片（支持 URL、绝对路径和占位符） |
| `sideBgImage` | String | `game_side_bg.jpg` | 游戏侧边背景图，相对于 `images/` 目录（支持绝对路径和占位符） |
| `floatingBallIcon` | String | `ic_floating_dave.png` | 悬浮球图标，相对于 `images/` 目录（支持绝对路径和占位符） |

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
| `exitConfirm` | String | `退出游戏二次确认` | 退出游戏二次确认开关标签 |
| `exitConfirmTitle` | String | `退出游戏` | 退出确认弹窗标题 |
| `exitConfirmMessage` | String | `确定要退出游戏吗？` | 退出确认弹窗内容 |
| `isUseExitConfirm` | Boolean | `true` | 退出游戏二次确认默认值 |
| `exitConfirmButtonText` | String | `确认退出` | 退出确认弹窗"确认"按钮文字 |
| `showFloatingWindow` | String | `是否开启悬浮窗` | 工具悬浮窗开关标签 |
| `isShowFloatingWindow` | Boolean | `true` | 是否默认开启工具悬浮窗 |
| `applyButtonText` | String | `应 用` | 游戏画面设置悬浮窗"应用"按钮文字 |
| `customGameDisplay` | String | `自定义游戏画面` | 自定义游戏画面总开关标签 |
| `customGameDisplayTitle` | String | `游戏画面设置` | 游戏画面子页面标题 |
| `gameDisplay` | Object | 见下表 | 游戏画面配置默认值（见下方 gameDisplay 详解） |

**settings.gameDisplay 配置详解（游戏画面设置子页面）：**

| 属性 | 类型 | 默认值              | 说明 |
|------|------|------------------|------|
| `isUseCustomGameDisplay` | Boolean | `true`           | 是否默认启用自定义游戏画面（总开关）|
| `allowRotation` | String | `允许随意翻转界面（支持竖屏）` | 允许翻转开关的标签文字 |
| `isAllowRotation` | Boolean | `false`          | 是否默认允许随意翻转（支持竖屏）|
| `customWindowSize` | String | `自定义窗口宽高`        | 自定义宽高模式的标签文字 |
| `customWindowRatio` | String | `自定义窗口比例`        | 自定义比例模式的标签文字 |
| `fullscreen` | String | `全屏`             | 全屏模式的标签文字 |
| `displayMode` | String | `ratio`      | 默认显示模式，可选值：`fullscreen`（全屏）/ `ratio`（自定义比例）/ `size`（自定义宽高）|
| `windowWidth` | Int | `0`              | 自定义宽高模式下的默认宽度（px，仅 `displayMode=size` 时生效。`0` = 自动使用屏幕实际宽度）|
| `windowHeight` | Int | `0`              | 自定义宽高模式下的默认高度（px，仅 `displayMode=size` 时生效。`0` = 自动使用屏幕实际高度）|
| `windowRatio` | Float | `1.5`            | 自定义比例模式下的默认宽高比（如 `1.5` = 3:2，仅 `displayMode=ratio` 时生效）|
| `ratioHint` | String | `宽高比（支持 1.5 或 3:2）` | 比例输入框提示文字 |
| `widthHint` | String | `宽度（dp）`        | 宽度输入框标签文字 |
| `heightHint` | String | `高度（dp）`        | 高度输入框标签文字 |

**gameDisplay 使用示例：**
```yaml
settings:
  customGameDisplay: "自定义游戏画面"
  customGameDisplayTitle: "游戏画面设置"
  showFloatingWindow: "是否开启悬浮窗"
  isShowFloatingWindow: true
  exitConfirm: "退出游戏二次确认"
  exitConfirmTitle: "退出游戏"
  exitConfirmMessage: "确定要退出游戏吗？"
  isUseExitConfirm: true
  exitConfirmButtonText: "确认退出"
  applyButtonText: "应 用"
  gameDisplay:
    isUseCustomGameDisplay: false     # 总开关，false = 不启用（默认横屏全屏）
    allowRotation: "允许随意翻转界面（支持竖屏）"
    isAllowRotation: false
    fullscreen: "全屏"
    customWindowRatio: "自定义窗口比例"
    customWindowSize: "自定义窗口宽高"
    displayMode: "ratio"         # fullscreen / ratio / size
    windowRatio: 1.5                  # 比例模式：3:2
    windowWidth: 0                    # 尺寸模式：0 = 屏幕实际宽度
    windowHeight: 0                   # 尺寸模式：0 = 屏幕实际高度
    ratioHint: "宽高比（支持 1.5 或 3:2）"
    widthHint: "宽度（dp）"
    heightHint: "高度（dp）"
```

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
  - **格式**：`{{icon|width=宽度|height=高度:文件名}}`（`width`/`height` 可选，单位 dp）。
  - **参数**：
     - `文件名`：`pvz2tool/images/` 下的图片文件名；以 `/` 开头则为绝对路径（如 `/data/data/com.example/files/icon.png`）。
     - `width=宽度`（可选）：图片宽度，单位 dp，默认 `fontSize * 1.2`。
     - `height=高度`（可选）：图片高度，单位 dp，默认 `fontSize * 1.2`。
  - **自动化处理**：组件会自动扫描文本中的 `icon` 标签，并尝试从 `${Pvz2ToolConfig.PATH_NAME}/images/文件名` 加载图片（绝对路径则直接从本地文件系统加载）。
  - **视觉对齐**：图标大小会随字体大小（fontSize）自动缩放（约 1.2 倍），并保持垂直居中。
  - **示例**：
     - `消耗 {{icon:sun.png}} 50 点阳光`（默认宽度，高度 18dp）。
     - `{{icon|width=32|height=32:coin.png}}`（32dp 正方形图标）。
     - `{{icon|width=24|height=24:/data/data/com.example/files/sword.png}}`（绝对路径图标）。

- **JS 表达式 (JS)**：
  - **作用**：执行一段 JavaScript 表达式，并将结果插入到文本中。
  - **格式**：`{{js:表达式}}`/`{{js:JS路径}}`。
  - **参数**：
    - `JS路径`：`pvz2tool/js/` 下的脚本文件名；以 `/` 开头则为绝对路径。
  - **递归解析**：JS 表达式的返回值如果包含 `{{...}}` 复合文本标签，会自动递归解析（支持颜色标签、链接标签、图标标签、嵌套 `{{js:...}}` 标签等）。
  - **示例**：
    - `{{js:test.js}}` → 执行`pvz2tool/js/test.js`并显示返回结果。
    - `{{js:/data/data/com.example/files/helper.js}}` → 执行绝对路径的 JS 文件。
    - `{{js:1 + 2}}` → 显示为 `3`。
    - `{{js:new Date().getFullYear()}}` → 显示为当前年份。
    - `{{js: '状态：' + (hp > 0 ? '{{green:存活}}' : '{{red:阵亡}}') }}` → JS 返回的字符串中含有 `{{green:...}}` / `{{red:...}}` 标签，会被继续解析为对应颜色的文本。
    - `{{js: '道具：' + itemIcon }}`（其中 `itemIcon = '{{icon|height=18:sword.png}}'`）→ JS 返回的字符串中含有 `{{icon|...}}` 标签，会被继续解析并渲染为图标。

---
