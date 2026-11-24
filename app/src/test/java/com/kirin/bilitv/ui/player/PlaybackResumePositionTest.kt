package com.kirin.bilitv.ui.player

import com.kirin.bilitv.core.player.PlaybackRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackResumePositionTest {
  private val request = PlaybackRequest("BVtest",1,"test",startPositionMs=1000)
  @Test fun firstAttemptUsesSavedProgressBeforeTrackValidation() { assertEquals(30000L,resolvePlaybackStartPosition(request,30000,null)) }
  @Test fun autoAndManualRetriesPreferLastValidPositionEvenForForcedRequest() { assertEquals(45000L,resolvePlaybackStartPosition(request.copy(forceStartPosition=true),30000,45000)) }
  @Test fun explicitZeroRecoveryPositionIsNotReplacedWithSavedProgress() { assertEquals(0L,resolvePlaybackStartPosition(request,30000,0)) }
  @Test fun explicitQualityAndHistoryStartRetainTheirOwnPosition() {
    assertEquals(1000L,resolvePlaybackStartPosition(request.copy(preferredQualityId=64),30000,null))
    assertEquals(1000L,resolvePlaybackStartPosition(request.copy(forceStartPosition=true),30000,null))
  }
  @Test fun negativePositionsAreClamped() { assertEquals(0L,resolvePlaybackStartPosition(request,-1,-20)) }
}
