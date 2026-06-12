import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import io.github.dreammooncai.util.ContextUtil
import java.io.File

@Suppress("DEPRECATION")
@SuppressLint("HardwareIds")
object DreamRootPath {
    private val context by lazy { ContextUtil.context }

    @JvmStatic
    val rootFile: File by lazy { context.filesDir }

    @JvmStatic
    val rootPath: String by lazy { rootFile.absolutePath }

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
    fun getPhoneType(tm: TelephonyManager): Int? {
        return try {
            tm.phoneType
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 网络类型
     */
    @JvmStatic
    @RequiresPermission("android.permission.READ_PHONE_STATE")
    fun getNetworkType(tm: TelephonyManager): Int? {
        return try {
            tm.networkType
        } catch (_: Exception) {
            null
        }
    }

    /**
     * SIM卡状态
     */
    @JvmStatic
    fun getSimState(tm: TelephonyManager): Int? {
        return try {
            tm.simState
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 是否漫游
     */
    @JvmStatic
    @RequiresPermission("android.permission.READ_PHONE_STATE")
    fun isNetworkRoaming(tm: TelephonyManager): Boolean? {
        return try {
            tm.isNetworkRoaming
        } catch (_: Exception) {
            null
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
}