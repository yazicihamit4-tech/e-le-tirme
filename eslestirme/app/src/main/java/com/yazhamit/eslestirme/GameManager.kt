package com.yazhamit.eslestirme

class GameManager {
    var currentLevel: Int = 1
        private set
    var score: Int = 0
        private set

    var comboCount: Int = 1
        private set

    private var lastMatchTime: Long = 0

    // Maximum kart sayısı
    private val maxCards = 30

    // Her seviye de kart sayısı 4 ile başlayıp 2'şer artacak
    fun getCardCountForLevel(): Int {
        val count = 4 + (currentLevel - 1) * 2
        return if (count > maxCards) maxCards else count
    }

    fun addScore() {
        val currentTime = System.currentTimeMillis()

        // Eger son eslesmeden itibaren 3 saniye icinde tekrar eslesirse kombo artar
        if (lastMatchTime > 0 && (currentTime - lastMatchTime) <= 3000) {
            comboCount++
        } else {
            comboCount = 1
        }

        lastMatchTime = currentTime

        // Temel puan 5, kombo ile katlanir. Max kombo x5
        val multiplier = if (comboCount > 5) 5 else comboCount
        score += (5 * multiplier)
    }

    fun resetCombo() {
        comboCount = 1
        lastMatchTime = 0
    }

    fun nextLevel() {
        currentLevel++
        resetCombo()
    }

    fun reset() {
        currentLevel = 1
        score = 0
        resetCombo()
    }
}
