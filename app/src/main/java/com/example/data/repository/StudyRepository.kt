package com.example.data.repository

import com.example.data.api.GeminiStudyService
import com.example.data.db.AppDatabase
import com.example.data.model.DeckEntity
import com.example.data.model.DeckWithCardCount
import com.example.data.model.FlashcardEntity
import com.example.data.model.GeneratedFlashcard
import com.example.data.model.GeneratedStudyPlan
import com.example.data.model.QuizQuestion
import com.example.data.model.StudyNoteEntity
import com.example.data.model.StudyPlanEntity
import com.example.data.model.StudyPlanItemEntity
import com.example.data.model.StudySessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull

class StudyRepository(
    private val database: AppDatabase,
    private val geminiService: GeminiStudyService = GeminiStudyService()
) {
    private val deckDao = database.deckDao()
    private val flashcardDao = database.flashcardDao()
    private val studyNoteDao = database.studyNoteDao()
    private val studyPlanDao = database.studyPlanDao()
    private val studySessionDao = database.studySessionDao()

    val allDecks: Flow<List<DeckEntity>> = deckDao.getAllDecks()
    val allNotes: Flow<List<StudyNoteEntity>> = studyNoteDao.getAllNotes()
    val allPlans: Flow<List<StudyPlanEntity>> = studyPlanDao.getAllPlans()
    val totalStudyMinutes: Flow<Int?> = studySessionDao.getTotalStudyMinutes()
    val totalCardsCount: Flow<Int> = flashcardDao.getTotalCardCount()
    val masteredCardsCount: Flow<Int> = flashcardDao.getMasteredCardCount()
    val completedQuizCount: Flow<Int> = studySessionDao.getCompletedQuizCount()

    fun getCardsForDeck(deckId: Long): Flow<List<FlashcardEntity>> = flashcardDao.getCardsForDeck(deckId)
    fun getPlanItems(planId: Long): Flow<List<StudyPlanItemEntity>> = studyPlanDao.getPlanItems(planId)

    suspend fun toggleCardMastery(cardId: Long, isMastered: Boolean) {
        flashcardDao.updateMastery(cardId, isMastered)
    }

    suspend fun resetDeckMastery(deckId: Long) {
        flashcardDao.resetDeckMastery(deckId)
    }

    suspend fun deleteDeck(deckId: Long) {
        flashcardDao.deleteCardsForDeck(deckId)
        deckDao.deleteDeckById(deckId)
    }

    suspend fun deleteNote(noteId: Long) {
        studyNoteDao.deleteNoteById(noteId)
    }

    suspend fun togglePlanItem(itemId: Long, completed: Boolean) {
        studyPlanDao.togglePlanItem(itemId, completed)
    }

    suspend fun deletePlan(planId: Long) {
        studyPlanDao.deletePlanItems(planId)
        studyPlanDao.deletePlan(planId)
    }

    suspend fun logSession(mode: String, subject: String, durationMinutes: Int, score: Int = 0) {
        studySessionDao.insertSession(
            StudySessionEntity(
                mode = mode,
                subject = subject,
                durationMinutes = durationMinutes,
                score = score
            )
        )
    }

    suspend fun askAiTutor(
        userMessage: String,
        persona: String,
        subject: String,
        conversationHistory: List<Pair<String, String>>
    ): String {
        return geminiService.askTutor(userMessage, persona, subject, conversationHistory)
    }

    suspend fun createDeckWithAi(title: String, subject: String, topicDescription: String, cardCount: Int = 6): Long {
        val cards = geminiService.generateFlashcards(topicDescription.ifBlank { title }, cardCount)
        val deckId = deckDao.insertDeck(
            DeckEntity(
                title = title.ifBlank { "Study Deck: $topicDescription" },
                subject = subject.ifBlank { "General" },
                description = topicDescription
            )
        )

        val cardEntities = cards.map {
            FlashcardEntity(
                deckId = deckId,
                front = it.front,
                back = it.back,
                hint = it.hint
            )
        }
        flashcardDao.insertFlashcards(cardEntities)
        return deckId
    }

    suspend fun generateQuiz(topic: String, count: Int = 5): List<QuizQuestion> {
        return geminiService.generateQuiz(topic, count)
    }

    suspend fun createStudyPlanWithAi(subject: String, goal: String, days: Int): Long {
        val plan = geminiService.generateStudyPlan(subject, goal, days)
        val planId = studyPlanDao.insertPlan(
            StudyPlanEntity(
                title = plan.title,
                subject = plan.subject,
                goalDescription = plan.goal,
                targetDays = days
            )
        )

        val itemEntities = plan.items.map {
            StudyPlanItemEntity(
                planId = planId,
                dayNumber = it.dayNumber,
                taskTitle = it.taskTitle,
                taskDescription = it.taskDescription,
                isCompleted = false
            )
        }
        studyPlanDao.insertPlanItems(itemEntities)
        return planId
    }

    suspend fun createNoteWithAi(title: String, subject: String, rawContent: String): Long {
        val (bullets, terms) = geminiService.summarizeNotes(rawContent, title)
        return studyNoteDao.insertNote(
            StudyNoteEntity(
                title = title,
                subject = subject,
                originalText = rawContent,
                summaryBullets = bullets,
                keyTerms = terms
            )
        )
    }

    /**
     * Seeds initial study decks if the database is empty on first install
     */
    suspend fun seedInitialDataIfEmpty() {
        val currentDecks = deckDao.getAllDecks().firstOrNull() ?: emptyList()
        if (currentDecks.isNotEmpty()) return

        // 1. Biology Deck
        val bioDeckId = deckDao.insertDeck(
            DeckEntity(
                title = "Cellular Respiration & Energy",
                subject = "Biology",
                description = "Master glycolysis, the Krebs cycle, and oxidative phosphorylation ATP synthesis."
            )
        )
        flashcardDao.insertFlashcards(
            listOf(
                FlashcardEntity(
                    deckId = bioDeckId,
                    front = "Where does Glycolysis occur in eukaryotic cells?",
                    back = "In the Cytoplasm (cytosol). It does not require oxygen (anaerobic).",
                    hint = "Fluid outside organelles"
                ),
                FlashcardEntity(
                    deckId = bioDeckId,
                    front = "What is the net yield of ATP from one molecule of glucose in glycolysis?",
                    back = "Net 2 ATP (4 ATP produced minus 2 ATP consumed in investment phase) + 2 NADH.",
                    hint = "Small single-digit number"
                ),
                FlashcardEntity(
                    deckId = bioDeckId,
                    front = "What is the final electron acceptor in the mitochondrial electron transport chain?",
                    back = "Molecular Oxygen (O2), which combines with protons (H+) to form Water (H2O).",
                    hint = "Vital gas we inhale"
                ),
                FlashcardEntity(
                    deckId = bioDeckId,
                    front = "What enzyme synthesizes ATP using proton motive force?",
                    back = "ATP Synthase, acting as a molecular turbine driven by chemiosmosis.",
                    hint = "Names the molecule it synthesizes"
                )
            )
        )

        // 2. Computer Science Deck
        val csDeckId = deckDao.insertDeck(
            DeckEntity(
                title = "Data Structures & Big-O",
                subject = "Computer Science",
                description = "Core algorithmic time complexity, graph traversal, and tree operations."
            )
        )
        flashcardDao.insertFlashcards(
            listOf(
                FlashcardEntity(
                    deckId = csDeckId,
                    front = "What is the average and worst-case time complexity of QuickSort?",
                    back = "Average: O(n log n). Worst-case: O(n²) when the pivot chosen is consistently the smallest or largest element.",
                    hint = "Log-linear vs quadratic"
                ),
                FlashcardEntity(
                    deckId = csDeckId,
                    front = "Which data structure implements FIFO (First-In, First-Out)?",
                    back = "Queue. Useful for Breadth-First Search (BFS) and task scheduling.",
                    hint = "Waiting in line"
                ),
                FlashcardEntity(
                    deckId = csDeckId,
                    front = "What is the lookup time in an ideal Hash Table?",
                    back = "O(1) constant time, assuming an efficient hash function with minimal collisions.",
                    hint = "Instantaneous"
                )
            )
        )

        // 3. Physics / Math Deck
        val physicsDeckId = deckDao.insertDeck(
            DeckEntity(
                title = "Mechanics & Calculus Principles",
                subject = "Physics & Math",
                description = "Newtonian kinematics, conservation of momentum, and derivative definitions."
            )
        )
        flashcardDao.insertFlashcards(
            listOf(
                FlashcardEntity(
                    deckId = physicsDeckId,
                    front = "What is Newton's Second Law of Motion?",
                    back = "F = m · a (Net force equals mass times acceleration), or more fundamentally F = dp/dt.",
                    hint = "Force, mass, acceleration"
                ),
                FlashcardEntity(
                    deckId = physicsDeckId,
                    front = "What does the first derivative of position with respect to time represent?",
                    back = "Velocity (v = dx/dt). The second derivative represents acceleration (a = d²x/dt²).",
                    hint = "Rate of change of displacement"
                ),
                FlashcardEntity(
                    deckId = physicsDeckId,
                    front = "State the Work-Energy Theorem.",
                    back = "The net work done by all forces on an object equals the change in its kinetic energy (W_net = ΔKE).",
                    hint = "Work = change in energy"
                )
            )
        )

        // Initial Seed Study Plan
        val planId = studyPlanDao.insertPlan(
            StudyPlanEntity(
                title = "Midterm Exam Mastery Sprint",
                subject = "General Sciences",
                goalDescription = "Score 95%+ on upcoming semester exams with active spaced recall",
                targetDays = 5
            )
        )
        studyPlanDao.insertPlanItems(
            listOf(
                StudyPlanItemEntity(
                    planId = planId,
                    dayNumber = 1,
                    taskTitle = "Diagnosis & Concept Mapping",
                    taskDescription = "Ask AI Tutor about 3 weakest topics and create flashcard decks.",
                    isCompleted = true
                ),
                StudyPlanItemEntity(
                    planId = planId,
                    dayNumber = 2,
                    taskTitle = "Active Recall Practice",
                    taskDescription = "Complete 20 flashcard reviews and master at least 10 cards.",
                    isCompleted = false
                ),
                StudyPlanItemEntity(
                    planId = planId,
                    dayNumber = 3,
                    taskTitle = "Quiz Simulation Drill",
                    taskDescription = "Take an AI Quiz with 5 questions and achieve >= 80% accuracy.",
                    isCompleted = false
                ),
                StudyPlanItemEntity(
                    planId = planId,
                    dayNumber = 4,
                    taskTitle = "Deep Dive & Pitfall Review",
                    taskDescription = "Use 'Problem Solver' tutor persona to work through tough problems.",
                    isCompleted = false
                ),
                StudyPlanItemEntity(
                    planId = planId,
                    dayNumber = 5,
                    taskTitle = "Final Sprint & Exam Day Confidence",
                    taskDescription = "Do a 25-minute Pomodoro focus session and review summary bullet points.",
                    isCompleted = false
                )
            )
        )

        // Initial Seed Note
        studyNoteDao.insertNote(
            StudyNoteEntity(
                title = "Cellular Bio Synthesis Quick-Notes",
                subject = "Biology",
                originalText = "Cellular respiration converts biochemical energy from nutrients into ATP. Aerobic respiration requires oxygen in order to create ATP. The overall equation is C6H12O6 + 6O2 -> 6CO2 + 6H2O + approx 30-32 ATP.",
                summaryBullets = "• Cellular respiration converts glucose into usable ATP energy\n• Aerobic path yields approximately 30-32 ATP per glucose molecule\n• 3 main phases: Glycolysis, Citric Acid Cycle, and Electron Transport Chain\n• Oxygen acts as terminal electron acceptor",
                keyTerms = "📌 Glycolysis: Cytoplasmic breakdown of glucose into pyruvate\n📌 Chemiosmosis: Movement of ions across semipermeable membrane generating ATP\n📌 Proton Gradient: Drives ATP Synthase rotary motor"
            )
        )

        // Seed initial study session so dashboard stats look great right away
        studySessionDao.insertSession(
            StudySessionEntity(
                mode = "Flashcards",
                subject = "Biology",
                durationMinutes = 25,
                score = 85
            )
        )
    }
}
