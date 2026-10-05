package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.StudyNoteEntity
import com.example.data.model.StudyPlanEntity
import com.example.data.model.StudyPlanItemEntity
import com.example.ui.theme.EmeraldGreen
import com.example.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanAndNotesScreen(
    viewModel: StudyViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val plans by viewModel.allPlans.collectAsStateWithLifecycle()
    val notes by viewModel.allNotes.collectAsStateWithLifecycle()
    val isGlobalLoading by viewModel.isGlobalLoading.collectAsStateWithLifecycle()

    var showCreatePlanDialog by remember { mutableStateOf(false) }
    var showCreateNoteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Roadmaps & Notes",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("plan_notes_back_button")
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (selectedTabIndex == 0) showCreatePlanDialog = true else showCreateNoteDialog = true
                },
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                text = { Text(if (selectedTabIndex == 0) "Create Plan" else "Summarize Note") },
                modifier = Modifier.testTag("plan_notes_create_fab")
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Study Roadmaps (${plans.size})", style = MaterialTheme.typography.titleSmall) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Smart Notes (${notes.size})", style = MaterialTheme.typography.titleSmall) }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (selectedTabIndex == 0) {
                    PlansTabView(
                        plans = plans,
                        viewModel = viewModel
                    )
                } else {
                    NotesTabView(
                        notes = notes,
                        onDeleteNote = { viewModel.deleteNote(it) }
                    )
                }

                if (isGlobalLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Synthesizing with Gemini AI...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlanDialog) {
        CreatePlanDialog(
            onDismiss = { showCreatePlanDialog = false },
            onConfirm = { subject, goal, days ->
                viewModel.createStudyPlanWithAi(subject, goal, days)
                showCreatePlanDialog = false
            }
        )
    }

    if (showCreateNoteDialog) {
        CreateNoteDialog(
            onDismiss = { showCreateNoteDialog = false },
            onConfirm = { title, subject, content ->
                viewModel.createNoteWithAi(title, subject, content)
                showCreateNoteDialog = false
            }
        )
    }
}

@Composable
fun PlansTabView(
    plans: List<StudyPlanEntity>,
    viewModel: StudyViewModel
) {
    val selectedPlanId by viewModel.selectedPlanId.collectAsStateWithLifecycle()
    val planItems by viewModel.planItems.collectAsStateWithLifecycle()

    // Automatically select the first plan if none selected
    LaunchedEffect(plans) {
        if (selectedPlanId == null && plans.isNotEmpty()) {
            viewModel.selectPlan(plans.first().id)
        }
    }

    if (plans.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.EventNote,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Study Roadmaps Yet",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Let Gemini break your exam goal into day-by-day milestones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Plan Selector Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    plans.forEach { plan ->
                        val isSelected = plan.id == selectedPlanId
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectPlan(plan.id) },
                            label = { Text(plan.subject) },
                            leadingIcon = {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // Current Plan Header Card
            val activePlan = plans.find { it.id == selectedPlanId }
            if (activePlan != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activePlan.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                IconButton(
                                    onClick = { viewModel.deletePlan(activePlan.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete Plan",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "🎯 Goal: ${activePlan.goalDescription}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Calculate completion
                            val completedCount = planItems.count { it.isCompleted }
                            val totalItems = planItems.size
                            val progress = if (totalItems > 0) completedCount.toFloat() / totalItems.toFloat() else 0f

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = EmeraldGreen,
                                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$completedCount of $totalItems milestones completed (${(progress * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Daily Milestones & Action Items",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(planItems, key = { it.id }) { item ->
                    PlanItemCard(
                        item = item,
                        onToggle = { completed ->
                            viewModel.togglePlanItem(item.id, completed)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PlanItemCard(
    item: StudyPlanItemEntity,
    onToggle: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!item.isCompleted) }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = item.isCompleted,
                onCheckedChange = { onToggle(it) },
                colors = CheckboxDefaults.colors(checkedColor = EmeraldGreen)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "DAY ${item.dayNumber}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.taskTitle,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.taskDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun NotesTabView(
    notes: List<StudyNoteEntity>,
    onDeleteNote: (Long) -> Unit
) {
    if (notes.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Notes,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Study Notes Yet",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tap 'Summarize Note' to turn long text or lecture slides into bullet summaries & glossaries.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(notes, key = { it.id }) { note ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = note.subject,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDeleteNote(note.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Note",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "AI Key Takeaways",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = note.summaryBullets,
                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (note.keyTerms.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = note.keyTerms,
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreatePlanDialog(
    onDismiss: () -> Unit,
    onConfirm: (subject: String, goal: String, days: Int) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }
    var days by remember { mutableIntStateOf(5) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate AI Study Roadmap") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject or Course") },
                    placeholder = { Text("e.g. Organic Chemistry, AP US History") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("plan_subject_input")
                )

                OutlinedTextField(
                    value = goal,
                    onValueChange = { goal = it },
                    label = { Text("Target Goal or Exam Date") },
                    placeholder = { Text("e.g. Midterm exam in 2 weeks, score 90%+") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("plan_goal_input")
                )

                Text("Timeline Days:", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(3, 5, 7, 14).forEach { d ->
                        FilterChip(
                            selected = days == d,
                            onClick = { days = d },
                            label = { Text("$d days") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(subject, goal, days) },
                enabled = subject.isNotBlank(),
                modifier = Modifier.testTag("plan_confirm_button")
            ) {
                Text("Generate Roadmap")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CreateNoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, subject: String, content: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("General") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI Notes Summarizer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title") },
                    placeholder = { Text("e.g. Chapter 4: Cellular Respiration") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Study Material or Lecture Notes") },
                    placeholder = { Text("Paste textbook paragraphs, lecture transcripts, or notes...") },
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_content_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title.ifBlank { "Study Notes" }, subject, content) },
                enabled = content.isNotBlank(),
                modifier = Modifier.testTag("note_confirm_button")
            ) {
                Text("Summarize & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
