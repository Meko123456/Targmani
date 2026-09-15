package io.github.meko123456.targmani.ui.translate

import io.github.meko123456.targmani.domain.Language
import io.github.meko123456.targmani.domain.TranslationDirection
import io.github.meko123456.targmani.domain.Translator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TranslateViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    /** Records download calls; translate returns a marker so output is checkable. */
    private class FakeTranslator(var ready: Boolean = true, val failTranslate: Boolean = false) : Translator {
        var downloads = 0
        override suspend fun isReady(direction: TranslationDirection) = ready
        override suspend fun download(direction: TranslationDirection, requireWifi: Boolean): Result<Unit> {
            downloads++; ready = true; return Result.success(Unit)
        }
        override suspend fun translate(text: String, direction: TranslationDirection): Result<String> =
            if (failTranslate) Result.failure(RuntimeException("nope"))
            else Result.success("[${direction.from.code}->${direction.to.code}] $text")
    }

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `swapping before the translation arrives keeps the typed text`() = runTest(dispatcher) {
        // The bug: type, realise the direction is wrong, hit swap before the output appears. The
        // sentence used to be moved into the output field and then cleared outright.
        val vm = TranslateViewModel(FakeTranslator(ready = true))
        vm.onInputChange("gamarjoba")
        vm.swap() // no advanceUntilIdle: still inside the debounce, output is empty

        assertEquals("gamarjoba", vm.state.value.input)
        assertEquals(TranslationDirection(Language.GEORGIAN, Language.ENGLISH), vm.state.value.direction)

        advanceUntilIdle()
        assertEquals("gamarjoba", vm.state.value.input)
        assertEquals("[ka->en] gamarjoba", vm.state.value.output)
    }

    @Test
    fun `swapping while the models are downloading keeps the typed text`() = runTest(dispatcher) {
        val vm = TranslateViewModel(FakeTranslator(ready = false))
        vm.onInputChange("hello there")
        vm.swap()
        advanceUntilIdle()
        assertEquals("hello there", vm.state.value.input)
    }

    @Test
    fun `swapping after a failed translation keeps the typed text`() = runTest(dispatcher) {
        val vm = TranslateViewModel(FakeTranslator(ready = true, failTranslate = true))
        vm.onInputChange("hello")
        advanceUntilIdle()
        assertTrue(vm.state.value.output.isBlank())

        vm.swap()
        advanceUntilIdle()
        assertEquals("hello", vm.state.value.input)
    }

    @Test
    fun `swapping with a translation present still exchanges the two texts`() = runTest(dispatcher) {
        val vm = TranslateViewModel(FakeTranslator(ready = true))
        vm.onInputChange("hello")
        advanceUntilIdle()
        assertEquals("[en->ka] hello", vm.state.value.output)

        vm.swap()
        assertEquals("[en->ka] hello", vm.state.value.input)
        assertEquals("hello", vm.state.value.output)
        assertEquals(TranslationDirection(Language.GEORGIAN, Language.ENGLISH), vm.state.value.direction)
    }

    @Test
    fun `typing translates after the debounce`() = runTest(dispatcher) {
        val vm = TranslateViewModel(FakeTranslator(ready = true))
        vm.onInputChange("hello")
        advanceUntilIdle()
        val s = vm.state.value
        assertEquals("[en->ka] hello", s.output)
        assertEquals(TranslateStatus.Idle, s.status)
    }

    @Test
    fun `a missing model is downloaded first, with a Downloading status, then translated`() = runTest(dispatcher) {
        val fake = FakeTranslator(ready = false)
        val vm = TranslateViewModel(fake)
        vm.onInputChange("hi")
        advanceUntilIdle()
        assertEquals(1, fake.downloads)
        assertEquals("[en->ka] hi", vm.state.value.output)
        assertEquals(TranslateStatus.Idle, vm.state.value.status)
    }

    @Test
    fun `blank input clears the output without translating`() = runTest(dispatcher) {
        val fake = FakeTranslator()
        val vm = TranslateViewModel(fake)
        vm.onInputChange("hello"); advanceUntilIdle()
        vm.onInputChange(""); advanceUntilIdle()
        assertEquals("", vm.state.value.output)
        assertEquals(TranslateStatus.Idle, vm.state.value.status)
    }

    @Test
    fun `swap reverses direction and swaps the text`() = runTest(dispatcher) {
        val vm = TranslateViewModel(FakeTranslator())
        vm.onInputChange("hello"); advanceUntilIdle()
        vm.swap(); advanceUntilIdle()
        val s = vm.state.value
        assertEquals(Language.GEORGIAN, s.direction.from)
        assertEquals(Language.ENGLISH, s.direction.to)
        assertEquals("[en->ka] hello", s.input) // the old output became the new input
    }

    @Test
    fun `choosing a target equal to the source swaps instead of making an invalid pair`() = runTest(dispatcher) {
        val vm = TranslateViewModel(FakeTranslator())
        vm.onTargetLanguage(Language.ENGLISH) // target == source (en); must resolve to a valid pair
        advanceUntilIdle()
        val d = vm.state.value.direction
        assertTrue(d.from != d.to)
    }

    @Test
    fun `a translation failure surfaces an error status`() = runTest(dispatcher) {
        val vm = TranslateViewModel(FakeTranslator(ready = true, failTranslate = true))
        vm.onInputChange("hello"); advanceUntilIdle()
        assertTrue(vm.state.value.status is TranslateStatus.Error)
    }
}
