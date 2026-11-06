package com.example.retrocompanion.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** Fires a haptic tick the moment [isRefreshing] transitions from true to false (refresh completed). */
@Composable
fun RefreshCompletionHaptic(isRefreshing: Boolean) {
    val haptic = LocalHapticFeedback.current
    var wasRefreshing by remember { mutableStateOf(isRefreshing) }
    LaunchedEffect(isRefreshing) {
        if (wasRefreshing && !isRefreshing) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        wasRefreshing = isRefreshing
    }
}
