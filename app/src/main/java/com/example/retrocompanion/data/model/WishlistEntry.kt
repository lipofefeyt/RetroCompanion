package com.example.retrocompanion.data.model

/**
 * A game the user wants to play later. Stores enough display data (title, console, icon) to
 * render the wishlist offline, including for games that aren't in the user's played library.
 */
data class WishlistEntry(
    val gameId: Int,
    val title: String,
    val consoleName: String? = null,
    val imageIcon: String? = null
) {
    val iconUrl: String
        get() = resolveMediaUrl(imageIcon)
}

/** Turns an API `ImageIcon` value (relative path, absolute path or full URL) into a full URL. */
fun resolveMediaUrl(icon: String?): String {
    if (icon == null) return ""
    return when {
        icon.startsWith("http://") || icon.startsWith("https://") -> icon
        icon.startsWith("/") -> "https://media.retroachievements.org$icon"
        else -> "https://media.retroachievements.org/Images/$icon"
    }
}
