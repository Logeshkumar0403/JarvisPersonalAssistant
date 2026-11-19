package com.assistant.app.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.assistant.app.ui.components.ChatInputField
import com.assistant.app.ui.components.MessageBubble
import com.assistant.app.ui.components.MicrophonePermanentlyDeniedDialog
import com.assistant.app.ui.components.MicrophoneRationaleDialog
import com.assistant.app.ui.components.StatusIndicator
import com.assistant.app.ui.components.TestStateControls
import com.assistant.app.utils.PermissionUtils
import com.assistant.app.viewmodels.AssistantViewModel

/**
 * Assistant Home Screen hosting:
 * - Conversation message history with per-message TTS speaker controls
 * - Runtime permission handling for microphone
 * - Interactive input bar with real Speech-to-Text recognition
 * - Visual indicators for Listening, Speaking, Processing, and Error states
 * - Navigation entry points to Memory, Reminders, and Settings
 * - ViewModel state testing controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AssistantViewModel,
    onNavigateToMemory: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val activity = context as? Activity

    // Standard Android runtime permission flow launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val shouldShowRationale = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.RECORD_AUDIO)
        } ?: false
        viewModel.onPermissionResult(isGranted, shouldShowRationale)
    }

    // Synchronize initial runtime permission state on first composition
    LaunchedEffect(Unit) {
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) {
            viewModel.onPermissionAlreadyGranted()
        }
    }

    // Auto-scroll to the newest message whenever the list changes
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI Assistant",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Phase 2 • Voice Engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Test state panel toggle
                    IconButton(onClick = { viewModel.toggleTestControls() }) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Toggle Test Controls",
                            tint = if (uiState.showTestControls) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Navigation: Personal Memory Placeholder
                    IconButton(onClick = onNavigateToMemory) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Personal Memory"
                        )
                    }

                    // Navigation: Reminders Placeholder
                    IconButton(onClick = onNavigateToReminders) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Reminders & Tasks"
                        )
                    }

                    // Navigation: Settings Screen (Voice Engine Settings)
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                // Collapsible Test State Controls Panel
                TestStateControls(
                    visible = uiState.showTestControls,
                    onTestListening = { viewModel.triggerTestListeningState() },
                    onTestProcessing = { viewModel.triggerTestProcessingState() },
                    onTestError = { viewModel.triggerTestErrorState() },
                    onResetIdle = { viewModel.resetStatusToIdle() },
                    onClearChat = { viewModel.clearConversation() }
                )

                // Input Bar with text field, mic button, and send button
                ChatInputField(
                    inputText = uiState.inputText,
                    onInputTextChanged = { viewModel.onInputTextChanged(it) },
                    onSendMessage = { viewModel.sendMessage() },
                    onMicClicked = {
                        val isGranted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (isGranted) {
                            viewModel.onMicClicked()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    isListening = uiState.isListening,
                    isProcessing = uiState.isProcessing,
                    isSpeaking = uiState.isSpeaking
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Visual state banner: LISTENING, SPEAKING, PROCESSING, or ERROR
            StatusIndicator(
                status = uiState.status,
                errorMessage = uiState.errorMessage,
                onDismissError = { viewModel.resetStatusToIdle() },
                onStopSpeaking = { viewModel.stopPlayback() }
            )

            // Conversation Messages Area
            if (uiState.messages.isEmpty()) {
                EmptyStateCard(
                    onTestMessage = {
                        viewModel.onInputTextChanged("Hello! Testing speech and text-to-speech.")
                        viewModel.sendMessage()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            onSpeakMessage = { id, text -> viewModel.speakMessage(id, text) },
                            isSpeakingThisMessage = uiState.isSpeaking && uiState.currentlySpeakingMessageId == message.id
                        )
                    }
                }
            }
        }
    }

    // Permission Rationale Dialog
    if (uiState.showPermissionRationale) {
        MicrophoneRationaleDialog(
            onConfirm = {
                viewModel.dismissRationaleDialog()
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            onDismiss = {
                viewModel.dismissRationaleDialog()
            }
        )
    }

    // Permission Permanently Denied Dialog
    if (uiState.showPermanentlyDeniedDialog) {
        MicrophonePermanentlyDeniedDialog(
            onOpenSettings = {
                viewModel.dismissPermanentlyDeniedDialog()
                PermissionUtils.openAppSettings(context)
            },
            onDismiss = {
                viewModel.dismissPermanentlyDeniedDialog()
            }
        )
    }
}

@Composable
private fun EmptyStateCard(
    onTestMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Voice Engine Active",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Phase 2 Speech-To-Text & Text-To-Speech are active:\n\n" +
                            "• Tap the microphone to dictate in English, Tamil, or Tanglish. Recognized text is inserted into the input box for review.\n" +
                            "• Tap the speaker icon on any assistant message to read it aloud via TTS.\n" +
                            "• Configure auto voice-responses, speech rate, and pitch in Settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    onClick = onTestMessage,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Send Test Message",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}
