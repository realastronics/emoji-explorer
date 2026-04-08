package com.example.emojiexplorer20.data.model

object SpawnConfig {

    val SPAWN_POINTS = listOf(
        EmojiObject("sp_01", 28.2463, 76.8120, "🔥", EmojiObject.Rarity.COMMON),
        EmojiObject("sp_02", 28.2475, 76.8136, "⭐", EmojiObject.Rarity.UNCOMMON),
        EmojiObject("sp_03", 28.2479, 76.8131, "💎", EmojiObject.Rarity.RARE),
        EmojiObject("sp_04", 28.2458, 76.8142, "👑", EmojiObject.Rarity.ULTRA),
        EmojiObject("sp_05", 28.2480, 76.8115, "🔥", EmojiObject.Rarity.COMMON),
        EmojiObject("sp_06", 28.2452, 76.8130, "⭐", EmojiObject.Rarity.UNCOMMON),
        EmojiObject("sp_07", 28.2471, 76.8148, "💎", EmojiObject.Rarity.RARE),
        EmojiObject("sp_08", 28.2485, 76.8138, "🔥", EmojiObject.Rarity.COMMON),
        EmojiObject("sp_09", 28.2473, 76.8143, "👑", EmojiObject.Rarity.ULTRA),
        EmojiObject("sp_10", 28.2477, 76.8122, "⭐", EmojiObject.Rarity.UNCOMMON),
        EmojiObject("sp_11", 28.2455, 76.8145, "🔥", EmojiObject.Rarity.COMMON),
        EmojiObject("sp_12", 28.2482, 76.8130, "💎", EmojiObject.Rarity.RARE),
        EmojiObject("sp_13", 28.2469, 76.8125, "💎", EmojiObject.Rarity.RARE),
        EmojiObject("sp_14", 28.2472, 76.8145, "👑", EmojiObject.Rarity.ULTRA),
        EmojiObject("sp_15", 28.2466, 76.8133, "🔥", EmojiObject.Rarity.COMMON),
    )

    val POWERUP_POINTS = listOf(
        EmojiObject(
            id = "pu_01",
            lat = 28.2468,
            lng = 76.8128,
            emoji = "⚡",
            rarity = EmojiObject.Rarity.RARE,
            respawnDelayMs = 180_000L,
            type = EmojiObject.ObjectType.POWERUP,
            powerUpType = PowerUpType.SLOW_CAPTURE
        ),
        EmojiObject(
            id = "pu_02",
            lat = 28.2475,
            lng = 76.8140,
            emoji = "💥",
            rarity = EmojiObject.Rarity.ULTRA,
            respawnDelayMs = 240_000L,
            type = EmojiObject.ObjectType.POWERUP,
            powerUpType = PowerUpType.SHRINK_ZONE
        ),
        EmojiObject(
            id = "pu_03",
            lat = 28.2458,
            lng = 76.8135,
            emoji = "🌑",
            rarity = EmojiObject.Rarity.ULTRA,
            respawnDelayMs = 300_000L,
            type = EmojiObject.ObjectType.POWERUP,
            powerUpType = null
        ),
    )

    val ALL_OBJECTS get() = SPAWN_POINTS + POWERUP_POINTS

    const val PROXIMITY_RADIUS_M     = 25.0
    const val AR_TRIGGER_RADIUS_M    = 20.0
    const val CAPTURE_HOLD_MS        = 1800L
    const val CAPTURE_HOLD_SLOW_MS   = 3600L
    const val SHRINK_ZONE_RADIUS_M   = 3.0
    const val GPS_ACCURACY_GATE_M    = 15f
    const val GPS_JUMP_THRESHOLD_M   = 50.0
    const val LEADERBOARD_SYNC_MS    = 4000L
    const val GPS_SMOOTH_WINDOW      = 3
}