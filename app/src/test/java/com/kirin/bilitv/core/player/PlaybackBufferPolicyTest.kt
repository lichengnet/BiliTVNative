package com.kirin.bilitv.core.player

import org.junit.Assert.*
import org.junit.Test

class PlaybackBufferPolicyTest {
  @Test fun constrainedBufferHasEightMiBTargetAndSizePriority() {
    val p = playbackBufferPolicy(true)
    assertEquals(8 * 1024 * 1024,p.targetBytes); assertFalse(p.prioritizeTime)
  }
  @Test fun constrainedDurationsMatchRecoveryBudget() {
    val p = playbackBufferPolicy(true)
    assertEquals(5000,p.minMs); assertEquals(15000,p.maxMs); assertEquals(1000,p.playbackMs); assertEquals(2500,p.rebufferMs)
  }
  @Test fun normalDeviceKeepsOriginalBehavior() {
    val p = playbackBufferPolicy(false)
    assertEquals(30000,p.minMs); assertEquals(90000,p.maxMs); assertEquals(1500,p.playbackMs); assertEquals(4000,p.rebufferMs)
    assertNull(p.targetBytes); assertTrue(p.prioritizeTime)
  }
}
