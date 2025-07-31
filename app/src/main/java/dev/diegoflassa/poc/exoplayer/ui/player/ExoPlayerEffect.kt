package dev.diegoflassa.poc.exoplayer.ui.player

sealed interface ExoPlayerEffect {
    object GoToSettings : ExoPlayerEffect
    data class ShowToast(val message: String) : ExoPlayerEffect
}