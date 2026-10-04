package com.Lia.assistant.action

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

// ── Installed App Cache ────────────────────────────────────────────────────
object InstalledAppLabelCache {
    private const val TTL_MS = 15 * 60 * 1000L
    private val mutex = Mutex()
    private var cache: Map<String, String> = emptyMap() // lowercase label -> package
    private var lastBuilt = 0L

    suspend fun prewarm(ctx: Context) { get(ctx) }

    suspend fun get(ctx: Context): Map<String, String> = mutex.withLock {
        val now = System.currentTimeMillis()
        if (cache.isNotEmpty() && now - lastBuilt < TTL_MS) return@withLock cache
        val pm = ctx.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val map = mutableMapOf<String, String>()
        for (app in apps) {
            if (pm.getLaunchIntentForPackage(app.packageName) != null) {
                val label = pm.getApplicationLabel(app).toString().lowercase()
                map[label] = app.packageName
            }
        }
        cache = map
        lastBuilt = now
        map
    }
}

// ── App Launcher ──────────────────────────────────────────────────────────
object AppLauncher {
    suspend fun findApp(ctx: Context, query: String): String? {
        val q = query.lowercase().trim()
        val apps = InstalledAppLabelCache.get(ctx)

        // Exact match first
        apps[q]?.let { return it }

        // Prefix/word match
        var bestPkg: String? = null
        var bestScore = Int.MAX_VALUE

        for ((label, pkg) in apps) {
            if (label.length < 3 && q.length < 3) continue
            val isMatch = when {
                label.startsWith(q) -> true
                q.startsWith(label) && label.length >= 3 -> true
                q.split(" ").all { word -> word.length >= 2 && label.split(" ").any { it.startsWith(word) } } -> true
                else -> false
            }
            if (isMatch) {
                val score = kotlin.math.abs(label.length - q.length)
                if (score < bestScore) {
                    bestScore = score
                    bestPkg = pkg
                }
            }
        }
        return bestPkg
    }

    fun launch(ctx: Context, packageName: String): Boolean {
        return try {
            val intent = ctx.packageManager.getLaunchIntentForPackage(packageName) ?: return false
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            true
        } catch (_: Exception) { false }
    }
}

// ── Contact Resolver ───────────────────────────────────────────────────────
sealed class ContactResult {
    data class Found(val displayName: String, val number: String) : ContactResult()
    object NoPermission : ContactResult()
    object NotFound : ContactResult()
}

object ContactResolver {
    private val PHONE_REGEX = Regex("[\\d\\s+\\-().]{5,}")

    fun lookup(ctx: Context, query: String): ContactResult {
        // Direct phone number
        if (PHONE_REGEX.matches(query.filter { it.isDigit() || it in "+- ()." })) {
            return ContactResult.Found(query, query.filter { it.isDigit() || it == '+' })
        }

        // Check permission
        if (ctx.checkSelfPermission(android.Manifest.permission.READ_CONTACTS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return ContactResult.NoPermission

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val args = arrayOf("%$query%")

        val results = mutableListOf<Pair<String, String>>() // name, number
        var cursor: Cursor? = null
        try {
            cursor = ctx.contentResolver.query(uri, projection, selection, args, null)
            cursor?.use { c ->
                while (c.moveToNext() && results.size < 5) {
                    val name = c.getString(0) ?: continue
                    val number = c.getString(1) ?: continue
                    results.add(Pair(name, number))
                }
            }
        } catch (_: Exception) {
        } finally {
            cursor?.close()
        }

        if (results.isEmpty()) return ContactResult.NotFound

        // Rank: exact > prefix > contains
        val lower = query.lowercase()
        val exact = results.find { it.first.lowercase() == lower }
        val prefix = results.find { it.first.lowercase().startsWith(lower) }
        val best = exact ?: prefix ?: results.first()
        return ContactResult.Found(best.first, best.second.filter { it.isDigit() || it == '+' })
    }
}

// ── Tool Declaration Helper ────────────────────────────────────────────────
fun functionDecl(name: String, description: String, params: Map<String, String>, required: List<String>): org.json.JSONObject {
    val properties = org.json.JSONObject()
    for ((k, v) in params) {
        properties.put(k, org.json.JSONObject().put("type", "STRING").put("description", v))
    }
    return org.json.JSONObject()
        .put("name", name)
        .put("description", description)
        .put("parameters", org.json.JSONObject()
            .put("type", "OBJECT")
            .put("properties", properties)
            .put("required", org.json.JSONArray(required)))
}
