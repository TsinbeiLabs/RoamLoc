package com.tsinbei.roamloc.xposed.utils

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Method
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val hookedMethods = mutableSetOf<Method>()
private val hookOnceLock = ReentrantLock()

/**
 * This method will only allow one hooker to add into Xposed.
 * @return Unhook object, you can use it to unhook the method.
 */
fun Method.onceHook(callback: XC_MethodHook): XC_MethodHook.Unhook? {
    return hookOnceLock.withLock {
        if (!hookedMethods.add(this)) {
            return@withLock null
        }

        try {
            XposedBridge.hookMethod(this, callback)
        } catch (throwable: Throwable) {
            hookedMethods.remove(this)
            throw throwable
        }
    }
}

/**
 * This method will only allow one hooker to add into Xposed.
 *
 * Note: The callback will only be executed before the original method.
 *
 * @return Unhook object, you can use it to unhook the method.
 */
fun Method.onceHookBefore(callback: XC_MethodHook.MethodHookParam.() -> Unit): XC_MethodHook.Unhook? {
    return onceHook(object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            kotlin.runCatching {
                param.callback()
            }.onFailure {
                XposedBridge.log(it)
            }
        }
    })
}

/**
 * This method will only allow one hooker to add into Xposed.
 * Note: the callback will only be executed after the original method.
 * @return Unhook object, you can use it to unhook the method.
 */
fun Method.onceHookAfter(callback: XC_MethodHook.MethodHookParam.() -> Unit): XC_MethodHook.Unhook? {
    return onceHook(object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            kotlin.runCatching {
                param.callback()
            }.onFailure {
                XposedBridge.log(it)
            }
        }
    })
}

/**
 * This method will only allow one hooker to add into Xposed.
 * @return set of Unhook object, you can use it to unhook the method.
 */
fun <T> Class<T>.onceHookAllMethod(methodName: String, callback: XC_MethodHook): Set<XC_MethodHook.Unhook> {
    val unhooks = mutableSetOf<XC_MethodHook.Unhook>()
    declaredMethods.forEach { method ->
        if (method.name == methodName) {
            method.onceHook(callback)?.let { unhooks.add(it) }
        }
    }
    return unhooks
}

/**
 * This method will only allow one hooker to add into Xposed.
 *
 * Note: if the method is not found, the method will return `null`.
 *
 * @return Unhook object, you can use it to unhook the method.
 */
fun <T> Class<T>.onceHookMethod(methodName: String, vararg parameterTypes: Class<*>, callback: XC_MethodHook): XC_MethodHook.Unhook? {
    return XposedHelpers.findMethodExactIfExists(this, methodName, *parameterTypes)?.onceHook(callback)
}

/**
 * This method will only allow one hooker to add into Xposed.
 *
 * Note: the callback will only be executed before the original method.
 *
 * @return Unhook object, you can use it to unhook the method.
 */
fun <T> Class<T>.onceHookMethodBefore(methodName: String, vararg parameterTypes: Class<*>, callback: XC_MethodHook.MethodHookParam.() -> Unit): XC_MethodHook.Unhook? {
    return XposedHelpers.findMethodExactIfExists(this, methodName, *parameterTypes)?.onceHookBefore(callback)
}

/**
 * This method will only allow one hooker to add into Xposed.
 * Note: the callback will only be executed after the original method.
 * @return Unhook object, you can use it to unhook the method.
 */
fun <T> Class<T>.onceHookMethodAfter(methodName: String, vararg parameterTypes: Class<*>, callback: XC_MethodHook.MethodHookParam.() -> Unit): XC_MethodHook.Unhook? {
    return XposedHelpers.findMethodExactIfExists(this, methodName, *parameterTypes)?.onceHookAfter(callback)
}

/**
 * This method will only allow one hooker to add into Xposed.
 *
 * Note: The original method will be executed when callback `shouldDoNothing` returns false.
 *
 * @return Unhook object, you can use it to unhook the method.
 */
fun <T> Class<T>.onceHookDoNothingMethod(methodName: String, vararg parameterTypes: Class<*>, shouldDoNothing: XC_MethodHook.MethodHookParam.() -> Boolean): XC_MethodHook.Unhook? {
    return onceHookMethodBefore(methodName, *parameterTypes) {
        if (kotlin.runCatching { shouldDoNothing() }.onFailure { XposedBridge.log(it) }.getOrNull() == true) {
            result = null
        }
    }
}

/**
 * This method will be hooked.
 * @return set of Unhook object, you can use it to unhook the method
 */
fun <T> Class<T>.hookAllMethods(methodName: String, callback: XC_MethodHook): Set<XC_MethodHook.Unhook> {
    return XposedBridge.hookAllMethods(this, methodName, callback)
}

/**
 * This method will be hooked,
 * but the callback will only be executed before the original method
 */
fun <T> Class<T>.hookAllMethodsBefore(methodName: String, callback: XC_MethodHook.MethodHookParam.() -> Unit): Set<XC_MethodHook.Unhook> {
    return hookAllMethods(methodName, object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            kotlin.runCatching {
                param.callback()
            }.onFailure {
                XposedBridge.log(it)
            }
        }
    })
}

/**
 * This method will be hooked,
 * but the callback will only be executed after the original method
 */
fun <T> Class<T>.hookAllMethodsAfter(methodName: String, callback: XC_MethodHook.MethodHookParam.() -> Unit): Set<XC_MethodHook.Unhook> {
    return hookAllMethods(methodName, object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            kotlin.runCatching {
                param.callback()
            }.onFailure {
                XposedBridge.log(it)
            }
        }
    })
}

/**
 * This method will be hooked.
 *
 * Note: If this method is an interface method or an abstract method, `IllegalArgumentException` will be thrown
 *
 * @return Unhook object, you can use it to unhook the method
 */
fun Method.hook(callback: XC_MethodHook): XC_MethodHook.Unhook? {
    return XposedBridge.hookMethod(this, callback)
}

/**
 * This method will be hooked,
 * but the callback will only be executed before the original method
 */
fun Method.hookBefore(callback: XC_MethodHook.MethodHookParam.() -> Unit): XC_MethodHook.Unhook? {
    return hook(object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            kotlin.runCatching {
                param.callback()
            }.onFailure {
                XposedBridge.log(it)
            }
        }
    })
}

/**
 * This method will be hooked,
 * but the callback will only be executed after the original method
 */
fun Method.hookAfter(callback: XC_MethodHook.MethodHookParam.() -> Unit): XC_MethodHook.Unhook? {
    return hook(object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            kotlin.runCatching {
                param.callback()
            }.onFailure {
                XposedBridge.log(it)
            }
        }
    })
}

/**
 * This method will be hooked.
 *
 * Note: If this method is not found, the method will return `null`
 *
 * @return Unhook object, you can use it to unhook the method
 */
fun <T> Class<T>.hookMethod(methodName: String, vararg parameterTypes: Class<*>, callback: XC_MethodHook): XC_MethodHook.Unhook? {
    return XposedHelpers.findMethodExactIfExists(this, methodName, *parameterTypes)?.hook(callback)
}

/**
 * This method will be hooked,
 * but the callback will only be executed before the original method
 */
fun <T> Class<T>.hookMethodBefore(methodName: String, vararg parameterTypes: Class<*>, callback: XC_MethodHook.MethodHookParam.() -> Unit): XC_MethodHook.Unhook? {
    return XposedHelpers.findMethodExactIfExists(this, methodName, *parameterTypes)?.hookBefore(callback)
}

/**
 * This method will be hooked,
 * but the callback will only be executed after the original method
 */
fun <T> Class<T>.hookMethodAfter(methodName: String, vararg parameterTypes: Class<*>, callback: XC_MethodHook.MethodHookParam.() -> Unit): XC_MethodHook.Unhook? {
    return XposedHelpers.findMethodExactIfExists(this, methodName, *parameterTypes)?.hookAfter(callback)
}

/**
 * This method will be hooked.
 *
 * Note: The original method will be executed when callback `shouldDoNothing` returns false
 *
 * @return Unhook object, you can use it to unhook the method
 */
fun <T> Class<T>.hookDoNothingMethod(methodName: String, vararg parameterTypes: Class<*>, shouldDoNothing: XC_MethodHook.MethodHookParam.() -> Boolean): XC_MethodHook.Unhook? {
    return hookMethodBefore(methodName, *parameterTypes) {
        if (kotlin.runCatching { shouldDoNothing() }.onFailure { XposedBridge.log(it) }.getOrNull() == true) {
            result = null
        }
    }
}

/**
 * @return XC_MethodHook object, you can use it to hook some method
 */
fun beforeHook(
    callback: XC_MethodHook.MethodHookParam.() -> Unit
): XC_MethodHook {
    return object: XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            kotlin.runCatching {
                param.callback()
            }.onFailure {
                XposedBridge.log(it)
            }
        }
    }
}

/**
 * @return XC_MethodHook object, you can use it to hook some method
 */
fun afterHook(
    callback: XC_MethodHook.MethodHookParam.() -> Unit
): XC_MethodHook {
    return object: XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            kotlin.runCatching {
                param.callback()
            }.onFailure {
                XposedBridge.log(it)
            }
        }
    }
}

/**
 * Calls a method on the given object instance.
 *
 * @param methodName The name of the method to call.
 * @param args The arguments to pass to the method.
 * @return The result of the method call.
 */
fun <T> T.callMethod(methodName: String, vararg args: Any?): Any? {
    return XposedHelpers.callMethod(this, methodName, *args)
}

/**
 * Calls a static method on the given class.
 *
 * @param methodName The name of the method to call.
 * @param args The arguments to pass to the method.
 * @return The result of the method call.
 */
fun <T> Class<T>.callStaticMethod(methodName: String, vararg args: Any?): Any? {
    return XposedHelpers.callStaticMethod(this, methodName, *args)
}

/**
 * Finds a class by its name if it exists.
 *
 * @receiver The name of the class to find.
 * @param classLoader The class loader to use for finding the class.
 * @return The class if it exists, or `null` if it does not.
 */
fun String.toClass(classLoader: ClassLoader?): Class<*>? {
    return XposedHelpers.findClassIfExists(this, classLoader)
}

/**
 * Finds a class by its name.
 *
 * @receiver The name of the class to find.
 * @param classLoader The class loader to use for finding the class.
 * @return The class.
 * @throws ClassNotFoundError If the class does not exist.
 */
fun String.toClassOrThrow(classLoader: ClassLoader?): Class<*> {
    return XposedHelpers.findClass(this, classLoader)
}

/**
 * Hooks a method with optional before and after callbacks.
 *
 * @param hookOnce If true, the method will only be hooked once. ('unhook' immediately after triggering a full callback)
 * @param soleHook If true, the method will only allow one hooker to add into Xposed.
 * @param after Callback to be executed after the method is called.
 * @param before Callback to be executed before the method is called.
 *          If the callback returns true and `hookOnce` is true, the method will be unhooked.
 * @return Unhook object, you can use it to unhook the method.
 */
fun Method.diyHook(
    hookOnce: Boolean = false,
    soleHook: Boolean = false,
    before: XC_MethodHook.MethodHookParam.() -> Boolean = { false },
    after: XC_MethodHook.MethodHookParam.() -> Unit = {},
): XC_MethodHook.Unhook? {
    var unhook: XC_MethodHook.Unhook? = null
    val unhookCallback = {
        if (soleHook) {
            hookOnceLock.withLock {
                hookedMethods.remove(this)
            }
        }
    }
    val baseHooker = {
        unhook = XposedBridge.hookMethod(this, object: XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                kotlin.runCatching {
                    if (before(param)) {
                        unhook?.unhook()
                        unhookCallback()
                    }
                }.onFailure {
                    XposedBridge.log(it)
                }
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                kotlin.runCatching {
                    after(param)
                    if (hookOnce) {
                        unhook?.unhook()
                        unhookCallback()
                    }
                }.onFailure {
                    XposedBridge.log(it)
                }
            }
        })
    }
    if (soleHook) {
        hookOnceLock.withLock {
            if (hookedMethods.add(this)) {
                try {
                    baseHooker()
                } catch (throwable: Throwable) {
                    hookedMethods.remove(this)
                    throw throwable
                }
            }
        }
    } else baseHooker()
    return unhook
}
