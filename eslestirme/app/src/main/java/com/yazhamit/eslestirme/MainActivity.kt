package com.yazhamit.eslestirme

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

    private lateinit var gameBoard: FrameLayout
    private lateinit var levelTextView: TextView
    private lateinit var scoreTextView: TextView
    private lateinit var soundButton: ImageView
    private lateinit var gameEngine: GameEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        gameBoard = findViewById(R.id.gameBoard)
        levelTextView = findViewById(R.id.levelTextView)
        scoreTextView = findViewById(R.id.scoreTextView)
        soundButton = findViewById(R.id.soundButton)

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
    }

    override fun onGameFinished() {
        Toast.makeText(this, "Tebrikler! Oyunu tamamladınız.", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        gameEngine.soundManager.release()
    }
}
