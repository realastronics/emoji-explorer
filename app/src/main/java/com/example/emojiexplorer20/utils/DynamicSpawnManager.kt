package com.example.emojiexplorer20.utils

import com.example.emojiexplorer20.data.model.EmojiObject
import com.example.emojiexplorer20.data.model.PowerUpType
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object DynamicSpawnManager {

    private var lastSpawnLat = 0.0
    private var lastSpawnLng = 0.0
    private var distanceSinceLastRare = 0.0
    private var distanceSinceLastUltra = 0.0
    private var lastCommonSpawnMs = 0L
    private var lastUncommonSpawnMs = 0L

    private const val RARE_SPAWN_EVERY_M      = 50.0
    private const val ULTRA_SPAWN_EVERY_M     = 75.0
    private const val COMMON_SPAWN_EVERY_MS   = 200_000L
    private const val UNCOMMON_SPAWN_EVERY_MS = 220_000L
    private const val COMMON_SPAWN_RADIUS_M   = 5.0
    private const val UNCOMMON_SPAWN_RADIUS_M = 17.0

    val dynamicSpawns = java.util.concurrent.CopyOnWriteArrayList<EmojiObject>()
    private val capturedIds = mutableSetOf<String>()

    // Track whether welcome can has already been spawned this session
    private var welcomeSpawned = false

    fun spawnWelcomeCan(lat: Double, lng: Double): EmojiObject {
        val welcome = EmojiObject(
            id = "welcome_${System.currentTimeMillis()}",
            lat = lat + offsetMetresToDeg(2.0),
            lng = lng,
            emoji = "blue",
            rarity = EmojiObject.Rarity.COMMON,
            respawnDelayMs = 0L
        )
        dynamicSpawns.add(welcome)
        lastSpawnLat = lat
        lastSpawnLng = lng
        // KEY FIX: initialise BOTH timers to NOW so neither fires immediately
        val now = System.currentTimeMillis()
        lastCommonSpawnMs = now
        lastUncommonSpawnMs = now
        welcomeSpawned = true
        return welcome
    }

    fun onLocationUpdate(lat: Double, lng: Double): List<EmojiObject> {
        // Don't process until welcome can has been spawned — avoids race on first update
        if (!welcomeSpawned) return emptyList()

        val newSpawns = mutableListOf<EmojiObject>()
        val now = System.currentTimeMillis()

        if (lastSpawnLat != 0.0) {
            val distMoved = GpsUtils.distanceMetres(lastSpawnLat, lastSpawnLng, lat, lng)
            if (distMoved > 4.0) {
                distanceSinceLastRare  += distMoved
                distanceSinceLastUltra += distMoved
            }
        }
        lastSpawnLat = lat
        lastSpawnLng = lng

        if (distanceSinceLastRare >= RARE_SPAWN_EVERY_M) {
            distanceSinceLastRare = 0.0
            newSpawns.add(createDynamicSpawn(lat, lng, EmojiObject.Rarity.RARE, 8.0)
                .also { dynamicSpawns.add(it) })
        }

        if (distanceSinceLastUltra >= ULTRA_SPAWN_EVERY_M) {
            distanceSinceLastUltra = 0.0
            newSpawns.add(createDynamicSpawn(lat, lng, EmojiObject.Rarity.ULTRA, 10.0)
                .also { dynamicSpawns.add(it) })
        }

        if (now - lastCommonSpawnMs >= COMMON_SPAWN_EVERY_MS) {
            lastCommonSpawnMs = now
            newSpawns.add(createDynamicSpawn(lat, lng, EmojiObject.Rarity.COMMON, COMMON_SPAWN_RADIUS_M)
                .also { dynamicSpawns.add(it) })
        }

        if (now - lastUncommonSpawnMs >= UNCOMMON_SPAWN_EVERY_MS) {
            lastUncommonSpawnMs = now
            newSpawns.add(createDynamicSpawn(lat, lng, EmojiObject.Rarity.UNCOMMON, UNCOMMON_SPAWN_RADIUS_M)
                .also { dynamicSpawns.add(it) })
        }

        // Expire spawns older than 5 min
        val expiredCutoff = now - 5 * 60_000L
        dynamicSpawns.removeAll { spawn ->
            spawn.id.startsWith("dyn_") &&
                    spawn.id.substringAfter("dyn_").substringBefore("_").toLongOrNull()
                        ?.let { it < expiredCutoff } == true
        }

        return newSpawns
    }

    private fun createDynamicSpawn(
        lat: Double, lng: Double,
        rarity: EmojiObject.Rarity,
        radius: Double
    ): EmojiObject {
        val angle    = Random.nextDouble(0.0, 360.0)
        val distance = Random.nextDouble(radius * 0.4, radius)
        val offsetLat = offsetMetresToDeg(distance * cos(Math.toRadians(angle)))
        val offsetLng = offsetMetresToDeg(distance * sin(Math.toRadians(angle))) /
                cos(Math.toRadians(lat))

        // FIX: removed duplicate UNCOMMON branch — each rarity maps to exactly one color
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
        distanceSinceLastRare = 0.0
        distanceSinceLastUltra = 0.0
        lastCommonSpawnMs = 0L
        lastUncommonSpawnMs = 0L
        welcomeSpawned = false
    }
}