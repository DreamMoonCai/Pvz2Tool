package io.github.dreammooncai.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.highcapable.yukireflection.factory.field
import com.highcapable.yukireflection.factory.method
import com.highcapable.yukireflection.type.android.ActivityThreadClass
import com.highcapable.yukireflection.type.android.ContextImplClass
import com.highcapable.yukireflection.type.android.LoadedApkClass
import kotlin.reflect.KClass

object ContextUtil {

    @JvmStatic
    val context: Context
        get() = try {
            val mainThread = ActivityThreadClass.method {
                name = "currentActivityThread"
                emptyParam()
            }.get().call() ?: throw NullPointerException("mainThread 反射值空")
            val mBoundApplication = ActivityThreadClass.field {
                name = "mBoundApplication"
            }.get(mainThread).any() ?: throw NullPointerException("mBoundApplication 反射值空")
            val packageInfo = mBoundApplication.javaClass.field {
                name = "info"
            }.get(mBoundApplication).any() ?: throw NullPointerException("packageInfo 反射值空")
            val contextImpl = ContextImplClass.method {
                name = "createAppContext"
                param(ActivityThreadClass, LoadedApkClass)
            }.get().invoke<Context>(mainThread, packageInfo)
            ContextWrapper(contextImpl)
        } catch (e: Throwable) {
            getCurrentActivity() ?: throw e
        }

    @JvmStatic
    fun getCurrentActivity(): Activity? {
        runCatching {
            val activityThread = ActivityThreadClass.method {
                name = "currentActivityThread"
            }.get().call()
            val activities = ActivityThreadClass.field {
                name = "mActivities"
            }.get(activityThread).cast<Map<*, *>>()!!
            for (activityRecord in activities.values) {
                val activityRecordClass: Class<*> = activityRecord!!::class.java
                if (!activityRecordClass.field {
                        name = "paused"
                    }.get(activityRecord).boolean()) {
                    return activityRecordClass.field {
                        name = "activity"
                    }.get(activityRecord).cast<Activity>()
                }
            }
        }
        return null
    }

    @JvmStatic
    val Context.sourceDir get() = applicationInfo?.sourceDir
}

