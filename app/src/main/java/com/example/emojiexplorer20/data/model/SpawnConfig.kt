package com.example.emojiexplorer20.data.model

object SpawnConfig {

    val SPAWN_POINTS = listOf(

        EmojiObject("sp_01", 28.2475011, 76.8128989, "\uD83D\uDEDE", EmojiObject.Rarity.COMMON), // 🛞
        EmojiObject("sp_02", 28.2472867, 76.8130619, "\uD83D\uDEA6", EmojiObject.Rarity.UNCOMMON), //🚦
        EmojiObject("sp_07", 28.2469092, 76.8129764, "\uD83C\uDFC1", EmojiObject.Rarity.RARE), // 🏁
        EmojiObject("sp_10", 28.2476269, 76.8136744, "\uD83D\uDCA6", EmojiObject.Rarity.COMMON), // 💦
        EmojiObject("sp_10", 28.2471015, 76.8135775, "\uD83C\uDFCE\uFE0F", EmojiObject.Rarity.ULTRA), // 🏎️
        EmojiObject("sp_07", 28.2472294, 76.8151845, "\uD83C\uDFC6", EmojiObject.Rarity.RARE), // 🏆

        EmojiObject("sp_01", 28.2483736, 76.8124088, "\uD83D\uDEDE", EmojiObject.Rarity.COMMON), // 🛞
        EmojiObject("sp_02", 28.2486677, 76.8111149, "\uD83D\uDEA6", EmojiObject.Rarity.UNCOMMON), //🚦
        EmojiObject("sp_07", 28.2467022, 76.8111216, "\uD83C\uDFC1", EmojiObject.Rarity.RARE), // 🏁
        EmojiObject("sp_10", 28.2466396, 76.8123920, "\uD83D\uDCA6", EmojiObject.Rarity.COMMON), // 💦
        EmojiObject("sp_10", 28.2462819, 76.8119109, "\uD83C\uDFCE\uFE0F", EmojiObject.Rarity.ULTRA), // 🏎️
        EmojiObject("sp_07", 28.2469069, 76.8119645, "\uD83C\uDFC6", EmojiObject.Rarity.RARE), // 🏆

        EmojiObject("sp_01", 28.2470929, 76.8124037, "\uD83D\uDEDE", EmojiObject.Rarity.COMMON), // 🛞
        EmojiObject("sp_02", 28.2464402, 76.8149689, "\uD83D\uDEA6", EmojiObject.Rarity.UNCOMMON), //🚦
        EmojiObject("sp_07", 28.2475640, 76.8142048, "\uD83C\uDFC1", EmojiObject.Rarity.RARE), // 🏁
        EmojiObject("sp_10", 28.2479645, 76.8130967, "\uD83D\uDCA6", EmojiObject.Rarity.COMMON), // 💦
        EmojiObject("sp_10", 28.2473425, 76.8134545, "\uD83C\uDFCE\uFE0F", EmojiObject.Rarity.ULTRA), // 🏎️
        EmojiObject("sp_07", 28.2470752, 76.8146471, "\uD83C\uDFC6", EmojiObject.Rarity.RARE), // 🏆

        EmojiObject("sp_01", 28.2475011, 76.8126813, "\uD83D\uDEDE", EmojiObject.Rarity.COMMON), // 🛞
        EmojiObject("sp_02", 28.2476706, 76.8132775, "\uD83D\uDEA6", EmojiObject.Rarity.UNCOMMON), //🚦
        EmojiObject("sp_07", 28.2469264, 76.8134655, "\uD83C\uDFC1", EmojiObject.Rarity.RARE), // 🏁
        EmojiObject("sp_10", 28.2463785, 76.8134441, "\uD83D\uDCA6", EmojiObject.Rarity.COMMON), // 💦
        EmojiObject("sp_10", 28.2472185, 76.8119451, "\uD83C\uDFCE\uFE0F", EmojiObject.Rarity.ULTRA), // 🏎️
        EmojiObject("sp_07", 28.2474595, 76.8158899, "\uD83C\uDFC6", EmojiObject.Rarity.RARE), // 🏆

        EmojiObject("sp_01", 28.2468410, 76.8143138, "\uD83D\uDEDE", EmojiObject.Rarity.COMMON), // 🛞
        EmojiObject("sp_02", 28.2466540, 76.8139866, "\uD83D\uDEA6", EmojiObject.Rarity.UNCOMMON), //🚦
        EmojiObject("sp_07", 28.2467618, 76.8127393, "\uD83C\uDFC1", EmojiObject.Rarity.RARE), // 🏁
        EmojiObject("sp_10", 28.2484731, 76.8127393, "\uD83D\uDCA6", EmojiObject.Rarity.COMMON), // 💦
        EmojiObject("sp_10", 28.2471898, 76.8126981, "\uD83C\uDFCE\uFE0F", EmojiObject.Rarity.ULTRA), // 🏎️
        EmojiObject("sp_07", 28.2472465, 76.8143725, "\uD83C\uDFC6", EmojiObject.Rarity.RARE), // 🏆

        EmojiObject("sp_01", 28.2470646, 76.8143098, "\uD83D\uDEDE", EmojiObject.Rarity.COMMON), // 🛞
        EmojiObject("sp_02", 28.2471854, 76.8139547, "\uD83D\uDEA6", EmojiObject.Rarity.UNCOMMON), //🚦
        EmojiObject("sp_07", 28.2474116, 76.8143188, "\uD83C\uDFC1", EmojiObject.Rarity.RARE), // 🏁
        EmojiObject("sp_10", 28.2476195, 76.8127558, "\uD83D\uDCA6", EmojiObject.Rarity.COMMON), // 💦
        EmojiObject("sp_10", 28.2480924, 76.8122482, "\uD83C\uDFCE\uFE0F", EmojiObject.Rarity.ULTRA), // 🏎️
        EmojiObject("sp_07", 28.2480894, 76.8118663, "\uD83C\uDFC6", EmojiObject.Rarity.RARE), // 🏆


    )

    const val PROXIMITY_RADIUS_M        = 20.0
    const val AR_TRIGGER_RADIUS_M       = 25.0
    const val CAPTURE_HOLD_MS           = 1800L
    const val CAPTURE_HOLD_SLOW_MS      = 3600L
    const val SHRINK_ZONE_RADIUS_M      = 3.0
    const val GPS_ACCURACY_GATE_M       = 10f
    const val GPS_JUMP_THRESHOLD_M      = 50.0
    const val LEADERBOARD_SYNC_MS       = 4000L
    const val GPS_SMOOTH_WINDOW         = 3
}