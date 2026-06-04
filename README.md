# PVZ2Tool — 植物大战僵尸2 内置启动器

一个嵌入 PVZ2 游戏 APK 的启动器项目，基于 Kotlin Multiplatform + Compose 构建。

---

## 功能介绍

PVZ2Tool 提供了一套完整的内置游戏修改框架：

| 模块 | 说明 |
|---|---|
| **OBB/RSB 解包重打包** | 完整解析 PopCap 的 RSB 格式（含 RSGP 资源组、ZLib 压缩、4K 对齐），支持自动 JSON↔RTON、PNG↔PTX 互转 |
| **SMF 脚本数据修改** | 支持加载、修改、保存 SMF 封装的游戏配置数据，按版本隔离缓存 |
| **JS 执行引擎** | 内置 keight 编译器，在游戏中注入 JS 脚本运行时，支持中英文 API |
| **RTON 编解码** | 完整 RTON 二进制格式读写（Magic 校验、字符串池、Rijndael 加密/解密） |
| **PTX 纹理处理** | 支持 ARGB8888、RGB565、ETC1、DXT1/3/5、PVRTC 等多种像素格式编解码 |
| **游戏存档修改** | 直接读写 PvZ2 游戏存档，支持数值加密/解密 |
| **动态版本管理** | 多版本区块隔离，每个版本独立 SMF 缓存和数据目录 |
| **VPN 抓包** | 内置 LocalVpnService，支持拦截/修改游戏网络请求 |

**栏目（Section）支持的类型**：

| 类型 | 用途 | 示例 |
|---|---|---|
| `RADIO` | 单选组（版本切换） | 切换国际版/中文版数据 |
| `CHECKBOX` | 多选开关 | 阳光自动收集、植物无冷却 |
| `BUTTON` | 触发按钮 | 一键修改金币、兑换礼品码 |
| `SLIDER` | 滑动条 | 调节游戏速度倍率 |
| `INPUT` | 文本输入 | 输入礼品码、自定义数值 |
| `INFO` | 信息展示 | 显示当前金币/钻石数量 |

---

## 项目结构

```
composeApp/
├── src/
│   ├── commonMain/          # 跨平台核心：RSB/RTON/PTX/ZLib 等
│   │   └── kotlin/.../pop/
│   │       ├── core/rsb/    # RSB 打包/解包 & RSGP 资源组
│   │       ├── image/ptx/   # PTX 纹理编解码
│   │       ├── plugin/      # 加解密、二进制流工具
│   │       └── rton/        # RTON 编解码
│   └── androidMain/        # Android 平台：JS 引擎、UI、配置
│       ├── assets/pvz2tool/ # 默认配置文件与 JS 脚本
│       └── kotlin/.../pvz2tool/
│           ├── js/          # JS 引擎 & API 实现
│           └── ui/          # Compose UI 界面
└── build/                   # 测试构建输出（HTML 报告）
```

---

## JS API 使用示例

项目中 JS 脚本通过 `this` 上下文访问当前栏目的状态，所有 API **同时支持中英文**调用。

### 基础：读取/修改栏目项状态

```javascript
// this - 当前上下文
console.log(this.version.id)       // 当前版本ID
console.log(this.version.name)     // 当前版本名称
console.log(this.gameActivity)     // 当前游戏Activity类名

// this.checked - CHECKBOX 选中状态 (可读写)
let isEnabled = this.checked
this.checked = true

// this.all - 遍历所有栏目和项
let code = this.all.welfare.giftcode_input.value.trim()

// 触发另一个项的JS
this.call("GoldQuantity")
// 中文：
this.调用("GoldQuantity")
```

### SMF 数据修改：自动收集阳光

```javascript
// 加载 SMF 中的配置文件
let config = data.dynamic.packages.activityconfig.load()

// 修改字段
config.objects[0].objdata.AutoSunCollect = this.checked

if (this.checked) {
    console.log("阳光自动收集", "已开启")
} else {
    console.log("阳光自动收集", "已关闭")
}

// 写回
config.save()
```

### 游戏存档读写：加减金币/钻石

```javascript
let saves = pvz.saves.load()           // 加载存档
let datas = saves.objects[0].objdata

// 金币 (gg 是明文)
datas.gg += 300

// 钻石 (g 是加密值，需要先解密)
datas.g = (datas.g.解密 + 300).加密
// 或英文：datas.g = (datas.g.decrypt + 300).encrypt

// 拼图碎片 (c 也是加密值)
datas.c = (datas.c.decrypt + 5000).encrypt

saves.save()                           // 保存
```

### 植物碎片操作

```javascript
// pvz.植物 - 按中文名访问植物数据
let pea = pvz.植物.豌豆射手
console.log(pea.name, pea.order)       // 名称和序号

function addPlantShard(plant, count) {
    if (!datas.ppr) {
        datas.ppr = []                 // 碎片数组
    }

    for (let i = 0; i < datas.ppr.length; i++) {
        if (datas.ppr[i].pi == plant.order) {
            datas.ppr[i].pc += count
            saves.save()
            return
        }
    }

    datas.ppr.push({ pi: plant.order, pc: count })
    saves.save()
}

addPlantShard(pvz.植物.豌豆射手, 10)
addPlantShard(pvz.植物.樱桃炸弹, 5)
```

### 持久化存储 & UI 弹窗

```javascript
// storage - 持久化键值存储
storage.set("last_code", "PVZ2026")
let record = storage.get("used_giftcode_PVZ2026")

// ui - 弹窗交互
ui.alert("兑换成功！", "获得：钻石×300 + 金币×5000")
ui.confirm("确认操作", "确定要重置进度吗？")

// console - 日志输出
console.log("操作完成")
console.error("保存失败：" + err)
```

### 文件操作

```javascript
// file API - 读写文件（支持占位符路径）
let f = file.resolve("$WORK_DIR/config/my_config.rtom")

let content = f.readText()
// 修改内容...
f.writeText(content)

f.copy("$WORK_DIR/backup/config.rtom")
f.delete()
```

### RSB / ZLib 操作

```javascript
// ZLib 压缩/解压
zlib.unpack("$ITEM/compressed.zlib", "$WORK_DIR/decompressed.bin")
zlib.pack("$WORK_DIR/data.bin", "$WORK_DIR/data.zlib")

// RSB 打包/解包
rsb.ウンpack("$ITEM/main.rsb", "$WORK_DIR/extracted/", {
    compression: true,            // 解压 ZLib 内容
    autoGunzip: true,             // 自动 .gz 解压
    convertRtonToJson: true       // RTON → JSON 自动转换
})

rsb.pack("$WORK_DIR/modified/", "$WORK_DIR/output.rsb", {
    compression: true,
    autoGzip: true,
    convertJsonToRton: true       // JSON → RTON 自动转换
})
```

### 占位符路径系统

| 占位符 | 解析结果 |
|---|---|
| `$WORK_DIR` | 工具箱工作根目录 |
| `$GAME_SAVES` | 游戏存档目录 |
| `$SMF` | 当前版本的 SMF 资源目录 |
| `$ITEM` | 当前栏目项的资源目录 |
| `$JS_DIR` | 当前 JS 脚本所在目录 |

---

## 全局对象速查

| 对象 | 中文名 | 常用方法 |
|---|---|---|
| `console` | `控制台` | `log()` / `info()` / `warn()` / `error()` |
| `pvz` | `植物大战僵尸` | `saves.load()` / `植物.豌豆射手` 等 17 类数据 |
| `ui` | `界面` | `alert()` / `confirm()` / `prompt()` / `progress()` |
| `storage` | `存储` | `get()` / `set()` / `delete()` / `has()` / `clear()` |
| `file` | `文件` | `resolve()` / `readText()` / `writeText()` / `copy()` / `delete()` |
| `data` | `数据` | `data.smfName.packages.fileName.load().save()` |
| `rton` | `rton` | `decode()` / `encode()` / `load()` / `encryptionKey` |
| `rsb` | `rsb` | `unpack()` / `pack()` |
| `zlib` | `zlib` | `unpack()` / `pack()` |
| `ptx` | `ptx` | `decode()` / `encode()` / `format` |
| `path` | `路径` | `resolve()` / `app` / `android` / `pvz` / `pvz2tool` |

> 完整 JS API 文档参见项目中 `assets/pvz2tool/js_documentation.md`。

---

## Kotlin API 使用示例

所有核心模块均以 Kotlin 协程风格暴露，在 `commonMain` 中可直接调用。

### RSB 打包/解包

```kotlin
import io.github.dreammooncai.pvz2tool.pop.core.rsb.Rsb
import java.io.File

// --- 解包 ---
Rsb.unpack(File("input/main.rsb"), File("output/extracted/")) {
    autoRtonToJson = true        // RTON 自动转 JSON
    deleteOriginalRton = true    // 转换后删除原 RTON
    autoPtxToPng = false         // PTX 自动转 PNG
    deleteOriginalPtx = true
    setOnProgress { progress, message ->
        println("解包：%.0f%% — %s".format(progress * 100, message))
    }
    setOnFinish { outDir ->
        println("解包完成 → $outDir")
    }
}

// --- 打包 ---
Rsb.pack(File("output/extracted/"), File("output/repacked.rsb")) {
    autoJsonToRton = true        // JSON 自动转 RTON
    deleteOriginalJson = false
    autoPngToPtx = true          // PNG 自动转 PTX
    deleteOriginalPng = false
    setOnProgress { progress, message ->
        println("打包：%.0f%% — %s".format(progress * 100, message))
    }
}
```

### RTON 编解码

```kotlin
import io.github.dreammooncai.pvz2tool.pop.rton.RTON
import java.io.File

// 自动识别加密 → 解码为文本
RTON.decodeAuto(File("data.rtom"), File("data.rtom.txt"))
// 注意：扩展名 .rtom.txt 时自动转为 JSON-like 文本；否则转为二进制

// 编码（自动加密）
RTON.encodeAuto(File("data.rtom.txt"), File("data_modified.rtom"))

// 修改加密密钥（可选）
RTON.encryptionKey = "custom_key_here"  // 默认值通常不需要修改

// 显式控制加密
RTON.decodeAndDecrypt(File("encrypted.rtom"), File("decrypted.txt"))
RTON.encodeAndEncrypt(File("decrypted.txt"), File("encrypted.rtom"))
```

### ZLib 压缩/解压

```kotlin
import io.github.dreammooncai.pvz2tool.pop.core.rsb.util.Zlib
import io.github.dreammooncai.pvz2tool.pop.core.rsb.util.CompressionLevel
import java.io.File

// 解压
Zlib.unpack(File("compressed.zlib"), File("decompressed.bin"))

// 压缩
Zlib.pack(File("data.bin"), File("data.zlib"), CompressionLevel.Optimal)

// 中文模式（部分游戏资源使用特殊压缩算法）
Zlib.pack(File("cht_data.bin"), File("cht_data.zlib"), CompressionLevel.Optimal, isChineseMode = true)
```

### PTX 纹理编解码

```kotlin
import io.github.dreammooncai.pvz2tool.pop.image.ptx.Ptx
import io.github.dreammooncai.pvz2tool.pop.image.ptx.PtxFormat
import io.github.dreammooncai.pvz2tool.pop.plugin.io.Endian
import java.io.File

// PTX → PNG 解码
Ptx.decode(File("texture.ptx"), File("texture.png"), fromRsb = false)

// PNG → PTX 编码（ARGB8888）
Ptx.encode(
    File("texture.png"),
    File("texture.ptx"),
    format = PtxFormat.ARGB8888,
    encodeEndian = Endian.Small
)

// 编码为 ETC1 格式
Ptx.encode(
    File("texture.png"),
    File("texture.etc1.ptx"),
    format = PtxFormat.ETC1_RGB,
    chineseMode = false
)

// 全局开关：ABGR8888 字节序修正
Ptx.PtxABGR8888Mode = true          // 部分资源的 ABGR 字节序需要交换
Ptx.RsbPtxABGR8888Mode = true       // RSB 中提取的 PTX 默认开启
```

### PvZ2 数值加密

```kotlin
import io.github.dreammooncai.pvz2tool.pop.plugin.crypt.Pvz2NumberCrypt

// 对游戏存档中的数值进行加密/解密
val rawValue = 12345678L
val encrypted = Pvz2NumberCrypt.encrypt(rawValue)    // 加密后存入存档
val decrypted = Pvz2NumberCrypt.decrypt(encrypted)   // 从存档读出后解密

// 纯计算，无协程依赖，可直接在任意线程调用
```

### CoroutineBinaryStream — 二进制流读写

```kotlin
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream
import io.github.dreammooncai.pvz2tool.pop.plugin.io.Endian
import java.io.File

// 读取模式
val bs = CoroutineBinaryStream.open(File("data.bin"))
val magic = bs.readInt32()
val version = bs.readUInt32()
val name = bs.readString(32)
bs.close()

// 写入模式
val writer = CoroutineBinaryStream.create(File("output.bin"))
writer.writeInt32(0x524F544E)          // Magic "RTON"
writer.writeUInt32(1u)                 // Version
writer.writeString("Hello World")
writer.saveFile()                      // 持久化到磁盘

// 切片（零拷贝，共享底层数据）
val slice = bs.slice(offset = 16, length = 1024)
slice.readBytes(1024)

// 切换字节序
Endian.active = Endian.Big
```

### 配置模型编程

```kotlin
import io.github.dreammooncai.pvz2tool.*

// 构建配置对象
val section = DynamicSection(
    id = "cheats",
    title = "修改项",
    targetPath = "\$GAME_SMF/dynamic.obb",
    items = listOf(
        SectionItem(
            id = "auto_collect",
            type = SectionType.CHECKBOX,
            name = "自动收集阳光",
            desc = "游戏内自动收集阳光和金币",
            jsScript = """
                let config = data.dynamic.packages.activityconfig.load()
                config.objects[0].objdata.AutoSunCollect = this.checked
                config.save()
            """.trimIndent()
        ),
        SectionItem(
            id = "speed_multiplier",
            type = SectionType.SLIDER,
            name = "游戏速度",
            minValue = 0.5f,
            maxValue = 5.0f,
            defaultValue = 1.0f,
            step = 0.5f,
            valueSuffix = "x"
        )
    )
)

// 读取目标目录
val targetDir = section.resolveTargetDirectory()

// 读取 JS 脚本
val jsContent = section.readJs(version)
```

---

## 构建项目

```shell
# macOS / Linux
./gradlew :composeApp:assembleDebug

# Windows
.\gradlew.bat :composeApp:assembleDebug
```

构建产物为标准 APK，位于 `composeApp/build/outputs/apk/`。

---

## 内置启动器适配流程

将本项目打包为 APK 后，按以下步骤将启动器嵌入目标游戏 APK。

### 第一步：提取并合并文件

从项目 APK 中提取以下内容，复制到**目标游戏 APK** 对应位置：

| 来源路径（项目 APK） | 操作 |
|---|---|
| `assets/pvz2tool/` | 整目录复制 |
| `kotlin/` | 整目录复制 |
| `META-INF/` | 整目录复制 |
| `org/` | 整目录复制 |
| `res/` | 整目录合并 |
| `DebugProbesKt.bin` | 复制到根 |
| `kotlin-tooling-metadata.json` | 复制到根 |

### 第二步：合并 resources.arsc

1. 使用 **MT 管理器 Arsc 编辑器++**（或同类工具）将项目 APK 的 `resources.arsc` 导出；
2. 将导出内容**导入**到目标游戏 APK 的 `resources.arsc` 中。

### 第三步：合并 DEX

1. 确认项目 APK 包含的 dex 数量（例如 4 个：`classes.dex` ~ `classes4.dex`）；
2. 将目标游戏 APK 原有的所有 dex **重命名**，起始序号 = 项目 APK dex 数量 + 1；
   - 示例：项目有 4 个 dex，则游戏原有 dex 依次改为 `classes5.dex`、`classes6.dex` …
3. 将项目 APK 的全部 dex **复制**到目标游戏 APK 中。

### 第四步：修改 AndroidManifest.xml

在目标游戏 APK 的 `AndroidManifest.xml` 中，定位 `<application ...>...</application>` 块，进行以下操作：

#### 4.1 删除原有 LAUNCHER intent-filter

找到并**删除**游戏原来的启动入口：

```xml
<intent-filter>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.LAUNCHER" />
</intent-filter>
```

#### 4.2 追加启动器组件声明

将以下内容追加到 `<application>` 块内（将所有 `游戏包名` 替换为目标游戏的实际包名）：

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:exported="false"
    android:authorities="游戏包名.fileprovider"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@app66:xml/file_paths" />
</provider>

<activity
    android:theme="@style/Theme.SexyApp"
    android:label="@string/app_label"
    android:name="io.github.dreammooncai.pvz2tool.Pvz2InitializeActivity"
    android:screenOrientation="sensorLandscape"
    android:configChanges="keyboardHidden|orientation"
    android:windowSoftInputMode="stateAlwaysHidden">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <data android:scheme="com.sexyactioncool.bejeweledblitz" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
    </intent-filter>
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
    <meta-data
        android:name="android.max_aspect"
        android:value="2.5" />
    <intent-filter android:label="导入 PVZ2 存档">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:mimeType="application/x-pvz2saves" />
        <data android:scheme="content" />
    </intent-filter>
    <intent-filter android:label="导入 PVZ2 存档">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="file" />
        <data android:host="*" />
        <data android:pathPattern=".*\\.pvz2saves" />
    </intent-filter>
    <intent-filter android:label="导入 PVZ2 存档">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="content" />
        <data android:scheme="file" />
        <data android:mimeType="*/*" />
        <data android:host="*" />
        <data android:pathPattern=".*\\.pvz2saves" />
    </intent-filter>
    <intent-filter android:label="导入 PVZ2 存档">
        <action android:name="android.intent.action.SEND" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:mimeType="application/x-pvz2saves" />
        <data android:mimeType="application/zip" />
    </intent-filter>
</activity>

<service
    android:name="io.github.dreammooncai.pvz2tool.service.LocalVpnService"
    android:permission="android.permission.BIND_VPN_SERVICE"
    android:exported="true">
    <intent-filter>
        <action android:name="android.net.VpnService" />
    </intent-filter>
</service>

<provider
    android:name="com.petterp.floatingx.assist.FxContentProvider"
    android:exported="false"
    android:multiprocess="true"
    android:authorities="游戏包名.fx.provider" />

<provider
    android:name="androidx.startup.InitializationProvider"
    android:exported="false"
    android:authorities="游戏包名.androidx-startup">
    <meta-data
        android:name="androidx.emoji2.text.EmojiCompatInitializer"
        android:value="androidx.startup" />
    <meta-data
        android:name="androidx.lifecycle.ProcessLifecycleInitializer"
        android:value="androidx.startup" />
    <meta-data
        android:name="androidx.profileinstaller.ProfileInstallerInitializer"
        android:value="androidx.startup" />
</provider>

<uses-library
    android:name="androidx.window.extensions"
    android:required="false" />
<uses-library
    android:name="androidx.window.sidecar"
    android:required="false" />

<receiver
    android:name="androidx.profileinstaller.ProfileInstallReceiver"
    android:permission="android.permission.DUMP"
    android:enabled="true"
    android:exported="true"
    android:directBootAware="false">
    <intent-filter>
        <action android:name="androidx.profileinstaller.action.INSTALL_PROFILE" />
    </intent-filter>
    <intent-filter>
        <action android:name="androidx.profileinstaller.action.SKIP_FILE" />
    </intent-filter>
    <intent-filter>
        <action android:name="androidx.profileinstaller.action.SAVE_PROFILE" />
    </intent-filter>
    <intent-filter>
        <action android:name="androidx.profileinstaller.action.BENCHMARK_OPERATION" />
    </intent-filter>
</receiver>

<provider
    android:name="com.kdroid.androidcontextprovider.ContextInitProvider"
    android:exported="false"
    android:authorities="游戏包名.kmploginitprovider" />
```

> **包名替换**：上述 XML 中所有 `游戏包名` 字符串需统一替换为目标游戏的实际包名（如 `com.ea.game.pvzfree_row`）。

#### 4.3 检查 targetSdkVersion

确认 `AndroidManifest.xml` 中 `targetSdkVersion` ≥ **21**，若低于 21 请手动改为 `21`。

### 第五步：处理 lib 目录（原生库）

> ⚠️ **重要**：`armeabi` 与 `armeabi-v7a` 内容完全相同，**不可同时保留**，否则部分设备会闪退。

- 根据目标游戏 APK 已有的 ABI 架构（如 `armeabi-v7a`、`arm64-v8a`、`x86` 等）选择对应的 so 文件合并；
- 不要直接整个替换 `lib/` 目录，按架构逐一对应替换。

---

至此，内置启动器适配完成。
