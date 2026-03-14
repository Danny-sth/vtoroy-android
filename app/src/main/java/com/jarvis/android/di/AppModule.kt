package com.jarvis.android.di

import android.content.Context
import com.jarvis.android.audio.AudioPlayer
import com.jarvis.android.audio.AudioPlayerInterface
import com.jarvis.android.audio.AudioRecorder
import com.jarvis.android.audio.AudioRecorderInterface
import com.jarvis.android.audio.VoiceActivityDetector
import com.jarvis.android.audio.VoiceActivityDetectorInterface
import com.jarvis.android.data.SettingsRepository
import com.jarvis.android.network.JarvisApiClient
import com.jarvis.android.network.VoiceApiClientInterface
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository {
        return SettingsRepository(context)
    }

    @Provides
    @Singleton
    fun provideVoiceApiClient(): VoiceApiClientInterface {
        return JarvisApiClient()
    }

    @Provides
    fun provideAudioRecorder(
        @ApplicationContext context: Context,
        vad: VoiceActivityDetectorInterface
    ): AudioRecorderInterface {
        return AudioRecorder(context, vad)
    }

    @Provides
    fun provideAudioPlayer(
        @ApplicationContext context: Context
    ): AudioPlayerInterface {
        return AudioPlayer(context)
    }

    @Provides
    fun provideVoiceActivityDetector(
        @ApplicationContext context: Context
    ): VoiceActivityDetectorInterface {
        return VoiceActivityDetector(context)
    }
}
