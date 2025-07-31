package dev.diegoflassa.poc.exoplayer.ui.player

import android.app.Application
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.diegoflassa.poc.exoplayer.data.config.IConfig
import dev.diegoflassa.poc.exoplayer.data.model.HardcodedStream
import dev.diegoflassa.poc.exoplayer.data.remote.YouTubeApiService
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.services.youtube.YoutubeService
import timber.log.Timber

private const val tag = "ExoPlayerViewModel"

@HiltViewModel
open class ExoPlayerViewModel @Inject constructor(
    private val app: Application,
    private val youTubeApiService: YouTubeApiService,
    private val config: IConfig
) : AndroidViewModel(app), Player.Listener {

    private val _uiState = MutableStateFlow(ExoPlayerUIState())
    val uiState = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ExoPlayerEffect>()
    val effect = _effect.asSharedFlow()

    private var exoPlayerInstance: ExoPlayer? = null

    private val shakaDemoStreams = listOf(
        HardcodedStream(
            "Tears of Steel (DASH)",
            "https://storage.googleapis.com/shaka-demo-assets/angel-one/dash.mpd",
            "DASH"
        ),
        HardcodedStream(
            "Tears of Steel (HLS)",
            "https://storage.googleapis.com/shaka-demo-assets/angel-one-hls/hls.m3u8",
            "HLS"
        )
    )

    init {
        initializePlayer()
    }

    private fun initializePlayer() {
        if (exoPlayerInstance == null) {
            exoPlayerInstance = ExoPlayer.Builder(app).build().apply {
                addListener(this@ExoPlayerViewModel)
            }
            _uiState.update { it.copy(player = exoPlayerInstance) }
        }
    }

    fun reduce(event: ExoPlayerIntent) {
        viewModelScope.launch {
            when (event) {
                is ExoPlayerIntent.GoToSettings ->_effect.emit(ExoPlayerEffect.GoToSettings)
                is ExoPlayerIntent.LoadExo -> handleLoadVideo(event.uri)
                is ExoPlayerIntent.LoadExoFromUrl -> handleLoadVideoFromUrl(event.url)
                is ExoPlayerIntent.PlayYouTubeExo -> handlePlayYouTubeVideo(event.videoId)
                is ExoPlayerIntent.FetchApiStreams -> handleLoadShakaDemoStreams()
                is ExoPlayerIntent.PermissionResult -> handlePermissionResult(event.isGranted)
                is ExoPlayerIntent.RequestPickExo -> { /* UI will handle picker */
                }

                is ExoPlayerIntent.ToggleControls -> _uiState.update { it.copy(showControls = !it.showControls) }
                is ExoPlayerIntent.TogglePlayPause -> handleTogglePlayPause()
                is ExoPlayerIntent.ExoEnded -> { /* TODO */
                }

                is ExoPlayerIntent.FetchYouTubeLiveGameStreams -> handleFetchYouTubeLiveGameStreams()
            }
        }
    }

    private fun handleLoadVideoFromUrl(url: String) {
        try {
            val uri = url.toUri()
            if (url.isBlank()) {
                _uiState.update { it.copy(isLoading = false, error = "URL cannot be empty") }
                viewModelScope.launch { _effect.emit(ExoPlayerEffect.ShowToast("URL cannot be empty")) }
                return
            }
            _uiState.update { it.copy(shakaStreamsError = null, youTubeStreamsError = null) }
            handleLoadVideo(uri)
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Invalid URL format: ${e.message}"
                )
            }
            viewModelScope.launch { _effect.emit(ExoPlayerEffect.ShowToast("Invalid URL format")) }
        }
    }

    private fun handlePlayYouTubeVideo(videoId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(isLoading = true, error = null, videoUri = null) }
            }

            try {
                val videoUrl = "https://www.youtube.com/watch?v=$videoId"
                Timber.tag(tag).d("Attempting to extract from: $videoUrl")
                val services =NewPipe.getServices()
                Timber.tag(tag).d("$services")
                val service = NewPipe.getService(YouTube.serviceId) as YoutubeService
                val extractor = service.getStreamExtractor(videoUrl)
                extractor.fetchPage()
                val streamInfo = extractor

                if (streamInfo == null) {
                    Timber.tag(tag).e("NewPipe: Failed to get StreamInfo for $videoId.")
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Failed to extract YouTube video info (null StreamInfo)."
                            )
                        }
                        _effect.emit(ExoPlayerEffect.ShowToast("Error extracting YouTube video details."))
                    }
                    return@launch
                }

                // Prioritize progressive MP4 streams (itag 22 for 720p, itag 18 for 360p)
                // NewPipeExtractor's VideoStream might not always have 'itag' populated reliably across all versions/formats
                // Let's prefer resolution and format.
                val preferredStream = extractor.videoStreams
                    .filter { it.format == MediaFormat.MPEG_4 && it.isVideoOnly() }
                    .maxByOrNull {
                        if (it.getResolution().contains("720")) 720 else if (it.getResolution()
                                .contains(
                                    "360"
                                )
                        ) 360 else 0
                    }
                    ?: streamInfo.videoStreams.firstOrNull {
                        it.content?.isNotBlank() == true && it.isVideoOnly().not()
                    }
                    ?: streamInfo.videoStreams.firstOrNull { it.content?.isNotBlank() == true }

                if (preferredStream != null && preferredStream.content?.isNotBlank() == true) {
                    Timber.tag(tag)
                        .d("NewPipe Extracted YouTube URL (${preferredStream.getResolution()}, Format: ${preferredStream.format}): ${preferredStream.content}")
                    withContext(Dispatchers.Main) {
                        handleLoadVideo(preferredStream.content?.toUri() ?: Uri.EMPTY)
                    }
                } else {
                    Timber.tag(tag)
                        .w("NewPipe: No suitable stream found for $videoId. Available streams: ${streamInfo.videoStreams.joinToString { it.getResolution() + " " + it.format + " vo:" + it.isVideoOnly() }}")
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "No suitable stream found for YouTube video."
                            )
                        }
                        _effect.emit(ExoPlayerEffect.ShowToast("Could not find playable YouTube stream format."))
                    }
                }

            } catch (e: ExtractionException) {
                Timber.tag(tag).e(e, "NewPipe ExtractionException for $videoId: ${e.message}")
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "YouTube extraction failed: ${e.message}"
                        )
                    }
                    _effect.emit(
                        ExoPlayerEffect.ShowToast(
                            "YouTube extraction error: ${
                                e.message?.take(
                                    100
                                )
                            }"
                        )
                    )
                }
            } catch (e: Exception) { // Catch other potential errors
                Timber.tag(tag).e(e, "Error during YouTube extraction for $videoId: ${e.message}")
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "An unexpected error occurred during extraction."
                        )
                    }
                    _effect.emit(
                        ExoPlayerEffect.ShowToast(
                            "Extraction error: ${
                                e.message?.take(
                                    100
                                )
                            }"
                        )
                    )
                }
            }
        }
    }

    private suspend fun handleFetchYouTubeLiveGameStreams() { // Already uses viewModelScope.launch(Dispatchers.IO) via Retrofit
        _uiState.update { it.copy(isLoadingYouTubeStreams = true, youTubeStreamsError = null) }
        try {
            val apiKey = config.youTubeDataAPIv3
            if (apiKey.isBlank() || apiKey == "YOUR_NEW_SECURE_API_KEY" || apiKey == "YOUR_ACTUAL_YOUTUBE_API_KEY") {
                Timber.tag(tag).e("YouTube API Key is not configured.")
                _uiState.update {
                    it.copy(
                        isLoadingYouTubeStreams = false,
                        youTubeStreamsError = "YouTube API Key is not configured."
                    )
                }
                _effect.emit(ExoPlayerEffect.ShowToast("API Key not configured for YouTube."))
                return
            }

            val response = youTubeApiService.searchVideos(
                apiKey = apiKey,
                query = "live game streaming",
                eventType = "live",
                type = "video",
                maxResults = 20
            )
            Timber.tag(tag).d("YouTube API Response: $response")

            val videos = response.items?.mapNotNull { item ->
                val videoId = item.id?.videoId
                val title = item.snippet?.title
                if (videoId != null && title != null) {
                    HardcodedStream(
                        name = title,
                        url = "https://www.youtube.com/watch?v=$videoId",
                        type = "YouTube",
                        videoId = videoId
                    )
                } else {
                    Timber.tag(tag)
                        .w("Skipping YouTube item due to missing videoId or title: $item")
                    null
                }
            } ?: emptyList()

            _uiState.update {
                it.copy(
                    youTubeLiveGameStreams = videos,
                    isLoadingYouTubeStreams = false
                )
            }
            if (videos.isEmpty()) {
                _effect.emit(ExoPlayerEffect.ShowToast("No live game streams found on YouTube."))
            }

        } catch (e: Exception) {
            Timber.tag(tag).e(t = e, message = "Error fetching YouTube videos: ${e.message}")
            _uiState.update {
                it.copy(
                    isLoadingYouTubeStreams = false,
                    youTubeStreamsError = "Failed to load YouTube videos: ${e.localizedMessage}"
                )
            }
            _effect.emit(ExoPlayerEffect.ShowToast("Error fetching YouTube videos: ${e.message}"))
        }
    }

    private fun handleLoadShakaDemoStreams() {
        _uiState.update { it.copy(isLoadingShakaStreams = true, shakaStreamsError = null) }
        _uiState.update { it.copy(shakaStreams = shakaDemoStreams, isLoadingShakaStreams = false) }
        if (shakaDemoStreams.isEmpty()) {
            viewModelScope.launch { _effect.emit(ExoPlayerEffect.ShowToast("No Shaka demo streams are defined.")) }
        }
    }

    private fun handleLoadVideo(uri: Uri) { // This should be called on Main thread
        _uiState.update { it.copy(isLoading = true, videoUri = uri, error = null) }
        initializePlayer()

        val mediaItem = MediaItem.fromUri(uri)
        exoPlayerInstance?.apply {
            stop()
            clearMediaItems()
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
            _uiState.update { it.copy(isPlaying = this.playWhenReady) }
        }
    }

    private fun handlePermissionResult(isGranted: Boolean) {
        _uiState.update { it.copy(hasReadMediaPermission = isGranted, permissionRequested = true) }
        if (!isGranted) {
            viewModelScope.launch { _effect.emit(ExoPlayerEffect.ShowToast("Permission denied. Cannot load videos from local storage.")) }
        }
    }

    private fun handleTogglePlayPause() {
        exoPlayerInstance?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _uiState.update { oldState ->
            val newIsLoading =
                if (oldState.isLoading && (isPlaying || exoPlayerInstance?.playbackState == Player.STATE_READY || exoPlayerInstance?.playbackState == Player.STATE_ENDED)) {
                    false
                } else {
                    oldState.isLoading
                }
            oldState.copy(isPlaying = isPlaying, isLoading = newIsLoading)
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
            if (_uiState.value.isLoading) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
        // Could add more detailed logging here if needed
        // Timber.tag(tag).d("Playback state changed: $playbackState, isLoading: ${_uiState.value.isLoading}")
    }


    override fun onPlayerError(error: PlaybackException) {
        val errorMessage = error.localizedMessage ?: "An unknown player error occurred"
        Timber.tag(tag).e(t = error, message = "PlayerError: $errorMessage")
        _uiState.update { it.copy(error = errorMessage, isLoading = false, videoUri = null) }
        viewModelScope.launch {
            _effect.emit(ExoPlayerEffect.ShowToast("Player error: $errorMessage"))
        }
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayerInstance?.removeListener(this)
        exoPlayerInstance?.release()
        exoPlayerInstance = null
        _uiState.update { it.copy(player = null) }
    }
}
