package com.yazhamit.eslestirme

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity(), GameEngine.GameCallback {

    private lateinit var mainLayout: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var gameBoard: FrameLayout
    private lateinit var levelTextView: TextView
    private lateinit var scoreTextView: TextView
    private lateinit var soundButton: ImageView
    private lateinit var gameEngine: GameEngine

    private var currentBgColor = Color.parseColor("#E3F2FD") // Baslangic rengi (Acik Mavi)

    // Seviyelere gore farkli renk paletleri
    private val levelColors = listOf(
        "#E3F2FD", // 1: Acik Mavi
        "#F3E5F5", // 2: Acik Mor
        "#E8F5E9", // 3: Acik Yesil
        "#FFF3E0", // 4: Acik Turuncu
        "#FFEBEE", // 5: Acik Pembe
        "#E0F7FA", // 6: Mint
        "#FFFDE7", // 7: Acik Turkuaz
        "#FBE9E7", // 8: Acik Sari
        "#EFEBE9", // 9: Acik Kahve
        "#FAFAFA"  // 10: Neredeyse Beyaz
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        mainLayout = findViewById(R.id.main)
        gameBoard = findViewById(R.id.gameBoard)
        levelTextView = findViewById(R.id.levelTextView)
        scoreTextView = findViewById(R.id.scoreTextView)
        soundButton = findViewById(R.id.soundButton)

        mainLayout.setBackgroundColor(currentBgColor)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        gameEngine = GameEngine(this, gameBoard, this)
        gameEngine.startGame()

        soundButton.setOnClickListener {
            gameEngine.soundManager.isMuted = !gameEngine.soundManager.isMuted

            if (gameEngine.soundManager.isMuted) {
                soundButton.setImageResource(R.drawable.ic_volume_off)
            } else {
                soundButton.setImageResource(R.drawable.ic_volume_up)
            }
        }
    }

    override fun onScoreChanged(score: Int) {
        scoreTextView.text = "Skor: $score"
    }

    override fun onLevelChanged(level: Int) {
        levelTextView.text = "Level: $level"

        // Arka plan rengini animasyonlu sekilde degistir
        val newColorStr = levelColors[(level - 1) % levelColors.size]
        val newColor = Color.parseColor(newColorStr)

        if (currentBgColor != newColor) {
            val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), currentBgColor, newColor)
            colorAnimation.duration = 1000 // 1 saniye icerisinde gecis yap
            colorAnimation.addUpdateListener { animator ->
                mainLayout.setBackgroundColor(animator.animatedValue as Int)
            }
            colorAnimation.start()
            currentBgColor = newColor
        }
    }

    override fun onGameFinished() {
        Toast.makeText(this, "Tebrikler! Oyunu tamamladınız.", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        gameEngine.soundManager.release()
    }
}
