package com.example.emojiexplorer20

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk

class MainActivity : AppCompatActivity() {

    // All permissions the app needs
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
            // Ask again after short delay — player must grant to continue
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
        when {
            availability.isSupported -> {
                // Device supports AR — proceed to game
                Toast.makeText(this, "AR Ready. Explorer loading...", Toast.LENGTH_SHORT).show()
                // TODO: Next step — launch MapFragment here
            }
            else -> {
                Toast.makeText(
                    this,
                    "This device does not support ARCore",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}