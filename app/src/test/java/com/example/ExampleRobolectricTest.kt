package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.example.data.model.DeckEntity
import com.example.data.model.FlashcardEntity
import com.example.data.model.QuizQuestion

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("StudyMind", appName)
  }

  @Test
  fun `verify flashcard entity and quiz models`() {
    val deck = DeckEntity(
        id = 1,
        title = "Biology",
        subject = "Science",
        description = "Test deck"
    )
    assertEquals("Biology", deck.title)

    val card = FlashcardEntity(
        id = 1,
        deckId = 1,
        front = "Question",
        back = "Answer",
        hint = "Hint"
    )
    assertEquals("Question", card.front)

    val quiz = QuizQuestion(
        question = "What is ATP?",
        options = listOf("Energy", "Protein", "Fat", "Carb"),
        correctIndex = 0,
        explanation = "Energy currency"
    )
    assertEquals(4, quiz.options.size)
    assertEquals(0, quiz.correctIndex)
  }
}
