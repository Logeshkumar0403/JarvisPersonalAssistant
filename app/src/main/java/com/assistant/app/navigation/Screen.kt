package com.assistant.app.navigation

/**
 * Navigation destinations for the Assistant app.
 */
sealed class Screen(val route: String, val title: String) {
    data object Home : Screen("home", "AI Assistant")
    data object Memory : Screen("memory", "Personal Memory")
    data object Reminders : Screen("reminders", "Reminders & Tasks")
    data object Settings : Screen("settings", "Settings")
}
