package com.tsinbei.roamloc.xposed.compat

import android.util.Log
import io.github.libxposed.api.XposedInterface

object XposedRuntime {
    @Volatile
    private var current: XposedInterface? = null

    val api: XposedInterface
        get() = current ?: error("libxposed API used before onModuleLoaded")

    fun attach(api: XposedInterface) {
        current = api
    }

    fun log(message: String, throwable: Throwable? = null) {
        api.log(Log.ERROR, "RoamLoc", message, throwable)
    }
}
