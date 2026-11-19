package com.assistant.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.assistant.app.ui.screens.HomeScreen
import com.assistant.app.ui.screens.MemoryScreen
import com.assistant.app.ui.screens.RemindersScreen
import com.assistant.app.ui.screens.SettingsScreen
import com.assistant.app.viewmodels.AssistantViewModel

/**
 * Root navigation graph managing navigation between:
 * - Assistant Home Screen
 * - Personal Memory Placeholder Screen
 * - Reminders & Tasks Placeholder Screen
 * - Settings Placeholder Screen
 */
@Composable
fun AssistantNavHost(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToMemory = { navController.navigate(Screen.Memory.route) },
                onNavigateToReminders = { navController.navigate(Screen.Reminders.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Memory.route) {
            MemoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Reminders.route) {
            RemindersScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
