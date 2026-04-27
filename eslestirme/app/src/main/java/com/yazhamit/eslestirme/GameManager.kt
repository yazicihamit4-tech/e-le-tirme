package com.yazhamit.eslestirme

class GameManager {
    var currentLevel: Int = 1
        private set
    var score: Int = 0
        private set

    var comboCount: Int = 1
        private set

    private var lastMatchTime: Long = 0

    private val maxCards = 30

    fun getCardCountForLevel(): Int {
        val count = 4 + (currentLevel - 1) * 2
        return if (count > maxCards) maxCards else count
    }

    // Oyun süresi (Saniye): Her kart başına ~2.5 saniye + 10s bonus
    fun getTimeLimitForLevel(): Int {
        val count = getCardCountForLevel()
        return (count * 2.5).toInt() + 10
    }

    fun addScore() {
        val currentTime = System.currentTimeMillis()

        if (lastMatchTime > 0 && (currentTime - lastMatchTime) <= 3000) {
            comboCount++
        } else {
            comboCount = 1
        }

        lastMatchTime = currentTime

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
