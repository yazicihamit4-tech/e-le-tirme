package com.yazhamit.eslestirme

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.os.Handler
import android.os.Looper
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.sqrt

class GameEngine(private val context: Context, private val sceneView: SceneView, private val callback: GameCallback) {
    private val gameManager = GameManager()
    private val cards = mutableListOf<Card>()
    private var firstSelectedCard: Card? = null
    private var isProcessing = false

    // Biz primitive olarak cube.glb ve uzerinde islem yapamadıgımız icin state ile oynayacagiz (Kullanici modeli degistirmeli).
    // Android ViewNode renderable builder patterni 0.10.0'da kolayca desteklenmedigi veya signature degistigi icin ModelNode'da tutuyoruz
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
        cards.forEach { it.node?.destroy() }
        cards.clear()

        val cardCount = gameManager.getCardCountForLevel()
        val pairsCount = cardCount / 2

        for (i in 0 until pairsCount) {
            val symbol = cardSymbols[i % cardSymbols.size]
            cards.add(Card(id = i * 2, pairId = i, symbol = symbol))
            cards.add(Card(id = i * 2 + 1, pairId = i, symbol = symbol))
        }

        cards.shuffle()

        val columns = ceil(sqrt(cardCount.toDouble())).toInt()
        val rows = ceil(cardCount.toDouble() / columns).toInt()

        val spacing = 1.2f
        val startX = -(columns - 1) * spacing / 2f
        val startY = (rows - 1) * spacing / 2f

        CoroutineScope(Dispatchers.Main).launch {
            for (i in 0 until cards.size) {
                val card = cards[i]
                val row = i / columns
                val col = i % columns

                val x = startX + col * spacing
                val y = startY - row * spacing
                val z = -6.0f

                card.startPosition = Position(x, y, z)

                val modelNode = ModelNode(sceneView.engine).apply {
                    position = card.startPosition
                }

                modelNode.onTap = { _, _ ->
                    onCardClicked(card)
                }

                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        modelNode.loadModelGlbAsync(
                            glbFileLocation = "cube.glb"
                        )
                        // TODO: SceneView 0.10.0'da bir modelin belli bir MaterialInstance'ina ulasip rengini degistirebilirsek
                        // farkli sembolleri temsilen farkli renkler verecegiz. Aksi halde, kullanici modelleri
                        // assets icine "card_0.glb", "card_1.glb" koymali. Biz mantik olarak "cube.glb" uzerinden isliyoruz.
                    } catch(e: Exception) {
                        e.printStackTrace()
                    }
                }

                card.node = modelNode
                sceneView.addChild(modelNode)
            }
        }
    }

    fun handleTap(node: Node?) {
        if (node == null) return
        val clickedCard = cards.find { it.node == node }
        if (clickedCard != null) {
            onCardClicked(clickedCard)
        }
    }

    private fun onCardClicked(card: Card) {
        if (isProcessing || card.isFaceUp || card.isMatched) return

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
        val node1 = card1.node ?: return
        val node2 = card2.node ?: return

        val centerX = (card1.startPosition.x + card2.startPosition.x) / 2f
        val centerY = (card1.startPosition.y + card2.startPosition.y) / 2f
        val centerZ = (card1.startPosition.z + card2.startPosition.z) / 2f

        val upY = centerY + 1.5f
        val downZ = centerZ - 5f

        val animatorSet = AnimatorSet()

        val moveCenterAnimatorX1 = ValueAnimator.ofFloat(card1.startPosition.x, centerX)
        val moveCenterAnimatorY1 = ValueAnimator.ofFloat(card1.startPosition.y, centerY)
        val moveCenterAnimatorZ1 = ValueAnimator.ofFloat(card1.startPosition.z, centerZ)

        moveCenterAnimatorX1.addUpdateListener { vala -> node1.position = Position(vala.animatedValue as Float, node1.position.y, node1.position.z) }
        moveCenterAnimatorY1.addUpdateListener { vala -> node1.position = Position(node1.position.x, vala.animatedValue as Float, node1.position.z) }
        moveCenterAnimatorZ1.addUpdateListener { vala -> node1.position = Position(node1.position.x, node1.position.y, vala.animatedValue as Float) }

        val moveCenterAnimatorX2 = ValueAnimator.ofFloat(card2.startPosition.x, centerX)
        val moveCenterAnimatorY2 = ValueAnimator.ofFloat(card2.startPosition.y, centerY)
        val moveCenterAnimatorZ2 = ValueAnimator.ofFloat(card2.startPosition.z, centerZ)

        moveCenterAnimatorX2.addUpdateListener { vala -> node2.position = Position(vala.animatedValue as Float, node2.position.y, node2.position.z) }
        moveCenterAnimatorY2.addUpdateListener { vala -> node2.position = Position(node2.position.x, vala.animatedValue as Float, node2.position.z) }
        moveCenterAnimatorZ2.addUpdateListener { vala -> node2.position = Position(node2.position.x, node2.position.y, vala.animatedValue as Float) }

        val moveUpAnimator = ValueAnimator.ofFloat(centerY, upY)
        val moveBackAnimator = ValueAnimator.ofFloat(centerZ, downZ)

        moveUpAnimator.addUpdateListener { vala ->
            val newY = vala.animatedValue as Float
            node1.position = Position(node1.position.x, newY, node1.position.z)
            node2.position = Position(node2.position.x, newY, node2.position.z)
        }

        moveBackAnimator.addUpdateListener { vala ->
            val newZ = vala.animatedValue as Float
            node1.position = Position(node1.position.x, node1.position.y, newZ)
            node2.position = Position(node2.position.x, node2.position.y, newZ)
        }

        animatorSet.playTogether(moveCenterAnimatorX1, moveCenterAnimatorY1, moveCenterAnimatorZ1, moveCenterAnimatorX2, moveCenterAnimatorY2, moveCenterAnimatorZ2)

        val moveOutSet = AnimatorSet()
        moveOutSet.playTogether(moveUpAnimator, moveBackAnimator)

        val finalAnimatorSet = AnimatorSet()
        finalAnimatorSet.playSequentially(animatorSet, moveOutSet)
        finalAnimatorSet.duration = 400

        finalAnimatorSet.start()

        Handler(Looper.getMainLooper()).postDelayed({
            card1.hide()
            card2.hide()
            checkLevelComplete()
            isProcessing = false
        }, 1000)
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
