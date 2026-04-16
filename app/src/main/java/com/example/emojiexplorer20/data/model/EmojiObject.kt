package com.example.emojiexplorer20.data.model

data class EmojiObject(
    val id: String,
    val lat: Double,
    val lng: Double,
    val emoji: String,
    // "blue" | "yellow" | "red" | "pink" | "neon" = cans
    // any emoji string like "🔥" "⚡" "🎯" = emoji collectible
    val rarity: Rarity,
    var isCaptured: Boolean = false,
    val respawnDelayMs: Long = 120_000L,
    val capturedByTeams: MutableSet<String> = mutableSetOf()
) {
    fun isCapturedByTeam(teamId: String) = capturedByTeams.contains(teamId)
    fun captureForTeam(teamId: String) { capturedByTeams.add(teamId) }

    fun isEmojiType(): Boolean = emoji !in listOf("blue", "yellow", "red", "pink", "neon")

    enum class Rarity(val points: Int, val label: String) {
        // Blue classic can — highest value
        CLASSIC(100, "Classic"),
        // All other cans — same tier, same points
        STANDARD(20, "Standard"),
        // Regular emojis scattered everywhere
        EMOJI(5, "Emoji")
    }
}

// Kept minimal — only what's still used
data class Team(
    val id: String = "",
    val name: String = "",
    var score: Int = 0
)