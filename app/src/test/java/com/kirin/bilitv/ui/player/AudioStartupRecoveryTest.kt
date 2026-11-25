package com.kirin.bilitv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioStartupRecoveryTest {
  @Test fun stalledResumePositionCanRestartAudioOnlyOnce() {
    val recovery = AudioStartupRecovery(stallMs = 3_000, trackDisableTimeoutMs = 2_000)
    assertFalse(recovery.shouldRestart(100, 142_013, eligible = true))
    assertFalse(recovery.shouldRestart(3_099, 142_013, eligible = true))
    assertTrue(recovery.shouldRestart(3_100, 142_013, eligible = true))
    recovery.interrupt()
    assertFalse(recovery.shouldRestart(6_100, 142_013, eligible = true))
  }

  @Test fun timeBeforeVideoAndBeforeResumeDoesNotCount() {
    val recovery = AudioStartupRecovery(stallMs = 3_000, trackDisableTimeoutMs = 2_000)
    assertFalse(recovery.shouldRestart(0, 0, eligible = false))
    assertFalse(recovery.shouldRestart(5_000, 0, eligible = true))
    recovery.interrupt()
    assertFalse(recovery.shouldRestart(20_000, 0, eligible = true))
    assertTrue(recovery.shouldRestart(23_000, 0, eligible = true))
  }

  @Test fun changedPlaybackPositionStartsANewObservation() {
    val recovery = AudioStartupRecovery(stallMs = 3_000, trackDisableTimeoutMs = 2_000)
    assertFalse(recovery.shouldRestart(0, 142_013, eligible = true))
    assertFalse(recovery.shouldRestart(3_000, 190_000, eligible = true))
    assertFalse(recovery.shouldRestart(5_999, 190_000, eligible = true))
    assertTrue(recovery.shouldRestart(6_000, 190_000, eligible = true))
  }

  @Test fun advancingAudioDisarmsStartupRecovery() {
    val recovery = AudioStartupRecovery(stallMs = 3_000, trackDisableTimeoutMs = 2_000)
    assertFalse(recovery.shouldRestart(0, 142_013, eligible = true))
    recovery.onAudioPositionAdvancing()
    assertFalse(recovery.shouldRestart(3_000, 142_013, eligible = true))
    recovery.interrupt()
    assertFalse(recovery.shouldRestart(6_000, 150_000, eligible = true))
    assertNull(recovery.restoreReason(6_000, audioSelected = false, keepWaiting = true))
  }

  @Test fun audioIsReenabledOnceAfterTheDisabledTracksArePublished() {
    val recovery = AudioStartupRecovery(stallMs = 3_000, trackDisableTimeoutMs = 2_000)
    assertFalse(recovery.shouldRestart(0, 329_700, eligible = true))
    assertTrue(recovery.shouldRestart(3_000, 329_700, eligible = true))
    assertNull(recovery.restoreReason(3_100, audioSelected = true, keepWaiting = true))
    // A late callback from the previous audio output cannot cancel re-enabling the track.
    recovery.onAudioPositionAdvancing()
    assertEquals(AudioRestoreReason.TRACKS_DISABLED,
      recovery.restoreReason(3_150, audioSelected = false, keepWaiting = true))
    assertNull(recovery.restoreReason(5_000, audioSelected = false, keepWaiting = true))
    assertFalse(recovery.shouldRestart(6_000, 329_700, eligible = true))
  }

  @Test fun missingDisableConfirmationTimesOutAndReenablesAudio() {
    val recovery = AudioStartupRecovery(stallMs = 3_000, trackDisableTimeoutMs = 2_000)
    recovery.shouldRestart(0, 329_700, eligible = true)
    assertTrue(recovery.shouldRestart(3_000, 329_700, eligible = true))
    assertNull(recovery.restoreReason(4_999, audioSelected = true, keepWaiting = true))
    assertEquals(AudioRestoreReason.TIMEOUT, recovery.restoreReason(5_000, audioSelected = true, keepWaiting = true))
    assertNull(recovery.restoreReason(5_001, audioSelected = false, keepWaiting = true))
  }

  @Test fun pausingReenablesAudioWithoutWaitingForTheDisableDeadline() {
    val recovery = AudioStartupRecovery(stallMs = 3_000, trackDisableTimeoutMs = 2_000)
    recovery.shouldRestart(0, 329_700, eligible = true)
    assertTrue(recovery.shouldRestart(3_000, 329_700, eligible = true))
    recovery.interrupt()
    assertEquals(AudioRestoreReason.INTERRUPTED,
      recovery.restoreReason(3_001, audioSelected = true, keepWaiting = false))
    assertNull(recovery.restoreReason(3_002, audioSelected = false, keepWaiting = true))
    assertFalse(recovery.shouldRestart(6_000, 329_700, eligible = true))
  }
}
