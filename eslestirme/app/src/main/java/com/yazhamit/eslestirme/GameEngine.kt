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

    // Zaman Kaymasi (Morphing) Handler
    private val morphHandler = Handler(Looper.getMainLooper())
    private val morphRunnable = object : Runnable {
        override fun run() {
            triggerMorphing()
            // Her 15-20 saniyede bir tetikle
            morphHandler.postDelayed(this, Random.nextLong(15000, 20000))
        }
    }

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

        // Önceki seviyeden kalan morph timer'ı temizle
        morphHandler.removeCallbacksAndMessages(null)

        val cardCount = gameManager.getCardCountForLevel()
        val pairsCount = cardCount / 2

        var hasPowerUp = false
        if (Random.nextInt(100) < 30 && pairsCount > 3) {
            hasPowerUp = true
        }

        // Kart çiftleri oluşturma
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

                card.gridRow = row
                card.gridCol = col

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

            // Grid yüklendiğinde Morph timer başlat (Level 3 ten sonra zorluk olarak ekleyelim)
            if (gameManager.currentLevel >= 3) {
                morphHandler.postDelayed(morphRunnable, Random.nextLong(10000, 15000))
            }
        }
    }

    private fun triggerMorphing() {
        if (isProcessing) return

        // Eşleşmemiş, kapalı olan ve Özel Güç kartı olmayanları bul
        val availableCards = cards.filter { !it.isMatched && !it.isFaceUp && !it.isPowerUp }

        // Farklı pairId'lere sahip en az 2 farklı çift (4 kart) gereklidir
        val pairIds = availableCards.map { it.pairId }.distinct()
        if (pairIds.size >= 2) {
            // Rastgele iki çift (pairId) seçiyoruz
            val pairId1 = pairIds.random()
            val pairId2 = pairIds.filter { it != pairId1 }.random()

            val pair1Cards = availableCards.filter { it.pairId == pairId1 }
            val pair2Cards = availableCards.filter { it.pairId == pairId2 }

            if (pair1Cards.size == 2 && pair2Cards.size == 2) {
                val symbol1 = pair1Cards[0].symbol
                val symbol2 = pair2Cards[0].symbol

                // Sembolleri takas et (A -> B, B -> A)
                showFrenzyMessage("ZAMAN KAYMASI!")
                // Morph sound varsa çal (mismatch sound alternatif olabilir şimdilik)
                soundManager.playMismatchSound()

                pair1Cards.forEach { it.morphSymbol(symbol2, pairId2) }
                pair2Cards.forEach { it.morphSymbol(symbol1, pairId1) }
            }
        }
    }

    fun stopEngine() {
        morphHandler.removeCallbacksAndMessages(null)
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
            callback.onScoreChanged(gameManager.score)

            soundManager.playMatchSound()

            if (gameManager.comboCount > 1) {
                showFrenzyMessage("x${gameManager.comboCount} KOMBO!")
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

        // Kartlar bir kez merkeze kaydığı icin grid pozisyonlarindaki X/Y'ler artik geçerli değil
        // Sadece Grid indexlerini kullanarak yukarıdan aşağıya (Gravity) siralama mantigi:
        val cardCountForLevel = gameManager.getCardCountForLevel()
        val totalCols = ceil(sqrt(cardCountForLevel.toDouble())).toInt()
        val totalRows = ceil(cardCountForLevel.toDouble() / totalCols).toInt()

        val spacing = 16
        val cardHeight = (gameBoard.height - (totalRows + 1) * spacing) / totalRows

        for (col in columnsToUpdate) {
            val columnCards = cards.filter { it.gridCol == col && !it.isMatched }
                .sortedByDescending { it.gridRow }

            var targetRow = totalRows - 1

            for (card in columnCards) {
                if (card.gridRow != targetRow) {
                    val oldRow = card.gridRow
                    card.gridRow = targetRow

                    val newY = spacing + targetRow * (cardHeight + spacing)

                    // Card'ın translationY sini değil, doğrudan layout'un Y parametresini değiştiriyoruz
                    val fallAnim = ObjectAnimator.ofFloat(card, "y", card.y, newY.toFloat())
                    fallAnim.duration = 300

                    animationCount++
                    fallAnim.addListener(object: AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            val lp = card.layoutParams as FrameLayout.LayoutParams
                            lp.topMargin = newY
                            card.layoutParams = lp

                            // Animasyon bitiminde y eksenini resetlemeliyiz ki view'in kendi render loopunda
                            // asil yukseklik layoutMargin uzerinden okundugunda asagi dogru sekmeler olmasin
                            card.translationY = 0f
                            card.y = newY.toFloat()

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
