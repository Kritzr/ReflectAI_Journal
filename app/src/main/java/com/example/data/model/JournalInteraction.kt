package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "journal_interactions")
data class JournalInteraction(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val title: String = "",
    val prompt: String = "",
    val response: String = "",
    val summary: String = "",
    val category: String = "Reflection", // "Reflection", "Brainstorming", "Summary", "Daily Journal"
    val tags: List<String> = emptyList(),
    val modelUsed: String = "gemini-2.5-flash",
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = true,
    val imageUri: String? = null,
    val audioUri: String? = null,
    val mediaType: String? = null,
    val transcription: String? = null
) {
    /**
     * Sanitizes the interaction into a clean, null-safe Map for Cloud Firestore
     * to eliminate any undefined/null driver crash risks.
     */
    fun toFirestoreMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        map["id"] = id.ifBlank { UUID.randomUUID().toString() }
        map["userId"] = userId.ifBlank { "anonymous_user" }
        map["title"] = title.ifBlank { "Untitled Reflection" }
        map["prompt"] = prompt.trim()
        map["response"] = response.trim()
        map["summary"] = summary.trim()
        map["category"] = category.ifBlank { "Reflection" }
        map["tags"] = tags.filter { it.isNotBlank() }
        map["modelUsed"] = modelUsed.ifBlank { "gemini-2.5-flash" }
        map["timestamp"] = if (timestamp > 0) timestamp else System.currentTimeMillis()
        map["isSynced"] = true
        imageUri?.let { map["imageUri"] = it }
        audioUri?.let { map["audioUri"] = it }
        mediaType?.let { map["mediaType"] = it }
        transcription?.let { map["transcription"] = it }
        return map
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromFirestoreMap(id: String, map: Map<String, Any?>): JournalInteraction {
            return JournalInteraction(
                id = (map["id"] as? String) ?: id,
                userId = (map["userId"] as? String) ?: "",
                title = (map["title"] as? String) ?: "Untitled Reflection",
                prompt = (map["prompt"] as? String) ?: "",
                response = (map["response"] as? String) ?: "",
                summary = (map["summary"] as? String) ?: "",
                category = (map["category"] as? String) ?: "Reflection",
                tags = (map["tags"] as? List<String>) ?: emptyList(),
                modelUsed = (map["modelUsed"] as? String) ?: "gemini-2.5-flash",
                timestamp = (map["timestamp"] as? Long) ?: System.currentTimeMillis(),
                isSynced = true,
                imageUri = map["imageUri"] as? String,
                audioUri = map["audioUri"] as? String,
                mediaType = map["mediaType"] as? String,
                transcription = map["transcription"] as? String
            )
        }
    }
}

enum class ReflectionCategory(val label: String, val promptPrefix: String) {
    REFLECTION(
        "Deep Reflection",
        "Provide a thoughtful, empathetic, and constructive psychological and philosophical reflection on this journal entry:"
    ),
    BRAINSTORMING(
        "Brainstorm Ideas",
        "Brainstorm 5 innovative, actionable, and creative avenues or next steps inspired by this reflection:"
    ),
    SUMMARY(
        "Session Summary",
        "Synthesize this journal entry into key takeaways, emotional tone analysis, and action items:"
    ),
    DAILY_JOURNAL(
        "Daily Journal",
        "Respond as an insightful, caring journaling companion to this personal thought:"
    )
}
