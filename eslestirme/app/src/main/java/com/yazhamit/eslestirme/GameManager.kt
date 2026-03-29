package com.yazhamit.eslestirme

class GameManager {
    var currentLevel: Int = 1
        private set
    var score: Int = 0
        private set

    // Maximum kart sayısı
    private val maxCards = 30

    // Her seviye de kart sayısı 4 ile başlayıp 2'şer artacak
    fun getCardCountForLevel(): Int {
        val count = 4 + (currentLevel - 1) * 2
        return if (count > maxCards) maxCards else count
    }

    fun addScore() {
        score += 5
    }

    fun nextLevel() {
        currentLevel++
    }

    fun reset() {
        currentLevel = 1
        score = 0
    }
}
