package com.tsinbei.roamloc.xposed.compat

import java.lang.reflect.Field
import java.lang.reflect.Method

object XposedHelpers {
    fun findClass(name: String, classLoader: ClassLoader?): Class<*> =
        Class.forName(name, false, classLoader)

    fun findClassIfExists(name: String, classLoader: ClassLoader?): Class<*>? =
        try {
            findClass(name, classLoader)
        } catch (_: ClassNotFoundException) {
            null
        }

    fun findFieldIfExists(clazz: Class<*>, name: String): Field? {
        for (current in generateSequence(clazz) { it.superclass }) {
            try {
                return current.getDeclaredField(name).apply { isAccessible = true }
            } catch (_: NoSuchFieldException) {
                // Continue with the superclass.
            }
        }
        return null
    }

    fun findMethodExactIfExists(
        clazz: Class<*>,
        name: String,
        vararg parameterTypes: Class<*>,
    ): Method? = try {
        clazz.getDeclaredMethod(name, *parameterTypes).apply { isAccessible = true }
    } catch (_: NoSuchMethodException) {
        null
    }

    fun findMethodBestMatch(clazz: Class<*>, name: String, vararg args: Any?): Method {
        return allMethods(clazz)
            .filter { it.name == name && it.parameterCount == args.size }
            .mapNotNull { method -> matchScore(method.parameterTypes, args)?.let { method to it } }
            .minByOrNull { it.second }
            ?.first
            ?.apply { isAccessible = true }
            ?: throw NoSuchMethodException("$clazz#$name(${args.size} args)")
    }

    fun findAndHookMethod(
        clazz: Class<*>,
        name: String,
        vararg parameterTypesAndCallback: Any,
    ): XC_MethodHook.Unhook {
        val callback = parameterTypesAndCallback.last() as XC_MethodHook
        val parameterTypes = parameterTypesAndCallback.dropLast(1).map { it as Class<*> }.toTypedArray()
        val method = findMethodExactIfExists(clazz, name, *parameterTypes)
            ?: throw NoSuchMethodException("$clazz#$name")
        return XposedBridge.hookMethod(method, callback)
    }

    fun callMethod(receiver: Any, name: String, vararg args: Any?): Any? =
        findMethodBestMatch(receiver.javaClass, name, *args).invoke(receiver, *args)

    fun callStaticMethod(clazz: Class<*>, name: String, vararg args: Any?): Any? =
        findMethodBestMatch(clazz, name, *args).invoke(null, *args)

    fun getStaticObjectField(clazz: Class<*>, name: String): Any? =
        requireNotNull(findFieldIfExists(clazz, name)).get(null)

    fun getStaticIntField(clazz: Class<*>, name: String): Int =
        requireNotNull(findFieldIfExists(clazz, name)).getInt(null)

    fun setIntField(receiver: Any, name: String, value: Int) {
        requireNotNull(findFieldIfExists(receiver.javaClass, name)).setInt(receiver, value)
    }

    private fun allMethods(clazz: Class<*>): Sequence<Method> =
        generateSequence(clazz) { it.superclass }.flatMap { it.declaredMethods.asSequence() }

    private fun matchScore(parameterTypes: Array<Class<*>>, args: Array<out Any?>): Int? {
        var score = 0
        parameterTypes.forEachIndexed { index, parameterType ->
            val argument = args[index]
            if (argument == null) {
                if (parameterType.isPrimitive) return null
                score += 4
            } else {
                val boxed = parameterType.boxed()
                if (!boxed.isAssignableFrom(argument.javaClass)) return null
                if (boxed != argument.javaClass) score += 1
            }
        }
        return score
    }

    private fun Class<*>.boxed(): Class<*> = when (this) {
        java.lang.Boolean.TYPE -> java.lang.Boolean::class.java
        java.lang.Byte.TYPE -> java.lang.Byte::class.java
        java.lang.Character.TYPE -> java.lang.Character::class.java
        java.lang.Short.TYPE -> java.lang.Short::class.java
        java.lang.Integer.TYPE -> java.lang.Integer::class.java
        java.lang.Long.TYPE -> java.lang.Long::class.java
        java.lang.Float.TYPE -> java.lang.Float::class.java
        java.lang.Double.TYPE -> java.lang.Double::class.java
        java.lang.Void.TYPE -> java.lang.Void::class.java
        else -> this
    }
}
