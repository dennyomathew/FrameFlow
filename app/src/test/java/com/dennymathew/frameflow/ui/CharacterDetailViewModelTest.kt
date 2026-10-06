package com.dennymathew.frameflow.ui

import com.dennymathew.frameflow.data.local.ImageDatabase
import com.dennymathew.frameflow.data.repository.CharacterRepository
import com.dennymathew.frameflow.testutil.FakeRickAndMortyApi
import com.dennymathew.frameflow.testutil.characterDto
import com.dennymathew.frameflow.testutil.inMemoryDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CharacterDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var database: ImageDatabase
    private val api = FakeRickAndMortyApi()
    private lateinit var viewModel: CharacterDetailViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        database = inMemoryDatabase()
        viewModel = CharacterDetailViewModel(CharacterRepository(api, database))
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun load_publishesCharacterAndClearsLoading() = runTest(dispatcher) {
        api.onGetCharacterById = { characterDto(it, name = "Rick Sanchez") }

        viewModel.load(1)
        advanceUntilIdle()

        assertEquals("Rick Sanchez", viewModel.character.value?.name)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun load_whenUnavailable_leavesCharacterNullAndClearsLoading() = runTest(dispatcher) {
        api.onGetCharacterById = { throw IOException("offline") }

        viewModel.load(1)
        advanceUntilIdle()

        assertNull(viewModel.character.value)
        assertFalse(viewModel.isLoading.value)
    }
}
