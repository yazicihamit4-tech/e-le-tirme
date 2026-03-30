package com.yazhamit.eslestirme

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.ceil
import kotlin.math.sqrt
import kotlin.random.Random

class GameEngine(private val context: Context, private val gameBoard: FrameLayout, private val callback: GameCallback) {
    private val gameManager = GameManager()
    private val dataManager = DataManager(context)
    private val cards = mutableListOf<Card>()
    private var firstSelectedCard: Card? = null
    private var isProcessing = false

    val soundManager = SoundManager(context)

    private val cardSymbols = listOf("★", "♥", "♦", "♣", "♠", "▲", "▼", "◆", "●", "■", "△", "▽", "◇", "○", "□")
    private val powerUpSymbols = listOf("👁️", "💣")

    var powerUpTextView: TextView? = null

    // Oyun modu: "CLASSIC" veya "SURVIVAL"
    var gameMode: String = "CLASSIC"

    private val morphHandler = Handler(Looper.getMainLooper())
    private val morphRunnable = object : Runnable {
        override fun run() {
            triggerMorphing()
            morphHandler.postDelayed(this, Random.nextLong(15000, 20000))
        }
    }

    // Survival Mod icin saniye basina yukaridan yeni kart ekleme handler'i
    private val survivalHandler = Handler(Looper.getMainLooper())
    private val survivalRunnable = object : Runnable {
        override fun run() {
            if (gameMode == "SURVIVAL") {
                spawnSurvivalRow()
                survivalHandler.postDelayed(this, 15000)
            }
        }
    }

    interface GameCallback {
        fun onScoreChanged(score: Int)
        fun onLevelChanged(level: Int)
        fun onGameFinished()
        fun onGameOver(score: Int)
    }

    fun startGame(mode: String) {
        this.gameMode = mode
        gameManager.reset()
        callback.onLevelChanged(gameManager.currentLevel)
        callback.onScoreChanged(gameManager.score)
        loadLevel()
    }

    private fun loadLevel() {
        gameBoard.removeAllViews()
        cards.clear()

        morphHandler.removeCallbacksAndMessages(null)
        survivalHandler.removeCallbacksAndMessages(null)

        var cardCount = gameManager.getCardCountForLevel()

        if (gameMode == "SURVIVAL") {
            // Survival mode ekranı tamamen doldurmayacak sekilde (mesela max 20) baslar
            cardCount = 20
        }

        val pairsCount = cardCount / 2

        var hasPowerUp = false
        if (gameMode == "CLASSIC" && Random.nextInt(100) < 30 && pairsCount > 3) {
            hasPowerUp = true
        }

        for (i in 0 until pairsCount) {
            if (hasPowerUp && i == pairsCount - 1) {
                val pType = if (Random.nextBoolean()) "RADAR" else "BOMB"
                val pSymbol = if (pType == "RADAR") powerUpSymbols[0] else powerUpSymbols[1]

                val pCard1 = Card(context, cardId = i * 2, pairId = i, symbol = pSymbol)
                val pCard2 = Card(context, cardId = i * 2 + 1, pairId = i, symbol = pSymbol)
                pCard1.isPowerUp = true
                pCard1.powerUpType = pType
                pCard2.isPowerUp = true
                pCard2.powerUpType = pType

                cards.add(pCard1)
                cards.add(pCard2)
            } else {
                val symbol = cardSymbols[i % cardSymbols.size]
                cards.add(Card(context, cardId = i * 2, pairId = i, symbol = symbol))
                cards.add(Card(context, cardId = i * 2 + 1, pairId = i, symbol = symbol))
            }
        }

        cards.shuffle()

        val columns = if (gameMode == "SURVIVAL") 4 else ceil(sqrt(cardCount.toDouble())).toInt()
        val rows = if (gameMode == "SURVIVAL") 6 else ceil(cardCount.toDouble() / columns).toInt()

        gameBoard.post {
            val boardWidth = gameBoard.width
            val boardHeight = gameBoard.height

            val spacing = 16
            val cardWidth = (boardWidth - (columns + 1) * spacing) / columns
            val cardHeight = (boardHeight - (rows + 1) * spacing) / rows

            // Survival'da kartlari ekranin altindan baslayarak doldurmak daha zorlayicidir,
            // biz simdilik yukaridan dizecegiz ama bosluklari altta birakacagiz
            var currentCardIndex = 0

            // Survival'da son x satırı dolu basalım
            val startRow = if (gameMode == "SURVIVAL") rows - (cards.size / columns) else 0

            for (r in startRow until rows) {
                for (c in 0 until columns) {
                    if (currentCardIndex >= cards.size) break

                    val card = cards[currentCardIndex]
                    card.gridRow = r
                    card.gridCol = c

                    val x = spacing + c * (cardWidth + spacing)
                    val y = spacing + r * (cardHeight + spacing)

                    val layoutParams = FrameLayout.LayoutParams(cardWidth, cardHeight).apply {
                        leftMargin = x
                        topMargin = y
                    }

                    card.layoutParams = layoutParams
                    card.setOnClickListener {
                        onCardClicked(card)
                    }

                    gameBoard.addView(card)
                    currentCardIndex++
                }
            }

            if (gameMode == "CLASSIC" && gameManager.currentLevel >= 3) {
                morphHandler.postDelayed(morphRunnable, Random.nextLong(10000, 15000))
            }

            if (gameMode == "SURVIVAL") {
                survivalHandler.postDelayed(survivalRunnable, 15000)
            }
        }
    }

    private fun spawnSurvivalRow() {
        // En ust satira kart ekleme
        val columns = 4
        val rows = 6

        // Eger herhangi bir sutunda ust satirlar (row 0) doluysa oyun biter
        val isGameOver = cards.any { !it.isMatched && it.gridRow == 0 }
        if (isGameOver) {
            endGame()
            return
        }

        // Mevcut kartlari 1 satir asagi kaydir (Dusen blok mantigi)
        val spacing = 16
        val cardHeight = (gameBoard.height - (rows + 1) * spacing) / rows

        for (card in cards.filter { !it.isMatched }) {
            card.gridRow += 1
            val newY = spacing + card.gridRow * (cardHeight + spacing)

            val fallAnim = ObjectAnimator.ofFloat(card, "translationY", 0f, (cardHeight + spacing).toFloat())
            fallAnim.duration = 200
            fallAnim.addListener(object: AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    val lp = card.layoutParams as FrameLayout.LayoutParams
                    lp.topMargin = newY
                    card.layoutParams = lp
                    card.translationY = 0f
                }
            })
            fallAnim.start()
        }

        // Ust satira (row = 0) yeni 1 satir kart ekle (Mesela 4 sutun)
        val newPairs = columns / 2
        val newCards = mutableListOf<Card>()
        val maxId = cards.maxOfOrNull { it.cardId } ?: 0
        val maxPairId = cards.maxOfOrNull { it.pairId } ?: 0

        for (i in 0 until newPairs) {
            val symbol = cardSymbols.random()
            val pairId = maxPairId + 1 + i
            newCards.add(Card(context, cardId = maxId + 1 + (i*2), pairId = pairId, symbol = symbol))
            newCards.add(Card(context, cardId = maxId + 2 + (i*2), pairId = pairId, symbol = symbol))
        }
        newCards.shuffle()

        val boardWidth = gameBoard.width
        val cardWidth = (boardWidth - (columns + 1) * spacing) / columns

        for (c in 0 until columns) {
            if (c >= newCards.size) break
            val card = newCards[c]
            card.gridRow = 0
            card.gridCol = c

            val x = spacing + c * (cardWidth + spacing)
            val y = spacing + 0 * (cardHeight + spacing)

            val layoutParams = FrameLayout.LayoutParams(cardWidth, cardHeight).apply {
                leftMargin = x
                topMargin = y
            }

            card.layoutParams = layoutParams
            card.setOnClickListener {
                onCardClicked(card)
            }

            cards.add(card)
            gameBoard.addView(card)
        }
    }

    private fun endGame() {
        stopEngine()
        dataManager.totalCoins += gameManager.score / 2 // Puanlarin yarisi coin olur
        if (gameManager.score > dataManager.highScore) {
            dataManager.highScore = gameManager.score
        }
        callback.onGameOver(gameManager.score)
    }

    private fun triggerMorphing() {
        if (isProcessing) return
        val availableCards = cards.filter { !it.isMatched && !it.isFaceUp && !it.isPowerUp }
        val pairIds = availableCards.map { it.pairId }.distinct()
        if (pairIds.size >= 2) {
            val pairId1 = pairIds.random()
            val pairId2 = pairIds.filter { it != pairId1 }.random()

            val pair1Cards = availableCards.filter { it.pairId == pairId1 }
            val pair2Cards = availableCards.filter { it.pairId == pairId2 }

            if (pair1Cards.size == 2 && pair2Cards.size == 2) {
                val symbol1 = pair1Cards[0].symbol
                val symbol2 = pair2Cards[0].symbol

                showFrenzyMessage("ZAMAN KAYMASI!")
                soundManager.playMismatchSound()

                pair1Cards.forEach { it.morphSymbol(symbol2, pairId2) }
                pair2Cards.forEach { it.morphSymbol(symbol1, pairId1) }
            }
        }
    }

    fun stopEngine() {
        morphHandler.removeCallbacksAndMessages(null)
        survivalHandler.removeCallbacksAndMessages(null)
    }

    private fun showFrenzyMessage(message: String) {
        powerUpTextView?.let {
            it.text = message
            it.visibility = View.VISIBLE
            it.alpha = 1f
            it.scaleX = 0f
            it.scaleY = 0f

            val scaleX = ObjectAnimator.ofFloat(it, "scaleX", 0f, 1.2f, 1f)
            val scaleY = ObjectAnimator.ofFloat(it, "scaleY", 0f, 1.2f, 1f)
            val fadeOut = ObjectAnimator.ofFloat(it, "alpha", 1f, 0f)
            fadeOut.startDelay = 800

            val animatorSet = AnimatorSet()
            animatorSet.playTogether(scaleX, scaleY, fadeOut)
            animatorSet.duration = 400

            animatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    it.visibility = View.GONE
                }
            })
            animatorSet.start()
        }
    }

    private fun onCardClicked(card: Card) {
        if (isProcessing || card.isFaceUp || card.isMatched) return
        if (firstSelectedCard == card) return

        card.flip()

        if (firstSelectedCard == null) {
            firstSelectedCard = card
        } else {
            val secondCard = card
            checkMatch(firstSelectedCard!!, secondCard)
            firstSelectedCard = null
        }
    }

    private fun checkMatch(card1: Card, card2: Card) {
        isProcessing = true
        if (card1.pairId == card2.pairId) {
            card1.isMatched = true
            card2.isMatched = true
            gameManager.addScore()

            dataManager.dailyMatches += 1
            if (dataManager.dailyMatches == 50) {
                dataManager.totalCoins += 500
                showFrenzyMessage("GÖREV: +500 🪙")
            }

            callback.onScoreChanged(gameManager.score)
            soundManager.playMatchSound()

            if (gameManager.comboCount > 1) {
                showFrenzyMessage("x${gameManager.comboCount} KOMBO!")
                if (gameManager.comboCount == 5 && !dataManager.achievementComboX5) {
                    dataManager.achievementComboX5 = true
                    showFrenzyMessage("BAŞARIM: X5 KOMBO!")
                }
            }

            if (card1.isPowerUp) {
                triggerPowerUp(card1.powerUpType)
            }

            card1.animateMatchPulse()
            card2.animateMatchPulse {
                animateMatchAndApplyGravity(card1, card2)
            }

        } else {
            gameManager.resetCombo()

            Handler(Looper.getMainLooper()).postDelayed({
                soundManager.playMismatchSound()
                card1.animateMismatch()
                card2.animateMismatch {
                    card1.flip()
                    card2.flip()
                    isProcessing = false
                }
            }, 600)
        }
    }

    private fun triggerPowerUp(type: String) {
        if (type == "RADAR") {
            showFrenzyMessage("RADAR AKTİF!")
            cards.filter { !it.isMatched && !it.isFaceUp }.forEach {
                it.peek()
            }
        } else if (type == "BOMB") {
            showFrenzyMessage("BOMBA PATLADI!")
            if (!dataManager.achievementFirstBomb) {
                dataManager.achievementFirstBomb = true
                showFrenzyMessage("BAŞARIM: Bombacı!")
            }
            val unmatched = cards.filter { !it.isMatched }
            if (unmatched.isNotEmpty()) {
                val pairIdToFind = unmatched.random().pairId
                val targetPair = unmatched.filter { it.pairId == pairIdToFind }

                if (targetPair.size == 2) {
                    targetPair[0].isMatched = true
                    targetPair[1].isMatched = true

                    targetPair[0].animateMatchPulse()
                    targetPair[1].animateMatchPulse {
                        animateMatchAndApplyGravity(targetPair[0], targetPair[1])
                    }
                }
            }
        }
    }

    private fun animateMatchAndApplyGravity(card1: Card, card2: Card) {
        val boardWidth = gameBoard.width
        val boardHeight = gameBoard.height

        val centerX = (boardWidth / 2f) - (card1.layoutParams.width / 2f)
        val centerY = (boardHeight / 2f) - (card1.layoutParams.height / 2f)

        val moveX1 = ObjectAnimator.ofFloat(card1, "translationX", centerX - card1.x)
        val moveY1 = ObjectAnimator.ofFloat(card1, "translationY", centerY - card1.y)

        val moveX2 = ObjectAnimator.ofFloat(card2, "translationX", centerX - card2.x)
        val moveY2 = ObjectAnimator.ofFloat(card2, "translationY", centerY - card2.y)

        val moveAnimatorSet = AnimatorSet()
        moveAnimatorSet.playTogether(moveX1, moveY1, moveX2, moveY2)
        moveAnimatorSet.duration = 400

        moveAnimatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                card1.setMatchedAndHide()
                card2.setMatchedAndHide {
                    applyGravity(card1, card2)
                }
            }
        })
        moveAnimatorSet.start()
    }

    private fun applyGravity(matchedCard1: Card, matchedCard2: Card) {
        val columnsToUpdate = listOf(matchedCard1.gridCol, matchedCard2.gridCol).distinct()

        var animationCount = 0
        var completedCount = 0

        val totalCols = if (gameMode == "SURVIVAL") 4 else ceil(sqrt(gameManager.getCardCountForLevel().toDouble())).toInt()
        val totalRows = if (gameMode == "SURVIVAL") 6 else ceil(gameManager.getCardCountForLevel().toDouble() / totalCols).toInt()

        val spacing = 16
        val cardHeight = (gameBoard.height - (totalRows + 1) * spacing) / totalRows

        for (col in columnsToUpdate) {
            val columnCards = cards.filter { it.gridCol == col && !it.isMatched }
                .sortedByDescending { it.gridRow }

            var targetRow = totalRows - 1

            for (card in columnCards) {
                if (card.gridRow != targetRow) {
                    val oldRow = card.gridRow
                    val rowsToFall = targetRow - oldRow
                    val fallDistance = rowsToFall * (cardHeight + spacing)

                    card.gridRow = targetRow

                    val fallAnim = ObjectAnimator.ofFloat(card, "translationY", 0f, fallDistance.toFloat())
                    fallAnim.duration = 300

                    animationCount++
                    fallAnim.addListener(object: AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            val lp = card.layoutParams as FrameLayout.LayoutParams
                            lp.topMargin += fallDistance
                            card.layoutParams = lp
                            card.translationY = 0f

                            completedCount++
                            if (completedCount == animationCount) {
                                checkLevelComplete()
                                isProcessing = false
                            }
                        }
                    })
                    fallAnim.start()
                }
                targetRow--
            }
        }

        if (animationCount == 0) {
            checkLevelComplete()
            isProcessing = false
        }
    }

    private fun checkLevelComplete() {
        if (gameMode == "SURVIVAL") return // Survival bitmez, game over olana kadar

        if (cards.all { it.isMatched }) {

            // Oyun sonu jeton ve rekor kaydi
            dataManager.totalCoins += gameManager.score / 2
            if (gameManager.score > dataManager.highScore) {
                dataManager.highScore = gameManager.score
            }

            gameManager.nextLevel()
            if (gameManager.currentLevel > 14) {
                callback.onGameFinished()
            } else {
                callback.onLevelChanged(gameManager.currentLevel)
                loadLevel()
            }
        }
    }
}
