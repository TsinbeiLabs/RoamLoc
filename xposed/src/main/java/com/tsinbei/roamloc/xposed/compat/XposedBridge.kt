package com.tsinbei.roamloc.xposed.compat

import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Method

object XposedBridge {
    fun hookMethod(executable: Executable, callback: XC_MethodHook): XC_MethodHook.Unhook {
        val handle = XposedRuntime.api.hook(executable)
            .setExceptionMode(XposedInterface.ExceptionMode.PASSTHROUGH)
            .intercept { chain ->
                val param = XC_MethodHook.MethodHookParam(
                    chain.executable,
                    chain.thisObject,
                    chain.args,
                )

                runCatching { callback.dispatchBefore(param) }
                    .onFailure {
                        param.resetAfterBeforeFailure()
                        log(it)
                    }

                if (param.isReturnEarly) {
                    param.throwable?.let(param::setAfterThrowable)
                        ?: param.setAfterResult(param.result)
                } else {
                    try {
                        param.setAfterResult(chain.proceed(param.args))
                    } catch (throwable: Throwable) {
                        param.setAfterThrowable(throwable)
                    }
                }

                val resultBeforeAfter = param.result
                val throwableBeforeAfter = param.throwable
                runCatching { callback.dispatchAfter(param) }
                    .onFailure {
                        param.restoreAfterFailure(resultBeforeAfter, throwableBeforeAfter)
                        log(it)
                    }

                param.throwable?.let { throw it }
                param.result
            }
        return XC_MethodHook.Unhook(handle::unhook)
    }

    fun hookAllMethods(
        clazz: Class<*>,
        methodName: String,
        callback: XC_MethodHook,
    ): Set<XC_MethodHook.Unhook> = clazz.declaredMethods
        .asSequence()
        .filter { it.name == methodName }
        .mapTo(linkedSetOf()) { hookMethod(it, callback) }

    fun hookAllConstructors(
        clazz: Class<*>,
        callback: XC_MethodHook,
    ): Set<XC_MethodHook.Unhook> = clazz.declaredConstructors
        .mapTo(linkedSetOf()) { hookMethod(it, callback) }

    fun invokeOriginalMethod(method: Method, thisObject: Any?, args: Array<out Any?>): Any? =
        XposedRuntime.api.getInvoker(method)
            .setType(XposedInterface.Invoker.Type.ORIGIN)
            .invoke(thisObject, *args)

    fun log(message: String) = XposedRuntime.log(message)

    fun log(throwable: Throwable) =
        XposedRuntime.log(throwable.message ?: throwable.javaClass.name, throwable)
}
