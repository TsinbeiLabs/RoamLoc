package com.tsinbei.roamloc.xposed.hooks.miui

import android.os.Build
import android.telephony.CellIdentity
import android.telephony.CellIdentityCdma
import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellSignalStrengthCdma
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import com.tsinbei.roamloc.xposed.BaseLocationHook
import com.tsinbei.roamloc.xposed.hooks.blindhook.BlindHookLocation
import com.tsinbei.roamloc.xposed.utils.BinderUtils
import com.tsinbei.roamloc.xposed.utils.FakeLoc
import com.tsinbei.roamloc.xposed.utils.beforeHook
import com.tsinbei.roamloc.xposed.utils.onceHookAllMethod
import com.tsinbei.roamloc.xposed.utils.onceHookMethodBefore

object MiuiBlurLocationProviderHook: BaseLocationHook() {
    operator fun invoke(classLoader: ClassLoader) {
        val cMiuiBlurLocationManagerImpl = XposedHelpers.findClassIfExists("com.android.server.location.MiuiBlurLocationManagerImpl", classLoader)
        if (cMiuiBlurLocationManagerImpl != null) {
            BlindHookLocation(cMiuiBlurLocationManagerImpl, classLoader)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val hooker: XC_MethodHook.MethodHookParam.() -> Unit = {
                    if (FakeLoc.enable && !BinderUtils.isSystemAppsCall()) {
                        result = CellIdentityCdma::class.java.getConstructor(
                            Int::class.java,
                            Int::class.java,
                            Int::class.java,
                            Int::class.java,
                            Int::class.java,
                            String::class.java,
                            String::class.java
                        ).newInstance(
                            Int.MAX_VALUE,
                            Int.MAX_VALUE,
                            Int.MAX_VALUE,
                            (FakeLoc.latitude * 14400.0).toInt(),
                            (FakeLoc.longitude * 14400.0).toInt(),
                            null, null
                        )
                    }
                }
                if(cMiuiBlurLocationManagerImpl.onceHookMethodBefore("getBlurryCellLocation", CellIdentity::class.java) { hooker() } == null) {
                    cMiuiBlurLocationManagerImpl.onceHookMethodBefore("getBlurryCellLocation",
                        CellIdentity::class.java, Int::class.java, String::class.java) { hooker() }
                }
            }

            cMiuiBlurLocationManagerImpl.onceHookAllMethod("getBlurryCellInfos", beforeHook {
                if (FakeLoc.enable && !BinderUtils.isSystemAppsCall()) {
                    val cellInfos = arrayListOf<CellInfo>()
                    val cellInfo = kotlin.runCatching {
                        CellInfoCdma::class.java.getConstructor().newInstance().also {
                            XposedHelpers.callMethod(it, "setRegistered", true)
                            XposedHelpers.callMethod(it, "setTimeStamp", System.nanoTime())
                            XposedHelpers.callMethod(it, "setCellConnectionStatus", 0)
                        }
                    }.getOrElse {
                        CellInfoCdma::class.java.getConstructor(
                            Int::class.java,
                            Boolean::class.java,
                            Long::class.java,
                            CellIdentityCdma::class.java,
                            CellSignalStrengthCdma::class.java
                        ).newInstance(0, true, System.nanoTime(), CellIdentityCdma::class.java.newInstance(), CellSignalStrengthCdma::class.java.newInstance())
                    }
                    cellInfos.add(cellInfo)

                    result = cellInfos
                }
            })
        }
    }


}
