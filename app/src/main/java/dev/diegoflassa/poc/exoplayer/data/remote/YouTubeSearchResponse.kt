package dev.diegoflassa.poc.exoplayer.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class YouTubeSearchResponse(
    val items: List<YouTubeSearchItem>? = null,
    val nextPageToken: String? = null,
    val pageInfo: YouTubePageInfo? = null
)

@Serializable
data class YouTubeSearchItem(
    val id: YouTubeVideoId? = null,
    val snippet: YouTubeSnippet? = null
)

@Serializable
data class YouTubeVideoId(
    val videoId: String? = null,
    val kind: String? = null // e.g., "youtube#video"
)

@Serializable
data class YouTubeSnippet(
    val publishedAt: String? = null,
    val channelId: String? = null,
    val title: String? = null,
    val description: String? = null,
    val thumbnails: YouTubeThumbnails? = null,
    val channelTitle: String? = null,
    val liveBroadcastContent: String? = null // "live", "upcoming", "none"
)

@Serializable
data class YouTubeThumbnails(
    val default: YouTubeThumbnail? = null,
    val medium: YouTubeThumbnail? = null,
    val high: YouTubeThumbnail? = null
)

@Serializable
data class YouTubeThumbnail(
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null
)

@Serializable
data class YouTubePageInfo(
    val totalResults: Int? = null,
    val resultsPerPage: Int? = null
)
