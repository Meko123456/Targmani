package io.github.meko123456.targmani.ui.translate

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The status line between the two cards. TalkBack's speech can't be observed from a test, so this
 * checks what TalkBack acts on: a download, the wait for Wi-Fi and an error are a polite live
 * region, "Translating…" isn't, and every state is a new wording of the same node.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class StatusLineTest {
    @get:Rule
    val rule = createComposeRule()

    private val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
    private val silent = SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion)

    @Test
    fun downloadsWaitsAndErrorsAreAnnouncedAndTranslatingIsNot() {
        var status by mutableStateOf<TranslateStatus>(TranslateStatus.Idle)
        var waitingForWifi by mutableStateOf(false)
        rule.setContent { StatusLine(status, waitingForWifi) }
        val line = rule.onNodeWithTag(STATUS_LINE_TAG, useUnmergedTree = true)
        line.assertTextEquals("").assert(silent)
        val node = line.fetchSemanticsNode().id

        // Typing: "Translating…" at every pause, kept quiet.
        status = TranslateStatus.Translating
        line.assertTextEquals("Translating…").assert(silent).assertSameNode(node)

        // A model to fetch first, held for Wi-Fi, then downloading: each one said.
        waitingForWifi = true
        status = TranslateStatus.Downloading
        line.assertTextEquals("Waiting for Wi-Fi to download the language model…").assert(polite).assertSameNode(node)
        waitingForWifi = false
        line.assertTextEquals("Downloading language model…").assert(polite).assertSameNode(node)

        status = TranslateStatus.Error("Couldn't download the language model. Check the connection and try again.")
        line.assertTextEquals("Couldn't download the language model. Check the connection and try again.")
            .assert(polite)
            .assertSameNode(node)

        status = TranslateStatus.Idle
        line.assertTextEquals("").assert(silent).assertSameNode(node)
    }

    private fun SemanticsNodeInteraction.assertSameNode(id: Int): SemanticsNodeInteraction {
        assertEquals("the status line is still the same node", id, fetchSemanticsNode().id)
        return this
    }
}
