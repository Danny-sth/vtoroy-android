package com.vtoroy.android.audio

interface AudioPlayerInterface {
    fun initialize()
    suspend fun playAudio(audioData: ByteArray): Boolean
    fun stop()
    fun release()
}
