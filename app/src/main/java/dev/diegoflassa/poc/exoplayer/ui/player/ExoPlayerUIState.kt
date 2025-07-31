package dev.diegoflassa.poc.exoplayer.ui.player

import android.net.Uri
import androidx.media3.common.Player
import dev.diegoflassa.poc.exoplayer.data.model.HardcodedStream

data class ExoPlayerUIState(
    val player: Player? = null,
    val videoUri: Uri? = null,
    val isLoading: Boolean = false,
    val isPlaying: Boolean = false,
    val showControls: Boolean = true,
    val error: String? = null,
    val hasReadMediaPermission: Boolean = false,
    val permissionRequested: Boolean = false,

    // States for Shaka Player Demo Streams
    val shakaStreams: List<HardcodedStream> = emptyList(),
    val isLoadingShakaStreams: Boolean = false,
    val shakaStreamsError: String? = null,

    // States for YouTube Live Game Streams
    val youTubeLiveGameStreams: List<HardcodedStream> = emptyList(),
    val isLoadingYouTubeStreams: Boolean = false,
    val youTubeStreamsError: String? = null
)
