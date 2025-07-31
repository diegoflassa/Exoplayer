package dev.diegoflassa.poc.exoplayer.extensions

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

fun Context.modoDebugHabilitado(): Boolean = try {
    val packageManager = packageManager
    val applicationInfo = packageManager.getApplicationInfo(this.packageName, 0)
    (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
} catch (e: PackageManager.NameNotFoundException) {
    e.printStackTrace()
    false
}