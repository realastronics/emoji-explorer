package com.example.emojiexplorer20.utils

import com.example.emojiexplorer20.data.model.EmojiObject
import com.example.emojiexplorer20.data.model.SpawnConfig
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object DynamicSpawnManager {

    private var lastSpawnLat = 0.0
    private var lastSpawnLng = 0.0
    private var distanceTravelled = 0.0
    private var lastTimedSpawnMs = 0L
    private var welcomeSpawned = false

    // Distance walked before a new spawn is triggered
    private const val SPAWN_EVERY_M = 20.0

    // Timed spawn interval (fallback for slow walkers)
    private const val TIMED_SPAWN_EVERY_MS = 90_000L

    // Max radius around player for dynamic spawns (metres)
    private const val SPAWN_RADIUS_M = 12.0

    val dynamicSpawns = java.util.concurrent.CopyOnWriteArrayList<EmojiObject>()
    private val capturedIds = mutableSetOf<String>()

    // -------------------------------------------------------------------------
    // Welcome spawn — one can placed right next to player on first location fix
    // -------------------------------------------------------------------------
    fun spawnWelcomeCan(lat: Double, lng: Double): EmojiObject {
        val welcome = EmojiObject(
            id = "welcome_${System.currentTimeMillis()}",
            lat = lat + offsetMetresToDeg(2.5),
            lng = lng,
            emoji = "blue",
            rarity = EmojiObject.Rarity.CLASSIC,
            respawnDelayMs = 0L
        )
        dynamicSpawns.add(welcome)
        lastSpawnLat = lat
        lastSpawnLng = lng
        lastTimedSpawnMs = System.currentTimeMillis()
        welcomeSpawned = true
        return welcome
    }

    // -------------------------------------------------------------------------
    // Called on every location update — returns list of newly spawned objects
    // -------------------------------------------------------------------------
    fun onLocationUpdate(lat: Double, lng: Double): List<EmojiObject> {
        if (!welcomeSpawned) return emptyList()

        val newSpawns = mutableListOf<EmojiObject>()
        val now = System.currentTimeMillis()

        // Accumulate distance walked
        if (lastSpawnLat != 0.0) {
            val moved = GpsUtils.distanceMetres(lastSpawnLat, lastSpawnLng, lat, lng)
            if (moved > 2.0) {    // ignore GPS jitter under 2m
                distanceTravelled += moved
            }
        }
        lastSpawnLat = lat
        lastSpawnLng = lng

        // Distance-triggered spawn
        if (distanceTravelled >= SPAWN_EVERY_M) {
            distanceTravelled = 0.0
            val spawn = createWeightedSpawn(lat, lng)
            dynamicSpawns.add(spawn)
            newSpawns.add(spawn)
        }

        // Timed fallback spawn (for players standing still)
        if (now - lastTimedSpawnMs >= TIMED_SPAWN_EVERY_MS) {
            lastTimedSpawnMs = now
            val spawn = createWeightedSpawn(lat, lng)
            dynamicSpawns.add(spawn)
            newSpawns.add(spawn)
        }

        // Expire dynamic spawns older than 5 minutes
        val expiryCutoff = now - 5 * 60_000L
        dynamicSpawns.removeAll { spawn ->
            spawn.id.startsWith("dyn_") &&
                    spawn.id.substringAfter("dyn_")
                        .substringBefore("_")
                        .toLongOrNull()
                        ?.let { it < expiryCutoff } == true
        }

        return newSpawns
    }

    // -------------------------------------------------------------------------
    // Weighted random spawn — 8% classic, 32% standard can, 60% emoji
    // -------------------------------------------------------------------------
    private fun createWeightedSpawn(lat: Double, lng: Double): EmojiObject {
        val roll = Random.nextInt(100)  // 0–99

        return when {
            roll < SpawnConfig.WEIGHT_CLASSIC -> {
                // 0–7  → Classic blue can (100 pts)
                createSpawn(lat, lng, "blue", EmojiObject.Rarity.CLASSIC)
            }
            roll < SpawnConfig.WEIGHT_CLASSIC + SpawnConfig.WEIGHT_STANDARD -> {
                // 8–39 → Standard can, random color from pool (20 pts)
                val color = SpawnConfig.STANDARD_CAN_COLORS.random()
                createSpawn(lat, lng, color, EmojiObject.Rarity.STANDARD)
            }
            else -> {
                // 40–99 → Random emoji (5 pts)
                val emoji = SpawnConfig.EMOJI_POOL.random()
                createSpawn(lat, lng, emoji, EmojiObject.Rarity.EMOJI)
            }
        }
    }

    private fun createSpawn(
        lat: Double,
        lng: Double,
        emoji: String,
        rarity: EmojiObject.Rarity
    ): EmojiObject {
        val angle    = Random.nextDouble(0.0, 360.0)
        val distance = Random.nextDouble(SPAWN_RADIUS_M * 0.3, SPAWN_RADIUS_M)
        val offsetLat = offsetMetresToDeg(distance * cos(Math.toRadians(angle)))
        val offsetLng = offsetMetresToDeg(distance * sin(Math.toRadians(angle))) /
                cos(Math.toRadians(lat))

        return EmojiObject(
            id = "dyn_${System.currentTimeMillis()}_${Random.nextInt(9999)}",
            lat = lat + offsetLat,
            lng = lng + offsetLng,
            emoji = emoji,
            rarity = rarity,
            respawnDelayMs = 0L
        )
    }

    fun isCaptured(id: String): Boolean = id in capturedIds

    fun removeCaptured(spawnId: String) {
        capturedIds.add(spawnId)
        dynamicSpawns.removeAll { it.id == spawnId }
    }

    private fun offsetMetresToDeg(metres: Double): Double = metres / 111_320.0

    fun reset() {
        dynamicSpawns.clear()
        capturedIds.clear()
        lastSpawnLat = 0.0
        lastSpawnLng = 0.0
        distanceTravelled = 0.0
        lastTimedSpawnMs = 0L
        welcomeSpawned = false
    }
}