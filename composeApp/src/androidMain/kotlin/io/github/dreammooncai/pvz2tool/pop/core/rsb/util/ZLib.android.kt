package io.github.dreammooncai.pvz2tool.pop.core.rsb.util

import android.os.Build

internal actual fun isSupportZlibBuffer(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM