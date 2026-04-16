package com.example.emojiexplorer20.data.model

object SpawnConfig {

    val SPAWN_POINTS = listOf(
        EmojiObject("sp_01", 28.2475011, 76.8128989, "neon",   EmojiObject.Rarity.COMMON),
        EmojiObject("sp_02", 28.2472867, 76.8130619, "yellow", EmojiObject.Rarity.UNCOMMON),
        EmojiObject("sp_03", 28.2469092, 76.8129764, "red",    EmojiObject.Rarity.RARE),
        EmojiObject("sp_04", 28.2476269, 76.8136744, "blue",   EmojiObject.Rarity.COMMON),
        EmojiObject("sp_05", 28.2471015, 76.8135775, "pink",   EmojiObject.Rarity.ULTRA),
    )

    val POWERUP_POINTS = listOf(
        EmojiObject(
            id = "pu_01",
            lat = 28.2471015, lng = 76.8135775,
            emoji = "yellow",
            rarity = EmojiObject.Rarity.RARE,
            respawnDelayMs = 180_000L,
            type = EmojiObject.ObjectType.POWERUP,
            powerUpType = PowerUpType.SLOW_CAPTURE
        ),
        EmojiObject(
            id = "pu_02",
            lat = 28.2469092, lng = 76.8129764,
            emoji = "pink",
            rarity = EmojiObject.Rarity.ULTRA,
            respawnDelayMs = 240_000L,
            type = EmojiObject.ObjectType.POWERUP,
            powerUpType = PowerUpType.SHRINK_ZONE
        ),
        EmojiObject(
            id = "pu_03",
            lat = 28.2476269, lng = 76.8136744,
            emoji = "pink",
            rarity = EmojiObject.Rarity.ULTRA,
            respawnDelayMs = 300_000L,
            type = EmojiObject.ObjectType.POWERUP,
            powerUpType = null
        ),
        EmojiObject(
            id = "pu_04",
            lat = 28.2483736, lng = 76.8124088,
            emoji = "yellow",
            rarity = EmojiObject.Rarity.RARE,
            respawnDelayMs = 180_000L,
            type = EmojiObject.ObjectType.POWERUP,
            powerUpType = PowerUpType.SLOW_CAPTURE
        ),
        EmojiObject(
            id = "pu_05",
            lat = 28.2466396, lng = 76.8123920,
            emoji = "pink",
            rarity = EmojiObject.Rarity.ULTRA,
            respawnDelayMs = 300_000L,
            type = EmojiObject.ObjectType.POWERUP,
            powerUpType = null
        ),
        EmojiObject(
            id = "pu_06",
            lat = 28.2483736, lng = 76.8124088,
            emoji = "neon",
            rarity = EmojiObject.Rarity.RARE,
            respawnDelayMs = 180_000L,
            type = EmojiObject.ObjectType.POWERUP,
            powerUpType = PowerUpType.SLOW_CAPTURE
        ),
    )

    fun resetCaptureState() {
        SPAWN_POINTS.forEach { it.capturedByTeams.clear() }
        POWERUP_POINTS.forEach { it.capturedByTeams.clear() }
    }

    val ALL_OBJECTS get() = SPAWN_POINTS + POWERUP_POINTS

    const val PROXIMITY_RADIUS_M     = 20.0
    const val AR_TRIGGER_RADIUS_M    = 15.0
    const val CAPTURE_HOLD_MS        = 1800L
    const val CAPTURE_HOLD_SLOW_MS   = 3600L
    const val SHRINK_ZONE_RADIUS_M   = 3.0
    const val GPS_ACCURACY_GATE_M    = 20f
    const val GPS_JUMP_THRESHOLD_M   = 80.0
    const val LEADERBOARD_SYNC_MS    = 4000L
    const val GPS_SMOOTH_WINDOW      = 3
}