package com.Lia.assistant.voice

enum class DetectedLanguage { HINDI, HINGLISH, ENGLISH, UNKNOWN }

object LanguageDetector {
    private val HINGLISH_MARKERS = setOf(
        "hai", "hain", "ho", "kya", "kyun", "kaise", "nahi", "haan",
        "mujhe", "tumhe", "aap", "mera", "yaar", "bhai", "accha", "theek",
        "matlab", "karna", "karo", "raha", "chahiye", "batao", "suno",
        "dekho", "abhi", "phir", "lekin", "bahut", "thoda", "kuch",
        "sab", "wala", "wali", "toh", "bhi", "aur", "par", "mein",
        "se", "ko", "ka", "ki", "ke", "ek", "do", "tera", "uska",
        "unka", "hamara", "tumhara", "lagta", "lagti", "jaana", "aana",
        "dena", "lena", "bolta", "bolti", "sunna", "dekhna"
    )

    fun detect(text: String): DetectedLanguage {
        if (text.isBlank()) return DetectedLanguage.UNKNOWN

        // Check for Devanagari characters
        if (text.any { it.code in 0x0900..0x097F }) return DetectedLanguage.HINDI

        val lower = text.lowercase()
        val words = lower.split(Regex("[^a-zA-Z']+")).filter { it.isNotBlank() }

        if (words.isEmpty()) return DetectedLanguage.UNKNOWN

        val hits = words.count { it in HINGLISH_MARKERS }

        return when {
            words.size <= 4 && hits >= 1 -> DetectedLanguage.HINGLISH
            words.size > 4 && (hits >= 2 || hits.toFloat() / words.size >= 0.15f) -> DetectedLanguage.HINGLISH
            else -> DetectedLanguage.ENGLISH
        }
    }
}
