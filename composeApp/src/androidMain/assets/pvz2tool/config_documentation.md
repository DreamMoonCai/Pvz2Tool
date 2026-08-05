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
| `isShowFloatingWindow` | Boolean | 否 | `false` | 默认开启悬浮窗 |
| `isUseExitConfirm` | Boolean | 否 | `false` | 启用退出确认弹窗 |
| `isUseCustomGameDisplay` | Boolean | 否 | `false` | 启用自定义游戏画面 |
| `displayMode` | String | 否 | `ratio` | 显示模式：`fullscreen` / `ratio` / `size` |
| `windowRatio` | Float | 否 | `1.5` | displayMode=ratio 时的宽高比 |
| `windowWidth` | Int | 否 | `0` | displayMode=size 时的窗口宽度 (px) |
| `windowHeight` | Int | 否 | `0` | displayMode=size 时的窗口高度 (px) |
| `isAllowRotation` | Boolean | 否 | `false` | 允许随意翻转界面 |
| `floatingWindow` | Array | 否 | `[]` | 悬浮窗按钮列表（直接使用 `FloatingWindowItem`，与完整模式 `ui.floatingWindow.items` 字段一致，见下方详解） |
| `schedules` | Array | 否 | `[]` | 定时任务列表（见「定时任务配置」章节） |

**`FloatingWindowItem` 悬浮窗条目（精简/完整模式共用）**：

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | String | 是 | 唯一标识 |
| `name` | String | 否 | 显示名称（支持 `{{js:...}}` 复合文本） |
| `desc` | String | 否 | 按钮下方描述文字 |
| `icon` | String | 否 | 左侧图标资源名（相对于 `assets/pvz2tool/images/`） |
| `buttonText` | String | 否 | 按钮文字（优先级高于 `name`） |
| `buttonColor` | String | 否 | 按钮颜色：`blue` / `red` / `green` / `orange` / `purple`，默认 `blue` |
| `jsScript` | String | 否 | 点击执行的 JS 内联脚本 |
| `jsPath` | String | 否 | JS 脚本文件路径（`jsScript` 为空时生效） |
| `isShowFromJs` | String | 否 | 可见性判定 JS 表达式 |
| `isShowFromJsPath` | String | 否 | 可见性判定脚本路径 |
| `smfList` | Array | 否 | 关联的 SMF 资源列表 |

**精简配置示例**：
```yaml
gameActivity: com.popcap.pvz2cmhd.SexyAppActivity
simplifiedLaunch: true
isShowFloatingWindow: true
isUseCustomGameDisplay: true
displayMode: ratio
windowRatio: 1.77
floatingWindow:
  - id: vpn_toggle
    name: 网络开关
    buttonColor: red
    jsScript: "vpn.isActive() ? vpn.restore() : vpn.disconnect();"
  - id: game_display
    name: 画面设置
    buttonColor: green
    jsScript: ui.showGameDisplay()
schedules:
  - id: daily_sign
    name: 每日签到
    cron: "0 10 * * *"
    jsScript: |
      notifications.show("签到提醒", "记得签到！");
      timer.nextTrigger();
# cgVideoPath: opening.mp4          # 可选，CG 视频路径
# gameActivityInvalid: "..."        # 可选，Activity 错误提示
```
# smfDirectory: files/              # 可选，自定义 SMF 目录
# cgVideoPath: opening.mp4          # 可选，CG 视频路径
# cgVideoPoster: poster.jpg         # 可选，CG 超时海报图
# cgVideoLoadTimeout: 5000          # 可选，CG 超时毫秒
# gameActivityInvalid: "..."        # 可选，Activity 错误提示
```

> **注意**：精简模式下 CG 开场视频仍会正常播放（如有版本变化），之后直接跳过主界面进入游戏。

---

#### 定时任务配置（schedules）

顶层 `schedules` 字段定义后台定时执行的 JS 脚本。每个定时器通过 `AlarmManager` 触发，在独立的前台 Service 中执行 JS，可配合 `notifications.show()` 发送通知。

| 属性 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | String | 是 | — | 唯一标识符，支持 `timer.list()` / `timer.cancel(id)` |
| `name` | String | 否 | `""` | 显示名称（用于 `timer.list()` 返回值） |
| `cron` | String | 是 | — | 触发规则：`"0 10 * * *"` (每天10:00) 或 `"every 30m"` (每30分钟) 等 |
| `jsScript` | String | 否 | `null` | 定时执行的内联 JS 脚本（与 `jsPath` 二选一） |
| `jsPath` | String | 否 | `null` | JS 文件路径（与 `jsScript` 二选一，路径解析同 `SectionItem.jsPath`） |
| `enabled` | Boolean | 否 | `true` | 是否启用 |

**cron 格式支持**：
- `"0 10 * * *"` — 每天 10:00（五段式 cron，当前只解析 hour/minute）
- `"every 30m"` / `"every 30min"` — 每 30 分钟
- `"every 2h"` / `"every 2hour"` — 每 2 小时
- `"every 1d"` / `"every 1day"` — 每天执行

**配置示例**：
```yaml
schedules:
  - id: daily_sign
    name: 每日签到提醒
    cron: "0 10 * * *"
    jsScript: |
      notifications.show("签到提醒", "今天记得签到哦！");
      timer.nextTrigger();
    enabled: true

  - id: health_check
    name: 定时检测
    cron: "every 30m"
    jsPath: "js/health_check.js"
```

**JS API 配合使用**：
```js
// 在脚本末尾调用，注册下一次触发（否则只执行一次）
timer.nextTrigger();

// 发送通知（脚本运行在后台，不能弹 UI 弹窗）
notifications.show("标题", "消息内容", {
    icon: "images/my_icon.png",    // 可选，assets/pvz2tool 下的大图
    channelId: "my_channel",       // 可选，自定义通知渠道
    autoCancel: true,              // 可选，点击后自动消失
    tapAction: "ui.alert('被点击')" // 可选，点击通知时执行 JS
});
```

> **注意事项**：
> - 定时脚本在后台 Service 中执行，无 UI 上下文（不能弹窗、不能操作界面），可读写文件、发通知。
> - 脚本末尾必须调用 `timer.nextTrigger()` 注册下次闹钟，否则定时器只触发一次。
> - 系统深度休眠或省电模式可能导致闹钟延迟。
> - Android 12+ 需要 `SCHEDULE_EXACT_ALARM` 权限，系统可能提示用户授予。

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
| `items[n].isShowFromJs` | String | 全部 | **可见性 JS 表达式**：返回真值才渲染该功能项，不填 = 始终显示；详见下方「功能项动态显隐」 |
| `items[n].isShowFromJsPath` | String | 全部 | **可见性脚本文件路径**（`isShowFromJs` 为空时生效）；路径规则同 `jsPath`；文件读不到时判定为隐藏 |

#### 功能项动态显隐（isShowFromJs / isShowFromJsPath）

给任意类型的功能项加上 `isShowFromJs`，即可用一行 JS 表达式描述「什么情况下才显示我」：

```yaml
items:
  - id: vpn_switch
    type: BUTTON
    name: "{{js:vpn.isActive() ? '恢复网络' : '断开网络'}}"
    isShowFromJs: "vpn.isPrepared()"        # VPN 已授权才显示这一项
    jsScript: "vpn.isActive() ? vpn.restore() : vpn.disconnect();"

  - id: advanced_patch
    type: CHECKBOX
    name: "高级补丁"
    isShowFromJsPath: "version/1.0/checks/advanced.js"   # 判定逻辑写在独立脚本文件里
```

- **写法**：`isShowFromJs` 是**裸表达式**，不要加 `{{js:}}` 包裹；`name` / `desc` 里的动态文案才用 `{{js:}}`。
- **优先级**：`isShowFromJs` > `isShowFromJsPath`，两者都为空 = 始终显示（不会触碰 JS 引擎，无性能开销）。
- **路径规则**：`isShowFromJsPath` 与 `jsPath` 一致 —— 支持占位符变量，按「绝对路径 → 本地工作目录 → APK Assets」顺序查找；文件不存在时判定为隐藏。
- **脚本文件写法**：判定结果取脚本的**完成值**，所以最后一句必须是「值表达式」，不能是 `if` 或 `return`。多分支请用三元或立即执行函数：

  ```js
  // ✅ 正确
  var v = app.version();
  v >= 11 && file.exists('$WORK_DIR/patch.rsb')

  // ✅ 正确（多分支用 IIFE）
  (function () { if (!vpn.isPrepared()) return false; return !vpn.isActive(); })()

  // ❌ 错误：末句是 if，完成值为 undefined → 判定为隐藏
  if (vpn.isPrepared()) { true } else { false }
  ```
- **真假判定**：`false` / `0` / `null` / `undefined` / `NaN` / 空串 → 隐藏，其余 → 显示；**表达式报错也按隐藏处理**（保守策略）。
- **自动重算**：与 `{{js:...}}` 动态文案共用同一条刷新通道 —— 同栏目内任意 BUTTON 点击、CHECKBOX 勾选、SLIDER 拖动结束后都会重新判定，因此功能项可以随运行时状态实时出现/消失，无需重进页面。
- **首帧行为**：求值完成前不渲染，避免「先闪现再消失」；被隐藏的项不参与分隔线计算，不会留下空白横线。

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
| `exportButton` | String | - | 游玩存档区域的导出按钮文字 |
| `importButton` | String | - | 导入存档按钮文字 |
| `backupButton` | String | - | 备份游玩存档按钮文字 |
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
| `floatingExitConfirmTitle` | String | `确认退出` | 悬浮球关闭二次确认弹窗标题 |
| `floatingExitConfirmMessage` | String | `确定要退出悬浮窗吗(直至重启游戏后显示)？` | 悬浮球关闭二次确认弹窗内容 |
| `floatingExitConfirmButtonText` | String | `确认` | 悬浮球关闭二次确认弹窗"确认"按钮文字 |
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
  floatingExitConfirmTitle: "确认退出"
  floatingExitConfirmMessage: "确定要退出悬浮窗吗(直至重启游戏后显示)？"
  floatingExitConfirmButtonText: "确认"
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

**floatingWindow 配置详解（悬浮窗面板内容，动态可配置）：**

悬浮窗展开面板（奶黄绿框卡片）内的按钮列表，按数组顺序从上到下排列。每个按钮点击后执行其配置的 JS 脚本，能力由 JS 全局 API 提供（详见 `js_documentation.md`，例如 `vpn.disconnect()` 断网、`ui.showGameDisplay()` 弹出画面设置）。字段与「栏目」的 BUTTON 项保持一致：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `floatingWindow` | Object | 见下表 | 悬浮窗内容配置（位于 `ui:` 下） |
| `items` | List | `[]` | 按钮列表，详见下表 |
| `emptyTip` | String | `（悬浮窗暂无内容，请在 dream.yml 的 ui.floatingWindow.items 中配置）` | `items` 未配置时的占位提示 |
| `allHiddenTip` | String | `（当前没有可用的功能）` | `items` 已配置但全部被 `isShowFromJs` 隐藏时的占位提示 |
| `items[].id` | String | - | 必填，唯一标识 |
| `items[].name` | String | - | 按钮文字（不填则回退 `buttonText` / `id`）。支持复合文本，含 `{{js:...}}` 时会在每次脚本执行后自动重算 |
| `items[].buttonText` | String | - | 按钮文字（可选，优先级高于 `name`），同样支持复合文本 |
| `items[].buttonColor` | String | `blue` | 按钮颜色：`blue` / `red` / `green` / `orange` / `purple` |
| `items[].icon` | String | - | 左侧图标资源名（相对于 `assets/pvz2tool/images/`，预留） |
| `items[].desc` | String | - | 按钮下方描述文字（预留） |
| `items[].jsScript` | String | - | 点击执行的 JS 脚本（`jsPath` 为空时生效） |
| `items[].jsPath` | String | - | JS 脚本文件路径（`jsScript` 为空时从本地/APK 加载） |
| `items[].isShowFromJs` | String | - | **可见性 JS 表达式**：返回 `true` 才渲染该按钮，不填 = 始终显示。会随复合文本一起自动重算，可实现运行时动态显隐 |
| `items[].isShowFromJsPath` | String | - | **可见性脚本文件路径**（`isShowFromJs` 为空时生效）。路径规则同 `jsPath`（占位符展开 + 绝对路径/本地工作目录/APK Assets 三级查找）；文件读不到时判定为隐藏 |
| `items[].smfList` | List | `[]` | 关联的 SMF 资源列表（用于 `JsSmfDataManager` 注入数据到 JS 执行上下文） |

**floatingWindow 使用示例（默认展示断网与画面设置）：**
```yaml
ui:
  floatingWindow:
    emptyTip: "（悬浮窗暂无内容，请在 dream.yml 的 ui.floatingWindow.items 中配置）"
    allHiddenTip: "（当前没有可用的功能）"
    items:
      # 按钮文案用 {{js:...}} 动态生成：点击执行 jsScript 后会自动重算，实现「断网 ⇄ 恢复」文案翻转
      # isShowFromJs 决定要不要渲染：VPN 未获系统授权时整个按钮直接隐藏
      - id: vpn_toggle
        name: "{{js:vpn.isActive() ? '恢复网络' : '断开网络'}}"
        desc: "点击在「断网」与「恢复网络」之间切换"
        buttonColor: "red"
        isShowFromJs: "vpn.isPrepared()"
        jsScript: "vpn.isActive() ? vpn.restore() : vpn.disconnect();"
      # 仅在设置里开启了「自定义游戏画面」时才显示
      - id: game_display
        name: "画面设置"
        buttonColor: "green"
        isShowFromJs: "ui.isCustomGameDisplayEnabled()"
        jsScript: "ui.showGameDisplay();"
```

> **动态文案原理**：`jsScript` 执行完毕后工具箱会广播一次「复合文本重算」信号，界面上所有 `{{js:...}}`（包含按钮自身的 `name`）都会重新求值。因此无需手动维护状态，直接用表达式描述「当前应该显示什么」即可。

**isShowFromJs 说明（按 JS 条件动态显隐）：**

- **写法**：直接写一个返回布尔的 JS 表达式，**不要**加 `{{js:}}` 包裹。例如 `vpn.isPrepared()`、`ui.isCustomGameDisplayEnabled()`，也可用逻辑运算组合：`vpn.isPrepared() && !vpn.isActive()`。
- **判定逻辑较长时**：改用 `isShowFromJsPath` 指向一个脚本文件（`isShowFromJs` 为空时才生效），路径规则同 `jsPath`（占位符展开 + 绝对路径/本地工作目录/APK Assets 三级查找），文件读不到时判定为隐藏。
- **同样适用于栏目功能项**：`sections[n].items[n]` 也支持这两个字段，用法完全一致，详见「功能项动态显隐」。
- **真假判定**：按 JS 语义解析返回值 —— `false` / `0` / `null` / `undefined` / `NaN` / 空串视为隐藏，其余视为显示。**表达式报错时按隐藏处理**（保守策略，避免展示不可用功能）。
- **重算时机**：与 `{{js:...}}` 文案完全一致 —— 任意用户交互脚本（悬浮窗按钮、栏目 BUTTON / CHECKBOX / SLIDER 等）执行完毕后自动重算，因此授权状态、设置开关变化后按钮会实时出现/消失。
- **首帧行为**：求值完成前按钮不渲染，避免「先闪现再消失」。
- **全部隐藏时**：面板显示 `allHiddenTip`；若 `items` 本身为空则显示 `emptyTip`。
- **常用判定 API**（完整列表见 `js_documentation.md`）：

  | 表达式 | 含义 |
  |--------|------|
  | `vpn.isPrepared()` | VPN 是否已获系统授权（等价于原先 Kotlin 侧的 `prepareVpn(context) == null`） |
  | `vpn.isActive()` | 当前是否处于断网状态 |
  | `ui.isCustomGameDisplayEnabled()` | 设置中「自定义游戏画面」开关是否已开启 |

**顶栏图标组（`ui.topBarIcons`，排在「设置」图标左侧）：**

在主界面顶部应用栏、设置图标**左侧**，由 yml 动态渲染一组可点击图标。每个图标资源（正常态 / 按下态）来自 `AssetExtractorHolder`（相对工作目录 / 绝对路径 / URL / APK Assets 三级解析），点击执行其配置的 JS 脚本。支持配置多个图标，按顺序从左到右排列。字段与悬浮窗按钮保持一致：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `topBarIcons` | Object | 见下表 | 顶栏图标组配置（位于 `ui:` 下） |
| `items` | List | `[]` | 图标列表，按数组顺序从左到右排列在「设置」图标左侧 |
| `items[].id` | String | - | 必填，唯一标识 |
| `items[].icon` | String | - | 正常态图标资源路径（`AssetExtractorHolder` 解析） |
| `items[].iconPress` | String | - | 按下态图标资源路径（同上）；不填则按下时复用 `icon` |
| `items[].contentDescription` | String | - | 无障碍描述 |
| `items[].jsScript` | String | - | 点击执行的 JS 脚本（`jsPath` 为空时生效） |
| `items[].jsPath` | String | - | JS 脚本文件路径（`jsScript` 为空时从本地工作目录 / APK 加载） |
| `items[].isShowFromJs` | String | - | 可见性 JS 表达式：返回 `true` 才渲染该图标，不填 = 始终显示。会随复合文本一起自动重算，可实现运行时动态显隐 |
| `items[].isShowFromJsPath` | String | - | 可见性脚本文件路径（`isShowFromJs` 为空时生效），路径规则同 `jsPath`；文件读不到时判定为隐藏 |
| `items[].pressSound` | String | - | 按下音效文件名（相对 `assets/pvz2tool/sound/`）；不填默认用设置按钮音效 |
| `items[].releaseSound` | String | - | 释放音效文件名（同上）；不填默认用设置按钮音效 |
| `items[].smfList` | List | `[]` | 关联的 SMF 资源列表（用于 `JsSmfDataManager` 注入数据到 JS 执行上下文） |

> **点击执行逻辑**：与悬浮窗按钮一致 —— `jsScript` 优先，为空时回退 `jsPath`（占位符展开 + 三级查找后读取文件内容执行）。脚本执行完毕后广播一次「复合文本重算」信号，使 `isShowFromJs` 等动态显隐即时更新。
> **显隐重算时机**：与 `{{js:...}}` 文案完全一致，任意用户交互脚本执行后自动重算。
> **示例**（见 `dream.yml` 的 `ui.topBarIcons`）：

```yaml
ui:
  topBarIcons:
    items:
      - id: refresh_top
        icon: "images/ic_refresh.png"
        iconPress: "images/ic_refresh_press.png"
        contentDescription: "刷新"
        isShowFromJs: "ui.isCustomGameDisplayEnabled()"
        jsScript: "ui.refreshAll();"
      - id: help_top
        icon: "images/ic_help.png"
        contentDescription: "帮助"
        jsPath: "topbar/help.js"
```

所有文本类配置均支持**复合颜色样式**，语法：

- **颜色与阴影**：
  - 格式：`{{colorName:content}}`（纯色）或 `{{colorName-shadow:content}}`（带同色描边阴影）。
  - 命名色（标签名即颜色名，直接书写即可）：`green`、`purple`、`red`、`gold`、`gray`、`white`、`olive`、`black`、`grey`、`blue`、`yellow`、`orange`、`cyan`、`pink`。
  - `-shadow` 后缀：仅 `green / purple / red / gold / gray / white / olive` 七个支持，例如 `{{red-shadow:警告}}` 显示为带红色描边阴影的文字；其余新增色（`black / grey / blue / yellow / orange / cyan / pink`）仅提供纯色形式。
  - 十六进制（新增）：支持 `{{#RGB:content}}` / `{{#RRGGBB:content}}` / `{{#AARRGGBB:content}}`，例如 `{{#FF5252:危险}}`、`{{#2196F3:信息}}`。
  - 示例：`{{green-shadow:松间烬雪}}` 显示为带深绿阴影的绿色文字；`{{gold:金币}}` 显示为金色文字；`{{#FF0000:错误}}` 显示为红色文字。

- **交互式链接 (Link)**：
  - **基础格式**：`{{link|URL:显示文本}}`。
  - **样式化链接**：`{{link-样式名|URL:显示文本}}`。
  - **实现逻辑**：系统会提取 `link-` 后的样式名并应用对应的颜色和阴影，同时保留下划线；点击时按以下规则决定行为：
    - **直接执行 JS**：满足以下任一条件即视为 JS 目标，点击后执行 JS（而非打开浏览器）：
      1. URL 以 `.js` 结尾（忽略 `?查询` / `#片段`），例如 `tools/foo.js`、`/sdcard/x.js`、`https://example.com/a.js`。
      2. URL 本身为一段 JS 代码（无 `http/https/ftp/...` 等协议前缀、又非 `.js` 文件），例如 `toast("hi")`。
    - **打开浏览器**：带有明确协议（`http://`、`https://`、`ftp://`、`mailto:`、`tel:`、`file://` 等）且不以 `.js` 结尾的普通链接，点击后用系统浏览器打开。
  - **JS 目标解析规则**：
    - 以 `.js` 结尾时按「文件」加载后执行，来源分三种：
      - 网络链接（`http://`/`https://` 开头）：下载该 `.js` 文件内容并执行。
      - 绝对本地路径（以 `/` 开头）：直接读取该文件并执行。
      - 工具箱相对路径（其余）：从 `pvz2tool/js/` 目录读取并执行（与 `{{js:...}}` 行为一致，无需写 `js/` 前缀）。
    - 非 `.js` 结尾则视为**内联 JS 代码**，直接执行。
    - 执行环境与普通 `{{js:...}}` 一致（可访问 `this.当前` 等上下文）。
  - 示例：
    - `{{link-gold-shadow|https://pvz2.com:前往官网}}` → 普通链接，点击打开浏览器。
    - `{{link|https://example.com/a.js:执行远程脚本}}` → 下载并执行远程 `.js`。
    - `{{link|/sdcard/scripts/local.js:执行本地脚本}}` → 读取并执行绝对路径 `.js`。
    - `{{link|myTool.js:执行工具箱脚本}}` → 读取 `pvz2tool/js/myTool.js` 并执行。
    - `{{link|toast("已点击"):点我弹提示}}` → 直接执行内联 JS 代码。

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
  - **自动重算（交互联动）**：任何**用户交互触发的 JS** 执行完毕后，界面上所有 `{{js:...}}` 都会**自动重新求值**一次，使文本能反映脚本刚刚改变的状态。触发源包括：
    - `BUTTON` 类型的按钮点击（`jsScript` / `jsPath`）
    - `CHECKBOX` 类型的勾选切换
    - `SLIDER` 类型的拖动结束
    - 栏目级确认按钮、进入游戏时的栏目级 / 版本级脚本
    - 悬浮窗（`ui.floatingWindow.items`）按钮点击
    - 复合文本中的链接点击（`{{link:...}}` 指向 JS）

    因此可以直接把「状态文案」写成 JS 表达式，点击后自动翻转：
    ```yaml
    - id: vpn_toggle
      name: "{{js:vpn.isActive() ? '恢复网络' : '断开网络'}}"
      jsScript: "vpn.isActive() ? vpn.restore() : vpn.disconnect();"
    ```
    > **说明**：复合文本自身求值时**不会**再次触发刷新，不存在死循环；连续多次刷新信号会被合并（约 60ms 防抖），避免同一批次重复求值。不含 `{{js:...}}` 的文本不参与重算，无额外开销。
  - **示例**：
    - `{{js:test.js}}` → 执行`pvz2tool/js/test.js`并显示返回结果。
    - `{{js:/data/data/com.example/files/helper.js}}` → 执行绝对路径的 JS 文件。
    - `{{js:1 + 2}}` → 显示为 `3`。
    - `{{js:new Date().getFullYear()}}` → 显示为当前年份。
    - `{{js: '状态：' + (hp > 0 ? '{{green:存活}}' : '{{red:阵亡}}') }}` → JS 返回的字符串中含有 `{{green:...}}` / `{{red:...}}` 标签，会被继续解析为对应颜色的文本。
    - `{{js: '道具：' + itemIcon }}`（其中 `itemIcon = '{{icon|height=18:sword.png}}'`）→ JS 返回的字符串中含有 `{{icon|...}}` 标签，会被继续解析并渲染为图标。

---
