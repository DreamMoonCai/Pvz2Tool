package io.github.dreammooncai.pvz2tool.js.code

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.Build
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.WindowManager
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.js
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.js.eq
import io.github.dreammooncai.pvz2tool.js.func
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Locale
import java.util.TimeZone

/**
 * 设备信息全局对象：`device`。
 *
 * 基于 Android 系统 API（[Build]、[WindowManager]、[ActivityManager]、
 * [BatteryManager]、[ConnectivityManager]、[StatFs] 等），
 * 提供当前安卓设备的系统、屏幕、内存、存储、电池、网络、应用、CPU 及 Root 状态等信息。
 *
 * 用法：
 * ```js
 * // 一次性获取全部信息（返回聚合对象）
 * let all = device.info();
 * console.log(all.system.model, all.system.androidVersion, all.battery.level);
 *
 * // 按分组获取
 * let s = device.screen.info();
 * console.log(s.width, s.height, s.densityDpi, s.refreshRate);
 *
 * // 直接读取常用字段
 * console.log(device.system.model());          // 设备型号
 * console.log(device.system.androidVersion()); // 安卓版本
 * console.log(device.system.sdkVersion());     // SDK 版本号
 * console.log(device.battery.level());         // 电量 0~100
 * console.log(device.battery.isCharging());    // 是否充电
 * console.log(device.memory.total());          // 总内存（字节）
 * console.log(device.network.type());          // wifi / cellular / ethernet / none
 * console.log(device.app.packageName());       // 当前应用包名
 * console.log(device.cpu.cores());             // CPU 核心数
 * console.log(device.cpu.arch());              // CPU 架构（如 arm64-v8a）
 * console.log(device.cpu.maxFreqMhz());        // CPU 最高频率（MHz）
 * console.log(device.isRooted());              // 是否已 Root
 * ```
 */
object JsDevice {

    private val context: Context
        get() = InitializePvz2.context

    // ===================== 系统信息 =====================

    private fun systemInfo(): JsObject = Object("systemInfo") {
        listOf("model".js, "型号".js) eq Build.MODEL.js
        listOf("brand".js, "品牌".js) eq Build.BRAND.js
        listOf("manufacturer".js, "制造商".js) eq Build.MANUFACTURER.js
        listOf("device".js, "设备代号".js) eq Build.DEVICE.js
        listOf("product".js, "产品".js) eq Build.PRODUCT.js
        listOf("board".js, "主板".js) eq Build.BOARD.js
        listOf("hardware".js, "硬件".js) eq Build.HARDWARE.js
        listOf("host".js, "主机".js) eq Build.HOST.js
        listOf("user".js, "用户".js) eq Build.USER.js
        listOf("androidVersion".js, "安卓版本".js) eq Build.VERSION.RELEASE.js
        listOf("sdkVersion".js, "SDK版本".js) eq Build.VERSION.SDK_INT.js
        listOf("codename".js, "版本代号".js) eq Build.VERSION.CODENAME.js
        listOf("incremental".js, "版本增量".js) eq Build.VERSION.INCREMENTAL.js
        listOf("securityPatch".js, "安全补丁".js) eq runCatching { Build.VERSION.SECURITY_PATCH }.getOrDefault("").js
        listOf("bootloader".js, "引导程序".js) eq Build.BOOTLOADER.js
        listOf("display".js, "显示版本".js) eq Build.DISPLAY.js
        listOf("fingerprint".js, "指纹".js) eq Build.FINGERPRINT.js
        listOf("kernelVersion".js, "内核版本".js) eq (System.getProperty("os.version") ?: "").js
        listOf("isEmulator".js, "是否模拟器".js) eq isEmulator().js
        listOf("language".js, "语言".js) eq Locale.getDefault().toLanguageTag().js
        listOf("timezone".js, "时区".js) eq TimeZone.getDefault().id.js
    }

    private fun isEmulator(): Boolean {
        val brand = Build.BRAND.lowercase()
        val model = Build.MODEL.lowercase()
        val hardware = Build.HARDWARE.lowercase()
        val fingerprint = Build.FINGERPRINT.lowercase()
        val product = Build.PRODUCT.lowercase()
        val device = Build.DEVICE.lowercase()
        return brand.contains("generic") ||
                model.contains("sdk") ||
                model.contains("emulator") ||
                hardware.contains("goldfish") ||
                hardware.contains("ranchu") ||
                fingerprint.contains("generic") ||
                product.contains("sdk") ||
                device.contains("generic")
    }

    // ===================== 屏幕信息 =====================

    private fun getScreenMetrics(): DisplayMetrics? {
        val wm = runCatching {
            context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        }.getOrNull() ?: return null
        val metrics = DisplayMetrics()
        return runCatching {
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            metrics
        }.getOrNull()
    }

    private fun screenInfo(): JsObject = Object("screenInfo") {
        val metrics = getScreenMetrics()
        val width = metrics?.widthPixels ?: 0
        val height = metrics?.heightPixels ?: 0
        val refreshRate = runCatching {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.WINDOW_SERVICE)
                .let { it as? WindowManager }?.defaultDisplay?.refreshRate?.toDouble() ?: 0.0
        }.getOrDefault(0.0)
        listOf("width".js, "宽度".js) eq width.js
        listOf("height".js, "高度".js) eq height.js
        listOf("resolution".js, "分辨率".js) eq "${width}x${height}".js
        listOf("densityDpi".js, "屏幕密度DPI".js) eq (metrics?.densityDpi ?: 0).js
        listOf("density".js, "密度比例".js) eq (metrics?.density?.toDouble() ?: 0.0).js
        listOf("scaledDensity".js, "缩放密度".js) eq (metrics?.scaledDensity?.toDouble() ?: 0.0).js
        listOf("refreshRate".js, "刷新率".js) eq refreshRate.js
    }

    // ===================== 内存信息 =====================

    private fun memoryInfo(): JsObject = Object("memoryInfo") {
        val am = runCatching {
            context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        }.getOrNull()
        val mi = ActivityManager.MemoryInfo()
        runCatching { am?.getMemoryInfo(mi) }.getOrNull()
        listOf("total".js, "总内存".js) eq mi.totalMem.js
        listOf("available".js, "可用内存".js) eq mi.availMem.js
        listOf("lowMemory".js, "内存不足".js) eq mi.lowMemory.js
        listOf("threshold".js, "低内存阈值".js) eq mi.threshold.js
    }

    // ===================== 存储信息 =====================

    private fun storageInfo(): JsObject = Object("storageInfo") {
        val internal = runCatching {
            val sf = StatFs(context.filesDir.absolutePath)
            sf.totalBytes to sf.availableBytes
        }.getOrNull()
        val externalPath = runCatching { context.getExternalFilesDir(null)?.absolutePath }.getOrNull()
        val external = externalPath?.let {
            runCatching {
                val sf = StatFs(it)
                sf.totalBytes to sf.availableBytes
            }.getOrNull()
        }
        listOf("internalTotal".js, "内部存储总量".js) eq (internal?.first ?: -1L).js
        listOf("internalAvailable".js, "内部存储可用".js) eq (internal?.second ?: -1L).js
        if (external != null) {
            listOf("externalTotal".js, "外部存储总量".js) eq external.first.js
            listOf("externalAvailable".js, "外部存储可用".js) eq external.second.js
        } else {
            listOf("externalTotal".js, "外部存储总量".js) eq Undefined
            listOf("externalAvailable".js, "外部存储可用".js) eq Undefined
        }
    }

    // ===================== 电池信息 =====================

    private fun batteryInfo(): JsObject = Object("batteryInfo") {
        // 通过粘性广播读取最新电池快照（无需权限、无需注册）
        val intent = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()
        val bm = runCatching {
            context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        }.getOrNull()

        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            ?: bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it >= 0 }
            ?: -1
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val statusStr = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "not_charging"
            else -> "unknown"
        }
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val pluggedStr = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "ac"
            BatteryManager.BATTERY_PLUGGED_USB -> "usb"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "wireless"
            else -> "none"
        }
        val health = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val healthStr = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "over_voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "cold"
            else -> "unknown"
        }
        val tempRaw = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val temperature = if (tempRaw >= 0) tempRaw / 10.0 else -1.0
        val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val technology = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: ""

        listOf("level".js, "电量".js) eq level.js
        listOf("isCharging".js, "是否充电".js) eq isCharging.js
        listOf("status".js, "充电状态".js) eq statusStr.js
        listOf("plugged".js, "充电方式".js) eq pluggedStr.js
        listOf("health".js, "电池健康".js) eq healthStr.js
        listOf("temperature".js, "电池温度".js) eq temperature.js
        listOf("voltage".js, "电池电压".js) eq voltage.js
        listOf("technology".js, "电池技术".js) eq technology.js
    }

    // ===================== 网络信息 =====================

    private fun getLocalIp(): String = runCatching {
        NetworkInterface.getNetworkInterfaces()?.toList()?.asSequence()
            ?.flatMap { it.inetAddresses.toList() }
            ?.firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
            ?.hostAddress ?: ""
    }.getOrDefault("")

    private fun networkInfo(): JsObject = Object("networkInfo") {
        @Suppress("DEPRECATION")
        val ni = runCatching {
            (context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
                ?.activeNetworkInfo
        }.getOrNull()
        val isConnected = ni?.isConnected == true
        val type = ni?.type ?: -1
        val typeStr = when (type) {
            ConnectivityManager.TYPE_WIFI -> "wifi"
            ConnectivityManager.TYPE_ETHERNET -> "ethernet"
            ConnectivityManager.TYPE_MOBILE -> "cellular"
            -1 -> "none"
            else -> "other"
        }
        listOf("isConnected".js, "是否已连接".js) eq isConnected.js
        listOf("isWifi".js, "是否Wifi".js) eq (type == ConnectivityManager.TYPE_WIFI).js
        listOf("type".js, "网络类型".js) eq typeStr.js
        listOf("ip".js, "本机IP".js) eq getLocalIp().js
    }

    // ===================== 应用信息（当前应用） =====================

    private fun appInfo(): JsObject = Object("appInfo") {
        val ctx = context
        val pm = ctx.packageManager
        val pkg = ctx.packageName
        val pi = runCatching { pm.getPackageInfo(pkg, 0) }.getOrNull()
        val appName = runCatching { ctx.applicationInfo.loadLabel(pm).toString() }.getOrDefault("")
        val isDebuggable = (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        @Suppress("DEPRECATION")
        val versionCode = pi?.versionCode ?: -1
        listOf("packageName".js, "包名".js) eq pkg.js
        listOf("versionName".js, "版本名".js) eq (pi?.versionName ?: "").js
        listOf("versionCode".js, "版本号".js) eq versionCode.js
        listOf("appName".js, "应用名称".js) eq appName.js
        listOf("isDebuggable".js, "是否调试版".js) eq isDebuggable.js
        listOf("targetSdk".js, "目标SDK".js) eq ctx.applicationInfo.targetSdkVersion.js
    }

    // ===================== CPU 信息 =====================

    private fun readSysfsLong(path: String): Long = runCatching {
        File(path).readText().trim().toLongOrNull()
    }.getOrNull() ?: -1L

    private fun readSysfsText(path: String): String = runCatching {
        File(path).readText().trim()
    }.getOrNull() ?: ""

    private fun cpuInfo(): JsObject = Object("cpuInfo") {
        val cores = runCatching { Runtime.getRuntime().availableProcessors() }.getOrDefault(-1)
        val supportedAbis = Build.SUPPORTED_ABIS.toList()
        val arch = supportedAbis.firstOrNull() ?: ""
        val cpu0 = "/sys/devices/system/cpu/cpu0/cpufreq"
        val maxFreq = readSysfsLong("$cpu0/cpuinfo_max_freq")   // kHz
        val minFreq = readSysfsLong("$cpu0/cpuinfo_min_freq")   // kHz
        val curFreq = readSysfsLong("$cpu0/scaling_cur_freq")   // kHz
        val governor = readSysfsText("$cpu0/scaling_governor")
        listOf("cores".js, "核心数".js) eq cores.js
        listOf("arch".js, "架构".js) eq arch.js
        listOf("supportedAbis".js, "支持的ABI".js) eq supportedAbis.map { it.js }.js
        listOf("maxFreq".js, "最高频率".js) eq maxFreq.js            // kHz
        listOf("maxFreqMhz".js, "最高频率MHz".js) eq (if (maxFreq > 0) maxFreq / 1000 else -1L).js
        listOf("minFreq".js, "最低频率".js) eq minFreq.js            // kHz
        listOf("minFreqMhz".js, "最低频率MHz".js) eq (if (minFreq > 0) minFreq / 1000 else -1L).js
        listOf("currentFreq".js, "当前频率".js) eq curFreq.js        // kHz
        listOf("currentFreqMhz".js, "当前频率MHz".js) eq (if (curFreq > 0) curFreq / 1000 else -1L).js
        listOf("governor".js, "调度器".js) eq governor.js
    }

    // ===================== Root 状态 =====================

    private fun isRooted(): Boolean {
        val suPaths = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/vendor/bin/su", "/data/local/xbin/su", "/data/local/bin/su"
        )
        if (suPaths.any { runCatching { File(it).exists() }.getOrDefault(false) }) return true
        return runCatching {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val result = process.inputStream.bufferedReader().readText()
            process.waitFor()
            result.isNotBlank()
        }.getOrDefault(false)
    }

    // ===================== 全局对象 =====================

    val js = Object("device") {
        val system = Object("system") {
            listOf("info".js, "信息".js).func { systemInfo() }
            listOf("model".js, "型号".js).func { Build.MODEL.js }
            listOf("brand".js, "品牌".js).func { Build.BRAND.js }
            listOf("manufacturer".js, "制造商".js).func { Build.MANUFACTURER.js }
            listOf("device".js, "设备代号".js).func { Build.DEVICE.js }
            listOf("product".js, "产品".js).func { Build.PRODUCT.js }
            listOf("board".js, "主板".js).func { Build.BOARD.js }
            listOf("hardware".js, "硬件".js).func { Build.HARDWARE.js }
            listOf("androidVersion".js, "安卓版本".js).func { Build.VERSION.RELEASE.js }
            listOf("sdkVersion".js, "SDK版本".js).func { Build.VERSION.SDK_INT.js }
            listOf("codename".js, "版本代号".js).func { Build.VERSION.CODENAME.js }
            listOf("incremental".js, "版本增量".js).func { Build.VERSION.INCREMENTAL.js }
            listOf("securityPatch".js, "安全补丁".js).func { runCatching { Build.VERSION.SECURITY_PATCH }.getOrDefault("").js }
            listOf("bootloader".js, "引导程序".js).func { Build.BOOTLOADER.js }
            listOf("display".js, "显示版本".js).func { Build.DISPLAY.js }
            listOf("fingerprint".js, "指纹".js).func { Build.FINGERPRINT.js }
            listOf("kernelVersion".js, "内核版本".js).func { (System.getProperty("os.version") ?: "").js }
            listOf("isEmulator".js, "是否模拟器".js).func { isEmulator().js }
            listOf("language".js, "语言".js).func { Locale.getDefault().toLanguageTag().js }
            listOf("timezone".js, "时区".js).func { TimeZone.getDefault().id.js }
        }

        val screen = Object("screen") {
            listOf("info".js, "信息".js).func { screenInfo() }
            listOf("width".js, "宽度".js).func { (getScreenMetrics()?.widthPixels ?: 0).js }
            listOf("height".js, "高度".js).func { (getScreenMetrics()?.heightPixels ?: 0).js }
            listOf("resolution".js, "分辨率".js).func {
                val m = getScreenMetrics()
                val w = m?.widthPixels ?: 0
                val h = m?.heightPixels ?: 0
                "${w}x${h}".js
            }
            listOf("densityDpi".js, "屏幕密度DPI".js).func { (getScreenMetrics()?.densityDpi ?: 0).js }
            listOf("density".js, "密度比例".js).func { (getScreenMetrics()?.density?.toDouble() ?: 0.0).js }
            listOf("scaledDensity".js, "缩放密度".js).func { (getScreenMetrics()?.scaledDensity?.toDouble() ?: 0.0).js }
            listOf("refreshRate".js, "刷新率".js).func {
                runCatching {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.WINDOW_SERVICE)
                        .let { it as? WindowManager }?.defaultDisplay?.refreshRate?.toDouble() ?: 0.0
                }.getOrDefault(0.0).js
            }
        }

        val memory = Object("memory") {
            listOf("info".js, "信息".js).func { memoryInfo() }
            listOf("total".js, "总内存".js).func { runCatching {
                (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
                    ?.let { am -> ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }.totalMem } ?: -1L
            }.getOrDefault(-1L).js }
            listOf("available".js, "可用内存".js).func { runCatching {
                (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
                    ?.let { am -> ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }.availMem } ?: -1L
            }.getOrDefault(-1L).js }
            listOf("lowMemory".js, "内存不足".js).func { runCatching {
                (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
                    ?.let { am -> ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }.lowMemory } ?: false
            }.getOrDefault(false).js }
            listOf("threshold".js, "低内存阈值".js).func { runCatching {
                (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
                    ?.let { am -> ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }.threshold } ?: -1L
            }.getOrDefault(-1L).js }
        }

        val storage = Object("storage") {
            listOf("info".js, "信息".js).func { storageInfo() }
            listOf("internalTotal".js, "内部存储总量".js).func { runCatching {
                StatFs(context.filesDir.absolutePath).totalBytes
            }.getOrDefault(-1L).js }
            listOf("internalAvailable".js, "内部存储可用".js).func { runCatching {
                StatFs(context.filesDir.absolutePath).availableBytes
            }.getOrDefault(-1L).js }
            listOf("externalTotal".js, "外部存储总量".js).func { runCatching {
                context.getExternalFilesDir(null)?.absolutePath?.let { StatFs(it).totalBytes }
            }.getOrNull()?.js ?: Undefined }
            listOf("externalAvailable".js, "外部存储可用".js).func { runCatching {
                context.getExternalFilesDir(null)?.absolutePath?.let { StatFs(it).availableBytes }
            }.getOrNull()?.js ?: Undefined }
        }

        val battery = Object("battery") {
            listOf("info".js, "信息".js).func { batteryInfo() }
            listOf("level".js, "电量".js).func { runCatching {
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it >= 0 }
                    ?: context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                        ?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            }.getOrDefault(-1).js }
            listOf("isCharging".js, "是否充电".js).func { runCatching {
                val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
            }.getOrDefault(false).js }
            listOf("status".js, "充电状态".js).func { runCatching {
                val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                when (intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> "charging"
                    BatteryManager.BATTERY_STATUS_DISCHARGING -> "discharging"
                    BatteryManager.BATTERY_STATUS_FULL -> "full"
                    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "not_charging"
                    else -> "unknown"
                }
            }.getOrDefault("unknown").js }
            listOf("plugged".js, "充电方式".js).func { runCatching {
                val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                when (intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1) {
                    BatteryManager.BATTERY_PLUGGED_AC -> "ac"
                    BatteryManager.BATTERY_PLUGGED_USB -> "usb"
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "wireless"
                    else -> "none"
                }
            }.getOrDefault("none").js }
            listOf("health".js, "电池健康".js).func { runCatching {
                val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                when (intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1) {
                    BatteryManager.BATTERY_HEALTH_GOOD -> "good"
                    BatteryManager.BATTERY_HEALTH_OVERHEAT -> "overheat"
                    BatteryManager.BATTERY_HEALTH_DEAD -> "dead"
                    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "over_voltage"
                    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "failure"
                    BatteryManager.BATTERY_HEALTH_COLD -> "cold"
                    else -> "unknown"
                }
            }.getOrDefault("unknown").js }
            listOf("temperature".js, "电池温度".js).func { runCatching {
                val t = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    ?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
                if (t >= 0) t / 10.0 else -1.0
            }.getOrDefault(-1.0).js }
            listOf("voltage".js, "电池电压".js).func { runCatching {
                context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    ?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
            }.getOrDefault(-1).js }
            listOf("technology".js, "电池技术".js).func { runCatching {
                context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    ?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: ""
            }.getOrDefault("").js }
        }

        val network = Object("network") {
            listOf("info".js, "信息".js).func { networkInfo() }
            listOf("isConnected".js, "是否已连接".js).func { @Suppress("DEPRECATION") runCatching {
                (context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
                    ?.activeNetworkInfo?.isConnected == true
            }.getOrDefault(false).js }
            listOf("isWifi".js, "是否Wifi".js).func { @Suppress("DEPRECATION") runCatching {
                (context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
                    ?.activeNetworkInfo?.type == ConnectivityManager.TYPE_WIFI
            }.getOrDefault(false).js }
            listOf("type".js, "网络类型".js).func { @Suppress("DEPRECATION") runCatching {
                val type = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
                    ?.activeNetworkInfo?.type ?: -1
                when (type) {
                    ConnectivityManager.TYPE_WIFI -> "wifi"
                    ConnectivityManager.TYPE_ETHERNET -> "ethernet"
                    ConnectivityManager.TYPE_MOBILE -> "cellular"
                    -1 -> "none"
                    else -> "other"
                }
            }.getOrDefault("none").js }
            listOf("ip".js, "本机IP".js).func { getLocalIp().js }
        }

        @Suppress("DEPRECATION")
        val app = Object("app") {
            listOf("info".js, "信息".js).func { appInfo() }
            listOf("packageName".js, "包名".js).func { context.packageName.js }
            listOf("versionName".js, "版本名".js).func {
                runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "" }
                    .getOrDefault("").js
            }
            listOf("versionCode".js, "版本号".js).func {
                @Suppress("DEPRECATION")
                runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionCode ?: -1 }
                    .getOrDefault(-1).js
            }
            listOf("appName".js, "应用名称".js).func {
                runCatching { context.applicationInfo.loadLabel(context.packageManager).toString() }
                    .getOrDefault("").js
            }
            listOf("isDebuggable".js, "是否调试版".js).func {
                ((context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0).js
            }
            listOf("targetSdk".js, "目标SDK".js).func { context.applicationInfo.targetSdkVersion.js }
        }

        val cpu = Object("cpu") {
            listOf("info".js, "信息".js).func { cpuInfo() }
            listOf("cores".js, "核心数".js).func { runCatching { Runtime.getRuntime().availableProcessors() }.getOrDefault(-1).js }
            listOf("arch".js, "架构".js).func { (Build.SUPPORTED_ABIS.firstOrNull() ?: "").js }
            listOf("supportedAbis".js, "支持的ABI".js).func { Build.SUPPORTED_ABIS.map { it.js }.js }
            listOf("maxFreq".js, "最高频率".js).func { readSysfsLong("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq").js }
            listOf("maxFreqMhz".js, "最高频率MHz".js).func {
                val f = readSysfsLong("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")
                (if (f > 0) f / 1000 else -1L).js
            }
            listOf("minFreq".js, "最低频率".js).func { readSysfsLong("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq").js }
            listOf("minFreqMhz".js, "最低频率MHz".js).func {
                val f = readSysfsLong("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq")
                (if (f > 0) f / 1000 else -1L).js
            }
            listOf("currentFreq".js, "当前频率".js).func { readSysfsLong("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq").js }
            listOf("currentFreqMhz".js, "当前频率MHz".js).func {
                val f = readSysfsLong("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq")
                (if (f > 0) f / 1000 else -1L).js
            }
            listOf("governor".js, "调度器".js).func { readSysfsText("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor").js }
        }

        // 分组对象挂载到 device 下
        listOf("system".js, "系统".js) eq system
        listOf("screen".js, "屏幕".js) eq screen
        listOf("memory".js, "内存".js) eq memory
        listOf("storage".js, "存储".js) eq storage
        listOf("battery".js, "电池".js) eq battery
        listOf("network".js, "网络".js) eq network
        listOf("app".js, "应用".js) eq app
        listOf("cpu".js, "CPU".js) eq cpu

        // 是否已 Root
        listOf("isRooted".js, "是否已Root".js).func { isRooted().js }

        // 聚合：一次性返回全部设备信息
        listOf("info".js, "信息".js).func {
            Object("deviceInfo") {
                listOf("system".js, "系统".js) eq systemInfo()
                listOf("screen".js, "屏幕".js) eq screenInfo()
                listOf("memory".js, "内存".js) eq memoryInfo()
                listOf("storage".js, "存储".js) eq storageInfo()
                listOf("battery".js, "电池".js) eq batteryInfo()
                listOf("network".js, "网络".js) eq networkInfo()
                listOf("app".js, "应用".js) eq appInfo()
                listOf("cpu".js, "CPU".js) eq cpuInfo()
                listOf("isRooted".js, "是否已Root".js) eq isRooted().js
            }
        }
    }
}
