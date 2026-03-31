package com.yazhamit.eslestirme

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView

class Card(context: Context, val cardId: Int, var pairId: Int, var symbol: String) : FrameLayout(context) {

    var isFaceUp: Boolean = false
    var isMatched: Boolean = false
    var isPowerUp: Boolean = false
    var powerUpType: String = "" // "RADAR" veya "BOMB"

    var gridRow: Int = 0
    var gridCol: Int = 0

    private val textView: TextView

    init {
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
                    if (isPowerUp) {
                        setBackgroundColor(Color.YELLOW)
                    } else {
                        setBackgroundResource(R.drawable.card_front)
                    }
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

    fun peek() {
        if (isMatched || isFaceUp) return

        val peekOut = ObjectAnimator.ofFloat(this, "rotationY", 0f, 90f).setDuration(150)
        val peekIn = ObjectAnimator.ofFloat(this, "rotationY", -90f, 0f).setDuration(150)

        val hideOut = ObjectAnimator.ofFloat(this, "rotationY", 0f, 90f).apply { startDelay = 1000; duration = 150 }
        val hideIn = ObjectAnimator.ofFloat(this, "rotationY", -90f, 0f).setDuration(150)

        peekOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                setBackgroundResource(R.drawable.card_front)
                textView.text = symbol
                peekIn.start()
            }
        })

        peekIn.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                hideOut.start()
            }
        })

        hideOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                setBackgroundResource(R.drawable.card_back)
                textView.text = ""
                hideIn.start()
            }
        })

        peekOut.start()
    }

    fun morphSymbol(newSymbol: String, newPairId: Int) {
        if (isMatched || isFaceUp) return

        val shake = ObjectAnimator.ofFloat(this, "rotation", 0f, 15f, -15f, 10f, -10f, 0f)
        shake.duration = 500
        shake.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                symbol = newSymbol
                pairId = newPairId
            }
        })
        shake.start()
    }

    fun animateMismatch(onEnd: (() -> Unit)? = null) {
        val animator = ObjectAnimator.ofFloat(this, "translationX", 0f, 20f, -20f, 20f, -20f, 10f, -10f, 0f)
        animator.duration = 400
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onEnd?.invoke()
            }
        })
        animator.start()
    }

    fun animateMatchPulse(onEnd: (() -> Unit)? = null) {
        val scaleX = ObjectAnimator.ofFloat(this, "scaleX", 1f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(this, "scaleY", 1f, 1.2f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY)
        animatorSet.duration = 300
        animatorSet.interpolator = OvershootInterpolator()

        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onEnd?.invoke()
            }
        })
        animatorSet.start()
    }

    fun setMatchedAndHide(onEnd: (() -> Unit)? = null) {
        val animatorSet = AnimatorSet()
        val scaleX = ObjectAnimator.ofFloat(this, "scaleX", 1f, 0f)
        val scaleY = ObjectAnimator.ofFloat(this, "scaleY", 1f, 0f)
        val alpha = ObjectAnimator.ofFloat(this, "alpha", 1f, 0f)

        animatorSet.playTogether(scaleX, scaleY, alpha)
        animatorSet.duration = 400
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                visibility = GONE
                onEnd?.invoke()
            }
        })
        animatorSet.start()
    }
}
