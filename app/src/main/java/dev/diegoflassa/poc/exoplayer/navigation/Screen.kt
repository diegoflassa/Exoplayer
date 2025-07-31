package dev.diegoflassa.poc.exoplayer.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Screen : NavKey {

    @Serializable
    data object Player : Screen

    @Serializable
    data object Settings : Screen
}
