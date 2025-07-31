package dev.diegoflassa.poc.exoplayer.data.downloaders

import okhttp3.Headers
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response

object OkHttpNewPipeDownloader : Downloader() {

    private val client = OkHttpClient()

    override fun execute(request: Request): Response {
        val okRequest = okhttp3.Request.Builder()
            .url(request.url())
            .headers(
                Headers.Builder().apply {
                    request.headers().forEach { (key, values) ->
                        values.forEach { value -> add(key, value) }
                    }
                }.build()
            )
            .build()

        val response = client.newCall(okRequest).execute()

        val responseCode = response.code
        val responseMessage = response.message
        val responseHeaders = response.headers.toMultimap()
        val responseBody = response.body.string()
        val latestUrl = response.request.url.toString()

        return Response(
            responseCode,
            responseMessage,
            responseHeaders,
            responseBody,
            latestUrl
        )
    }
}