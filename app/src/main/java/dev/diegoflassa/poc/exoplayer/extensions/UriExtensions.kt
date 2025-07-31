@file:Suppress("unused")

package dev.diegoflassa.poc.exoplayer.extensions

import android.net.Uri

/**
 * Checks if the Uri points to a local Android file.
 * This includes "file://" schemes (direct file paths) and "content://" schemes (ContentProviders).
 */
fun Uri?.isLocal(): Boolean {
    return when (this?.scheme) {
        "file", "content" -> true
        else -> false
    }
}

/**
 * Checks if the Uri points to a remote URL.
 * This includes common web protocols like "http://", "https://", "ftp://", and "rtsp://".
 */
fun Uri?.isRemote(): Boolean {
    return when (this?.scheme) {
        "http", "https", "ftp", "rtsp" -> true
        else -> false
    }
}