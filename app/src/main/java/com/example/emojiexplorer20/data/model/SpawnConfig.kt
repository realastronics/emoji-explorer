package com.example.emojiexplorer20.data.model

object SpawnConfig {

    val SPAWN_POINTS = listOf(
        // Spot 1 — your first coordinate
        EmojiObject("sp_01", 28.2463, 76.8120, "🔥", EmojiObject.Rarity.COMMON),
        // Spot 2 — your second coordinate
        EmojiObject("sp_02", 28.2475, 76.8136, "⭐", EmojiObject.Rarity.UNCOMMON),
        // Spread around campus — ~30-50m apart from your two anchor points
        EmojiObject("sp_03", 28.2468, 76.8128, "💎", EmojiObject.Rarity.RARE),
        EmojiObject("sp_04", 28.2458, 76.8142, "👑", EmojiObject.Rarity.ULTRA),
        EmojiObject("sp_05", 28.2480, 76.8115, "🔥", EmojiObject.Rarity.COMMON),
        EmojiObject("sp_06", 28.2452, 76.8130, "⭐", EmojiObject.Rarity.UNCOMMON),
        EmojiObject("sp_07", 28.2471, 76.8148, "💎", EmojiObject.Rarity.RARE),
        EmojiObject("sp_08", 28.2485, 76.8138, "🔥", EmojiObject.Rarity.COMMON),
        EmojiObject("sp_09", 28.2460, 76.8108, "👑", EmojiObject.Rarity.ULTRA),
        EmojiObject("sp_10", 28.2477, 76.8122, "⭐", EmojiObject.Rarity.UNCOMMON),
        EmojiObject("sp_11", 28.2455, 76.8145, "🔥", EmojiObject.Rarity.COMMON),
        EmojiObject("sp_12", 28.2482, 76.8130, "💎", EmojiObject.Rarity.RARE),
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