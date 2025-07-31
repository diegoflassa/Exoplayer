package dev.diegoflassa.poc.exoplayer.navigation

sealed interface NavigationIntent {
    data class NavigateTo(val screen: Screen) : NavigationIntent
    data object GoToPlayer : NavigationIntent
    data object GoBack : NavigationIntent
    data class ReplaceAll(val screen: Screen) : NavigationIntent
}