package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.StudyViewModel

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Tutor : Screen("tutor", "AI Tutor", Icons.Filled.Psychology, Icons.Outlined.Psychology)
    object Flashcards : Screen("flashcards", "Cards", Icons.Filled.Style, Icons.Outlined.Style)
    object Quiz : Screen("quiz", "Quiz", Icons.Filled.Quiz, Icons.Outlined.Quiz)
    object Planner : Screen("planner", "Plans", Icons.Filled.AutoStories, Icons.Outlined.AutoStories)
    object Focus : Screen("focus", "Focus", Icons.Filled.Timer, Icons.Outlined.Timer)
}

@Composable
fun StudyMindApp(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val feedbackMessage by viewModel.actionFeedback.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    // Handle back button when inside a secondary screen
    if (currentScreen != Screen.Home) {
        BackHandler {
            currentScreen = Screen.Home
        }
    }

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Tutor,
        Screen.Flashcards,
        Screen.Quiz,
        Screen.Planner
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                bottomNavItems.forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        when (currentScreen) {
            Screen.Home -> HomeScreen(
                viewModel = viewModel,
                onNavigateToTutor = { currentScreen = Screen.Tutor },
                onNavigateToFlashcards = { currentScreen = Screen.Flashcards },
                onNavigateToQuiz = { currentScreen = Screen.Quiz },
                onNavigateToPlanner = { currentScreen = Screen.Planner },
                onNavigateToFocus = { currentScreen = Screen.Focus },
                modifier = Modifier.padding(innerPadding)
            )
            Screen.Tutor -> TutorScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = Screen.Home },
                modifier = Modifier.padding(innerPadding)
            )
            Screen.Flashcards -> FlashcardsScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = Screen.Home },
                modifier = Modifier.padding(innerPadding)
            )
            Screen.Quiz -> QuizScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = Screen.Home },
                modifier = Modifier.padding(innerPadding)
            )
            Screen.Planner -> PlanAndNotesScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = Screen.Home },
                modifier = Modifier.padding(innerPadding)
            )
            Screen.Focus -> FocusTimerScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = Screen.Home },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
