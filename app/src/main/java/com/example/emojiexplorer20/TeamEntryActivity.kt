package com.example.emojiexplorer20

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.emojiexplorer20.data.repository.FirebaseRepository
import kotlinx.coroutines.launch

class TeamEntryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_team_entry)

        val etTeamName = findViewById<EditText>(R.id.et_team_name)
        val etDriverNum = findViewById<EditText>(R.id.et_driver_number)
        val btnStart = findViewById<Button>(R.id.btn_start_race)
        val tvStatus = findViewById<TextView>(R.id.tv_entry_status)

        btnStart.setOnClickListener {
            val name = etTeamName.text.toString().trim()
            val number = etDriverNum.text.toString().trim()

            if (name.isEmpty()) {
                etTeamName.error = "Enter your team name"
                return@setOnClickListener
            }

            val teamName = if (number.isNotEmpty()) "$name #$number" else name
            tvStatus.text = "Registering team..."
            btnStart.isEnabled = false

            val repo = FirebaseRepository()
            lifecycleScope.launch {
                val result = repo.registerTeam(teamName)
                val team = result.getOrNull()
                if (team != null) {
                    val intent = Intent(
                        this@TeamEntryActivity,
                        MainActivity::class.java
                    ).apply {
                        putExtra("team_name", team.name)
                        putExtra("team_id", team.id)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    tvStatus.text = "Connection error — retrying..."
                    btnStart.isEnabled = true
                }
            }
        }
    }
}