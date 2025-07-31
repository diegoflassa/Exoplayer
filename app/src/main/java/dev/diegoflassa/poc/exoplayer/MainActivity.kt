package dev.diegoflassa.poc.exoplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.google.android.material.R
import dagger.hilt.android.AndroidEntryPoint
import dev.diegoflassa.poc.exoplayer.navigation.NavDisplay
import dev.diegoflassa.poc.exoplayer.navigation.NavigationViewModel
import dev.diegoflassa.poc.exoplayer.ui.hiltActivityViewModel
import dev.diegoflassa.poc.exoplayer.ui.theme.ExoPlayerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_Material3_DayNight_NoActionBar)
        enableEdgeToEdge()

        setContent {
            val navigationViewModel: NavigationViewModel = hiltActivityViewModel()
            ExoPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavDisplay(modifier = Modifier, navigationViewModel = navigationViewModel)
                }
            }

            BackHandler {
                navigationViewModel.goBack()
            }
        }
    }
}
