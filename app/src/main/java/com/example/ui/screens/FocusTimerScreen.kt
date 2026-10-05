package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.Indigo40
import com.example.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTimerScreen(
    viewModel: StudyViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pomodoroState by viewModel.pomodoroState.collectAsStateWithLifecycle()

    val minutes = pomodoroState.remainingSeconds / 60
    val seconds = pomodoroState.remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    val progress = if (pomodoroState.totalSeconds > 0) {
        (pomodoroState.totalSeconds - pomodoroState.remainingSeconds).toFloat() / pomodoroState.totalSeconds.toFloat()
    } else 0f

    val subjects = listOf("General", "Math & Physics", "Biology", "Computer Science", "History")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Focus Pomodoro",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("focus_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Mode & Subject Selectors
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (pomodoroState.isBreak) EmeraldGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (pomodoroState.isBreak) "☕ Rest & Refresh Break" else "🎯 Deep Focus Study Sprint",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (pomodoroState.isBreak) EmeraldGreen else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Duration Selector Chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15 to "15m", 25 to "25m", 45 to "45m", 60 to "60m").forEach { (mins, label) ->
                        FilterChip(
                            selected = pomodoroState.totalSeconds == mins * 60,
                            onClick = { viewModel.setTimerDuration(mins) },
                            label = { Text(label) },
                            enabled = !pomodoroState.isRunning
                        )
                    }
                }
            }

            // Big Timer Display
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(260.dp)
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 12.dp,
                    color = if (pomodoroState.isBreak) EmeraldGreen else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 54.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = pomodoroState.subject,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Bottom Controls & Stats
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Subject Picker row
                Text(
                    text = "Select Study Subject",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    subjects.take(4).forEach { subj ->
                        SuggestionChip(
                            onClick = { viewModel.setTimerSubject(subj) },
                            label = { Text(subj, style = MaterialTheme.typography.labelSmall) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (pomodoroState.subject == subj) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }

                // Control Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.resetTimer() },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("focus_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Timer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            if (pomodoroState.isRunning) viewModel.pauseTimer() else viewModel.startTimer()
                        },
                        shape = CircleShape,
                        modifier = Modifier
                            .size(80.dp)
                            .testTag("focus_play_pause_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (pomodoroState.isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (pomodoroState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (pomodoroState.isRunning) "Pause" else "Start",
                            modifier = Modifier.size(36.dp),
                            tint = Color.White
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${pomodoroState.completedSessions}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "done",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
