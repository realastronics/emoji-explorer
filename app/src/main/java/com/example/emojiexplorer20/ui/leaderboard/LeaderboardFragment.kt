package com.example.emojiexplorer20.ui.leaderboard

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
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
    private var teams: List<Team> = emptyList()

    companion object {
        fun newInstance(teamId: String): LeaderboardFragment {
            val f = LeaderboardFragment()
            f.arguments = Bundle().apply { putString("team_id", teamId) }
            return f
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_leaderboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = FirebaseRepository()
        currentTeamId = arguments?.getString("team_id") ?: ""
        leaderboardContainer = view.findViewById(R.id.leaderboard_container)

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
            val row = layoutInflater.inflate(
                android.R.layout.simple_list_item_2,
                leaderboardContainer,
                false
            )

            val text1 = row.findViewById<TextView>(android.R.id.text1)
            val text2 = row.findViewById<TextView>(android.R.id.text2)

            val medal = when (index) { 0 -> "1st" 1 -> "2nd" 2 -> "3rd" else -> "${index+1}th" }
            text1.text = "$medal  ${team.name}"
            text1.textSize = 16f
            text1.setTextColor(
                if (team.id == currentTeamId) Color.parseColor("#EF9F27")
                else Color.WHITE
            )

            text2.text = "${team.score} pts"
            text2.setTextColor(Color.parseColor("#9FE1CB"))

            // Long press to apply power-up
            row.setOnLongClickListener {
                if (team.id != currentTeamId) {
                    showPowerUpDialog(team)
                }
                true
            }

            leaderboardContainer.addView(row)
        }
    }

    private fun showPowerUpDialog(targetTeam: Team) {
        val options = PowerUpType.values()
        val items = options.map { it.label }.toTypedArray()

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Apply power-up to ${targetTeam.name}")
            .setItems(items) { _, which ->
                applyPowerUp(targetTeam, options[which])
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
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        repository.cleanup()
    }
}