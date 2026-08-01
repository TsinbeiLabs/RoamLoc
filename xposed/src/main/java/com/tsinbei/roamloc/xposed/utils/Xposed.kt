package com.tsinbei.roamloc.xposed.utils

import com.tsinbei.roamloc.xposed.compat.XC_MethodHook
import com.tsinbei.roamloc.xposed.compat.XposedBridge
import com.tsinbei.roamloc.xposed.compat.XposedHelpers
import java.lang.reflect.Method
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val hookedMethods = mutableSetOf<Method>()
private val hookOnceLock = ReentrantLock()

fun Method.onceHook(callback: XC_MethodHook): XC_MethodHook.Unhook? = hookOnceLock.withLock {
    if (!hookedMethods.add(this)) return@withLock null
    try {
        XposedBridge.hookMethod(this, callback)
    } catch (throwable: Throwable) {
        hookedMethods.remove(this)
        throw throwable
    }
}

fun Method.onceHookBefore(callback: XC_MethodHook.MethodHookParam.() -> Unit) =
    onceHook(beforeHook(callback))

fun Method.onceHookAfter(callback: XC_MethodHook.MethodHookParam.() -> Unit) =
    onceHook(afterHook(callback))

fun <T> Class<T>.onceHookAllMethod(
    methodName: String,
    callback: XC_MethodHook,
): Set<XC_MethodHook.Unhook> = declaredMethods
    .asSequence()
    .filter { it.name == methodName }
    .mapNotNullTo(linkedSetOf()) { it.onceHook(callback) }

fun <T> Class<T>.onceHookMethod(
    methodName: String,
    vararg parameterTypes: Class<*>,
    callback: XC_MethodHook,
) = XposedHelpers.findMethodExactIfExists(this, methodName, *parameterTypes)?.onceHook(callback)

fun <T> Class<T>.onceHookMethodBefore(
    methodName: String,
    vararg parameterTypes: Class<*>,
    callback: XC_MethodHook.MethodHookParam.() -> Unit,
) = XposedHelpers.findMethodExactIfExists(this, methodName, *parameterTypes)?.onceHookBefore(callback)

fun <T> Class<T>.onceHookMethodAfter(
    methodName: String,
    vararg parameterTypes: Class<*>,
    callback: XC_MethodHook.MethodHookParam.() -> Unit,
) = XposedHelpers.findMethodExactIfExists(this, methodName, *parameterTypes)?.onceHookAfter(callback)

fun <T> Class<T>.onceHookDoNothingMethod(
    methodName: String,
    vararg parameterTypes: Class<*>,
    shouldDoNothing: XC_MethodHook.MethodHookParam.() -> Boolean,
) = onceHookMethodBefore(methodName, *parameterTypes) {
    if (runCatching { shouldDoNothing() }.onFailure(XposedBridge::log).getOrNull() == true) result = null
}

fun <T> Class<T>.hookAllMethods(methodName: String, callback: XC_MethodHook) =
    XposedBridge.hookAllMethods(this, methodName, callback)

fun <T> Class<T>.hookAllMethodsBefore(
    methodName: String,
    callback: XC_MethodHook.MethodHookParam.() -> Unit,
) = hookAllMethods(methodName, beforeHook(callback))

fun <T> Class<T>.hookAllMethodsAfter(
    methodName: String,
    callback: XC_MethodHook.MethodHookParam.() -> Unit,
) = hookAllMethods(methodName, afterHook(callback))

fun Method.hook(callback: XC_MethodHook) = XposedBridge.hookMethod(this, callback)

fun Method.hookBefore(callback: XC_MethodHook.MethodHookParam.() -> Unit) = hook(beforeHook(callback))

fun Method.hookAfter(callback: XC_MethodHook.MethodHookParam.() -> Unit) = hook(afterHook(callback))

fun <T> Class<T>.hookMethod(
    methodName: String,
    vararg parameterTypes: Class<*>,
    callback: XC_MethodHook,
) = XposedHelpers.findMethodExactIfExists(this, methodName, *parameterTypes)?.hook(callback)

fun <T> Class<T>.hookMethodBefore(
    methodName: String,
    vararg parameterTypes: Class<*>,
    callback: XC_MethodHook.MethodHookParam.() -> Unit,
) = XposedHelpers.findMethodExactIfExists(this, methodName, *parameterTypes)?.hookBefore(callback)

fun <T> Class<T>.hookMethodAfter(
    methodName: String,
    vararg parameterTypes: Class<*>,
    callback: XC_MethodHook.MethodHookParam.() -> Unit,
) = XposedHelpers.findMethodExactIfExists(this, methodName, *parameterTypes)?.hookAfter(callback)

fun <T> Class<T>.hookDoNothingMethod(
    methodName: String,
    vararg parameterTypes: Class<*>,
    shouldDoNothing: XC_MethodHook.MethodHookParam.() -> Boolean,
) = hookMethodBefore(methodName, *parameterTypes) {
    if (runCatching { shouldDoNothing() }.onFailure(XposedBridge::log).getOrNull() == true) result = null
}

fun beforeHook(callback: XC_MethodHook.MethodHookParam.() -> Unit): XC_MethodHook =
    object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) = callback(param)
    }

fun afterHook(callback: XC_MethodHook.MethodHookParam.() -> Unit): XC_MethodHook =
    object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) = callback(param)
    }

fun <T> T.callMethod(methodName: String, vararg args: Any?) =
    XposedHelpers.callMethod(this as Any, methodName, *args)

fun <T> Class<T>.callStaticMethod(methodName: String, vararg args: Any?) =
    XposedHelpers.callStaticMethod(this, methodName, *args)

fun String.toClass(classLoader: ClassLoader?) = XposedHelpers.findClassIfExists(this, classLoader)

fun String.toClassOrThrow(classLoader: ClassLoader?) = XposedHelpers.findClass(this, classLoader)

fun Method.diyHook(
    hookOnce: Boolean = false,
    soleHook: Boolean = false,
    before: XC_MethodHook.MethodHookParam.() -> Boolean = { false },
    after: XC_MethodHook.MethodHookParam.() -> Unit = {},
): XC_MethodHook.Unhook? {
    if (soleHook && !hookOnceLock.withLock { hookedMethods.add(this) }) return null

    var unhook: XC_MethodHook.Unhook? = null
    return try {
        unhook = hook(object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (runCatching { before(param) }.onFailure(XposedBridge::log).getOrDefault(false)) {
                    unhook?.unhook()
                    if (soleHook) hookOnceLock.withLock { hookedMethods.remove(this@diyHook) }
                }
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                runCatching { after(param) }.onFailure(XposedBridge::log)
                if (hookOnce) {
                    unhook?.unhook()
                    if (soleHook) hookOnceLock.withLock { hookedMethods.remove(this@diyHook) }
                }
            }
        })
        unhook
    } catch (throwable: Throwable) {
        if (soleHook) hookOnceLock.withLock { hookedMethods.remove(this) }
        throw throwable
    }
}
