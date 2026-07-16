# PvZ2 Tool JS 脚本 API 文档

本文档描述了 PvZ2 Tool 中可用于 JS 脚本的所有内置对象、函数和 API。

> **中文别名说明**：
> 所有 API 同时支持英文和中文两种调用方式。例如 `console.log()` 和 `console.日志()` 等效。
> 本文档中以 `属性/中文别名` 的形式标注。

### 别名速查表

#### console 对象
| 英文 | 中文别名 | 说明 |
|------|----------|------|
| `console.log()` | `console.日志()` | 普通日志 |
| `console.info()` | `console.信息()` | 信息日志 |
| `console.debug()` | `console.调试()` | 调试日志 |
| `console.warn()` | `console.警告()` | 警告日志 |
| `console.error()` | `console.错误()` | 错误日志 |

#### path 对象
| 英文 | 中文别名 |
|------|----------|
| `path` | `路径` |
| `path.app` | `路径.应用` |
| `path.app.data` | `路径.应用.数据` |
| `path.app.files` | `路径.应用.文件` |
| `path.app.cache` | `路径.应用.缓存` |
| `path.android` | `路径.安卓` |
| `path.android.data` | `路径.安卓.数据` |
| `path.android.files` | `路径.安卓.文件` |
| `path.android.cache` | `路径.安卓.缓存` |
| `path.pvz` | `路径.植物大战僵尸` |
| `path.pvz.saves` | `路径.植物大战僵尸.存档` |
| `path.pvz.smf` | `路径.植物大战僵尸.资源` |
| `path.pvz2tool` | `路径.工具` |
| `path.pvz2tool.files` | `路径.工具.文件` |
| `path.pvz2tool.smf` | `路径.工具.SMF` |
| `path.pvz2tool.section` | `路径.工具.资源` |
| `path.pvz2tool.jsDir` | `路径.工具.JS目录` |
| `path.toInternalPath()` | `路径.转换为内部路径()` |
| `path.resolve()` | `路径.解析路径()` |
| `path.resolveUri()` | `路径.解析URI()` |

#### rton 对象
| 英文 | 中文别名 |
|------|----------|
| `rton` | `rton` |
| `rton.encryptionKey` | `rton.加密密钥` |
| `rton.decode()` | `rton.解码()` |
| `rton.encode()` | `rton.编码()` |
| `rton.load()` | `rton.加载()` |
| `rton.load().save()` | `rton.加载().保存()` |
| `rton.save()` | `rton.保存()` |

#### rsb 对象
| 英文 | 中文别名 |
|------|----------|
| `rsb` | `rsb` |
| `rsb.unpack()` | `rsb.解包()` |
| `rsb.pack()` | `rsb.打包()` |

#### zlib 对象
| 英文 | 中文别名 |
|------|----------|
| `zlib` | `zlib` |
| `zlib.unpack()` | `zlib.解包()` |
| `zlib.pack()` | `zlib.打包()` |

#### ptx 对象
| 英文 | 中文别名 |
|------|----------|
| `ptx` | `ptx` |
| `ptx.PtxABGR8888Mode` | `ptx.像素ABGR8888模式` |
| `ptx.PtxARGB8888PaddingMode` | `ptx.像素ARGB8888填充模式` |
| `ptx.RsbPtxABGR8888Mode` | `ptx.资源像素ABGR8888模式` |
| `ptx.RsbPtxARGB8888PaddingMode` | `ptx.资源像素ARGB8888填充模式` |
| `ptx.format` | `ptx.格式` |
| `ptx.decode()` | `ptx.解码()` |
| `ptx.encode()` | `ptx.编码()` |

#### storage 对象
| 英文 | 中文别名 | 说明 |
|------|----------|------|
| `storage.get()` | `storage.获取()` | 获取值 |
| `storage.set()` | `storage.设置()` | 设置值 |
| `storage.delete()` | `storage.删除()` | 删除键 |
| `storage.has()` | `storage.有()` | 检查键是否存在 |
| `storage.clear()` | `storage.清空()` | 清空所有数据 |
| `storage.keys()` | `storage.键列表()` | 获取所有键 |
| `storage.getAll()` | `storage.获取全部()` | 获取所有数据 |

#### file 对象
| 英文 | 中文别名 | 说明 |
|------|----------|------|
| `file.resolve()` | `file.解析()` | 解析占位符路径，返回文件对象 |
| `file.readBytes()` | `file.读字节()` | 读取字节数组 |
| `file.readText()` | `file.读文本()` | 读取文本（UTF-8） |
| `file.writeBytes()` | `file.写字节()` | 写入字节数组 |
| `file.writeText()` | `file.写文本()` | 写入文本（UTF-8） |
| `file.appendText()` | `file.追加文本()` | 追加文本（UTF-8） |
| `file.copy()` | `file.复制()` / `file.复制到()` | 通过解压管线复制文件（toPath 详见下方说明，按扩展名决定目标目录或目标文件名） |
| `file.rename()` | `file.重命名()` / `file.移动到()` | 重命名/移动文件 |
| `file.delete()` | `file.删除()` | 删除文件或目录 |
| `file.exists()` | `file.存在()` | 检查文件是否存在 |
| `file.list()` | `file.列表()` | 列出目录子项，返回文件对象数组 |
| `file.mkdir()` | `file.创建目录()` / `file.mkdirs()` | 创建目录（含父目录） |
| `file.size()` | `file.大小()` / `file.length()` / `file.长度()` | 文件大小（字节） |
| `file.isDirectory()` | `file.是目录()` | 是否为目录 |
| `file.isFile()` | `file.是文件()` | 是否为文件 |
| `file.lastModified()` | `file.修改时间()` | 最后修改时间（Unix 毫秒） |
| `file.extension()` | `file.扩展名()` | 扩展名（不含点） |
| `file.parent()` | `file.父目录()` | 父目录路径（无则为 null） |

#### pvz 对象
| 英文 | 中文别名 | 说明 |
|------|----------|------|
| `pvz.encrypt()` | `pvz.加密()` | 数字加密 |
| `pvz.decrypt()` | `pvz.解密()` | 数字解密 |
| `pvz.saves.load()` | `pvz.存档.加载()` | 加载存档 |
| `pvz.plants` | `pvz.植物` / `pvz.植物.全部` | 植物数据 |
| `pvz.zombies` | `pvz.僵尸` / `pvz.僵尸.全部` | 僵尸数据 |
| `pvz.gameFeatures` | `pvz.强化道具` / `pvz.强化道具.全部` | 强化道具数据 |
| `pvz.worlds` | `pvz.世界` / `pvz.世界.全部` | 世界数据 |
| `pvz.levelModules` | `pvz.关卡模块` / `pvz.关卡模块.全部` | 关卡模块数据 |
| `pvz.gridItems` | `pvz.障碍物` / `pvz.障碍物.全部` | 障碍物数据 |
| `pvz.projectiles` | `pvz.子弹` / `pvz.子弹.全部` | 子弹数据 |
| `pvz.toolPackets` | `pvz.传送带` / `pvz.传送带.全部` | 传送带数据 |
| `pvz.properties` | `pvz.挂件` / `pvz.挂件.全部` | 挂件数据 |
| `pvz.resources` | `pvz.资源` / `pvz.资源.全部` | 资源数据 |
| `pvz.avatars` | `pvz.头像` / `pvz.头像.全部` | 头像数据 |
| `pvz.artifacts` | `pvz.神器` / `pvz.神器.全部` | 神器数据 |
| `pvz.statuses` | `pvz.状态` / `pvz.状态.全部` | 状态数据 |
| `pvz.powerups` | `pvz.金手指` / `pvz.金手指.全部` | 金手指数据 |
| `pvz.genes` | `pvz.基因` / `pvz.基因.全部` | 基因数据 |
| `pvz.gacha` | `pvz.藏品` / `pvz.藏品.全部` | 藏品数据 |
| `pvz.plantFamilies` | `pvz.植物家族` / `pvz.植物家族.全部` | 植物家族数据 |

#### ui 对象
| 英文 | 中文别名 | 说明 |
|------|----------|------|
| `ui.alert()` | `ui.提示()` | 提示弹窗（单按钮） |
| `ui.confirm()` | `ui.确认()` | 确认弹窗 |
| `ui.prompt()` | `ui.输入()` | 输入弹窗 |
| `ui.progress()` | `ui.进度()` | 进度弹窗 |
| `ui.progress().update()` | `ui.进度.更新()` | 更新进度 |
| `ui.progress().close()` | `ui.进度.关闭()` | 关闭进度 |
| `ui.extract()` | `ui.解压()` | 解压根资源 |

#### audio 对象
| 英文 | 中文别名 | 说明 |
|------|----------|------|
| `audio.getBgmVolume()` | `audio.获取背景音乐音量()` | 获取背景音乐音量（0.0~1.0） |
| `audio.setBgmVolume(v)` | `audio.设置背景音乐音量()` | 设置背景音乐音量（0.0~1.0） |
| `audio.getSfxVolume()` | `audio.获取音效音量()` | 获取音效音量（0.0~1.0） |
| `audio.setSfxVolume(v)` | `audio.设置音效音量()` | 设置音效音量（0.0~1.0） |

#### assets 对象
| 英文 | 中文别名 |
|------|----------|
| `assets.list()` | `assets.列表()` |
| `assets.listAssets()` | `assets.列表Assets()` |
| `assets.exists()` | `assets.存在()` |
| `assets.info()` | `assets.信息()` |
| `assets.read()` | `assets.读取()` |
| `assets.readBytes()` | `assets.读取字节()` |
| `assets.readBase64()` | `assets.读取Base64()` |
| `assets.readArrayBuffer()` | `assets.读取ArrayBuffer()` |

#### http 对象
| 英文 | 中文别名 | 说明 |
|------|----------|------|
| `http.get(url, options?)` | `网络.获取()` | GET 请求 |
| `http.post(url, body?, options?)` | `网络.提交()` | POST 请求 |
| `http.put(url, body?, options?)` | `网络.上传()` | PUT 请求 |
| `http.delete(url, options?)` | `网络.删除()` | DELETE 请求 |
| `http.patch(url, body?, options?)` | `网络.修改()` | PATCH 请求 |
| `http.head(url, options?)` | `网络.头部()` | HEAD 请求 |
| `http.request(options)` | `网络.请求()` | 通用请求（支持指定 method） |
| `response.status` | | HTTP 状态码（number） |
| `response.ok` | | 状态码是否 200~299（boolean） |
| `response.statusText` | | 状态文本（如 "OK"） |
| `response.body` | | 响应体文本（string） |
| `response.headers` | | 响应头对象 |
| `response.json()` | `response.解析JSON()` | 将 body 直接解析为 JS 对象（已自动 JSON.parse，无需再调用 JSON.parse；解析失败返回 null） |

#### this 对象
| 英文 | 中文别名 | 说明 |
|------|----------|------|
| `this` | `当前` | 工具上下文（所有属性的根对象） |
| `this.findById()` | `this.查找()` | 快捷查找 |
| `this.version` | `this.版本` | 版本信息 |
| `this.all` | `this.全部` | 所有栏目 |
| `this.gameActivity` | `this.游戏界面` | 当前 gameActivity |
| `this.checked` | `this.选中` / `this.勾选` | CHECKBOX 状态（可读写） |
| `this.value` | `this.值` | SLIDER/INPUT/INFO 值（可读写） |
| `this.setValue()` | `this.设置值()` | 设置栏目项值 |
| `this.call()` | `this.调用()` | 主动触发指定项目ID的JS |
| `this.refresh()` | `this.刷新()` | 刷新整个界面（触发 UI 重组，使 this 中对 state 的修改立即生效） |

#### this.version
| 英文 | 中文别名 |
|------|----------|
| `this.version.id` | `this.版本.编号` |
| `this.version.name` | `this.版本.名称` |
| `this.version.baseAssetPath` | `this.版本.基础资源路径` |
| `this.version.assetPath` | `this.版本.资源路径` |
| `this.version.forceOverride` | `this.版本.强制覆盖` |

#### section（栏目）
| 英文 | 中文别名 |
|------|----------|
| `section.id` | `section.编号` |
| `section.title` | `section.标题` |
| `section.theme` | `section.主题` |
| `section.targetPath` | `section.目标路径` |
| `section.items` | `section.项目` |
| `section.checkedItems` | `section.选中项` |
| `section.sliderValues` | `section.滑块值` |
| `section.inputValues` | `section.输入值` |
| `section.infoValues` | `section.信息值` |

#### item（栏目项）
| 英文 | 中文别名 | 说明 |
|------|----------|------|
| `item.id` | `item.编号` | 项目 ID |
| `item.name` | `item.名称` | 显示名称 |
| `item.desc` | `item.描述` | 描述信息 |
| `item.type` | `item.类型` | 项目类型 |
| `item.icon` | `item.图标` | 图标路径 |
| `item.assetPath` | `item.资源路径` | 资源路径 |
| `item.groupId` | `item.分组` | 分组 ID |
| `item.displayName` | `item.显示名` | 显示名称 |
| `item.selected` | `item.选中` | 是否选中（RADIO） |
| `item.checked` | `item.勾选` | 选中状态（CHECKBOX，可读写） |
| `item.value` | `item.值` | 当前值（SLIDER/INPUT/INFO，可读写） |
| `item.minValue` | `item.最小值` | 最小值 |
| `item.maxValue` | `item.最大值` | 最大值 |
| `item.step` | `item.步长` | 步长 |
| `item.valueSuffix` | `item.值后缀` | 值后缀 |
| `item.inputDefault` | `item.默认输入` | 默认输入 |
| `item.placeholder` | `item.占位符` | 占位符 |
| `item.sectionId` | `item.栏目编号` | 所属栏目 ID |
| `item.sectionTitle` | `item.栏目标题` | 所属栏目标题 |
| `item.resolvedPath` | `item.解析后路径` | 解析后的路径 |
| `item.call()` | `item.执行()` | 主动触发此项目的JS |

------

## 1. 内置对象总览

| 对象 | 中文名 | 说明 | 作用域 |
|------|--------|------|--------|
| `console` | 控制台 | 日志输出 | 全局 |
| `path` | 路径 | 路径解析和构建 | 局部 |
| `file` | 文件 | 通用文件读写操作 | 局部 |
| `picker` | 选择器 | 系统文件/目录选择器（SAF），返回文件对象 | 全局 |
| `clipboard` | 剪切板 | 系统剪切板读写（复制文本 / 读取文本 / 清空） | 全局 |
| `device` | 设备 | 当前安卓设备信息（系统 / 屏幕 / 内存 / 存储 / 电池 / 网络 / 应用 / Root） | 全局 |
| `rton` | RTON | RTON 文件编解码 | 局部 |
| `rsb` | RSB | RSB 资源包解包/打包 | 局部 |
| `zlib` | ZLIB | ZLib 压缩/解压 | 局部 |
| `ptx` | PTX | PTX 纹理编解码 | 局部 |
| `pvz` | 植物大战僵尸 | 数字加密/存档操作/游戏数据访问 | 全局 |
| `ui` | 界面 | 弹窗、进度条、解压 | 全局 |
| `js` | JS执行器 | 动态执行JS代码/文件 | 全局 |
| `http` | 网络 | HTTP 网络请求（GET/POST/PUT/DELETE/PATCH/HEAD） | 全局 |
| `audio` | 音频 | 背景音乐/音效音量控制 | 全局 |
| `assets` | 资源 | 工具箱资源访问（本地优先+URL支持） | 全局 |
| `storage` | 存储 | 持久化键值存储 | 全局 |
| `data` | 数据 | SMF 数据访问（通过 smfList 配置） | 局部 |
| `this` | - | 工具上下文（含版本、栏目状态等） | 局部 |

> **作用域说明**：
> - **全局**：在所有 JS 脚本中均可访问
> - **局部**：只在当前 JS 脚本上下文中可用（如 `path`、`rton`、`rsb`、`zlib`、`data` 等依赖版本上下文的对象）

---

## 2. console - 日志输出

提供标准日志输出功能，日志会显示在 UI 的日志面板中。

### 方法

| 方法 | 中文别名 | 说明 |
|------|----------|------|
| `console.log(message)` | `console.日志(message)` | 输出普通日志 |
| `console.info(message)` | `console.信息(message)` | 输出信息日志 |
| `console.warn(message)` | `console.警告(message)` | 输出警告日志 |
| `console.error(message)` | `console.错误(message)` | 输出错误日志 |
| `console.debug(message)` | `console.调试(message)` | 输出调试日志 |

### 示例

```javascript
// 英文写法
console.log("开始处理...");

// 中文写法（等效）
console.日志("开始处理...");
```

---

## 3. path - 路径工具

提供路径解析和构建功能，支持占位符路径自动解析。

### 属性

| 属性 | 中文别名 | 说明 | 示例值 |
|------|----------|------|--------|
| `path.app.data` | `path.应用.数据` | 应用私有数据目录 | `$APP_DATA`（即 `/data/user/0/<包名>`） |
| `path.app.files` | `path.应用.文件` | 应用文件目录 | `$APP_FILES`（即 `/data/user/0/<包名>/files`） |
| `path.app.cache` | `path.应用.缓存` | 应用缓存目录 | `$APP_CACHE`（即 `/data/user/0/<包名>/cache`） |
| `path.android.data` | `path.安卓.数据` | Android 外部数据目录 | `$ANDROID_DATA`（即 `/storage/emulated/0/Android/data/<包名>`） |
| `path.android.files` | `path.安卓.文件` | Android 外部文件目录 | `$ANDROID_FILES`（即 `/storage/emulated/0/Android/data/<包名>/files`） |
| `path.android.cache` | `path.安卓.缓存` | Android 外部缓存目录 | `$ANDROID_CACHE`（即 `/storage/emulated/0/Android/data/<包名>/cache`） |
| `path.pvz.saves` | `path.植物大战僵尸.存档` | 游戏存档目录 | `$GAME_SAVES` |
| `path.pvz.smf` | `path.植物大战僵尸.资源` | 游戏 smf 目录 | `$GAME_SMF` |
| `path.pvz2tool.files` | `path.工具.文件` | 工具箱工作目录 | `$WORK_DIR` |
| `path.pvz2tool.smf` | `path.工具.SMF` | 当前选中版本的 SMF 目录 | `$SMF` |
| `path.pvz2tool.section` | `path.工具.资源` | 当前功能所在的资源目录 | `$ITEM` |
| `path.pvz2tool.jsDir` | `path.工具.JS目录` | 执行当前JS所在的 JS 目录 | `$JS_DIR` |

### 方法

#### path.resolve / path.解析路径

将占位符路径解析为绝对路径字符串。

**参数**：
- `placeholderPath` (string): 包含占位符的路径

**返回**：绝对路径字符串

**示例**：
```javascript
var savePath = path.resolve("$GAME_SAVES/SeedChooserUserData.rton");
// 中文写法
var savePath = path.解析路径("$GAME_SAVES/SeedChooserUserData.rton");
```

#### path.resolveUri / path.解析URI

将占位符路径解析为 URI 字符串（用于 content:// 等场景）。

**参数**：
- `placeholderPath` (string): 包含占位符的路径

**返回**：URI 字符串

**示例**：
```javascript
var uri = path.resolveUri("$WORK_DIR/data.bin");
```

#### path.toInternalPath / path.转换为内部路径

将 JS 占位符路径转换为 `AssetExtractorHolder.resource()` 可使用的 `internalPath`（即 assets 内的相对路径，不含 `pvz2tool/` 前缀）。

转换规则：
- `$WORK_DIR` → `""` 或 `subPath`（对应 `pvz2tool/` 根目录或其子路径）
- `$SMF/xxx` → `version/assetPath/xxx`（不存在则降级到 `version/baseAssetPath`）
- `$ITEM/xxx` → `version/id/sectionId/itemId/xxx`（不存在则降级到 `$SMF`，再降级到 `baseAssetPath`）
- `$JS_DIR/xxx` → `version/enterGamePath` 的父目录（不存在则逐级向上降级）
- `/absolute/path` → 原样返回（绝对路径不经过 `pvz2tool/` 前缀处理）
- `relative/path`（不以 `$` 或 `/` 开头）→ 自动视为 `$WORK_DIR/relative/path` 处理，因此返回的 internalPath 即为 `relative/path` 这一段子路径

**参数**：
- `placeholderPath` (string): 包含占位符的路径

**返回**：string | null - internalPath（不含 `pvz2tool/` 前缀），无法转换时返回 null

**示例**：
```javascript
// $WORK_DIR → 空字符串（对应 pvz2tool/ 根目录）
var internal1 = path.toInternalPath("$WORK_DIR"); // ""

// $WORK_DIR/xxx → xxx（对应 pvz2tool/xxx）
var internal2 = path.toInternalPath("$WORK_DIR/scripts/main.js"); // "scripts/main.js"

// $SMF → version 的 assetPath
var internal3 = path.toInternalPath("$SMF"); // "version/new/smf"

// $ITEM → item 的资源路径（支持降级）
var internal4 = path.toInternalPath("$ITEM/config.json");
// → "version/1.0.0/secret/mapeditor/config.json"（若存在）
// → 降级到 "version/new/smf/config.json"（若不存在）

// 配合 ui.extract 使用
ui.extract(
    [path.toInternalPath("$ITEM/resources.rsb.smf")],
    "$WORK_DIR/output"
);
```

---

## 4. rton - RTON 文件处理

RTON 是游戏使用的二进制格式。`rton` 对象提供编解码功能。

### 属性

#### rton.encryptionKey / rton.加密密钥

读写全局 RTON 加密密钥。

**类型**：string (读写)

**示例**：
```javascript
var key = rton.encryptionKey;  // 或 rton.加密密钥
rton.encryptionKey = "your_key_here";
```

### 方法

#### rton.decode / rton.解码

将 RTON 文件解码为 JSON 文件。

**参数**：
- `inputPath` (string): 输入 RTON 文件路径
- `outputPath` (string): 输出 JSON 文件路径

**示例**：
```javascript
rton.decode(
    path.resolve("$SMF/data.rton"),
    path.resolve("$WORK_DIR/output/data.json")
);
```

#### rton.encode / rton.编码

将 JSON 文件编码为 RTON 文件。

**参数**：
- `inputPath` (string): 输入 JSON 文件路径
- `outputPath` (string): 输出 RTON 文件路径

**示例**：
```javascript
rton.encode(
    path.resolve("$WORK_DIR/input/data.json"),
    path.resolve("$SMF/output.rton")
);
```

#### rton.load / rton.加载

加载文件为 JS 对象，并注入 `save()` 方法。输入可以是 `.rton` 文件（自动解码为 JSON）或 `.json` 文件（直接作为 JSON 读取，无需解码）。

**参数**：
- `inputPath` (string): 输入 RTON 文件路径
- `outputPath` (string, 可选): save() 默认写入目标路径

**返回**：JSObject - 包含 `save()` 方法的 JS 对象

**示例**：
```javascript
var obj = rton.load(path.resolve("$GAME_SAVES/SeedChooserUserData.rton"));
console.log(obj.zombie1_resources_digest);

// 修改数据
obj.zombie1_resources_digest = "122";

// 保存（自动编码回 RTON）
obj.save();
```

#### rton.save / rton.保存

将 JSON 字符串保存为 RTON 文件。

**参数**：
- `outputPath` (string): 输出 RTON 文件路径
- `jsonString` (string): JSON 字符串

**示例**：
```javascript
rton.save(
    path.resolve("$SMF/output.rton"),
    JSON.stringify({key: "value"})
);
```

---

## 5. rsb - RSB 资源包处理

RSB 是游戏资源包格式。`rsb` 对象提供解包和打包功能。

### 方法

#### rsb.unpack / rsb.解包

解包 RSB 资源包。

**参数**：
- `inFilePath` (string): 输入 RSB 文件路径
- `outFolderPath` (string): 输出目录路径
- `options` (object, 可选): 回调选项

**options 参数**（均支持中英文别名）：

| 属性 | 中文别名 | 类型 | 说明 |
|------|----------|------|------|
| `onStart` | `开始` | function | 开始时调用 |
| `onLog` | `日志` | function(level, message) | 日志回调（level: "INFO"/"WARN"/"ERROR"） |
| `onProgress` | `进度` | function(progress, message) | 进度回调 (progress: 0.0-1.0) |
| `onResourceGroupStart` | `资源组开始` | function(index, id) | 资源组开始回调 |
| `onResourceGroupEnd` | `资源组结束` | function(index, id) | 资源组结束回调 |
| `onHeaderRead` | `头部读取完成` | function(rsbInfo) | RSB 头部读取完成回调，参数为 RSB 信息字符串 |
| `onError` | `错误` | function(error, message) | 错误回调（error: 错误对象） |
| `onFinish` | `完成` | function(filePath) | 完成回调 |

**通用配置属性**（RsbCommonConfig，均支持中英文别名）：

| 属性 | 中文别名 | 类型 | 默认值 | 说明 |
|------|----------|------|--------|------|
| `version` | `版本` | number | `3` | RSB 版本号 |
| `bigEndian` | `大端序` | boolean | `false` | 是否使用大端序 |
| `compressionLevel` | `压缩级别` | string | `"Optimal"` | 压缩级别：`"Fastest"` / `"Optimal"` / `"SmallestSize"` |
| `ptxInfoLength` | `PTX信息长度` | number | `0x10` | PTX 信息长度 |
| `smfCompress` | `SMF压缩` | boolean | `false` | 是否对 RSB 进行 SMF 压缩 |
| `specialPool` | `特殊池` | boolean | `false` | 是否使用特殊池模式 |
| `compressPart0` | `压缩Part0` | boolean | `false` | 是否压缩 Part0 数据 |
| `compressPart1` | `压缩Part1` | boolean | `true` | 是否压缩 Part1 数据 |
| `maxRetries` | `最大重试次数` | number | `1` | 最大重试次数 |
| `retryDelayMs` | `重试延迟毫秒` | number | `500` | 重试延迟（毫秒），支持指数退避 |

**解包专属配置属性**（RsbUnpackConfig）：

| 属性 | 中文别名 | 类型 | 默认值 | 说明 |
|------|----------|------|--------|------|
| `autoRtonToJson` | `自动RTON转JSON` | boolean | `true` | 是否自动将 RTON 转换为 JSON |
| `deleteOriginalRton` | `删除原始RTON` | boolean | `true` | 转换后是否删除原始 RTON 文件 |
| `autoPtxToPng` | `自动PTX转PNG` | boolean | `false` | 是否自动将 PTX 转换为 PNG |
| `deleteOriginalPtx` | `删除原始PTX` | boolean | `true` | 转换后是否删除原始 PTX 文件 |

**示例**：
```javascript
rsb.unpack(
    path.resolve("$SMF/resources.rsb"),
    path.resolve("$SMF/resources_unpacked"),
    {
        onProgress: (pct, msg) => console.log(msg),
        onHeaderRead: (info) => console.log("RSB信息:", info),
        bigEndian: false,
        autoRtonToJson: true,
        deleteOriginalRton: false
    }
);
```

#### rsb.pack / rsb.打包

打包文件夹为 RSB 资源包。

**参数**：
- `inFolderPath` (string): 输入目录路径
- `outFilePath` (string): 输出 RSB 文件路径
- `options` (object, 可选): 回调选项

**options 参数**（均支持中英文别名）：

| 属性 | 中文别名 | 类型 | 说明 |
|------|----------|------|------|
| `onStart` | `开始` | function | 开始时调用 |
| `onLog` | `日志` | function(level, message) | 日志回调（level: "INFO"/"WARN"/"ERROR"） |
| `onProgress` | `进度` | function(progress, message) | 进度回调 (progress: 0.0-1.0) |
| `onXmlParsed` | `解析XML完成` | function(count) | XML 解析完成回调，参数为资源组数量 |
| `onResourceGroupStart` | `资源组开始` | function(index, id) | 资源组开始回调 |
| `onResourceGroupEnd` | `资源组结束` | function(index, id) | 资源组结束回调 |
| `onError` | `错误` | function(error, message) | 错误回调（error: 错误对象） |
| `onFinish` | `完成` | function(filePath) | 完成回调 |

**通用配置属性**（RsbCommonConfig，均支持中英文别名）：

| 属性 | 中文别名 | 类型 | 默认值 | 说明 |
|------|----------|------|--------|------|
| `version` | `版本` | number | `3` | RSB 版本号 |
| `bigEndian` | `大端序` | boolean | `false` | 是否使用大端序 |
| `compressionLevel` | `压缩级别` | string | `"Optimal"` | 压缩级别：`"Fastest"` / `"Optimal"` / `"SmallestSize"` |
| `ptxInfoLength` | `PTX信息长度` | number | `0x10` | PTX 信息长度 |
| `smfCompress` | `SMF压缩` | boolean | `false` | 是否对 RSB 进行 SMF 压缩 |
| `specialPool` | `特殊池` | boolean | `false` | 是否使用特殊池模式 |
| `compressPart0` | `压缩Part0` | boolean | `false` | 是否压缩 Part0 数据 |
| `compressPart1` | `压缩Part1` | boolean | `true` | 是否压缩 Part1 数据 |
| `maxRetries` | `最大重试次数` | number | `1` | 最大重试次数 |
| `retryDelayMs` | `重试延迟毫秒` | number | `500` | 重试延迟（毫秒），支持指数退避 |

**打包专属配置属性**（RsbPackConfig）：

| 属性 | 中文别名 | 类型 | 默认值 | 说明 |
|------|----------|------|--------|------|
| `autoJsonToRton` | `自动JSON转RTON` | boolean | `true` | 是否自动将 JSON 转换为 RTON |
| `deleteOriginalJson` | `删除原始JSON` | boolean | `false` | 转换后是否删除原始 JSON 文件 |
| `autoPngToPtx` | `自动PNG转PTX` | boolean | `true` | 是否自动将 PNG 转换为 PTX |
| `deleteOriginalPng` | `删除原始PNG` | boolean | `false` | 转换后是否删除原始 PNG 文件 |

**示例**：
```javascript
rsb.pack(
    path.resolve("$SMF/resources_unpacked"),
    path.resolve("$SMF/resources.rsb"),
    {
        onProgress: (pct, msg) => console.log(msg),
        version: 3,
        bigEndian: false,
        compressionLevel: "Optimal",
        autoJsonToRton: true,
        autoPngToPtx: true
    }
);
```

---

## 5.5. zlib - ZLib 压缩/解压

ZLib 是游戏常用的数据压缩格式。`zlib` 对象提供对 ZLib 格式文件的打包（压缩）与解包（解压）功能。

> **别名**：`zlib` 和 `ZLIB` 均可访问同一对象。

### 方法

#### zlib.unpack / zlib.解包

将 ZLib 压缩文件解压到目标路径。

**参数**：
- `inFilePath` (string): 输入 ZLib 压缩文件路径
- `outFilePath` (string): 输出解压后文件路径

**示例**：
```javascript
// 解压 ZLib 文件
zlib.unpack(
    path.resolve("$SMF/data.zlib"),
    path.resolve("$WORK_DIR/data.bin")
);

// 中文别名
zlib.解包(
    path.resolve("$SMF/data.zlib"),
    path.resolve("$WORK_DIR/data.bin")
);
```

#### zlib.pack / zlib.打包

将文件压缩为 ZLib 格式。

**参数**：
- `inFilePath` (string): 输入文件路径
- `outFilePath` (string): 输出 ZLib 压缩文件路径
- `level` (string): 压缩级别，可选值见下表，默认 Optimal
- `isChineseMode` (boolean): 是否启用中文版打包，自动追加头，默认 false

**压缩级别（level）**：

| 值 | 说明 |
|----|------|
| `"Fastest"` | 最快速度，压缩率最低（对应 deflate level 0） |
| `"Optimal"` | 平衡速度与压缩率（对应 deflate level 6，**默认推荐**） |
| `"Smallest"` | 最高压缩率，速度最慢（对应 deflate level 9） |

**示例**：
```javascript
// 以最优压缩率打包
zlib.pack(
    path.resolve("$WORK_DIR/data.bin"),
    path.resolve("$SMF/data.zlib"),
    "Optimal",
    false
);

// 以最小体积打包
zlib.pack(
    path.resolve("$WORK_DIR/data.bin"),
    path.resolve("$SMF/data.zlib"),
    "Smallest",
    false
);

// 中文别名
zlib.打包(
    path.resolve("$WORK_DIR/data.bin"),
    path.resolve("$SMF/data.zlib"),
    "Optimal",
    false
);
```

---

## 6. ptx - PTX 纹理处理

PTX 是游戏纹理格式。`ptx` 对象提供纹理编解码功能。

### 属性（可读写）

| 属性 | 中文别名 | 类型 | 说明 |
|------|----------|------|------|
| `ptx.PtxABGR8888Mode` | `ptx.像素ABGR8888模式` | boolean | PTX ABGR8888 模式 |
| `ptx.PtxARGB8888PaddingMode` | `ptx.像素ARGB8888填充模式` | boolean | PTX ARGB8888 填充模式 |
| `ptx.RsbPtxABGR8888Mode` | `ptx.资源像素ABGR8888模式` | boolean | RSB PTX ABGR8888 模式 |
| `ptx.RsbPtxARGB8888PaddingMode` | `ptx.资源像素ARGB8888填充模式` | boolean | RSB PTX ARGB8888 填充模式 |
| `ptx.format` | `ptx.格式` | object | 纹理格式常量 |

### ptx.format 格式常量

```javascript
ptx.format.ABGR8888
ptx.format.ARGB8888
ptx.format.RGB565
ptx.format.ARGB4444
ptx.format.A8
ptx.format.L8
// ... 其他格式
```

### 方法

#### ptx.decode / ptx.解码

将 PTX 文件解码为图片文件。

**参数**：
- `inputPath` (string): 输入 PTX 文件路径
- `outputPath` (string): 输出图片文件路径

**示例**：
```javascript
ptx.decode(
    path.resolve("$SMF/texture.ptx"),
    path.resolve("$WORK_DIR/texture.png")
);
```

#### ptx.encode / ptx.编码

将图片文件编码为 PTX 文件。

**参数**：
- `inputPath` (string): 输入图片文件路径
- `outputPath` (string): 输出 PTX 文件路径
- `format` (string): 目标格式名称

**示例**：
```javascript
ptx.encode(
    path.resolve("$WORK_DIR/texture.png"),
    path.resolve("$SMF/texture.ptx"),
    "ARGB8888"
);
```

---

## 7. pvz - 植物大战僵尸工具

提供游戏相关的工具函数和数据访问能力。支持中英文两种调用方式。

### 方法

#### pvz.encrypt / pvz.加密

数字加密。

**参数**：
- `value` (number): 要加密的数字

**返回**：加密后的数字

**示例**：
```javascript
var raw = 1000;
var enc = pvz.encrypt(raw);
console.log("加密结果: " + enc);
```

#### pvz.decrypt / pvz.解密

数字解密。

**参数**：
- `value` (number): 要解密的值

**返回**：解密后的原始数字

**示例**：
```javascript
var enc = 12345;
var dec = pvz.decrypt(enc);
console.log("解密结果: " + dec);
```

#### pvz.saves.load / pvz.存档.加载

加载游戏存档。

**返回**：JSObject - 存档对象

**示例**：
```javascript
var saves = pvz.saves.load();
console.log(saves.objects[0].objclass);
saves.save();
```

---

## 7.1. pvz 数据访问 API

提供对游戏各类数据（植物、僵尸、强化道具等）的访问能力。数据从 `assets/pvz2tool/pvz/` 目录下的 JSON 文件加载，支持中英文两种访问方式。

### 支持的数据类型

| 英文访问 | 中文访问 | 说明 | JSON 文件 |
|----------|----------|------|-----------|
| `pvz.plants` / `pvz.植物` | 植物数据 | `plants.json` |
| `pvz.zombies` / `pvz.僵尸` | 僵尸数据 | `zombies.json` |
| `pvz.gameFeatures` / `pvz.强化道具` | 强化道具数据 | `game_features.json` |
| `pvz.worlds` / `pvz.世界` | 世界数据 | `worlds.json` |
| `pvz.levelModules` / `pvz.关卡模块` | 关卡模块数据 | `level_modules.json` |
| `pvz.gridItems` / `pvz.障碍物` | 障碍物数据 | `grid_items.json` |
| `pvz.projectiles` / `pvz.子弹` | 子弹数据 | `projectiles.json` |
| `pvz.toolPackets` / `pvz.传送带` | 传送带数据 | `tool_packets.json` |
| `pvz.properties` / `pvz.挂件` | 挂件数据 | `properties.json` |
| `pvz.resources` / `pvz.资源` | 资源数据 | `resources.json` |
| `pvz.avatars` / `pvz.头像` | 头像数据 | `avatars.json` |
| `pvz.artifacts` / `pvz.神器` | 神器数据 | `artifacts.json` |
| `pvz.statuses` / `pvz.状态` | 状态数据 | `statuses.json` |
| `pvz.powerups` / `pvz.金手指` | 金手指数据 | `powerups.json` |
| `pvz.genes` / `pvz.基因` | 基因数据 | `genes.json` |
| `pvz.gacha` / `pvz.藏品` | 藏品数据 | `gacha.json` |
| `pvz.plantFamilies` / `pvz.植物家族` | 植物家族数据 | `plant_families.json` |

### 数据访问方式

#### 获取全部数据

使用 `all` / `全部` 属性获取某类数据的所有条目。类型为数组。

**语法**：
```javascript
pvz.<type>.all
pvz.<中文名>.全部
```

**返回**：Array - 数据对象数组，每个元素包含该条目的全部字段（id/name/code 等）。

> **说明**：`all` / `全部` 返回的是**数组**，可用 `for...of` 遍历；同时父对象上仍可按 **代码** 或 **名称** 直接访问单个条目（如 `pvz.plants.peashooter`、`pvz.植物.向日葵`）。两者不要混淆：`pvz.plants.all` 是数组，`pvz.plants.peashooter` 是单个对象。

**示例**：
```javascript
// 获取所有植物
var allPlants = pvz.plants.all;
var 所有植物 = pvz.植物.全部;

// 遍历植物
for (var plant of allPlants) {
    console.log("植物代码: " + plant.code);
    console.log("植物名称: " + plant.name);
}
```

#### 按代码/名称访问单个数据

可以直接通过代码或中文名称访问特定的数据条目（名称自适应小写）。

**语法**：
```javascript
pvz.<type>.<code>
pvz.<type>.<中文名>
```

**示例**：
```javascript
// 通过代码访问
var peashooter = pvz.plants.peashooter;
console.log("豌豆射手名称: " + peashooter.name);

// 通过中文名访问（会自动转小写）
var 向日葵 = pvz.植物.向日葵;
console.log("向日葵代码: " + 向日葵.code);
```

### 通用数据属性

以下属性在所有数据类型中都可用（支持中英文别名）：

| 英文 | 中文别名 | 说明 |
|------|----------|------|
| `id` / `编号` / `ID` | 唯一编号 |
| `name` / `昵称` | 显示名称 |
| `code` / `代号` / `代码` | 代码标识 |

### 各类型特有属性

#### 植物 (plants / 植物)

| 英文 | 中文别名 | 说明 |
|------|----------|------|
| `order` / `序号` | 植物序号 |
| `shardId` / `碎片编号` / `碎片ID` | 碎片 ID |
| `avatarId` / `装扮编号` / `装扮ID` | 装扮 ID |
| `avatarShardId` / `装扮碎片编号` | 装扮碎片 ID |
| `avatars` / `装扮` | 装扮列表 |

**植物示例**：
```javascript
var plant = pvz.plants.peashooter;
console.log("名称: " + plant.name);
console.log("序号: " + plant.order);
console.log("碎片ID: " + plant.shardId);

// 遍历装扮
if (plant.avatars) {
    plant.avatars.forEach(function(avatar) {
        console.log("装扮: " + avatar.name);
    });
}
```

#### 僵尸 (zombies / 僵尸)

| 英文 | 中文别名 | 说明 |
|------|----------|------|
| `id` / `编号` | 僵尸 ID |
| `name` / `昵称` | 僵尸名称 |
| `code` / `代号` | 僵尸代码 |

#### 强化道具 (gameFeatures / 强化道具)

| 英文 | 中文别名 | 说明 |
|------|----------|------|
| `id` / `编号` | 道具 ID |
| `name` / `昵称` | 道具名称 |
| `code` / `代号` | 道具代码 |

#### 世界 (worlds / 世界)

| 英文 | 中文别名 | 说明 |
|------|----------|------|
| `id` / `编号` | 世界 ID |
| `name` / `昵称` | 世界名称 |
| `code` / `代号` | 世界代码 |

#### 关卡模块 (levelModules / 关卡模块)

| 英文 | 中文别名 | 说明 |
|------|----------|------|
| `name` / `昵称` | 模块名称 |
| `code` / `代号` | 模块代码 |

#### 障碍物 (gridItems / 障碍物)

| 英文 | 中文别名 | 说明 |
|------|----------|------|
| `name` / `昵称` | 障碍物名称 |
| `code` / `代号` | 障碍物代码 |

#### 子弹 (projectiles / 子弹)

| 英文 | 中文别名 | 说明 |
|------|----------|------|
| `name` / `昵称` | 子弹名称 |
| `code` / `代号` | 子弹代码 |

#### 挂件 (properties / 挂件)

| 英文 | 中文别名 | 说明 |
|------|----------|------|
| `id` / `编号` | 挂件 ID |
| `name` / `昵称` | 挂件名称 |
| `code` / `代号` | 挂件代码 |
| `shardId` / `碎片编号` | 碎片 ID |

#### 神器 (artifacts / 神器)

| 英文 | 中文别名 | 说明 |
|------|----------|------|
| `id` / `编号` | 神器 ID |
| `name` / `昵称` | 神器名称 |
| `code` / `代号` | 神器代码 |

#### 植物家族 (plantFamilies / 植物家族)

植物家族数据结构特殊，包含 `families`（家族列表）和 `attributes`（属性列表）。

| 英文 | 中文别名 | 说明 |
|------|----------|------|
| `families` / `家族` | 家族列表 |
| `attributes` / `属性` | 属性列表 |

**家族条目属性**：

| 英文 | 中文别名 | 说明 |
|------|----------|------|
| `id` / `编号` | 家族 ID |
| `name` / `昵称` | 家族名称 |
| `members` / `成员` | 成员列表 |

**示例**：
```javascript
// 获取植物家族数据
var familyData = pvz.plantFamilies;
console.log("家族数量: " + Object.keys(familyData.families).length);

// 遍历家族
for (var fid in familyData.families) {
    var family = familyData.families[fid];
    console.log("家族: " + family.name);
    console.log("成员: " + family.members.join(", "));
}
```

### 完整示例

```javascript
// 示例1：查找特定植物信息
var plant = pvz.plants.向日葵;
if (plant) {
    console.log("=== 植物信息 ===");
    console.log("名称: " + plant.name);
    console.log("代码: " + plant.code);
    console.log("序号: " + plant.order);
    console.log("碎片ID: " + plant.shardId);
}

// 示例2：统计各类数据数量
console.log("植物总数: " + Object.keys(pvz.plants).length);
console.log("僵尸总数: " + Object.keys(pvz.zombies).length);
console.log("世界总数: " + Object.keys(pvz.worlds).length);

// 示例3：遍历所有世界
var worlds = pvz.worlds;
for (var code in worlds) {
    console.log("世界: " + worlds[code].name + " (" + code + ")");
}

// 示例4：混合中英文访问
var 豌豆射手 = pvz.植物.全部.peashooter;
var 坚果墙 = pvz.植物.全部.坚果墙;
console.log(豌豆射手.name + " 和 " + 坚果墙.name);
```

### 注意事项

1. **数据来源**：所有数据从 `assets/pvz2tool/pvz/` 目录下的 JSON 文件加载
2. **缓存机制**：数据会缓存在内存中，首次访问后后续访问更快
3. **名称匹配**：通过中文名访问时，名称会自动转为小写进行匹配
4. **数据不存在**：访问不存在的代码或名称会返回 `undefined`

---

## 7.5. storage - 持久化存储配置

持久化存储 API，基于 SharedPreferences。数据存储在应用私有目录，重启后保留。

**支持的类型**：String、Number、Boolean、Object、Array

##### storage.get / storage.获取

获取存储的值。

```javascript
let name = storage.get("username");  // "张三"
let lvl = storage.get("level");      // 99
let settings = storage.get("settings");  // { theme: "dark" }
```

##### storage.set / storage.设置

设置存储的值。

```javascript
storage.set("username", "张三");
storage.set("level", 99);
storage.set("settings", { theme: "dark", sound: true });
storage.set("items", ["apple", "banana", "cherry"]);
```

##### storage.delete / storage.删除

删除指定的键。

```javascript
storage.delete("username");
```

##### storage.has / storage.有

检查键是否存在。

```javascript
let exists = storage.has("username");  // true 或 false
```

##### storage.clear / storage.清空

清空所有存储的数据。

```javascript
storage.clear();
```

##### storage.keys / storage.键列表

获取所有键的列表。

```javascript
let keys = storage.keys();  // ["username", "level", "settings"]
```

##### storage.getAll / storage.获取全部

获取所有存储的数据。**返回数组**（按 key 顺序排列），每个元素为对应 key 的值，而非以 key 为字段的对象。

```javascript
let all = storage.getAll();
// 返回数组，例如：["张三", 99, { theme: "dark", sound: true }]
// 如需 key-value 形式，可结合 storage.keys() 自行组装：
let keys = storage.keys();
let map = {};
for (let i = 0; i < keys.length; i++) map[keys[i]] = all[i];
```

---

## 7.6. file - 文件操作

`file` 对象提供通用文件读写功能，支持占位符路径解析。

> **说明**：所有 API 同时支持英文和中文两种调用方式。

---

### 一、顶层简写方法（传路径，直接返回结果）

> **路径不存在时的行为**：
> - **读写类方法**（`readText` / `readBytes` / `writeText` / `writeBytes` / `appendText` / `rename` / `delete`）在路径不存在（或无法解析）时会**抛出异常**。
> - **属性类方法**（`size` / `lastModified` / `isDirectory` / `isFile` / `exists` / `parent`）在路径不存在时**不会抛异常**，而是返回安全默认值：`size`/`lastModified` 返回 `0`、`isDirectory`/`isFile`/`exists` 返回 `false`、`parent` 返回 `null`。
> - 若需要优雅处理"路径可能不存在"的场景，请使用 `file.resolve(path)`（返回中性对象，写操作可成功，读操作抛异常）。

以下方法直接传入占位符路径，无需先 `resolve()`。

#### file.readBytes / file.读字节

读取文件为字节数组。

**参数**：`placeholderPath` (string)
**返回**：Uint8Array

```javascript
let bytes = file.readBytes("$ITEM/config.bin");
```

#### file.readText / file.读文本

读取文件为文本（UTF-8）。

**参数**：`placeholderPath` (string)
**返回**：string

```javascript
let text = file.readText("$ITEM/config.txt");
```

#### file.writeBytes / file.写字节

写入字节数组到文件。

**参数**：`placeholderPath` (string), `bytes` (Uint8Array)

```javascript
file.writeBytes("$ITEM/output.bin", bytes);
```

#### file.writeText / file.写文本

写入文本到文件（UTF-8）。

**参数**：`placeholderPath` (string), `text` (string)

```javascript
file.writeText("$ITEM/config.txt", "hello");
```

#### file.appendText / file.追加文本

追加文本到文件末尾（UTF-8），文件不存在则自动创建。

**参数**：`placeholderPath` (string), `text` (string)

```javascript
file.appendText("$WORK_DIR/log.txt", "new line\n");
```

#### file.copy / file.复制 / file.复制到

通过解压管线（extract）复制文件。源路径会先转换为 `internalPath`（支持 `$SMF`/`$ITEM`/`$JS_DIR` 降级规则），然后解压到目标位置。`toPath` 的**判定规则**（取决于是否带扩展名）如下：

- **目标目录**：当 `toPath` 为目录，或**不带文件扩展名**时，视为目标目录，源文件以**原文件名**复制进该目录。
- **目标文件**：当 `toPath` **带有文件扩展名**（或已存在同名文件）时，视为目标文件路径，复制后会将文件**重命名为 `toPath` 指定的文件名**。

> **注意**：`toPath` 既可以填目录也可以填带扩展名的文件路径，二者行为不同，请按需选择。

**参数**：`fromPath` (string), `toPath` (string)

**示例**：
```javascript
// toPath 不带扩展名 → 视为目标目录，源文件以原文件名 source.bin 复制
file.copy("$SMF/source.bin", "$WORK_DIR/output/");
// 结果：$WORK_DIR/output/source.bin

// toPath 带扩展名 → 视为目标文件，复制后重命名为 target.bin
file.copy("$SMF/source.bin", "$WORK_DIR/output/target.bin");
// 结果：$WORK_DIR/output/target.bin

// 将 ITEM 资源复制到指定目录（支持降级）
file.copy("$ITEM/config.bin", "$WORK_DIR/extracted/");
```

**等价实现**（以目标目录为例）：
```javascript
var internalPath = path.toInternalPath(fromPath);
file.mkdir(toPath);          // 确保目标目录存在
ui.extract([internalPath], toPath, "");
// 若 toPath 带扩展名（目标文件），extract 后会额外将文件重命名为该文件名
```

#### file.rename / file.重命名 / file.移动到

重命名或移动文件/目录。

**参数**：`fromPath` (string), `toPath` (string)
**返回**：boolean

```javascript
file.rename("$WORK_DIR/old.txt", "$WORK_DIR/new.txt");
```

#### file.delete / file.删除

删除文件或目录。

**参数**：`placeholderPath` (string)
**返回**：boolean

```javascript
file.delete("$ITEM/temp.bin");
```

#### file.exists / file.存在

检查文件是否存在。

**参数**：`placeholderPath` (string)
**返回**：boolean

```javascript
if (file.exists("$ITEM/config.txt")) { ... }
```

#### file.list / file.列表

列出目录下的直接子项，返回文件对象数组。

- 使用 `listDirectory()` 实现，**不拷贝 assets/SAF 内容到缓存**，只返回子项名列表
- 返回的文件对象使用**占位符路径**（如 `$SMF/xxx`），而非缓存路径
- 路径不是目录或不存在时返回空数组 `[]`（不会返回 `null`）
- 目录为空时返回空数组 `[]`

**参数**：`placeholderPath` (string)
**返回**：Array\<文件对象\>（路径无效时为空数组 `[]`）

**示例**：
```javascript
let children = file.list("$SMF/packages/");
if (children) {
    for (let f of children) {
        console.log(f.name, f.isFile);
    }
}
```

#### file.mkdir / file.创建目录 / file.mkdirs

创建目录（包括所有不存在的父目录）。

**参数**：`placeholderPath` (string)
**返回**：boolean

```javascript
file.mkdir("$WORK_DIR/subdir/");
```

#### file.size / file.大小 / file.length / file.长度

获取文件大小（字节），目录返回 0。

**参数**：`placeholderPath` (string)
**返回**：number

```javascript
let sz = file.size("$ITEM/data.bin");
```

#### file.isDirectory / file.是目录

检查路径是否为目录。

**参数**：`placeholderPath` (string)
**返回**：boolean

```javascript
if (file.isDirectory("$SMF/packages/")) { ... }
```

#### file.isFile / file.是文件

检查路径是否为文件。

**参数**：`placeholderPath` (string)
**返回**：boolean

#### file.lastModified / file.修改时间

获取最后修改时间（Unix 毫秒时间戳）。

**参数**：`placeholderPath` (string)
**返回**：number

#### file.extension / file.扩展名

获取扩展名（不含开头的点），无扩展名返回空字符串。

**参数**：`placeholderPath` (string)
**返回**：string

```javascript
let ext = file.extension("$ITEM/data.bin"); // "bin"
```

#### file.parent / file.父目录

获取父目录路径，无父目录返回 `null`。

**参数**：`placeholderPath` (string)
**返回**：string | null

```javascript
let p = file.parent("$ITEM/config.txt"); // "$ITEM"
```

---

### 二、file.resolve — 文件对象

`file.resolve(path)` 根据路径类型返回不同的文件对象，API 不同：

- **路径存在且为目录** → 返回**目录对象**（`isDirectory=true`）：提供 `list()`、`mkdir()`，不提供文件读写 API
- **路径存在且为文件** → 返回**文件对象**（`isFile=true`）：提供 `readText()`、`writeText()`、`copyTo()` 等文件 API，不提供 `list()`、`mkdir()`
- **路径不存在** → 返回**中性对象**：同时提供文件对象与目录对象的全部 API（`readText()`/`readBytes()`/`writeText()`/`writeBytes()`/`copyTo()`/`appendText()` 与 `list()`/`mkdir()` 均可用）；属性方面 `isDirectory=false`、`isFile=true`、`exists()=false`、`size` 为 `0`，写操作可成功（自动创建），读操作会抛异常（注意：因兼具文件对象特征，中性对象的 `isFile` 实际为 `true` 而非 `false`）

```javascript
let f = file.resolve("$SMF/packages/icon.bin");
let bytes = f.readBytes();
f.writeBytes(modified);
```

#### 文件对象属性

| 属性 | 中文别名 | 类型 | 说明 |
|------|----------|------|------|
| `name` | `文件名` | string | 文件名（不含目录部分） |
| `normalizePath` | `规范路径` | string | 占位符路径（如 `$SMF/xxx`），与传入 `file.resolve()` 的路径对应 |
| `path` | `路径` | string | 解析后的绝对路径（缓存文件显示占位符路径） |
| `internalPath` | `内部路径` | string | assets 内部路径（供 `AssetExtractorHolder.resource()` / `ui.extract` 使用，通过 `path.toInternalPath()` 同逻辑计算） |
| `size` | `大小` | number | 文件大小（字节），目录返回 0 |
| `isDirectory` | `是目录` | boolean | 是否为目录 |
| `isFile` | `是文件` | boolean | 是否为文件 |
| `lastModified` | `修改时间` | number | 最后修改时间（Unix 毫秒） |
| `extension` | `扩展名` | string | 扩展名（不含点），无则返回 `""` |
| `parent` | `父目录` | string\|null | 父目录绝对路径，无则为 `null` |

#### 文件对象方法

> **注意**：`file.resolve(path)` 根据路径类型返回不同的对象，API 不同：
> - **目录对象**（`isDirectory=true`）：提供 `list()`、`mkdir()`，**不提供** `readText()`、`readBytes()`、`writeText()`、`writeBytes()`、`copyTo()`、`appendText()`
> - **文件对象**（`isFile=true`）：提供 `readText()`、`readBytes()`、`writeText()`、`writeBytes()`、`copyTo()`、`appendText()`，**不提供** `list()`、`mkdir()`
> - **中性对象**（路径不存在）：提供全部 API，属性返回默认值（`size=0`、`isDirectory=false`、`isFile=true`、`exists=false`），写操作可成功（自动创建），读操作会抛异常

| 方法 | 中文别名 | 说明 |
|------|----------|------|
| `readBytes()` | `读字节()` | （仅文件对象）读取字节数组 |
| `readText()` | `读文本()` | （仅文件对象）读取文本（UTF-8） |
| `writeBytes(bytes)` | `写字节(bytes)` | （仅文件对象）写入字节数组 |
| `writeText(text)` | `写文本(text)` | （仅文件对象）写入文本（UTF-8） |
| `appendText(text)` | `追加文本(text)` | （仅文件对象）追加文本 |
| `copyTo(toPath)` | `复制到(toPath)` | （仅文件对象）通过解压管线复制文件；`toPath` 带扩展名时按目标文件处理并重命名，否则视为目标目录 |
| `renameTo(newPath)` | `重命名到(newPath)` / `移动到(newPath)` | 重命名/移动（文件/目录均支持） |
| `delete()` | `删除()` | 删除（文件/目录均支持） |
| `exists()` | `存在()` | 是否存在（文件/目录均支持） |
| `mkdir()` / `mkdirs()` | `创建目录()` | （仅目录对象）创建目录（含父目录） |
| `list()` | `列表()` | （仅目录对象）列出子项，返回文件对象数组 |

#### 完整示例

```javascript
// 读取文件信息
let f = file.resolve("$SMF/packages/icon.bin");
console.log("名称:", f.name);
console.log("大小:", f.size, "字节");
console.log("扩展名:", f.extension);
console.log("修改时间:", new Date(f.lastModified));
console.log("父目录:", f.parent);

// 链式读写
file.resolve("$SMF/data.bin").writeBytes(
    file.resolve("$SMF/data_backup.bin").readBytes()
);

// 遍历目录
let children = file.resolve("$SMF/packages/").list();
for (let child of children) {
    console.log(child.name, child.isFile ? "文件" : "目录");
}

// 创建目录后写入
let dir = file.resolve("$WORK_DIR/output/");
dir.mkdir();
dir.resolve("result.txt").writeText("done");

// 重命名
file.resolve("$WORK_DIR/old.txt").renameTo("$WORK_DIR/new.txt");
```


---

## 8. ui - 用户界面

提供弹窗和进度条功能。

### 方法

#### ui.alert / ui.提示

显示一个带单按钮的提示对话框。

**参数**：
- `title` (string): 弹窗标题
- `message` (string): 弹窗内容

**返回**：void - 用户点击确定后 resolve

**示例**：
```javascript
ui.alert("兑换成功", "恭喜获得豪华礼包！");
```

#### ui.confirm / ui.确认

显示确认对话框。

**参数**：
- `title` (string): 标题
- `message` (string): 消息内容

**返回**：boolean - 用户点击确定返回 true，取消返回 false

**示例**：
```javascript
var confirmed = ui.confirm("确认删除", "确定要删除这个文件吗？");
if (confirmed) {
    // 执行删除
}
```

#### ui.prompt / ui.输入

显示输入对话框。

**参数**：
- `title` (string): 标题
- `message` (string): 提示消息
- `defaultValue` (string, 可选): 输入框默认值

**返回**：string|null - 用户输入的字符串，取消返回 null

**示例**：
```javascript
var name = ui.prompt("输入名称", "请输入文件名", "默认名称");
if (name !== null) {
    console.log("输入: " + name);
}
```

#### ui.progress / ui.进度

显示进度对话框。

**参数**：
- `title` (string): 标题
- `options` (object, 可选): 选项

**options 参数**：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `message` | string | "" | 初始消息 |
| `indeterminate` | boolean | false | 是否不确定模式 |
| `showCancel` | boolean | true | 是否显示取消按钮 |

**返回**：ProgressController - 进度控制器对象

**ProgressController 方法**：

| 方法 | 中文别名 | 说明 |
|------|----------|------|
| `update(message?, progress?)` | `更新(message?, progress?)` | 更新进度 (progress: 0.0-1.0) |
| `close()` | `关闭()` | 关闭进度对话框 |

**示例**：
```javascript
var progress = ui.progress("正在处理...", { showCancel: true });

try {
    progress.update("加载中...", 0.1);
    progress.更新("处理中...", 0.5);  // 混用中英文
} finally {
    progress.close();
    progress.关闭();  // 混用中文
}
```

#### ui.extract / ui.解压

从工具箱资源目录解压文件到目标目录。

**参数**：
- `sourcePaths` (Array\<string\>): 资源路径数组，如 `["pvz2tool/version/v1/smf/dynamic.rsb.smf"]`
- `targetDir` (string): 目标目录路径（绝对路径或占位符路径）
- `sectionName` (string, 可选): 栏目名称（用于日志显示）

**示例**：
```javascript
// 解压单个资源
ui.extract(
    ["pvz2tool/version/v1/smf/dynamic.rsb.smf"],
    "$GAME_SMF"
);
```

## 8.5. js - JS 执行器

`js` 对象提供在 JS 脚本中动态执行其他 JS 代码或 JS 文件的能力。

> **别名**：`js.run()` / `js.运行()` / `js.call()` / `js.执行()` 均可访问同一方法。

### 方法

#### js.run / js.运行 / js.call / js.执行

动态执行 JS 表达式或 JS 文件。

**参数**：
- `expr` (string): JS 表达式或 JS 文件路径（以 `.js` 结尾）

**执行逻辑**：
1. **如果 `expr` 以 `.js` 结尾**：
   - 如果路径以 `/` 开头（如 `/data/data/com.example/files/test.js`），作为**绝对路径**直接从本地文件系统读取
   - 否则从 `assets/js/` 目录读取 JS 文件并执行
   - 如果找不到文件，则尝试从工作目录读取
   - 如果都找不到，则直接编译执行 `expr` 字符串
2. **如果 `expr` 不以 `.js` 结尾**：
   - 直接编译执行 `expr` 表达式

**返回**：any - JS 执行结果

**示例**：
```javascript
// 执行 JS 表达式
var result = js.run("1 + 2");  // 3
var result2 = js.run("new Date().getFullYear()");  // 当前年份

// 执行 assets/js/ 目录下的 JS 文件（相对路径）
js.run("test.js");  // 执行 assets/js/test.js
js.运行("test.js");  // 中文别名

// 执行绝对路径的 JS 文件
js.run("/data/data/com.example/files/scripts/helper.js");

// 混用中英文
js.call("test.js");
js.执行("1 + 2");
```

### 完整示例

```javascript
// 示例1：执行简单表达式
var sum = js.run("var a = 1; var b = 2; a + b;");
console.log("结果：" + sum);  // 3

// 示例2：执行 JS 文件（assets/js/ 目录下）
// 假设 assets/js/helper.js 内容：
// var helper = { add: (a, b) => a + b };
// helper;
js.run("helper.js");  // 执行后返回 helper 对象

// 示例3：执行绝对路径的 JS 文件
js.run("/data/data/com.example/files/scripts/custom.js");

// 示例4：动态生成并执行代码
var code = "var x = 10; x * 2;";
var result = js.run(code);  // 20

// 示例5：在 BUTTON 中使用
// jsScript: |
//   var year = js.run("new Date().getFullYear()");
//   "当前年份：" + year;
```

---

## 8.6. audio - 音频控制

`audio` 对象提供背景音乐（BGM）与音效（SFX）音量控制，取值均为 `0.0`（静音）~ `1.0`（最大）。

> **中文别名说明**：`audio` 下的所有方法均支持中文别名。

### 方法

#### audio.getBgmVolume / audio.获取背景音乐音量

获取当前背景音乐音量。

**返回**：number - 音量值（0.0~1.0）

```javascript
var v = audio.getBgmVolume();   // 或 audio.获取背景音乐音量()
```

#### audio.setBgmVolume / audio.设置背景音乐音量

设置背景音乐音量，会立即同步更新正在播放的音乐。

**参数**：`volume` (number) - 音量值（0.0~1.0，超出范围会被钳制）

**返回**：void

```javascript
audio.setBgmVolume(0.5);        // 或 audio.设置背景音乐音量(0.5)
```

#### audio.getSfxVolume / audio.获取音效音量

获取当前音效音量。

**返回**：number - 音量值（0.0~1.0）

```javascript
var sv = audio.getSfxVolume();  // 或 audio.获取音效音量()
```

#### audio.setSfxVolume / audio.设置音效音量

设置音效音量，会同步更新所有已存在的音效播放器。

**参数**：`volume` (number) - 音量值（0.0~1.0，超出范围会被钳制）

**返回**：void

```javascript
audio.setSfxVolume(0.8);        // 或 audio.设置音效音量(0.8)
```

---

## 9. assets - 资源访问

`assets` 对象提供对工具箱内置资源的访问功能，支持本地覆盖优先和 URL 资源。

此API仅支持 pvz2tool 目录(手动狗头)

> **路径规则**：
> - **相对路径**（如 `pvz2tool/config.json`）：相对于 `pvz2tool` 工作目录解析（本地优先 > Assets）
> - **绝对路径**（以 `/` 开头，如 `/data/data/com.example/files/config.json`）：直接使用本地文件系统，无视工作目录
> - **URL**（以 `http://` 或 `https://` 开头）：直接使用远程资源

### 方法

#### assets.list / assets.列表

列出资源目录下的所有文件（本地优先）。

**参数**：
- `path` (string): 资源相对路径

**返回**：Array\<string\> - 文件路径数组

**示例**：
```javascript
var files = assets.list("pvz2tool/version/v1/smf");
console.log("SMF 目录文件:", files);
```

#### assets.listAssets / assets.列表Assets

列出 assets 目录下的所有文件（仅 APK 内置资源）。

**参数**：
- `path` (string): assets 相对路径

**返回**：Array\<string\> - 文件路径数组

**示例**：
```javascript
var files = assets.listAssets("pvz2tool/sound");
console.log("音效文件:", files);
```

#### assets.exists / assets.存在

检查资源是否存在。

**参数**：
- `path` (string): 资源相对路径

**返回**：boolean

**示例**：
```javascript
if (assets.exists("pvz2tool/video/opening.mp4")) {
    console.log("开场视频存在");
}
```

#### assets.info / assets.信息

获取资源详细信息。

**参数**：
- `path` (string): 资源相对路径

**返回**：Object - 资源信息对象

| 属性 | 类型 | 说明 |
|------|------|------|
| `exists` | boolean | 是否存在 |
| `isDirectory` | boolean | 是否为目录 |
| `isFile` | boolean | 是否为文件 |
| `size` | number | 文件大小（字节），-1 表示未知 |
| `lastModified` | number | 最后修改时间戳 |

**示例**：
```javascript
var info = assets.info("pvz2tool/sound/bg_music.wav");
console.log("文件大小:", info.size, "字节");
console.log("存在:", info.exists);
```

#### assets.read / assets.读取

读取资源文件内容为字符串。

**参数**：
- `path` (string): 资源相对路径

**返回**：string | undefined - 文件内容，不存在则返回 undefined

**示例**：
```javascript
var content = assets.read("pvz2tool/config.json");
if (content) {
    console.log("配置内容:", content);
}
```

#### assets.readBytes / assets.读取字节

读取资源文件为字节数组（Uint8Array）。

**参数**：
- `path` (string): 资源相对路径

**返回**：Uint8Array | undefined

**示例**：
```javascript
var bytes = assets.readBytes("pvz2tool/image/icon.png");
console.log("图片大小:", bytes.length);
```

#### assets.readBase64 / assets.读取Base64

读取资源文件并编码为 Base64 字符串。

**参数**：
- `path` (string): 资源相对路径

**返回**：string | undefined - Base64 编码字符串

**示例**：
```javascript
var base64 = assets.readBase64("pvz2tool/image/logo.png");
// 可用于 data URI
var dataUri = "data:image/png;base64," + base64;
```

#### assets.readArrayBuffer / assets.读取ArrayBuffer

读取资源文件为 ArrayBuffer（用于二进制数据处理）。

**参数**：
- `path` (string): 资源相对路径

**返回**：ArrayBuffer | undefined

**示例**：
```javascript
var buffer = assets.readArrayBuffer("pvz2tool/data.bin");
// 处理二进制数据
```

---

## 9.5. http - 网络请求

`http` / `网络` 对象提供 HTTP 网络请求能力，基于 Ktor 客户端实现，支持 GET、POST、PUT、DELETE、PATCH、HEAD 等常用方法，以及完整的请求头、请求体、超时配置。

### 9.5.1 请求方法概览

| 方法 | 中文别名 | 签名 | 说明 |
|------|----------|------|------|
| `http.get` | `网络.获取` | `get(url, options?)` | GET 请求，适合查询数据 |
| `http.post` | `网络.提交` | `post(url, body?, options?)` | POST 请求，适合提交数据 |
| `http.put` | `网络.上传` | `put(url, body?, options?)` | PUT 请求，适合替换资源 |
| `http.delete` | `网络.删除` | `delete(url, options?)` | DELETE 请求，适合删除资源 |
| `http.patch` | `网络.修改` | `patch(url, body?, options?)` | PATCH 请求，适合局部更新 |
| `http.head` | `网络.头部` | `head(url, options?)` | HEAD 请求，只获取响应头 |
| `http.request` | `网络.请求` | `request(options)` | 通用请求，通过 options.method 指定方法 |

### 9.5.2 options 参数

所有方法均支持可选的 `options` 对象：

| 字段 | 中文别名字段 | 类型 | 默认值 | 说明 |
|------|-------------|------|--------|------|
| `headers` | `请求头` | `object` | `{}` | 请求头键值对 |
| `contentType` | `内容类型` | `string` | `"application/json"` | 请求体的 Content-Type |
| `timeout` | `超时` | `number` | `30000` | 超时毫秒数 |

`http.request` 的 `options` 额外支持：

| 字段 | 中文别名字段 | 类型 | 说明 |
|------|-------------|------|------|
| `url` | `地址` | `string` | 请求地址（必填） |
| `method` | `方法` | `string` | HTTP 方法，默认 `"GET"` |
| `body` | `数据` | `string` | 请求体 |

### 9.5.3 Response 响应对象

每个请求方法均返回一个 Response 对象：

| 属性/方法 | 中文别名 | 类型 | 说明                          |
|-----------|----------|------|-----------------------------|
| `response.status` | — | `number` | HTTP 状态码（如 200、404）         |
| `response.ok` | — | `boolean` | 状态码是否在 200~299 范围内          |
| `response.statusText` | — | `string` | 状态文本（如 `"OK"`、`"Not Found"`） |
| `response.body` | — | `string` | 响应体文本                       |
| `response.headers` | — | `object` | 响应头键值对对象                    |
| `response.json()` | `response.解析JSON()` | `string` | 将 body 规范化为 JSON 对象使用       |

### 9.5.4 使用示例

#### 简单 GET 请求

```javascript
var res = http.get("https://httpbin.org/get");
if (res.ok) {
    var data = res.json();
    console.log("状态码：" + res.status);
    console.log("响应体：", data);
} else {
    console.error("请求失败：" + res.status + " " + res.statusText);
}
```

#### 带请求头的 GET 请求

```javascript
var res = http.get("https://api.example.com/data", {
    headers: {
        "Authorization": "Bearer token123",
        "Accept": "application/json"
    },
    timeout: 15000
});
var data = JSON.parse(res.body);
console.log(data);
```

#### POST JSON 数据

```javascript
var payload = JSON.stringify({ name: "test", value: 42 });
var res = http.post("https://httpbin.org/post", payload, {
    headers: { "X-Custom-Header": "hello" },
    contentType: "application/json"
});
if (res.ok) {
    console.log("提交成功：" + res.status);
}
```

#### 中文别名调用

```javascript
var res = 网络.获取("https://httpbin.org/get");
console.log("状态：" + res.ok);
```

#### 通用 request 方法

```javascript
var res = http.request({
    url: "https://httpbin.org/put",
    method: "PUT",
    body: JSON.stringify({ key: "value" }),
    contentType: "application/json",
    headers: { "Authorization": "Bearer abc" },
    timeout: 20000
});
console.log(res.status, res.body);
```

#### 读取响应头

```javascript
var res = http.get("https://httpbin.org/headers");
var contentType = res.headers["Content-Type"];
console.log("Content-Type:", contentType);
```

---

## 10. data - SMF 数据访问

`data` 对象提供对 SMF 数据的便捷访问能力。**仅当栏目项配置了 `smfList` 时可用**。

> **性能优化**：SMF 打包会延迟到**进入游戏**或**切换版本**时统一执行，无需手动打包。

### 版本隔离

SMF 修改按版本隔离，每个版本的修改独立管理：
- 切换版本时，当前版本的 pending 修改会被立即打包
- 进入游戏时，目标版本的修改会被打包
- 避免跨版本 SMF 修改污染

### 数据结构

`data` 对象的结构按 `smfList` 中定义的名称组织：

```javascript
data.<smfName>.<subDir>.<fileName>.<field>
data.obb.<subDir>.<fileName>.<field> // 如果是OBB文件则默认名为obb
```

例如，`smfList: ["dynamic"]` 配置下：
```javascript
data.dynamic.packages.npcs.load()  // 加载 npcs.rton
```

### RTON/JSON 文件对象

当访问到 RTON/JSON 文件时，返回的对象包含：

| 属性/方法 | 中文别名 | 说明 |
|-----------|----------|------|
| `path` | `路径` | extracted 目录中的文件绝对路径 |
| `readPath` | `读取路径` | 实际读取源路径（优先 modified 目录） |
| `modifiedPath` | `修改路径` | modified 目录中的镜像路径 |
| `load()` | `加载()` | 同步加载文件内容为 JS 对象 |
| `save()` | `保存()` | 同步保存修改到 modified 目录 |
| `readBytes()` | `读字节()` | 同步读取文件为字节数组 |
| `writeBytes(bytes)` | `写字节(bytes)` | 同步写入字节数组 |

### 通用二进制文件对象

当访问到非 RTON/JSON 文件（如 .bin、.dat、.png 等）时，返回的对象包含：

| 属性/方法 | 中文别名 | 说明 |
|-----------|----------|------|
| `path` | `路径` | extracted 目录中的文件绝对路径 |
| `readPath` | `读取路径` | 实际读取源路径（优先 modified 目录） |
| `modifiedPath` | `修改路径` | modified 目录中的镜像路径 |
| `readBytes()` | `读字节()` | 同步读取文件为字节数组 |
| `writeBytes(bytes)` | `写字节(bytes)` | 同步写入字节数组 |
| `readText()` | `读文本()` | 同步读取文件为文本（UTF-8） |
| `writeText(text)` | `写文本(text)` | 同步写入文本（UTF-8） |

### load/save 语义

- **load()**: 先从 modified 目录读取（保留历史修改），无则从 extracted 读取
- **save()**: 写入 modified 目录（不会覆盖原始 extracted 文件）

### data 示例

**RTON/JSON 文件**：
```javascript
// 加载并修改 npcs 数据
var npcs = data.dynamic.packages.npcs.load();
npcs["field"] = "value";
npcs.save();
```

**通用二进制文件**（同步操作，不需要 await）：
```javascript
// 读取二进制文件
let bytes = data.dynamic.packages.myfile.readBytes();
console.log("文件大小:", bytes.length);

// 写入二进制文件
data.dynamic.packages.myfile.writeBytes(modifiedBytes);

// 读取文本文件
let text = data.dynamic.packages.config.readText();
console.log("配置内容:", text);

// 写入文本文件
data.dynamic.packages.config.writeText("hello world");

// 查看文件路径
console.log("原始路径:", data.dynamic.packages.myfile.path);
console.log("读取源:", data.dynamic.packages.myfile.readPath);
console.log("写入目标:", data.dynamic.packages.myfile.modifiedPath);
```

---

## 11. this 对象 - 工具上下文

`this` 对象在脚本执行时自动注入，提供访问工具状态的能力。

> **中文别名说明**：`this` 下的所有属性均支持中文别名。

### 组件状态（可读写）

当通过 CHECKBOX 或 SLIDER 触发脚本时，可访问当前组件的状态。**这些属性支持直接赋值！**

| 属性 | 中文别名 | 类型 | 说明 |
|------|----------|------|------|
| `this.checked` | `this.选中` | boolean | CHECKBOX 当前选中状态（仅 CHECKBOX 触发时可读写） |
| `this.value` | `this.值` | number | SLIDER 当前值（仅 SLIDER 触发时可读写） |

**示例**：
```javascript
// 读取状态
if (this.checked) { ... }      // 英文
if (this.选中) { ... }          // 中文（等效）

// 直接修改状态（会同步到 UI！）
this.checked = false;           // 取消勾选
this.value = 50;               // 修改滑块值
```

### this.setValue / this.设置值

在 JS 脚本中修改栏目项的值。修改会同步更新到 UI 状态。

**参数**：
- `itemId` (string): 栏目项 ID
- `value` (any): 新值
  - CHECKBOX: boolean（true/false）
  - SLIDER: number（浮点数）
  - INPUT: string（文本）

**返回**：boolean - 是否设置成功

**示例**：
```javascript
// 修改 CHECKBOX 状态
this.setValue("my_checkbox_item", true);   // 勾选
this.setValue("my_checkbox_item", false);  // 取消勾选

// 修改 SLIDER 值
this.setValue("my_slider_item", 50);       // 设置为 50

// 修改 INPUT 值
this.setValue("my_input_item", "hello");   // 设置文本

// 通过 this.all 查找并修改
this.setValue(this.all.mySection.myCheckbox.id, true);

// SLIDER 修改示例：根据另一个 SLIDER 的值动态调整
var speed = this.all.gameSettings.speedSlider.value;
this.setValue("autoCollectThreshold", speed * 0.8);
```

### 版本信息

| 属性 | 中文别名 | 类型 | 说明 |
|------|----------|------|------|
| `this.version.id` | `this.版本.编号` | string | 版本 ID |
| `this.version.name` | `this.版本.名称` | string | 版本名称 |
| `this.version.baseAssetPath` | `this.版本.基础资源路径` | string | 基础资源路径（可能为 null） |
| `this.version.assetPath` | `this.版本.资源路径` | string | 解析后的资源路径 |
| `this.version.forceOverride` | `this.版本.强制覆盖` | boolean | 是否强制覆盖 |

### this.all / this.全部

`this.all` 包含所有栏目的信息和状态。

#### 栏目属性（均支持中文别名）

| 属性 | 中文别名 | 说明 |
|------|----------|------|
| `section.id` | `编号` | 栏目 ID |
| `section.title` | `标题` | 栏目标题 |
| `section.theme` | `主题` | 主题名称 |
| `section.targetPath` | `目标路径` | 目标目录绝对路径 |
| `section.items` | `项目` | 栏目下所有项 |
| `section.checkedItems` | `选中项` | 所有勾选的 CHECKBOX 项 |
| `section.sliderValues` | `滑块值` | 所有 SLIDER 的值 |
| `section.inputValues` | `输入值` | 所有 INPUT 的值 |
| `section.infoValues` | `信息值` | 所有 INFO 的值 |
| `section.descriptionValues` | `描述值` | 所有 DESCRIPTION 类型项的值 |

#### 栏目项属性（均支持中文别名）

| 属性 | 中文别名 | 类型 | 说明 |
|------|----------|------|------|
| `item.id` | `编号` | string | 项目 ID |
| `item.name` | `名称` | string | 显示名称 |
| `item.desc` | `描述` | string | 描述文字 |
| `item.type` | `类型` | string | 类型 |
| `item.icon` | `图标` | string | 图标名称 |
| `item.assetPath` | `资源路径` | string | 原始资源路径配置 |
| `item.groupId` | `分组` | string | 分组 ID |
| `item.displayName` | `显示名` | string | 显示名称 |
| `item.sectionId` | `栏目编号` | string | 所属栏目 ID |
| `item.sectionTitle` | `栏目标题` | string | 所属栏目标题 |
| `item.resolvedPath` | `解析后路径` | string | 解析后的完整资源路径 |

#### 栏目项类型特定属性（可读写）

| 类型 | 属性 | 中文别名 | 说明 |
|------|------|----------|------|
| RADIO | `item.selected` / `item.checked` | `选中` | 是否选中（可读写；`selected` 与 `checked` 均可，二者等价） |
| CHECKBOX | `item.checked` | `勾选` / `选中` | 选中状态（可读写） |
| SLIDER | `item.value` | `值` | 当前值（可读写） |
| SLIDER | `item.minValue` | `最小值` | 最小值 |
| SLIDER | `item.maxValue` | `最大值` | 最大值 |
| SLIDER | `item.step` | `步长` | 步进值 |
| SLIDER | `item.valueSuffix` | `值后缀` | 值后缀 |
| INPUT | `item.value` | `值` | 当前输入值（可读写） |
| INPUT | `item.inputDefault` | `默认输入` | 默认值 |
| INPUT | `item.placeholder` | `占位符` | 占位符 |
| INFO | `item.value` | `值` | 当前信息值（可读写） |

**重要**：CHECKBOX、SLIDER、INPUT 的 `value`/`checked` 属性支持直接赋值！

```javascript
// 直接修改（推荐方式）
this.all.mySection.myCheckbox.checked = true;
this.all.mySection.mySlider.value = 75;
this.all.mySection.myInput.value = "new text";
```

#### 栏目整体状态

```javascript
var section = this.all.栏目ID;

// 所有勾选的 CHECKBOX 项
// 返回格式: { itemId1: true, itemId2: true, ... }
var checked = section.checkedItems;

// 所有 SLIDER 的值
// 返回格式: { itemId1: value1, itemId2: value2, ... }
var sliders = section.sliderValues;

// 所有 INPUT 的值
// 返回格式: { itemId1: value1, itemId2: value2, ... }
var inputs = section.inputValues;

// 所有 INFO 的值
// 返回格式: { itemId1: value1, itemId2: value2, ... }
var infos = section.infoValues;

// 所有 DESCRIPTION 的值
// 返回格式: { itemId1: value1, itemId2: value2, ... }
var descriptions = section.descriptionValues;
```

### this.findById / this.查找

快速查找栏目或栏目项。**仅接受一个 `id` 参数**：先在全局栏目项中查找，找不到再按栏目 ID 查找；返回对应的栏目项对象或栏目对象，未找到返回 `null`。

```javascript
// 英文写法：按栏目项 ID 查找
var item = this.findById("auto_collect_js");

// 中文写法（等效）
var item = this.查找("auto_collect_js");

// 也可按栏目 ID 查找（返回栏目对象）
var section = this.findById("js_linkage");
var section2 = this.查找("js_linkage");
```

### 游戏 Activity

```javascript
this.gameActivity  // string - 当前配置的 gameActivity 名称
this.游戏界面      // 中文别名
```

### 版本隔离的状态管理

栏目状态（开关/RADIO/SLIDER/INPUT）按**版本**隔离存储，每个版本的配置独立管理：

- **切换版本时**：开关状态自动重置为当前版本的默认值

这意味着：
- 版本 A 打开的开关，切换到版本 B 后会自动关闭（除非版本 B 默认开启）
- 同一功能栏目在不同版本下可以有不同的默认配置

### 综合示例

```javascript
// 读取版本信息
console.log("版本: " + this.version.name);

// 读取栏目配置
var section = this.all.js_linkage;
console.log("栏目: " + section.title);

// 读取特定项状态
var autoCollect = this.all.js_linkage.auto_collect_js;
console.log("自动收集: " + (autoCollect.checked ? "启用" : "禁用"));

// 直接修改状态
autoCollect.checked = false;  // 取消勾选

// 遍历所有项
var items = section.items;
for (var key in items) {
    var item = items[key];
    console.log(item.name + " - " + item.type);
}

// 根据配置执行逻辑
if (this.all.js_linkage.speed_normal.selected) {
    console.log("正常速度模式");
}
```

---

## 12. 占位符路径

在脚本中使用占位符，系统会自动解析为实际路径。

### 支持的占位符

| 占位符 | 说明 | 示例 |
|--------|------|------|
| `$WORK_DIR` | 用户通过 SAF 选择的工作目录 | `$WORK_DIR/config.json` |
| `$GAME_SAVES` | 游戏存档目录 | `$GAME_SAVES/SeedChooserUserData.rton` |
| `$GAME_SMF` | 游戏 smf 目录（基于 config.smfDirectory） | `$GAME_SMF/main.rton` |
| `$SMF` | 当前选中版本的 smf 目录 | `$SMF/resources.rsb` |
| `$ITEM` | 栏目下的 item assetPath，降级到 $SMF | `$ITEM/config.json` |
| `$JS_DIR` | 栏目下的 item jsPath.dir，没有则逐级递升 | `$JS_DIR/helper.js` |
| `$APP_DATA` | 应用内部数据目录 | `$APP_DATA/shared_prefs/` |
| `$APP_FILES` | 应用内部文件目录 | `$APP_FILES/config.json` |
| `$APP_CACHE` | 应用内部缓存目录 | `$APP_CACHE/temp.dat` |
| `$ANDROID_DATA` | 应用外部数据根目录 | `$ANDROID_DATA/files/` |
| `$ANDROID_FILES` | 应用外部文件目录 | `$ANDROID_FILES/saves/` |
| `$ANDROID_CACHE` | 应用外部缓存目录 | `$ANDROID_CACHE/export/` |

### 占位符解析规则

- `$SMF` - 基于当前选中版本的 `assetPath` 解析；**降级规则**：版本目录不存在时回退到 `baseAssetPath`
- `$ITEM` - 基于栏目下 item 的 `assetPath` 解析（默认 `version/版本ID/栏目ID/功能项ID`）；**降级规则**：item assetPath 不存在时降级到 `$SMF`
- `$JS_DIR` - 基于栏目下 item 的 `jsPath.dir` 解析（默认 `version/版本ID/栏目ID/功能项ID`）；**降级规则**：item jsPath.dir 不存在时自动向上查询
- `$GAME_SAVES` - 从配置 `sections[id="saves"].targetPath` 解析
- `$GAME_SMF` - `rootDirectory + config.smfDirectory`
- `$WORK_DIR` - 用户选择的 SAF 工作目录
- `$APP_DATA` / `$APP_FILES` / `$APP_CACHE` - 应用内部存储绝对路径（`/data/user/0/<包名>/...`），不依赖版本/栏目上下文
- `$ANDROID_DATA` / `$ANDROID_FILES` / `$ANDROID_CACHE` - 应用外部存储绝对路径（`/storage/emulated/0/Android/data/<包名>/...`），不依赖版本/栏目上下文

### 示例

```javascript
// 读取游戏存档
var savePath = path.resolve("$GAME_SAVES/SeedChooserUserData.rton");

// 解包当前选中版本的资源
var smfDir = path.resolve("$SMF");
rsb.unpack(smfDir + "/resources.rsb", smfDir + "/resources_unpacked");

// 操作工作目录
var configPath = path.resolve("$WORK_DIR/config.json");

// 解包游戏资源
var gameSmfDir = path.resolve("$GAME_SMF");
rsb.unpack(gameSmfDir + "/resources.rsb", gameSmfDir + "/unpacked");

// 读取应用内部文件
var internalFile = file.readText("$APP_FILES/my_config.json");

// 列出外部数据目录下的文件
var files = file.list("$ANDROID_FILES/");

// 读取外部缓存中的临时文件
var data = file.readText("$ANDROID_CACHE/export/result.json");
```

---

## 13. Number 扩展方法

数字类型扩展了加密/解密方法，可直接在数字上调用。

### 方法

| 方法 | 中文别名 |
|------|----------|
| `number.encrypt` | `number.加密` |
| `number.decrypt` | `number.解密` |

### 示例

```javascript
var raw = 1000;
var enc = raw.encrypt;     // 或 raw.加密
var dec = enc.decrypt;      // 或 enc.解密
```

---

## 完整示例

### 示例1：修复存档摘要

```javascript
var savePath = path.resolve("$GAME_SAVES/SeedChooserUserData.rton");
var obj = rton.load(savePath);
obj.zombie1_resources_digest = "122";
obj.save();
"修复完成";
```

### 示例2：解包 RSB 并显示进度

```javascript
var smfDir = path.resolve("$SMF");
var progress = ui.progress("解包中...", { showCancel: true });

try {
    rsb.unpack(
        smfDir + "/resources.rsb",
        smfDir + "/resources_unpacked",
        {
            onProgress: (pct, msg) => {
                progress.update(msg, pct);
            }
        }
    );
} finally {
    progress.close();
}
"解包完成";
```

### 示例3：数字加密解密

```javascript
var raw = 1000;
var enc = raw.encrypt;
var dec = enc.decrypt;
"原始值: " + raw + ", 加密: " + enc + ", 解密: " + dec;
```

### 示例4：根据栏目配置执行操作

```javascript
// 读取多个配置项
var speedMode = this.all.js_linkage.speed_normal.selected ? "正常" : "快速";
var autoCollect = this.all.js_linkage.auto_collect_js.checked;
var sunValue = this.all.js_linkage.sun_value_js.value;

console.log("=== 当前配置 ===");
console.log("速度: " + speedMode);
console.log("自动收集: " + (autoCollect ? "启用" : "禁用"));
console.log("阳光值: " + sunValue);
```

### 示例5：确认对话框

```javascript
var confirmed = ui.confirm("确认操作", "此操作不可恢复，确定要继续吗？");
if (!confirmed) {
    "已取消";
} else {
    // 执行实际操作
    "操作完成";
}
```

### 示例6：直接修改栏目项状态

```javascript
// 取消勾选某个复选框
this.all.js_linkage.auto_collect_js.checked = false;

// 设置滑块值
this.all.js_linkage.sun_value_js.value = 150;

// 通过 ID 修改
this.setValue("my_checkbox", true);
```

### 示例7：使用 SMF 数据（需配置 smfList）

```javascript
// 假设 dream.yml 中配置了 smfList: ["dynamic"]
// 加载并修改 npcs 数据
var npcs = data.dynamic.packages.npcs.load();
console.log("当前 NPC 数量:", Object.keys(npcs).length);

// 修改数据
npcs["coin"] = 9999;
npcs.save();

"SMF 数据修改完成";
```

---

## 10. picker - 文件选择器

`picker` 对象提供系统文件选择器（基于 Android SAF：文档/目录选择），可以让脚本在运行时由用户选择**目录、单个文件或多个文件**，并返回对应的文件对象（或文件对象数组），供后续读写使用。

> **异步说明**：三个方法都会**挂起当前 JS 协程**直到用户完成选择（与 `ui.alert()` 等行为一致），请直接 `await` 或同步取值：
> ```javascript
> let dir = picker.directory();   // 选择完成后才继续
> ```

### 10.1 方法

| 方法 | 中文别名 | 说明 |
|------|----------|------|
| `picker.directory(options?)` | `picker.选择目录()` / `picker.选择文件夹()` | 选择一个**目录**（ACTION_OPEN_DOCUMENT_TREE），返回文件对象；取消时返回 `undefined` |
| `picker.file(options?)` | `picker.选择文件()` | 选择一个**文件**（ACTION_OPEN_DOCUMENT），返回文件对象；取消时返回 `undefined` |
| `picker.files(options?)` | `picker.选择多个文件()` | 选择**多个文件**（ACTION_OPEN_DOCUMENT + 多选），返回文件对象**数组**；取消时返回空数组 `[]` |

**options 参数（可选对象）**：
- `mimeType`（string）：文件类型过滤，仅 `file` / `files` 模式生效。默认 `"*/*"`（所有类型）。例如 `"image/*"`、`"application/json"`。

### 10.2 返回：文件对象

`directory` / `file` 返回**单个文件对象**，`files` 返回**文件对象数组**。该文件对象的字段与方法与 [`file` 对象](#) 基本一致，但底层基于 SAF 的 `DocumentFile`（content URI），而非本地文件路径。

**字段**：

| 字段 | 中文别名 | 说明 |
|------|----------|------|
| `name` | `文件名` | 文件/目录名称 |
| `uri` | `地址` | 内容 URI 字符串（访问该文件的真实地址） |
| `path` / `normalizePath` | `路径` / `规范路径` | 同 `uri`（content URI） |
| `extension` | `扩展名` | 扩展名（不含点） |
| `size` | `大小` | 大小（字节，数字） |
| `isDirectory` | `是目录` | 是否为目录 |
| `isFile` | `是文件` | 是否为文件 |
| `lastModified` | `修改时间` | 最后修改时间（Unix 毫秒时间戳） |
| `parent` | `父目录` | 内容 URI 无法获取父目录，固定为 `undefined` |

**方法**：

| 方法 | 中文别名 | 说明 |
|------|----------|------|
| `exists()` | `存在()` | 是否存在 |
| `delete()` | `删除()` | 删除文件/目录 |
| `rename(newName)` | `重命名()` / `renameTo()` | 重命名 |
| `readBytes()` | `读字节()` | 读取为字节数组（Uint8Array） |
| `readText()` | `读文本()` | 读取为字符串（UTF-8） |
| `writeBytes(bytes)` | `写字节()` | 写入字节数组 |
| `writeText(text)` | `写文本()` | 写入字符串（UTF-8，覆盖） |
| `appendText(text)` | `追加文本()` | 追加字符串（UTF-8） |
| `list()` | `列表()` | 目录下列出子项（返回文件对象数组，仅目录可用） |
| `mkdir()` / `mkdirs()` | `创建目录()` | 目录 tree URI 无法就地创建，固定返回 `false` |
| `copy(toPath)` / `copyTo()` / `复制到()` | `复制()` / `复制到()` | 把选中的文件**复制**到本地/占位符路径（通过 `JsFileAccess` 解析，如 `$WORK_DIR/...`、绝对路径） |

> **与 `file` 对象的差异**：
> - `path` / `normalizePath` 是 content URI，而非本地绝对路径；无法直接用于 `file.resolve()` 等基于本地/占位符路径的 API。
> - `parent` 不可用（内容 URI 无父目录概念）。
> - `copy(toPath)` 方向为「选中的 SAF 文件 → 本地/工作目录路径」；反向（从占位符路径复制到 SAF）不在本对象职责内。
> - `copy(toPath)` 的**目标文件即使不存在也会自动创建**：解析 `toPath` 时若文件不存在，会在 SAF 树根（`$WORK_DIR` / `$GAME_SAVES` / `$GAME_SMF`）内新建该文件（含中间目录），再写入内容。只读占位符（`$SMF` / `$ITEM` / `$JS_DIR`）因目录只读仍会失败。

### 10.3 使用示例

```javascript
// 示例1：选择目录并列出其子文件
let dir = picker.directory();
if (dir) {
  console.log("选择的目录:", dir.name, dir.uri);
  dir.list().forEach(child => console.log("  -", child.name));
}

// 示例2：选择单个文本文件并读取/改写
let f = picker.file({ mimeType: "text/plain" });
if (f) {
  let content = f.readText();
  console.log("原内容:", content);
  f.writeText(content + "\n// 由脚本追加");
}

// 示例3：选择多个图片并批量复制到工作目录
let imgs = picker.files({ mimeType: "image/*" });
imgs.forEach((img, i) => {
  img.copy("$WORK_DIR/imported_" + i + "_" + img.name);
});
console.log("已导入", imgs.length, "张图片");
```

---

## 12. clipboard - 剪切板

基于 Android 系统 `ClipboardManager`，对系统剪切板进行文本读写与清空。

> **平台限制**：Android 10（API 29）及以上，应用仅能在自身处于前台（有焦点）时读取剪切板。本工具的 JS 运行于前台界面，读取可正常工作；后台/无焦点状态下 `read()` 可能返回 `undefined`。

### 12.1 clipboard.copy / clipboard.复制 / clipboard.写入

将指定字符串写入系统剪切板。

**参数**：`text` (string) — 要复制的内容（为 `undefined`/未传时复制空字符串）

**返回**：`undefined`

```javascript
// 复制指定字符串
clipboard.copy("这是要复制的内容");
clipboard.复制("中文别名同样可用");

// 配合其它 API：把读取到的文件内容复制到剪切板
let f = picker.file({ mimeType: "text/plain" });
if (f) clipboard.copy(f.readText());
```

### 12.2 clipboard.read / clipboard.读取 / clipboard.粘贴

读取剪切板当前的主文本内容。

**返回**：`string` — 剪切板文本；无内容 / 服务不可用 / 无焦点时返回 `undefined`

```javascript
let text = clipboard.read();
if (text !== undefined) {
  console.log("剪切板内容:", text);
} else {
  console.log("剪切板为空或当前无法读取");
}

// 别名
let pasted = clipboard.粘贴();
```

### 12.3 clipboard.clear / clipboard.清空

清空系统剪切板（写入一个空内容）。

**返回**：`undefined`

```javascript
clipboard.clear();
```

---

## 13. device - 设备信息

基于 Android 系统 API（`Build` / `WindowManager` / `ActivityManager` / `BatteryManager` / `ConnectivityManager` / `StatFs` 等），提供当前安卓设备的各种信息。支持按分组（`system` / `screen` / `memory` / `storage` / `battery` / `network` / `app`）获取，也支持 `device.info()` 一次性聚合全部信息。

> **说明**：所有信息均为实时读取（每次调用刷新），例如电池电量、网络状态会反映当前真实值，而非注册时的快照。

### 13.1 device.info / device.信息

一次性返回全部设备信息（聚合对象，各分组作为子对象存在）。

**返回**：`object` — 结构为 `{ system, screen, memory, storage, battery, network, app, isRooted }`

```js
let d = device.info();
console.log(d.system.model, d.system.androidVersion, d.screen.width, d.battery.level);
console.log("Root:", d.isRooted);
```

### 13.2 device.system / device.系统

系统相关信息（设备型号、厂商、安卓版本、内核、语言时区、是否模拟器等）。

| 方法 | 中文别名 | 返回 |
|------|----------|------|
| `system.info()` | `系统.信息()` | 系统信息聚合对象 |
| `system.model()` | `系统.型号()` | 设备型号（如 `Pixel 8`） |
| `system.brand()` | `系统.品牌()` | 品牌（如 `google`） |
| `system.manufacturer()` | `系统.制造商()` | 制造商（如 `Google`） |
| `system.device()` | `系统.设备代号()` | 设备代号（`Build.DEVICE`） |
| `system.product()` | `系统.产品()` | 产品名（`Build.PRODUCT`） |
| `system.board()` | `系统.主板()` | 主板（`Build.BOARD`） |
| `system.hardware()` | `系统.硬件()` | 硬件（`Build.HARDWARE`） |
| `system.androidVersion()` | `系统.安卓版本()` | 安卓版本号（`Build.VERSION.RELEASE`，如 `14`） |
| `system.sdkVersion()` | `系统.SDK版本()` | SDK 版本号（`Build.VERSION.SDK_INT`，如 `34`） |
| `system.codename()` | `系统.版本代号()` | 版本代号（`Build.VERSION.CODENAME`） |
| `system.incremental()` | `系统.版本增量()` | 版本增量（`Build.VERSION.INCREMENTAL`） |
| `system.securityPatch()` | `系统.安全补丁()` | 安全补丁级别（`Build.VERSION.SECURITY_PATCH`） |
| `system.bootloader()` | `系统.引导程序()` | Bootloader（`Build.BOOTLOADER`） |
| `system.display()` | `系统.显示版本()` | 显示版本（`Build.DISPLAY`） |
| `system.fingerprint()` | `系统.指纹()` | 系统指纹（`Build.FINGERPRINT`） |
| `system.kernelVersion()` | `系统.内核版本()` | 内核版本（`os.version`） |
| `system.isEmulator()` | `系统.是否模拟器()` | 是否模拟器（`boolean`） |
| `system.language()` | `系统.语言()` | 系统语言标签（如 `zh-CN`） |
| `system.timezone()` | `系统.时区()` | 系统时区 ID（如 `Asia/Shanghai`） |

### 13.3 device.screen / device.屏幕

屏幕分辨率、密度、刷新率等信息。

| 方法 | 中文别名 | 返回 |
|------|----------|------|
| `screen.info()` | `屏幕.信息()` | 屏幕信息聚合对象 |
| `screen.width()` | `屏幕.宽度()` | 屏幕宽（像素，`number`） |
| `screen.height()` | `屏幕.高度()` | 屏幕高（像素，`number`） |
| `screen.resolution()` | `屏幕.分辨率()` | 分辨率字符串（如 `1080x2400`） |
| `screen.densityDpi()` | `屏幕.屏幕密度DPI()` | 屏幕密度 DPI（`number`） |
| `screen.density()` | `屏幕.密度比例()` | 密度比例（`density`，`number`） |
| `screen.scaledDensity()` | `屏幕.缩放密度()` | 缩放密度（`scaledDensity`，`number`） |
| `screen.refreshRate()` | `屏幕.刷新率()` | 刷新率（Hz，`number`） |

### 13.4 device.memory / device.内存

运行内存（RAM）信息，单位均为字节。

| 方法 | 中文别名 | 返回 |
|------|----------|------|
| `memory.info()` | `内存.信息()` | 内存信息聚合对象 |
| `memory.total()` | `内存.总内存()` | 总内存（字节，`number`） |
| `memory.available()` | `内存.可用内存()` | 当前可用内存（字节，`number`） |
| `memory.lowMemory()` | `内存.内存不足()` | 是否处于低内存状态（`boolean`） |
| `memory.threshold()` | `内存.低内存阈值()` | 低内存阈值（字节，`number`） |

### 13.5 device.storage / device.存储

内部/外部存储容量，单位均为字节。外部存储不可用时对应字段返回 `undefined`。

| 方法 | 中文别名 | 返回 |
|------|----------|------|
| `storage.info()` | `存储.信息()` | 存储信息聚合对象 |
| `storage.internalTotal()` | `存储.内部存储总量()` | 内部存储总容量（字节，`number`） |
| `storage.internalAvailable()` | `存储.内部存储可用()` | 内部存储可用容量（字节，`number`） |
| `storage.externalTotal()` | `存储.外部存储总量()` | 外部存储总容量（字节，`number` / `undefined`） |
| `storage.externalAvailable()` | `存储.外部存储可用()` | 外部存储可用容量（字节，`number` / `undefined`） |

### 13.6 device.battery / device.电池

电池电量、充电状态、健康度等信息（通过系统粘性广播实时读取，无需权限）。

| 方法 | 中文别名 | 返回 |
|------|----------|------|
| `battery.info()` | `电池.信息()` | 电池信息聚合对象 |
| `battery.level()` | `电池.电量()` | 电量百分比 `0~100`（`number`，无法获取时为 `-1`） |
| `battery.isCharging()` | `电池.是否充电()` | 是否正在充电（`boolean`） |
| `battery.status()` | `电池.充电状态()` | 状态：`charging` / `discharging` / `full` / `not_charging` / `unknown` |
| `battery.plugged()` | `电池.充电方式()` | 充电方式：`ac` / `usb` / `wireless` / `none` |
| `battery.health()` | `电池.电池健康()` | 健康度：`good` / `overheat` / `dead` / `over_voltage` / `failure` / `cold` / `unknown` |
| `battery.temperature()` | `电池.电池温度()` | 温度（℃，如 `36.5`；未知为 `-1`） |
| `battery.voltage()` | `电池.电池电压()` | 电压（mV，`number`） |
| `battery.technology()` | `电池.电池技术()` | 电池技术（如 `Li-ion`） |

### 13.7 device.network / device.网络

网络连接状态、类型与本地 IPv4 地址。

| 方法 | 中文别名 | 返回 |
|------|----------|------|
| `network.info()` | `网络.信息()` | 网络信息聚合对象 |
| `network.isConnected()` | `网络.是否已连接()` | 是否已连接到网络（`boolean`） |
| `network.isWifi()` | `网络.是否Wifi()` | 当前是否 Wifi（`boolean`） |
| `network.type()` | `网络.网络类型()` | 类型：`wifi` / `cellular` / `ethernet` / `none` / `other` |
| `network.ip()` | `网络.本机IP()` | 本机 IPv4 地址（如 `192.168.1.10`） |

### 13.8 device.app / device.应用

当前应用（Pvz2Tool 自身）的包信息。

| 方法 | 中文别名 | 返回 |
|------|----------|------|
| `app.info()` | `应用.信息()` | 应用信息聚合对象 |
| `app.packageName()` | `应用.包名()` | 应用包名 |
| `app.versionName()` | `应用.版本名()` | 版本名（如 `1.2.3`） |
| `app.versionCode()` | `应用.版本号()` | 版本号（`number`） |
| `app.appName()` | `应用.应用名称()` | 应用显示名称 |
| `app.isDebuggable()` | `应用.是否调试版()` | 是否 debug 构建（`boolean`） |
| `app.targetSdk()` | `应用.目标SDK()` | 目标 SDK 版本（`number`） |

### 13.9 device.isRooted / device.是否已Root

判断设备是否已 Root（检查常见 `su` 路径并回退到 `which su`）。

**返回**：`boolean` — 已 Root 返回 `true`

```js
if (device.isRooted()) {
    console.log("当前设备已 Root");
}
```

---

*文档版本: 2.0*
*最后更新: 2026-07-17*
*新增：device 设备信息对象（system/screen/memory/storage/battery/network/app 分组 + info() 聚合，及中文别名），并补入内置对象总览表*
*新增：audio 音频控制对象（getBgmVolume/setBgmVolume/getSfxVolume/setSfxVolume 及中文别名），并补入内置对象总览表（同时补 http）*
*修正：http.json()/response.解析JSON() 返回已解析的 JS 对象（解析失败返回 null），非 JSON 字符串*
*新增：picker 文件选择器（directory/file/files，返回文件对象，支持多选与 copy 到 SAF 树内新建文件）*
*新增：clipboard 剪切板对象（copy/复制、read/读取/粘贴、clear/清空，基于系统 ClipboardManager）*
*修正：pvz.<type>.all 返回 Array（数据对象数组），单个条目仍可由父对象按 code/name 访问*
*修正：file.copy/file.复制 当 toPath 带扩展名时按目标文件处理并重命名，否则视为目标目录*
*修正：file.list 路径无效时返回空数组 []（非 null）*
*修正：file.resolve() 中性对象 isFile 实际为 true（兼具文件对象特征），并区分读写类/属性类方法的异常行为*
*修正：storage.getAll() 返回数组（非 key-value 对象）*
*修正：this.findById 仅接受单个 id 参数（返回 item 或 section 对象），无双参及 .item/.section 子属性*
*补充：section 对象新增 descriptionValues/描述值；RADIO 项同时支持 checked/选中 别名*
*补充：rton.load 支持直接加载 .json 文件；path.toInternalPath 相对路径自动按 $WORK_DIR 处理*
*新增：picker 文件选择器对象（directory/file/files 及中文别名），支持选择目录/单文件/多文件并返回文件对象（基于 SAF DocumentFile）*
