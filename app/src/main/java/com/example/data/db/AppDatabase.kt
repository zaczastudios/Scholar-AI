package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.DeckEntity
import com.example.data.model.FlashcardEntity
import com.example.data.model.StudyNoteEntity
import com.example.data.model.StudyPlanEntity
import com.example.data.model.StudyPlanItemEntity
import com.example.data.model.StudySessionEntity

@Database(
    entities = [
        DeckEntity::class,
        FlashcardEntity::class,
        StudyNoteEntity::class,
        StudyPlanEntity::class,
        StudyPlanItemEntity::class,
        StudySessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun studyNoteDao(): StudyNoteDao
    abstract fun studyPlanDao(): StudyPlanDao
    abstract fun studySessionDao(): StudySessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "studymind_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
