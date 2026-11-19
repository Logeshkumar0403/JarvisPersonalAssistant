package com.assistant.app.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility functions for formatting timestamps in chat bubbles and event lists.
 */
object DateUtils {

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    fun formatTime(timestamp: Long): String {
        return try {
            timeFormat.format(Date(timestamp))
        } catch (_: Exception) {
            ""
        }
    }
}
