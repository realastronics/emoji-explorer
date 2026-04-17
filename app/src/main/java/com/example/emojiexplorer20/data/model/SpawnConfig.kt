package com.example.emojiexplorer20.data.model

object SpawnConfig {

    // Static spawn points on the map — no power-ups, pure collectibles
    val SPAWN_POINTS = listOf(
        // MPH
        EmojiObject("sp_01", 28.2475011, 76.8128989, "neon", EmojiObject.Rarity.STANDARD),
        // Pond
        EmojiObject("sp_02", 28.2472867, 76.8130619, "yellow", EmojiObject.Rarity.STANDARD),
        // Volleyball Court
        EmojiObject("sp_03", 28.2469092, 76.8129764, "🏐", EmojiObject.Rarity.EMOJI),
        // Burger Singh
        EmojiObject("sp_04", 28.2476269, 76.8136744, "red", EmojiObject.Rarity.STANDARD),
        // Library Grass
        EmojiObject("sp_05", 28.2471015, 76.8135775, "🌿", EmojiObject.Rarity.EMOJI),
        // Memorial
        EmojiObject("sp_06", 28.2472294, 76.8151845, "blue", EmojiObject.Rarity.CLASSIC),
        // Faculty Housing
        EmojiObject("sp_07", 28.2483736, 76.8124088, "pink", EmojiObject.Rarity.STANDARD),
        // College Back Area
        EmojiObject("sp_08", 28.2486677, 76.8111149, "\uD83C\uDFCE\uFE0F", EmojiObject.Rarity.EMOJI),
        // Mailbox Road
        EmojiObject("sp_09", 28.2467022, 76.8111216, "📬", EmojiObject.Rarity.EMOJI),
        // Lazeez Memorial
        EmojiObject("sp_10", 28.2474252, 76.8114857, "yellow", EmojiObject.Rarity.STANDARD),
        // BSH Arm
        EmojiObject("sp_11", 28.2466396, 76.8123920, "\uD83C\uDFCE\uFE0F", EmojiObject.Rarity.EMOJI),
        // Circle Circle
        EmojiObject("sp_13", 28.2469069, 76.8119645, "🔵", EmojiObject.Rarity.EMOJI),
        // Actual Pond
        EmojiObject("sp_14", 28.2470929, 76.8124037, "blue", EmojiObject.Rarity.CLASSIC),
        // Savera Gate
        EmojiObject("sp_15", 28.2464402, 76.8149689, "\uD83D\uDE0D", EmojiObject.Rarity.EMOJI),
        // Couple Area
        EmojiObject("sp_16", 28.2475640, 76.8142048, "\uD83D\uDE0D", EmojiObject.Rarity.EMOJI),
        // NB Back Road
        EmojiObject("sp_17", 28.2479645, 76.8130967, "neon", EmojiObject.Rarity.STANDARD),
        // NB + Dominos Road
        EmojiObject("sp_18", 28.2473425, 76.8134545, "🍕", EmojiObject.Rarity.EMOJI),
        // Always Wet Area
        EmojiObject("sp_19", 28.2470752, 76.8146471, "💧", EmojiObject.Rarity.EMOJI),
        // NB Stairs
        EmojiObject("sp_20", 28.2478729, 76.8126813, "⬆️", EmojiObject.Rarity.EMOJI),
        // ACM Room (1)
        EmojiObject("sp_21", 28.2476706, 76.8132775, "🎮", EmojiObject.Rarity.EMOJI),
        // Random Spot
        EmojiObject("sp_22", 28.2469264, 76.8134655, "⚡", EmojiObject.Rarity.EMOJI),
        // Workshop Labs
        EmojiObject("sp_23", 28.2463785, 76.8134441, "💥", EmojiObject.Rarity.EMOJI),
        // Forest
        EmojiObject("sp_24", 28.2472185, 76.8119451, "\uD83C\uDFCE\uFE0F", EmojiObject.Rarity.EMOJI),
        // Gate of Wrong Decisions
        EmojiObject("sp_25", 28.2474595, 76.8158899, "🚫", EmojiObject.Rarity.EMOJI),
        // Tarakki Ka Rasta
        EmojiObject("sp_26", 28.2468410, 76.8143138, "📈", EmojiObject.Rarity.EMOJI),
        // Survival Needs
        EmojiObject("sp_27", 28.2466540, 76.8139866, "💥", EmojiObject.Rarity.EMOJI),
        // Disability Advocacy
        EmojiObject("sp_28", 28.2467618, 76.8127393, "♿", EmojiObject.Rarity.EMOJI),
        // United Stop
        EmojiObject("sp_29", 28.2484731, 76.8119169, "🚌", EmojiObject.Rarity.EMOJI),
        // Khudai Ho Gayi
        EmojiObject("sp_30", 28.2471898, 76.8126981, "⛏️", EmojiObject.Rarity.EMOJI),
        // Best Sofas
        EmojiObject("sp_32", 28.2472465, 76.8143725, "🛋️", EmojiObject.Rarity.EMOJI),
        // Amphitheatre
        EmojiObject("sp_33", 28.2471854, 76.8139547, "🎤", EmojiObject.Rarity.EMOJI),
        // Audi
        EmojiObject("sp_34", 28.2474116, 76.8143188, "🎧", EmojiObject.Rarity.EMOJI),
        // Badminton Court
        EmojiObject("sp_35", 28.2476195, 76.8127558, "\uD83D\uDE0D", EmojiObject.Rarity.EMOJI),
        // Faculties
        EmojiObject("sp_36", 28.2480924, 76.8122482, "\uD83D\uDE0D", EmojiObject.Rarity.EMOJI),
        // Hostel
        EmojiObject("sp_37", 28.2480894, 76.8118663, "🏠", EmojiObject.Rarity.EMOJI),
        // My Room
        EmojiObject("sp_38", 28.2464098, 76.8119759, "red", EmojiObject.Rarity.STANDARD),
        // Room 508
        EmojiObject("sp_39", 28.2464296, 76.8122958, "pink", EmojiObject.Rarity.STANDARD),
        // Mitali Room
        EmojiObject("sp_41", 28.2485903, 76.8118411, "💥", EmojiObject.Rarity.EMOJI),
        // BMU Gatepath
        EmojiObject("sp_42", 28.2474849, 76.8156757, "🚶", EmojiObject.Rarity.EMOJI),
        // Excellence Centre
        EmojiObject("sp_43", 28.2473100, 76.8132446, "🏆", EmojiObject.Rarity.EMOJI),
    )

    fun resetCaptureState() {
        SPAWN_POINTS.forEach { it.capturedByTeams.clear() }
    }

    // ALL_OBJECTS is now just SPAWN_POINTS — no power-ups
    val ALL_OBJECTS get() = SPAWN_POINTS

    // Proximity and capture config
    const val VISIBILITY_RADIUS_M  = 35.0   // marker appears on map
    const val AR_TRIGGER_RADIUS_M  =  10.0   // CAPTURE button shown
    const val PROXIMITY_RADIUS_M   = 35.0   // keep same as visibility for hint text
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
        "\uD83C\uDFCE\uFE0F", "⚡", "\uD83D\uDE0D", "💥", "\uD83E\uDD64", "🏆", "🎮", "\uD83D\uDE08",
    )
}