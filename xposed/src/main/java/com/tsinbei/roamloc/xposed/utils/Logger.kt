package com.tsinbei.roamloc.xposed.utils

import com.tsinbei.roamloc.xposed.compat.XposedBridge

object Logger {
    private fun isEnableLog(): Boolean {
        return FakeLoc.enableLog
    }

    fun info(msg: String) {
        if (isEnableLog()) {
            XposedBridge.log("[RoamLoc] $msg")
        }
    }

    fun info(msg: String, throwable: Throwable) {
        if (isEnableLog()) {
            XposedBridge.log("[RoamLoc] $msg: ${throwable.stackTraceToString()}")
        }
    }

    fun debug(msg: String) {
        XposedBridge.log("[RoamLoc][DEBUG] $msg")
    }

    fun debug(msg: String, throwable: Throwable) {
        XposedBridge.log("[RoamLoc][DEBUG] $msg: ${throwable.stackTraceToString()}")
    }

    fun error(msg: String) {
        XposedBridge.log("[RoamLoc][ERROR] $msg")
    }

    fun error(msg: String, throwable: Throwable) {
        XposedBridge.log("[RoamLoc][ERROR] $msg: ${throwable.stackTraceToString()}")
    }

    fun warn(msg: String) {
        XposedBridge.log("[RoamLoc][WARN] $msg")
    }

    fun warn(msg: String, throwable: Throwable) {
        XposedBridge.log("[RoamLoc][WARN] $msg: ${throwable.stackTraceToString()}")
    }
}
