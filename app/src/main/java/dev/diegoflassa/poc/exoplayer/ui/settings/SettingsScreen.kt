package dev.diegoflassa.poc.exoplayer.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration // Keep this import
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.diegoflassa.poc.exoplayer.navigation.NavigationViewModel
import dev.diegoflassa.poc.exoplayer.ui.hiltActivityViewModel
import dev.diegoflassa.poc.exoplayer.ui.theme.ExoPlayerTheme
import kotlinx.coroutines.flow.collectLatest
import dev.diegoflassa.poc.exoplayer.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navigationViewModel: NavigationViewModel? = hiltActivityViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    val relevantPermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

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
            relevantPermission = relevantPermission,
            onPermissionItemRequest = { permission ->
                if (activity != null) {
                    settingsViewModel.processIntent(
                        SettingsIntent.RequestRelevantPermissions(
                            activity
                        )
                    )
                }
            },
            onPermissionItemOpenSettings = {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).also { intent ->
                    intent.data = Uri.fromParts("package", context.packageName, null)
                    context.startActivity(intent)
                }
            },
            onInitialStatusLoadIfEmpty = {
                if (activity != null) {
                    settingsViewModel.processIntent(
                        SettingsIntent.RefreshPermissionStatuses(
                            activity
                        )
                    )
                }
            }
        )
    }
}

@Composable
fun SettingsScreenContent(
    paddingValues: PaddingValues,
    uiState: SettingsUIState,
    relevantPermission: String,
    onPermissionItemRequest: (permission: String) -> Unit,
    onPermissionItemOpenSettings: () -> Unit,
    onInitialStatusLoadIfEmpty: () -> Unit
) {
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

        if (uiState.permissionDisplayStatuses.isEmpty()) {
            item {
                CircularProgressIndicator()
                Text("Loading permission status...", modifier = Modifier.padding(top = 8.dp))
            }
        }

        items(uiState.permissionDisplayStatuses.entries.toList()) { (permission, status) ->
            if (permission == relevantPermission) {
                PermissionItem(
                    permissionName = getPermissionFriendlyNameSettings(permission),
                    permissionDescription = getPermissionDescriptionSettings(permission),
                    status = status,
                    onRequestPermission = { onPermissionItemRequest(permission) },
                    onOpenAppSettings = { onPermissionItemOpenSettings() }
                )
            }
        }

        if (uiState.permissionDisplayStatuses.containsKey(relevantPermission) &&
            uiState.permissionDisplayStatuses[relevantPermission]?.shouldShowRationale == true &&
            uiState.permissionDisplayStatuses[relevantPermission]?.isGranted == false
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(id = R.string.settings_permissions_rationale_title),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = getPermissionRationaleSettings(relevantPermission),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

// Updated PermissionItem to accept onOpenAppSettings
@Composable
fun PermissionItem(
    permissionName: String,
    permissionDescription: String,
    status: PermissionDisplayStatus?,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit // Added parameter
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = permissionName, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = permissionDescription, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(12.dp))

            if (status == null) {
                Text(stringResource(R.string.settings_permissions_status_loading))
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (status.isGranted) stringResource(R.string.settings_permissions_status_granted)
                        else stringResource(R.string.settings_permissions_status_denied),
                        color = if (status.isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    if (!status.isGranted) {
                        Button(
                            onClick = {
                                if (!status.shouldShowRationale) {
                                    onOpenAppSettings() // Now correctly calls the passed lambda
                                } else {
                                    onRequestPermission()
                                }
                            }
                        ) {
                            Text(
                                if (!status.shouldShowRationale && !status.isGranted) stringResource(
                                    R.string.settings_permissions_button_open_settings
                                )
                                else stringResource(R.string.settings_permissions_button_grant)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper functions - ensure these return appropriate strings for READ_MEDIA_VIDEO
fun getPermissionFriendlyNameSettings(permission: String): String {
    return when (permission) {
        Manifest.permission.READ_MEDIA_VIDEO -> "Access to Videos"
        Manifest.permission.READ_EXTERNAL_STORAGE -> "Access to Storage (Videos)"
        // Add other permissions if needed by your app, though this screen now focuses on media
        else -> "Unknown Permission"
    }
}

fun getPermissionDescriptionSettings(permission: String): String {
    return when (permission) {
        Manifest.permission.READ_MEDIA_VIDEO -> "Needed to select and play local video files from your device."
        Manifest.permission.READ_EXTERNAL_STORAGE -> "Needed to select and play local video files from your device (for older Android versions)."
        else -> "No description available."
    }
}

fun getPermissionRationaleSettings(permission: String): String {
    return when (permission) {
        Manifest.permission.READ_MEDIA_VIDEO -> "This app requires access to your video files to allow you to select and play them. Please grant this permission to use the video playback features."
        Manifest.permission.READ_EXTERNAL_STORAGE -> "For older Android versions, this app requires access to your storage to find and play video files. Please grant this permission."
        else -> "This permission is important for the app's functionality."
    }
}

// --- Updated Previews ---

@Preview(
    showBackground = true,
    name = "Settings Screen - Light - Granted",
    device = "spec:width=1080px,height=2560px,dpi=440",
    group = "Settings Content"
)
@Composable
fun SettingsScreenContentPreview_Granted() {
    ExoPlayerTheme {
        // For previews, Scaffold is often managed by the preview itself or a wrapper if needed for top bars.
        // Here, SettingsScreenContent expects paddingValues, so we provide some default.
        // The actual relevantPermission for preview doesn't strictly matter if we control the map.
        val relevantPermissionPreview = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        Scaffold { paddingValues -> // Add Scaffold for consistent preview padding
            SettingsScreenContent(
                paddingValues = paddingValues,
                uiState = SettingsUIState(
                    permissionDisplayStatuses = mapOf(
                        relevantPermissionPreview to PermissionDisplayStatus(
                            isGranted = true,
                            shouldShowRationale = false
                        )
                    )
                ),
                relevantPermission = relevantPermissionPreview,
                onPermissionItemRequest = {},
                onPermissionItemOpenSettings = {},
                onInitialStatusLoadIfEmpty = {}
            )
        }
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Settings Screen - Dark - Denied with Rationale",
    device = "spec:width=1080px,height=2560px,dpi=440",
    group = "Settings Content"
)
@Composable
fun SettingsScreenContentPreview_Denied_Rationale() {
    ExoPlayerTheme {
        val relevantPermissionPreview = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        Scaffold { paddingValues ->
            SettingsScreenContent(
                paddingValues = paddingValues,
                uiState = SettingsUIState(
                    permissionDisplayStatuses = mapOf(
                        relevantPermissionPreview to PermissionDisplayStatus(
                            isGranted = false,
                            shouldShowRationale = true
                        )
                    )
                ),
                relevantPermission = relevantPermissionPreview,
                onPermissionItemRequest = {},
                onPermissionItemOpenSettings = {},
                onInitialStatusLoadIfEmpty = {}
            )
        }
    }
}

@Preview(
    showBackground = true,
    name = "Settings Screen - Denied Permanently",
    device = "spec:width=1080px,height=2560px,dpi=440",
    group = "Settings Content"
)
@Composable
fun SettingsScreenContentPreview_Denied_Permanently() {
    ExoPlayerTheme {
        val relevantPermissionPreview = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        Scaffold { paddingValues ->
            SettingsScreenContent(
                paddingValues = paddingValues,
                uiState = SettingsUIState(
                    permissionDisplayStatuses = mapOf(
                        relevantPermissionPreview to PermissionDisplayStatus(
                            isGranted = false,
                            shouldShowRationale = false // Key for "Open Settings" button
                        )
                    )
                ),
                relevantPermission = relevantPermissionPreview,
                onPermissionItemRequest = {},
                onPermissionItemOpenSettings = {},
                onInitialStatusLoadIfEmpty = {}
            )
        }
    }
}

@Preview(
    showBackground = true,
    name = "Settings Screen - Empty/Loading",
    device = "spec:width=1080px,height=2560px,dpi=440",
    group = "Settings Content"
)
@Composable
fun SettingsScreenContentPreview_Empty() {
    ExoPlayerTheme {
        val relevantPermissionPreview = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        Scaffold { paddingValues ->
            SettingsScreenContent(
                paddingValues = paddingValues,
                uiState = SettingsUIState(
                    permissionDisplayStatuses = emptyMap() // Empty state
                ),
                relevantPermission = relevantPermissionPreview,
                onPermissionItemRequest = {},
                onPermissionItemOpenSettings = {},
                onInitialStatusLoadIfEmpty = {}
            )
        }
    }
}
