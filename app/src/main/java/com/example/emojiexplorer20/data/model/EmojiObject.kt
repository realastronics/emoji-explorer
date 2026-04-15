package com.example.emojiexplorer20.data.model

data class EmojiObject(
    val id: String,
    val lat: Double,
    val lng: Double,
    val emoji: String,         // now stores: "blue" | "yellow" | "red" | "pink"
    val rarity: Rarity,
    var isCaptured: Boolean = false,
    val respawnDelayMs: Long = 120_000L,
    val capturedByTeams: MutableSet<String> = mutableSetOf(),
    val type: ObjectType = ObjectType.COLLECTIBLE,
    val powerUpType: PowerUpType? = null
) {
    fun isCapturedByTeam(teamId: String) = capturedByTeams.contains(teamId)
    fun captureForTeam(teamId: String) { capturedByTeams.add(teamId) }

    // Returns the correct drawable resource ID for this can
    fun getCanDrawableId(): Int = when (emoji) {
        "blue"   -> android.R.drawable.btn_default  // placeholder — replaced below
        "yellow" -> android.R.drawable.btn_default
        "red"    -> android.R.drawable.btn_default
        "pink"   -> android.R.drawable.btn_default
        else     -> android.R.drawable.btn_default
    }

    enum class ObjectType {
        COLLECTIBLE,
        POWERUP
    }

    enum class Rarity(val points: Int, val label: String) {
        COMMON(10, "Common"),
        UNCOMMON(25, "Uncommon"),
        RARE(50, "Rare"),
        ULTRA(100, "Ultra Rare")
    }
}

data class Team(
    val id: String = "",
    val name: String = "",
    var score: Int = 0,
    val debuffs: Map<String, Long> = emptyMap()
)

enum class PowerUpType(val durationMs: Long, val label: String) {
    SLOW_CAPTURE(10_000L, "Slow Crawl"),
    SHRINK_ZONE(45_000L, "Shrink Zone"),
    DOUBLE_POINTS(20_000L, "Double Down")
}

data class PowerUp(
    val type: PowerUpType,
    val fromTeamId: String,
    val targetTeamId: String,
    val appliedAt: Long = System.currentTimeMillis(),
    val durationMs: Long = type.durationMs
) {
    fun isActive(): Boolean = System.currentTimeMillis() < appliedAt + durationMs
}