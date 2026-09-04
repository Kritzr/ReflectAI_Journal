package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CalmBlushLight
import com.example.ui.theme.CalmLavender
import com.example.ui.theme.CalmLavenderGlow
import com.example.ui.theme.CalmLavenderLight
import com.example.ui.theme.CalmSage
import com.example.ui.theme.CalmSageGlow
import com.example.ui.theme.CalmSageLight
import com.example.ui.theme.TertiaryTeal
import kotlinx.coroutines.delay

private val CalmingAffirmations = listOf(
    "In this moment, there is nowhere else you need to be.",
    "Breathe gently. Your feelings are valid and welcomed here.",
    "Give yourself permission to pause, soften, and simply be.",
    "Every breath in brings clarity; every breath out releases tension.",
    "Be kind to the version of you that is learning and healing.",
    "Thoughts are like clouds drifting through a vast, quiet sky."
)

enum class BreathPhase(val instruction: String, val cue: String, val durationMs: Int) {
    INHALE("Breathe In", "Filling with calm and quiet space", 4000),
    HOLD("Hold Gently", "Resting in peaceful stillness", 4000),
    EXHALE("Release Out", "Letting go of all tension and rush", 4000),
    REST("Rest & Soften", "Present, grounded, and centered", 2000)
}

/**
 * A serene, beautifully animated Mindful Sanctuary & Guided Breathing Widget.
 * Offers tranquil breathing pacing and daily centering affirmations.
 */
@Composable
fun MindfulBreathingWidget(
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isBreathingActive by remember { mutableStateOf(false) }
    var currentPhase by remember { mutableStateOf(BreathPhase.INHALE) }
    var currentAffirmationIndex by remember { mutableIntStateOf(0) }

    // Loop through guided breath phases when active
    LaunchedEffect(isBreathingActive) {
        if (!isBreathingActive) return@LaunchedEffect
        while (isBreathingActive) {
            currentPhase = BreathPhase.INHALE
            delay(BreathPhase.INHALE.durationMs.toLong())
            if (!isBreathingActive) break

            currentPhase = BreathPhase.HOLD
            delay(BreathPhase.HOLD.durationMs.toLong())
            if (!isBreathingActive) break

            currentPhase = BreathPhase.EXHALE
            delay(BreathPhase.EXHALE.durationMs.toLong())
            if (!isBreathingActive) break

            currentPhase = BreathPhase.REST
            delay(BreathPhase.REST.durationMs.toLong())
        }
    }

    // Infinite soothing ambient shimmer
    val ambientTransition = rememberInfiniteTransition(label = "sanctuary_ambient")
    val pulseGlow by ambientTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sanctuary_glow"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    CalmSage.copy(alpha = 0.35f),
                    CalmLavender.copy(alpha = 0.40f),
                    TertiaryTeal.copy(alpha = 0.30f)
                )
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("mindful_breathing_widget")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Icon + Sanctuary Title + Expand/Collapse Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        CalmSageLight.copy(alpha = pulseGlow),
                                        CalmLavenderLight.copy(alpha = 0.4f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Spa,
                            contentDescription = null,
                            tint = CalmSage,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Mindful Sanctuary",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = CalmSage.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Center & Breathe",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CalmSage,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isBreathingActive) "${currentPhase.instruction} • ${currentPhase.cue}" else "Take a mindful pause before writing",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle Sanctuary",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Compact Daily Affirmation Banner (Always visible or compact)
            if (!isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            currentAffirmationIndex = (currentAffirmationIndex + 1) % CalmingAffirmations.size
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✨",
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "“${CalmingAffirmations[currentAffirmationIndex]}”",
                            fontSize = 11.5.sp,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Tap",
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Expanded Guided Breathing Circle Canvas & Exercises
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Guided Breathing Canvas Orb
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Dynamic animated breath scale
                        val breathProgressTransition = rememberInfiniteTransition(label = "breath_orb")
                        val orbScale by breathProgressTransition.animateFloat(
                            initialValue = if (isBreathingActive) 0.65f else 0.85f,
                            targetValue = if (isBreathingActive) 1.0f else 0.95f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(
                                    durationMillis = if (isBreathingActive) 4000 else 3000,
                                    easing = FastOutSlowInEasing
                                ),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "orb_scale"
                        )

                        Canvas(modifier = Modifier.size(140.dp)) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val baseRadius = (size.minDimension / 2f) * orbScale

                            // Outer ambient aura
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        CalmLavenderGlow.copy(alpha = 0.35f),
                                        CalmSageGlow.copy(alpha = 0.15f),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = baseRadius * 1.25f
                                ),
                                radius = baseRadius * 1.25f,
                                center = center
                            )

                            // Middle organic breathing orb
                            drawCircle(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        CalmSage.copy(alpha = 0.25f),
                                        CalmLavender.copy(alpha = 0.35f),
                                        TertiaryTeal.copy(alpha = 0.25f)
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, size.height)
                                ),
                                radius = baseRadius,
                                center = center
                            )

                            // Delicate ring accent
                            drawCircle(
                                color = CalmLavender.copy(alpha = 0.55f),
                                radius = baseRadius * 0.98f,
                                center = center,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        // Text Inside Orb
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = if (isBreathingActive) currentPhase.instruction else "Begin",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isBreathingActive) currentPhase.cue else "Tap button to start 4-4-4 rhythm",
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Breathing Action Buttons & Affirmation Refresh
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = {
                                isBreathingActive = !isBreathingActive
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("toggle_breathing_button")
                        ) {
                            Icon(
                                imageVector = if (isBreathingActive) Icons.Default.Close else Icons.Default.SelfImprovement,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isBreathingActive) MaterialTheme.colorScheme.error else CalmSage
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBreathingActive) "Stop Breathing Guide" else "Start Guided Breath (4-4-4)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                currentAffirmationIndex = (currentAffirmationIndex + 1) % CalmingAffirmations.size
                            },
                            modifier = Modifier.testTag("refresh_affirmation_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Next Affirmation",
                                tint = CalmLavender,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Full Calm Affirmation Quote Box
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🕊️ Sanctuary Affirmation",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CalmSage
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "“${CalmingAffirmations[currentAffirmationIndex]}”",
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }
    }
}
