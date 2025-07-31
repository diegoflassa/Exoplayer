package dev.diegoflassa.poc.exoplayer.ui.settings

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    app: Application
) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(SettingsUIState())
    val uiState = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<SettingsEffect>()
    val effect = _effect.asSharedFlow()

    fun processIntent(intent: SettingsIntent) {
        viewModelScope.launch {
            when (intent) {
                is SettingsIntent.RefreshPermissionStatuses -> {
                    handleRefreshPermissionStatuses(intent.activity)
                }
                is SettingsIntent.RequestRelevantPermissions -> {
                    handleRequestRelevantPermissions(intent.activity)
                }
                is SettingsIntent.PermissionResults -> {
                    handlePermissionResults(intent.results)
                }
            }
        }
    }

    private fun getRequiredPermissions(): List<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> { // Android 14+
                listOf(
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> { // Android 13
                listOf(Manifest.permission.READ_MEDIA_VIDEO)
            }
            else -> { // Below Android 13
                listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    // The 'activity' parameter here is the actual Activity context passed from the UI
    private fun handleRefreshPermissionStatuses(activity: Activity) {
        val requiredPermissions = getRequiredPermissions()
        val newStatuses = mutableMapOf<String, PermissionDisplayStatus>()

        requiredPermissions.forEach { permission ->
            val isGranted = ContextCompat.checkSelfPermission(
                activity, // Using the passed Activity context
                permission
            ) == PackageManager.PERMISSION_GRANTED
            val shouldShowRationale =
                if (permission == Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) {
                    false
                } else {
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) // Using passed Activity
                }

            newStatuses[permission] = PermissionDisplayStatus(
                isGranted = isGranted,
                shouldShowRationale = !isGranted && shouldShowRationale
            )
        }
        _uiState.update { it.copy(permissionDisplayStatuses = newStatuses) }
    }

    // The 'activity' parameter here is the actual Activity context passed from the UI
    private fun handleRequestRelevantPermissions(activity: Activity) {
        val requiredPermissions = getRequiredPermissions()
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED // Using passed Activity
        }

        if (permissionsToRequest.isNotEmpty()) {
            viewModelScope.launch {
                _effect.emit(SettingsEffect.LaunchPermissionRequest(permissionsToRequest))
            }
        } else {
            handleRefreshPermissionStatuses(activity) // Using passed Activity
        }
    }

    private fun handlePermissionResults(results: Map<String, Boolean>) {
        val currentStatuses = _uiState.value.permissionDisplayStatuses.toMutableMap()
        var changed = false

        results.forEach { (permission, isGranted) ->
            val existingStatus = currentStatuses[permission]
            // For shouldShowRationale in this specific handler after results:
            // It's tricky without the activity context.
            // The current model relies on the screen calling RefreshPermissionStatuses next,
            // which will use the activity to correctly update shouldShowRationale.
            // So, here we primarily update isGranted.
            val newStatus = PermissionDisplayStatus(
                isGranted = isGranted,
                // Rationale is best determined by a full refresh using Activity context
                shouldShowRationale = !isGranted && (existingStatus?.shouldShowRationale ?: false)
            )
            if (currentStatuses[permission] != newStatus) { // Compare the whole status object
                currentStatuses[permission] = newStatus
                changed = true
            }
        }

        if (changed) {
            _uiState.update { it.copy(permissionDisplayStatuses = currentStatuses) }
        }
        // Screen should call RefreshPermissionStatuses after this.
    }
}
