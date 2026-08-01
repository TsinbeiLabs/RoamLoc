@file:Suppress("LocalVariableName", "PrivateApi", "UNCHECKED_CAST")
package com.tsinbei.roamloc.xposed

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
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

class FakeLocation: IXposedHookLoadPackage, IXposedHookZygoteInit {
    private lateinit var cServiceManager: Class<*> // android.os.ServiceManager
    private val mServiceManagerCache by lazy {
        kotlin.runCatching { cServiceManager.getDeclaredField("sCache") }.onSuccess {
            it.isAccessible = true
        }.getOrNull()
        // the field is not guaranteed to exist
    }

    /**
     * Called very early during startup of Zygote.
     * @param startupParam Details about the module itself and the started process.
     * @throws Throwable everything is caught, but will prevent further initialization of the module.
     */
    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam?) {
        if(startupParam == null) return
    }

    /**
     * This method is called when an app is loaded. It's called very early, even before
     * [Application.onCreate] is called.
     * Modules can set up their app-specific hooks here.
     *
     * @param lpparam Information about the app.
     * @throws Throwable Everything the callback throws is caught and logged.
     */
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        if (lpparam?.packageName != "android" && lpparam?.packageName != "com.android.phone") {
            return
        }

        val systemClassLoader = (kotlin.runCatching {
            lpparam.classLoader.loadClass("android.app.ActivityThread")
                ?: Class.forName("android.app.ActivityThread")
        }.onFailure {
            Logger.error("Failed to find ActivityThread", it)
        }.getOrNull() ?: return)
            .getMethod("currentActivityThread")
            .invoke(null)
            .javaClass
            .getClassLoader()

        if (systemClassLoader == null) {
            Logger.error("Failed to get system class loader")
            return
        }

        if(System.getProperty("roamloc.injected_${lpparam.packageName}") == "true") {
            return
        } else {
            System.setProperty("roamloc.injected_${lpparam.packageName}", "true")
        }

        when (lpparam.packageName) {
            "com.android.phone" -> {
                Logger.info("Found com.android.phone")
                TelephonyHook(lpparam.classLoader)
                MiuiTelephonyManagerHook(lpparam.classLoader)
            }
            "android" -> {
                Logger.info("Debug Log Status: ${FakeLoc.enableDebugLog}")
                FakeLoc.isSystemServerProcess = true
                startFakeLocHook(systemClassLoader)
                TelephonyHook.hookSubOnTransact(lpparam.classLoader)
                WlanHook(systemClassLoader)
                AndroidFusedLocationProviderHook(lpparam.classLoader)
                SystemSensorManagerHook(lpparam.classLoader)

                ThirdPartyLocationHook(lpparam.classLoader)
            }
            "com.android.location.fused" -> {
                AndroidFusedLocationProviderHook(lpparam.classLoader)
            }
            "com.xiaomi.location.fused" -> {
                ThirdPartyLocationHook(lpparam.classLoader)
            }
            "com.oplus.location" -> {
                OplusLocationHook(lpparam.classLoader)
            }
        }
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
