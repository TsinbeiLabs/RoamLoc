@file:Suppress("LocalVariableName", "PrivateApi", "UNCHECKED_CAST")
package com.tsinbei.roamloc.xposed

import com.tsinbei.roamloc.xposed.compat.XposedHelpers
import com.tsinbei.roamloc.xposed.compat.XposedRuntime
import com.tsinbei.roamloc.xposed.hooks.LocationManagerHook
import com.tsinbei.roamloc.xposed.hooks.LocationServiceHook
import com.tsinbei.roamloc.xposed.hooks.fused.AndroidFusedLocationProviderHook
import com.tsinbei.roamloc.xposed.hooks.fused.ThirdPartyLocationHook
import com.tsinbei.roamloc.xposed.hooks.oplus.OplusLocationHook
import com.tsinbei.roamloc.xposed.hooks.telephony.miui.MiuiTelephonyManagerHook
import com.tsinbei.roamloc.xposed.hooks.sensor.SystemSensorManagerHook
import com.tsinbei.roamloc.xposed.hooks.telephony.TelephonyHook
import com.tsinbei.roamloc.xposed.hooks.wlan.WlanHook
import com.tsinbei.roamloc.xposed.utils.FakeLoc
import com.tsinbei.roamloc.xposed.utils.Logger
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam

class FakeLocation : XposedModule() {
    private lateinit var cServiceManager: Class<*> // android.os.ServiceManager
    private val mServiceManagerCache by lazy {
        kotlin.runCatching { cServiceManager.getDeclaredField("sCache") }.onSuccess {
            it.isAccessible = true
        }.getOrNull()
        // the field is not guaranteed to exist
    }

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        if (!markInjected("android")) {
            return
        }

        val classLoader = param.classLoader
        Logger.info("Debug Log Status: ${FakeLoc.enableDebugLog}")
        FakeLoc.isSystemServerProcess = true
        startFakeLocHook(classLoader)
        TelephonyHook.hookSubOnTransact(classLoader)
        WlanHook(classLoader)
        AndroidFusedLocationProviderHook(classLoader)
        SystemSensorManagerHook(classLoader)
        ThirdPartyLocationHook(classLoader)
    }

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        XposedRuntime.attach(this)
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (!markInjected(param.packageName)) {
            return
        }

        when (param.packageName) {
            "com.android.phone" -> {
                Logger.info("Found com.android.phone")
                TelephonyHook(param.classLoader)
                MiuiTelephonyManagerHook(param.classLoader)
            }
            "com.android.location.fused" -> {
                AndroidFusedLocationProviderHook(param.classLoader)
            }
            "com.xiaomi.location.fused" -> {
                ThirdPartyLocationHook(param.classLoader)
            }
            "com.oplus.location" -> {
                OplusLocationHook(param.classLoader)
            }
        }
    }

    private fun markInjected(packageName: String): Boolean {
        val property = "roamloc.injected_$packageName"
        if (System.getProperty(property) == "true") {
            return false
        }
        System.setProperty(property, "true")
        return true
    }

    private fun startFakeLocHook(classLoader: ClassLoader) {
        cServiceManager = XposedHelpers.findClass("android.os.ServiceManager", classLoader)

        XposedHelpers.findClassIfExists("com.android.server.TelephonyRegistry", classLoader)?.let {
            TelephonyHook.hookTelephonyRegistry(it)
        } // for MUMU emulator

        val cLocationManager =
            XposedHelpers.findClass("android.location.LocationManager", classLoader)

        LocationServiceHook(classLoader)
        LocationManagerHook(cLocationManager)  // intrusive hooks
    }
}
