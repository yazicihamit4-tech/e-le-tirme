package com.yazhamit.eslestirme

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class SoundManager(context: Context) {
    private var soundPool: SoundPool
    private var matchSoundId: Int = 0
    private var mismatchSoundId: Int = 0

    var isMuted = false

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()

        // Ses dosyalarını yüklüyoruz
        matchSoundId = soundPool.load(context, R.raw.match, 1)
        mismatchSoundId = soundPool.load(context, R.raw.mismatch, 1)
    }

    fun playMatchSound() {
        if (!isMuted) {
            soundPool.play(matchSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun playMismatchSound() {
        if (!isMuted) {
            soundPool.play(mismatchSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool.release()
    }
}
