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
            showTeamNameDialog()
        } else {
            Toast.makeText(
                this,
                "ARCore not supported on this device",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showTeamNameDialog() {
        val input = android.widget.EditText(this).apply {
            hint = "Enter your team name"
            setPadding(40, 20, 40, 20)
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("Welcome to Emoji Explorer!")
            .setMessage("What is your team name?")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Let's Go!") { _, _ ->
                val name = input.text.toString().trim()
                    .ifEmpty { "Team ${(100..999).random()}" }
                registerAndLaunch(name)
            }
            .show()
    }

    private fun registerAndLaunch(teamName: String) {
        val repo = FirebaseRepository()
        lifecycleScope.launch {
            val result = repo.registerTeam(teamName)
            val team = result.getOrNull()
            val teamId = team?.id ?: ""
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container,
                    MapFragment.newInstance(teamName, teamId)
                )
                .commit()
        }
    }
}