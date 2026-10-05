package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subject: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    val front: String,
    val back: String,
    val hint: String = "",
    val isMastered: Boolean = false,
    val reviewCount: Int = 0
)

@Entity(tableName = "study_notes")
data class StudyNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subject: String,
    val originalText: String,
    val summaryBullets: String, // Stored as newline or JSON delimited
    val keyTerms: String,        // Key definitions
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_plans")
data class StudyPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subject: String,
    val goalDescription: String,
    val targetDays: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_plan_items")
data class StudyPlanItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val dayNumber: Int,
    val taskTitle: String,
    val taskDescription: String,
    val isCompleted: Boolean = false
)

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String, // "Tutor", "Flashcards", "Quiz", "Timer"
    val subject: String,
    val durationMinutes: Int,
    val score: Int = 0, // e.g., quiz score percentage
    val timestamp: Long = System.currentTimeMillis()
)

// Non-entity UI models
data class DeckWithCardCount(
    val deck: DeckEntity,
    val totalCards: Int,
    val masteredCards: Int
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isGenerating: Boolean = false
)

enum class MessageSender {
    USER, AI
}

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class GeneratedFlashcard(
    val front: String,
    val back: String,
    val hint: String = ""
)

data class GeneratedStudyPlan(
    val title: String,
    val subject: String,
    val goal: String,
    val items: List<GeneratedPlanDay>
)

data class GeneratedPlanDay(
    val dayNumber: Int,
    val taskTitle: String,
    val taskDescription: String
)
