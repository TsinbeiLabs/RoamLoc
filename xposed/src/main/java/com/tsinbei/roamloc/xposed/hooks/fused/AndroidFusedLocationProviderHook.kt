package com.tsinbei.roamloc.xposed.hooks.fused

import android.location.Location
import com.tsinbei.roamloc.xposed.BaseLocationHook
import com.tsinbei.roamloc.xposed.hooks.blindhook.BlindHookLocation
import com.tsinbei.roamloc.xposed.utils.FakeLoc
import com.tsinbei.roamloc.xposed.utils.Logger
import com.tsinbei.roamloc.xposed.utils.hookMethodAfter
import com.tsinbei.roamloc.xposed.utils.toClass

object AndroidFusedLocationProviderHook: BaseLocationHook() {
    operator fun invoke(classLoader: ClassLoader) {
        val cFusedLocationProvider = "com.android.location.fused.FusedLocationProvider".toClass(classLoader)
        if (cFusedLocationProvider == null) {
            Logger.warn("Failed to find FusedLocationProvider")
            return
        }

        if(!initDivineService("AndroidFusedLocationProvider")) {
            Logger.error("Failed to init DivineService in AndroidFusedLocationProvider")
            return
        }

        cFusedLocationProvider.hookMethodAfter("chooseBestLocation", Location::class.java, Location::class.java) {
            if (result == null) return@hookMethodAfter

            if (FakeLoc.enable) {
                result = injectLocation(result as Location)
            }
        }

//        cFusedLocationProvider.hookMethodBefore("reportBestLocationLocked") {
//
//        }

        val cChildLocationListener = "com.android.location.fused.FusedLocationProvider\$ChildLocationListener".toClass(classLoader)
        if (cChildLocationListener == null) {
            Logger.warn("Failed to find ChildLocationListener")
            return
        }

        BlindHookLocation(cChildLocationListener, classLoader)
    }
}
