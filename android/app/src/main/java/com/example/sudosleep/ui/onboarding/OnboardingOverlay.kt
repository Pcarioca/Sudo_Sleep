package com.example.sudosleep.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Pinch
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private data class CoachMarkStep(
    val emoji: String,
    val title: String,
    val description: String,
    val hintBadge: String,
    val icon: ImageVector,
    val verticalAlignment: Alignment
)

private val TUTORIAL_STEPS = listOf(
    CoachMarkStep(
        emoji = "👆✨",
        title = "Direct Tap-to-Add on Schedule",
        description = "Touch any empty hour slot on the 08:00–22:00 grid to immediately add a class, lab, or exam right at that exact day and time—no extra menus needed!",
        hintBadge = "Try tapping any empty row on the grid",
        icon = Icons.Default.TouchApp,
        verticalAlignment = Alignment.Center
    ),
    CoachMarkStep(
        emoji = "🤏🔍",
        title = "Pinch to Zoom & Swipe Days",
        description = "Pinch vertically with two fingers on the timetable to zoom out and see your whole 08:00–22:00 day on a single screen, or swipe left/right to glide between days!",
        hintBadge = "Pinch to scale • Swipe left/right for days",
        icon = Icons.Default.Pinch,
        verticalAlignment = Alignment.Center
    ),
    CoachMarkStep(
        emoji = "🗓️🎯",
        title = "Day, Week & Month Views + Exams",
        description = "Use the switcher at the top to flip between Detailed Day View, Bird's-Eye 7-Day Week View, and Monthly Calendar View with Exam countdowns!",
        hintBadge = "Located in the top view switcher bar",
        icon = Icons.Default.CalendarViewMonth,
        verticalAlignment = Alignment.TopCenter
    ),
    CoachMarkStep(
        emoji = "🖼️🔔",
        title = "Wallpaper Export, Themes & Reminders",
        description = "Open the top-left Hamburger Menu to export your schedule as a Lock Screen Wallpaper (PNG) or print-ready PDF, customize cute pastel themes, and set multiple reminders!",
        hintBadge = "Open the ☰ Hamburger Menu anytime",
        icon = Icons.Default.PhotoCamera,
        verticalAlignment = Alignment.BottomCenter
    )
)

@Composable
fun OnboardingCoachMarkOverlay(
    onComplete: () -> Unit
) {
    var currentStepIndex by remember { mutableIntStateOf(0) }
    val step = TUTORIAL_STEPS[currentStepIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .padding(24.dp),
        contentAlignment = step.verticalAlignment
    ) {
        AnimatedContent(
            targetState = currentStepIndex,
            label = "OnboardingStepTransition"
        ) { index ->
            val item = TUTORIAL_STEPS[index]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 36.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Progress Row + Skip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Quick Tour • ${index + 1} of ${TUTORIAL_STEPS.size}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }

                        TextButton(onClick = onComplete) {
                            Text("Skip Tour")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Icon Circle
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "${item.emoji} ${item.title}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = "💡 ${item.hintBadge}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Step indicator dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TUTORIAL_STEPS.indices.forEach { dotIdx ->
                            val active = dotIdx == index
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(if (active) 24.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (active) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Navigation Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (index > 0) {
                            OutlinedButton(
                                onClick = { currentStepIndex = index - 1 },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Back")
                            }
                        }

                        Button(
                            onClick = {
                                if (index < TUTORIAL_STEPS.lastIndex) {
                                    currentStepIndex = index + 1
                                } else {
                                    onComplete()
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = if (index < TUTORIAL_STEPS.lastIndex) "Next ✨" else "Let's Go! 🎉",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
