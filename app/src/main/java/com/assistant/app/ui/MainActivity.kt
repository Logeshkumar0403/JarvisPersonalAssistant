package com.assistant.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.assistant.app.AssistantApplication
import com.assistant.app.navigation.AssistantNavHost
import com.assistant.app.ui.theme.AssistantTheme
import com.assistant.app.viewmodels.AssistantViewModel
import com.assistant.app.viewmodels.AssistantViewModelFactory

/**
 * Main Activity hosting the Jetpack Compose assistant interface.
 * Implements lifecycle awareness to cancel/pause voice operations when backgrounded.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: AssistantViewModel by viewModels {
        val app = application as AssistantApplication
        AssistantViewModelFactory(
            repository = app.container.assistantRepository,
            voiceRepository = app.container.voiceRepository,
            voiceSettingsRepository = app.container.voiceSettingsRepository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AssistantTheme {
                AssistantNavHost(viewModel = viewModel)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Cancel or pause voice work when the app goes into the background
        viewModel.onAppBackgrounded()
    }
}
