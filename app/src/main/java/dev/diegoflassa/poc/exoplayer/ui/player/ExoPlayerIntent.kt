package dev.diegoflassa.poc.exoplayer.ui.player

import android.net.Uri

sealed interface ExoPlayerIntent {
    object GoToSettings : ExoPlayerIntent
    data class LoadExo(val uri: Uri) : ExoPlayerIntent
    data class LoadExoFromUrl(val url: String) : ExoPlayerIntent
    data class PlayYouTubeExo(val videoId: String) : ExoPlayerIntent
    data class PermissionResult(val isGranted: Boolean) : ExoPlayerIntent
    object RequestPickExo : ExoPlayerIntent
    object FetchApiStreams : ExoPlayerIntent
    object FetchYouTubeLiveGameStreams : ExoPlayerIntent
    object ToggleControls : ExoPlayerIntent
    object TogglePlayPause : ExoPlayerIntent
    object ExoEnded : ExoPlayerIntent
}
