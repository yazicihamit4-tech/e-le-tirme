package com.yazhamit.eslestirme

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class SoundManager(context: Context) {
    private var soundPool: SoundPool
    private var matchSoundId: Int = 0
    private var mismatchSoundId: Int = 0
    private var comboSoundId: Int = 0
    private var gameOverSoundId: Int = 0
    private var bossHitSoundId: Int = 0

    var isMuted = false

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        matchSoundId = soundPool.load(context, R.raw.match, 1)
        mismatchSoundId = soundPool.load(context, R.raw.mismatch, 1)
        comboSoundId = soundPool.load(context, R.raw.combo, 1)
        gameOverSoundId = soundPool.load(context, R.raw.gameover, 1)
        bossHitSoundId = soundPool.load(context, R.raw.boss_hit, 1)
    }

    fun playMatchSound() { if (!isMuted) soundPool.play(matchSoundId, 1f, 1f, 1, 0, 1f) }
    fun playMismatchSound() { if (!isMuted) soundPool.play(mismatchSoundId, 1f, 1f, 1, 0, 1f) }
    fun playComboSound() { if (!isMuted) soundPool.play(comboSoundId, 1f, 1f, 1, 0, 1f) }
    fun playGameOverSound() { if (!isMuted) soundPool.play(gameOverSoundId, 1f, 1f, 1, 0, 1f) }
    fun playBossHitSound() { if (!isMuted) soundPool.play(bossHitSoundId, 1f, 1f, 1, 0, 1f) }

    fun release() {
        soundPool.release()
    }
}
