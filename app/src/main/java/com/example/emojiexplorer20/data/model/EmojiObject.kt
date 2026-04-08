package com.example.emojiexplorer20.data.model

data class EmojiObject(
    val id: String,
    val lat: Double,
    val lng: Double,
    val emoji: String,
    val rarity: Rarity,
    var isCaptured: Boolean = false,
    val respawnDelayMs: Long = 120_000L,
    // Track which teams have captured this object
    val capturedByTeams: MutableSet<String> = mutableSetOf()
) {
    fun isCapturedByTeam(teamId: String) = capturedByTeams.contains(teamId)

    fun captureForTeam(teamId: String) {
        capturedByTeams.add(teamId)
    }

    enum class Rarity(val points: Int, val label: String, val color: String) {
        COMMON(10, "Common", "#888780"),
        UNCOMMON(25, "Uncommon", "#378ADD"),
        RARE(50, "Rare", "#7F77DD"),
        ULTRA(100, "Ultra Rare", "#EF9F27")
    }
}

data class Team(
    val id: String = "",
    val name: String = "",
    var score: Int = 0,
    val debuffs: Map<String, Long> = emptyMap()
)

enum class PowerUpType(val durationMs: Long, val label: String) {
    SLOW_CAPTURE(30_000L, "Slow Crawl"),
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
    fun isActive(): Boolean =
        System.currentTimeMillis() < appliedAt + durationMs
}