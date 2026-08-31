package io.github.meko123456.targmani.ui.models

import io.github.meko123456.targmani.domain.Language
import io.github.meko123456.targmani.domain.ModelState
import io.github.meko123456.targmani.domain.ModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ModelsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class FakeStore(
        initial: Set<Language> = emptySet(),
        val failDownload: Boolean = false,
    ) : ModelStore {
        val present = initial.toMutableSet()
        var downloads = 0
        var deletes = 0
        override suspend fun downloaded(): Set<Language> = present.toSet()
        override suspend fun download(language: Language, requireWifi: Boolean): Result<Unit> {
            downloads++
            if (failDownload) return Result.failure(RuntimeException("no network"))
            present += language
            return Result.success(Unit)
        }
        override suspend fun delete(language: Language): Result<Unit> {
            deletes++; present -= language; return Result.success(Unit)
        }
    }

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `rows load with the already-downloaded languages marked`() = runTest(dispatcher) {
        val vm = ModelsViewModel(FakeStore(setOf(Language.ENGLISH)))
        advanceUntilIdle()
        val rows = vm.state.value.rows
        assertEquals(3, rows.size)
        assertEquals(ModelState.DOWNLOADED, rows.first { it.language == Language.ENGLISH }.state)
        assertEquals(ModelState.NOT_DOWNLOADED, rows.first { it.language == Language.GEORGIAN }.state)
    }

    @Test
    fun `downloading a language marks it downloaded afterwards`() = runTest(dispatcher) {
        val store = FakeStore()
        val vm = ModelsViewModel(store)
        advanceUntilIdle()
        vm.download(Language.GEORGIAN)
        advanceUntilIdle()
        assertEquals(1, store.downloads)
        assertEquals(
            ModelState.DOWNLOADED,
            vm.state.value.rows.first { it.language == Language.GEORGIAN }.state,
        )
        assertNull(vm.state.value.error)
    }

    @Test
    fun `deleting a language returns it to not-downloaded`() = runTest(dispatcher) {
        val store = FakeStore(setOf(Language.ENGLISH, Language.GEORGIAN))
        val vm = ModelsViewModel(store)
        advanceUntilIdle()
        vm.delete(Language.GEORGIAN)
        advanceUntilIdle()
        assertEquals(1, store.deletes)
        assertEquals(
            ModelState.NOT_DOWNLOADED,
            vm.state.value.rows.first { it.language == Language.GEORGIAN }.state,
        )
        // the untouched language is still present
        assertEquals(
            ModelState.DOWNLOADED,
            vm.state.value.rows.first { it.language == Language.ENGLISH }.state,
        )
    }

    @Test
    fun `a failed download surfaces an error and leaves the model absent`() = runTest(dispatcher) {
        val vm = ModelsViewModel(FakeStore(failDownload = true))
        advanceUntilIdle()
        vm.download(Language.ARABIC)
        advanceUntilIdle()
        assertNotNull(vm.state.value.error)
        assertEquals(
            ModelState.NOT_DOWNLOADED,
            vm.state.value.rows.first { it.language == Language.ARABIC }.state,
        )
        vm.dismissError()
        assertNull(vm.state.value.error)
    }

    @Test
    fun `a second request while one is in flight is ignored`() = runTest(dispatcher) {
        val store = FakeStore()
        val vm = ModelsViewModel(store)
        advanceUntilIdle()
        vm.download(Language.GEORGIAN)
        vm.download(Language.GEORGIAN) // still busy — must not double-fire
        advanceUntilIdle()
        assertEquals(1, store.downloads)
    }

    @Test
    fun `wifi-only toggle is reflected in state`() = runTest(dispatcher) {
        val vm = ModelsViewModel(FakeStore())
        advanceUntilIdle()
        assertTrue(!vm.state.value.wifiOnly)
        vm.setWifiOnly(true)
        assertTrue(vm.state.value.wifiOnly)
    }
}
