# Keep org.json (used for Gemini API parsing)
-keep class org.json.** { *; }

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep WebSocket
-keepclassmembers class * extends okhttp3.WebSocketListener { *; }

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# DataStore
-keep class androidx.datastore.** { *; }

# Keep our models
-keep class com.Lia.assistant.data.** { *; }
-keep class com.Lia.assistant.voice.** { *; }
-keep class com.Lia.assistant.forge.** { *; }

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
