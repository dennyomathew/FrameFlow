package com.dennymathew.frameflow.ui.screens

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import coil.Coil
import coil.ImageLoader
import coil.decode.DataSource
import coil.intercept.Interceptor
import coil.request.ErrorResult
import coil.request.ImageResult
import coil.request.SuccessResult
import com.dennymathew.frameflow.data.local.CharacterEntity
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CharacterImageTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** Image requests made so far; the first [failures] fail (like a 429 after all retries). */
    private val requests = AtomicInteger(0)
    private var failures = 1

    private val character = CharacterEntity(
        id = 1, name = "Rick Sanchez", status = "Alive", species = "Human",
        imageUrl = "https://example.com/1.jpeg"
    )

    @Before
    fun setUp() {
        // Answers every request in an interceptor, so nothing touches the network.
        val interceptor = object : Interceptor {
            override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
                val request = chain.request
                return if (requests.incrementAndGet() <= failures) {
                    ErrorResult(null, request, IOException("HTTP 429"))
                } else {
                    SuccessResult(ColorDrawable(Color.RED), request, DataSource.NETWORK)
                }
            }
        }
        Coil.setImageLoader(
            ImageLoader.Builder(ApplicationProvider.getApplicationContext())
                .components { add(interceptor) }
                .build()
        )
    }

    @After
    fun tearDown() {
        Coil.reset()
    }

    private fun waitForText(text: String) = composeRule.waitUntil {
        composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    private fun waitForImage() = composeRule.waitUntil {
        composeRule.onAllNodesWithContentDescription("Rick Sanchez").fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun failedImage_retryRequestsItAgainWithoutOpeningTheCharacter() {
        var cardClicks = 0
        composeRule.setContent {
            CharacterCard(
                character = character,
                isFavorite = false,
                onClick = { cardClicks++ },
                onToggleFavorite = {}
            )
        }
        waitForText("Retry")
        composeRule.onNodeWithText("Couldn't load image").assertExists()
        assertEquals(1, requests.get())

        composeRule.onNodeWithText("Retry").performClick()
        waitForImage()

        assertEquals(2, requests.get())
        composeRule.onNodeWithText("Retry").assertDoesNotExist()
        assertEquals(0, cardClicks)
    }

    @Test
    fun retryThatFailsAgain_keepsOfferingRetry() {
        failures = 2
        composeRule.setContent {
            CharacterImage(url = character.imageUrl, contentDescription = character.name)
        }
        waitForText("Retry")

        composeRule.onNodeWithText("Retry").performClick()
        composeRule.waitUntil { requests.get() == 2 }
        waitForText("Retry")

        composeRule.onNodeWithText("Retry").performClick()
        waitForImage()
        assertEquals(3, requests.get())
    }
}
