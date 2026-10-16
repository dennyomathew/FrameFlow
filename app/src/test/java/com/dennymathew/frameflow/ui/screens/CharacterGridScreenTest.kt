package com.dennymathew.frameflow.ui.screens

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.dennymathew.frameflow.data.local.FavoriteEntity
import com.dennymathew.frameflow.data.local.ImageDatabase
import kotlinx.coroutines.runBlocking
import androidx.compose.ui.test.onAllNodesWithText
import com.dennymathew.frameflow.data.network.ConnectivityObserver
import com.dennymathew.frameflow.data.repository.CharacterRepository
import com.dennymathew.frameflow.data.repository.FavoritesRepository
import com.dennymathew.frameflow.testutil.FakeRickAndMortyApi
import com.dennymathew.frameflow.testutil.inMemoryDatabase
import com.dennymathew.frameflow.ui.CharacterViewModel
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CharacterGridScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: ImageDatabase
    private lateinit var viewModel: CharacterViewModel

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        val api = FakeRickAndMortyApi()
        viewModel = CharacterViewModel(
            CharacterRepository(api, database),
            FavoritesRepository(database),
            ConnectivityObserver(ApplicationProvider.getApplicationContext())
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun statusFilter_narrowsFavoritesAndSurvivesSaveAndRestore() {
        runBlocking {
            database.favoriteDao.insert(favorite(1, "Rick Sanchez", status = "Alive"))
            database.favoriteDao.insert(favorite(2, "Birdperson", status = "Dead"))
        }
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            CharacterGridScreen(viewModel = viewModel, onCharacterClick = {})
        }
        composeRule.onNodeWithText("Favorites").performClick()
        composeRule.waitUntil { composeRule.onAllNodesWithText("Birdperson").fetchSemanticsNodes().isNotEmpty() }

        composeRule.onNodeWithText("Status").performClick()
        composeRule.onNodeWithText("Dead").performClick()

        composeRule.onNodeWithText("Birdperson").assertExists()
        composeRule.onNodeWithText("Rick Sanchez").assertDoesNotExist()

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("Dead").assertIsSelected()
        composeRule.onNodeWithText("Rick Sanchez").assertDoesNotExist()

        composeRule.onNodeWithText("Clear filters").performClick()
        composeRule.onNodeWithText("Rick Sanchez").assertExists()
    }

    private fun favorite(id: Int, name: String, status: String) = FavoriteEntity(
        characterId = id, name = name, status = status, species = "Human",
        imageUrl = "https://example.com/$id.jpeg", addedAtMillis = id.toLong()
    )

    @Test
    fun favoritesTab_survivesSaveAndRestore() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            CharacterGridScreen(viewModel = viewModel, onCharacterClick = {})
        }
        composeRule.onNodeWithText("Favorites").performClick()
        composeRule.onNodeWithText("No favorites yet. Tap ♡ on a character to save it.")
            .assertExists()

        // What happens when Android reclaims the app in the background.
        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("Favorites").assertIsSelected()
        composeRule.onNodeWithText("No favorites yet. Tap ♡ on a character to save it.")
            .assertExists()
    }
}
