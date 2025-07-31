package dev.diegoflassa.poc.exoplayer.ui.player.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView

@Composable
fun ExoPlayerWidget(
    player: androidx.media3.common.Player?,
    showControls: Boolean,
    onToggleControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                // Initial setup. Player and useController will be updated in the update block.
            }
        },
        update = { view ->
            // Update the PlayerView when the player instance or showControls changes
            if (view.player != player) {
                view.player = player
            }
            view.useController = showControls
        },
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16 /9f)
            .clickable { onToggleControls() }
    )
}
