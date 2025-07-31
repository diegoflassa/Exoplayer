package dev.diegoflassa.poc.exoplayer.ui.player.widgets

import androidx.annotation.OptIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun ExoPlayerWidget(
    player: Player?,
    showControls: Boolean,
    onToggleControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                setArtworkDisplayMode(PlayerView.ARTWORK_DISPLAY_MODE_FIT)

                controllerHideOnTouch = true
                controllerShowTimeoutMs = 3000
            }
        },
        update = { view ->
            if (view.player != player) {
                view.player = player
            }
            view.useController = showControls
        },
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16 / 9f)
            .clickable { onToggleControls() }
    )
}
