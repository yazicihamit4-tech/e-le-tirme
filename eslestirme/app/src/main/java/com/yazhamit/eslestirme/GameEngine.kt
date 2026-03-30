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
    private val cards = mutableListOf<Card>()
    private var firstSelectedCard: Card? = null
    private var isProcessing = false

    val soundManager = SoundManager(context)

    private val cardSymbols = listOf("★", "♥", "♦", "♣", "♠", "▲", "▼", "◆", "●", "■", "△", "▽", "◇", "○", "□")
    private val powerUpSymbols = listOf("👁️", "💣")

    // UI'da kombo ve power-up bildirimi icin MainActivity'den gelecek TextView
    var powerUpTextView: TextView? = null

    interface GameCallback {
        fun onScoreChanged(score: Int)
        fun onLevelChanged(level: Int)
        fun onGameFinished()
    }

    fun startGame() {
        gameManager.reset()
        callback.onLevelChanged(gameManager.currentLevel)
        callback.onScoreChanged(gameManager.score)
        loadLevel()
    }

    private fun loadLevel() {
        gameBoard.removeAllViews()
        cards.clear()

        val cardCount = gameManager.getCardCountForLevel()
        val pairsCount = cardCount / 2

        var hasPowerUp = false
        // %30 ihtimalle bu seviyede bir power-up olabilir.
        if (Random.nextInt(100) < 30 && pairsCount > 3) {
            hasPowerUp = true
        }

        // Kart çiftleri oluşturma
        for (i in 0 until pairsCount) {
            if (hasPowerUp && i == pairsCount - 1) { // Son cifti Power-Up yapiyoruz
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

        val columns = ceil(sqrt(cardCount.toDouble())).toInt()
        val rows = ceil(cardCount.toDouble() / columns).toInt()

        gameBoard.post {
            val boardWidth = gameBoard.width
            val boardHeight = gameBoard.height

            val spacing = 16
            val cardWidth = (boardWidth - (columns + 1) * spacing) / columns
            val cardHeight = (boardHeight - (rows + 1) * spacing) / rows

            for (i in 0 until cards.size) {
                val card = cards[i]
                val row = i / columns
                val col = i % columns

                val x = spacing + col * (cardWidth + spacing)
                val y = spacing + row * (cardHeight + spacing)

                val layoutParams = FrameLayout.LayoutParams(cardWidth, cardHeight).apply {
                    leftMargin = x
                    topMargin = y
                }

                card.layoutParams = layoutParams
                card.setOnClickListener {
                    onCardClicked(card)
                }

                gameBoard.addView(card)
            }
        }
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
            fadeOut.startDelay = 800 // Biraz beklesin

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
            callback.onScoreChanged(gameManager.score)

            soundManager.playMatchSound()

            // Kombo tetiklemesi kontrolu
            if (gameManager.comboCount > 1) {
                showFrenzyMessage("x${gameManager.comboCount} KOMBO!")
            }

            // Ozel Guc (Power-Up) kontrolu
            if (card1.isPowerUp) {
                triggerPowerUp(card1.powerUpType)
            }

            card1.animateMatchPulse()
            card2.animateMatchPulse {
                animateMatch(card1, card2)
            }

        } else {
            // Mismatch durumunda komboyu resetliyoruz
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
            // Ekranda eşleşmemiş tüm kartları 1 saniyeliğine göster
            cards.filter { !it.isMatched && !it.isFaceUp }.forEach {
                it.peek()
            }
        } else if (type == "BOMB") {
            showFrenzyMessage("BOMBA PATLADI!")
            // Eşleşmemiş rastgele bir çift bul
            val unmatched = cards.filter { !it.isMatched }
            if (unmatched.isNotEmpty()) {
                val pairIdToFind = unmatched.random().pairId
                val targetPair = unmatched.filter { it.pairId == pairIdToFind }

                if (targetPair.size == 2) {
                    targetPair[0].isMatched = true
                    targetPair[1].isMatched = true

                    // Bomb match efektleri
                    targetPair[0].animateMatchPulse()
                    targetPair[1].animateMatchPulse {
                        animateMatch(targetPair[0], targetPair[1])
                    }
                }
            }
        }
    }

    private fun animateMatch(card1: Card, card2: Card) {
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
                    checkLevelComplete()
                    isProcessing = false
                }
            }
        })
        moveAnimatorSet.start()
    }

    private fun checkLevelComplete() {
        if (cards.all { it.isMatched }) {
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
