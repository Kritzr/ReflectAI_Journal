package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class RecapType {
    MONTHLY,
    YEARLY
}

data class RecapPeriod(
    val type: RecapType,
    val year: Int,
    val month: Int = 1 // 1-12 for monthly
) {
    val displayLabel: String
        get() = when (type) {
            RecapType.MONTHLY -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month - 1)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
            }
            RecapType.YEARLY -> "Year of $year"
        }

    fun matchesTimestamp(timestamp: Long): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val itemYear = cal.get(Calendar.YEAR)
        val itemMonth = cal.get(Calendar.MONTH) + 1
        return when (type) {
            RecapType.MONTHLY -> itemYear == year && itemMonth == month
            RecapType.YEARLY -> itemYear == year
        }
    }
}

data class CategoryStat(
    val category: String,
    val count: Int,
    val percentage: Float
)

data class RecapAnalysis(
    val period: RecapPeriod,
    val totalEntries: Int,
    val totalWords: Int,
    val categoryBreakdown: List<CategoryStat>,
    val topThemes: List<String>,
    val narrativeSynthesis: String,
    val keyTakeaways: List<String>,
    val mostActiveDay: String,
    val keyMoments: List<JournalInteraction>
) {
    companion object {
        fun buildInitialStats(period: RecapPeriod, entries: List<JournalInteraction>): RecapAnalysis {
            val matchingEntries = entries.filter { period.matchesTimestamp(it.timestamp) }
            val totalCount = matchingEntries.size
            val wordCount = matchingEntries.sumOf {
                it.prompt.split("\\s+".toRegex()).count { w -> w.isNotBlank() }
            }

            val categoryCounts = matchingEntries.groupingBy { it.category }.eachCount()
            val categoryStats = categoryCounts.map { (cat, count) ->
                CategoryStat(
                    category = cat,
                    count = count,
                    percentage = if (totalCount > 0) (count.toFloat() / totalCount) * 100f else 0f
                )
            }.sortedByDescending { it.count }

            val themes = mutableListOf<String>()
            if (categoryCounts.getOrDefault("Deep Reflection", 0) > 0) themes.add("Mindful Self-Awareness")
            if (categoryCounts.getOrDefault("Brainstorm Ideas", 0) > 0) themes.add("Creative Problem Solving")
            if (categoryCounts.getOrDefault("Daily Journal", 0) > 0) themes.add("Daily Grounding")
            if (categoryCounts.getOrDefault("Session Summary", 0) > 0) themes.add("Strategic Synthesis")
            if (themes.isEmpty()) themes.add("Personal Discovery")

            val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            val dayCounts = matchingEntries.groupingBy {
                dayFormat.format(Date(it.timestamp))
            }.eachCount()
            val mostActive = dayCounts.maxByOrNull { it.value }?.key ?: "Flexible Routine"

            return RecapAnalysis(
                period = period,
                totalEntries = totalCount,
                totalWords = wordCount,
                categoryBreakdown = categoryStats,
                topThemes = themes,
                narrativeSynthesis = "",
                keyTakeaways = emptyList(),
                mostActiveDay = mostActive,
                keyMoments = matchingEntries.take(5)
            )
        }
    }
}
