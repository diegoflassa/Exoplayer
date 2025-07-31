package dev.diegoflassa.poc.exoplayer.data.config

import android.content.Context
import dev.diegoflassa.poc.exoplayer.R
import javax.inject.Inject

class Config @Inject constructor(val context: Context) : IConfig {
    override val youTubeDataAPIv3 by lazy {
        context.getString(R.string.YOUTUBE_DATA_API_V3)
    }
}
