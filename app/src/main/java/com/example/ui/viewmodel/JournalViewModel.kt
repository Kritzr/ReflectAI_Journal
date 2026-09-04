package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AppUser
import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthState
import com.example.data.model.AiListenerPersona
import com.example.data.model.HabitJournalPromptsRepository
import com.example.data.model.JournalInteraction
import com.example.data.model.PromptHabitTrack
import com.example.data.model.RecapPeriod
import com.example.data.model.ReflectionCategory
import com.example.data.remote.firestore.FirestoreRepository
import com.example.data.remote.firestore.PersistenceResult
import com.example.data.remote.gemini.ConversationTurn
import com.example.data.remote.gemini.GeminiRepository
import com.example.util.AttachedMediaItem
import com.example.util.AudioRecordingManager
import com.example.util.MediaAttachmentHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class DashboardUiState(
    val promptInput: String = "",
    val selectedCategory: ReflectionCategory = ReflectionCategory.REFLECTION,
    val selectedPersona: AiListenerPersona = AiListenerPersona.MAYA,
    val isGenerating: Boolean = false,
    val activeInteraction: JournalInteraction? = null,
    val historyInteractions: List<JournalInteraction> = emptyList(),
    val searchQuery: String = "",
    val filterCategory: String = "All",
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val canRetry: Boolean = false,
    val lastFailedPrompt: String? = null,
    val activeModelBadge: String = "gemini-2.5-flash",
    val isGeneratingRecap: Boolean = false,
    val recapNarratives: Map<String, String> = emptyMap(),
    val attachedMedia: List<AttachedMediaItem> = emptyList(),
    val isRecordingAudio: Boolean = false,
    val recordingDurationMillis: Long = 0L,
    val recordingAmplitude: Float = 0f,
    val isTranscribingMedia: Boolean = false,
    val isGeneratingHabitPrompt: Boolean = false,
    val lastCustomPrompt: String? = null,
    val conversationThread: List<ConversationTurn> = emptyList()
)

class JournalViewModel @JvmOverloads constructor(
    application: Application,
    private val authRepository: AuthRepository = AuthRepository(application),
    private val geminiRepository: GeminiRepository = GeminiRepository(),
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val authState: StateFlow<AuthState> = authRepository.authState

    private val audioRecordingManager by lazy { AudioRecordingManager(application) }
    private var historyObservationJob: Job? = null

    init {
        viewModelScope.launch {
            authState.collectLatest { state ->
                when (state) {
                    is AuthState.Authenticated -> {
                        observeUserHistory(state.user.uid)
                    }
                    else -> {
                        historyObservationJob?.cancel()
                        _uiState.update { it.copy(historyInteractions = emptyList(), activeInteraction = null) }
                    }
                }
            }
        }
    }

    private fun observeUserHistory(userId: String) {
        historyObservationJob?.cancel()
        historyObservationJob = viewModelScope.launch {
            firestoreRepository.observeUserInteractions(userId).collectLatest { list ->
                _uiState.update { it.copy(historyInteractions = list) }
            }
        }
    }

    fun updatePromptInput(input: String) {
        _uiState.update { it.copy(promptInput = input, errorMessage = null) }
    }

    fun selectCategory(category: ReflectionCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun selectPersona(persona: AiListenerPersona) {
        _uiState.update { it.copy(selectedPersona = persona) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setFilterCategory(category: String) {
        _uiState.update { it.copy(filterCategory = category) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, canRetry = false) }
    }

    fun startNewSession() {
        _uiState.update {
            it.copy(
                promptInput = "",
                activeInteraction = null,
                errorMessage = null,
                statusMessage = null,
                attachedMedia = emptyList()
            )
        }
    }

    fun selectHistoryEntry(entry: JournalInteraction) {
        _uiState.update {
            it.copy(
                activeInteraction = entry,
                promptInput = "",
                selectedCategory = ReflectionCategory.entries.find { cat -> cat.label == entry.category }
                    ?: ReflectionCategory.REFLECTION
            )
        }
    }

    /**
     * Applies a curated or AI-generated journal prompt to the user's reflection prompt input
     * and sets the optimal reflection category to eliminate blank page friction.
     */
    fun applyPrompt(promptText: String, category: ReflectionCategory? = null) {
        _uiState.update { state ->
            state.copy(
                promptInput = promptText,
                selectedCategory = category ?: state.selectedCategory,
                activeInteraction = null, // Focus on crafting a new reflection
                statusMessage = "Prompt ready in reflection sanctuary"
            )
        }
    }

    /**
     * Generates a fresh AI habit-building prompt tailored to the requested track and current persona.
     */
    fun generateAiHabitPrompt(track: PromptHabitTrack = PromptHabitTrack.ALL) {
        val persona = _uiState.value.selectedPersona
        _uiState.update { it.copy(isGeneratingHabitPrompt = true, statusMessage = "Crafting inspiring habit prompt with ${persona.displayName}...") }

        viewModelScope.launch {
            try {
                val result = geminiRepository.generateHabitPrompt(track.title, persona)
                result.fold(
                    onSuccess = { generatedPrompt ->
                        _uiState.update {
                            it.copy(
                                isGeneratingHabitPrompt = false,
                                lastCustomPrompt = generatedPrompt,
                                promptInput = generatedPrompt,
                                statusMessage = "Generated habit prompt with ${persona.displayName}!"
                            )
                        }
                    },
                    onFailure = {
                        val fallback = HabitJournalPromptsRepository.prompts.shuffled().firstOrNull()?.promptText
                            ?: "What is one kind thought you can give yourself today?"
                        _uiState.update {
                            it.copy(
                                isGeneratingHabitPrompt = false,
                                lastCustomPrompt = fallback,
                                promptInput = fallback,
                                statusMessage = "Loaded habit prompt for your sanctuary"
                            )
                        }
                    }
                )
            } catch (t: Throwable) {
                Log.e("JournalViewModel", "Error generating habit prompt", t)
                val fallback = HabitJournalPromptsRepository.prompts.shuffled().firstOrNull()?.promptText
                    ?: "What is one kind thought you can give yourself today?"
                _uiState.update {
                    it.copy(
                        isGeneratingHabitPrompt = false,
                        lastCustomPrompt = fallback,
                        promptInput = fallback,
                        statusMessage = "Loaded habit prompt for your sanctuary"
                    )
                }
            }
        }
    }

    // --- Media & Multimodal Ingestion ---

    fun attachImage(uri: Uri) {
        viewModelScope.launch {
            val item = MediaAttachmentHelper.processImageUri(getApplication(), uri)
            if (item != null) {
                _uiState.update { it.copy(attachedMedia = it.attachedMedia + item, errorMessage = null) }
            } else {
                _uiState.update { it.copy(errorMessage = "Could not load image. Please try another file.") }
            }
        }
    }

    fun attachAudioUri(uri: Uri) {
        viewModelScope.launch {
            val item = MediaAttachmentHelper.processAudioUri(getApplication(), uri)
            if (item != null) {
                _uiState.update { it.copy(attachedMedia = it.attachedMedia + item, errorMessage = null) }
            } else {
                _uiState.update { it.copy(errorMessage = "Could not load audio file.") }
            }
        }
    }

    fun removeAttachedMedia(mediaId: String) {
        _uiState.update { state ->
            state.copy(attachedMedia = state.attachedMedia.filter { it.id != mediaId })
        }
    }

    // --- In-App Voice Recording ---

    fun startVoiceRecording(): Boolean {
        val success = audioRecordingManager.startRecording { amp, elapsedMillis ->
            _uiState.update {
                it.copy(
                    recordingAmplitude = amp,
                    recordingDurationMillis = elapsedMillis
                )
            }
        }
        if (success) {
            _uiState.update {
                it.copy(
                    isRecordingAudio = true,
                    recordingDurationMillis = 0L,
                    recordingAmplitude = 0f,
                    errorMessage = null
                )
            }
        } else {
            _uiState.update { it.copy(errorMessage = "Could not start audio recorder. Please check microphone permission.") }
        }
        return success
    }

    fun stopVoiceRecording(autoTranscribe: Boolean = true) {
        val file = audioRecordingManager.stopRecording()
        val durationSecs = (_uiState.value.recordingDurationMillis / 1000).toInt().coerceAtLeast(1)
        _uiState.update {
            it.copy(
                isRecordingAudio = false,
                recordingDurationMillis = 0L,
                recordingAmplitude = 0f
            )
        }

        if (file != null) {
            val item = MediaAttachmentHelper.processAudioFile(file, durationSecs)
            if (item != null) {
                _uiState.update { it.copy(attachedMedia = it.attachedMedia + item) }
                if (autoTranscribe) {
                    transcribeMediaItem(item)
                }
            }
        }
    }

    fun cancelVoiceRecording() {
        audioRecordingManager.cancelRecording()
        _uiState.update {
            it.copy(
                isRecordingAudio = false,
                recordingDurationMillis = 0L,
                recordingAmplitude = 0f
            )
        }
    }

    // --- Transcription Feature ---

    fun transcribeMediaItem(mediaItem: AttachedMediaItem) {
        val persona = _uiState.value.selectedPersona
        _uiState.update { state ->
            val updatedList = state.attachedMedia.map {
                if (it.id == mediaItem.id) it.copy(isTranscribing = true) else it
            }
            state.copy(
                attachedMedia = updatedList,
                isTranscribingMedia = true,
                statusMessage = "Transcribing ${if (mediaItem.isAudio) "voice note" else "image"} with Gemini..."
            )
        }

        viewModelScope.launch {
            val result = geminiRepository.transcribeMedia(
                mediaData = mediaItem.toGeminiInlineData(),
                isAudio = mediaItem.isAudio,
                persona = persona
            )

            result.fold(
                onSuccess = { transcriptionText ->
                    _uiState.update { state ->
                        val updatedList = state.attachedMedia.map {
                            if (it.id == mediaItem.id) it.copy(isTranscribing = false, transcription = transcriptionText) else it
                        }
                        // Prepend or append transcription to the prompt input buffer
                        val currentPrompt = state.promptInput.trim()
                        val newPrompt = if (currentPrompt.isBlank()) {
                            transcriptionText
                        } else {
                            "$currentPrompt\n\n[Transcribed ${if (mediaItem.isAudio) "Voice Note" else "Image"}]:\n$transcriptionText"
                        }
                        state.copy(
                            attachedMedia = updatedList,
                            isTranscribingMedia = false,
                            promptInput = newPrompt,
                            statusMessage = "Transcribed into reflection prompt!"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        val updatedList = state.attachedMedia.map {
                            if (it.id == mediaItem.id) it.copy(isTranscribing = false) else it
                        }
                        state.copy(
                            attachedMedia = updatedList,
                            isTranscribingMedia = false,
                            errorMessage = "Transcription failed: ${error.localizedMessage ?: "Unknown error"}"
                        )
                    }
                }
            )
        }
    }

    /**
     * Submits a journal entry to the Gemini API using the Resilient Fallback Ladder
     * and guarantees persistence to Cloud Firestore under /users/{userId}/interactions/{interactionId}.
     */
    fun submitJournalEntry() {
        val mediaList = _uiState.value.attachedMedia
        var currentPrompt = _uiState.value.promptInput.trim()

        if (currentPrompt.isBlank()) {
            if (mediaList.isNotEmpty()) {
                currentPrompt = if (mediaList.any { it.isAudio }) {
                    "Please listen to my attached voice reflection and share your compassionate insights."
                } else {
                    "Please reflect on this attached image note and share your compassionate insights."
                }
            } else {
                _uiState.update { it.copy(errorMessage = "Please write a thought, record audio, or attach an image before submitting.") }
                return
            }
        }

        val auth = authState.value
        if (auth !is AuthState.Authenticated) {
            _uiState.update { it.copy(errorMessage = "You must be signed in to submit reflections.") }
            return
        }

        val category = _uiState.value.selectedCategory
        val persona = _uiState.value.selectedPersona
        val user = auth.user
        val geminiMediaParts = mediaList.map { it.toGeminiInlineData() }

        _uiState.update {
            it.copy(
                isGenerating = true,
                errorMessage = null,
                statusMessage = "Reflecting with ${persona.displayName}...",
                lastFailedPrompt = currentPrompt
            )
        }

        val currentThread = _uiState.value.conversationThread

        viewModelScope.launch {
            try {
                val result = geminiRepository.generateContentWithFallback(
                    userPrompt = currentPrompt,
                    mediaList = geminiMediaParts,
                    category = category.label,
                    persona = persona,
                    conversationHistory = currentThread
                )

                result.fold(
                    onSuccess = { geminiResult ->
                        val firstImage = mediaList.firstOrNull { !it.isAudio }
                        val firstAudio = mediaList.firstOrNull { it.isAudio }
                        val combinedTranscripts = mediaList.mapNotNull { it.transcription }.joinToString("\n").ifBlank { null }
                        val mediaType = when {
                            firstAudio != null && firstImage != null -> "multimodal"
                            firstAudio != null -> "voice_note"
                            firstImage != null -> "image"
                            else -> null
                        }

                        val interaction = JournalInteraction(
                            id = UUID.randomUUID().toString(),
                            userId = user.uid,
                            title = currentPrompt.lines().firstOrNull()?.take(50) ?: "Reflection with ${persona.displayName}",
                            prompt = currentPrompt,
                            response = geminiResult.text,
                            summary = geminiResult.summary,
                            category = category.label,
                            tags = listOf(category.name.lowercase(), "ai-reflection", persona.id),
                            modelUsed = geminiResult.modelUsed,
                            timestamp = System.currentTimeMillis(),
                            imageUri = firstImage?.uriString,
                            audioUri = firstAudio?.uriString,
                            mediaType = mediaType,
                            transcription = combinedTranscripts
                        )

                        val updatedThread = currentThread + listOf(
                            ConversationTurn(
                                role = "user",
                                text = currentPrompt,
                                mediaUri = firstImage?.uriString ?: firstAudio?.uriString
                            ),
                            ConversationTurn(
                                role = "model",
                                text = geminiResult.text
                            )
                        )

                        // Persist to Firestore / Room with completion verification
                        firestoreRepository.saveInteraction(interaction).collectLatest { persistResult ->
                            when (persistResult) {
                                is PersistenceResult.InProgress -> {
                                    _uiState.update { it.copy(statusMessage = "Saving reflection to personal archive...") }
                                }
                                is PersistenceResult.Success -> {
                                    _uiState.update {
                                        it.copy(
                                            isGenerating = false,
                                            activeInteraction = interaction,
                                            conversationThread = updatedThread,
                                            promptInput = "", // Only clear input buffer AFTER successful write
                                            attachedMedia = emptyList(), // Clear media attachments
                                            statusMessage = "Reflection saved with ${persona.displayName}",
                                            activeModelBadge = geminiResult.modelUsed
                                        )
                                    }
                                }
                                is PersistenceResult.Error -> {
                                    _uiState.update {
                                        it.copy(
                                            isGenerating = false,
                                            activeInteraction = interaction,
                                            conversationThread = updatedThread,
                                            errorMessage = persistResult.message,
                                            canRetry = persistResult.canRetry,
                                            statusMessage = null
                                        )
                                    }
                                }
                            }
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isGenerating = false,
                                errorMessage = "Generation failed: ${error.localizedMessage ?: "Unknown error"}",
                                canRetry = true,
                                statusMessage = null
                            )
                        }
                    }
                )
            } catch (t: Throwable) {
                Log.e("JournalViewModel", "Error submitting reflection", t)
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = "Reflection interrupted: ${t.localizedMessage ?: "Please try again"}",
                        canRetry = true,
                        statusMessage = null
                    )
                }
            }
        }
    }

    /**
     * Generates a comprehensive monthly or yearly AI journey recap narrative.
     */
    fun generateRecap(period: RecapPeriod) {
        val periodKey = period.displayLabel
        val matchingEntries = _uiState.value.historyInteractions.filter { period.matchesTimestamp(it.timestamp) }
        val persona = _uiState.value.selectedPersona

        _uiState.update { it.copy(isGeneratingRecap = true) }

        viewModelScope.launch {
            try {
                val excerpts = matchingEntries.take(15).joinToString("\n---\n") {
                    "Title: ${it.title}\nCategory: ${it.category}\nPrompt: ${it.prompt.take(150)}"
                }

                val themes = matchingEntries.map { it.category }.distinct()

                val result = geminiRepository.generateRecapNarrative(
                    periodLabel = periodKey,
                    persona = persona,
                    entriesSummary = excerpts.ifBlank { "Regular reflections on life, work, and personal growth." },
                    totalEntries = matchingEntries.size,
                    topThemes = themes.ifEmpty { listOf("Self-awareness", "Daily reflection") }
                )

                result.fold(
                    onSuccess = { narrative ->
                        _uiState.update { state ->
                            val updatedNarratives = state.recapNarratives.toMutableMap()
                            updatedNarratives[periodKey] = narrative
                            state.copy(
                                isGeneratingRecap = false,
                                recapNarratives = updatedNarratives
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isGeneratingRecap = false, errorMessage = "Recap generation failed: ${error.message}") }
                    }
                )
            } catch (t: Throwable) {
                Log.e("JournalViewModel", "Error generating recap", t)
                _uiState.update { it.copy(isGeneratingRecap = false, errorMessage = "Recap generation was interrupted.") }
            }
        }
    }

    fun retryLastPrompt() {
        _uiState.value.lastFailedPrompt?.let { failedPrompt ->
            _uiState.update { it.copy(promptInput = failedPrompt) }
            submitJournalEntry()
        }
    }

    fun deleteInteraction(interaction: JournalInteraction) {
        val auth = authState.value as? AuthState.Authenticated ?: return
        viewModelScope.launch {
            firestoreRepository.deleteInteraction(auth.user.uid, interaction.id)
            if (_uiState.value.activeInteraction?.id == interaction.id) {
                _uiState.update { it.copy(activeInteraction = null) }
            }
        }
    }

    fun sendFollowUpMessage(text: String) {
        if (text.isBlank()) return
        _uiState.update { it.copy(promptInput = text) }
        submitJournalEntry()
    }

    fun clearConversationThread() {
        _uiState.update {
            it.copy(
                conversationThread = emptyList(),
                activeInteraction = null,
                promptInput = "",
                statusMessage = "Started fresh conversation thread"
            )
        }
    }

    fun signInWithEmail(email: String, pass: String, onResult: (Result<AppUser>) -> Unit) {
        viewModelScope.launch {
            authRepository.signInWithEmailPassword(email, pass).collectLatest { onResult(it) }
        }
    }

    fun signUpWithEmail(email: String, pass: String, name: String, onResult: (Result<AppUser>) -> Unit) {
        viewModelScope.launch {
            authRepository.signUpWithEmailPassword(email, pass, name).collectLatest { onResult(it) }
        }
    }

    fun signInAnonymously(onResult: (Result<AppUser>) -> Unit) {
        viewModelScope.launch {
            authRepository.signInAnonymously().collectLatest { onResult(it) }
        }
    }

    fun signInSandbox(name: String = "Reflective Explorer", email: String = "explorer@reflectai.app") {
        authRepository.signInSandboxUser(name, email)
    }

    fun signInCustom(user: AppUser) {
        authRepository.signInWithCustomUser(user)
    }

    fun signOut() {
        authRepository.signOut()
        startNewSession()
    }

    override fun onCleared() {
        super.onCleared()
        audioRecordingManager.cancelRecording()
    }
}


