package com.yazhamit.eslestirme

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.sceneview.SceneView

class MainActivity : AppCompatActivity(), GameEngine.GameCallback {

    private lateinit var sceneView: SceneView
    private lateinit var levelTextView: TextView
    private lateinit var scoreTextView: TextView
    private lateinit var gameEngine: GameEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        sceneView = findViewById(R.id.sceneView)
        levelTextView = findViewById(R.id.levelTextView)
        scoreTextView = findViewById(R.id.scoreTextView)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        gameEngine = GameEngine(this, sceneView, this)
        gameEngine.startGame()
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
}
