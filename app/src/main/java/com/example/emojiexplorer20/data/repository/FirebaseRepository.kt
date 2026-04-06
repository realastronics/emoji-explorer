package com.example.emojiexplorer20.data.repository

import com.example.emojiexplorer20.data.model.PowerUp
import com.example.emojiexplorer20.data.model.PowerUpType
import com.example.emojiexplorer20.data.model.Team
import com.example.emojiexplorer20.data.model.SpawnConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val db = FirebaseFirestore.getInstance()
    private val teamsCollection = db.collection("teams")
    private var leaderboardListener: ListenerRegistration? = null

    // --- Register or fetch a team ---
    suspend fun registerTeam(teamName: String): Result<Team> {
        return try {
            // Check if team already exists
            val existing = teamsCollection
                .whereEqualTo("name", teamName)
                .get()
                .await()

            if (!existing.isEmpty) {
                val doc = existing.documents[0]
                val team = Team(
                    id = doc.id,
                    name = doc.getString("name") ?: teamName,
                    score = doc.getLong("score")?.toInt() ?: 0
                )
                Result.success(team)
            } else {
                // Create new team
                val newTeam = hashMapOf(
                    "name" to teamName,
                    "score" to 0,
                    "debuffs" to emptyMap<String, Long>(),
                    "lastSeen" to System.currentTimeMillis()
                )
                val ref = teamsCollection.add(newTeam).await()
                Result.success(Team(id = ref.id, name = teamName, score = 0))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Push score update to Firestore ---
    suspend fun updateScore(teamId: String, newScore: Int): Result<Unit> {
        return try {
            teamsCollection.document(teamId).update(
                mapOf(
                    "score" to newScore,
                    "lastSeen" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Real-time leaderboard as a Flow ---
    fun leaderboardFlow(): Flow<List<Team>> = callbackFlow {
        leaderboardListener = teamsCollection
            .orderBy("score", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val teams = snapshot.documents.mapNotNull { doc ->
                    Team(
                        id = doc.id,
                        name = doc.getString("name") ?: return@mapNotNull null,
                        score = doc.getLong("score")?.toInt() ?: 0,
                        debuffs = (doc.get("debuffs") as? Map<String, Long>) ?: emptyMap()
                    )
                }
                trySend(teams)
            }
        awaitClose { leaderboardListener?.remove() }
    }

    // --- Apply a power-up to a target team ---
    suspend fun applyPowerUp(powerUp: PowerUp): Result<Unit> {
        return try {
            val expiryTime = powerUp.appliedAt + powerUp.durationMs
            teamsCollection.document(powerUp.targetTeamId).update(
                mapOf("debuffs.${powerUp.type.name}" to expiryTime)
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Check if a debuff is active on a team ---
    fun isDebuffActive(team: Team, type: PowerUpType): Boolean {
        val expiry = team.debuffs[type.name] ?: return false
        return System.currentTimeMillis() < expiry
    }

    // --- Get active capture hold duration (respects SLOW_CAPTURE debuff) ---
    fun getCaptureHoldMs(team: Team): Long {
        return if (isDebuffActive(team, PowerUpType.SLOW_CAPTURE)) {
            SpawnConfig.CAPTURE_HOLD_SLOW_MS
        } else {
            SpawnConfig.CAPTURE_HOLD_MS
        }
    }

    // --- Get active proximity radius (respects SHRINK_ZONE debuff) ---
    fun getProximityRadius(team: Team): Double {
        return if (isDebuffActive(team, PowerUpType.SHRINK_ZONE)) {
            SpawnConfig.SHRINK_ZONE_RADIUS_M
        } else {
            SpawnConfig.AR_TRIGGER_RADIUS_M
        }
    }

    fun cleanup() {
        leaderboardListener?.remove()
    }
}