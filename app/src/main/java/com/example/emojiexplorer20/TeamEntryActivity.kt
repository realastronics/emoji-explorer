package com.example.emojiexplorer20

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.emojiexplorer20.data.repository.FirebaseRepository
import kotlinx.coroutines.launch

class TeamEntryActivity : AppCompatActivity() {

    private lateinit var etTeamName: EditText
    private lateinit var etPin: EditText
    private lateinit var btnJoin: Button
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar

    private val repository = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_team_entry)

        etTeamName  = findViewById(R.id.et_team_name)
        etPin       = findViewById(R.id.et_pin)
        btnJoin     = findViewById(R.id.btn_join)
        tvStatus    = findViewById(R.id.tv_status)
        progressBar = findViewById(R.id.progress_bar)

        // Auto-fill if previously saved
        val prefs = getSharedPreferences("team_prefs", MODE_PRIVATE)
        val savedName = prefs.getString("team_name", "") ?: ""
        val savedPin  = prefs.getString("team_pin",  "") ?: ""
        if (savedName.isNotEmpty()) etTeamName.setText(savedName)
        if (savedPin.isNotEmpty())  etPin.setText(savedPin)

        btnJoin.setOnClickListener {
            val name = etTeamName.text.toString().trim()
            val pin  = etPin.text.toString().trim()

            when {
                name.isEmpty() -> { etTeamName.error = "Enter team name"; return@setOnClickListener }
                pin.length < 4 -> { etPin.error = "PIN must be 4+ digits";  return@setOnClickListener }
            }

            setLoading(true)
            lifecycleScope.launch {
                val result = repository.joinOrCreateTeam(teamName = name, pin = pin)
                setLoading(false)
                result.fold(
                    onSuccess = { team ->
                        // Persist for next session
                        prefs.edit()
                            .putString("team_name", team.name)
                            .putString("team_pin",  pin)
                            .apply()

                        // Go straight to the map — passing teamId (= PIN) and name
                        startActivity(
                            Intent(this@TeamEntryActivity, MainActivity::class.java)
                                .putExtra("team_name", team.name)
                                .putExtra("team_id",   team.id)
                        )
                        finish()
                    },
                    onFailure = { e ->
                        tvStatus.text = "Error: ${e.message}"
                        tvStatus.visibility = View.VISIBLE
                        Toast.makeText(this@TeamEntryActivity,
                            "Could not join team", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        btnJoin.isEnabled     = !loading
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        tvStatus.visibility    = View.GONE
    }
}