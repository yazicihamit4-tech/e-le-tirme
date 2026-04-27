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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.random.Random

class LobbyActivity : AppCompatActivity() {

    private lateinit var playButton: Button
    private lateinit var survivalButton: Button
    private lateinit var shopButton: Button
    private lateinit var questsButton: Button
    private lateinit var animatedBackground: FrameLayout
    private lateinit var coinsTextView: TextView
    private lateinit var highScoreTextView: TextView
    private lateinit var livesTextView: TextView

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
        dataManager.checkAndRestoreLives()

        playButton = findViewById(R.id.playButton)
        survivalButton = findViewById(R.id.survivalButton)
        shopButton = findViewById(R.id.shopButton)
        questsButton = findViewById(R.id.questsButton)
        animatedBackground = findViewById(R.id.animatedBackground)
        coinsTextView = findViewById(R.id.coinsTextView)
        highScoreTextView = findViewById(R.id.highScoreTextView)
        livesTextView = findViewById(R.id.livesTextView)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.lobby_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        updateUI()

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
            startGameWithLivesCheck("CLASSIC")
        }

        survivalButton.setOnClickListener {
            startGameWithLivesCheck("SURVIVAL")
        }

        shopButton.setOnClickListener {
            showShopDialog()
        }

        questsButton.setOnClickListener {
            val totalNeeded = 50
            val current = dataManager.dailyMatches

            if (current >= totalNeeded) {
                Toast.makeText(this, "Günlük Görev Tamamlandı! \n(50/50 Kart Eşleşti)", Toast.LENGTH_LONG).show()
            } else {
                val remaining = totalNeeded - current
                Toast.makeText(this, "Günlük Görev:\n50 Kart Eşleştir.\nKalan: $remaining\nÖdül: 500 🪙", Toast.LENGTH_LONG).show()
            }
        }

        startFloatingSymbols()
    }

    private fun startGameWithLivesCheck(mode: String) {
        dataManager.checkAndRestoreLives() // Tekrar kontrol et
        if (dataManager.lives > 0) {
            dataManager.lives -= 1
            updateUI()

            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("GAME_MODE", mode)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Canınız bitti! Biraz bekleyin veya mağazadan can alın.", Toast.LENGTH_LONG).show()
        }
    }

    private fun showShopDialog() {
        val themes = arrayOf("KLASİK (Ücretsiz)", "HAYVANLAR (1000 🪙)", "MEYVELER (1000 🪙)", "EMOJİLER (1500 🪙)", "CAN DOLDUR (+5 ❤️ / 500 🪙)")
        val themeKeys = arrayOf("CLASSIC", "ANIMALS", "FRUITS", "EMOJIS", "HEAL")
        val themePrices = arrayOf(0, 1000, 1000, 1500, 500)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Mağaza (Bakiye: ${dataManager.totalCoins} 🪙)")

        builder.setItems(themes) { dialog, which ->
            val selectedKey = themeKeys[which]
            val price = themePrices[which]

            if (selectedKey == "HEAL") {
                if (dataManager.totalCoins >= price) {
                    if (dataManager.lives < 5) {
                        dataManager.totalCoins -= price
                        dataManager.lives = 5
                        updateUI()
                        Toast.makeText(this, "Canlar dolduruldu! ❤️", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Canınız zaten dolu!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Yetersiz Jeton! 🪙", Toast.LENGTH_SHORT).show()
                }
                return@setItems
            }

            // Tema islemi
            if (dataManager.isThemeUnlocked(selectedKey)) {
                dataManager.selectedTheme = selectedKey
                Toast.makeText(this, "Tema seçildi!", Toast.LENGTH_SHORT).show()
            } else {
                if (dataManager.totalCoins >= price) {
                    dataManager.totalCoins -= price
                    dataManager.unlockTheme(selectedKey)
                    dataManager.selectedTheme = selectedKey
                    updateUI()
                    Toast.makeText(this, "Tema satın alındı ve seçildi! 🎉", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Yetersiz Jeton! 🪙", Toast.LENGTH_SHORT).show()
                }
            }
        }

        builder.setNegativeButton("Kapat") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    override fun onResume() {
        super.onResume()
        dataManager.checkAndRestoreLives()
        updateUI()
    }

    private fun updateUI() {
        coinsTextView.text = dataManager.totalCoins.toString()
        highScoreTextView.text = dataManager.highScore.toString()
        livesTextView.text = "${dataManager.lives}/5"
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
