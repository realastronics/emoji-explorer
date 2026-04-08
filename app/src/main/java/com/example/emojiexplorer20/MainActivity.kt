package com.example.emojiexplorer20

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.emojiexplorer20.data.repository.FirebaseRepository
import com.example.emojiexplorer20.ui.map.MapFragment
import com.google.ar.core.ArCoreApk
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
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
        requestPermissionsIfNeeded()
    }

    private fun requestPermissionsIfNeeded() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            checkArCoreAndProceed()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun checkArCoreAndProceed() {
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        if (availability.isSupported) {
            // Check if launched from TeamEntryActivity with team data
            val teamName = intent.getStringExtra("team_name")
            val teamId = intent.getStringExtra("team_id")
            if (teamName != null && teamId != null) {
                supportFragmentManager.beginTransaction()
                    .replace(
                        R.id.fragment_container,
                        com.example.emojiexplorer20.ui.map.MapFragment
                            .newInstance(teamName, teamId)
                    )
                    .commit()
            } else {
                // Fallback — go to team entry
                startActivity(
                    android.content.Intent(this, TeamEntryActivity::class.java)
                )
                finish()
            }
        } else {
            Toast.makeText(this, "ARCore not supported", Toast.LENGTH_LONG).show()
        }
    }


}