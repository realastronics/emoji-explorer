package com.example.emojiexplorer20.data.repository

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

    suspend fun getTeamName(teamId: String): String {
        return try {
            val doc = teamsCollection.document(teamId).get().await()
            doc.getString("name") ?: "Unknown Team"
        } catch (e: Exception) {
            "Unknown Team"
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
                    )
                }
                trySend(teams)
            }
        awaitClose { leaderboardListener?.remove() }
    }
    fun cleanup() {
        leaderboardListener?.remove()
    }
}