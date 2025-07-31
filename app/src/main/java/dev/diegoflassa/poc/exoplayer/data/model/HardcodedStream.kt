package dev.diegoflassa.poc.exoplayer.data.model

data class HardcodedStream(
    val name: String,
    val url: String,
    val type: String, // e.g., "HLS", "DASH", "YouTube"
    val videoId: String? = null // Added for YouTube videos
)
