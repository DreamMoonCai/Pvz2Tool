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
| `file.readText()` | `file.读文本()` | 读取文本 |
| `file.writeBytes()` | `file.写字节()` | 写入字节数组 |
| `file.writeText()` | `file.写文本()` | 写入文本 |
| `file.copy()` | `file.复制()` | 复制文件 |
| `file.delete()` | `file.删除()` | 删除文件 |
| `file.exists()` | `file.存在()` | 检查文件是否存在 |

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
| `rton` | RTON | RTON 文件编解码 | 局部 |
| `rsb` | RSB | RSB 资源包解包/打包 | 局部 |
| `zlib` | ZLIB | ZLib 压缩/解压 | 局部 |
| `ptx` | PTX | PTX 纹理编解码 | 局部 |
| `pvz` | 植物大战僵尸 | 数字加密/存档操作/游戏数据访问 | 全局 |
| `ui` | 界面 | 弹窗、进度条、解压 | 全局 |
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
| `path.app.data` | `path.应用.数据` | 应用私有数据目录 | `/data/data/com.popcap.pvz2restart` |
| `path.app.files` | `path.应用.文件` | 应用文件目录 | `/data/data/com.popcap.pvz2restart/files` |
| `path.app.cache` | `path.应用.缓存` | 应用缓存目录 | `/data/data/com.popcap.pvz2restart/cache` |
| `path.android.data` | `path.安卓.数据` | Android 外部数据目录 | `/storage/emulated/0/Android/data/com.popcap.pvz2restart` |
| `path.android.files` | `path.安卓.文件` | Android 外部文件目录 | `/storage/emulated/0/Android/data/com.popcap.pvz2restart/files` |
| `path.android.cache` | `path.安卓.缓存` | Android 外部缓存目录 | `/storage/emulated/0/Android/data/com.popcap.pvz2restart/cache` |
| `path.pvz.saves` | `path.植物大战僵尸.存档` | 游戏存档目录 | 解析后的绝对路径 |
| `path.pvz.smf` | `path.植物大战僵尸.资源` | 游戏 smf 目录 | 解析后的绝对路径 |
| `path.pvz2tool.files` | `path.工具.文件` | 工具箱工作目录 | 解析后的绝对路径 |
| `path.pvz2tool.smf` | `path.工具.SMF` | 当前选中版本的 SMF 目录 | `<WORK_DIR>/<version.assetPath>` |
| `path.pvz2tool.section` | `path.工具.资源` | 当前功能所在的资源目录（$ITEM） | `<WORK_DIR>/<item.assetPath>` |
| `path.pvz2tool.jsDir` | `path.工具.JS目录` | 执行当前JS所在的 JS 目录 | `<WORK_DIR>/<item.jsPath.dir/section.jsPath.dir>` |

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

加载 RTON 文件为 JS 对象，并注入 `save()` 方法。

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

**返回**：Object - 以代码(code)或名称(name)为键的数据对象

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

获取所有存储的数据。

```javascript
let all = storage.getAll();
// { username: "张三", level: 99, settings: { theme: "dark", sound: true } }
```

---

## 7.6. file - 文件操作

`file` 对象提供通用文件读写功能，支持占位符路径解析。

### 方法

#### file.resolve / file.解析

将占位符路径解析为文件对象，支持链式调用。

**参数**：
- `placeholderPath` (string): 包含占位符的路径

**返回**：文件对象

**文件对象属性**：

| 属性 | 中文别名 | 说明 |
|------|----------|------|
| `path` | `路径` | 解析后的绝对路径 |
| `readBytes()` | `读字节()` | 读取字节数组 |
| `readText()` | `读文本()` | 读取文本 |
| `writeBytes(bytes)` | `写字节(bytes)` | 写入字节数组 |
| `writeText(text)` | `写文本(text)` | 写入文本 |
| `copyTo(toPath)` | `复制到(toPath)` | 复制到目标路径 |
| `delete()` | `删除()` | 删除文件 |
| `exists()` | `存在()` | 检查文件是否存在 |

**示例**：
```javascript
// 链式调用
let bytes = file.resolve("$SMF/packages/icon.bin").readBytes();
file.resolve("$SMF/packages/output.bin").writeBytes(bytes);

// 创建文件对象后多次操作
let fileObj = file.resolve("$SMF/data.bin");
let bytes1 = fileObj.readBytes();
fileObj.writeBytes(modifiedBytes);
```

#### file.readBytes / file.读字节

读取文件为字节数组（简写方式，同步操作）。

**参数**：
- `placeholderPath` (string): 包含占位符的文件路径

**返回**：Uint8Array

**示例**：
```javascript
let bytes = file.readBytes("$ITEM/config.bin");
console.log("文件大小:", bytes.length);
```

#### file.readText / file.读文本

读取文件为文本（简写方式，UTF-8，同步操作）。

**参数**：
- `placeholderPath` (string): 包含占位符的文件路径

**返回**：string

**示例**：
```javascript
let text = file.readText("$ITEM/config.txt");
console.log("配置:", text);
```

#### file.writeBytes / file.写字节

写入字节数组到文件（简写方式，同步操作）。

**参数**：
- `placeholderPath` (string): 目标文件路径
- `bytes` (Uint8Array): 要写入的字节数组

**示例**：
```javascript
file.writeBytes("$ITEM/output.bin", bytes);
```

#### file.writeText / file.写文本

写入文本到文件（简写方式，UTF-8，同步操作）。

**参数**：
- `placeholderPath` (string): 目标文件路径
- `text` (string): 要写入的文本

**示例**：
```javascript
file.writeText("$ITEM/config.txt", "hello world");
```

#### file.copy / file.复制

复制文件（同步操作）。

**参数**：
- `fromPath` (string): 源文件路径
- `toPath` (string): 目标文件路径

**示例**：
```javascript
file.copy("$SMF/source.bin","$SMF/target.bin");
```

#### file.delete / file.删除

删除文件（同步操作）。

**参数**：
- `placeholderPath` (string): 要删除的文件路径

**返回**：boolean - 是否删除成功

**示例**：
```javascript
let deleted = file.delete("$ITEM/temp.bin");
console.log("删除结果:", deleted);
```

#### file.exists / file.存在

检查文件是否存在（同步操作）。

**参数**：
- `placeholderPath` (string): 要检查的文件路径

**返回**：boolean

**示例**：
```javascript
let exists = file.exists("$ITEM/config.txt");
if (exists) {
    console.log("文件存在");
}
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

## 9. assets - 资源访问

`assets` 对象提供对工具箱内置资源的访问功能，支持本地覆盖优先和 URL 资源。

此API仅支持 pvz2tool 目录(手动狗头)

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
| RADIO | `item.selected` | `选中` | 是否选中（可读写） |
| CHECKBOX | `item.checked` | `勾选` | 选中状态（可读写） |
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
```

### this.findById / this.查找

快速查找栏目或栏目项。

```javascript
// 英文写法
var item = this.findById("auto_collect_js");

// 中文写法（等效）
var item = this.查找("auto_collect_js");

// 双参数精确查找
var item = this.findById("栏目ID", "栏目项ID");
var item = this.查找("栏目ID", "栏目项ID");

// 使用子属性查找
var item = this.findById.item("栏目项ID");
var section = this.findById.section("栏目ID");
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
| `$JS_DIR` | 栏目下的 item jsPath.dir，没有则逐级递升 | `$ITEM/config.json` |

### 占位符解析规则

- `$SMF` - 基于当前选中版本的 `assetPath` 解析；**降级规则**：版本目录不存在时回退到 `baseAssetPath`
- `$ITEM` - 基于栏目下 item 的 `assetPath` 解析（默认 `version/版本ID/栏目ID/功能项ID`）；**降级规则**：item assetPath 不存在时降级到 `$SMF`
- `$JS_DIR` - 基于栏目下 item 的 `jsPath.dir` 解析（默认 `version/版本ID/栏目ID/功能项ID`）；**降级规则**：item jsPath.dir 不存在时自动向上查询
- `$GAME_SAVES` - 从配置 `sections[id="saves"].targetPath` 解析
- `$GAME_SMF` - `rootDirectory + config.smfDirectory`
- `$WORK_DIR` - 用户选择的 SAF 工作目录

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

*文档版本: 1.7*
*最后更新: 2026-05-05*
*新增：pvz 数据访问 API（植物、僵尸、强化道具等16类游戏数据）*
*新增：pvz.plants/all、pvz.zombies/all 等数据访问接口*
*新增：数据属性中英文别名支持（id/编号、name/昵称、code/代号等）*
*新增：$SMF 和 path.pvz2tool.smf 指向版本目录*
*新增：$ITEM 和 path.pvz2tool.section 指向所在资源目录*
*新增：$JS_DIR 和 path.pvz2tool.jsDir 指向所在JS目录*
*新增：版本隔离的状态管理说明*
*补充：path 对象完整路径说明*
*补充：config_documentation.md UI 配置完整字段*
