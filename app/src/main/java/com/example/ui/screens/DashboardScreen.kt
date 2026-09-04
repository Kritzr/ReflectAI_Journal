package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import com.example.data.remote.gemini.ConversationTurn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.AppUser
import com.example.data.model.AiListenerPersona
import com.example.data.model.JournalInteraction
import com.example.data.model.PromptHabitTrack
import com.example.data.model.ReflectionCategory
import com.example.ui.components.CategoryChip
import com.example.ui.components.ErrorBannerWithRetry
import com.example.ui.components.HabitJournalPromptsSection
import com.example.ui.components.HumanListenerAvatar
import com.example.ui.components.ListenerAvatarState
import com.example.ui.components.ListenerCompanionHeader
import com.example.ui.components.MediaAttachmentSection
import com.example.ui.components.MindfulBreathingWidget
import com.example.ui.components.ModelBadge
import com.example.ui.components.PersonaSelectionModal
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.CalmLavender
import com.example.ui.theme.CalmSage
import com.example.ui.theme.SecondaryViolet
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.util.AttachedMediaItem
import com.example.ui.viewmodel.DashboardUiState
import coil.compose.AsyncImage

@Composable
fun DashboardScreen(
    user: AppUser,
    uiState: DashboardUiState,
    onPromptChanged: (String) -> Unit,
    onCategorySelected: (ReflectionCategory) -> Unit,
    onPersonaSelected: (AiListenerPersona) -> Unit,
    onSubmitClicked: () -> Unit,
    onRetryClicked: () -> Unit,
    onErrorDismissed: () -> Unit,
    onNewSessionClicked: () -> Unit,
    onImageSelected: (android.net.Uri) -> Unit,
    onAudioSelected: (android.net.Uri) -> Unit,
    onStartRecording: () -> Boolean,
    onStopRecording: (Boolean) -> Unit,
    onCancelRecording: () -> Unit,
    onRemoveMedia: (String) -> Unit,
    onTranscribeMedia: (AttachedMediaItem) -> Unit,
    onPromptSelected: (String, ReflectionCategory) -> Unit = { p, c -> onPromptChanged(p); onCategorySelected(c) },
    onGenerateAiHabitPrompt: (PromptHabitTrack) -> Unit = {},
    onFollowUpSubmitted: (String) -> Unit = {},
    onClearThreadClicked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var showPersonaModal by remember { mutableStateOf(false) }

    val persona = uiState.selectedPersona

    val avatarState = when {
        uiState.isGenerating -> ListenerAvatarState.REFLECTING
        uiState.isRecordingAudio -> ListenerAvatarState.LISTENING
        uiState.promptInput.isNotBlank() || uiState.attachedMedia.isNotEmpty() -> ListenerAvatarState.LISTENING
        else -> ListenerAvatarState.READY
    }

    val starterPrompts = listOf(
        "🌿 What is one small moment of peace I felt today?",
        "🕊️ Help me gently reframe a worry or doubt...",
        "✨ Reflect on an insight or change I'm experiencing...",
        "💡 Brainstorm 5 inspiring avenues for...",
        "🌙 What can I forgive myself for and release tonight?"
    )

    val ambientBackground = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ambientBackground)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Personalized Human AI Listener Companion Header
        ListenerCompanionHeader(
            persona = persona,
            state = avatarState,
            onTunePersonaClicked = { showPersonaModal = true },
            modifier = Modifier.testTag("listener_companion_header")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Interactive Mindful Breathing & Sanctuary Affirmations Widget
        MindfulBreathingWidget()

        Spacer(modifier = Modifier.height(12.dp))

        // User Context Strip
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(persona.accentColorHex).copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User",
                            tint = Color(persona.accentColorHex),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "Reflecting as ${user.displayName}",
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                if (uiState.activeInteraction != null) {
                    FilledTonalButton(
                        onClick = onNewSessionClicked,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("new_session_header_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Reflection",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Entry", fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Daily Habit & Consistency Prompts Section
        HabitJournalPromptsSection(
            interactions = uiState.historyInteractions,
            isGeneratingAiPrompt = uiState.isGeneratingHabitPrompt,
            onPromptSelected = { promptText, category ->
                onPromptSelected(promptText, category)
            },
            onGenerateAiPrompt = { track ->
                onGenerateAiHabitPrompt(track)
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Reflection Category Selector
        Text(
            text = "Conversation Focus",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReflectionCategory.entries.forEach { category ->
                CategoryChip(
                    category = category,
                    isSelected = uiState.selectedCategory == category,
                    onSelect = { onCategorySelected(category) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // System Error Banner with Retry
        ErrorBannerWithRetry(
            message = uiState.errorMessage,
            canRetry = uiState.canRetry,
            onRetry = onRetryClicked,
            onDismiss = onErrorDismissed
        )

        // Active Multi-Turn Conversation Thread or Single Interaction Result
        if (uiState.conversationThread.isNotEmpty()) {
            MultiTurnConversationCard(
                turns = uiState.conversationThread,
                persona = persona,
                isGenerating = uiState.isGenerating,
                onFollowUpSubmitted = onFollowUpSubmitted,
                onClearThreadClicked = onClearThreadClicked,
                onCopyClicked = { text ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("ReflectAI", text))
                    Toast.makeText(context, "Copied reflection to clipboard", Toast.LENGTH_SHORT).show()
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        } else if (uiState.activeInteraction != null) {
            ActiveInteractionView(
                interaction = uiState.activeInteraction,
                persona = persona,
                onCopyClicked = { text ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("ReflectAI", text))
                    Toast.makeText(context, "Copied reflection to clipboard", Toast.LENGTH_SHORT).show()
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Live Generation Progress Indicator
        if (uiState.isGenerating) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("generation_loading_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "spin")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing)
                        ),
                        label = "spin_angle"
                    )

                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(rotation)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = uiState.statusMessage ?: "${persona.displayName} is listening and reflecting...",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Multimodal Ladder: 2.5 Flash ➔ 3.1 Flash-Lite ➔ Pro",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Prompt Input Section
        val promptCardBrush = Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(promptCardBrush, RoundedCornerShape(22.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(persona.accentColorHex))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Reflect with ${persona.displayName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${uiState.promptInput.length} chars",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = uiState.promptInput,
                    onValueChange = onPromptChanged,
                    placeholder = {
                        Text(
                            text = "Share your thoughts, feelings, voice note, or photo in this quiet sanctuary...",
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    minLines = 4,
                    maxLines = 10,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("journal_prompt_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Multimedia Ingestion Bar (Photo Picker, Voice Note Recorder, Audio Uploader)
                MediaAttachmentSection(
                    attachedMedia = uiState.attachedMedia,
                    isRecordingAudio = uiState.isRecordingAudio,
                    recordingDurationMillis = uiState.recordingDurationMillis,
                    recordingAmplitude = uiState.recordingAmplitude,
                    isTranscribingMedia = uiState.isTranscribingMedia,
                    onImageSelected = onImageSelected,
                    onAudioSelected = onAudioSelected,
                    onStartRecording = onStartRecording,
                    onStopRecording = onStopRecording,
                    onCancelRecording = onCancelRecording,
                    onRemoveMedia = onRemoveMedia,
                    onTranscribeMedia = onTranscribeMedia,
                    modifier = Modifier.testTag("media_attachment_section")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Starter Prompts Chips (if input is empty)
                if (uiState.promptInput.isBlank() && uiState.attachedMedia.isEmpty()) {
                    Text(
                        text = "Centering Invocations & Starters:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        starterPrompts.forEach { starter ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onPromptChanged(starter) }
                                    .testTag("starter_chip_${starter.take(10)}")
                            ) {
                                Text(
                                    text = starter,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val canSubmit = !uiState.isGenerating && (uiState.promptInput.isNotBlank() || uiState.attachedMedia.isNotEmpty())
                    Button(
                        onClick = onSubmitClicked,
                        enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("submit_reflection_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = AccentAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Reflect with ${persona.displayName}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    if (uiState.activeInteraction != null) {
                        FilledTonalButton(
                            onClick = onNewSessionClicked,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("new_entry_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Entry",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Persona Selector Dialog
    if (showPersonaModal) {
        PersonaSelectionModal(
            selectedPersona = persona,
            onPersonaSelected = {
                onPersonaSelected(it)
                showPersonaModal = false
            },
            onDismiss = { showPersonaModal = false }
        )
    }
}

@Composable
private fun ActiveInteractionView(
    interaction: JournalInteraction,
    persona: AiListenerPersona,
    onCopyClicked: (String) -> Unit
) {
    val accentColor = Color(persona.accentColorHex)
    val cardBrush = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.surface,
            accentColor.copy(alpha = 0.05f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        )
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBrush, RoundedCornerShape(22.dp))
            .testTag("active_interaction_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Persona Avatar, Category & Model Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HumanListenerAvatar(
                        persona = persona,
                        state = ListenerAvatarState.READY,
                        size = 38.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${persona.displayName}'s Reflection",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = interaction.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = accentColor
                        )
                    }
                }

                ModelBadge(modelName = interaction.modelUsed)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // User Prompt Excerpt & Attached Media
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Your Reflection:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = interaction.prompt,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Attached Image in Reflection Card
                    if (!interaction.imageUri.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        AsyncImage(
                            model = interaction.imageUri,
                            contentDescription = "Attached Reflection Image",
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }

                    // Attached Audio/Voice Note Badge
                    if (!interaction.audioUri.isNullOrBlank() || interaction.mediaType == "voice_note") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = SecondaryViolet.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = SecondaryViolet,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Voice Note Attached",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SecondaryViolet
                                )
                            }
                        }
                    }

                    // Transcription Excerpt if present
                    if (!interaction.transcription.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Transcribed Voice & Image Content:",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = interaction.transcription,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Persona Response Body Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Empathic Guidance",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = { onCopyClicked(interaction.response) },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("copy_reflection_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Reflection",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = interaction.response,
                fontSize = 14.5.sp,
                lineHeight = 23.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("gemini_response_text")
            )

            if (interaction.summary.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = AccentAmber.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = AccentAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = interaction.summary,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiTurnConversationCard(
    turns: List<ConversationTurn>,
    persona: AiListenerPersona,
    isGenerating: Boolean,
    onFollowUpSubmitted: (String) -> Unit,
    onClearThreadClicked: () -> Unit,
    onCopyClicked: (String) -> Unit
) {
    var followUpInput by remember { mutableStateOf("") }
    val accentColor = Color(persona.accentColorHex)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("multi_turn_conversation_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Persona Avatar, Thread Title, Clear Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HumanListenerAvatar(
                        persona = persona,
                        state = if (isGenerating) ListenerAvatarState.REFLECTING else ListenerAvatarState.READY,
                        size = 36.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Dialogue with ${persona.displayName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${turns.size} conversation turns • Gemini Multi-Turn",
                            fontSize = 11.sp,
                            color = accentColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(
                    onClick = onClearThreadClicked,
                    modifier = Modifier.size(32.dp).testTag("clear_thread_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "New Thread",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Turns Display
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                turns.forEachIndexed { index, turn ->
                    if (turn.role == "user") {
                        // User message bubble (Right aligned)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth(0.88f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "You",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (turn.mediaUri != null) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Mic,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Media attached",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = turn.text,
                                        fontSize = 13.5.sp,
                                        lineHeight = 19.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    } else {
                        // Model reflection bubble (Left aligned)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth(0.92f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Psychology,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = persona.displayName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = accentColor
                                            )
                                        }

                                        IconButton(
                                            onClick = { onCopyClicked(turn.text) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = turn.text,
                                        fontSize = 13.5.sp,
                                        lineHeight = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Multi-Turn Follow-up Input Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = followUpInput,
                    onValueChange = { followUpInput = it },
                    placeholder = {
                        Text(
                            text = "Reply to ${persona.displayName}...",
                            fontSize = 13.sp
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("follow_up_input")
                )

                IconButton(
                    onClick = {
                        if (followUpInput.isNotBlank()) {
                            val textToSend = followUpInput.trim()
                            followUpInput = ""
                            onFollowUpSubmitted(textToSend)
                        }
                    },
                    enabled = !isGenerating && followUpInput.isNotBlank(),
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            if (!isGenerating && followUpInput.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                        .testTag("send_follow_up_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Follow-up",
                        tint = if (!isGenerating && followUpInput.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

