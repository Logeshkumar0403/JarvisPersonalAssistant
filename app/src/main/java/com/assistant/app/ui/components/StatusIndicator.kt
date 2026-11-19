package com.assistant.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.assistant.app.models.AssistantStatus
import com.assistant.app.ui.theme.StatusErrorColor
import com.assistant.app.ui.theme.StatusListeningColor
import com.assistant.app.ui.theme.StatusProcessingColor
import com.assistant.app.ui.theme.StatusSpeakingColor

/**
 * Visual indicator displaying the assistant's active state:
 * - LISTENING: Animated green badge indicating audio capture state with stop/cancel control
 * - SPEAKING: Animated purple badge indicating speech playback with visible stop control
 * - PROCESSING: Blue circular progress indicator representing request execution
 * - ERROR: Red error notification banner with dismiss action
 * - IDLE: Collapsed / hidden
 */
@Composable
fun StatusIndicator(
    status: AssistantStatus,
    errorMessage: String?,
    onDismissError: () -> Unit,
    onStopSpeaking: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = status != AssistantStatus.IDLE,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            color = when (status) {
                AssistantStatus.LISTENING -> StatusListeningColor.copy(alpha = 0.15f)
                AssistantStatus.SPEAKING -> StatusSpeakingColor.copy(alpha = 0.15f)
                AssistantStatus.PROCESSING -> StatusProcessingColor.copy(alpha = 0.15f)
                AssistantStatus.ERROR -> StatusErrorColor.copy(alpha = 0.15f)
                AssistantStatus.IDLE -> MaterialTheme.colorScheme.surfaceVariant
            },
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    when (status) {
                        AssistantStatus.LISTENING -> {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(StatusListeningColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Listening... (tap mic to finish)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = StatusListeningColor
                            )
                        }

                        AssistantStatus.SPEAKING -> {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(StatusSpeakingColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Speaking assistant response...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = StatusSpeakingColor
                            )
                        }

                        AssistantStatus.PROCESSING -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = StatusProcessingColor
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Routing through architecture seam...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = StatusProcessingColor
                            )
                        }

                        AssistantStatus.ERROR -> {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = StatusErrorColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMessage ?: "A voice error occurred.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = StatusErrorColor
                            )
                        }

                        AssistantStatus.IDLE -> {
                            // Hidden by AnimatedVisibility
                        }
                    }
                }

                // Action button on the right side
                when (status) {
                    AssistantStatus.SPEAKING -> {
                        Surface(
                            onClick = onStopSpeaking,
                            shape = RoundedCornerShape(8.dp),
                            color = StatusSpeakingColor.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop playback",
                                    tint = StatusSpeakingColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Stop",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusSpeakingColor
                                )
                            }
                        }
                    }

                    AssistantStatus.ERROR -> {
                        IconButton(
                            onClick = onDismissError,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss error",
                                tint = StatusErrorColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}
