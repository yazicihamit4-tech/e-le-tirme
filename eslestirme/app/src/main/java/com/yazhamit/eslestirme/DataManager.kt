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

    var achievementFirstBomb: Boolean
        get() = prefs.getBoolean("ACHIEVEMENT_BOMB", false)
        set(value) = prefs.edit().putBoolean("ACHIEVEMENT_BOMB", value).apply()

    var achievementComboX5: Boolean
        get() = prefs.getBoolean("ACHIEVEMENT_COMBOX5", false)
        set(value) = prefs.edit().putBoolean("ACHIEVEMENT_COMBOX5", value).apply()

    var lastPlayDate: Long
        get() = prefs.getLong("LAST_PLAY_DATE", 0L)
        set(value) = prefs.edit().putLong("LAST_PLAY_DATE", value).apply()

    // --- YENİ EKLENEN ÖZELLİKLER: MAĞAZA VE CAN SİSTEMİ --- //

    // Can Sistemi (Max 5)
    var lives: Int
        get() = prefs.getInt("LIVES", 5)
        set(value) {
            var v = value
            if (v > 5) v = 5
            if (v < 0) v = 0
            prefs.edit().putInt("LIVES", v).apply()
        }

    var lastLiveUpdateTime: Long
        get() = prefs.getLong("LAST_LIVE_UPDATE", System.currentTimeMillis())
        set(value) = prefs.edit().putLong("LAST_LIVE_UPDATE", value).apply()

    // Mağaza (Temalar)
    var selectedTheme: String
        get() = prefs.getString("SELECTED_THEME", "CLASSIC") ?: "CLASSIC"
        set(value) = prefs.edit().putString("SELECTED_THEME", value).apply()

    // Satın alınan temaları virgülle ayrılmış string olarak tutalım (Örn: "CLASSIC,ANIMALS")
    var unlockedThemes: String
        get() = prefs.getString("UNLOCKED_THEMES", "CLASSIC") ?: "CLASSIC"
        set(value) = prefs.edit().putString("UNLOCKED_THEMES", value).apply()

    fun unlockTheme(themeName: String) {
        val current = unlockedThemes.split(",").toMutableList()
        if (!current.contains(themeName)) {
            current.add(themeName)
            unlockedThemes = current.joinToString(",")
        }
    }

    fun isThemeUnlocked(themeName: String): Boolean {
        return unlockedThemes.split(",").contains(themeName)
    }

    fun checkAndResetDailyTasks() {
        val currentTime = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L
        if (currentTime - lastPlayDate > oneDayMillis) {
            dailyMatches = 0
            lastPlayDate = currentTime
        }
    }

    fun checkAndRestoreLives() {
        val currentTime = System.currentTimeMillis()
        val fifteenMinsMillis = 15 * 60 * 1000L

        if (lives < 5) {
            val timePassed = currentTime - lastLiveUpdateTime
            if (timePassed > fifteenMinsMillis) {
                // Kaç 15 dakika geçtiyse o kadar can ver
                val livesToAdd = (timePassed / fifteenMinsMillis).toInt()
                lives += livesToAdd

                // Kalan zamanı (artık) yeni başlangıç süresine ekle ki süre kaybı olmasın
                val remainder = timePassed % fifteenMinsMillis
                lastLiveUpdateTime = currentTime - remainder
            }
        } else {
            // Can doluysa sayacı hep güncel tut
            lastLiveUpdateTime = currentTime
        }
    }
}
