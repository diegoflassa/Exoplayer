package dev.diegoflassa.poc.exoplayer.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import dev.diegoflassa.poc.exoplayer.navigation.NavigationViewModel
import dev.diegoflassa.poc.exoplayer.ui.hiltActivityViewModel
import dev.diegoflassa.poc.exoplayer.ui.theme.ExoPlayerTheme
import kotlinx.coroutines.flow.collectLatest
import dev.diegoflassa.poc.exoplayer.R
import dev.diegoflassa.poc.exoplayer.ui.settings.widgets.PermissionItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navigationViewModel: NavigationViewModel? = hiltActivityViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResultMap ->
        settingsViewModel.processIntent(SettingsIntent.PermissionResults(permissionsResultMap))
        if (activity != null) {
            settingsViewModel.processIntent(SettingsIntent.RefreshPermissionStatuses(activity))
        }
    }

    LaunchedEffect(key1 = activity) {
        if (activity != null) {
            settingsViewModel.processIntent(SettingsIntent.RefreshPermissionStatuses(activity))
        }
        settingsViewModel.effect.collectLatest { effect ->
            when (effect) {
                is SettingsEffect.LaunchPermissionRequest -> {
                    permissionLauncher.launch(effect.permissions.toTypedArray())
                }
                is SettingsEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { navigationViewModel?.goBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.settings_cd_navigate_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        SettingsScreenContent(
            paddingValues = paddingValues,
            uiState = uiState,
            onOpenAppSettings = {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).also { intent ->
                    intent.data = Uri.fromParts("package", context.packageName, null)
                    context.startActivity(intent)
                }
            },
            onInitialStatusLoadIfEmpty = {
                if (activity != null) {
                    settingsViewModel.processIntent(SettingsIntent.RefreshPermissionStatuses(activity))
                }
            },
            onShowVisualUserSelectedInfo = {
                Toast.makeText(
                    context,
                    "Persistent access to selected media is granted via the Photo Picker.",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }
}

@Composable
fun SettingsScreenContent(
    paddingValues: PaddingValues,
    uiState: SettingsUIState,
    onOpenAppSettings: () -> Unit,
    onInitialStatusLoadIfEmpty: () -> Unit,
    onShowVisualUserSelectedInfo: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(key1 = Unit) {
        if (uiState.permissionDisplayStatuses.isEmpty()) {
            onInitialStatusLoadIfEmpty()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(id = R.string.settings_permissions_header),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Media Access",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This app uses the Android Photo Picker to let you choose local video files. " +
                                "This is a secure way to access your media without granting broad access to your entire library.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val visualUserSelectedPermission = Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            val status = uiState.permissionDisplayStatuses[visualUserSelectedPermission]
                ?: PermissionDisplayStatus(
                    isGranted = ContextCompat.checkSelfPermission(context, visualUserSelectedPermission) == PackageManager.PERMISSION_GRANTED,
                    shouldShowRationale = false
                )

            item {
                PermissionItem(
                    permissionName = getPermissionFriendlyNameSettings(visualUserSelectedPermission),
                    permissionDescription = getPermissionDescriptionSettings(visualUserSelectedPermission),
                    status = status,
                    onRequestPermission = {
                        onShowVisualUserSelectedInfo()
                    },
                    onOpenAppSettings = { onOpenAppSettings() }
                )
            }
            if (status.shouldShowRationale && !status.isGranted) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Note: Specific Media Access",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = getPermissionRationaleSettings(visualUserSelectedPermission),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onOpenAppSettings() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open App Settings")
            }
        }
    }
}

// Updated helper functions
fun getPermissionFriendlyNameSettings(permission: String): String {
    return when (permission) {
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED -> "Access to Selected Media"
        Manifest.permission.READ_MEDIA_VIDEO -> "Access to Videos (Legacy)"
        Manifest.permission.READ_EXTERNAL_STORAGE -> "Access to Storage (Legacy)"
        else -> "Unknown Permission"
    }
}

fun getPermissionDescriptionSettings(permission: String): String {
    return when (permission) {
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED -> "Allows the app to remember and access specific photos or videos you select via the Photo Picker for future use."
        Manifest.permission.READ_MEDIA_VIDEO -> "Needed to select and play local video files from your device."
        Manifest.permission.READ_EXTERNAL_STORAGE -> "Needed to select and play local video files from your device (for older Android versions)."
        else -> "No description available."
    }
}

fun getPermissionRationaleSettings(permission: String): String {
    return when (permission) {
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED -> "To provide persistent access to media you've previously selected, this app uses the 'READ_MEDIA_VISUAL_USER_SELECTED' permission. You can manage this access in the system's app settings. Granting this allows the app to directly re-access items you've picked without asking each time."
        Manifest.permission.READ_MEDIA_VIDEO -> "This app requires access to your video files to allow you to select and play them. Please grant this permission to use the video playback features."
        Manifest.permission.READ_EXTERNAL_STORAGE -> "For older Android versions, this app requires access to your storage to find and play video files. Please grant this permission."
        else -> "This permission is important for the app's functionality."
    }
}

// --- Updated Previews ---

@Preview(showBackground = true, name = "Settings Screen - Photo Picker Info", group = "Settings Content")
@Composable
fun SettingsScreenContentPreview_PhotoPicker() {
    ExoPlayerTheme {
        Scaffold { paddingValues ->
            SettingsScreenContent(
                paddingValues = paddingValues,
                uiState = SettingsUIState(
                    permissionDisplayStatuses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        mapOf(
                            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED to PermissionDisplayStatus(isGranted = true, shouldShowRationale = false)
                        )
                    } else {
                        emptyMap()
                    }
                ),
                onOpenAppSettings = {},
                onInitialStatusLoadIfEmpty = {},
                onShowVisualUserSelectedInfo = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Settings Screen - Visual User Selected (Denied)", group = "Settings Content")
@Composable
fun SettingsScreenContentPreview_VisualUserSelectedDenied() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        Text("Preview for Android 14+ (READ_MEDIA_VISUAL_USER_SELECTED)")
        return
    }
    ExoPlayerTheme {
        Scaffold { paddingValues ->
            SettingsScreenContent(
                paddingValues = paddingValues,
                uiState = SettingsUIState(
                    permissionDisplayStatuses = mapOf(
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED to PermissionDisplayStatus(isGranted = false, shouldShowRationale = false)
                    )
                ),
                onOpenAppSettings = {},
                onInitialStatusLoadIfEmpty = {},
                onShowVisualUserSelectedInfo = {}
            )
        }
    }
}
