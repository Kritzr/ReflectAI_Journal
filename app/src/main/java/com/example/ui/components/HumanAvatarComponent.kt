package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiListenerPersona
import com.example.data.model.AvatarStyle
import com.example.ui.theme.AccentAmber

enum class ListenerAvatarState {
    READY,
    LISTENING,
    REFLECTING
}

/**
 * Expressive Human Avatar Composable for the Personalized AI Listener.
 * Renders stylized facial features, hairstyles, empathetic eyes, active listening aura,
 * and reflective insight glows.
 */
@Composable
fun HumanListenerAvatar(
    persona: AiListenerPersona,
    state: ListenerAvatarState = ListenerAvatarState.READY,
    size: Dp = 80.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_motion")

    // Pulse animation for active listening state
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (state == ListenerAvatarState.LISTENING || state == ListenerAvatarState.REFLECTING) 1.15f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_pulse"
    )

    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = if (state == ListenerAvatarState.LISTENING || state == ListenerAvatarState.REFLECTING) 0.65f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_alpha"
    )

    val accentColor = Color(persona.accentColorHex)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Outer Listening Glow
        Box(
            modifier = Modifier
                .size(size * 1.08f)
                .scale(auraScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = auraAlpha),
                            accentColor.copy(alpha = 0f)
                        )
                    )
                )
        )

        // Main Stylized Canvas Avatar
        Canvas(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
        ) {
            drawHumanAvatarFace(persona.avatarStyle, accentColor)
        }

        // Active State Badge (Earphone / Audio waves / Sparkle)
        val badgeSize = (size.value * 0.32f).dp
        Surface(
            color = when (state) {
                ListenerAvatarState.READY -> accentColor
                ListenerAvatarState.LISTENING -> AccentAmber
                ListenerAvatarState.REFLECTING -> Color(0xFF8E24AA)
            },
            shape = CircleShape,
            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .size(badgeSize)
                .align(Alignment.BottomEnd)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = when (state) {
                        ListenerAvatarState.READY -> Icons.Default.Hearing
                        ListenerAvatarState.LISTENING -> Icons.Default.Headphones
                        ListenerAvatarState.REFLECTING -> Icons.Default.AutoAwesome
                    },
                    contentDescription = "Listener State",
                    tint = Color.White,
                    modifier = Modifier.size(badgeSize * 0.6f)
                )
            }
        }
    }
}

/**
 * Draws custom vector styled human characters with expressive traits.
 */
private fun DrawScope.drawHumanAvatarFace(style: AvatarStyle, accentColor: Color) {
    val width = size.width
    val height = size.height

    // Background circle gradient
    val bgBrush = when (style) {
        AvatarStyle.MAYA -> Brush.verticalGradient(listOf(Color(0xFFEDE7F6), Color(0xFFD1C4E9)))
        AvatarStyle.LEO -> Brush.verticalGradient(listOf(Color(0xFFE0F2F1), Color(0xFFB2DFDB)))
        AvatarStyle.ELENA -> Brush.verticalGradient(listOf(Color(0xFFFFF8E1), Color(0xFFFFECB3)))
        AvatarStyle.SAM -> Brush.verticalGradient(listOf(Color(0xFFE3F2FD), Color(0xFFBBDEFB)))
    }
    drawCircle(brush = bgBrush, radius = width / 2f, center = Offset(width / 2f, height / 2f))

    // Skin tones and Hair styles
    val skinColor = when (style) {
        AvatarStyle.MAYA -> Color(0xFFFFCC80) // Warm honey
        AvatarStyle.LEO -> Color(0xFFE0A96D) // Golden sand
        AvatarStyle.ELENA -> Color(0xFFFFE0B2) // Peach porcelain
        AvatarStyle.SAM -> Color(0xFFF5CBA7) // Warm beige
    }

    val hairColor = when (style) {
        AvatarStyle.MAYA -> Color(0xFF3E2723) // Deep dark espresso
        AvatarStyle.LEO -> Color(0xFF263238) // Charcoal wave
        AvatarStyle.ELENA -> Color(0xFF5D4037) // Chestnut brown
        AvatarStyle.SAM -> Color(0xFF795548) // Caramel soft crop
    }

    val clothingColor = when (style) {
        AvatarStyle.MAYA -> Color(0xFF7C4DFF)
        AvatarStyle.LEO -> Color(0xFF00BFA5)
        AvatarStyle.ELENA -> Color(0xFFFFB300)
        AvatarStyle.SAM -> Color(0xFF2979FF)
    }

    // Shoulders & Collar (Bottom curves)
    val shoulderPath = Path().apply {
        moveTo(width * 0.1f, height)
        cubicTo(
            width * 0.2f, height * 0.72f,
            width * 0.8f, height * 0.72f,
            width * 0.9f, height
        )
        close()
    }
    drawPath(shoulderPath, clothingColor)

    // Neck
    val neckPath = Path().apply {
        moveTo(width * 0.42f, height * 0.62f)
        lineTo(width * 0.58f, height * 0.62f)
        lineTo(width * 0.60f, height * 0.78f)
        lineTo(width * 0.40f, height * 0.78f)
        close()
    }
    drawPath(neckPath, skinColor.copy(alpha = 0.9f))

    // Head / Face oval
    drawOval(
        color = skinColor,
        topLeft = Offset(width * 0.26f, height * 0.22f),
        size = Size(width * 0.48f, height * 0.52f)
    )

    // Hair (Back / Top silhouette)
    when (style) {
        AvatarStyle.MAYA -> {
            // Elegant flowing wavy hair
            val hairPath = Path().apply {
                moveTo(width * 0.22f, height * 0.42f)
                cubicTo(width * 0.20f, height * 0.12f, width * 0.80f, height * 0.12f, width * 0.78f, height * 0.42f)
                cubicTo(width * 0.86f, height * 0.65f, width * 0.82f, height * 0.85f, width * 0.75f, height * 0.88f)
                cubicTo(width * 0.70f, height * 0.55f, width * 0.75f, height * 0.32f, width * 0.68f, height * 0.26f)
                cubicTo(width * 0.50f, height * 0.20f, width * 0.32f, height * 0.26f, width * 0.25f, height * 0.88f)
                close()
            }
            drawPath(hairPath, hairColor)
        }
        AvatarStyle.LEO -> {
            // Modern textured swept crop
            val hairPath = Path().apply {
                moveTo(width * 0.24f, height * 0.32f)
                cubicTo(width * 0.22f, height * 0.10f, width * 0.78f, height * 0.08f, width * 0.80f, height * 0.32f)
                cubicTo(width * 0.72f, height * 0.24f, width * 0.52f, height * 0.18f, width * 0.28f, height * 0.28f)
                close()
            }
            drawPath(hairPath, hairColor)
        }
        AvatarStyle.ELENA -> {
            // Refined side-parted bob with soft curls
            val hairPath = Path().apply {
                moveTo(width * 0.20f, height * 0.52f)
                cubicTo(width * 0.18f, height * 0.14f, width * 0.82f, height * 0.14f, width * 0.80f, height * 0.52f)
                cubicTo(width * 0.74f, height * 0.38f, width * 0.68f, height * 0.22f, width * 0.30f, height * 0.26f)
                close()
            }
            drawPath(hairPath, hairColor)
        }
        AvatarStyle.SAM -> {
            // Friendly relaxed soft parted hair
            val hairPath = Path().apply {
                moveTo(width * 0.24f, height * 0.36f)
                cubicTo(width * 0.20f, height * 0.12f, width * 0.80f, height * 0.12f, width * 0.76f, height * 0.36f)
                cubicTo(width * 0.65f, height * 0.24f, width * 0.35f, height * 0.24f, width * 0.24f, height * 0.36f)
                close()
            }
            drawPath(hairPath, hairColor)
        }
    }

    // Warm Empathetic Eyes
    val eyeColor = Color(0xFF212121)
    drawOval(
        color = eyeColor,
        topLeft = Offset(width * 0.38f, height * 0.44f),
        size = Size(width * 0.065f, height * 0.045f)
    )
    drawOval(
        color = eyeColor,
        topLeft = Offset(width * 0.56f, height * 0.44f),
        size = Size(width * 0.065f, height * 0.045f)
    )

    // Eye sparkles (reflecting warmth)
    drawCircle(
        color = Color.White,
        radius = width * 0.012f,
        center = Offset(width * 0.395f, height * 0.45f)
    )
    drawCircle(
        color = Color.White,
        radius = width * 0.012f,
        center = Offset(width * 0.575f, height * 0.45f)
    )

    // Gentle Welcoming Smile
    val smilePath = Path().apply {
        moveTo(width * 0.42f, height * 0.58f)
        cubicTo(
            width * 0.46f, height * 0.65f,
            width * 0.54f, height * 0.65f,
            width * 0.58f, height * 0.58f
        )
    }
    drawPath(
        path = smilePath,
        color = Color(0xFF8D6E63),
        style = Stroke(width = width * 0.025f)
    )

    // Cheerful rosy cheeks
    drawCircle(
        color = Color(0xFFFF8A80).copy(alpha = 0.35f),
        radius = width * 0.045f,
        center = Offset(width * 0.34f, height * 0.52f)
    )
    drawCircle(
        color = Color(0xFFFF8A80).copy(alpha = 0.35f),
        radius = width * 0.045f,
        center = Offset(width * 0.66f, height * 0.52f)
    )
}

/**
 * Top listener hero banner card shown on the workspace screen.
 */
@Composable
fun ListenerCompanionHeader(
    persona: AiListenerPersona,
    state: ListenerAvatarState,
    onTunePersonaClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(persona.accentColorHex)
    val cardBackgroundBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            accentColor.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    accentColor.copy(alpha = 0.30f),
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(cardBackgroundBrush, RoundedCornerShape(22.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HumanListenerAvatar(
                persona = persona,
                state = state,
                size = 70.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = persona.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = accentColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = persona.subtitle,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when (state) {
                        ListenerAvatarState.READY -> persona.listeningGreeting
                        ListenerAvatarState.LISTENING -> "Listening closely to your voice and words..."
                        ListenerAvatarState.REFLECTING -> "Synthesizing thoughtful, caring perspectives..."
                    },
                    fontSize = 12.sp,
                    lineHeight = 16.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtle Calming State Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                when (state) {
                                    ListenerAvatarState.READY -> accentColor
                                    ListenerAvatarState.LISTENING -> AccentAmber
                                    ListenerAvatarState.REFLECTING -> Color(0xFF8E24AA)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = when (state) {
                            ListenerAvatarState.READY -> "Serene & Present"
                            ListenerAvatarState.LISTENING -> "Attentive Listening"
                            ListenerAvatarState.REFLECTING -> "Deep Reflection"
                        },
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = CircleShape,
                modifier = Modifier.size(38.dp)
            ) {
                IconButton(
                    onClick = onTunePersonaClicked,
                    modifier = Modifier.testTag("tune_persona_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Change Listener Persona",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Persona Selection Dialog / Sheet.
 */
@Composable
fun PersonaSelectionModal(
    selectedPersona: AiListenerPersona,
    onPersonaSelected: (AiListenerPersona) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Choose Your Listener",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Personalize who listens to your daily reflections and thoughts:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AiListenerPersona.entries.forEach { persona ->
                    val isSelected = persona == selectedPersona
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                Color(persona.accentColorHex).copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(persona.accentColorHex)) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onPersonaSelected(persona)
                            }
                            .testTag("persona_option_${persona.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HumanListenerAvatar(
                                persona = persona,
                                state = ListenerAvatarState.READY,
                                size = 46.dp
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = persona.displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "• ${persona.subtitle}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = persona.description,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color(persona.accentColorHex),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Close")
            }
        }
    )
}
