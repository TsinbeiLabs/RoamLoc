package com.tsinbei.roamloc.xposed.hooks.oplus

import android.location.LocationListener
import android.os.Bundle
import com.tsinbei.roamloc.xposed.compat.XposedHelpers
import com.tsinbei.roamloc.xposed.BaseLocationHook
import com.tsinbei.roamloc.xposed.hooks.blindhook.BlindHookLocation
import com.tsinbei.roamloc.xposed.hooks.blindhook.BlindHookLocation.invoke
import com.tsinbei.roamloc.xposed.hooks.fused.ThirdPartyLocationHook
import com.tsinbei.roamloc.xposed.utils.FakeLoc
import com.tsinbei.roamloc.xposed.utils.Logger
import com.tsinbei.roamloc.xposed.utils.hookMethodAfter
import com.tsinbei.roamloc.xposed.utils.onceHookMethodBefore
import com.tsinbei.roamloc.xposed.utils.toClass
import java.lang.reflect.Modifier

object OplusLocationHook: BaseLocationHook() {
    operator fun invoke(classLoader: ClassLoader) {
        ThirdPartyLocationHook(classLoader)
    }
}
