package com.example.emojiexplorer20

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.emojiexplorer20.ui.map.MapFragment
import com.google.ar.core.ArCoreApk

class MainActivity : AppCompatActivity() {

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            checkArCoreAndProceed()
        } else {
            Toast.makeText(
                this,
                "Camera and Location are required to play Explorer",
                Toast.LENGTH_LONG
            ).show()
            requestPermissionsIfNeeded()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        showBannerOnce()          // ← new: one-time banner
        requestPermissionsIfNeeded()
    }

    // -------------------------------------------------------------------------
    // Show redbull_banner image exactly once per app install
    // -------------------------------------------------------------------------
    private fun showBannerOnce() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val hasSeenBanner = prefs.getBoolean("banner_shown", false)
        if (hasSeenBanner) return

        // Mark as shown immediately so back-presses can't re-trigger it
        prefs.edit().putBoolean("banner_shown", true).apply()

        val bannerView = findViewById<ImageView>(R.id.iv_launch_banner) ?: return
        bannerView.setImageResource(R.drawable.redbull_banner)
        bannerView.visibility = View.VISIBLE

        // Auto-dismiss after 3 seconds; also tappable to dismiss early
        val dismiss = Runnable { bannerView.visibility = View.GONE }
        Handler(Looper.getMainLooper()).postDelayed(dismiss, 3000L)
        bannerView.setOnClickListener {
            Handler(Looper.getMainLooper()).removeCallbacks(dismiss)
            bannerView.visibility = View.GONE
        }
    }

    private fun requestPermissionsIfNeeded() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) checkArCoreAndProceed()
        else permissionLauncher.launch(missing.toTypedArray())
    }

    private fun checkArCoreAndProceed() {
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        if (availability.isSupported) {
            val teamName = intent.getStringExtra("team_name")
            val teamId   = intent.getStringExtra("team_id")
            if (teamName != null && teamId != null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container,
                        MapFragment.newInstance(teamName, teamId))
                    .commit()
            } else {
                startActivity(android.content.Intent(this, TeamEntryActivity::class.java))
                finish()
            }
        } else {
            Toast.makeText(this, "ARCore not supported", Toast.LENGTH_LONG).show()
        }
    }
}