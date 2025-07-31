package dev.diegoflassa.poc.exoplayer.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import dev.diegoflassa.poc.exoplayer.ui.player.ExoPlayerScreen
import dev.diegoflassa.poc.exoplayer.ui.settings.SettingsScreen

@Composable
fun NavDisplay(modifier: Modifier, navigationViewModel: NavigationViewModel) {
    val backstack = navigationViewModel.state.collectAsStateWithLifecycle().value.backStack
    NavDisplay(
        backStack = backstack,
        modifier = modifier,
        transitionSpec = {
            fadeIn(tween(300)) togetherWith fadeOut(tween(300))
        },
        entryDecorators = listOf(
            rememberSceneSetupNavEntryDecorator(),
            rememberSavedStateNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Screen.Player> {
                ExoPlayerScreen(navigationViewModel = navigationViewModel)
            }
            entry<Screen.Settings> {
                SettingsScreen(navigationViewModel = navigationViewModel)
            }
        }
    )
}

