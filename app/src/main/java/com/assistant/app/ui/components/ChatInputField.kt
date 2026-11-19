package com.assistant.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.assistant.app.ui.theme.StatusListeningColor
import com.assistant.app.ui.theme.StatusSpeakingColor

/**
 * Text input field with send button and microphone activation button.
 * Microphone button is wired to real speech-to-text recognition and stops playback when speaking.
 */
@Composable
fun ChatInputField(
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onMicClicked: () -> Unit,
    isListening: Boolean,
    isProcessing: Boolean,
    isSpeaking: Boolean = false,
    modifier: Modifier = Modifier
) {
    val canSend = inputText.isNotBlank() && !isProcessing

    // Pulsing animation when listening
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val micButtonColor by animateColorAsState(
        targetValue = when {
            isListening -> StatusListeningColor
            isSpeaking -> StatusSpeakingColor
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "mic_color"
    )

    val micIconTint by animateColorAsState(
        targetValue = when {
            isListening || isSpeaking -> Color.White
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "mic_tint"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text Input Box
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChanged,
                placeholder = {
                    Text(
                        text = when {
                            isListening -> "Listening... (tap mic to stop)"
                            isSpeaking -> "Speaking response..."
                            else -> "Ask or type a message..."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                maxLines = 4,
                singleLine = false,
                enabled = !isProcessing
            )

            // Mic activation button (toggles listening / stops speaking)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .scale(if (isListening) pulseScale else 1f),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onMicClicked,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(micButtonColor),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = micButtonColor
                    )
                ) {
                    Icon(
                        imageVector = when {
                            isSpeaking -> Icons.Default.Stop
                            else -> Icons.Default.Mic
                        },
                        contentDescription = when {
                            isListening -> "Stop listening"
                            isSpeaking -> "Stop speaking"
                            else -> "Start voice recognition (Microphone)"
                        },
                        tint = micIconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Send Button
            IconButton(
                onClick = onSendMessage,
                enabled = canSend,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (canSend) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message",
                    tint = if (canSend) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
