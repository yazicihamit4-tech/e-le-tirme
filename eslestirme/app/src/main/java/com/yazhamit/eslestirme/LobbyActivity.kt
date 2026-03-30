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
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.random.Random

class LobbyActivity : AppCompatActivity() {

    private lateinit var playButton: Button
    private lateinit var survivalButton: Button
    private lateinit var questsButton: Button
    private lateinit var animatedBackground: FrameLayout
    private lateinit var coinsTextView: TextView
    private lateinit var highScoreTextView: TextView

    private val symbols = listOf("★", "♥", "♦", "♣", "♠", "▲", "▼", "◆", "●", "■", "△", "▽", "◇", "○", "□")
    private val handler = Handler(Looper.getMainLooper())
    private var isAnimating = true

    private lateinit var dataManager: DataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lobby)

        dataManager = DataManager(this)
        dataManager.checkAndResetDailyTasks()

        playButton = findViewById(R.id.playButton)
        survivalButton = findViewById(R.id.survivalButton)
        questsButton = findViewById(R.id.questsButton)
        animatedBackground = findViewById(R.id.animatedBackground)
        coinsTextView = findViewById(R.id.coinsTextView)
        highScoreTextView = findViewById(R.id.highScoreTextView)

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
            intent.putExtra("GAME_MODE", "CLASSIC")
            startActivity(intent)
            finish()
        }

        survivalButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("GAME_MODE", "SURVIVAL")
            startActivity(intent)
            finish()
        }

        questsButton.setOnClickListener {
            val totalNeeded = 50
            val current = dataManager.dailyMatches

            if (current >= totalNeeded) {
                // TODO: Odulu bir defa almak icin bir flag de tutulabilir
                Toast.makeText(this, "Günlük Görev Zaten Tamamlandı! \n(50/50 Kart Eşleşti)", Toast.LENGTH_LONG).show()
            } else {
                val remaining = totalNeeded - current
                Toast.makeText(this, "Günlük Görev:\n50 Kart Eşleştir.\nKalan: $remaining\nÖdül: 500 🪙", Toast.LENGTH_LONG).show()
            }
        }

        startFloatingSymbols()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        coinsTextView.text = dataManager.totalCoins.toString()
        highScoreTextView.text = dataManager.highScore.toString()
    }

    private fun startFloatingSymbols() {
        val runnable = object : Runnable {
            override fun run() {
                if (!isAnimating) return

                spawnFloatingSymbol()
                handler.postDelayed(this, 800)
            }
        }
        handler.post(runnable)
    }

    private fun spawnFloatingSymbol() {
        val textView = TextView(this).apply {
            text = symbols[Random.nextInt(symbols.size)]
            textSize = Random.nextInt(24, 64).toFloat()
            setTextColor(android.graphics.Color.argb(Random.nextInt(50, 150), 255, 64, 129))
            gravity = Gravity.CENTER
        }

        val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        animatedBackground.addView(textView, params)

        animatedBackground.post {
            val width = animatedBackground.width
            val height = animatedBackground.height
            if (width == 0 || height == 0) return@post

            val startX = Random.nextInt(0, width).toFloat()
            val startY = height.toFloat() + 100f
            val endY = -100f

            textView.x = startX
            textView.y = startY

            val floatUp = ObjectAnimator.ofFloat(textView, "translationY", startY, endY)
            floatUp.duration = Random.nextLong(4000, 8000)

            val rotate = ObjectAnimator.ofFloat(textView, "rotation", 0f, Random.nextInt(180, 360).toFloat())
            rotate.duration = floatUp.duration

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
