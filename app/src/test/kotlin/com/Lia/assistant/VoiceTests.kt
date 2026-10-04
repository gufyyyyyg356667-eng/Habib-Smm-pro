package com.Lia.assistant

import com.Lia.assistant.voice.*
import com.Lia.assistant.forge.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

// ── Language Detector ─────────────────────────────────────────────────────────
class LanguageDetectorTest {
    @Test fun devanagari_is_hindi() {
        assertEquals(DetectedLanguage.HINDI, LanguageDetector.detect("नमस्ते कैसे हो"))
    }

    @Test fun hinglish_markers_detected() {
        assertEquals(DetectedLanguage.HINGLISH, LanguageDetector.detect("kya haal hai yaar"))
    }

    @Test fun short_sentence_one_hit() {
        assertEquals(DetectedLanguage.HINGLISH, LanguageDetector.detect("kya ho"))
    }

    @Test fun english_sentence() {
        assertEquals(DetectedLanguage.ENGLISH, LanguageDetector.detect("what is the weather today"))
    }

    @Test fun blank_is_unknown() {
        assertEquals(DetectedLanguage.UNKNOWN, LanguageDetector.detect(""))
    }

    @Test fun spaces_only_is_unknown() {
        assertEquals(DetectedLanguage.UNKNOWN, LanguageDetector.detect("   "))
    }

    @Test fun mixed_ratio_hinglish() {
        // 3 hinglish words out of 10 = 30% > 15% threshold
        assertEquals(DetectedLanguage.HINGLISH, LanguageDetector.detect("I want to go kya bhai theek hai today"))
    }
}

// ── ConversationMemory ────────────────────────────────────────────────────────
class ConversationMemoryTest {
    @Before fun setup() { ConversationMemory.clear() }

    @Test fun recap_null_when_empty() {
        assertNull(ConversationMemory.recap())
    }

    @Test fun recap_contains_turns() {
        ConversationMemory.add(ConversationTurn.Role.USER, "hello")
        ConversationMemory.add(ConversationTurn.Role.ASSISTANT, "hi there")
        val recap = ConversationMemory.recap()
        assertNotNull(recap)
        assertTrue(recap!!.contains("hello"))
        assertTrue(recap.contains("hi there"))
    }

    @Test fun max_12_turns_rolling() {
        repeat(15) { i -> ConversationMemory.add(ConversationTurn.Role.USER, "msg$i") }
        assertEquals(12, ConversationMemory.size())
    }

    @Test fun recap_capped_at_1600_chars() {
        repeat(12) { ConversationMemory.add(ConversationTurn.Role.USER, "x".repeat(200)) }
        val recap = ConversationMemory.recap()
        assertNotNull(recap)
        assertTrue((recap?.length ?: 0) <= 1600)
    }

    @Test fun clear_empties_memory() {
        ConversationMemory.add(ConversationTurn.Role.USER, "test")
        ConversationMemory.clear()
        assertEquals(0, ConversationMemory.size())
        assertNull(ConversationMemory.recap())
    }
}

// ── ToolCallDeduplicator ──────────────────────────────────────────────────────
class ToolCallDeduplicatorTest {
    @Test fun first_call_not_seen() {
        val dedup = ToolCallDeduplicator()
        assertFalse(dedup.seen("id-001"))
    }

    @Test fun second_call_same_id_is_seen() {
        val dedup = ToolCallDeduplicator()
        dedup.seen("id-001")
        assertTrue(dedup.seen("id-001"))
    }

    @Test fun different_ids_not_seen() {
        val dedup = ToolCallDeduplicator()
        dedup.seen("id-001")
        assertFalse(dedup.seen("id-002"))
    }

    @Test fun clear_resets() {
        val dedup = ToolCallDeduplicator()
        dedup.seen("id-001")
        dedup.clear()
        assertFalse(dedup.seen("id-001"))
    }
}

// ── VoiceLatencyTracker ───────────────────────────────────────────────────────
class VoiceLatencyTrackerTest {
    @Before fun setup() { VoiceLatencyTracker.newTurn() }

    @Test fun speech_end_to_first_audio_correct() {
        VoiceLatencyTracker.markSpeechEnd()
        Thread.sleep(50)
        VoiceLatencyTracker.markFirstAudioChunk()
        val delta = VoiceLatencyTracker.current.value.speechEndToFirstAudio
        assertTrue("Expected > 0 but was $delta", delta >= 40)
    }

    @Test fun out_of_order_chunk_mark_ignored() {
        VoiceLatencyTracker.markFirstAudioChunk()
        VoiceLatencyTracker.markFirstAudioChunk() // second call — should be ignored
        val ts = VoiceLatencyTracker.current.value.firstAudioChunk
        assertTrue(ts > 0)
    }

    @Test fun new_turn_resets_deltas() {
        VoiceLatencyTracker.markSpeechEnd()
        VoiceLatencyTracker.markFirstAudioChunk()
        VoiceLatencyTracker.newTurn()
        assertEquals(-1L, VoiceLatencyTracker.current.value.speechEndToFirstAudio)
    }
}

// ── ForgeHtml ─────────────────────────────────────────────────────────────────
class ForgeHtmlTest {
    @Test fun strip_fences() {
        val input = "```html\n<html><body>hello</body></html>\n```"
        val result = ForgeHtml.stripFences(input)
        assertFalse(result.contains("```"))
        assertTrue(result.contains("<html>"))
    }

    @Test fun looks_like_html_true() {
        assertTrue(ForgeHtml.looksLikeHtml("<html><body></body></html>"))
        assertTrue(ForgeHtml.looksLikeHtml("<!DOCTYPE html>"))
        assertTrue(ForgeHtml.looksLikeHtml("<body>test</body>"))
    }

    @Test fun looks_like_html_false() {
        assertFalse(ForgeHtml.looksLikeHtml("just some text"))
        assertFalse(ForgeHtml.looksLikeHtml(""))
    }

    @Test fun repair_adds_closing_tags() {
        val partial = "<html><body><script>var x=1;"
        val repaired = ForgeHtml.repairTruncated(partial)
        assertTrue(repaired.contains("</script>"))
        assertTrue(repaired.contains("</body>"))
        assertTrue(repaired.contains("</html>"))
    }

    @Test fun safety_net_is_idempotent() {
        val base = "<html><body><div style='opacity:0'>content</div></body></html>"
        val first = ForgeHtml.withSafetyNet(base)
        val second = ForgeHtml.withSafetyNet(first)
        // Script only added once — check the safety net marker is present once
        assertEquals(1, first.split("data-lia-safety-net").size - 1)
    }
}

// ── ForgeStageDetector ────────────────────────────────────────────────────────
class ForgeStageDetectorTest {
    @Test fun empty_html_is_reading_idea() {
        assertEquals(ForgeStage.READING_IDEA, ForgeStageDetector.detect(""))
    }

    @Test fun head_only_stays_at_start() {
        assertEquals(ForgeStage.READING_IDEA, ForgeStageDetector.detect("<html><head><title>Test</title></head>"))
    }

    @Test fun body_tag_advances_to_laying_out() {
        assertEquals(ForgeStage.LAYING_OUT, ForgeStageDetector.detect("<html><body><section>"))
    }

    @Test fun multiple_sections_advances_to_styling() {
        val html = "<html><body><header></header><section></section><footer></footer>"
        assertEquals(ForgeStage.STYLING, ForgeStageDetector.detect(html))
    }

    @Test fun canvas_advances_to_visuals() {
        val html = "<html><body><header></header><section></section><footer></footer><canvas id='hero'></canvas>"
        assertEquals(ForgeStage.BUILDING_VISUALS, ForgeStageDetector.detect(html))
    }

    @Test fun gsap_advances_to_motion() {
        val html = "<html><body><header></header><section></section><footer></footer><canvas></canvas><script>gsap.to('.el',{})</script>"
        assertEquals(ForgeStage.INJECTING_MOTION, ForgeStageDetector.detect(html))
    }

    @Test fun closing_html_is_finalizing() {
        assertEquals(ForgeStage.FINALIZING, ForgeStageDetector.detect("<html><body></body></html>"))
    }
}

// ── ForgeIntent ───────────────────────────────────────────────────────────────
class ForgeIntentTest {
    @Test fun matches_website_plus_verb() {
        assertNotNull(ForgeIntent.extract("ek cafe ki website banao"))
        assertNotNull(ForgeIntent.extract("make me a landing page for my business"))
        assertNotNull(ForgeIntent.extract("build a website for my portfolio"))
    }

    @Test fun ignores_question_about_website() {
        assertNull(ForgeIntent.extract("website kya hota hai"))
        assertNull(ForgeIntent.extract("what is a landing page"))
    }

    @Test fun ignores_missing_verb() {
        assertNull(ForgeIntent.extract("website"))
    }

    @Test fun ignores_missing_subject() {
        assertNull(ForgeIntent.extract("build something nice"))
    }
}

// ── PersonalityPromptBuilder ──────────────────────────────────────────────────
class PromptBuilderTest {
    @Test fun builds_without_recap() {
        val personalities = com.Lia.assistant.data.Personality.values()
        val languages = com.Lia.assistant.data.LanguagePreference.values()
        for (p in personalities) {
            for (l in languages) {
                val prompt = PersonalityPromptBuilder.build(p, l, null, null, "Lia")
                assertFalse("Prompt empty for $p/$l", prompt.isBlank())
                assertFalse("Prompt should not contain recap", prompt.contains("do not greet again"))
            }
        }
    }

    @Test fun recap_section_present_when_given() {
        val prompt = PersonalityPromptBuilder.build(
            com.Lia.assistant.data.Personality.NORMAL,
            com.Lia.assistant.data.LanguagePreference.AUTO,
            null,
            "User: hello\nYou: hi",
            "Lia"
        )
        assertTrue(prompt.contains("do not greet again"))
        assertTrue(prompt.contains("hello"))
    }

    @Test fun all_40_personality_language_combinations_compile() {
        val personalities = com.Lia.assistant.data.Personality.values()
        val languages = com.Lia.assistant.data.LanguagePreference.values()
        assertEquals(40, personalities.size * languages.size)
    }
}
