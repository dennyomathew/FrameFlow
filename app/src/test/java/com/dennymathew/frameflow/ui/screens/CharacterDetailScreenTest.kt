package com.dennymathew.frameflow.ui.screens

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dennymathew.frameflow.data.local.ImageDatabase
import com.dennymathew.frameflow.data.repository.CharacterRepository
import com.dennymathew.frameflow.data.repository.FavoritesRepository
import com.dennymathew.frameflow.testutil.FakeRickAndMortyApi
import com.dennymathew.frameflow.testutil.characterDto
import com.dennymathew.frameflow.testutil.inMemoryDatabase
import com.dennymathew.frameflow.ui.CharacterDetailViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CharacterDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: ImageDatabase
    private val api = FakeRickAndMortyApi()
    private var backClicks = 0

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        api.onGetCharacterById = { characterDto(it, name = "Rick Sanchez") }
        val viewModel = CharacterDetailViewModel(
            CharacterRepository(api, database),
            FavoritesRepository(database)
        )
        composeRule.setContent {
            CharacterDetailScreen(
                characterId = 1,
                onBackClick = { backClicks++ },
                viewModel = viewModel
            )
        }
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("Rick Sanchez").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun topBar_hasBackArrowInsteadOfText() {
        composeRule.onNodeWithContentDescription("Back").assertExists()
        composeRule.onNodeWithText("Back").assertDoesNotExist()
    }

    @Test
    fun tappingBackArrow_navigatesBack() {
        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backClicks)
    }
}
