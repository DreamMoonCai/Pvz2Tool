package io.github.dreammooncai.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import io.github.dreammooncai.yukireflection.factory.cacheFunction
import io.github.dreammooncai.yukireflection.factory.cacheProperty
import io.github.dreammooncai.yukireflection.type.android.ActivityThreadKClass
import io.github.dreammooncai.yukireflection.type.android.ContextImplKClass
import io.github.dreammooncai.yukireflection.type.android.LoadedApkKClass
import kotlin.reflect.KClass

object ContextUtil {

    @JvmStatic
    val context: Context
        get() = try {
            val mainThread = ActivityThreadKClass.cacheFunction {
                name = "currentActivityThread"
                emptyParam()
            }.get().call() ?: throw NullPointerException("mainThread 反射值空")
            val mBoundApplication = ActivityThreadKClass.cacheProperty {
                name = "mBoundApplication"
            }.get(mainThread).any() ?: throw NullPointerException("mBoundApplication 反射值空")
            val packageInfo = mBoundApplication.javaClass.kotlin.cacheProperty {
                name = "info"
            }.get(mBoundApplication).any() ?: throw NullPointerException("packageInfo 反射值空")
            val contextImpl = ContextImplKClass.cacheFunction {
                name = "createAppContext"
                param(ActivityThreadKClass, LoadedApkKClass)
            }.get().invoke<Context>(mainThread, packageInfo)
            ContextWrapper(contextImpl)
        } catch (e: Throwable) {
            getCurrentActivity() ?: throw e
        }

    @JvmStatic
    fun getCurrentActivity(): Activity? {
        runCatching {
            val activityThread = ActivityThreadKClass.cacheFunction {
                name = "currentActivityThread"
            }.get().call()
            val activities = ActivityThreadKClass.cacheProperty {
                name = "mActivities"
            }.get(activityThread).cast<Map<*, *>>()!!
            for (activityRecord in activities.values) {
                val activityRecordClass: KClass<*> = activityRecord!!::class
                if (!activityRecordClass.cacheProperty {
                        name = "paused"
                    }.get(activityRecord).boolean()) {
                    return activityRecordClass.cacheProperty {
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

