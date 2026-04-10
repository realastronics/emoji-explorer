package com.example.emojiexplorer20.ui.leaderboard

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.emojiexplorer20.R
import com.example.emojiexplorer20.data.model.PowerUp
import com.example.emojiexplorer20.data.model.PowerUpType
import com.example.emojiexplorer20.data.model.Team
import com.example.emojiexplorer20.data.repository.FirebaseRepository
import kotlinx.coroutines.launch

class LeaderboardFragment : Fragment() {

    private lateinit var repository: FirebaseRepository
    private lateinit var leaderboardContainer: LinearLayout
    private var currentTeamId: String = ""
    private var currentTeamName: String = ""
    private var heldPowerUp: PowerUpType? = null
    private var teams: List<Team> = emptyList()

    companion object {
        fun newInstance(
            teamId: String,
            teamName: String = "",
            heldPowerUp: String? = null
        ): LeaderboardFragment {
            val f = LeaderboardFragment()
            f.arguments = Bundle().apply {
                putString("team_id", teamId)
                putString("team_name", teamName)
                if (heldPowerUp != null) putString("held_power_up", heldPowerUp)
            }
            return f
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_leaderboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = FirebaseRepository()
        currentTeamId = arguments?.getString("team_id") ?: ""
        currentTeamName = arguments?.getString("team_name") ?: ""

        val heldPowerUpName = arguments?.getString("held_power_up")
        heldPowerUp = heldPowerUpName?.let {
            try { PowerUpType.valueOf(it) } catch (e: Exception) { null }
        }

        leaderboardContainer = view.findViewById(R.id.leaderboard_container)

        // Show held power-up badge if available
        val tvHeldPowerup = view.findViewById<TextView?>(R.id.tv_held_powerup)
        val btnUsePowerup = view.findViewById<Button?>(R.id.btn_use_powerup)

        if (heldPowerUp != null) {
            tvHeldPowerup?.text = "⚡ ${heldPowerUp!!.label}"
            tvHeldPowerup?.visibility = View.VISIBLE
            btnUsePowerup?.visibility = View.VISIBLE
            btnUsePowerup?.text = "USE: ${heldPowerUp!!.label.uppercase()}"
            btnUsePowerup?.setOnClickListener {
                val otherTeams = teams.filter { it.id != currentTeamId }
                if (otherTeams.isEmpty()) {
                    Toast.makeText(requireContext(), "No other teams yet!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val names = otherTeams.map { it.name }.toTypedArray()
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Select target team")
                    .setItems(names) { _, which ->
                        applyPowerUp(otherTeams[which], heldPowerUp!!)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        view.findViewById<Button>(R.id.btn_close_leaderboard).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        observeLeaderboard()
    }

    private fun observeLeaderboard() {
        lifecycleScope.launch {
            repository.leaderboardFlow().collect { teamList ->
                teams = teamList
                renderLeaderboard(teamList)
            }
        }
    }

    private fun renderLeaderboard(teamList: List<Team>) {
        leaderboardContainer.removeAllViews()

        teamList.forEachIndexed { index, team ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setBackgroundColor(
                    if (team.id == currentTeamId)
                        Color.parseColor("#1FE8002D")
                    else if (index % 2 == 0)
                        Color.parseColor("#1A1A1A")
                    else
                        Color.parseColor("#141414")
                )
                setPadding(20, 16, 20, 16)
            }

            // Position medal
            val tvPosition = TextView(requireContext()).apply {
                text = when (index) {
                    0 -> "🥇"
                    1 -> "🥈"
                    2 -> "🥉"
                    else -> "${index + 1}."
                }
                textSize = 14f
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(60,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            // Team name
            val tvName = TextView(requireContext()).apply {
                text = team.name
                textSize = 15f
                setTextColor(
                    if (team.id == currentTeamId)
                        Color.parseColor("#E8002D")
                    else Color.WHITE
                )
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            // Score
            val tvScore = TextView(requireContext()).apply {
                text = "${team.score} pts"
                textSize = 14f
                setTextColor(Color.parseColor("#EF9F27"))
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            row.addView(tvPosition)
            row.addView(tvName)
            row.addView(tvScore)

            // Long press to apply power-up or blackout
            row.setOnLongClickListener {
                if (team.id != currentTeamId) showAttackDialog(team)
                true
            }

            leaderboardContainer.addView(row)

            // Divider
            val divider = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                )
                setBackgroundColor(Color.parseColor("#2A2A2A"))
            }
            leaderboardContainer.addView(divider)
        }
    }

    private fun showAttackDialog(targetTeam: Team) {
        val options = mutableListOf(
            "⚡ Slow Crawl — slow their capture",
            "🎯 Shrink Zone — reduce their range",
            "🌑 BLACKOUT — blind them 7 seconds"
        )
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Attack ${targetTeam.name}")
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> applyPowerUp(targetTeam, PowerUpType.SLOW_CAPTURE)
                    1 -> applyPowerUp(targetTeam, PowerUpType.SHRINK_ZONE)
                    2 -> applyBlackout(targetTeam)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun applyPowerUp(targetTeam: Team, type: PowerUpType) {
        val powerUp = PowerUp(
            type = type,
            fromTeamId = currentTeamId,
            targetTeamId = targetTeam.id,
            durationMs = type.durationMs
        )
        lifecycleScope.launch {
            repository.applyPowerUp(powerUp)
            Toast.makeText(
                requireContext(),
                "Power-up applied to ${targetTeam.name}!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun applyBlackout(targetTeam: Team) {
        lifecycleScope.launch {
            val myName = repository.getTeamName(currentTeamId)
            repository.applyBlackout(currentTeamId, myName, targetTeam.id)
            Toast.makeText(
                requireContext(),
                "BLACKOUT sent to ${targetTeam.name}!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        repository.cleanup()
    }
}