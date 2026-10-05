package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.QuizQuestion
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import com.example.ui.viewmodel.QuizState
import com.example.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    viewModel: StudyViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quizState by viewModel.quizState.collectAsStateWithLifecycle()
    var customTopicInput by remember { mutableStateOf("") }

    val presetTopics = listOf(
        "Biology: Genetics & DNA Replication",
        "Physics: Newton's Laws & Momentum",
        "Computer Science: Algorithms & Data Structures",
        "Chemistry: Acids, Bases & pH Calculations",
        "World History: The Industrial Revolution"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (quizState.questions.isNotEmpty() && !quizState.isFinished) quizState.topic else "AI Quiz Generator",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (quizState.questions.isNotEmpty() && !quizState.isFinished) {
                                viewModel.resetQuiz()
                            } else {
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.testTag("quiz_back_button")
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                quizState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Crafting adaptive quiz questions...",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Evaluating key concepts and distractors with Gemini AI",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                quizState.isFinished -> {
                    QuizResultsView(
                        state = quizState,
                        onRetake = { viewModel.startAiQuiz(quizState.topic) },
                        onNewQuiz = { viewModel.resetQuiz() }
                    )
                }

                quizState.questions.isNotEmpty() -> {
                    QuizRunnerView(
                        state = quizState,
                        onSelectOption = { viewModel.selectQuizOption(it) },
                        onConfirmAnswer = { viewModel.confirmQuizAnswer() },
                        onNextQuestion = { viewModel.nextQuizQuestion() }
                    )
                }

                else -> {
                    // Quiz Launcher / Topic Selector
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Test Your Knowledge",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Generate instant multiple-choice exams on any subject with real-time feedback and detailed step-by-step explanations.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Create Custom Quiz",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = customTopicInput,
                                    onValueChange = { customTopicInput = it },
                                    placeholder = { Text("e.g. Cellular Mitosis, Macroeconomics") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("quiz_topic_input"),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Button(
                                    onClick = {
                                        if (customTopicInput.isNotBlank()) {
                                            viewModel.startAiQuiz(customTopicInput.trim())
                                        }
                                    },
                                    enabled = customTopicInput.isNotBlank(),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .height(56.dp)
                                        .testTag("quiz_start_custom_button")
                                ) {
                                    Text("Start")
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Or Choose a High-Yield Subject",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        itemsIndexed(presetTopics) { index, topic ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.startAiQuiz(topic) }
                                    .testTag("preset_quiz_$index")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${index + 1}",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = topic,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Text(
                                            text = "5 adaptive questions • Instant explanations",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuizRunnerView(
    state: QuizState,
    onSelectOption: (Int) -> Unit,
    onConfirmAnswer: () -> Unit,
    onNextQuestion: () -> Unit
) {
    val totalQuestions = state.questions.size
    val currentQuestion = state.questions.getOrNull(state.currentIndex) ?: return
    val letters = listOf("A", "B", "C", "D")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Progress Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Question ${state.currentIndex + 1} of $totalQuestions",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Score: ${state.correctAnswersCount}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = {
                ((state.currentIndex + 1).toFloat() / totalQuestions.toFloat()).coerceIn(0f, 1f)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Question Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = currentQuestion.question,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 24.sp
                        ),
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }

            // Options List
            itemsIndexed(currentQuestion.options) { index, optionText ->
                val isSelected = state.selectedOptionIndex == index
                val isConfirmed = state.isAnswerConfirmed
                val isCorrectAnswer = index == currentQuestion.correctIndex

                val containerColor = when {
                    isConfirmed && isCorrectAnswer -> EmeraldGreen.copy(alpha = 0.2f)
                    isConfirmed && isSelected && !isCorrectAnswer -> RoseRed.copy(alpha = 0.2f)
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surface
                }

                val borderColor = when {
                    isConfirmed && isCorrectAnswer -> EmeraldGreen
                    isConfirmed && isSelected && !isCorrectAnswer -> RoseRed
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = containerColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected || (isConfirmed && isCorrectAnswer)) 2.dp else 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable(enabled = !isConfirmed) {
                            onSelectOption(index)
                        }
                        .testTag("quiz_option_$index")
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = letters.getOrElse(index) { "?" },
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = optionText,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )

                        if (isConfirmed) {
                            if (isCorrectAnswer) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Correct",
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = "Incorrect",
                                    tint = RoseRed,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Explanation Card (Visible once answer is confirmed)
            if (state.isAnswerConfirmed) {
                item {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically()
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (state.selectedOptionIndex == currentQuestion.correctIndex) {
                                    EmeraldGreen.copy(alpha = 0.1f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Explanation & Concept",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = currentQuestion.explanation,
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Button Bar
        if (!state.isAnswerConfirmed) {
            Button(
                onClick = onConfirmAnswer,
                enabled = state.selectedOptionIndex != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("quiz_confirm_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Confirm Answer", style = MaterialTheme.typography.labelLarge)
            }
        } else {
            Button(
                onClick = onNextQuestion,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("quiz_next_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (state.currentIndex + 1 >= totalQuestions) "View Results" else "Next Question →",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun QuizResultsView(
    state: QuizState,
    onRetake: () -> Unit,
    onNewQuiz: () -> Unit
) {
    val total = state.questions.size
    val correct = state.correctAnswersCount
    val scorePercent = if (total > 0) (correct * 100) / total else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = if (scorePercent >= 70) EmeraldGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(110.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$scorePercent%",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (scorePercent >= 70) EmeraldGreen else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$correct of $total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = when {
                scorePercent >= 90 -> "🌟 Mastery Achieved!"
                scorePercent >= 70 -> "👏 Great Work!"
                else -> "📚 Good Practice! Keep Drilling"
            },
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Completed quiz on '${state.topic}'. Your answers and progress have been recorded in your study statistics.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRetake,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retake Quiz")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onNewQuiz,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Explore Other Topics")
        }
    }
}
