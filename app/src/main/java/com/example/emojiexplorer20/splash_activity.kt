package com.example.emojiexplorer20

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val loadingBar = findViewById<View>(R.id.loading_bar)
        val tvLoading = findViewById<TextView>(R.id.tv_loading_text)

        val messages = listOf(
            "Starting engine...",
            "Warming up tyres...",
            "Scanning track...",
            "Lights out!"
        )

        // Animate loading bar
        val animator = ValueAnimator.ofInt(0, 240.dpToPx()).apply {
            duration = 2800L
            addUpdateListener { anim ->
                val params = loadingBar.layoutParams
                params.width = anim.animatedValue as Int
                loadingBar.layoutParams = params
            }
            start()
        }

        // Cycle loading messages
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        messages.forEachIndexed { index, msg ->
            handler.postDelayed({
                tvLoading.text = msg
            }, index * 700L)
        }

        // Launch MainActivity after animation
        handler.postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 3000L)
    }

    private fun Int.dpToPx(): Int =
        (this * resources.displayMetrics.density).toInt()
}