package com.yazhamit.eslestirme

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView

class Card(context: Context, val cardId: Int, val pairId: Int, val symbol: String) : FrameLayout(context) {

    var isFaceUp: Boolean = false
    var isMatched: Boolean = false

    private val textView: TextView

    init {
        // Kartın ön yüzü ve arka yüzü ayarları
        setBackgroundResource(R.drawable.card_back)

        textView = TextView(context).apply {
            text = ""
            textSize = 32f
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
        }

        addView(textView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun flip() {
        if (isMatched) return

        val flipOut = ObjectAnimator.ofFloat(this, "rotationY", 0f, 90f)
        flipOut.duration = 150

        val flipIn = ObjectAnimator.ofFloat(this, "rotationY", -90f, 0f)
        flipIn.duration = 150

        flipOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                isFaceUp = !isFaceUp
                if (isFaceUp) {
                    setBackgroundResource(R.drawable.card_front)
                    textView.text = symbol
                } else {
                    setBackgroundResource(R.drawable.card_back)
                    textView.text = ""
                }
                flipIn.start()
            }
        })

        flipOut.start()
    }

    fun setMatchedAndHide(onEnd: (() -> Unit)? = null) {
        val animatorSet = AnimatorSet()

        // Animasyonla kaybolacak (örnek: scale küçülerek yok olma ve saydamlaşma)
        val scaleX = ObjectAnimator.ofFloat(this, "scaleX", 1f, 0f)
        val scaleY = ObjectAnimator.ofFloat(this, "scaleY", 1f, 0f)
        val alpha = ObjectAnimator.ofFloat(this, "alpha", 1f, 0f)

        animatorSet.playTogether(scaleX, scaleY, alpha)
        animatorSet.duration = 500
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                visibility = GONE
                onEnd?.invoke()
            }
        })
        animatorSet.start()
    }
}
