package io.github.dreammooncai.pvz2tool.controller

import android.content.Context
import android.content.SharedPreferences
import com.petterp.floatingx.listener.IFxConfigStorage
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import io.github.dreammooncai.pvz2tool.InitializePvz2

private const val CONFIG_X = "saveX"
private const val CONFIG_Y = "saveY"

/** fx的sp存储示例*/
class FxConfigStorageToSpImpl(private val name: String,private val defX: Float = 100f, private val defY: Float = 100f) : IFxConfigStorage {

    val settings: Settings by lazy {
        SharedPreferencesSettings(InitializePvz2.context.getSharedPreferences("FxConfigStorageToSp-$name", 0))
    }

    override fun getX(): Float = settings[CONFIG_X,defX]

    override fun getY(): Float = settings[CONFIG_Y,defY]

    override fun update(x: Float, y: Float) {
        settings[CONFIG_X] = x
        settings[CONFIG_Y] = y
    }

    override fun hasConfig(): Boolean = true

    override fun clear() {
        settings.clear()
    }
}
