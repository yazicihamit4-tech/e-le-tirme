package com.yazhamit.eslestirme

import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation

class Card(val id: Int, val pairId: Int, val colorIndex: Int) {
    var isFaceUp: Boolean = false
    var isMatched: Boolean = false
    var node: ModelNode? = null
    var startPosition: Position = Position()

    fun flip() {
        isFaceUp = !isFaceUp
        node?.let {
            if (isFaceUp) {
                // Burada dönme veya model değiştirme animasyonu olmalı.
                // ModelNode ile renkleri ayırt edemediğimiz için şimdilik loglarda ve state üzerinde ilerliyor.
                // Gerçek senaryoda burada model değişir veya Material rengi güncellenir.
                it.rotation = Rotation(x = 0f, y = 180f, z = 0f)
            } else {
                it.rotation = Rotation(x = 0f, y = 0f, z = 0f)
            }
        }
    }

    fun hide() {
        // Hide card logic
        node?.isVisible = false
    }
}
