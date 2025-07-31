package dev.diegoflassa.poc.exoplayer.ui.settings

/**
 * Represents one-time side effects triggered by the ViewModel, to be handled by the View.
 */
sealed interface SettingsEffect {
    data class LaunchPermissionRequest(val permissions: List<String>) : SettingsEffect
    data class ShowToast(val message: String) : SettingsEffect
}