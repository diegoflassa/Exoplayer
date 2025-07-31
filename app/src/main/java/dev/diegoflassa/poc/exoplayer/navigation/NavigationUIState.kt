package dev.diegoflassa.poc.exoplayer.navigation

data class NavigationUIState(
    val backStack: List<Screen> = listOf(Screen.Player)
)