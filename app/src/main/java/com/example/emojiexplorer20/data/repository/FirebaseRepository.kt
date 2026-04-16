package com.example.emojiexplorer20.data.repository

import com.example.emojiexplorer20.data.model.Team
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val db = FirebaseFirestore.getInstance()
    private val teamsCollection = db.collection("teams")
    private var leaderboardListener: ListenerRegistration? = null

    /**
     * Join an existing team (by PIN) or create a new one.
     * Document ID = PIN — all teammates sharing the same PIN
     * automatically write to the same Firestore document.
     *
     * Rules:
     *  - If the PIN doc doesn't exist → create it with this name.
     *  - If it exists AND name matches → let them in (resume session).
     *  - If it exists AND name differs → return failure (wrong PIN for that team).
     */
    suspend fun joinOrCreateTeam(teamName: String, pin: String): Result<Team> {
        return try {
            val docRef = teamsCollection.document(pin)
            val snapshot = docRef.get().await()

            if (!snapshot.exists()) {
                // New team — create document keyed by PIN
                val data = mapOf(
                    "name"        to teamName,
                    "pin"         to pin,
                    "score"       to 0,
                    "lastSeen"    to System.currentTimeMillis(),
                    "memberCount" to 1
                )
                docRef.set(data).await()
                Result.success(Team(id = pin, name = teamName, score = 0))

            } else {
                val existingName = snapshot.getString("name") ?: ""
                if (existingName.lowercase() != teamName.lowercase()) {
                    // PIN belongs to a different team
                    Result.failure(Exception(
                        "PIN $pin is already used by team \"$existingName\". " +
                                "Use that name or choose a different PIN."
                    ))
                } else {
                    // Existing member rejoining — bump lastSeen and memberCount
                    docRef.update(
                        mapOf(
                            "lastSeen"    to System.currentTimeMillis(),
                            "memberCount" to (snapshot.getLong("memberCount")?.plus(1) ?: 1)
                        )
                    ).await()
                    Result.success(
                        Team(
                            id    = pin,
                            name  = existingName,
                            score = snapshot.getLong("score")?.toInt() ?: 0
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add points to the team's score atomically using FieldValue.increment,
     * so two players scoring simultaneously don't clobber each other.
     */
    suspend fun addPoints(teamId: String, pointsToAdd: Int): Result<Unit> {
        return try {
            teamsCollection.document(teamId).update(
                mapOf(
                    "score"    to com.google.firebase.firestore.FieldValue.increment(
                        pointsToAdd.toLong()),
                    "lastSeen" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Keep the old updateScore for absolute overwrites (end-of-session sync).
     * Prefer addPoints for incremental captures.
     */
    suspend fun updateScore(teamId: String, newScore: Int): Result<Unit> {
        return try {
            teamsCollection.document(teamId).set(
                mapOf(
                    "score"    to newScore,
                    "lastSeen" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTeamName(teamId: String): String {
        return try {
            teamsCollection.document(teamId).get().await().getString("name") ?: "Unknown Team"
        } catch (e: Exception) { "Unknown Team" }
    }

    fun leaderboardFlow(): Flow<List<Team>> = callbackFlow {
        leaderboardListener = teamsCollection
            .orderBy("score", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val teams = snapshot.documents.mapNotNull { doc ->
                    Team(
                        id    = doc.id,
                        name  = doc.getString("name") ?: return@mapNotNull null,
                        score = doc.getLong("score")?.toInt() ?: 0
                    )
                }
                trySend(teams)
            }
        awaitClose { leaderboardListener?.remove() }
    }

    fun cleanup() { leaderboardListener?.remove() }
}