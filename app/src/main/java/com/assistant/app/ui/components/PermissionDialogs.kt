package com.assistant.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Dialog shown when Android suggests displaying a rationale to the user
 * before requesting the microphone permission again.
 */
@Composable
fun MicrophoneRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Microphone Permission Required",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = "The assistant needs access to your microphone to listen to your voice and enable voice commands in Phase 3. In Phase 1, granting this exercises the permission flow and activates the listening UI state.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Allow Access")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        }
    )
}

/**
 * Dialog shown when the microphone permission has been permanently denied
 * (user selected "Don't ask again" or denied multiple times). Directs user to App Settings.
 */
@Composable
fun MicrophonePermanentlyDeniedDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "Permission Permanently Denied",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = "Microphone permission has been disabled for this app. To test or enable voice features, please open the system App Settings and enable the Microphone permission manually.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
