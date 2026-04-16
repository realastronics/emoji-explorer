package com.example.emojiexplorer20.data.model

object SpawnConfig {

    // Static spawn points on the map — no power-ups, pure collectibles
    val SPAWN_POINTS = listOf(
        EmojiObject("sp_01", 28.2475011, 76.8128989, "neon",   EmojiObject.Rarity.STANDARD),
        EmojiObject("sp_02", 28.2472867, 76.8130619, "yellow", EmojiObject.Rarity.STANDARD),
        EmojiObject("sp_03", 28.2469092, 76.8129764, "red",    EmojiObject.Rarity.STANDARD),
        EmojiObject("sp_04", 28.2476269, 76.8136744, "blue",   EmojiObject.Rarity.CLASSIC),
        EmojiObject("sp_05", 28.2471015, 76.8135775, "pink",   EmojiObject.Rarity.STANDARD),
        EmojiObject("sp_06", 28.2483736, 76.8124088, "yellow", EmojiObject.Rarity.STANDARD),
        EmojiObject("sp_07", 28.2466396, 76.8123920, "pink",   EmojiObject.Rarity.STANDARD),
        EmojiObject("sp_08", 28.2478500, 76.8131200, "red",    EmojiObject.Rarity.STANDARD),
        EmojiObject("sp_09", 28.2467800, 76.8133400, "blue",   EmojiObject.Rarity.CLASSIC),
        EmojiObject("sp_10", 28.2480100, 76.8127600, "neon",   EmojiObject.Rarity.STANDARD),
    )

    fun resetCaptureState() {
        SPAWN_POINTS.forEach { it.capturedByTeams.clear() }
    }

    // ALL_OBJECTS is now just SPAWN_POINTS — no power-ups
    val ALL_OBJECTS get() = SPAWN_POINTS

    // Proximity and capture config
    const val PROXIMITY_RADIUS_M      = 20.0
    const val AR_TRIGGER_RADIUS_M     = 15.0
    const val CAPTURE_HOLD_MS         = 1800L
    const val GPS_ACCURACY_GATE_M     = 35f
    const val GPS_JUMP_THRESHOLD_M    = 80.0
    const val LEADERBOARD_SYNC_MS     = 4000L
    const val GPS_SMOOTH_WINDOW       = 3

    // Spawn weights for dynamic spawning — must sum to 100
    const val WEIGHT_CLASSIC  = 8   // blue classic can
    const val WEIGHT_STANDARD = 32  // pink/yellow/red/neon cans
    const val WEIGHT_EMOJI    = 60  // regular emojis

    // Standard can colors (non-blue)
    val STANDARD_CAN_COLORS = listOf("yellow", "red", "pink", "neon")

    // Emoji pool for EMOJI-type spawns
    val EMOJI_POOL = listOf(
        "🔥", "⚡", "🎯", "💥", "🌟", "🏆", "🎮", "🚀",
        "💎", "🎪", "🌈", "🎸", "🦁", "🐯", "🦊", "🐉",
        "🍀", "🌺", "🎭", "🎨", "🎲", "🃏", "🎰", "🏅"
    )
}