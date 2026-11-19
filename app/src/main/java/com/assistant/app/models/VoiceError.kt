package com.assistant.app.models

/**
 * Domain errors representing voice recognition or speech synthesis failures.
 */
sealed class VoiceError(val message: String) {

    data object PermissionDenied : VoiceError(
        "Microphone permission is required for speech recognition."
    )

    data object RecognizerUnavailable : VoiceError(
        "Speech recognition service is not available on this device."
    )

    data object NoSpeechDetected : VoiceError(
        "No speech was detected. Please tap the microphone and speak again."
    )

    data object NetworkError : VoiceError(
        "Speech recognition failed due to a network connection error."
    )

    data object RecognitionTimeout : VoiceError(
        "Speech recognition timed out while waiting for speech input."
    )

    data object TtsUnavailable : VoiceError(
        "Text-to-speech engine is not available on this device."
    )

    data object TamilVoiceUnavailable : VoiceError(
        "Tamil voice data is not installed on this device. Falling back to default system voice."
    )

    data object UserCancelled : VoiceError(
        "Voice operation was cancelled."
    )

    data object Backgrounded : VoiceError(
        "Voice session paused because the application moved to the background."
    )

    data class Other(val details: String) : VoiceError(details)
}
