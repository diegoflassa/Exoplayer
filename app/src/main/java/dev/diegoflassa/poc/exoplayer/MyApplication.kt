package dev.diegoflassa.poc.exoplayer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.diegoflassa.poc.exoplayer.extensions.modoDebugHabilitado
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import dev.diegoflassa.poc.exoplayer.data.downloaders.OkHttpNewPipeDownloader
import timber.log.Timber

@HiltAndroidApp
class MyApplication : Application() {

    companion object {
        private val tag = MyApplication::class.simpleName ?: "MyApplication"
    }

    override fun onCreate() {
        super.onCreate()
        if (modoDebugHabilitado()) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.tag(tag).i("onCreate")

        try {
            NewPipe.init(
                OkHttpNewPipeDownloader,
                Localization.DEFAULT,
                ContentCountry.DEFAULT
            )
            Timber.tag(tag).i("NewPipeExtractor initialized successfully.")
        } catch (e: ExceptionInInitializerError) {
            Timber.tag(tag).e(
                e,
                "NewPipeExtractor already initialized or error during initial static initialization."
            )
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Failed to initialize NewPipeExtractor.")
        }
    }
}
