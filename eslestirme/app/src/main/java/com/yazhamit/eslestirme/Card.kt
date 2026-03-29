package com.yazhamit.eslestirme

import io.github.sceneview.node.ModelNode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation

class Card(val id: Int, val pairId: Int, val symbol: String) {
    var isFaceUp: Boolean = false
    var isMatched: Boolean = false
    var node: ModelNode? = null
    var startPosition: Position = Position()

    fun flip() {
        isFaceUp = !isFaceUp

        node?.let {
            if (isFaceUp) {
                // Burada aslinda glb modelin sembol olan yuzunu dondurmesi gerekiyor.
                it.rotation = Rotation(x = 0f, y = 180f, z = 0f)
            } else {
                it.rotation = Rotation(x = 0f, y = 0f, z = 0f)
            }
        }
    }

    fun hide() {
        node?.isVisible = false
    }
}
