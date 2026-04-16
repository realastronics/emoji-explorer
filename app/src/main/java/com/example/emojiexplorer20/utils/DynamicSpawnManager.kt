package com.example.emojiexplorer20.utils

import com.example.emojiexplorer20.data.model.EmojiObject
import com.example.emojiexplorer20.data.model.PowerUpType
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object DynamicSpawnManager {

    // Distance tracking
    private var totalDistanceTravelledM = 0.0
    private var lastSpawnLat = 0.0
    private var lastSpawnLng = 0.0
    private var distanceSinceLastRare = 0.0
    private var distanceSinceLastUltra = 0.0

    // Timer tracking
    private var lastCommonSpawnMs = 0L
    private var lastUncommonSpawnMs = 0L

    // Thresholds
    private const val RARE_SPAWN_EVERY_M    = 50.0   // every 50m walked
    private const val ULTRA_SPAWN_EVERY_M   = 75.0   // every 75m walked
    private const val COMMON_SPAWN_EVERY_MS = 60_000L  // 45 seconds
    private const val UNCOMMON_SPAWN_EVERY_MS = 220_000L // 2 minutes
    private const val COMMON_SPAWN_RADIUS_M  = 5.0
    private const val UNCOMMON_SPAWN_RADIUS_M = 17.0

    // Active dynamic spawns — cleared on capture
    val dynamicSpawns = java.util.concurrent.CopyOnWriteArrayList<EmojiObject>()

    // To prevent duplicate capture of the same emoji
    private val capturedIds = mutableSetOf<String>()

    // Called once on game start — spawn welcome can at player position
    fun spawnWelcomeCan(lat: Double, lng: Double): EmojiObject {
        val welcome = EmojiObject(
            id = "welcome_${System.currentTimeMillis()}",
            lat = lat + offsetMetresToDeg(2.0),  // 2m directly ahead
            lng = lng,
            emoji = "blue",
            rarity = EmojiObject.Rarity.COMMON,
            respawnDelayMs = 0L
        )
        dynamicSpawns.add(welcome)
        lastSpawnLat = lat
        lastSpawnLng = lng
        lastCommonSpawnMs = System.currentTimeMillis()
        lastUncommonSpawnMs = System.currentTimeMillis()
        return welcome
    }

    // Call this every time location updates — returns any new spawns
    fun onLocationUpdate(lat: Double, lng: Double): List<EmojiObject> {
        val newSpawns = mutableListOf<EmojiObject>()
        val now = System.currentTimeMillis()

        // Calculate distance moved since last update
        if (lastSpawnLat != 0.0) {
            val distMoved = GpsUtils.distanceMetres(lastSpawnLat, lastSpawnLng, lat, lng)
            // Only count meaningful movement (filter GPS jitter < 3m)
            if (distMoved > 4.0) {
                distanceSinceLastRare  += distMoved
                distanceSinceLastUltra += distMoved
            }
        }
        lastSpawnLat = lat
        lastSpawnLng = lng

        // --- RARE: every 20m walked ---
        if (distanceSinceLastRare >= RARE_SPAWN_EVERY_M) {
            distanceSinceLastRare = 0.0
            val spawn = createDynamicSpawn(lat, lng, EmojiObject.Rarity.RARE, radius = 8.0)
            dynamicSpawns.add(spawn)
            newSpawns.add(spawn)
        }

        // --- ULTRA: every 35m walked ---
        if (distanceSinceLastUltra >= ULTRA_SPAWN_EVERY_M) {
            distanceSinceLastUltra = 0.0
            val spawn = createDynamicSpawn(lat, lng, EmojiObject.Rarity.ULTRA, radius = 10.0)
            dynamicSpawns.add(spawn)
            newSpawns.add(spawn)
        }

        // --- COMMON: every 45 seconds ---
        if (now - lastCommonSpawnMs >= COMMON_SPAWN_EVERY_MS) {
            lastCommonSpawnMs = now
            val spawn = createDynamicSpawn(
                lat, lng, EmojiObject.Rarity.COMMON, radius = COMMON_SPAWN_RADIUS_M
            )
            dynamicSpawns.add(spawn)
            newSpawns.add(spawn)
        }

        // --- UNCOMMON: every 120 seconds ---
        if (now - lastUncommonSpawnMs >= UNCOMMON_SPAWN_EVERY_MS) {
            lastUncommonSpawnMs = now
            val spawn = createDynamicSpawn(
                lat, lng, EmojiObject.Rarity.UNCOMMON, radius = UNCOMMON_SPAWN_RADIUS_M
            )
            dynamicSpawns.add(spawn)
            newSpawns.add(spawn)
        }

        // Auto-remove dynamic spawns that are more than 5 min old and uncaptured
        val expiredCutoff = now - 5 * 60_000L
        dynamicSpawns.removeAll { spawn ->
            spawn.id.startsWith("dyn_") &&
                    spawn.id.substringAfter("dyn_").toLongOrNull()?.let { it < expiredCutoff } == true
        }

        return newSpawns
    }

    private fun createDynamicSpawn(
        lat: Double, lng: Double,
        rarity: EmojiObject.Rarity,
        radius: Double
    ): EmojiObject {
        // Random angle, random distance within radius
        val angle = Random.nextDouble(0.0, 360.0)
        val distance = Random.nextDouble(radius * 0.4, radius)

        val offsetLat = offsetMetresToDeg(distance * cos(Math.toRadians(angle)))
        val offsetLng = offsetMetresToDeg(distance * sin(Math.toRadians(angle))) /
                cos(Math.toRadians(lat))

        val canColor = when (rarity) {
            EmojiObject.Rarity.COMMON   -> "blue"
            EmojiObject.Rarity.UNCOMMON -> "yellow"
            EmojiObject.Rarity.RARE     -> "red"
            EmojiObject.Rarity.ULTRA    -> "pink"
        }

        return EmojiObject(
            id = "dyn_${System.currentTimeMillis()}_${Random.nextInt(1000)}",
            lat = lat + offsetLat,
            lng = lng + offsetLng,
            emoji = canColor,
            rarity = rarity,
            respawnDelayMs = 0L  // dynamic spawns don't respawn
        )
    }

    // to prevent duplicate emoji generation and capturing
    fun isCaptured(id: String): Boolean = id in capturedIds

    // Converts metres to degrees latitude (approximate, valid at any lat)
    private fun offsetMetresToDeg(metres: Double): Double = metres / 111_320.0

    fun removeCaptured(spawnId: String) {
        capturedIds.add(spawnId)                          // mark before removing
        dynamicSpawns.removeAll { it.id == spawnId }
    }

    fun reset() {
        dynamicSpawns.clear()
        capturedIds.clear()
        totalDistanceTravelledM = 0.0
        lastSpawnLat = 0.0
        lastSpawnLng = 0.0
        distanceSinceLastRare = 0.0
        distanceSinceLastUltra = 0.0
        lastCommonSpawnMs = 0L
        lastUncommonSpawnMs = 0L
    }
}