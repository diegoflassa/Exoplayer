package dev.diegoflassa.poc.exoplayer.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApiService {

    @GET("youtube/v3/search")
    suspend fun searchVideos(
        @Query("key") apiKey: String,
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query("eventType") eventType: String? = null,
        @Query("maxResults") maxResults: Int = 25
    ): YouTubeSearchResponse
}
