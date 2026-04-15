package com.example.emojiexplorer20.data.model

object SpawnConfig {

    val SPAWN_POINTS = listOf(
        EmojiObject("sp_01", 28.2475011, 76.8128989, "blue",   EmojiObject.Rarity.COMMON),
        EmojiObject("sp_02", 28.2472867, 76.8130619, "yellow", EmojiObject.Rarity.UNCOMMON),
        EmojiObject("sp_03", 28.2469092, 76.8129764, "red",    EmojiObject.Rarity.RARE),
        EmojiObject("sp_04", 28.2476269, 76.8136744, "blue",   EmojiObject.Rarity.COMMON),
        EmojiObject("sp_05", 28.2471015, 76.8135775, "pink",   EmojiObject.Rarity.ULTRA),
        EmojiObject("sp_06", 28.2472294, 76.8151845, "red",    EmojiObject.Rarity.RARE),
        EmojiObject("sp_07", 28.2483736, 76.8124088, "blue",   EmojiObject.Rarity.COMMON),
        EmojiObject("sp_08", 28.2486677, 76.8111149, "yellow", EmojiObject.Rarity.UNCOMMON),
        EmojiObject("sp_09", 28.2467022, 76.8111216, "red",    EmojiObject.Rarity.RARE),
        EmojiObject("sp_10", 28.2466396, 76.8123920, "blue",   EmojiObject.Rarity.COMMON),
        EmojiObject("sp_11", 28.2462819, 76.8119109, "pink",   EmojiObject.Rarity.ULTRA),
        EmojiObject("sp_12", 28.2469069, 76.8119645, "red",    EmojiObject.Rarity.RARE),
        EmojiObject("sp_13", 28.2470929, 76.8124037, "blue",   EmojiObject.Rarity.COMMON),
        EmojiObject("sp_14", 28.2464402, 76.8149689, "yellow", EmojiObject.Rarity.UNCOMMON),
        EmojiObject("sp_15", 28.2475640, 76.8142048, "red",    EmojiObject.Rarity.RARE),
        EmojiObject("sp_16", 28.2479645, 76.8130967, "blue",   EmojiObject.Rarity.COMMON),
        EmojiObject("sp_17", 28.2473425, 76.8134545, "pink",   EmojiObject.Rarity.ULTRA),
        EmojiObject("sp_18", 28.2470752, 76.8146471, "red",    EmojiObject.Rarity.RARE),
        EmojiObject("sp_19", 28.2475011, 76.8126813, "blue",   EmojiObject.Rarity.COMMON),
        EmojiObject("sp_20", 28.2476706, 76.8132775, "yellow", EmojiObject.Rarity.UNCOMMON),
        EmojiObject("sp_21", 28.2469264, 76.8134655, "red",    EmojiObject.Rarity.RARE),
        EmojiObject("sp_22", 28.2463785, 76.8134441, "blue",   EmojiObject.Rarity.COMMON),
        EmojiObject("sp_23", 28.2472185, 76.8119451, "pink",   EmojiObject.Rarity.ULTRA),
        EmojiObject("sp_24", 28.2474595, 76.8158899, "red",    EmojiObject.Rarity.RARE),
        EmojiObject("sp_25", 28.2468410, 76.8143138, "blue",   EmojiObject.Rarity.COMMON),
        EmojiObject("sp_26", 28.2466540, 76.8139866, "yellow", EmojiObject.Rarity.UNCOMMON),
        EmojiObject("sp_27", 28.2467618, 76.8127393, "red",    EmojiObject.Rarity.RARE),
        EmojiObject("sp_28", 28.2484731, 76.8127393, "blue",   EmojiObject.Rarity.COMMON),
        EmojiObject("sp_29", 28.2471898, 76.8126981, "pink",   EmojiObject.Rarity.ULTRA),
        EmojiObject("sp_30", 28.2472465, 76.8143725, "red",    EmojiObject.Rarity.RARE),
        EmojiObject("sp_31", 28.2470646, 76.8143098, "blue",   EmojiObject.Rarity.COMMON),
        EmojiObject("sp_32", 28.2471854, 76.8139547, "yellow", EmojiObject.Rarity.UNCOMMON),
        EmojiObject("sp_33", 28.2474116, 76.8143188, "red",    EmojiObject.Rarity.RARE),
        EmojiObject("sp_34", 28.2476195, 76.8127558, "blue",   EmojiObject.Rarity.COMMON),
        EmojiObject("sp_35", 28.2480924, 76.8122482, "pink",   EmojiObject.Rarity.ULTRA),
        EmojiObject("sp_36", 28.2480894, 76.8118663, "red",    EmojiObject.Rarity.RARE),
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
    )

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