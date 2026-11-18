package com.example.retrocompanion.data.model

/**
 * The minimum of one achievement unlock needed for activity stats: which achievement and when.
 * [date] is the API's raw "yyyy-MM-dd HH:mm:ss" string (interpreted as UTC), kept unparsed so
 * cached chunks stay valid if the device time zone changes.
 */
data class UnlockStamp(
    val achievementId: Int,
    val date: String
)
