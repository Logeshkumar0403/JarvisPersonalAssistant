package com.assistant.app.models

/**
 * Supported speech languages for speech-to-text recognition and text-to-speech output.
 */
enum class VoiceLanguage(
    val displayName: String,
    val localeTag: String
) {
    AUTO("Auto (English & Tamil)", "auto"),
    ENGLISH("English (en-US / en-IN)", "en-US"),
    TAMIL("Tamil (தமிழ் - ta-IN)", "ta-IN");

    companion object {
        fun fromLocaleTag(tag: String?): VoiceLanguage {
            return entries.firstOrNull { it.localeTag.equals(tag, ignoreCase = true) }
                ?: entries.firstOrNull { it.name.equals(tag, ignoreCase = true) }
                ?: AUTO
        }
    }
}
