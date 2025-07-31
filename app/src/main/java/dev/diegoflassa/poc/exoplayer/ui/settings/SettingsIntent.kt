package dev.diegoflassa.poc.exoplayer.ui.settings

import android.app.Activity

/**
 * Represents user actions or events that can modify the state or trigger effects.
 */
sealed interface SettingsIntent {
    data class RefreshPermissionStatuses(val activity: Activity) : SettingsIntent
    data class RequestRelevantPermissions(val activity: Activity) : SettingsIntent
    data class PermissionResults(val results: Map<String, Boolean>) : SettingsIntent
}