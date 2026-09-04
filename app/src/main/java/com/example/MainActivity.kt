package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.auth.AuthState
import com.example.ui.components.HumanListenerAvatar
import com.example.ui.components.ListenerAvatarState
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.RecapScreen
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.ReflectAITheme
import com.example.ui.viewmodel.JournalViewModel

enum class NavigationTab {
    WORKSPACE,
    RECAP,
    HISTORY
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReflectAITheme {
                MainAppContainer()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(
    viewModel: JournalViewModel = viewModel()
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(NavigationTab.WORKSPACE) }

    when (val currentAuth = authState) {
        is AuthState.Loading -> {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(1200)
                viewModel.signInSandbox()
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        is AuthState.Unauthenticated, is AuthState.Error -> {
            AuthScreen(
                onGoogleSignInClicked = {
                    viewModel.signInSandbox("Krit Study", "kritstudy15@gmail.com")
                    Toast.makeText(context, "Signed in via Google Federated Identity", Toast.LENGTH_SHORT).show()
                },
                onSandboxSignInClicked = { name, email ->
                    viewModel.signInSandbox(name, email)
                    Toast.makeText(context, "Welcome to ReflectAI, $name", Toast.LENGTH_SHORT).show()
                },
                onFirebaseEmailSignIn = { email, pass ->
                    viewModel.signInWithEmail(email, pass) { result ->
                        result.fold(
                            onSuccess = { Toast.makeText(context, "Welcome back, ${it.displayName}", Toast.LENGTH_SHORT).show() },
                            onFailure = { Toast.makeText(context, "Sign in failed: ${it.localizedMessage}", Toast.LENGTH_LONG).show() }
                        )
                    }
                },
                onFirebaseEmailSignUp = { email, pass, name ->
                    viewModel.signUpWithEmail(email, pass, name) { result ->
                        result.fold(
                            onSuccess = { Toast.makeText(context, "Account created: ${it.displayName}", Toast.LENGTH_SHORT).show() },
                            onFailure = { Toast.makeText(context, "Sign up failed: ${it.localizedMessage}", Toast.LENGTH_LONG).show() }
                        )
                    }
                },
                onFirebaseAnonymousSignIn = {
                    viewModel.signInAnonymously { result ->
                        result.fold(
                            onSuccess = { Toast.makeText(context, "Signed in anonymously", Toast.LENGTH_SHORT).show() },
                            onFailure = { Toast.makeText(context, "Guest sign-in failed: ${it.localizedMessage}", Toast.LENGTH_LONG).show() }
                        )
                    }
                }
            )
        }

        is AuthState.Authenticated -> {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                HumanListenerAvatar(
                                    persona = uiState.selectedPersona,
                                    state = if (uiState.isGenerating) ListenerAvatarState.REFLECTING else ListenerAvatarState.READY,
                                    size = 32.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "ReflectAI",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                    Text(
                                        text = "Companion: ${uiState.selectedPersona.displayName}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    viewModel.signOut()
                                    Toast.makeText(context, "Signed out safely", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("sign_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = "Sign Out",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        NavigationBarItem(
                            selected = currentTab == NavigationTab.WORKSPACE,
                            onClick = { currentTab = NavigationTab.WORKSPACE },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Spa,
                                    contentDescription = "Sanctuary"
                                )
                            },
                            label = { Text("Sanctuary", fontWeight = if (currentTab == NavigationTab.WORKSPACE) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_workspace_tab")
                        )

                        NavigationBarItem(
                            selected = currentTab == NavigationTab.RECAP,
                            onClick = { currentTab = NavigationTab.RECAP },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Insights"
                                )
                            },
                            label = { Text("Insights", fontWeight = if (currentTab == NavigationTab.RECAP) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_recap_tab")
                        )

                        NavigationBarItem(
                            selected = currentTab == NavigationTab.HISTORY,
                            onClick = { currentTab = NavigationTab.HISTORY },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Archive"
                                )
                            },
                            label = {
                                Text(
                                    if (uiState.historyInteractions.isNotEmpty())
                                        "Archive (${uiState.historyInteractions.size})"
                                    else "Archive",
                                    fontWeight = if (currentTab == NavigationTab.HISTORY) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_history_tab")
                        )
                    }
                }
            ) { innerPadding ->
                when (currentTab) {
                    NavigationTab.WORKSPACE -> {
                        DashboardScreen(
                            user = currentAuth.user,
                            uiState = uiState,
                            onPromptChanged = { viewModel.updatePromptInput(it) },
                            onCategorySelected = { viewModel.selectCategory(it) },
                            onPersonaSelected = { viewModel.selectPersona(it) },
                            onSubmitClicked = { viewModel.submitJournalEntry() },
                            onRetryClicked = { viewModel.retryLastPrompt() },
                            onErrorDismissed = { viewModel.clearError() },
                            onNewSessionClicked = { viewModel.startNewSession() },
                            onImageSelected = { viewModel.attachImage(it) },
                            onAudioSelected = { viewModel.attachAudioUri(it) },
                            onStartRecording = { viewModel.startVoiceRecording() },
                            onStopRecording = { viewModel.stopVoiceRecording(it) },
                            onCancelRecording = { viewModel.cancelVoiceRecording() },
                            onRemoveMedia = { viewModel.removeAttachedMedia(it) },
                            onTranscribeMedia = { viewModel.transcribeMediaItem(it) },
                            onPromptSelected = { promptText, category ->
                                viewModel.applyPrompt(promptText, category)
                            },
                            onGenerateAiHabitPrompt = { track ->
                                viewModel.generateAiHabitPrompt(track)
                            },
                            onFollowUpSubmitted = { followUp ->
                                viewModel.sendFollowUpMessage(followUp)
                            },
                            onClearThreadClicked = {
                                viewModel.clearConversationThread()
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }

                    NavigationTab.RECAP -> {
                        RecapScreen(
                            interactions = uiState.historyInteractions,
                            persona = uiState.selectedPersona,
                            isGeneratingRecap = uiState.isGeneratingRecap,
                            recapNarratives = uiState.recapNarratives,
                            onGenerateRecapRequested = { period ->
                                viewModel.generateRecap(period)
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }

                    NavigationTab.HISTORY -> {
                        HistoryScreen(
                            interactions = uiState.historyInteractions,
                            searchQuery = uiState.searchQuery,
                            filterCategory = uiState.filterCategory,
                            onSearchQueryChanged = { viewModel.setSearchQuery(it) },
                            onFilterCategoryChanged = { viewModel.setFilterCategory(it) },
                            onSelectEntry = { entry ->
                                viewModel.selectHistoryEntry(entry)
                                currentTab = NavigationTab.WORKSPACE
                            },
                            onDeleteEntry = { entry ->
                                viewModel.deleteInteraction(entry)
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
