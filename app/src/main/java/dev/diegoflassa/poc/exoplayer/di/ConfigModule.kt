package dev.diegoflassa.poc.exoplayer.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.diegoflassa.poc.exoplayer.data.config.IConfig
import dev.diegoflassa.poc.exoplayer.data.config.Config
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {

    @Provides
    @Singleton
    fun provideIConfig(@ApplicationContext context: Context): IConfig {
        return Config(context)
    }
}
