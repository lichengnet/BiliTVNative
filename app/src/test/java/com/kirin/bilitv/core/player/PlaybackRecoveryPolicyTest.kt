package com.kirin.bilitv.core.player

import org.junit.Assert.*
import org.junit.Test

class PlaybackRecoveryPolicyTest {
  private fun policy(id: Int) = PlaybackRecoveryPolicy().apply { start(id) }
  private fun selectedPolicy(id: Int) = policy(id).apply { accepted(info(track(id, 640, 360))) }

  @Test fun fullHdHasOnlyFiveAttempts() {
    val p = selectedPolicy(80)
    assertTrue(p.next(PlaybackFailureKind.Decoder)); assertEquals(80, p.qualityId)
    assertTrue(p.next(PlaybackFailureKind.Decoder)); assertEquals(64, p.qualityId)
    assertTrue(p.next(PlaybackFailureKind.Decoder)); assertEquals(32, p.qualityId)
    assertTrue(p.next(PlaybackFailureKind.Decoder)); assertEquals(16, p.qualityId)
    assertFalse(p.next(PlaybackFailureKind.Decoder)); assertEquals(5, p.attemptCount)
  }
  @Test fun hdSkipsDuplicate720Tier() {
    val p = selectedPolicy(64)
    assertTrue(p.next(PlaybackFailureKind.Decoder)); assertEquals(64, p.qualityId)
    assertTrue(p.next(PlaybackFailureKind.Decoder)); assertEquals(32, p.qualityId)
    assertTrue(p.next(PlaybackFailureKind.Decoder)); assertEquals(16, p.qualityId)
    assertFalse(p.next(PlaybackFailureKind.Decoder)); assertEquals(4, p.attemptCount)
  }
  @Test fun startingAt360NeverUpgradesAndStopsAfterOneRebuild() {
    val p = selectedPolicy(16)
    assertTrue(p.next(PlaybackFailureKind.Decoder)); assertEquals(16, p.qualityId)
    assertFalse(p.next(PlaybackFailureKind.Decoder)); assertEquals(2, p.attemptCount)
  }
  @Test fun knownUnsupportedFormatSkipsSameQualityRetry() {
    val p = selectedPolicy(80)
    assertTrue(p.next(PlaybackFailureKind.UnsupportedVideo)); assertEquals(64, p.qualityId)
    assertTrue(p.next(PlaybackFailureKind.UnsupportedVideo)); assertEquals(32, p.qualityId)
    assertTrue(p.next(PlaybackFailureKind.UnsupportedVideo)); assertEquals(16, p.qualityId)
    assertFalse(p.next(PlaybackFailureKind.UnsupportedVideo)); assertEquals(4, p.attemptCount)
  }
  @Test fun transportAudioAndOutputFailuresDoNotConsumeRecoveryBudget() {
    for (kind in listOf(PlaybackFailureKind.Network, PlaybackFailureKind.Audio, PlaybackFailureKind.Output, PlaybackFailureKind.Other)) {
      val p = policy(80); val token = p.generation
      assertFalse(p.next(kind)); assertEquals(1, p.attemptCount); assertTrue(p.isCurrent(token))
    }
  }
  @Test fun manualRetryKeepsLastTierPositionPauseAndFreshBudget() {
    val p = selectedPolicy(80)
    repeat(3) { assertTrue(p.next(PlaybackFailureKind.Decoder)) }
    p.updatePosition(34567); p.pausedByUser = true
    p.manualRetry()
    assertEquals(32, p.qualityId); assertEquals(PlaybackAttemptLimit.P480, p.limit)
    assertEquals(34567L, p.lastPositionMs); assertEquals(34567L, p.resumePositionOverride); assertTrue(p.pausedByUser)
    p.accepted(info(track(32, 854, 480)))
    assertTrue(p.next(PlaybackFailureKind.Decoder)); assertEquals(32, p.qualityId)
    assertTrue(p.next(PlaybackFailureKind.Decoder)); assertEquals(16, p.qualityId)
    assertFalse(p.next(PlaybackFailureKind.Decoder))
  }
  @Test fun oldAttemptTokenIsInvalidAfterRecoveryAndNewVideo() {
    val p = selectedPolicy(80); val first = p.generation
    p.next(PlaybackFailureKind.Decoder); assertFalse(p.isCurrent(first))
    val second = p.generation
    p.invalidate(); assertFalse(p.isCurrent(second))
    p.start(64)
    assertEquals(1, p.attemptCount); assertEquals(0L, p.lastPositionMs); assertNull(p.resumePositionOverride)
  }

  @Test fun failureAfterSeekingToBeginningRestartsAtZero() {
    val p = selectedPolicy(64)
    p.updatePosition(40_000)
    p.updatePosition(-1)
    assertEquals(40_000L, p.lastPositionMs)
    p.updatePosition(0)
    assertTrue(p.next(PlaybackFailureKind.Decoder))
    assertEquals(0L, p.resumePositionOverride)
  }

  @Test fun qualityChangeKeepsPauseForTheSamePart() {
    val p = policy(64)
    p.accepted(info(track(64, 1280, 720)).copy(cid = 42))
    p.pausedByUser = true
    p.start(32, bvid = "BVtest", cid = 42)
    assertTrue(p.pausedByUser)
    p.start(32, bvid = "BVtest", cid = 43)
    assertFalse(p.pausedByUser)
  }
  @Test fun actualLowerReturnedTierDoesNotUpgradeOnRecovery() {
    val p = policy(80)
    p.accepted(info(track(32, 854, 480)))
    assertTrue(p.next(PlaybackFailureKind.Decoder)); assertEquals(32, p.qualityId)
    assertTrue(p.next(PlaybackFailureKind.Decoder)); assertEquals(16, p.qualityId)
    assertFalse(p.next(PlaybackFailureKind.Decoder))
  }
  @Test fun oversized720MetadataDoesNotCauseDuplicate720Request() {
    val p = policy(64)
    p.accepted(info(track(64,1280,900)))
    assertTrue(p.next(PlaybackFailureKind.Decoder))
    assertTrue(p.next(PlaybackFailureKind.Decoder))
    assertEquals(32,p.qualityId)
    assertTrue(p.next(PlaybackFailureKind.Decoder)); assertEquals(16, p.qualityId)
    assertFalse(p.next(PlaybackFailureKind.Decoder))
  }

  @Test fun recoveryLocksResolvedEpisodeAndNewVideoClearsIt() {
    val p = policy(80)
    p.accepted(info(track(80,1920,1080)).copy(bvid="BVresolved",cid=42))
    assertNull(p.recoveryCid)
    p.next(PlaybackFailureKind.Decoder)
    assertEquals("BVresolved",p.videoId); assertEquals(42L,p.recoveryCid)
    p.manualRetry(); assertEquals(42L,p.recoveryCid)
    p.start(80,bvid="BVnext",cid=7)
    assertNull(p.recoveryCid); assertEquals("BVnext",p.videoId); assertEquals(7L,p.resolvedCid)
  }
  @Test fun initialVideoTrackFailureMovesToBounded720480And360() {
    val p = policy(80)
    assertTrue(p.next(PlaybackFailureKind.VideoTracks)); assertEquals(64, p.qualityId)
    assertTrue(p.next(PlaybackFailureKind.VideoTracks)); assertEquals(32, p.qualityId)
    assertTrue(p.next(PlaybackFailureKind.VideoTracks)); assertEquals(PlaybackAttemptLimit.P360, p.limit)
    assertFalse(p.next(PlaybackFailureKind.VideoTracks))
  }

  @Test fun firstActualTrackRetainsOneDecoderRetryAfterAnEmptySelection() {
    val p = policy(80)
    assertTrue(p.next(PlaybackFailureKind.VideoTracks)); assertEquals(64, p.qualityId)
    p.accepted(info(track(64, 1280, 720)))
    assertTrue(p.next(PlaybackFailureKind.Decoder)); assertEquals(64, p.qualityId)
    assertTrue(p.next(PlaybackFailureKind.Decoder)); assertEquals(32, p.qualityId)
    assertTrue(p.next(PlaybackFailureKind.Decoder)); assertEquals(16, p.qualityId)
    assertFalse(p.next(PlaybackFailureKind.Decoder)); assertEquals(5, p.attemptCount)
  }

  @Test fun trialRequiresCurrentGenerationAndIsConsumedOnlyOnSubmission() {
    val p = policy(16)
    val selected = info(track(16, 640, 360)).copy(videoSelectionMode = PlaybackVideoSelectionMode.LevelTrial)
    assertTrue(p.canTryLevel(selected.bvid, selected.cid))
    assertFalse(p.submitLevelTrial(p.generation - 1, selected))
    assertTrue(p.canTryLevel(selected.bvid, selected.cid))
    assertTrue(p.submitLevelTrial(p.generation, selected))
    assertFalse(p.next(PlaybackFailureKind.Decoder))
    p.manualRetry()
    assertFalse(p.canTryLevel(selected.bvid, selected.cid))
    p.start(16, bvid = selected.bvid, cid = selected.cid)
    assertFalse(p.canTryLevel(selected.bvid, selected.cid))
    assertTrue(p.canTryLevel(selected.bvid, selected.cid + 1))
  }

  private fun track(id: Int, w: Int, h: Int) = PlaybackTrack(id, "https://example.invalid/video", emptyList(), 1000, "avc1.640033", w, h, "video/mp4", PlaybackSegmentBase("0-1", "2-3"))
  private fun info(track: PlaybackTrack) = PlaybackInfo("BVtest", 1, "test", 60000, listOf(PlaybackQuality(track.id, "test")), PlaybackQuality(track.id, "test"), listOf(track), emptyList(), BiliPlaybackHeaders(null, null))
}
