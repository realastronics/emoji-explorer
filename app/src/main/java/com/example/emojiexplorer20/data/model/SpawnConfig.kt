package com.example.emojiexplorer20.data.model

object SpawnConfig {

    // ⚠️ IMPORTANT: Walk your actual BML campus before the event
    // Open Google Maps, stand at each spot, long-press to get exact coordinates
    // Replace these placeholder coordinates with your real survey points
    val SPAWN_POINTS = listOf(
        EmojiObject("sp_01", 28.9124, 76.5832, "🔥", EmojiObject.Rarity.COMMON),
        EmojiObject("sp_02", 28.9131, 76.5840, "⭐", EmojiObject.Rarity.UNCOMMON),
        EmojiObject("sp_03", 28.9118, 76.5825, "💎", EmojiObject.Rarity.RARE),
        EmojiObject("sp_04", 28.9140, 76.5850, "👑", EmojiObject.Rarity.ULTRA),
        EmojiObject("sp_05", 28.9128, 76.5860, "🔥", EmojiObject.Rarity.COMMON),
        EmojiObject("sp_06", 28.9135, 76.5820, "⭐", EmojiObject.Rarity.UNCOMMON),
        EmojiObject("sp_07", 28.9115, 76.5845, "💎", EmojiObject.Rarity.RARE),
        EmojiObject("sp_08", 28.9145, 76.5835, "🔥", EmojiObject.Rarity.COMMON),
        EmojiObject("sp_09", 28.9122, 76.5855, "⭐", EmojiObject.Rarity.UNCOMMON),
        EmojiObject("sp_10", 28.9138, 76.5828, "👑", EmojiObject.Rarity.ULTRA),
    )

    // Tuning constants — adjust after on-site testing
    const val PROXIMITY_RADIUS_M        = 10.0   // ring appears on map
    const val AR_TRIGGER_RADIUS_M       = 7.0    // AR prompt appears
    const val CAPTURE_HOLD_MS           = 1800L  // normal tap-hold duration
    const val CAPTURE_HOLD_SLOW_MS      = 3600L  // hold duration when SLOW_CAPTURE debuff active
    const val SHRINK_ZONE_RADIUS_M      = 3.0    // radius when SHRINK_ZONE debuff active
    const val GPS_ACCURACY_GATE_M       = 15f    // ignore readings noisier than this
    const val GPS_JUMP_THRESHOLD_M      = 50.0   // anti-cheat: max jump in 2 seconds
    const val LEADERBOARD_SYNC_MS       = 4000L  // how often to push score to Firebase
    const val GPS_SMOOTH_WINDOW         = 3      // number of readings to average
}