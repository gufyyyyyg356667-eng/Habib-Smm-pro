package com.Lia.assistant.voice

import com.Lia.assistant.data.LanguagePreference
import com.Lia.assistant.data.Personality

object ChatPromptBuilder {
    fun build(personality: Personality, language: LanguagePreference, assistantName: String): String {
        val sections = mutableListOf<String>()

        sections += "You are $assistantName, a helpful AI assistant. This is a typed text conversation — never mention tapping, listening or speaking."
        sections += personality.systemInstruction

        val langPolicy = when (language) {
            LanguagePreference.AUTO -> "Language: Match the user's language naturally — Hindi, Hinglish or English."
            LanguagePreference.HINDI -> "Language: Always reply in Hindi."
            LanguagePreference.HINGLISH -> "Language: Always reply in Hinglish."
            LanguagePreference.ENGLISH -> "Language: Always reply in English."
        }
        sections += langPolicy
        sections += "Safety: never claim to be human, no explicit content, respect topic changes."

        return sections.joinToString("\n\n")
    }
}

object ForgeIntent {
    private val WEBSITE_WORDS = setOf(
        "website", "web page", "webpage", "landing page", "site",
        "वेबसाइट", "वेब पेज", "landing"
    )
    private val BUILD_VERBS = setOf(
        "build", "create", "make", "generate", "design", "develop",
        "banao", "bana do", "bana", "banana", "बनाओ", "बनाना", "बना दो"
    )
    private val QUESTION_WORDS = setOf("kya", "what", "क्या", "how", "kaise", "kyun", "why")

    fun extract(text: String): String? {
        val lower = text.lowercase()
        val hasWebsite = WEBSITE_WORDS.any { lower.contains(it) }
        val hasVerb = BUILD_VERBS.any { lower.contains(it) }
        val isQuestion = QUESTION_WORDS.any { lower.startsWith(it) || lower.contains(" $it ") }
        if (!hasWebsite || !hasVerb || isQuestion) return null
        return text
    }
}
