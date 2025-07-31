package dev.diegoflassa.poc.exoplayer.ui.settings.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.diegoflassa.poc.exoplayer.R
import dev.diegoflassa.poc.exoplayer.ui.settings.PermissionDisplayStatus

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