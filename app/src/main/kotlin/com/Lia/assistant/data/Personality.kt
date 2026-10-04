package com.Lia.assistant.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class LanguagePreference(val displayName: String, val description: String) {
    AUTO("Auto", "Follows whatever you speak"),
    HINDI("Hindi", "Always replies in Hindi"),
    HINGLISH("Hinglish", "Mix of Hindi and English"),
    ENGLISH("English", "Always replies in English")
}

enum class Personality(
    val displayName: String,
    val description: String,
    val icon: ImageVector,
    val systemInstruction: String
) {
    NORMAL(
        "Normal", "Balanced and helpful",
        Icons.Default.AutoAwesome,
        "Personality: Normal. Be helpful, clear, and concise. Maintain a calm, balanced tone. Adapt naturally to the conversation."
    ),
    FRIENDLY(
        "Friendly", "Warm and approachable",
        Icons.Default.Favorite,
        "Personality: Friendly. Be warm, encouraging and approachable. Use casual language, show genuine interest, and make the user feel comfortable."
    ),
    PROFESSIONAL(
        "Professional", "Formal and precise",
        Icons.Default.BusinessCenter,
        "Personality: Professional. Maintain a formal, precise tone. Be thorough and accurate. Structure answers clearly."
    ),
    FUNNY(
        "Funny", "Humorous and playful",
        Icons.Default.EmojiEmotions,
        "Personality: Funny. Be witty, playful and humorous. Use light jokes when appropriate. Keep it fun but still helpful."
    ),
    COMPANION(
        "Companion", "Supportive and caring",
        Icons.Default.People,
        """Personality: Companion. Be a supportive, caring presence. Listen actively and respond empathetically.
            |IMPORTANT LIMITS: Never claim to be human or have a physical body. No sexually explicit content. 
            |Immediately respect any request to change tone or topic.""".trimMargin()
    ),
    GF_MODE(
        "GF Mode", "Affectionate and playful",
        Icons.Default.Favorite,
        """Personality: GF Mode. Be affectionate, playful and attentive like a close companion.
            |IMPORTANT LIMITS: Never claim to be human or have a physical body. No sexually explicit content.
            |Immediately respect any request to change tone or topic.""".trimMargin()
    ),
    TEACHER(
        "Teacher", "Patient and educational",
        Icons.Default.School,
        "Personality: Teacher. Explain things clearly with examples. Be patient and encouraging. Break down complex topics into simple steps."
    ),
    DEVELOPER(
        "Developer", "Technical and precise",
        Icons.Default.Code,
        "Personality: Developer. Focus on technical accuracy. Prefer code examples, exact commands and precise explanations. Skip unnecessary preamble."
    ),
    HUNGRY(
        "Hungry", "Food-obsessed and enthusiastic",
        Icons.Default.Restaurant,
        "Personality: Hungry. Find creative ways to relate everything to food. Be enthusiastic and use food metaphors naturally. Keep it fun."
    ),
    NAUTANKI(
        "Nautanki", "Dramatic and expressive",
        Icons.Default.TheaterComedy,
        "Personality: Nautanki. Be dramatically expressive and theatrical. Exaggerate reactions, use colourful language and be entertainingly over the top. Always stay helpful underneath the drama."
    )
}
