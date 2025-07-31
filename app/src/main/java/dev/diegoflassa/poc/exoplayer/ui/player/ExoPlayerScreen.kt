@file:OptIn(ExperimentalMaterial3Api::class)

package dev.diegoflassa.poc.exoplayer.ui.player

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.diegoflassa.poc.exoplayer.data.model.HardcodedStream
import dev.diegoflassa.poc.exoplayer.extensions.isRemote
import dev.diegoflassa.poc.exoplayer.navigation.NavigationViewModel
import dev.diegoflassa.poc.exoplayer.ui.hiltActivityViewModel
import dev.diegoflassa.poc.exoplayer.ui.player.widgets.ExoPlayerWidget
import dev.diegoflassa.poc.exoplayer.ui.theme.ExoPlayerTheme
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

private const val tag = "ExoPlayerScreen"

@Composable
fun ExoPlayerScreen(
    navigationViewModel: NavigationViewModel? = hiltActivityViewModel(),
    exoPlayerViewModel: ExoPlayerViewModel = viewModel()
) {
    val uiState by exoPlayerViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        exoPlayerViewModel.effect.collectLatest { effect ->
            when (effect) {
                is ExoPlayerEffect.GoToSettings -> {
                    navigationViewModel?.goToSettings()
                }

                is ExoPlayerEffect.ShowToast -> {
                    Toast.makeText(
                        context,
                        effect.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    ExoPlayerScreenContent(exoPlayerUIState = uiState) {
        exoPlayerViewModel.reduce(it)
    }
}

@Composable
fun ExoPlayerScreenContent(
    exoPlayerUIState: ExoPlayerUIState? = null,
    onIntent: ((ExoPlayerIntent) -> Unit)? = null,
) {
    val context = LocalContext.current
    var streamUrl by remember { mutableStateOf("") }
    var shakaStreamsListExpanded by remember { mutableStateOf(false) }
    var youTubeStreamsListExpanded by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        onIntent?.invoke(ExoPlayerIntent.PermissionResult(isGranted))
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            onIntent?.invoke(ExoPlayerIntent.LoadExo(it))
        }
    }

    LaunchedEffect(exoPlayerUIState?.youTubeLiveGameStreams) {
        if (exoPlayerUIState?.youTubeLiveGameStreams?.isNotEmpty() == true && !youTubeStreamsListExpanded && exoPlayerUIState.isLoadingYouTubeStreams.not() && exoPlayerUIState.youTubeStreamsError == null) {
            Timber.tag(tag).d("LaunchedEffect(exoPlayerUIState?.youTubeLiveGameStreams)")
            youTubeStreamsListExpanded = true
        }
    }


    LaunchedEffect(Unit) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (exoPlayerUIState?.hasReadMediaPermission?.not() == true && exoPlayerUIState.permissionRequested.not()) {
                onIntent?.invoke(ExoPlayerIntent.PermissionResult(true))
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(exoPlayerUIState?.player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (exoPlayerUIState?.player?.isPlaying == true) exoPlayerUIState.player.pause()
                }

                Lifecycle.Event.ON_RESUME -> {
                    if (exoPlayerUIState?.player?.playWhenReady == false && exoPlayerUIState.isPlaying) {
                        exoPlayerUIState.player.play()
                    }
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ExoPlayer MVI Sample") },
                actions = {
                    IconButton(onClick = {
                        onIntent?.invoke(ExoPlayerIntent.GoToSettings)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings"
                        )
                    }
                })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (exoPlayerUIState?.hasReadMediaPermission?.not() == true && exoPlayerUIState.permissionRequested.not()) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(onClick = {
                        val permissionToRequest =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_EXTERNAL_STORAGE
                        permissionLauncher.launch(permissionToRequest)
                    }) { Text("Grant Media Permission") }
                    Text(
                        "Permission is required to select and play local videos.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                if (exoPlayerUIState?.player != null && exoPlayerUIState.videoUri != null) {
                    ExoPlayerWidget(
                        player = exoPlayerUIState.player,
                        showControls = exoPlayerUIState.showControls,
                        onToggleControls = { onIntent?.invoke(ExoPlayerIntent.ToggleControls) }
                    )
                } else if (exoPlayerUIState?.isLoading == true) {
                    CircularProgressIndicator(modifier = Modifier.padding(vertical = 32.dp))
                    Text("Loading video...", modifier = Modifier.padding(bottom = 8.dp))
                }

                if (exoPlayerUIState?.showControls == true && exoPlayerUIState.player != null && exoPlayerUIState.videoUri != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = { onIntent?.invoke(ExoPlayerIntent.TogglePlayPause) }) {
                            Text(
                                if (exoPlayerUIState.isPlaying) "Pause" else "Play"
                            )
                        }
                    }
                }
                exoPlayerUIState?.error?.let { error ->
                    Text(
                        "Player Error: $error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // YouTube Live Game Streams Section
                Button(
                    onClick = {
                        onIntent?.invoke(ExoPlayerIntent.FetchYouTubeLiveGameStreams)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Load YouTube Live Game Streams")
                }

                if (exoPlayerUIState?.isLoadingYouTubeStreams == true) {
                    CircularProgressIndicator(modifier = Modifier.padding(vertical = 16.dp))
                }

                exoPlayerUIState?.youTubeStreamsError?.let { error ->
                    Text(
                        "Error loading YouTube Streams: $error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                if (exoPlayerUIState?.youTubeLiveGameStreams?.isNotEmpty() == true) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { youTubeStreamsListExpanded = !youTubeStreamsListExpanded }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "YouTube Live Game Streams",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (youTubeStreamsListExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (youTubeStreamsListExpanded) "Collapse" else "Expand"
                        )
                    }

                    if (youTubeStreamsListExpanded) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                        ) {
                            items(
                                exoPlayerUIState.youTubeLiveGameStreams
                            ) { stream ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            stream.videoId?.let { videoId ->
                                                onIntent?.invoke(
                                                    ExoPlayerIntent.PlayYouTubeExo(
                                                        videoId
                                                    )
                                                )
                                            } ?: run {
                                                // Fallback or error if videoId is somehow null
                                                Toast.makeText(
                                                    context,
                                                    "Error: Video ID not found for this stream.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(text = stream.name, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = "Type: ${stream.type}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Shaka Demo Stream Section
                Button(
                    onClick = {
                        onIntent?.invoke(ExoPlayerIntent.FetchApiStreams)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Load Shaka Demo Streams")
                }

                if (exoPlayerUIState?.isLoadingShakaStreams == true) {
                    CircularProgressIndicator(modifier = Modifier.padding(vertical = 16.dp))
                }

                exoPlayerUIState?.shakaStreamsError?.let { error ->
                    Text(
                        "Error loading Shaka Streams: $error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                if (exoPlayerUIState?.shakaStreams?.isNotEmpty() == true) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { shakaStreamsListExpanded = !shakaStreamsListExpanded }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Shaka Demo Streams",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (shakaStreamsListExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (shakaStreamsListExpanded) "Collapse" else "Expand"
                        )
                    }

                    if (shakaStreamsListExpanded) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                        ) {
                            items(
                                exoPlayerUIState.shakaStreams
                            ) { stream: HardcodedStream ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            streamUrl = stream.url
                                            onIntent?.invoke(
                                                ExoPlayerIntent.LoadExoFromUrl(
                                                    stream.url
                                                )
                                            )
                                        },
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = stream.name,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Type: ${stream.type}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = streamUrl,
                    onValueChange = { streamUrl = it },
                    label = { Text("Stream URL (HLS, DASH, etc.)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (streamUrl.isNotBlank()) {
                            onIntent?.invoke(ExoPlayerIntent.LoadExoFromUrl(streamUrl))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Load from Entered URL")
                }

                if (exoPlayerUIState?.hasReadMediaPermission == true) {
                    val pickButtonText =
                        if (exoPlayerUIState.videoUri != null && !exoPlayerUIState.videoUri.isRemote()) {
                            "Pick Another Local Video"
                        } else {
                            "Pick Local Video"
                        }
                    Button(
                        onClick = { videoPickerLauncher.launch("video/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(pickButtonText)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// --- Preview Code ---

@Composable
private fun ExoPlayerScreenPreview(state: ExoPlayerUIState) {
    ExoPlayerTheme {
        ExoPlayerScreenContent(
            exoPlayerUIState = state,
            onIntent = { event ->
                Timber.tag("Preview").d("Intent: $event")
            }
        )
    }
}

// --- Previews ---

@Preview(name = "1. Permission Needed", group = "ExoPlayerScreen", showBackground = true)
@Composable
fun ExoPlayerScreenPreview_PermissionNeeded() {
    ExoPlayerScreenPreview(
        state = ExoPlayerUIState(
            hasReadMediaPermission = false,
            permissionRequested = false
        )
    )
}

@Preview(name = "2. Permission Denied", group = "ExoPlayerScreen", showBackground = true)
@Composable
fun ExoPlayerScreenPreview_PermissionDenied() {
    ExoPlayerScreenPreview(
        state = ExoPlayerUIState(
            hasReadMediaPermission = false,
            permissionRequested = true
        )
    )
}

@Preview(
    name = "3. Default Empty (Perm. Granted)",
    group = "ExoPlayerScreen",
    showBackground = true
)
@Composable
fun ExoPlayerScreenPreview_DefaultEmpty() {
    ExoPlayerScreenPreview(state = ExoPlayerUIState(hasReadMediaPermission = true))
}

@Preview(name = "4. Loading Video", group = "ExoPlayerScreen", showBackground = true)
@Composable
fun ExoPlayerScreenPreview_LoadingVideo() {
    ExoPlayerScreenPreview(
        state = ExoPlayerUIState(
            isLoading = true,
            hasReadMediaPermission = true
        )
    )
}

@Preview(name = "5. Player Error", group = "ExoPlayerScreen", showBackground = true)
@Composable
fun ExoPlayerScreenPreview_Error() {
    ExoPlayerScreenPreview(
        state = ExoPlayerUIState(
            error = "Network connection lost during playback.",
            hasReadMediaPermission = true
        )
    )
}

val sampleShakaStreamsForPreview = listOf(
    HardcodedStream("Preview Shaka DASH", "fake://dash.mpd", "DASH"),
    HardcodedStream("Preview Shaka HLS", "fake://hls.m3u8", "HLS")
)

@Preview(name = "6. Shaka Streams Loaded", group = "ExoPlayerScreen", showBackground = true)
@Composable
fun ExoPlayerScreenPreview_ShakaStreamsLoaded() {
    ExoPlayerScreenPreview(
        state = ExoPlayerUIState(
            shakaStreams = sampleShakaStreamsForPreview,
            hasReadMediaPermission = true
        )
    )
}

val sampleYouTubeStreamsForPreview = listOf(
    HardcodedStream("Preview YouTube Game 1", "yt_url1", "YouTube", videoId = "vid1_preview"),
    HardcodedStream("Preview YouTube Game 2", "yt_url2", "YouTube", videoId = "vid2_preview")
)

@Preview(name = "7. YouTube Streams Loaded", group = "ExoPlayerScreen", showBackground = true)
@Composable
fun ExoPlayerScreenPreview_YouTubeStreamsLoaded() {
    ExoPlayerScreenPreview(
        state = ExoPlayerUIState(
            youTubeLiveGameStreams = sampleYouTubeStreamsForPreview,
            hasReadMediaPermission = true
        )
    )
}

@Preview(
    name = "8. Video Loaded (Player Placeholder)",
    group = "ExoPlayerScreen",
    showBackground = true
)
@Composable
fun ExoPlayerScreenPreview_VideoLoaded() {
    ExoPlayerScreenPreview(
        state = ExoPlayerUIState(
            videoUri = "fake://video_loaded.mp4".toUri(),
            player = null,
            hasReadMediaPermission = true,
            showControls = true,
            isPlaying = false
        )
    )
}

@Preview(name = "9. Loading YouTube Streams", group = "ExoPlayerScreen", showBackground = true)
@Composable
fun ExoPlayerScreenPreview_LoadingYouTubeStreams() {
    ExoPlayerScreenPreview(
        state = ExoPlayerUIState(
            isLoadingYouTubeStreams = true,
            hasReadMediaPermission = true
        )
    )
}

@Preview(name = "10. Error YouTube Streams", group = "ExoPlayerScreen", showBackground = true)
@Composable
fun ExoPlayerScreenPreview_ErrorYouTubeStreams() {
    ExoPlayerScreenPreview(
        state = ExoPlayerUIState(
            youTubeStreamsError = "Failed to fetch YouTube streams.",
            hasReadMediaPermission = true
        )
    )
}

// --- End Preview Code ---
