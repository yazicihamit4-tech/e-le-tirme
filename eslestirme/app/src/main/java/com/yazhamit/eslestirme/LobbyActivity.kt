package com.yazhamit.eslestirme

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.random.Random

class LobbyActivity : AppCompatActivity() {

    private lateinit var playButton: Button
    private lateinit var animatedBackground: FrameLayout
    private val symbols = listOf("★", "♥", "♦", "♣", "♠", "▲", "▼", "◆", "●", "■", "△", "▽", "◇", "○", "□")
    private val handler = Handler(Looper.getMainLooper())
    private var isAnimating = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lobby)

        playButton = findViewById(R.id.playButton)
        animatedBackground = findViewById(R.id.animatedBackground)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.lobby_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Play Button Pulse Animation
        val scaleX = ObjectAnimator.ofFloat(playButton, "scaleX", 1f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(playButton, "scaleY", 1f, 1.1f, 1f)
        scaleX.repeatCount = ObjectAnimator.INFINITE
        scaleY.repeatCount = ObjectAnimator.INFINITE
        scaleX.duration = 1000
        scaleY.duration = 1000

        val pulseAnimator = AnimatorSet()
        pulseAnimator.playTogether(scaleX, scaleY)
        pulseAnimator.start()

        playButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Lobby'i kapatıyoruz
        }

        // Arka plan hareketli simgeler başlatılıyor
        startFloatingSymbols()
    }

    private fun startFloatingSymbols() {
        val runnable = object : Runnable {
            override fun run() {
                if (!isAnimating) return

                spawnFloatingSymbol()
                handler.postDelayed(this, 800) // Her 800ms'de bir sembol çıkar
            }
        }
        handler.post(runnable)
    }

    private fun spawnFloatingSymbol() {
        val textView = TextView(this).apply {
            text = symbols[Random.nextInt(symbols.size)]
            textSize = Random.nextInt(24, 64).toFloat()
            setTextColor(android.graphics.Color.argb(Random.nextInt(50, 150), 255, 64, 129)) // Yari saydam, rastgele tonlarda pembe
            gravity = Gravity.CENTER
        }

        val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        animatedBackground.addView(textView, params)

        // Rastgele X pozisyonu (Ekran genişliğine göre hesaplayalım)
        animatedBackground.post {
            val width = animatedBackground.width
            val height = animatedBackground.height
            if (width == 0 || height == 0) return@post

            val startX = Random.nextInt(0, width).toFloat()
            val startY = height.toFloat() + 100f
            val endY = -100f

            textView.x = startX
            textView.y = startY

            // Yukarı doğru süzülme animasyonu
            val floatUp = ObjectAnimator.ofFloat(textView, "translationY", startY, endY)
            floatUp.duration = Random.nextLong(4000, 8000)

            // Yavaşça dönerken süzülme (Rotation)
            val rotate = ObjectAnimator.ofFloat(textView, "rotation", 0f, Random.nextInt(180, 360).toFloat())
            rotate.duration = floatUp.duration

            // X ekseninde hafifçe dalgalanma
            val driftX = ObjectAnimator.ofFloat(textView, "translationX", startX, startX + Random.nextInt(-50, 50))
            driftX.duration = floatUp.duration

            val animatorSet = AnimatorSet()
            animatorSet.playTogether(floatUp, rotate, driftX)
            animatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    animatedBackground.removeView(textView)
                }
            })
            animatorSet.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isAnimating = false
        handler.removeCallbacksAndMessages(null)
    }
}
