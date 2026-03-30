package com.yazhamit.eslestirme

import android.content.Context
import android.content.SharedPreferences

class DataManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("GameData", Context.MODE_PRIVATE)

    var totalCoins: Int
        get() = prefs.getInt("TOTAL_COINS", 0)
        set(value) = prefs.edit().putInt("TOTAL_COINS", value).apply()

    var highScore: Int
        get() = prefs.getInt("HIGH_SCORE", 0)
        set(value) = prefs.edit().putInt("HIGH_SCORE", value).apply()

    var dailyMatches: Int
        get() = prefs.getInt("DAILY_MATCHES", 0)
        set(value) = prefs.edit().putInt("DAILY_MATCHES", value).apply()

    // Basarimlar (Achievements) icin basit boolean tutucular
    var achievementFirstBomb: Boolean
        get() = prefs.getBoolean("ACHIEVEMENT_BOMB", false)
        set(value) = prefs.edit().putBoolean("ACHIEVEMENT_BOMB", value).apply()

    var achievementComboX5: Boolean
        get() = prefs.getBoolean("ACHIEVEMENT_COMBOX5", false)
        set(value) = prefs.edit().putBoolean("ACHIEVEMENT_COMBOX5", value).apply()

    // Gunluk gorev kontrolu icin son oynama gunu
    var lastPlayDate: Long
        get() = prefs.getLong("LAST_PLAY_DATE", 0L)
        set(value) = prefs.edit().putLong("LAST_PLAY_DATE", value).apply()

    fun checkAndResetDailyTasks() {
        val currentTime = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L
        if (currentTime - lastPlayDate > oneDayMillis) {
            dailyMatches = 0
            lastPlayDate = currentTime
        }
    }
}
