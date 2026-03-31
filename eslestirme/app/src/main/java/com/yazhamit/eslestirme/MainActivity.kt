package com.yazhamit.eslestirme

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity(), GameEngine.GameCallback {

    private lateinit var mainLayout: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var gameBoard: FrameLayout
    private lateinit var levelTextView: TextView
    private lateinit var scoreTextView: TextView
    private lateinit var powerUpTextView: TextView
    private lateinit var soundButton: ImageView
    private lateinit var timerTextView: TextView
    private lateinit var bossBarContainer: LinearLayout
    private lateinit var gameEngine: GameEngine

    private var currentBgColor = Color.parseColor("#E3F2FD")

    private val levelColors = listOf(
        "#E3F2FD",
        "#F3E5F5",
        "#E8F5E9",
        "#FFF3E0",
        "#FFEBEE",
        "#E0F7FA",
        "#FFFDE7",
        "#FBE9E7",
        "#EFEBE9",
        "#FAFAFA"
    )

    private var isGameOverHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        mainLayout = findViewById(R.id.main)
        gameBoard = findViewById(R.id.gameBoard)
        levelTextView = findViewById(R.id.levelTextView)
        scoreTextView = findViewById(R.id.scoreTextView)
        soundButton = findViewById(R.id.soundButton)
        powerUpTextView = findViewById(R.id.powerUpTextView)
        timerTextView = findViewById(R.id.timerTextView)
        bossBarContainer = findViewById(R.id.bossBarContainer)

        mainLayout.setBackgroundColor(currentBgColor)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val gameMode = intent.getStringExtra("GAME_MODE") ?: "CLASSIC"

        gameEngine = GameEngine(this, gameBoard, this)
        gameEngine.powerUpTextView = powerUpTextView
        gameEngine.timerTextView = timerTextView
        gameEngine.bossBarContainer = bossBarContainer
        gameEngine.bossProgressBar = findViewById(R.id.bossProgressBar)

        if (gameMode == "SURVIVAL") {
            levelTextView.text = "SURVIVAL"
        }

        gameEngine.startGame(gameMode)

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
        if (gameEngine.gameMode == "SURVIVAL") return
        levelTextView.text = "Level: $level"

        val newColorStr = levelColors[(level - 1) % levelColors.size]
        val newColor = Color.parseColor(newColorStr)

        if (currentBgColor != newColor) {
            val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), currentBgColor, newColor)
            colorAnimation.duration = 1000
            colorAnimation.addUpdateListener { animator ->
                mainLayout.setBackgroundColor(animator.animatedValue as Int)
            }
            colorAnimation.start()
            currentBgColor = newColor
        }
    }

    override fun onGameFinished() {
        if (isGameOverHandled) return
        isGameOverHandled = true
        gameEngine.stopEngine()
        Toast.makeText(this, "Tebrikler! Oyunu tamamladınız.", Toast.LENGTH_LONG).show()

        // Bir miktar bekleyip lobiye dönelim ki Toast görünsün ve UI Thread çökmesin
        Handler(Looper.getMainLooper()).postDelayed({
            goBackToLobby()
        }, 1500)
    }

    override fun onGameOver(score: Int) {
        if (isGameOverHandled) return
        isGameOverHandled = true
        gameEngine.stopEngine()
        Toast.makeText(this, "OYUN BİTTİ! Skor: $score", Toast.LENGTH_LONG).show()

        Handler(Looper.getMainLooper()).postDelayed({
            goBackToLobby()
        }, 1500)
    }

    private fun goBackToLobby() {
        if (!isFinishing && !isDestroyed) {
            val intent = Intent(this, LobbyActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gameEngine.stopEngine()
        gameEngine.soundManager.release()
    }
}
