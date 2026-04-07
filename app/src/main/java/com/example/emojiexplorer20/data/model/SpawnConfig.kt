package com.example.emojiexplorer20.data.model

object SpawnConfig {

    val SPAWN_POINTS = listOf(
        // Main Building entrance
        EmojiObject("sp_01", 28.58234, 76.63891, "🔥", EmojiObject.Rarity.COMMON),
        // Library
        EmojiObject("sp_02", 28.58198, 76.63942, "⭐", EmojiObject.Rarity.UNCOMMON),
        // Cafeteria
        EmojiObject("sp_03", 28.58267, 76.63978, "💎", EmojiObject.Rarity.RARE),
        // Sports Ground
        EmojiObject("sp_04", 28.58312, 76.63856, "👑", EmojiObject.Rarity.ULTRA),
        // North Block
        EmojiObject("sp_05", 28.58178, 76.63820, "🔥", EmojiObject.Rarity.COMMON),
        // Parking Area
        EmojiObject("sp_06", 28.58345, 76.63910, "⭐", EmojiObject.Rarity.UNCOMMON),
        // Admin Block
        EmojiObject("sp_07", 28.58156, 76.63865, "💎", EmojiObject.Rarity.RARE),
        // Workshop Area
        EmojiObject("sp_08", 28.58289, 76.64012, "🔥", EmojiObject.Rarity.COMMON),
        // Auditorium
        EmojiObject("sp_09", 28.58223, 76.64045, "👑", EmojiObject.Rarity.ULTRA),
        // Girls Hostel Road
        EmojiObject("sp_10", 28.58134, 76.63798, "⭐", EmojiObject.Rarity.UNCOMMON),
        // Boys Hostel
        EmojiObject("sp_11", 28.58367, 76.63845, "🔥", EmojiObject.Rarity.COMMON),
        // Football Ground
        EmojiObject("sp_12", 28.58401, 76.63923, "💎", EmojiObject.Rarity.RARE),
    )

    const val PROXIMITY_RADIUS_M        = 15.0
    const val AR_TRIGGER_RADIUS_M       = 10.0
    const val CAPTURE_HOLD_MS           = 1800L
    const val CAPTURE_HOLD_SLOW_MS      = 3600L
    const val SHRINK_ZONE_RADIUS_M      = 3.0
    const val GPS_ACCURACY_GATE_M       = 15f
    const val GPS_JUMP_THRESHOLD_M      = 50.0
    const val LEADERBOARD_SYNC_MS       = 4000L
    const val GPS_SMOOTH_WINDOW         = 3
}