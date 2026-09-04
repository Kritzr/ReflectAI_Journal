package com.example

import com.example.data.model.HabitJournalPromptsRepository
import com.example.data.model.JournalInteraction
import com.example.data.model.PromptHabitTrack
import com.example.data.model.ReflectionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class JournalDataModelTest {

    @Test
    fun testFirestoreMapSanitization() {
        val interaction = JournalInteraction(
            id = "test-id-123",
            userId = "user_456",
            title = "Overcoming Doubt",
            prompt = "   I felt anxious before my presentation today.   ",
            response = "   Acknowledge the feeling as natural energy.   ",
            summary = "Reframe anxiety into readiness",
            category = "Deep Reflection",
            tags = listOf("anxiety", "growth"),
            modelUsed = "gemini-2.5-flash",
            timestamp = 1700000000000L
        )

        val map = interaction.toFirestoreMap()

        assertEquals("test-id-123", map["id"])
        assertEquals("user_456", map["userId"])
        assertEquals("Overcoming Doubt", map["title"])
        assertEquals("I felt anxious before my presentation today.", map["prompt"])
        assertEquals("Acknowledge the feeling as natural energy.", map["response"])
        assertEquals("Reframe anxiety into readiness", map["summary"])
        assertEquals("Deep Reflection", map["category"])
        assertEquals("gemini-2.5-flash", map["modelUsed"])
        assertEquals(1700000000000L, map["timestamp"])
        assertEquals(true, map["isSynced"])
    }

    @Test
    fun testFirestoreDeserializationWithMissingKeys() {
        val partialMap = mapOf<String, Any?>(
            "prompt" to "Simple note",
            "response" to "Reflected thought"
        )

        val item = JournalInteraction.fromFirestoreMap("generated-id", partialMap)

        assertEquals("generated-id", item.id)
        assertEquals("Simple note", item.prompt)
        assertEquals("Reflected thought", item.response)
        assertEquals("Untitled Reflection", item.title)
        assertEquals("Reflection", item.category)
        assertNotNull(item.timestamp)
        assertTrue(item.isSynced)
    }

    @Test
    fun testReflectionCategoryEnums() {
        assertEquals("Deep Reflection", ReflectionCategory.REFLECTION.label)
        assertEquals("Brainstorm Ideas", ReflectionCategory.BRAINSTORMING.label)
        assertEquals("Session Summary", ReflectionCategory.SUMMARY.label)
        assertEquals("Daily Journal", ReflectionCategory.DAILY_JOURNAL.label)
    }

    @Test
    fun testHabitPromptsRepositoryTracksAndItems() {
        val prompts = HabitJournalPromptsRepository.prompts
        assertTrue("Prompt library should contain multiple curated habit prompts", prompts.size >= 10)

        for (prompt in prompts) {
            assertTrue(prompt.id.isNotBlank())
            assertTrue(prompt.title.isNotBlank())
            assertTrue(prompt.promptText.isNotBlank())
            assertTrue(prompt.habitTip.isNotBlank())
            assertTrue(prompt.estimatedTime.isNotBlank())
            assertNotNull(prompt.suggestedCategory)
            assertNotNull(prompt.track)
        }

        val starters = prompts.filter { it.track == PromptHabitTrack.KICKSTARTER }
        assertTrue("Should have 2-min starter habit prompts", starters.isNotEmpty())

        val morning = prompts.filter { it.track == PromptHabitTrack.MORNING }
        assertTrue("Should have morning intention prompts", morning.isNotEmpty())

        val evening = prompts.filter { it.track == PromptHabitTrack.EVENING }
        assertTrue("Should have evening wind-down prompts", evening.isNotEmpty())

        val resistance = prompts.filter { it.track == PromptHabitTrack.RESISTANCE }
        assertTrue("Should have writer's block resistance prompts", resistance.isNotEmpty())
    }

    @Test
    fun testHabitStreakCalculation() {
        // Empty history
        val emptyResult = HabitJournalPromptsRepository.calculateHabitStreak(emptyList())
        assertEquals(0, emptyResult.streakDays)
        assertEquals(false, emptyResult.hasEntryToday)

        // Today only
        val now = System.currentTimeMillis()
        val todayResult = HabitJournalPromptsRepository.calculateHabitStreak(listOf(now))
        assertEquals(1, todayResult.streakDays)
        assertEquals(true, todayResult.hasEntryToday)

        // 3 consecutive days
        val cal = Calendar.getInstance()
        val day0 = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val day1 = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val day2 = cal.timeInMillis

        val streak3 = HabitJournalPromptsRepository.calculateHabitStreak(listOf(day0, day1, day2))
        assertEquals(3, streak3.streakDays)
        assertEquals(true, streak3.hasEntryToday)
        assertEquals(3, streak3.totalEntries)
    }
}
