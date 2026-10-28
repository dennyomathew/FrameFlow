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
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.dennymathew.frameflow.testutil.pageResponse
import org.junit.Assert.assertEquals
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetworkCapabilities
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
    private val api = FakeRickAndMortyApi()
    private lateinit var viewModel: CharacterViewModel

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        api.onGetCharacters = { _, _ -> pageResponse(1..20, hasNext = false) }
        viewModel = newViewModel()
    }

    private fun newViewModel() = CharacterViewModel(
        CharacterRepository(api, database),
        FavoritesRepository(database),
        ConnectivityObserver(ApplicationProvider.getApplicationContext())
    )

    /** Makes the device's active network validated (online) or not (offline). */
    private fun setOnline(online: Boolean) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val capabilities = ShadowNetworkCapabilities.newInstance()
        if (online) {
            shadowOf(capabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            shadowOf(capabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
        shadowOf(connectivityManager).setNetworkCapabilities(connectivityManager.activeNetwork, capabilities)
    }

    /** Shows the grid, waits for the first page, then pulls to refresh. Returns the API requests made by the pull. */
    private fun pullToRefreshRequests(): Int {
        composeRule.setContent {
            CharacterGridScreen(viewModel = viewModel, onCharacterClick = {})
        }
        composeRule.waitUntil { composeRule.onAllNodesWithText("Character 1").fetchSemanticsNodes().isNotEmpty() }
        composeRule.waitForIdle()
        val before = api.characterPageRequests.size

        composeRule.onNodeWithText("Character 1").performTouchInput { swipeDown(endY = bottom * 3) }
        composeRule.waitForIdle()

        return api.characterPageRequests.size - before
    }

    @Test
    fun pullToRefresh_online_refreshes() {
        setOnline(true)
        viewModel = newViewModel()

        assertEquals(1, pullToRefreshRequests())
    }

    @Test
    fun pullToRefresh_offline_isDisabled() {
        setOnline(false)
        viewModel = newViewModel()

        assertEquals(0, pullToRefreshRequests())
        composeRule.onNodeWithText("Offline: showing saved characters").assertExists()
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
