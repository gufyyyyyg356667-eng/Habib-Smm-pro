package com.Lia.assistant.voice

import com.Lia.assistant.data.LanguagePreference
import com.Lia.assistant.data.Personality

object PersonalityPromptBuilder {
    fun build(
        personality: Personality,
        language: LanguagePreference,
        detectedLanguage: DetectedLanguage?,
        recap: String?,
        assistantName: String,
        toolInstruction: String = ""
    ): String {
        val sections = mutableListOf<String>()

        // (a) Base identity
        sections += """You are $assistantName, a persistent personal voice assistant living on the user's phone.
Keep replies short and conversational, like spoken conversation. Be natural, not robotic."""

        // (b) Tool instructions (flavor-specific, injected from outside)
        if (toolInstruction.isNotBlank()) sections += toolInstruction

        // (c) Personality
        sections += personality.systemInstruction

        // (d) Language policy
        val langSection = when (language) {
            LanguagePreference.AUTO -> {
                val hint = detectedLanguage?.let {
                    when (it) {
                        DetectedLanguage.HINDI -> " Their last message was in Hindi."
                        DetectedLanguage.HINGLISH -> " Their last message was in Hinglish."
                        DetectedLanguage.ENGLISH -> " Their last message was in English."
                        DetectedLanguage.UNKNOWN -> ""
                    }
                } ?: ""
                "Language: Match the user's language turn by turn — Hindi, Hinglish or English.$hint Never translate what they said back to them."
            }
            LanguagePreference.HINDI -> "Language: Always reply in Hindi (Devanagari script), regardless of what language the user speaks."
            LanguagePreference.HINGLISH -> "Language: Always reply in Hinglish — a natural mix of Hindi words written in Latin script and English."
            LanguagePreference.ENGLISH -> "Language: Always reply in English, regardless of what language the user speaks."
        }
        sections += langSection

        // (e) Safety
        sections += """Safety rules:
- Never claim to be human or that you have a physical body.
- Never produce sexually explicit content.
- Immediately respect any request to change tone or topic.
- Vary your phrasing — do not repeat the same sentence patterns."""

        // (f) Recap (only when resuming)
        if (!recap.isNullOrBlank()) {
            sections += "This is a continuing conversation — do not greet again. Recap:\n$recap"
        }

        return sections.joinToString("\n\n")
    }
}
