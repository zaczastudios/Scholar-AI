package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DeckEntity
import com.example.data.model.FlashcardEntity
import com.example.data.model.StudyNoteEntity
import com.example.data.model.StudyPlanEntity
import com.example.data.model.StudyPlanItemEntity
import com.example.data.model.StudySessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Query("SELECT * FROM decks ORDER BY createdAt DESC")
    fun getAllDecks(): Flow<List<DeckEntity>>

    @Query("SELECT * FROM decks WHERE id = :id LIMIT 1")
    suspend fun getDeckById(id: Long): DeckEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: DeckEntity): Long

    @Delete
    suspend fun deleteDeck(deck: DeckEntity)

    @Query("DELETE FROM decks WHERE id = :deckId")
    suspend fun deleteDeckById(deckId: Long)
}

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards WHERE deckId = :deckId")
    fun getCardsForDeck(deckId: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId")
    suspend fun getCardsForDeckSync(deckId: Long): List<FlashcardEntity>

    @Query("SELECT COUNT(*) FROM flashcards")
    fun getTotalCardCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM flashcards WHERE isMastered = 1")
    fun getMasteredCardCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(card: FlashcardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(cards: List<FlashcardEntity>)

    @Update
    suspend fun updateFlashcard(card: FlashcardEntity)

    @Query("UPDATE flashcards SET isMastered = :isMastered, reviewCount = reviewCount + 1 WHERE id = :id")
    suspend fun updateMastery(id: Long, isMastered: Boolean)

    @Query("UPDATE flashcards SET isMastered = 0 WHERE deckId = :deckId")
    suspend fun resetDeckMastery(deckId: Long)

    @Query("DELETE FROM flashcards WHERE deckId = :deckId")
    suspend fun deleteCardsForDeck(deckId: Long)

    @Delete
    suspend fun deleteFlashcard(card: FlashcardEntity)
}

@Dao
interface StudyNoteDao {
    @Query("SELECT * FROM study_notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<StudyNoteEntity>>

    @Query("SELECT * FROM study_notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): StudyNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: StudyNoteEntity): Long

    @Delete
    suspend fun deleteNote(note: StudyNoteEntity)

    @Query("DELETE FROM study_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)
}

@Dao
interface StudyPlanDao {
    @Query("SELECT * FROM study_plans ORDER BY createdAt DESC")
    fun getAllPlans(): Flow<List<StudyPlanEntity>>

    @Query("SELECT * FROM study_plan_items WHERE planId = :planId ORDER BY dayNumber ASC")
    fun getPlanItems(planId: Long): Flow<List<StudyPlanItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: StudyPlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanItems(items: List<StudyPlanItemEntity>)

    @Query("UPDATE study_plan_items SET isCompleted = :completed WHERE id = :itemId")
    suspend fun togglePlanItem(itemId: Long, completed: Boolean)

    @Query("DELETE FROM study_plans WHERE id = :planId")
    suspend fun deletePlan(planId: Long)

    @Query("DELETE FROM study_plan_items WHERE planId = :planId")
    suspend fun deletePlanItems(planId: Long)
}

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<StudySessionEntity>>

    @Query("SELECT SUM(durationMinutes) FROM study_sessions")
    fun getTotalStudyMinutes(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM study_sessions WHERE mode = 'Quiz'")
    fun getCompletedQuizCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity): Long
}
