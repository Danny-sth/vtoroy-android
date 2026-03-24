package com.vtoroy.android.di

import android.content.Context
import com.vtoroy.android.audio.AudioPlayer
import com.vtoroy.android.audio.AudioPlayerInterface
import com.vtoroy.android.audio.AudioRecorder
import com.vtoroy.android.audio.AudioRecorderInterface
import com.vtoroy.android.audio.VoiceActivityDetector
import com.vtoroy.android.audio.VoiceActivityDetectorInterface
import com.vtoroy.android.data.SettingsRepository
import com.vtoroy.android.network.VtoroyApiClient
import com.vtoroy.android.network.VoiceApiClientInterface
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
        return VtoroyApiClient()
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
