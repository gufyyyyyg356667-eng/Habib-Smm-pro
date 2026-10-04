package com.Lia.assistant.forge

import android.content.Context
import androidx.core.content.FileProvider
import java.io.File

// ── ForgeHtml ────────────────────────────────────────────────────────────────
object ForgeHtml {
    fun stripFences(html: String): String =
        html.replace(Regex("^```html\\s*", RegexOption.MULTILINE), "")
            .replace(Regex("^```\\s*$", RegexOption.MULTILINE), "")
            .trim()

    fun looksLikeHtml(html: String): Boolean =
        html.contains("<html") || html.contains("<!DOCTYPE") || html.contains("<body")

    fun repairTruncated(html: String): String {
        val sb = StringBuilder(html)
        if (!html.contains("</script>") && html.contains("<script")) sb.append("</script>")
        if (!html.contains("</style>") && html.contains("<style")) sb.append("</style>")
        if (!html.contains("</body>")) sb.append("</body>")
        if (!html.contains("</html>")) sb.append("</html>")
        return sb.toString()
    }

    fun withSafetyNet(html: String): String {
        val script = """
            <script data-lia-safety-net="1">
            (function(){
              if(document.querySelector('[data-lia-safety-applied]'))return;
              document.documentElement.setAttribute('data-lia-safety-applied','1');
              function reveal(){
                document.querySelectorAll('*').forEach(function(el){
                  var st=getComputedStyle(el);
                  if(parseFloat(st.opacity)===0 && st.position!=='fixed' && st.pointerEvents!=='none' && el.getAttribute('aria-hidden')!=='true' && el.tagName.toLowerCase()!=='canvas'){
                    var r=el.getBoundingClientRect();
                    if(r.top<window.innerHeight && r.bottom>0 && r.width>0 && r.height>0){
                      el.style.opacity='1';el.style.transform='none';el.style.visibility='visible';
                    }
                  }
                });
              }
              setTimeout(reveal,1400);
            })();
            </script>
        """.trimIndent()
        return html.replace("</body>", "$script</body>")
    }
}

// ── Stage detection ──────────────────────────────────────────────────────────
object ForgeStageDetector {
    fun detect(html: String): ForgeStage = when {
        html.contains("</html>") -> ForgeStage.FINALIZING
        html.contains("gsap.") || html.contains("ScrollTrigger") -> ForgeStage.INJECTING_MOTION
        html.contains("<canvas") || html.contains("perspective(") ||
                html.contains("picsum") || html.contains("pravatar") -> ForgeStage.BUILDING_VISUALS
        countElements(html) >= 2 -> ForgeStage.STYLING
        html.contains("<section") || html.contains("<body") -> ForgeStage.LAYING_OUT
        else -> ForgeStage.READING_IDEA
    }

    private fun countElements(html: String): Int {
        var count = 0
        if (html.contains("<header")) count++
        if (html.contains("<footer")) count++
        count += Regex("<section").findAll(html).count()
        return count
    }

    fun extractSections(html: String): List<String> {
        val result = mutableListOf<String>()
        Regex("""<section[^>]*id="([^"]+)"""").findAll(html).forEach { result.add(it.groupValues[1]) }
        Regex("""<section[^>]*class="([^"]+)"""").findAll(html).take(6).forEach {
            val cls = it.groupValues[1].split(" ").firstOrNull() ?: return@forEach
            if (!result.contains(cls)) result.add(cls)
        }
        return result.take(8)
    }
}

// ── Forge System Prompt ──────────────────────────────────────────────────────
object ForgeSystemPrompt {
    fun forNewSite(prompt: String): String = """
You are an expert web developer building a single self-contained HTML file.

REQUIREMENTS:
- Single HTML file with all CSS and JS inline (no external files except CDNs below)
- Tailwind CSS via CDN: <script src="https://cdn.tailwindcss.com"></script>
- GSAP + ScrollTrigger via CDN: <script src="https://cdnjs.cloudflare.com/ajax/libs/gsap/3.12.2/gsap.min.js"></script> and ScrollTrigger plugin
- Three.js via CDN for 3D: <script src="https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js"></script>
- Images ONLY from picsum.photos or pravatar.cc — never placeholder.com or lorempixel
- 5–6 sections: hero, features/about, gallery or bento grid, testimonials or stats, CTA, footer
- PHOTOS in AT MOST 2 sections (hero and one gallery). Every other section uses CSS 3D or Three.js
- Three.js scene in the hero (themed particle field or geometric object reacting to mouse) and CTA
- CSS 3D elsewhere: tilt cards, flip cards, rotating cubes, depth layers
- Photo sections MUST have dark gradient overlay for text readability
- Every Three.js canvas MUST fill its section and show a visible animated object
- Mobile-first, hamburger nav under 768px
- Respect prefers-reduced-motion
- Hero: magnetic buttons, parallax, flair
- NEVER hide content waiting for animation — do not use opacity:0 that requires ScrollTrigger to reveal

COLOUR THEME: deep indigo night + warm marigold accent #FFB547
FONT: Google Fonts (Instrument Serif for headings, Manrope for body)

OUTPUT: complete, valid HTML from <!DOCTYPE html> to </html>. No truncation. No markdown fences.
""".trimIndent()

    fun forEdit(currentHtml: String, change: String): String = """
You are editing an existing website. Apply this change: "$change"
Return the complete modified HTML. No markdown fences. Keep all existing features unless the change requires removing them.
Current HTML length: ${currentHtml.length} chars.
""".trimIndent()
}

// ── FileProvider ─────────────────────────────────────────────────────────────
class ForgeFileProvider : FileProvider()

// ── SSE Forge Client ─────────────────────────────────────────────────────────
class WebsiteForgeClient {
    suspend fun stream(
        apiKey: String,
        systemPrompt: String,
        userPrompt: String,
        onChunk: (String) -> Unit,
        onError: (String) -> Unit,
        onDone: (String) -> Unit
    ) {
        val model = com.Lia.assistant.voice.GeminiTextClient.resolveModel(apiKey)
        val useThinkingBudget = model.contains("2.5")

        val body = org.json.JSONObject().apply {
            put("systemInstruction", org.json.JSONObject()
                .put("parts", org.json.JSONArray().put(org.json.JSONObject().put("text", systemPrompt))))
            put("contents", org.json.JSONArray().put(org.json.JSONObject()
                .put("role", "user")
                .put("parts", org.json.JSONArray().put(org.json.JSONObject().put("text", userPrompt)))))
            if (useThinkingBudget) {
                put("generationConfig", org.json.JSONObject()
                    .put("thinkingConfig", org.json.JSONObject().put("thinkingBudget", 0)))
            }
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/$model:streamGenerateContent?alt=sse&key=$apiKey"
        val request = okhttp3.Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .header("x-goog-api-key", apiKey)
            .build()

        var finishReason = "STOP"
        try {
            com.Lia.assistant.voice.GeminiTextClient.httpClient
                .newBuilder()
                .readTimeout(180, java.util.concurrent.TimeUnit.SECONDS)
                .build()
                .newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        if (resp.code == 503) {
                            onError("Gemini is busy right now. Please try again in a moment.")
                            return
                        }
                        onError("Error ${resp.code}")
                        return
                    }
                    val source = resp.body?.source() ?: run { onError("Empty response"); return }
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (!line.startsWith("data:")) continue
                        val data = line.removePrefix("data:").trim()
                        if (data == "[DONE]") break
                        try {
                            val json = org.json.JSONObject(data)
                            if (json.has("error")) {
                                onError(json.getJSONObject("error").optString("message", "Unknown error"))
                                return
                            }
                            val candidates = json.optJSONArray("candidates") ?: continue
                            val candidate = candidates.getJSONObject(0)
                            val fr = candidate.optString("finishReason", "")
                            if (fr.isNotBlank() && fr != "STOP") finishReason = fr
                            val parts = candidate.optJSONObject("content")?.optJSONArray("parts") ?: continue
                            for (i in 0 until parts.length()) {
                                val text = parts.getJSONObject(i).optString("text", "")
                                if (text.isNotBlank()) onChunk(text)
                            }
                        } catch (_: Exception) {}
                    }
                    onDone(finishReason)
                }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            onError("Couldn't reach Gemini. Check your connection.")
        }
    }
}

private fun String.toMediaType() = okhttp3.MediaType.Companion.toMediaType(this)
private fun String.toRequestBody(mediaType: okhttp3.MediaType) =
    okhttp3.RequestBody.Companion.toRequestBody(this, mediaType)
