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
| `ui.progress()` | `ui.进度()` | 进度弹窗（支持 onCancel 取消回调） |
| `ui.progress().update()` | `ui.进度.更新()` | 更新进度 |
| `ui.progress().close()` | `ui.进度.关闭()` | 关闭进度 |
| `ui.progress().cancel()` | `ui.进度.取消()` | 主动取消（触发 onCancel） |
| `ui.progress().isCancelled()` | `ui.进度.是否已取消()` | 是否已取消 |
| `ui.extract()` | `ui.解压()` | 解压根资源 |
| `ui.select()` | `ui.选择()` | 单项选择弹窗（图标网格 / 列表 / 纯文字单选） |
| `ui.multiSelect()` | `ui.多选()` | 多项选择弹窗（返回选中值数组） |
| `ui.showGameDisplay()` | `ui.弹出画面设置()` / `ui.画面设置()` | 弹出游戏画面设置全屏浮窗（同悬浮球"画面设置"） |
| `ui.isCustomGameDisplayEnabled()` | `ui.是否启用自定义画面()` / `ui.画面设置是否可用()` | 「自定义游戏画面」开关是否已开启（可用作 `isShowFromJs` 条件） |
| `ui.refreshAll()` | `ui.刷新所有()` / `ui.刷新复合文本()` | 主动刷新所有复合文本与动态显隐（见下方详解） |

#### vpn 对象
| 英文 | 中文别名 | 说明 |
|------|----------|------|
| `vpn.disconnect()` | `vpn.断网()` / `vpn.断开网络()` | 断开网络（开启 VPN 拦截） |
| `vpn.restore()` | `vpn.恢复()` / `vpn.恢复网络()` | 恢复网络（关闭 VPN） |
| `vpn.isActive()` | `vpn.是否激活()` / `vpn.是否开启()` | 当前 VPN 是否处于激活状态（即是否断网） |
| `vpn.isPrepared()` | `vpn.是否已授权()` / `vpn.已授权()` / `vpn.是否可用()` | VPN 是否已获系统授权（可用作 `isShowFromJs` 条件） |

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
| `browser` | 浏览器 | 调用系统浏览器打开链接 | 全局 |
| `thread` | 协程 | 异步/协程执行（run/all/launch/sleep/race/timeout/retry/map/interval/setTimeout，返回 Promise，支持并发与后台任务）；并支持协程上下文（context/withContext/local）定义作用域、调度器与局部状态 | 全局 |
| `toast` | 吐司 | 系统轻提示（show/显示/提示/吐司，及 short/短、long/长 便捷方法；支持 short/long 时长与数字 0/1），切主线程显示 | 全局 |
| `app` | 应用 | 应用进程控制（restart/重启/重启应用、restartGame/重启游戏、exit/退出/退出应用/退出APP）；冷重启、退出进程、重启后自动进入游戏 | 全局 |
| `dex` | dex加载 | DEX 加载（load/加载、loadFromAsset/从资源加载、loadFromUrl/从网络加载），把外部 .dex/.apk/.jar 加载到独立 DexClassLoader 并返回句柄 | 全局 |
| `reflect` | 反射 | 反射（findClass/查找类 及 YukiReflection 风格的类/方法/字段/构造器/实例链式操作，活对象以 `Wrapper<T>` 子类句柄（携带原始对象）形式往返 JS） | 全局 |
| `device` | 设备 | 当前安卓设备信息（系统 / 屏幕 / 内存 / 存储 / 电池 / 网络 / 应用 / CPU / Root） | 全局 |
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

将占位符路径解析为绝对路径字符串。**占位符可作路径前缀拼接子路径，子路径不必已存在**（纯字符串展开，不要求目标文件/目录实际存在）。

**参数**：
- `placeholderPath` (string): 包含占位符的路径，支持在占位符后拼接子路径

**返回**：绝对路径字符串；无法解析（如缺少上下文、根目录不可用）时返回 null/undefined

**示例**：
```javascript
var savePath = path.resolve("$GAME_SAVES/SeedChooserUserData.rton");
// 中文写法
var savePath = path.解析路径("$GAME_SAVES/SeedChooserUserData.rton");

// 占位符拼接子路径（子路径不必已存在，常用于构造输出目标）
var testPath = path.解析路径(path.android.files + "/test");
// → /storage/emulated/0/Android/data/<pkg>/files/test
var smfSub = path.解析路径(path.pvz2tool.smf + "/abc/def.txt");
// → workDir/<version.assetPath>/abc/def.txt
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

### 通用选项与回调

下列选项为多数弹窗（alert / confirm / prompt / select / multiSelect / actionSheet / slider / loading）通用，仅在各自小节列出差异项。

**按钮文字与颜色**
- `confirmText` (string): 确认/主按钮文字。默认值因弹窗而异：alert / prompt / slider 为 `"确定"`，confirm 为 `"确认"`。
- `cancelText` (string): 取消/次按钮文字，默认 `"取消"`。
- `confirmColor` / `cancelColor` (string): 对应按钮背景色。留空或非法值则使用主题默认渐变。支持：
  - 命名色：`black` `white` `red` `green` `blue` `yellow` `orange` `purple` `gray`(=`grey`) `gold` `cyan` `pink`
  - 十六进制：`#RGB` / `#RRGGBB` / `#AARRGGBB`（例如 `"#FF5722"`、`"#80FFFFFF"`）

**可关闭性**
- `dismissible` / `可关闭` (boolean, 默认 `false`): 是否允许点击弹窗外部关闭。
  - 对 confirm / prompt / slider：点外部等价于「取消」，触发 `onCancel` 并以取消值返回。
  - 对 alert / loading：点外部即关闭（alert 触发 `onConfirm`，loading 触发 `onDismiss`）。
- 注：`select` / `multiSelect` / `actionSheet` 使用 `cancelable` 控制同一行为（是否显示底部「取消」并允许点外部关闭）。

**性能开关（适用于内容可能很多的弹窗：select / multiSelect / actionSheet）**
- `forceMaxForm` / `最高形态` (boolean, 默认 `false`): 开启后弹窗**必定以最高形态展示**——内容区直接固定为上限高度（默认 250dp），**跳过 `maxIntrinsicHeight` 探测重测**，在键盘升降 / 窗口 resize 等可用高度连续小幅变化时不触发任何探测计算。适合选项很多、内容必然超出可视区的场景；屏幕不足时仍自动挤压贴合，不会溢出。普通内容少、需要自适应高度的场景保持默认 `false` 即可。

**事件回调**
所有回调均为 `function(value) { ... }` 形式，在对应交互发生时异步触发；回调返回值被忽略，不会阻塞 `await()`。

| 回调 | 触发时机 | 收到的 value |
|------|----------|--------------|
| `onConfirm(value)` | 点击确认 | alert 无参数；confirm 收到 `true`；prompt 收到输入字符串；slider 收到确认的数值 |
| `onCancel()` | 点击取消或点外部关闭 | 无参数 |
| `onSelect(value)` | 选中某项 | select 收到选中项 `value`；multiSelect 收到选中 `value` 数组；actionSheet 收到选中项 `value` |
| `onChange(value)` | slider 拖动过程中 | 当前数值（实时） |
| `onDismiss()` | loading 被关闭 | 无参数 |

> 示例：`ui.confirm("标题", "内容", { confirmText: "好的", onConfirm: function(){ console.log("已确认") } })`

### 方法

#### ui.alert / ui.提示

显示一个带单按钮的提示对话框。

**语法**：`ui.alert(title, message, options?) -> void`

**参数**：
- `title` (string): 弹窗标题
- `message` (string): 弹窗内容
- `options` (object, 可选): 见下方

**options 参数**：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `confirmText` | string | "确定" | 按钮文字 |
| `confirmColor` | string | "" | 按钮背景色（命名色或 `#RRGGBB`，见通用说明） |
| `dismissible` / `可关闭` | boolean | false | 是否允许点击外部关闭（关闭时触发 `onConfirm`） |
| `onConfirm` | function | 无 | 点击按钮时触发（无参数） |

**返回**：void - 用户点击确定后 resolve

**示例**：
```javascript
// 自定义按钮文字与颜色，并可点击外部关闭
ui.alert("兑换成功", "恭喜获得豪华礼包！", {
    confirmText: "太棒了",
    confirmColor: "gold",
    dismissible: true,
    onConfirm: function () { console.log("用户已查看") }
});
```

#### ui.confirm / ui.确认

显示确认对话框。

**语法**：`ui.confirm(title, message, options?) -> boolean`

**参数**：
- `title` (string): 标题
- `message` (string): 消息内容
- `options` (object, 可选): 见下方

**options 参数**：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `confirmText` | string | "确认" | 确认按钮文字 |
| `cancelText` | string | "取消" | 取消按钮文字 |
| `confirmColor` | string | "" | 确认按钮背景色 |
| `cancelColor` | string | "" | 取消按钮背景色 |
| `dismissible` / `可关闭` | boolean | false | 点击外部关闭时等价于取消（触发 `onCancel`） |
| `onConfirm` | function | 无 | 点击确认时触发（收到 `true`） |
| `onCancel` | function | 无 | 点击取消 / 点外部时触发（无参数） |

**返回**：boolean - 用户点击确定返回 true，取消返回 false

**示例**：
```javascript
var confirmed = ui.confirm("确认删除", "确定要删除这个文件吗？", {
    confirmText: "删吧",
    confirmColor: "red",
    cancelText: "再想想",
    onConfirm: function () { console.log("已确认删除") }
});
if (confirmed) {
    // 执行删除
}
```

#### ui.prompt / ui.输入

显示输入对话框。

**语法**：`ui.prompt(title, message, defaultValue?, placeholder?, options?) -> string|null`

**参数**：
- `title` (string): 标题
- `message` (string): 提示消息
- `defaultValue` (string, 可选): 输入框默认值
- `placeholder` (string, 可选): 输入框占位提示文字（空输入时显示，留空默认显示「请输入...」）
- `options` (object, 可选): 见下方

**options 参数**：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `confirmText` | string | "确定" | 确认按钮文字 |
| `cancelText` | string | "取消" | 取消按钮文字 |
| `confirmColor` | string | "" | 确认按钮背景色 |
| `cancelColor` | string | "" | 取消按钮背景色 |
| `dismissible` / `可关闭` | boolean | false | 点击外部关闭时等价于取消（触发 `onCancel`） |
| `onConfirm` | function | 无 | 点击确认时触发（收到输入字符串） |
| `onCancel` | function | 无 | 点击取消 / 点外部时触发（无参数） |

**返回**：string|null - 用户输入的字符串，取消返回 null

**示例**：
```javascript
var name = ui.prompt("输入名称", "请输入文件名", "默认名称", "例如：level_1", {
    confirmText: "保存",
    confirmColor: "green",
    onConfirm: function (v) { console.log("提交：" + v) }
});
if (name !== null) {
    console.log("输入: " + name);
}
```

#### ui.progress / ui.进度

显示**全屏**进度弹窗（与 `ui.loading` 同款半透明阴影遮罩，覆盖刘海/小白条，内容在安全区内布局）：**进度条永远固定在底部**；上半区垂直水平居中——indeterminate 模式时 loading 动图居中、文字在图之下（与普通 `ui.loading` 一致），无加载器时文字直接居中。

**参数**：
- `title` (string): 标题
- `options` (object, 可选): 选项

**options 参数**：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `message` | string | "" | 初始消息 |
| `indeterminate` | boolean | false | 是否不确定模式 |
| `showCancel` | boolean | true | 是否显示取消按钮 |
| `onCancel` | function | 无 | 用户点击“取消”时触发的回调（用于中断耗时任务） |

**返回**：ProgressController - 进度控制器对象

**ProgressController 方法**：

| 方法 | 中文别名 | 说明 |
|------|----------|------|
| `update(message?, progress?)` | `更新(message?, progress?)` | 更新进度 (progress: 0.0-1.0) |
| `close()` | `关闭()` | 正常完成，关闭进度对话框 |
| `cancel()` | `取消()` | 主动取消（等效点击“取消”按钮）：隐藏对话框并触发 `onCancel` 回调 |
| `isCancelled()` | `是否已取消()` | 返回 `boolean`，是否已取消（可在循环中轮询以中断任务） |

> **取消机制说明**：点击进度条上的“取消”按钮（或调用 `controller.cancel()`）会隐藏对话框并触发 `options.onCancel` 回调；JS 应在 `onCancel` 中设置中断标志，并在循环里通过 `controller.isCancelled()` 及时退出耗时逻辑。取消后 `isCancelled()` 保持 `true`，直到下一次 `ui.progress()` 重新打开。

**示例**：
```javascript
var cancelled = false;
var progress = ui.progress("正在处理...", {
    showCancel: true,
    onCancel: function () {
        // 用户点击取消时设置标志（也可在循环里检查 isCancelled()）
        cancelled = true;
        console.log("用户取消了操作");
    }
});

try {
    for (var i = 0; i < 100; i++) {
        if (progress.isCancelled()) break;   // 或检查 cancelled
        // ... 耗时工作 ...
        progress.update("处理中 " + i + "%", i / 100);
    }
} finally {
    progress.close();
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

#### ui.select / ui.选择

单项选择弹窗：从一批条目中任选其一。

**参数**：
- `title` (string): 标题
- `items` (Array): 条目数组，元素可为：
  - 字符串 → 直接作为名称（值为该字符串）
  - 对象 → `{ name/名称, icon/图标?, value/值?, showIndex/显示序号?, showIndexColor/序号颜色? }`，`value` 缺省回退 `name`；`showIndex` 为单项单独控制是否显示序号（优先级高于外层 options.showIndex），`showIndexColor` 为单项单独控制序号颜色（"black"/"white"，优先级高于外层 options.showIndexColor）
- `options` (object, 可选): 见下方表格

**options 参数**：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `columns` | number | 4 | 网格模式每排列数（2~6，仅条目 **≥ 8** 且有图标时生效） |
| `cancelable` | boolean | false | 是否允许点击外部取消（关闭时触发 `onCancel`） |
| `showIndex` | boolean | false | 全局默认是否叠加序号（从 1 开始），仅对有图标的项生效 |
| `showIndexColor` | string | "black" | 序号颜色，支持命名色或 `#RRGGBB`（见通用说明）；单项可用 `showIndexColor`/`序号颜色` 覆盖 |
| `confirmText` | string | "确定" | 「确定」按钮文字（仅多选模式显示） |
| `cancelText` | string | "取消" | 「取消」按钮文字（单/多选底部均显示） |
| `confirmColor` | string | "" | 确定按钮背景色（仅多选） |
| `cancelColor` | string | "" | 取消按钮背景色 |
| `onSelect` | function | 无 | 选中某项时触发（单选收到 `value`；多选收到 `value` 数组） |
| `onCancel` | function | 无 | 点击取消 / 点外部时触发（无参数） |

**返回**：string|null - 选中条目的 `value`，取消返回 null

**布局规则（参考 SectionType.RADIO 样式）**：
- 任意条目带图标且总数 **≥ 8** → 网格模式，每排若干，单条目 = 图标(或占位矩形) + 底部文字
- 任意条目带图标且总数 **< 8** → 列表模式，每条目独占一行，图标(或占位矩形)在文字前
- 所有条目**均无图标** → 纯文字模式，每条目前带单选标记（圆点），独占一行
- 无图标条目用与图标**同尺寸**的矩形占位，内部居中显示与底部相同的文字，超出自动截断

**示例**：
```javascript
// 带图标（列表模式，< 8 条）：图标在文字前，无图标项显示占位矩形
var items = [
    { name: "存档1", icon: "save_icon.png", value: "s1" },
    { name: "存档2", icon: "save_icon.png", value: "s2" },
    { name: "存档3", value: "s3" }   // 无图标 → 占位矩形
];
var v = ui.select("选择存档", items, {
    cancelText: "返回",
    cancelColor: "gray",
    onSelect: function (val) { console.log("选中：" + val) }
});
console.log("选择了：" + v);

// 生成 ≥ 8 条触发网格模式，并在每项图标上叠加金色序号
var lv = [];
for (var i = 1; i <= 20; i++) lv.push({ name: "关卡" + i, icon: "lv.png", value: String(i) });
var pick = ui.select("选择关卡", lv, { showIndex: true, showIndexColor: "gold" });

// 纯文字单选（无图标 → 每条目前带单选圆点）
var t = ui.select("难度", ["简单", "普通", "困难"]);
```

#### ui.multiSelect / ui.多选

多项选择弹窗：从一批条目中选择任意多个。

**参数**：
- `title` (string): 标题
- `items` (Array): 同 `ui.select`（字符串或 `{ name, icon?, value?, showIndex?, showIndexColor? }`），单项 `showIndex`/`showIndexColor` 可单独覆盖外层设置
- `options` (object, 可选): 见下方表格

**options 参数**：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `defaultValues` | Array\<string\> | [] | 默认选中项的 name 或 value 列表 |
| `columns` | number | 4 | 网格模式每排列数（2~6，仅条目 **≥ 8** 且有图标时生效） |
| `cancelable` | boolean | false | 是否允许点击外部取消（关闭时触发 `onCancel`） |
| `showIndex` | boolean | false | 全局默认是否叠加序号（从 1 开始），仅对有图标的项生效 |
| `showIndexColor` | string | "black" | 序号颜色，支持命名色或 `#RRGGBB`（见通用说明）；单项可用 `showIndexColor`/`序号颜色` 覆盖 |
| `confirmText` | string | "确定" | 「确定」按钮文字 |
| `cancelText` | string | "取消" | 「取消」按钮文字 |
| `confirmColor` | string | "" | 确定按钮背景色 |
| `cancelColor` | string | "" | 取消按钮背景色 |
| `onSelect` | function | 无 | 选中集合变化时触发（收到当前选中 `value` 数组） |
| `onCancel` | function | 无 | 点击取消 / 点外部时触发（无参数） |

**返回**：string[] - 选中条目的 `value` 数组（未选返回空数组）

**布局**：与 `ui.select` 一致；纯文字模式每条目前带勾选标记，有图标模式选中后在图标右上角显示勾标。需点击「确定」确认，或「取消」清空。

**示例**：
```javascript
var items = ["苹果", "香蕉", "橙子"];
var arr = ui.multiSelect("选择水果", items, {
    defaultValues: ["苹果"],
    confirmText: "就这些",
    confirmColor: "green",
    onSelect: function (vals) { console.log("当前选择：" + vals.join(",")) }
});
console.log("已选：" + arr.join(", "));
```

#### ui.actionSheet / ui.操作菜单

**语法**：`ui.actionSheet(title, actions, options?) -> string|null`

弹出底部动作列表，点击某项**立即返回**其 `value`（或 `name`），适合「执行哪个操作」的场景，比 `ui.select` 更具操作感。

- `title` (string): 标题
- `actions` (Array): 动作数组，元素可为：
  - 字符串 → 直接作为名称（值为该字符串）
  - 对象 → `{ name/名称, value/值?, danger/危险? }`，`value` 缺省回退 `name`；`danger=true` 时该按钮显示为红色警示样式
- `options` (object, 可选): 见下方表格

**options 参数**：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `cancelable` | boolean | true | 是否显示底部「取消」按钮且允许点击外部关闭 |
| `cancelText` | string | "取消" | 「取消」按钮文字 |
| `cancelColor` | string | "" | 「取消」按钮背景色 |
| `onSelect` | function | 无 | 点击某项时触发（收到选中项的 `value`） |
| `onCancel` | function | 无 | 点击取消 / 点外部时触发（无参数） |

**返回**：string|null - 选中项的 `value`（或 `name`）；取消/点外部返回 `null`。

**示例**：
```javascript
// 纯字符串动作
var v = ui.actionSheet("选择操作", ["复制", "重命名", "删除"], {
    onSelect: function (act) { console.log("执行：" + act) }
});
console.log("操作：" + v);

// 带 danger 的危险操作（红色按钮）、无取消按钮、自定义取消文字
var d = ui.actionSheet("确认删除？", [
  { name: "取消", value: "cancel" },
  { name: "彻底删除", value: "del", danger: true }
], { cancelable: false, cancelText: "算了", cancelColor: "gray" });
```

#### ui.slider / ui.滑块

**语法**：`ui.slider(title, options?) -> number`

弹出数值滑块，拖动后点击「确定」返回数值。弥补当前 JS 缺少数值输入能力的问题。滑块样式参考配置栏目的 `SectionType.SLIDER`（胶囊轨道 + 主题色渐变 + 齿轮滑块随进度旋转）。

- `title` (string): 标题
- `options` (object, 可选): 见下方表格

**options 参数**：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `min` | number | 0 | 最小值 |
| `max` | number | 100 | 最大值 |
| `step` | number | 1 | 步长（须 > 0） |
| `default` | number | `min` | 初始值（自动约束到 [min, max]） |
| `unit` | string | "" | 单位后缀，如 "%"、"px"，仅用于显示 |
| `decimals` | number | 2 | 当前数值显示的小数位数 |
| `showValue` | boolean | true | 是否在滑块上方以大字体显示当前数值 |
| `confirmText` | string | "确定" | 「确定」按钮文字 |
| `cancelText` | string | "取消" | 「取消」按钮文字 |
| `confirmColor` | string | "" | 确定按钮背景色 |
| `cancelColor` | string | "" | 取消按钮背景色 |
| `dismissible` / `可关闭` | boolean | false | 是否允许点击外部关闭（等价于取消，触发 `onCancel`） |
| `onChange` | function | 无 | 拖动过程中实时触发（收到当前数值） |
| `onConfirm` | function | 无 | 点击确定时触发（收到确认的数值） |
| `onCancel` | function | 无 | 点击取消 / 点外部时触发（无参数） |

**返回**：number|null - 确认后的数值；点击「取消」返回 null。

**示例**：
```javascript
// 基础用法
var speed = ui.slider("移动速度", { min: 0, max: 200, step: 5, default: 60, unit: "%" });
console.log("速度：" + speed);

// 实时回调 + 自定义按钮与小数位
var vol = ui.slider("音量", {
    min: 0, max: 1, step: 0.05, default: 0.5, decimals: 2,
    confirmText: "设定", confirmColor: "green",
    onChange: function (v) { console.log("拖动中：" + v) },
    onConfirm: function (v) { console.log("最终：" + v) }
});
```

#### ui.loading / ui.加载

**语法**：`ui.loading(title, options?) -> controller{ close(), 关闭() }`

全屏「请稍候」加载遮罩：半透明阴影背景覆盖整个屏幕（含刘海/cutout 与底部手势条区域），中央显示 loading 动图（约 184dp），**标题与说明文字显示在图片下方**，且整体在安全区(safe area)内居中，自动避让刘海与底部小白条。与 `ui.progress`（任务进度/可取消）语义不同——它只表示等待，**不阻塞 `await`**，需手动调用返回的 controller 关闭。

- `title` (string): 标题
- `options` (object, 可选): 见下方表格

**options 参数**：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `message` | string | "" | 说明文字 |
| `dismissible` / `可关闭` | boolean | false | 是否允许点击外部关闭并显示「取消」按钮（关闭时触发 `onDismiss`） |
| `cancelText` | string | "取消" | 「取消」按钮文字（仅 `dismissible` 时显示） |
| `cancelColor` | string | "" | 「取消」按钮背景色 |
| `onDismiss` | function | 无 | 被关闭（点取消 / 点外部）时触发（无参数） |

**返回**：controller 对象，含以下方法：

| 方法 | 中文别名 | 说明 |
|------|----------|------|
| `close()` | `关闭()` | 关闭遮罩 |
| `update(message)` | `更新(message)` | 实时更新说明文字（不关闭遮罩） |

> 与 `ui.progress` 不同，`ui.loading` **不阻塞 `await`**，需手动调用 `close()` 关闭；适合「等待某段异步逻辑完成」的场景。

**示例**：
```javascript
// 基础：显示后手动关闭
var ctrl = ui.loading("处理中", { message: "正在解包资源，请稍候..." });
// ... 执行耗时任务 ...
ctrl.close(); // 完成后关闭

// 实时更新文字 + 可点击外部关闭（触发 onDismiss）
var ctrl2 = ui.loading("同步中", {
    message: "准备...",
    dismissible: true,
    cancelText: "停止",
    onDismiss: function () { console.log("用户中止") }
});
// 任务进行中可随时刷新提示
ctrl2.update("已下载 50%");
// ... 继续 ...
ctrl2.close();
```

#### ui.showGameDisplay / ui.弹出画面设置 / ui.画面设置

弹出游戏画面设置全屏浮窗（与悬浮球"画面设置"按钮同款）。

**语法**：`ui.showGameDisplay() -> void`

**参数**：无

**说明**：
- 通过 `ContextUtil.getCurrentActivity()` 获取当前前台 Activity 来承载浮窗；若拿不到，则回退到全局 `InitializePvz2.context`（须为 Activity 才能弹窗）。
- 弹窗内容与悬浮球面板的"画面设置"完全一致：分辨率、比例、全屏、自定义窗口宽高等游戏画面选项。
- 任何异常均静默吞掉（runCatching），不影响脚本后续执行。

**返回**：void

**示例**：
```javascript
// 直接弹出游戏画面设置浮窗
ui.showGameDisplay();
ui.弹出画面设置();
```

#### ui.isCustomGameDisplayEnabled / ui.是否启用自定义画面 / ui.画面设置是否可用

查询设置中「自定义游戏画面」开关是否已开启。

**语法**：`ui.isCustomGameDisplayEnabled() -> boolean`

**参数**：无

**说明**：
- 直接读取 `SettingsDialogState.isUseCustomGameDisplay`（用户在设置弹窗里切换的持久化开关）。
- 开关关闭时 `ui.showGameDisplay()` 不会有任何反应（控制器内部会直接 return），因此建议先用本方法判断。
- 典型用法是作为悬浮窗按钮的 `isShowFromJs`，未开启时自动隐藏「画面设置」按钮，见 `config_documentation.md` 的 floatingWindow 章节。
- 任何异常均返回 `false`。

**返回**：boolean - 是否已开启自定义游戏画面

**示例**：
```javascript
if (ui.isCustomGameDisplayEnabled()) {
    ui.showGameDisplay();
} else {
    toast.show("请先在设置中开启「自定义游戏画面」");
}
```

```yaml
# 用作悬浮窗按钮的可见性条件
- id: game_display
  name: "画面设置"
  isShowFromJs: "ui.isCustomGameDisplayEnabled()"
  jsScript: "ui.showGameDisplay();"
```

#### ui.refreshAll / ui.刷新所有 / ui.刷新复合文本

手动广播一次「复合文本重算」信号，让界面上所有 `{{js:...}}` 文本与 `isShowFromJs` 动态显隐立即重新求值。

**语法**：`ui.refreshAll() -> void`

**参数**：无

**说明**：
- 绝大多数交互由系统自动触发刷新（BUTTON 点击 / CHECKBOX / SLIDER / 确认 / 进入游戏脚本执行完毕后都会自动广播一次），一般**无需**手动调用。
- 当脚本通过**异步路径**改变了运行时状态、而该路径不归上述自动刷新覆盖时，才需要手动调用。典型场景：
  - `ui.confirm` / `ui.prompt` 的 `onConfirm` 回调里修改了状态；
  - `thread` / `timer` 定时器、`http` 网络回调中修改了状态；
  - 自行维护的全局变量被某段逻辑改掉后希望界面立刻同步。
- 内部仅把重算版本号 +1，订阅侧（复合文本与动态显隐）会以「合并防抖」方式统一刷新，因此**不会**出现「刷新 → 重算 → 再刷新」的死循环（前提是文本表达式本身不要直接调用 `ui.refreshAll()`）。
- 任何线程都可调用，线程安全。

**返回**：无（void）

**示例**：
```javascript
// 在确认弹窗回调里改了状态，自动刷新覆盖不到，手动补一次
ui.confirm("切换主题", "确定切换到暗色主题？", {
    onConfirm: () => {
        myTheme = "dark";        // 自定义全局状态
        ui.refreshAll();         // 手动让 {{js:myTheme}} 等文本立即重算
    }
});
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

列出 assets 目录下的所有文件（仅 APK 内置资源，不含本地工作目录覆盖）。若需要「本地优先」的列举，请使用 `assets.list()`。

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

获取资源详细信息（**工作目录优先**：本地覆盖 > APK 内置资源，绝对路径直接查本地文件系统）。

**参数**：
- `path` (string): 资源相对路径（或 `/` 开头的绝对路径）

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

基于 Android 系统 API（`Build` / `WindowManager` / `ActivityManager` / `BatteryManager` / `ConnectivityManager` / `StatFs` 等），提供当前安卓设备的各种信息。支持按分组（`system` / `screen` / `memory` / `storage` / `battery` / `network` / `app` / `cpu`）获取，也支持 `device.info()` 一次性聚合全部信息。

> **说明**：所有信息均为实时读取（每次调用刷新），例如电池电量、网络状态会反映当前真实值，而非注册时的快照。

### 13.1 device.info / device.信息

一次性返回全部设备信息（聚合对象，各分组作为子对象存在）。

**返回**：`object` — 结构为 `{ system, screen, memory, storage, battery, network, app, cpu, isRooted }`

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

### 13.9 device.cpu / device.CPU

CPU 信息。核心数来自 `Runtime.getRuntime().availableProcessors()`；架构 / 支持的 ABI 来自 `Build.SUPPORTED_ABIS`；频率与调度器读取 `cpu0` 的 cpufreq sysfs 节点（`/sys/devices/system/cpu/cpu0/cpufreq/...`）。频率单位为 **kHz**（另提供 `*Mhz` 便捷字段，单位 MHz）。部分设备可能限制 sysfs 读取，频率/调度器读取失败时返回 `-1`（数值）或 `""`（字符串）。

| 方法 | 中文别名 | 说明 |
|------|---------|------|
| `cpu.info()` | `CPU.信息()` | CPU 信息聚合对象 |
| `cpu.cores()` | `CPU.核心数()` | CPU 核心数（`number`，如 `8`） |
| `cpu.arch()` | `CPU.架构()` | 主 ABI（如 `arm64-v8a`） |
| `cpu.supportedAbis()` | `CPU.支持的ABI()` | 全部支持的 ABI（`Array<string>`，如 `["arm64-v8a","armeabi-v7a"]`） |
| `cpu.maxFreq()` | `CPU.最高频率()` | 最高频率（kHz，`number`） |
| `cpu.maxFreqMhz()` | `CPU.最高频率MHz()` | 最高频率（MHz，`number`） |
| `cpu.minFreq()` | `CPU.最低频率()` | 最低频率（kHz，`number`） |
| `cpu.minFreqMhz()` | `CPU.最低频率MHz()` | 最低频率（MHz，`number`） |
| `cpu.currentFreq()` | `CPU.当前频率()` | 当前频率（kHz，`number`，实时） |
| `cpu.currentFreqMhz()` | `CPU.当前频率MHz()` | 当前频率（MHz，`number`，实时） |
| `cpu.governor()` | `CPU.调度器()` | 当前调度器（如 `schedutil` / `performance`） |

**示例**
```js
console.log("核心数:", device.cpu.cores());
console.log("架构:", device.cpu.arch());
console.log("支持ABI:", device.cpu.supportedAbis());    // ["arm64-v8a","armeabi-v7a"]
console.log("最高频率(MHz):", device.cpu.maxFreqMhz());
console.log("当前频率(MHz):", device.cpu.currentFreqMhz());
console.log("调度器:", device.cpu.governor());
```

### 13.10 device.isRooted / device.是否已Root

判断设备是否已 Root（检查常见 `su` 路径并回退到 `which su`）。

**返回**：`boolean` — 已 Root 返回 `true`

```js
if (device.isRooted()) {
    console.log("当前设备已 Root");
}
```

---

## 14. browser - 在浏览器中打开

调用系统浏览器（或具备 `ACTION_VIEW` 处理能力的其它应用）打开指定链接。内部使用 `Intent.ACTION_VIEW` + `FLAG_ACTIVITY_NEW_TASK` 启动，与复合文本普通链接的跳转行为一致。

> **说明**：若设备上没有可处理该协议的应用（例如孤立的 `tel:` 在无电话能力的设备），启动会静默失败——不抛异常、不影响后续脚本执行。未携带协议（不含 `://`）的地址会自动补全 `https://`。

### 14.1 browser.open / browser.打开 / browser.打开链接 / browser.openLink

在系统浏览器中打开目标链接。

**参数**：`url` (string) — 目标地址；支持完整协议（`http` / `https` / `ftp` / `mailto` / `tel` / `file` 等），未带协议时自动补全 `https://`；为空 / 空白时不执行任何操作

**返回**：`undefined`

```javascript
// 用系统浏览器打开网页
browser.open("https://github.com");
browser.打开("https://www.bing.com");

// 未带协议时自动补全 https://
browser.open("github.com");

// 别名
browser.打开链接("https://example.com");
browser.openLink("https://example.com");

// 配合其它 API：打开接口返回的跳转地址
let resp = http.get("https://api.example.com/redirect");
if (resp && resp.url) browser.open(resp.url);
```

---

## 15. thread - 异步 / 协程

面向 JS 脚本的异步与协程原语。基于 keight 引擎能力（`ScriptRuntime` 本身即 `CoroutineScope`，且引擎可将 Kotlin 协程桥接为 JS `Promise`），提供「后台执行、并发、即发即忘、非阻塞等待」四类能力。所有 JS 回调均在引擎线程上执行，线程安全。

> **关于并行**：JS 运行时（QuickJS）本身是单线程的，纯 JS CPU 密集循环无法真正并行；但异步原语让 `await` 切出点、网络 / 文件等挂起调用、以及多个任务之间的挂起点能够交错执行，从而把「重任务」放到后台协程、避免阻塞当前脚本的后续流程。

### 15.1 thread.run / thread.运行 / thread.执行

在后台协程中执行 `task`，返回一个 **Promise**，resolve 为 `task` 的返回值（即「异步执行结果」）。

**参数**：
- `task` (function) — 要执行的函数（必填；非函数则无操作，返回 `undefined`）
- `arg` (any, 可选) — 传给 `task` 的唯一参数

**返回**：`Promise` — resolve 为 `task` 的返回值

```javascript
// 异步执行并拿到结果
let r = await thread.run(() => { return 1 + 2; });
console.log(r); // 3

// 带参数
let r2 = await thread.run((x) => x * 2, 21); // 42
```

### 15.2 thread.all / thread.全部 / thread.并行

并发执行多个 task（类似 `Promise.all`），返回一个 **Promise**，resolve 为结果数组，**顺序与入参一致**；任一 task 抛错则该 Promise 以首个错误 reject。

**参数**：`tasks` (array) — 函数数组（过滤掉非函数项；为空数组时 resolve 为空数组）

**返回**：`Promise<array>`

```javascript
let [a, b] = await thread.all([
    () => heavyWorkA(),
    () => heavyWorkB()
]);
console.log(a, b);

// 也可配合现有异步 API 并发请求
let [r1, r2] = await thread.all([
    () => http.get("https://a.com"),
    () => http.get("https://b.com")
]);
```

### 15.3 thread.launch / thread.启动 / thread.后台

「即发即忘」地在后台执行 `task`，**返回 Promise**。任务异常会被记录到日志（`JsConsole.error`），不影响后续脚本执行。

**参数**：
- `task` (function) — 要执行的函数（必填；非函数则无操作，返回 `undefined`）
- `arg` (any, 可选) — 传给 `task` 的唯一参数

**返回**：`Promise<Undefined>`

```javascript
thread.launch(() => { console.log("后台任务跑完了"); });
console.log("这句会立刻执行，不等待上面的后台任务");
```

### 15.4 thread.sleep / thread.睡眠 / thread.等待

非阻塞等待 `ms` 毫秒，返回一个 **Promise**（底层使用 `delay`，不占用 JS 主线程），可用于轮询 / 节流 / 定时。

**参数**：`ms` (number) — 等待毫秒数（小于 0 按 0 处理）

**返回**：`Promise` — `ms` 毫秒后 resolve

```javascript
console.log("开始");
await thread.sleep(1000); // 非阻塞等待 1 秒
console.log("1 秒后");
```

### 15.5 thread.race / thread.竞争 / thread.竞速

并发执行多个 task（类似 `Promise.race`），返回一个 **Promise**，resolve 为**最先完成**的那个结果；其余未完成的 task 会在外层 Promise 完成后被取消。

**参数**：`tasks` (array) — 函数数组（过滤掉非函数项；为空数组时返回 `undefined`）

**返回**：`Promise` — resolve 为最先完成的结果

```javascript
// 谁先返回用谁
let first = await thread.race([
    () => fastRequest(),
    () => slowRequest()
]);
console.log("最快的结果:", first);
```

### 15.6 thread.timeout / thread.超时

限时执行 `task`：在 `ms` 毫秒内完成任务则正常 resolve 其结果；**超时则以异常 reject Promise**（可通过 `try/catch` 捕获，类似超时控制）。

**参数**：
- `ms` (number) — 超时毫秒数（小于 0 按 0 处理）
- `task` (function) — 要执行的函数（必填；非函数则无操作，返回 `undefined`）
- `args` (any, 可选，可变参) — 传给 `task` 的参数（数组形式）

**返回**：`Promise` — 正常完成 resolve 结果；超时 reject

```javascript
try {
    let r = await thread.timeout(2000, () => heavyWork());
    console.log("在 2 秒内完成:", r);
} catch (e) {
    console.log("超时或执行失败:", e);
}
```

### 15.7 thread.retry / thread.重试

失败自动重试执行 `task`，最多 `count` 次；任意一次成功即 resolve 其结果并停止重试；**全部失败则 reject Promise**（异常为最后一次的错误）。

**参数**：
- `count` (number) — 最大尝试次数（小于 1 按 1 处理）
- `task` (function) — 要执行的函数（必填；非函数则无操作，返回 `undefined`）
- `args` (any, 可选，可变参) — 传给 `task` 的参数（数组形式）

**返回**：`Promise` — 成功 resolve 结果；全部失败 reject

```javascript
// 最多重试 3 次（含首次），适合网络抖动等偶发失败场景
let data = await thread.retry(3, () => http.get("https://api.example.com/data"));
```

### 15.8 thread.map / thread.映射 / thread.并行映射

将 `fn` **并发**作用于数组每个元素，返回一个 **Promise**，resolve 为结果数组，**顺序与入参一致**。可选 `concurrency` 限制最大并发数（缺省为全部并发，适合大数组限流）。

**参数**：
- `items` (array) — 待处理元素数组（必填；非数组返回 `undefined`）
- `fn` (function) — 处理函数，签名为 `fn(item, index)`（必填；非函数返回 `undefined`）
- `concurrency` (number, 可选) — 最大并发数（缺省为 `items` 长度，即全并发）

**返回**：`Promise<array>`

```javascript
// 并发把每个数乘 10
let results = await thread.map([1, 2, 3], (x, i) => x * 10);
console.log(results); // [10, 20, 30]

// 限流为最多 2 个并发（适合大量网络请求）
let pages = await thread.map(urls, (url) => http.get(url), 2);
```

### 15.9 thread.interval / thread.定时 / thread.定时器 / thread.setInterval

每 `ms` 毫秒在后台**重复**执行 `task`，返回一个**定时器句柄对象**（不阻塞 `await`）。句柄方法：
- `stop()` / `停止()` / `取消()` —— 停止定时器
- `isActive()` / `是否在运行()` —— 是否仍在运行（boolean）

**参数**：
- `ms` (number) — 间隔毫秒数（小于 0 按 0 处理）
- `task` (function) — 要执行的函数（必填；非函数则无操作，返回 `undefined`）
- `args` (any, 可选，可变参) — 传给 `task` 的参数（数组形式）

**返回**：定时器句柄对象（含 `stop` / `isActive` 等方法）

```javascript
let timer = thread.interval(1000, () => console.log("每秒一次"));
console.log(timer.isActive()); // true
thread.setTimeout(5000, () => {
    timer.stop(); // 5 秒后停止
    console.log("已停止:", !timer.isActive());
});
```

### 15.10 thread.setTimeout / thread.延时执行 / thread.延迟执行

延时 `ms` 毫秒后在后台**单次**执行 `task`，返回一个**可取消的句柄对象**（不阻塞 `await`）。句柄方法：
- `cancel()` / `取消()` / `停止()` —— 取消尚未执行的任务
- `isActive()` / `是否在运行()` —— 是否仍在等待执行（boolean）

**参数**：
- `ms` (number) — 延时毫秒数（小于 0 按 0 处理）
- `task` (function) — 要执行的函数（必填；非函数则无操作，返回 `undefined`）
- `args` (any, 可选，可变参) — 传给 `task` 的参数（数组形式）

**返回**：定时器句柄对象（含 `cancel` / `isActive` 等方法）

```javascript
let job = thread.setTimeout(3000, () => console.log("3 秒后执行一次"));
// 若想取消：job.cancel();
```

---

## 16. thread 协程上下文

协程上下文用于「定义一段异步任务运行的环境」——包括**作用域（可整体取消）、调度器（运行在哪个线程池）、局部状态（上下文内共享变量）**三个维度。引擎本身基于 `ScriptRuntime`（即 `CoroutineScope`），所有 JS 回调仍在引擎线程上执行以保证单线程安全；上下文的 value 在于把一组任务归并到同一个可管理的生命周期、并按需切到 IO/Main 等调度器做挂起调用。

### 16.1 thread.context / thread.协程上下文 / thread.创建上下文 / thread.createContext

创建一个**协程上下文作用域对象**，返回一个上下文句柄。该句柄自带一套与 `thread` 平行的异步方法，并额外提供 `local` / `cancel` / `isActive` / `name`。

**参数**：`options` (object, 可选) —— 上下文配置：
- `name` / `名称` (string) —— 上下文名称（用于日志标识，缺省 `"context"`）
- `dispatcher` / `调度器` (string) —— 该上下文任务的默认调度器，可选 `main`/`ui`、`io`、`default`/`computation`/`cpu`、`unconfined`（缺省为引擎线程，即与全局 `thread` 一致）
- `onError` / `错误处理` (function) —— **暂未实现**，上下文任务异常统一由 `JsConsole.error` 记录（见末尾说明）

**返回**：上下文对象（含下列方法）

**上下文对象方法**：
- `run(task, args?)` / `运行` / `执行` —— 在上下文中异步执行，返回 Promise；**任务首个参数为上下文对象自身**，便于在任务内读取 `local`
- `launch(task, args?)` / `启动` / `后台` —— 在上下文中即发即忘
- `all([t1, t2, ...])` / `全部` / `并行` —— 在上下文中并发执行多个 task
- `withContext(dispatcher, task, args?)` / `切换上下文` / `切换调度器` —— 在本次调用内临时切换调度器
- `local(key, value?)` / `变量` —— 上下文局部变量读写（**仅本上下文可见**，与全局 `thread.local` 互不影响）
- `cancel()` / `取消` / `停止` —— 整体取消该上下文下的所有任务
- `isActive()` / `是否在运行` —— 上下文作用域是否仍在运行（boolean）
- `name` / `名称` —— 上下文名称

```javascript
// 创建一个运行在 IO 调度器的 worker 上下文
let ctx = thread.context({ name: "worker", dispatcher: "io" });

// 上下文局部变量（仅本上下文可见）
ctx.local("token", "abc123");

// 任务首个参数即上下文自身，可读取其 local
let r = await ctx.run((c) => { return c.local("token"); });
console.log(r); // "abc123"

// 并发跑多个任务，结果保序
let [a, b] = await ctx.all([ () => heavyA(), () => heavyB() ]);

// 即发即忘 + 随时整体取消
ctx.launch(() => { console.log("后台"); });
console.log(ctx.isActive()); // true
ctx.cancel();                // 取消该上下文下的所有任务
console.log(ctx.isActive()); // false
```

### 16.2 thread.withContext / thread.切换上下文 / thread.切换调度器

在**指定调度器**上运行 `task`，返回一个 **Promise**，resolve 为 `task` 的返回值。适用于把阻塞型 / 挂起型 Kotlin 操作（如文件 IO、网络）放到 `io` 线程、或把需要主线程的调用放到 `main`。

> **线程安全**：JS 运行时（QuickJS）是单线程的，所有 JS 函数调用都会被**调度回引擎线程**执行；`withContext` 仅影响任务内 Kotlin 挂起调用的调度器归属，不会把 JS 调用放到其他线程，因此始终是安全的。

**参数**：
- `dispatcher` (string) —— 调度器名称：`main`/`ui`、`io`、`default`/`computation`/`cpu`、`unconfined`
- `task` (function) —— 要执行的函数（必填；非函数返回 `undefined`）
- `args` (any, 可选，可变参) —— 传给 `task` 的参数（数组形式）

**返回**：`Promise` —— resolve 为 `task` 的返回值

```javascript
// 在 IO 线程跑重 IO 任务（JS 调用仍回引擎线程，安全）
let data = await thread.withContext("io", () => readHugeFile());
```

### 16.3 thread.local / thread.变量 / thread.上下文变量

引擎级**共享变量**读写（协程上下文的「状态」维度）。写入后跨脚本调用持久存在，适合在多个脚本 / 多次执行之间传递状态。

**参数**：
- `key` (string) —— 变量名（必填）
- `value` (any, 可选) —— 若提供则执行**赋值**并返回该值；若不提供则执行**取值**，不存在时返回 `undefined`

**返回**：取值时返回变量值（或 `undefined`）；赋值时返回被赋的值

```javascript
// 写入 / 读取 引擎级共享变量
thread.local("lastUser", "dreammoon");
console.log(thread.local("lastUser")); // "dreammoon"

// 与上下文局部变量区分：thread.local 是全局共享，ctx.local 仅本上下文可见
```

---

## 17. toast 轻提示

全局对象 `toast`（中文别名 `吐司`），用于弹出 Android 系统 Toast 轻提示。因 JS 引擎运行于后台线程，所有 Toast 都会切回主线程后显示（避免非主线程调用崩溃），失败时静默忽略，不影响后续脚本。

**方法一览**：

| 方法 | 中文别名 | 说明 |
| --- | --- | --- |
| `toast.show(message, duration?)` | `显示` / `提示` / `吐司` | 显示一条提示；`duration` 可省略（默认短） |
| `toast.short(message)` | `短` | 短提示（2 秒档） |
| `toast.long(message)` | `长` | 长提示（3.5 秒档） |

**时长参数 `duration`（仅 `show` 支持，可选）**：

- 省略 → 短提示
- 字符串：`"short"` / `"短"` → 短；`"long"` / `"长"` → 长
- 数字：`0` → 短；其它（如 `1`） → 长

> 注：原生 Toast 仅「短 / 长」两档，不支持任意毫秒；`duration` 仅决定这两档之一。

**示例**：

```javascript
// 默认短提示
toast.show("保存成功");
toast.吐司("操作完成");

// 长提示（字符串或数字指定）
toast.show("正在加载资源...", "long");
toast.show("即将完成", 1);
toast.long("这条会停留久一点");

// 短提示便捷写法
toast.short("已复制");
```

---

## 18. app - 应用进程控制

全局对象 `app`（中文别名 `应用`），提供三类进程级操作：重启应用、重启并自动进入游戏、退出应用。

所有操作均在协程主线程上下文（`Dispatchers.Main`）执行，对 Activity / 任务的变更（启动 Intent、结束任务栈、终止进程）均安全；异常静默忽略，不影响脚本后续执行。

### app.restart / app.重启 / app.重启应用 / app.重启APP

退出当前进程并以 LAUNCHER Intent 冷重启，重新打开主界面（**不会**自动进入游戏）。

**参数**：无

**返回**：无（返回 `null`）

**示例**：
```javascript
app.restart();   // 重新打开工具主界面
app.重启();
```

### app.restartGame / app.重启游戏

退出当前进程并冷重启，重启后**自动触发「进入游戏」逻辑**（等价于点击主界面「进入游戏」按钮）。

实现：重启的 LAUNCHER Intent 附带 `EXTRA_AUTO_ENTER_GAME`，入口 Activity 在初始化完成后自动调用进入游戏流程（跳过开场 CG 视频、直接启动游戏 Activity）。

**参数**：无

**返回**：无（返回 `null`）

**示例**：
```javascript
app.restartGame();   // 重启并直接进游戏
app.重启游戏();
```

### app.exit / app.退出 / app.退出应用 / app.退出APP

结束当前 Activity 任务栈并终止进程（退出整个应用）。

**参数**：无

**返回**：无（返回 `null`）

**示例**：
```javascript
app.exit();
app.退出();
```

---

## 19. dex - DEX 加载

全局对象 `dex`（中文别名 `dex加载`），用于把外部的 `.dex` / `.apk` / `.jar` 加载到独立的 [DexClassLoader]，从而反射其中的类。加载得到的「类加载器句柄」可传给 `reflect.findClass(name, loader)` 来反射 DEX 内的类。

所有失败均静默返回 `null`，不影响脚本。

### dex.load / dex.加载 / dex.loadDex

统一加载入口，**路径规则与项目其余文件 API 完全一致**（基于 `JsFileAccess`）：

- **绝对路径**（`/` 开头）→ 直接作为本地文件加载；
- **相对路径 / `$WORK_DIR` 等占位符** → 走 `JsFileAccess.resolveInput`：**工作目录优先，无则回退 `assets/pvz2tool/`**；
- **`http(s)://` URL** → 先下载到缓存再加载。

**参数**：
- `path`（string，必填）：`.dex` / `.apk` / `.jar` 文件的绝对路径、工作目录相对路径、`$WORK_DIR` 占位符，或 `http(s)://` URL。
- `parent`（可选）：父类加载器句柄（即另一个 `dex` 对象），用于让新加载的类能引用父加载器中的类；省略则使用应用自身类加载器。

**返回**：类加载器句柄对象（含 `path`、以及 `findClass(name)` 方法，句柄本身携带原始 `ClassLoader`，无需 id），失败（路径不存在 / 解析失败）返回 `null`。

### dex.loadFromAsset / dex.从资源加载

从 APK assets 提取并加载 DEX。

**参数**：`assetName`（string，assets 内相对路径）、可选 `parent`。

### dex.loadFromUrl / dex.从网络加载

从网络 URL 下载并加载 DEX。

**参数**：`url`（string）、可选 `parent`。

**示例**：
```javascript
// 加载外部 DEX 并反射其中类
let loader = dex.load("/sdcard/plugins/my.dex");
let Cls = loader.findClass("com.example.Plugin");
let inst = Cls.newInstance();           // 无参构造
inst.call("doSomething", 1, "x");       // 在实例上调用方法
```

---

## 20. reflect - 反射

全局对象 `reflect`（中文别名 `反射`），基于 [YukiReflection](https://github.com/HighCapable/YukiReflection) 的调用风格，提供类 / 方法 / 字段 / 构造器 / 实例的链式反射操作。

**句柄互通**：keight 通过 `Wrapper<T>` 接口（每个句柄继承 `JsObjectImpl` 并实现 `Wrapper`，把原始 Java 对象（Class / 实例 / ClassLoader）藏在 `value` 中、并 override `toKotlin` 还原）实现任意 Java 对象的 JS 往返。**无需 id、无需注册表**——句柄本身即携带原始对象。把句柄作为实参回传时，`convertArg` 调用 `toKotlin` 直接还原成原始 Java 对象，因此可以「拿到实例再调其方法 / 读写其字段」，也可以「把实例作为另一个方法的参数」。

所有失败均静默返回 `null`。

### reflect.findClass / reflect.查找类 / reflect.反射

按类名取得 `Class` 包装。

**参数**：
- `name`（string，必填）：完整类名，如 `"android.content.Context"`。
- `loader`（可选）：`dex` 加载器句柄（即 `dex.load` 等返回的句柄对象），用于反射 DEX 内的类；省略则在应用类路径中查找。

**返回**：Class 句柄，失败返回 `null`。

### Class 句柄方法

- `method(name, [paramTypes?])` / `方法`：取得方法句柄。`paramTypes` 为类型名数组（如 `["int","java.lang.String"]`），省略则按方法名匹配第一个。
  - **类型匹配说明**：短类型名（`int`/`long`/`boolean`/...）一律解析为**基本类型**（`int.class` 等），其对应装箱类型请用全限定名（如 `java.lang.Integer`）。YukiReflection 的 `param` 按 `Class` 精确匹配、**不会**在基本/装箱间自动转换，因此框架中声明为 `int` 形参的方法用 `"int"` 即可命中；若签名是装箱 `Integer`，引擎会在首次匹配失败后自动用「基本↔装箱」对应类型重试一次，两种写法都能匹配到。
- `field(name)` / `字段`：取得字段句柄。
- `constructor([paramTypes?])` / `构造器` / `构造`：取得构造器句柄。
- `newInstance(...args)` / `新建` / `实例化`：依次尝试无参 / 按实参类型推断的构造器创建实例。
- `getSuperclass()` / `父类`：返回父类 Class 句柄。
- `getDeclaredMethods()` / `方法列表`、`getDeclaredFields()` / `字段列表`：返回名称数组。
- `name` / `simpleName`：类名与短名。

### Method 句柄方法

- `call(instance?, ...args)` / `调用`：在 `instance` 上调用（首参为实例，可 `null` 表示静态），其余为方法实参。
- `invoke(...args)` / `执行`：静态调用（`instance = null`）。

### Field 句柄方法

- `get(instance?)` / `读取` / `获取`：读字段值（首参为实例，可省略/`null` 表示静态）。
- `set(instance?, value)` / `写入` / `设置`：写字段值。

### Constructor 句柄方法

- `newInstance(...args)` / `新建` / `实例化`：创建实例。

### Instance 句柄方法（活对象）

- `call(methodName, ...args)` / `调用方法`：在自身上调用方法。
- `get(fieldName)` / `读字段`：读字段。
- `set(fieldName, value)` / `写字段`：写字段。
- `getId()` / `取ID`、`getClass()` / `取类`、`toString()`。
- `value()` / `原值()` / `js()`：零参方法，返回被包装对象的 keight 原生 JS 值（String→原生字符串、List→JS 数组、数组→JS 数组、映射→JS 对象……）；需要把实例当原生值使用时调用。

> ⚠️ **返回值类型说明**：`findClass` 返回 Class 句柄，`constructor`/`method`/`field` 返回对应句柄；而 `newInstance`/`call`/`invoke`/`get` 的**返回值**按极简、统一的规则包装：
> - 返回 `Class` → Class 句柄（可继续 `.method`/`.field`）；
> - 返回 **`Number` / `Boolean`**（基础类型、装箱数字）→ keight 原生值（`JsNumberWrapper`/`JSBooleanWrapper` 等），**没有** `.call`，但数值比较 `===` **按值可用**（keight 对 `JsNumberWrapper` 做严格值比较），断言直接写 `x === 5`，**无需** `x.toString()`；
> - **其余一切引用类型**（含 `String`、`List`/`Map`/`Set`、数组、`Regex`、`Throwable`、自定义类、`StringBuffer` 等）→ **一律**包装为 `JsInstanceWrapper`，**持有** `.call`/`.get`/`.set`，可继续反射（例如 `reflect.findClass("java.lang.String").newInstance("Hi").call("length")` 现在合法，`String` 同样可 `.call`）。
> - 若要把某个实例句柄转成 keight 原生 JS 值（如把 `String` 当原生字符串拼接、把 `List` 当 JS 数组遍历），调用它的 **`value()` / `原值()` / `js()`** 零参方法即可拿到原生值（内部严格对齐 keight 官方 `Mapping.kt`：CharSequence→原生字符串、`List`/`Set`/`Map`/数组→JS 数组/集合/对象、数字/Boolean→原生、`Regex`/`Throwable`→`JsRegexWrapper`/`JSError`，兜底 `toString()`）。
>
> 因此：除数字/Boolean/Class 外，所有反射结果都是「可继续 `.call` 的实例句柄」，规则简单且无遗漏类型；需要原生 JS 值时再调 `.value()`。

**示例**：
```javascript
// 反射系统类并调用静态方法
let Ctx = reflect.findClass("android.content.Context");
let MODE = Ctx.field("MODE_PRIVATE").get();   // 读静态字段

// 构造实例 + 链式调用
let Cls = reflect.findClass("com.popcap.SexyAppFramework.SomeClass");
let inst = Cls.constructor(["int"]).newInstance(0);
inst.call("init");
let r = inst.call("compute", 1, 2);
console.log(r);

// 把实例作为另一个方法的参数
otherInst.call("attach", inst);
```

## 21. vpn - VPN 控制

`vpn` 对象提供 VPN 断网 / 恢复的网络控制能力（底层为 `LocalVpnService`）。

> **别名**：`vpn` / `虚拟专网` / `VPN` 均可访问同一对象。

### 方法

#### vpn.disconnect / vpn.断网 / vpn.断开网络

断开网络（开启 VPN 拦截）。

**语法**：`vpn.disconnect() -> void`

**参数**：无

**说明**：
- 内部调用 `LocalVpnService.startVpn(context)` 启动 VPN 拦截，使设备进入断网状态。
- 若系统尚未授予 VPN 权限，`startVpn` 不会真正断网（与悬浮球"断开网络"按钮行为一致）。
- 操作在主线程执行，任何异常均静默吞掉（runCatching）。

**返回**：void

**示例**：
```javascript
vpn.disconnect();   // 断网
vpn.断网();
```

#### vpn.restore / vpn.恢复 / vpn.恢复网络

恢复网络（关闭 VPN）。

**语法**：`vpn.restore() -> void`

**参数**：无

**说明**：
- 内部调用 `LocalVpnService.stopVpn(context)` 彻底关闭 VPN，设备恢复正常网络。
- 操作在主线程执行，任何异常均静默吞掉（runCatching）。

**返回**：void

**示例**：
```javascript
vpn.restore();      // 恢复网络
vpn.恢复网络();
```

#### vpn.isActive / vpn.是否激活 / vpn.是否开启

查询当前 VPN 是否处于激活状态（即是否处于断网状态）。

**语法**：`vpn.isActive() -> boolean`

**参数**：无

**说明**：
- 直接读取 `LocalVpnService.isVpnActive` 状态流，返回 `true` 表示 VPN 已开启（已断网），`false` 表示网络正常。

**返回**：boolean - 是否已断网

**示例**：
```javascript
if (vpn.isActive()) {
    console.log("当前处于断网状态");
    vpn.restore();   // 恢复网络
} else {
    vpn.disconnect(); // 断网
}
```

#### vpn.isPrepared / vpn.是否已授权 / vpn.已授权 / vpn.是否可用

查询 VPN 是否已获得系统授权（即能否真正执行断网）。

**语法**：`vpn.isPrepared() -> boolean`

**参数**：无

**说明**：
- 等价于 Kotlin 侧的 `LocalVpnService.prepareVpn(context) == null`：系统 `VpnService.prepare()` 返回 `null` 代表已授权，返回 Intent 代表需要用户先在系统弹窗中确认。
- 返回 `false` 时调用 `vpn.disconnect()` **不会真正断网**，因此断网类功能建议先用本方法做前置判断。
- Context 优先取当前前台 Activity（`ContextUtil.getCurrentActivity()`），拿不到则回退全局 Context；调用切主线程执行，异常一律返回 `false`。

**返回**：boolean - 是否已授权

**示例**：
```javascript
if (!vpn.isPrepared()) {
    toast.show("请先授权 VPN 后再使用断网功能");
} else if (vpn.isActive()) {
    vpn.restore();
} else {
    vpn.disconnect();
}
```

```yaml
# 用作悬浮窗按钮的可见性条件：未授权时整个按钮隐藏
- id: vpn_toggle
  name: "{{js:vpn.isActive() ? '恢复网络' : '断开网络'}}"
  isShowFromJs: "vpn.isPrepared()"
  jsScript: "vpn.isActive() ? vpn.restore() : vpn.disconnect();"
```

*文档版本: 2.1*
*最后更新: 2026-07-17*
*新增：ui 系列弹窗通用可定制化——所有弹窗（alert/confirm/prompt/select/multiSelect/actionSheet/slider/loading）新增按钮文字（confirmText/cancelText）、按钮背景色（confirmColor/cancelColor，支持命名色与 #RRGGBB/#AARRGGBB 十六进制）、可关闭性（dismissible/可关闭，alert/confirm/prompt/slider/loading 用此名；select/multiSelect/actionSheet 沿用 cancelable），以及事件回调（onConfirm/onCancel/onSelect/onChange/onDismiss，均为 function(value) 形式、异步触发且不阻塞 await）。slider 另增 decimals（小数位）/showValue（是否显示大字体数值）与实时 onChange 回调；loading 另增 update()/更新() 实时刷新文字。详见各弹窗小节与开头「通用选项与回调」*
*优化：ui.select/ui.multiSelect 序号颜色 showIndexColor 改为透传至复合文本 PvzRichText 的 defaultStyle，从而支持任意颜色（命名色 black/white/red/green/gold/purple/gray/olive/blue/yellow/orange/cyan/pink 或 #RRGGBB/#AARRGGBB 十六进制，默认 black），不再仅限黑白；外层统一控制 + 单项字段覆盖机制不变*
*新增：ui.select/ui.multiSelect 的 options 新增 showIndexColor（原仅 black/white）——统一控制序号颜色；每个条目对象亦可用 showIndexColor/序号颜色 字段单独控制，优先级高于外层（与 showIndex 的单项覆盖机制一致）*
*优化：ui.slider/ui.滑块（数值滑块）——点击「取消」由返回初始默认值改为返回 null（与 ui.prompt/ui.select/ui.actionSheet 的取消语义一致）；滑块视觉改为参考 SectionType.SLIDER 的自定义齿轮滑块（胶囊轨道 + 主题色渐变 + 齿轮随进度旋转），不再是系统原生 Slider*
*移除：ui.select/ui.选择 的 options.defaultValue —— 单选为「点选即返回」，预选仅高亮、无确认/清除场景，属无用代码；多选 defaultValues 保留（配合确定按钮生效）*
*新增：ui.select/ui.multiSelect 的 options 新增 showIndex（boolean，默认 false）——开启后仅对「有图标」的项在其图标上居中叠加从 1 开始的序号（黑色文字，不遮盖原图）；无图标项不显示数字。每个条目对象亦可用 showIndex/显示序号 字段单独控制，优先级高于外层 options.showIndex*
*优化：ui.select/ui.multiSelect 选择弹窗大数据量卡顿——列表改 LazyColumn、网格改 LazyVerticalGrid（仅组合可见项），并给 PvzStyledDialog 增加 contentScrollable 开关避免与 Lazy 嵌套滚动冲突*
*新增：ui.select/ui.选择 单项选择弹窗（图标网格>8 / 列表<=8 / 纯文字单选 三态；无图标项以同尺寸占位矩形居中显示截断文字；参考 RADIO 样式）；ui.multiSelect/ui.多选 多项选择弹窗（返回选中值数组）*
*新增：device 设备信息对象（system/screen/memory/storage/battery/network/app/cpu 分组 + info() 聚合，及中文别名），并补入内置对象总览表；cpu 分组含核心数/架构/ABI/频率(kHz与MHz)/调度器*
*新增：ui.prompt/ui.输入 新增第 4 个可选参数 placeholder（输入框占位提示文字），state 数据类与弹窗渲染同步支持，缺省回退「请输入...」*
*新增：audio 音频控制对象（getBgmVolume/setBgmVolume/getSfxVolume/setSfxVolume 及中文别名），并补入内置对象总览表（同时补 http）*
*修正：http.json()/response.解析JSON() 返回已解析的 JS 对象（解析失败返回 null），非 JSON 字符串*
*新增：picker 文件选择器（directory/file/files，返回文件对象，支持多选与 copy 到 SAF 树内新建文件）*
*新增：clipboard 剪切板对象（copy/复制、read/读取/粘贴、clear/清空，基于系统 ClipboardManager）*
*新增：browser 浏览器对象（open/打开/打开链接/openLink，调用系统浏览器打开链接；未带协议自动补全 https://，基于 Intent.ACTION_VIEW）*
*新增：thread 协程/异步对象（run/运行/执行 返回 Promise 的异步执行结果；all/全部/并行 并发执行多任务返回结果数组，类似 Promise.all；launch/启动/后台 即发即忘后台执行、异常记日志；sleep/睡眠/等待 非阻塞 delay 返回 Promise）。基于 ScriptRuntime 即 CoroutineScope 与 keight 的协程↔Promise 桥接（JSFunction(isAsync=true) 返回 Promise），回调均在引擎线程执行保证线程安全*
*扩展：thread 异步原语新增 race/竞争/竞速（并发竞速取最快结果，类似 Promise.race）、timeout/超时（限时执行，超时 reject）、retry/重试（失败自动重试最多 count 次）、map/映射/并行映射（并发映射数组，可选 concurrency 限流）、interval/定时/定时器/setInterval（重复定时，返回可取消句柄）、setTimeout/延时执行/延迟执行（延时单次，返回可取消句柄）；均沿用 run/all 的 async{}.js Promise 风格与 args 可变参约定*
*修正：pvz.<type>.all 返回 Array（数据对象数组），单个条目仍可由父对象按 code/name 访问*
*修正：file.copy/file.复制 当 toPath 带扩展名时按目标文件处理并重命名，否则视为目标目录*
*修正：file.list 路径无效时返回空数组 []（非 null）*
*修正：file.resolve() 中性对象 isFile 实际为 true（兼具文件对象特征），并区分读写类/属性类方法的异常行为*
*修正：storage.getAll() 返回数组（非 key-value 对象）*
*修正：this.findById 仅接受单个 id 参数（返回 item 或 section 对象），无双参及 .item/.section 子属性*
*新增：vpn VPN 控制对象（disconnect/断网/断开网络、restore/恢复/恢复网络、isActive/是否激活/是否开启，底层 LocalVpnService）+ ui.showGameDisplay/弹出画面设置/画面设置 弹出游戏画面设置全屏浮窗（同悬浮球"画面设置"），并补入别名速查表与第 21 章详解*
*新增：可见性判定 API —— vpn.isPrepared/是否已授权/已授权/是否可用（等价 LocalVpnService.prepareVpn(context) == null，未授权时 disconnect 不会真断网）、ui.isCustomGameDisplayEnabled/是否启用自定义画面/画面设置是否可用（读取 SettingsDialogState.isUseCustomGameDisplay）。二者主要配合 dream.yml 悬浮窗按钮的 isShowFromJs 字段做运行时动态显隐，详见 config_documentation.md 的 floatingWindow 章节*
*补充：section 对象新增 descriptionValues/描述值；RADIO 项同时支持 checked/选中 别名*
*补充：rton.load 支持直接加载 .json 文件；path.toInternalPath 相对路径自动按 $WORK_DIR 处理*
*新增：picker 文件选择器对象（directory/file/files 及中文别名），支持选择目录/单文件/多文件并返回文件对象（基于 SAF DocumentFile）*
*新增：thread 协程上下文（context/协程上下文/创建上下文/createContext 创建可定义 name/dispatcher、可整体 cancel、可共享 local 局部变量的作用域对象；上下文自带 run/launch/all/withContext/local/cancel/isActive/name；任务首个参数为上下文自身便于读取 local）；thread.withContext/切换上下文/切换调度器（在 main/io/default/computation/unconfined 指定调度器上运行 task，JS 调用仍调度回引擎线程保证单线程安全）；thread.local/变量/上下文变量（引擎级全局共享变量，跨脚本持久）。参见新增第 16 节*
*新增：toast 轻提示对象（show/显示/提示/吐司，及 short/短、long/长 便捷方法；duration 支持 short/long 字符串或 0/1 数字，省略默认短），切主线程显示、失败静默忽略。参见新增第 17 节*
*新增：app 应用进程控制对象（restart/重启/重启应用、restartGame/重启游戏、exit/退出/退出应用/退出APP；冷重启经 LAUNCHER Intent + NEW_TASK|CLEAR_TASK，重启游戏经 EXTRA_AUTO_ENTER_GAME 让入口 Activity 自动进入游戏，退出经 finishAffinity + killProcess）。参见新增第 18 节*
*修正：app 第 18 节说明——主线程切换已由 Handler(Looper.getMainLooper()) 改为协程 Dispatchers.Main（与 JsToast/toast 一致）*
*新增：dex DEX 加载对象（load/加载/loadDex、loadFromAsset/从资源加载、loadFromUrl/从网络加载；基于 DexClassLoader 加载 .dex/.apk/.jar 到独立类加载器并返回句柄，句柄可传给 reflect.findClass 反射 DEX 内类）。参见新增第 19 节*
*新增：reflect 反射对象（findClass/查找类/反射 及 YukiReflection 风格的 Class/Method/Field/Constructor/Instance 链式操作；活对象以「`Wrapper<T>` 子类句柄」（JsClassWrapper/JsInstanceWrapper/JsLoaderWrapper，均实现 Wrapper 并 override toKotlin 还原原始对象）在 JS 间往返，支持实例方法调用、字段读写、实例作为方法参数，无需 id 注册表）。参见新增第 20 节*
*修正：dex/reflect 句柄机制由「id 注册表 + 句柄」重构为 keight 原生 `Wrapper<T>` 子类（消除全局注册表与所有 `.id` 属性，`convertArg` 退化为 `arg.toKotlin(runtime)`）；并移除 `dex.of/取加载器` 与 `reflect.of/取类` 两个按 id 取回的方法（句柄本身即携带原始对象，无需按 id 找回）*
