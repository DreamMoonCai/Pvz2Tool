import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import android.view.Display
import android.view.WindowManager
import androidx.annotation.RequiresPermission
import androidx.compose.ui.util.fastMaxBy
import com.highcapable.yukireflection.factory.allFields
import com.highcapable.yukireflection.factory.constructor
import com.highcapable.yukireflection.factory.field
import com.highcapable.yukireflection.factory.method
import com.highcapable.yukireflection.factory.toClass
import com.highcapable.yukireflection.type.java.IntType
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.ui.main.SettingsDialogState
import io.github.dreammooncai.util.ContextUtil
import io.github.dreammooncai.yukireflection.factory.returnType
import org.lsposed.hiddenapibypass.HiddenApiBypass
import org.lsposed.hiddenapibypass.LSPass
import java.io.File
import kotlin.random.Random

@Suppress("DEPRECATION")
@SuppressLint("HardwareIds")
object DreamRootPath {
    private val context by lazy { runCatching { InitializePvz2.context }.getOrNull() ?: ContextUtil.context }

    @JvmStatic
    val rootFile: File by lazy { context.filesDir }

    @JvmStatic
    val rootPath: String by lazy { rootFile.absolutePath }

    private var windowDisplay: CustomDisplay? = null

    @JvmStatic
    fun getDefaultDisplay(windowManager: WindowManager): Display {
        val df = windowManager.defaultDisplay
        val windowDisplay = windowDisplay
        var customWidth = SettingsDialogState.lastScreenSize[0]
        var customHeight = SettingsDialogState.lastScreenSize[1]
        if (customWidth == 0 || customHeight == 0) {
            val result = SettingsDialogState.calcRatioAndPadding(df.width,df.height)
            customWidth = result[0]
            customHeight = result[1]
        }
        return if (windowDisplay != null) {
            windowDisplay.updateDisplay(customWidth,customHeight)
            windowDisplay.display
        } else runCatching {
            val windowDisplay = CustomDisplay(df)
            this.windowDisplay = windowDisplay
            windowDisplay.updateDisplay(customWidth,customHeight)
            windowDisplay.display
        }.getOrDefault(df)
    }

    @JvmStatic
    @RequiresPermission("android.permission.READ_PRIVILEGED_PHONE_STATE")
    fun getDeviceId(tm: TelephonyManager): String? {
        if (Build.VERSION.SDK_INT >= 29) {
            return null
        }
        return try {
            tm.deviceId
        } catch (_: Exception) {
            null
        }
    }

    @JvmStatic
    @RequiresPermission("android.permission.READ_PRIVILEGED_PHONE_STATE")
    fun getSimSerialNumber(tm: TelephonyManager): String? {
        if (Build.VERSION.SDK_INT >= 29) {
            return null
        }
        return try {
            tm.simSerialNumber
        } catch (_: Exception) {
            null
        }
    }

    @JvmStatic
    @RequiresPermission("android.permission.READ_PRIVILEGED_PHONE_STATE")
    fun getSubscriberId(tm: TelephonyManager): String? {
        if (Build.VERSION.SDK_INT >= 29) {
            return null
        }
        return try {
            tm.subscriberId
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 手机号码
     */
    @JvmStatic
    @RequiresPermission(allOf = [Manifest.permission.READ_SMS, Manifest.permission.READ_PHONE_NUMBERS, Manifest.permission.READ_PHONE_STATE])
    fun getLine1Number(tm: TelephonyManager): String? {
        if (Build.VERSION.SDK_INT >= 29) {
            return null
        }
        return try {
            tm.line1Number
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 网络运营商名称
     */
    @JvmStatic
    @RequiresPermission("android.permission.READ_PHONE_STATE")
    fun getNetworkOperatorName(tm: TelephonyManager): String? {
        return try {
            tm.networkOperatorName
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 网络运营商编码
     */
    @JvmStatic
    @RequiresPermission("android.permission.READ_PHONE_STATE")
    fun getNetworkOperator(tm: TelephonyManager): String? {
        return try {
            tm.networkOperator
        } catch (_: Exception) {
            null
        }
    }

    /**
     * SIM卡运营商名称
     */
    @JvmStatic
    @RequiresPermission("android.permission.READ_PHONE_STATE")
    fun getSimOperatorName(tm: TelephonyManager): String? {
        return try {
            tm.simOperatorName
        } catch (_: Exception) {
            null
        }
    }

    /**
     * SIM卡运营商编码
     */
    @JvmStatic
    @RequiresPermission("android.permission.READ_PHONE_STATE")
    fun getSimOperator(tm: TelephonyManager): String? {
        return try {
            tm.simOperator
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 设备语音邮箱号码
     */
    @JvmStatic
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    fun getVoiceMailNumber(tm: TelephonyManager): String? {
        if (Build.VERSION.SDK_INT >= 29) {
            return null
        }
        return try {
            tm.voiceMailNumber
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 国家代码
     */
    @JvmStatic
    @RequiresPermission("android.permission.READ_PHONE_STATE")
    fun getNetworkCountryIso(tm: TelephonyManager): String? {
        return try {
            tm.networkCountryIso
        } catch (_: Exception) {
            null
        }
    }

    /**
     * SIM卡所属国家代码
     */
    @JvmStatic
    @RequiresPermission("android.permission.READ_PHONE_STATE")
    fun getSimCountryIso(tm: TelephonyManager): String? {
        return try {
            tm.simCountryIso
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 手机类型
     */
    @JvmStatic
    fun getPhoneType(tm: TelephonyManager): Int {
        return try {
            tm.phoneType
        } catch (_: Exception) {
            -1
        }
    }

    /**
     * 网络类型
     */
    @JvmStatic
    @RequiresPermission("android.permission.READ_PHONE_STATE")
    fun getNetworkType(tm: TelephonyManager): Int {
        return try {
            tm.networkType
        } catch (_: Exception) {
            -1
        }
    }

    /**
     * SIM卡状态
     */
    @JvmStatic
    fun getSimState(tm: TelephonyManager): Int {
        return try {
            tm.simState
        } catch (_: Exception) {
            -1
        }
    }

    /**
     * 是否漫游
     */
    @JvmStatic
    @RequiresPermission("android.permission.READ_PHONE_STATE")
    fun isNetworkRoaming(tm: TelephonyManager): Boolean {
        return try {
            tm.isNetworkRoaming
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 手机IMEI (多卡槽旧接口)
     */
    @JvmStatic
    @RequiresPermission("android.permission.READ_PRIVILEGED_PHONE_STATE")
    fun getDeviceIdBySlot(tm: TelephonyManager, slotIndex: Int): String? {
        if (Build.VERSION.SDK_INT >= 29) {
            return null
        }
        return try {
            tm.getDeviceId(slotIndex)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * MEID 码 (CDMA设备)
     */
    @Suppress("SimplifyNegatedBinaryExpression")
    @JvmStatic
    @RequiresPermission("android.permission.READ_PRIVILEGED_PHONE_STATE")
    fun getMeid(tm: TelephonyManager): String? {
        if (!(Build.VERSION.SDK_INT in 26..<29)) {
            return null
        }
        return try {
            tm.meid
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 语音邮箱标签 String
     */
    @JvmStatic
    @RequiresPermission("android.permission.READ_PHONE_STATE")
    fun getVoiceMailAlphaTag(tm: TelephonyManager): String? {
        return try {
            tm.voiceMailAlphaTag
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 基站位置 CellLocation
     */
    @JvmStatic
    @RequiresPermission("android.permission.ACCESS_FINE_LOCATION")
    fun getCellLocation(tm: TelephonyManager): android.telephony.CellLocation? {
        return try {
            tm.cellLocation
        } catch (_: Exception) {
            null
        }
    }

    @JvmStatic
    @JvmOverloads
    fun getIMSIBySlot(thiz: Any,context: Context = DreamRootPath.context,slotID: Int = 0): String? {
        return try {
            "com.popcap.SexyAppFramework.AndroidGameApp".toClass().method { name = "getIMSIBySlot" }.get(thiz).string(context,slotID)
        } catch (_: Exception) {
            null
        }
    }
}

private class CustomDisplay(
    private val originalDisplay: Display
) {
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                LSPass.setHiddenApiExemptions("Landroid/view/")
            } catch (_: Exception) {
                HiddenApiBypass.setHiddenApiExemptions("Landroid/view/")
            }
        }
    }

    private val displayClass = Display::class.java
    private val originalDisplayInfo = displayClass.field { name = "mDisplayInfo" }.get(originalDisplay).any()
    private val displayInfoClass = originalDisplayInfo?.javaClass ?: error("not impl")

    // 创建DisplayInfo的副本(因为原始对象可能被系统共享)
    private val newDisplayInfo = displayInfoClass.constructor().get().call()?.also { newDisplayInfo ->
        // 复制所有字段到新对象
        displayInfoClass.allFields { _, field ->
            field.set(newDisplayInfo, field.get(originalDisplayInfo))
        }
    }

    val display by lazy {
        val constructor = displayClass.constructor().giveAll().fastMaxBy { it.parameterTypes.size } ?: return@lazy originalDisplay
        constructor.isAccessible = true
        val param = constructor.parameterTypes.map {
            if (it == IntType) return@map Random.nextInt()
            if (it == displayInfoClass) return@map newDisplayInfo
            displayClass.field { type = it }.get(originalDisplay).any()
        }

        // 实例化新的Display对象
        val customDisplay = constructor.newInstance(
            *param.toTypedArray()
        ) as Display

        runCatching { displayClass.field { name = "mResources" }.get(customDisplay).set(null) }
        runCatching {
            displayClass.field { name = "mDisplayAdjustments" }.let { adj ->
                adj.get(customDisplay).set(adj.give()?.returnType?.constructor { emptyParam() }?.get()?.call())
            }
        }
        customDisplay
    }

    fun updateDisplay(
        customWidth: Int,
        customHeight: Int,
        calculateNominalSizes: Boolean = true
    ) {
        // 设置自定义宽高
        // logicalWidth/logicalHeight: 逻辑屏幕尺寸(用于getRealSize/getRealMetrics)
        setDisplayInfoField("logicalWidth",customWidth)
        setDisplayInfoField("logicalHeight",customHeight)

        // appWidth/appHeight: 应用可用尺寸(用于getSize/getMetrics/getWidth/getHeight)
        setDisplayInfoField("appWidth",customWidth)
        setDisplayInfoField("appHeight",customHeight)

        // ========== 新增：动态计算标称尺寸范围 ==========
        if (calculateNominalSizes) {
            // 获取原始尺寸比例
            val originalLogicalWidth = getDisplayInfoField<Int>("logicalWidth") ?: 0
            val originalLogicalHeight = getDisplayInfoField<Int>("logicalHeight") ?: 0

            if (originalLogicalWidth > 0 && originalLogicalHeight > 0) {
                // 计算缩放比例
                val widthScale = customWidth.toFloat() / originalLogicalWidth.toFloat()
                val heightScale = customHeight.toFloat() / originalLogicalHeight.toFloat()

                // 获取原始标称尺寸并按比例缩放
                val originalSmallestWidth = getDisplayInfoField<Int>("smallestNominalAppWidth") ?: 0
                setDisplayInfoField("smallestNominalAppWidth",(originalSmallestWidth * widthScale).toInt())

                val originalSmallestHeight = getDisplayInfoField<Int>("smallestNominalAppHeight") ?: 0
                setDisplayInfoField("smallestNominalAppHeight",(originalSmallestHeight * heightScale).toInt())

                val originalLargestWidth = getDisplayInfoField<Int>("largestNominalAppWidth") ?: 0
                setDisplayInfoField("largestNominalAppWidth",(originalLargestWidth * widthScale).toInt())

                val originalLargestHeight = getDisplayInfoField<Int>("largestNominalAppHeight") ?: 0
                setDisplayInfoField("largestNominalAppHeight",(originalLargestHeight * heightScale).toInt())

            } else {
                // 无法获取原始尺寸时，直接使用自定义尺寸
                setDisplayInfoField("smallestNominalAppWidth",customWidth)
                setDisplayInfoField("smallestNominalAppHeight",customHeight)
                setDisplayInfoField("largestNominalAppWidth",customWidth)
                setDisplayInfoField("largestNominalAppHeight",customHeight)
            }
        } else {
            // 强制所有标称尺寸与自定义尺寸一致
            setDisplayInfoField("smallestNominalAppWidth",customWidth)
            setDisplayInfoField("smallestNominalAppHeight",customHeight)
            setDisplayInfoField("largestNominalAppWidth",customWidth)
            setDisplayInfoField("largestNominalAppHeight",customHeight)
        }
    }

    private fun setDisplayInfoField(name: String,value: Any?) = runCatching { displayInfoClass.field { this.name = name }.get(newDisplayInfo).set(value) }

    private fun <T> getDisplayInfoField(name: String) = runCatching { displayInfoClass.field { this.name = name }.get(originalDisplayInfo).cast<T>() }.getOrNull()

}