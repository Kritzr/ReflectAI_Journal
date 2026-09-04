package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Habit tracks to guide users in building and keeping a daily journaling ritual.
 */
enum class PromptHabitTrack(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconEmoji: String
) {
    ALL(
        id = "all",
        title = "All Tracks",
        subtitle = "Full collection of habit prompts",
        iconEmoji = "🌟"
    ),
    KICKSTARTER(
        id = "kickstarter",
        title = "2-Min Starters",
        subtitle = "Low friction, 1-2 sentence habit builders",
        iconEmoji = "🌱"
    ),
    MORNING(
        id = "morning",
        title = "Morning Intentions",
        subtitle = "Calm clarity to ground your day",
        iconEmoji = "☀️"
    ),
    EVENING(
        id = "evening",
        title = "Evening Wind-Down",
        subtitle = "Release stress & celebrate quiet wins",
        iconEmoji = "🌙"
    ),
    RESISTANCE(
        id = "resistance",
        title = "Writer's Block",
        subtitle = "When you don't know what to write",
        iconEmoji = "🧭"
    ),
    GRATITUDE(
        id = "gratitude",
        title = "Gratitude & Calm",
        subtitle = "Anchor joy in the everyday present",
        iconEmoji = "🌸"
    ),
    EMOTIONAL(
        id = "emotional",
        title = "Heart Check-In",
        subtitle = "Honest emotional check-ins without judgment",
        iconEmoji = "💭"
    ),
    GROWTH(
        id = "growth",
        title = "Deep Self-Discovery",
        subtitle = "Values, courage, and inner exploration",
        iconEmoji = "✨"
    )
}

/**
 * A structured prompt designed to spark reflective journaling and habit formation.
 */
data class JournalPromptItem(
    val id: String,
    val track: PromptHabitTrack,
    val title: String,
    val promptText: String,
    val habitTip: String,
    val suggestedCategory: ReflectionCategory,
    val estimatedTime: String = "2 mins",
    val iconEmoji: String
)

object HabitJournalPromptsRepository {

    val habitGuidanceTips = listOf(
        "Consistency over perfection: Writing just one honest sentence keeps the neural habit alive.",
        "Sanctuary rule: Your journal is never graded. Spill unedited, messy thoughts freely.",
        "Anchor your ritual: Pair writing with an existing anchor, like your morning tea or bedside routine.",
        "When stuck, simply write: 'Right now I feel...' until the authentic voice takes over.",
        "Notice without judging: You don't have to fix every feeling today, merely witness it."
    )

    val prompts: List<JournalPromptItem> = listOf(
        // 🌱 2-Min Habit Starters
        JournalPromptItem(
            id = "starter_1",
            track = PromptHabitTrack.KICKSTARTER,
            title = "Three Words Today",
            promptText = "If I had to describe my current headspace in three single words right now, they would be:\n1.\n2.\n3.\nBecause:",
            habitTip = "Micro-entries lower the barrier to entry and cement the daily habit.",
            suggestedCategory = ReflectionCategory.DAILY_JOURNAL,
            estimatedTime = "1 min",
            iconEmoji = "🌱"
        ),
        JournalPromptItem(
            id = "starter_2",
            track = PromptHabitTrack.KICKSTARTER,
            title = "Small Unnoticed Win",
            promptText = "One small thing I handled or endured today, even if nobody else noticed or praised it:",
            habitTip = "Acknowledging tiny wins wires your brain to look forward to daily reflection.",
            suggestedCategory = ReflectionCategory.REFLECTION,
            estimatedTime = "2 mins",
            iconEmoji = "🌿"
        ),
        JournalPromptItem(
            id = "starter_3",
            track = PromptHabitTrack.KICKSTARTER,
            title = "Next Hour Kindness",
            promptText = "The gentlest, most supportive thing I can do for myself in the next hour is:",
            habitTip = "Turn reflection into immediate compassionate self-care.",
            suggestedCategory = ReflectionCategory.DAILY_JOURNAL,
            estimatedTime = "1 min",
            iconEmoji = "🕊️"
        ),
        JournalPromptItem(
            id = "starter_4",
            track = PromptHabitTrack.KICKSTARTER,
            title = "Unload The Heavy Thought",
            promptText = "Something buzzing or weighing on my mind that I want to leave right here on this page:",
            habitTip = "Externalizing a worry on paper frees working memory for rest.",
            suggestedCategory = ReflectionCategory.REFLECTION,
            estimatedTime = "2 mins",
            iconEmoji = "🍃"
        ),

        // ☀️ Morning Intentions
        JournalPromptItem(
            id = "morning_1",
            track = PromptHabitTrack.MORNING,
            title = "Energy of the Day",
            promptText = "What kind of energy do I want to embody today, regardless of external circumstances? How can I protect this state?",
            habitTip = "Deciding your emotional posture early sets a calm proactive tone.",
            suggestedCategory = ReflectionCategory.REFLECTION,
            estimatedTime = "3 mins",
            iconEmoji = "☀️"
        ),
        JournalPromptItem(
            id = "morning_2",
            track = PromptHabitTrack.MORNING,
            title = "The One True Priority",
            promptText = "If only one meaningful thing gets accomplished or felt today, what should it be? Everything else is secondary.",
            habitTip = "Reduces overwhelm from endless to-do lists to single-point clarity.",
            suggestedCategory = ReflectionCategory.BRAINSTORMING,
            estimatedTime = "2 mins",
            iconEmoji = "🎯"
        ),
        JournalPromptItem(
            id = "morning_3",
            track = PromptHabitTrack.MORNING,
            title = "Anticipated Joy",
            promptText = "What is one small, simple pleasure I am looking forward to today (a warm sip, a song, a breath of air)?",
            habitTip = "Priming your attention for small daily pleasures elevates baseline mood.",
            suggestedCategory = ReflectionCategory.DAILY_JOURNAL,
            estimatedTime = "2 mins",
            iconEmoji = "☕"
        ),

        // 🌙 Evening Wind-Down
        JournalPromptItem(
            id = "evening_1",
            track = PromptHabitTrack.EVENING,
            title = "Gentle Self-Forgiveness",
            promptText = "What can I forgive myself for today, knowing that I did the best I could with the energy and tools I had?",
            habitTip = "Nightly forgiveness stops overthinking before sleep.",
            suggestedCategory = ReflectionCategory.REFLECTION,
            estimatedTime = "3 mins",
            iconEmoji = "🌙"
        ),
        JournalPromptItem(
            id = "evening_2",
            track = PromptHabitTrack.EVENING,
            title = "Closing The Open Loops",
            promptText = "What is one lingering thought or unfinished task I can consciously surrender until tomorrow?",
            habitTip = "Writing closure down acts as a psychological bookmark for the brain.",
            suggestedCategory = ReflectionCategory.DAILY_JOURNAL,
            estimatedTime = "2 mins",
            iconEmoji = "🕯️"
        ),
        JournalPromptItem(
            id = "evening_3",
            track = PromptHabitTrack.EVENING,
            title = "Unexpected Smile",
            promptText = "What brought an unexpected smile, soft breath, or moment of relief to my day today?",
            habitTip = "End the day anchored in peace rather than stress.",
            suggestedCategory = ReflectionCategory.DAILY_JOURNAL,
            estimatedTime = "2 mins",
            iconEmoji = "✨"
        ),

        // 🧭 Writer's Block & Resistance
        JournalPromptItem(
            id = "block_1",
            track = PromptHabitTrack.RESISTANCE,
            title = "Unfiltered Weather Report",
            promptText = "If my mind were an open meteorological report right now, what is the exact weather? (e.g. foggy mist, restless wind, quiet drizzle)",
            habitTip = "Metaphors bypass intellectual resistance and tap straight into emotion.",
            suggestedCategory = ReflectionCategory.REFLECTION,
            estimatedTime = "2 mins",
            iconEmoji = "🧭"
        ),
        JournalPromptItem(
            id = "block_2",
            track = PromptHabitTrack.RESISTANCE,
            title = "Honest Resistance",
            promptText = "Right now, I really don't feel like journaling or opening up because:\n(Be completely raw and unfiltered; there are no wrong answers)",
            habitTip = "Writing ABOUT your reluctance instantly dissolves it.",
            suggestedCategory = ReflectionCategory.DAILY_JOURNAL,
            estimatedTime = "2 mins",
            iconEmoji = "⚡"
        ),
        JournalPromptItem(
            id = "block_3",
            track = PromptHabitTrack.RESISTANCE,
            title = "The Secret Wish",
            promptText = "Complete this sentence 4 times without second-guessing:\n1. I secretly wish that...\n2. I secretly wish that...\n3. I secretly wish that...\n4. I secretly wish that...",
            habitTip = "Fast sentence stems bypass overthinking effortlessly.",
            suggestedCategory = ReflectionCategory.BRAINSTORMING,
            estimatedTime = "2 mins",
            iconEmoji = "🗝️"
        ),

        // 🌸 Gratitude & Calm
        JournalPromptItem(
            id = "gratitude_1",
            track = PromptHabitTrack.GRATITUDE,
            title = "Sensory Sanctuary",
            promptText = "Describe one physical comfort in your immediate environment right now that you appreciate (a soft blanket, quiet room, gentle light):",
            habitTip = "Grounding in physical senses brings nervous system regulation.",
            suggestedCategory = ReflectionCategory.REFLECTION,
            estimatedTime = "2 mins",
            iconEmoji = "🌸"
        ),
        JournalPromptItem(
            id = "gratitude_2",
            track = PromptHabitTrack.GRATITUDE,
            title = "Someone Who Made a Difference",
            promptText = "Think of someone whose presence or kindness made a positive difference in your journey. What do you appreciate about them?",
            habitTip = "Relational gratitude fosters a deep sense of connection and peace.",
            suggestedCategory = ReflectionCategory.REFLECTION,
            estimatedTime = "3 mins",
            iconEmoji = "💖"
        ),

        // 💭 Heart Check-In
        JournalPromptItem(
            id = "heart_1",
            track = PromptHabitTrack.EMOTIONAL,
            title = "What Does The Quiet Voice Say?",
            promptText = "Beneath the noise, deadlines, and duties, what is the quietest voice inside me trying to whisper right now?",
            habitTip = "Attuning to subtle inner cues prevents long-term burnout.",
            suggestedCategory = ReflectionCategory.REFLECTION,
            estimatedTime = "3 mins",
            iconEmoji = "💭"
        ),
        JournalPromptItem(
            id = "heart_2",
            track = PromptHabitTrack.EMOTIONAL,
            title = "Emotional Body Scan",
            promptText = "Where in my body am I holding tension or emotion right now? What happens if I breathe gently into that exact spot?",
            habitTip = "Somatic journaling bridges mind and body awareness.",
            suggestedCategory = ReflectionCategory.REFLECTION,
            estimatedTime = "3 mins",
            iconEmoji = "🧘"
        ),

        // ✨ Deep Self-Discovery
        JournalPromptItem(
            id = "growth_1",
            track = PromptHabitTrack.GROWTH,
            title = "Releasing The Old Script",
            promptText = "What is an old assumption or belief about myself ('I am not good at...', 'I always...') that I am ready to gently outgrow?",
            habitTip = "Rewriting self-limiting scripts creates space for renewal.",
            suggestedCategory = ReflectionCategory.REFLECTION,
            estimatedTime = "4 mins",
            iconEmoji = "✨"
        ),
        JournalPromptItem(
            id = "growth_2",
            track = PromptHabitTrack.GROWTH,
            title = "A Courageous Step",
            promptText = "If I had 10% more courage and felt completely safe to be myself this week, what would I say or do?",
            habitTip = "Small increments of courage feel manageable and actionable.",
            suggestedCategory = ReflectionCategory.BRAINSTORMING,
            estimatedTime = "3 mins",
            iconEmoji = "🚀"
        )
    )

    /**
     * Calculates the user's current streak in consecutive days and whether they completed an entry today.
     */
    fun calculateHabitStreak(timestamps: List<Long>): HabitStreakInfo {
        if (timestamps.isEmpty()) {
            return HabitStreakInfo(
                streakDays = 0,
                hasEntryToday = false,
                totalEntries = 0,
                statusText = "Ready to start your daily reflection habit"
            )
        }

        val sortedDays = timestamps
            .map { toDayIdentifier(it) }
            .distinct()
            .sortedDescending()

        val todayId = toDayIdentifier(System.currentTimeMillis())
        val hasEntryToday = sortedDays.contains(todayId)

        var currentStreak = 0
        val cal = Calendar.getInstance()
        if (!hasEntryToday) {
            // Check if streak was maintained yesterday
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayId = toDayIdentifier(cal.timeInMillis)
            if (!sortedDays.contains(yesterdayId)) {
                return HabitStreakInfo(
                    streakDays = 0,
                    hasEntryToday = false,
                    totalEntries = timestamps.size,
                    statusText = "Take 2 minutes to reflect and begin your streak today"
                )
            }
        }

        // Count consecutive days backward
        val checkCal = Calendar.getInstance()
        if (!hasEntryToday) {
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        }

        val maxDaysToCheck = minOf(sortedDays.size + 1, 365)
        for (i in 0 until maxDaysToCheck) {
            val dayStr = toDayIdentifier(checkCal.timeInMillis)
            if (sortedDays.contains(dayStr)) {
                currentStreak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        val status = if (hasEntryToday) {
            "Today's reflection completed ✨ • $currentStreak day streak!"
        } else {
            "Keep your $currentStreak day streak alive with today's reflection"
        }

        return HabitStreakInfo(
            streakDays = currentStreak,
            hasEntryToday = hasEntryToday,
            totalEntries = timestamps.size,
            statusText = status
        )
    }

    private fun toDayIdentifier(timeMillis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(timeMillis))
    }
}

data class HabitStreakInfo(
    val streakDays: Int,
    val hasEntryToday: Boolean,
    val totalEntries: Int,
    val statusText: String
)
