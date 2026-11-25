package com.kirin.bilitv.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioStartupRecoveryTest {
  @Test fun stalledResumePositionCanRestartAudioOnlyOnce() {
    val recovery = AudioStartupRecovery(stallMs = 3_000)
    assertFalse(recovery.shouldRestart(100, 142_013, eligible = true))
    assertFalse(recovery.shouldRestart(3_099, 142_013, eligible = true))
    assertTrue(recovery.shouldRestart(3_100, 142_013, eligible = true))
    recovery.interrupt()
    assertFalse(recovery.shouldRestart(6_100, 142_013, eligible = true))
  }

  @Test fun timeBeforeVideoAndBeforeResumeDoesNotCount() {
    val recovery = AudioStartupRecovery(stallMs = 3_000)
    assertFalse(recovery.shouldRestart(0, 0, eligible = false))
    assertFalse(recovery.shouldRestart(5_000, 0, eligible = true))
    recovery.interrupt()
    assertFalse(recovery.shouldRestart(20_000, 0, eligible = true))
    assertTrue(recovery.shouldRestart(23_000, 0, eligible = true))
  }

  @Test fun changedPlaybackPositionStartsANewObservation() {
    val recovery = AudioStartupRecovery(stallMs = 3_000)
    assertFalse(recovery.shouldRestart(0, 142_013, eligible = true))
    assertFalse(recovery.shouldRestart(3_000, 190_000, eligible = true))
    assertFalse(recovery.shouldRestart(5_999, 190_000, eligible = true))
    assertTrue(recovery.shouldRestart(6_000, 190_000, eligible = true))
  }

  @Test fun advancingAudioDisarmsStartupRecovery() {
    val recovery = AudioStartupRecovery(stallMs = 3_000)
    assertFalse(recovery.shouldRestart(0, 142_013, eligible = true))
    recovery.onAudioPositionAdvancing()
    assertFalse(recovery.shouldRestart(3_000, 142_013, eligible = true))
    recovery.interrupt()
    assertFalse(recovery.shouldRestart(6_000, 150_000, eligible = true))
  }
}
