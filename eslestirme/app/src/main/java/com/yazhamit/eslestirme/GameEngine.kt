package com.yazhamit.eslestirme

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.FrameLayout
import kotlin.math.ceil
import kotlin.math.sqrt

class GameEngine(private val context: Context, private val gameBoard: FrameLayout, private val callback: GameCallback) {
    private val gameManager = GameManager()
    private val cards = mutableListOf<Card>()
    private var firstSelectedCard: Card? = null
    private var isProcessing = false

    private val cardSymbols = listOf("★", "♥", "♦", "♣", "♠", "▲", "▼", "◆", "●", "■", "△", "▽", "◇", "○", "□")

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
        // Ekrandaki önceki kartları temizle
        gameBoard.removeAllViews()
        cards.clear()

        val cardCount = gameManager.getCardCountForLevel()
        val pairsCount = cardCount / 2

        for (i in 0 until pairsCount) {
            val symbol = cardSymbols[i % cardSymbols.size]
            cards.add(Card(context, cardId = i * 2, pairId = i, symbol = symbol))
            cards.add(Card(context, cardId = i * 2 + 1, pairId = i, symbol = symbol))
        }

        cards.shuffle()

        // 2D grid hesaplaması (sutun/satır)
        val columns = ceil(sqrt(cardCount.toDouble())).toInt()
        val rows = ceil(cardCount.toDouble() / columns).toInt()

        gameBoard.post {
            val boardWidth = gameBoard.width
            val boardHeight = gameBoard.height

            val spacing = 16 // piksel
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

    private fun onCardClicked(card: Card) {
        if (isProcessing || card.isFaceUp || card.isMatched) return

        // Double-tap bug'ını onlemek icin:
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

            // Birleşme ve yok olma animasyonu
            animateMatch(card1, card2)

        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                card1.flip()
                card2.flip()
                isProcessing = false
            }, 1000)
        }
    }

    private fun animateMatch(card1: Card, card2: Card) {
        // Kartları ekranın ortasına hareket ettirip (Translation X,Y) daha sonra boyutlarını sıfırlayarak kaybedeceğiz.
        val boardWidth = gameBoard.width
        val boardHeight = gameBoard.height

        val centerX = (boardWidth / 2f) - (card1.layoutParams.width / 2f)
        val centerY = (boardHeight / 2f) - (card1.layoutParams.height / 2f)

        // Card1 merkeze gider
        val moveX1 = ObjectAnimator.ofFloat(card1, "translationX", centerX - card1.x)
        val moveY1 = ObjectAnimator.ofFloat(card1, "translationY", centerY - card1.y)

        // Card2 merkeze gider
        val moveX2 = ObjectAnimator.ofFloat(card2, "translationX", centerX - card2.x)
        val moveY2 = ObjectAnimator.ofFloat(card2, "translationY", centerY - card2.y)

        val moveAnimatorSet = AnimatorSet()
        moveAnimatorSet.playTogether(moveX1, moveY1, moveX2, moveY2)
        moveAnimatorSet.duration = 400

        moveAnimatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Merkeze geldiklerinde yok olma animasyonunu tetikle
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
