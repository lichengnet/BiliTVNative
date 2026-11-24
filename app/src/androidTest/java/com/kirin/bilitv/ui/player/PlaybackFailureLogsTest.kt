package com.kirin.bilitv.ui.player

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.platform.app.InstrumentationRegistry
import com.kirin.bilitv.core.device.PlaybackLogLevel
import com.kirin.bilitv.core.device.SessionDiagnostics
import com.kirin.bilitv.core.device.SessionEvent
import com.kirin.bilitv.core.player.PlaybackTrace
import com.kirin.bilitv.core.settings.AppSettingsStore
import com.kirin.bilitv.ui.input.InteractionMode
import com.kirin.bilitv.ui.input.LocalInteractionMode
import com.kirin.bilitv.ui.theme.BiliTvTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlaybackFailureLogsTest {
  @get:Rule val rule = createComposeRule()

  @Test fun failureLogReturnsFocusAndPersistsLogLevel() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    val store = AppSettingsStore(context)
    val prior = runBlocking { store.playbackLogLevel.first() }
    val trace = PlaybackTrace(System.nanoTime().toInt(), 1, 2L, 3, "BV1234567890", 42L)
    val identity = mapOf("session" to trace.session.toString(), "bvid" to trace.bvid, "cid" to trace.cid.toString())
    SessionDiagnostics.event(SessionEvent.PLAY_FAILURE, identity + mapOf("code" to "VIDEO_TRACKS", "requestedQn" to "16"))
    repeat(10) { SessionDiagnostics.event(SessionEvent.PLAY_ATTEMPT, identity + ("attempt" to it.toString())) }
    SessionDiagnostics.event(SessionEvent.PLAY_FAILURE, identity + mapOf("cid" to "43", "code" to "OTHER_PART"))
    runBlocking { store.setPlaybackLogLevel(PlaybackLogLevel.Info) }
    try {
      rule.setContent {
        BiliTvTheme {
          CompositionLocalProvider(LocalInteractionMode provides InteractionMode.Tv) {
            PlayerFailureLogActions("VIDEO_TRACKS", store, trace) {}
          }
        }
      }
      val retry = rule.onNodeWithTag("playback_failed_retry")
      val logs = rule.onNodeWithTag("playback_failed_log")
      retry.assertIsFocused().performKeyInput { pressKey(Key.DirectionRight) }
      logs.assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
      rule.onNodeWithTag("playback_log_dialog").assertExists()
      rule.waitUntil(5_000) { rule.onAllNodesWithText("requestedQn=16", substring = true).fetchSemanticsNodes().isNotEmpty() }
      rule.onNodeWithText("requestedQn=16", substring = true).assertIsDisplayed()
      rule.onNodeWithText("OTHER_PART", substring = true).assertDoesNotExist()
      rule.onNodeWithTag("playback_log_level").assertIsFocused().performClick()
      rule.waitUntil(5_000) { runBlocking { store.playbackLogLevel.first() } == PlaybackLogLevel.Debug }
      rule.onNodeWithTag("playback_log_back").performClick()
      logs.assertIsFocused()
      assertEquals(PlaybackLogLevel.Debug, runBlocking { AppSettingsStore(context).playbackLogLevel.first() })
    } finally {
      runBlocking { store.setPlaybackLogLevel(prior) }
    }
  }
}
