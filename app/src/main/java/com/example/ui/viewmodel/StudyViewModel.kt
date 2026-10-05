package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.DeckEntity
import com.example.data.model.FlashcardEntity
import com.example.data.model.MessageSender
import com.example.data.model.QuizQuestion
import com.example.data.model.StudyNoteEntity
import com.example.data.model.StudyPlanEntity
import com.example.data.model.StudyPlanItemEntity
import com.example.data.repository.StudyRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FlashcardStudyState(
    val deck: DeckEntity? = null,
    val cards: List<FlashcardEntity> = emptyList(),
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    val isFinished: Boolean = false,
    val masteredThisRound: Int = 0
)

data class QuizState(
    val topic: String = "",
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedOptionIndex: Int? = null,
    val isAnswerConfirmed: Boolean = false,
    val correctAnswersCount: Int = 0,
    val isFinished: Boolean = false,
    val isLoading: Boolean = false
)

data class PomodoroState(
    val totalSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val isBreak: Boolean = false,
    val subject: String = "General Study",
    val completedSessions: Int = 0
)

class StudyViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    val repository = StudyRepository(database)

    // Room Flows
    val allDecks: StateFlow<List<DeckEntity>> = repository.allDecks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<StudyNoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlans: StateFlow<List<StudyPlanEntity>> = repository.allPlans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalStudyMinutes: StateFlow<Int?> = repository.totalStudyMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 25)

    val totalCardsCount: StateFlow<Int> = repository.totalCardsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val masteredCardsCount: StateFlow<Int> = repository.masteredCardsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val completedQuizCount: StateFlow<Int> = repository.completedQuizCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    // Active Plan Items
    private val _selectedPlanId = MutableStateFlow<Long?>(null)
    val selectedPlanId = _selectedPlanId.asStateFlow()

    private val _planItems = MutableStateFlow<List<StudyPlanItemEntity>>(emptyList())
    val planItems = _planItems.asStateFlow()

    // AI Tutor State
    private val _tutorPersona = MutableStateFlow("Socratic Tutor")
    val tutorPersona = _tutorPersona.asStateFlow()

    private val _tutorSubject = MutableStateFlow("General")
    val tutorSubject = _tutorSubject.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = MessageSender.AI,
                content = "👋 Hello! I am your **StudyMind AI Tutor**.\n\nAsk me any concept to break down, solve complex problems step-by-step, generate flashcards, or quiz your knowledge. What are we studying today?"
            )
        )
    )
    val chatMessages = _chatMessages.asStateFlow()

    private val _isTutorThinking = MutableStateFlow(false)
    val isTutorThinking = _isTutorThinking.asStateFlow()

    // Flashcard Study Session State
    private val _flashcardState = MutableStateFlow(FlashcardStudyState())
    val flashcardState = _flashcardState.asStateFlow()

    // Quiz State
    private val _quizState = MutableStateFlow(QuizState())
    val quizState = _quizState.asStateFlow()

    // Pomodoro Timer State
    private val _pomodoroState = MutableStateFlow(PomodoroState())
    val pomodoroState = _pomodoroState.asStateFlow()
    private var timerJob: Job? = null

    // General App Status / Notifications
    private val _actionFeedback = MutableStateFlow<String?>(null)
    val actionFeedback = _actionFeedback.asStateFlow()

    private val _isGlobalLoading = MutableStateFlow(false)
    val isGlobalLoading = _isGlobalLoading.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    fun clearFeedback() {
        _actionFeedback.value = null
    }

    // --- AI Tutor Actions ---

    fun setTutorPersona(persona: String) {
        _tutorPersona.value = persona
    }

    fun setTutorSubject(subject: String) {
        _tutorSubject.value = subject
    }

    fun sendTutorMessage(prompt: String) {
        if (prompt.isBlank() || _isTutorThinking.value) return

        val userMsg = ChatMessage(sender = MessageSender.USER, content = prompt.trim())
        _chatMessages.update { it + userMsg }
        _isTutorThinking.value = true

        viewModelScope.launch {
            val history = _chatMessages.value.takeLast(6).map {
                val who = if (it.sender == MessageSender.USER) "Student" else "Tutor"
                Pair(who, it.content)
            }
            val reply = repository.askAiTutor(
                userMessage = prompt.trim(),
                persona = _tutorPersona.value,
                subject = _tutorSubject.value,
                conversationHistory = history
            )
            val aiMsg = ChatMessage(sender = MessageSender.AI, content = reply)
            _chatMessages.update { it + aiMsg }
            _isTutorThinking.value = false

            // Log brief interaction
            repository.logSession("Tutor", _tutorSubject.value, 3)
        }
    }

    fun clearChat() {
        _chatMessages.value = listOf(
            ChatMessage(
                sender = MessageSender.AI,
                content = "🧹 Chat cleared! Ready for your next study topic."
            )
        )
    }

    // --- Flashcards Actions ---

    fun startFlashcardSession(deck: DeckEntity) {
        viewModelScope.launch {
            repository.getCardsForDeck(deck.id).collect { cards ->
                _flashcardState.value = FlashcardStudyState(
                    deck = deck,
                    cards = cards,
                    currentIndex = 0,
                    isFlipped = false,
                    isFinished = cards.isEmpty(),
                    masteredThisRound = 0
                )
            }
        }
    }

    fun flipCard() {
        _flashcardState.update { it.copy(isFlipped = !it.isFlipped) }
    }

    fun markCardMastery(mastered: Boolean) {
        val state = _flashcardState.value
        val currentCard = state.cards.getOrNull(state.currentIndex) ?: return

        viewModelScope.launch {
            repository.toggleCardMastery(currentCard.id, mastered)
        }

        val nextIndex = state.currentIndex + 1
        val newMasteredCount = if (mastered) state.masteredThisRound + 1 else state.masteredThisRound
        val finished = nextIndex >= state.cards.size

        if (finished) {
            viewModelScope.launch {
                repository.logSession("Flashcards", state.deck?.subject ?: "General", 10, (newMasteredCount * 100 / (state.cards.size.coerceAtLeast(1))))
            }
        }

        _flashcardState.update {
            it.copy(
                currentIndex = nextIndex,
                isFlipped = false,
                isFinished = finished,
                masteredThisRound = newMasteredCount
            )
        }
    }

    fun restartFlashcardSession() {
        val deck = _flashcardState.value.deck ?: return
        startFlashcardSession(deck)
    }

    fun createDeckWithAi(title: String, subject: String, topic: String, count: Int = 6) {
        viewModelScope.launch {
            _isGlobalLoading.value = true
            try {
                repository.createDeckWithAi(title, subject, topic, count)
                _actionFeedback.value = "Deck '$title' generated successfully!"
            } catch (e: Exception) {
                _actionFeedback.value = "Error generating deck: ${e.message}"
            } finally {
                _isGlobalLoading.value = false
            }
        }
    }

    fun deleteDeck(deckId: Long) {
        viewModelScope.launch {
            repository.deleteDeck(deckId)
            _actionFeedback.value = "Deck deleted"
        }
    }

    // --- Quiz Actions ---

    fun startAiQuiz(topic: String, count: Int = 5) {
        _quizState.value = QuizState(topic = topic, isLoading = true)
        viewModelScope.launch {
            try {
                val questions = repository.generateQuiz(topic, count)
                _quizState.value = QuizState(
                    topic = topic,
                    questions = questions,
                    currentIndex = 0,
                    selectedOptionIndex = null,
                    isAnswerConfirmed = false,
                    correctAnswersCount = 0,
                    isFinished = false,
                    isLoading = false
                )
            } catch (e: Exception) {
                _quizState.update { it.copy(isLoading = false) }
                _actionFeedback.value = "Failed to create quiz: ${e.message}"
            }
        }
    }

    fun selectQuizOption(index: Int) {
        if (_quizState.value.isAnswerConfirmed) return
        _quizState.update { it.copy(selectedOptionIndex = index) }
    }

    fun confirmQuizAnswer() {
        val state = _quizState.value
        val currentQ = state.questions.getOrNull(state.currentIndex) ?: return
        val selected = state.selectedOptionIndex ?: return

        val isCorrect = selected == currentQ.correctIndex
        val newCorrectCount = if (isCorrect) state.correctAnswersCount + 1 else state.correctAnswersCount

        _quizState.update {
            it.copy(
                isAnswerConfirmed = true,
                correctAnswersCount = newCorrectCount
            )
        }
    }

    fun nextQuizQuestion() {
        val state = _quizState.value
        val nextIdx = state.currentIndex + 1
        val finished = nextIdx >= state.questions.size

        if (finished) {
            val scorePercent = if (state.questions.isNotEmpty()) {
                (state.correctAnswersCount * 100) / state.questions.size
            } else 0

            viewModelScope.launch {
                repository.logSession("Quiz", state.topic.ifBlank { "Quiz" }, 15, scorePercent)
            }
        }

        _quizState.update {
            it.copy(
                currentIndex = nextIdx,
                selectedOptionIndex = null,
                isAnswerConfirmed = false,
                isFinished = finished
            )
        }
    }

    fun resetQuiz() {
        _quizState.value = QuizState()
    }

    // --- Study Plan Actions ---

    fun selectPlan(planId: Long) {
        _selectedPlanId.value = planId
        viewModelScope.launch {
            repository.getPlanItems(planId).collect { items ->
                _planItems.value = items
            }
        }
    }

    fun togglePlanItem(itemId: Long, completed: Boolean) {
        viewModelScope.launch {
            repository.togglePlanItem(itemId, completed)
        }
    }

    fun createStudyPlanWithAi(subject: String, goal: String, days: Int) {
        viewModelScope.launch {
            _isGlobalLoading.value = true
            try {
                val newPlanId = repository.createStudyPlanWithAi(subject, goal, days)
                selectPlan(newPlanId)
                _actionFeedback.value = "Study plan created!"
            } catch (e: Exception) {
                _actionFeedback.value = "Error creating plan: ${e.message}"
            } finally {
                _isGlobalLoading.value = false
            }
        }
    }

    fun deletePlan(planId: Long) {
        viewModelScope.launch {
            repository.deletePlan(planId)
            if (_selectedPlanId.value == planId) {
                _selectedPlanId.value = null
                _planItems.value = emptyList()
            }
            _actionFeedback.value = "Plan removed"
        }
    }

    // --- Notes & Summaries ---

    fun createNoteWithAi(title: String, subject: String, content: String) {
        viewModelScope.launch {
            _isGlobalLoading.value = true
            try {
                repository.createNoteWithAi(title, subject, content)
                _actionFeedback.value = "AI summary and notes generated!"
            } catch (e: Exception) {
                _actionFeedback.value = "Error saving note: ${e.message}"
            } finally {
                _isGlobalLoading.value = false
            }
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
            _actionFeedback.value = "Note deleted"
        }
    }

    // --- Focus / Pomodoro Timer ---

    fun setTimerDuration(minutes: Int) {
        pauseTimer()
        val totalSecs = minutes * 60
        _pomodoroState.update {
            it.copy(
                totalSeconds = totalSecs,
                remainingSeconds = totalSecs,
                isBreak = false
            )
        }
    }

    fun setTimerSubject(subject: String) {
        _pomodoroState.update { it.copy(subject = subject) }
    }

    fun startTimer() {
        if (_pomodoroState.value.isRunning) return
        _pomodoroState.update { it.copy(isRunning = true) }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_pomodoroState.value.remainingSeconds > 0 && _pomodoroState.value.isRunning) {
                delay(1000)
                _pomodoroState.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
            }

            if (_pomodoroState.value.remainingSeconds == 0) {
                onTimerFinished()
            }
        }
    }

    fun pauseTimer() {
        _pomodoroState.update { it.copy(isRunning = false) }
        timerJob?.cancel()
    }

    fun resetTimer() {
        pauseTimer()
        _pomodoroState.update { it.copy(remainingSeconds = it.totalSeconds) }
    }

    private fun onTimerFinished() {
        pauseTimer()
        val currentState = _pomodoroState.value
        if (!currentState.isBreak) {
            // Completed a study block!
            val durationMinutes = (currentState.totalSeconds / 60).coerceAtLeast(1)
            viewModelScope.launch {
                repository.logSession("Timer", currentState.subject, durationMinutes)
            }
            _pomodoroState.update {
                it.copy(
                    completedSessions = it.completedSessions + 1,
                    isBreak = true,
                    totalSeconds = 5 * 60,
                    remainingSeconds = 5 * 60
                )
            }
            _actionFeedback.value = "🎉 Great job! Study session logged. Time for a 5-minute break!"
        } else {
            // Completed a break!
            _pomodoroState.update {
                it.copy(
                    isBreak = false,
                    totalSeconds = 25 * 60,
                    remainingSeconds = 25 * 60
                )
            }
            _actionFeedback.value = "🔔 Break over! Ready for another focused study sprint?"
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
