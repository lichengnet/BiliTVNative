package com.kirin.bilitv.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoundedPlaybackTrackSelectionTest {
  private fun track(
    id: Int, width: Int, height: Int, codec: String = "avc1.640033", bitrate: Int = 1000, rate: String? = "30",
  ) = PlaybackTrack(id, "https://example.invalid/video", emptyList(), bitrate, codec, width, height,
    "video/mp4", PlaybackSegmentBase("0-1", "2-3"), rate)

  private val supported = AvcTrackAssessment(AvcTrackDecision.Supported)
  private val levelOnly = AvcTrackAssessment(AvcTrackDecision.LevelOnly)
  private val trialAvailable = LevelTrialAvailability.Available

  @Test fun codecFilteringNeverAdmitsHevcOrAv1() {
    val avc = track(64, 1280, 720)
    val result = selectBoundedHardwareAvcTracks(
      listOf(avc.copy(codecs = "hvc1"), avc.copy(codecs = "av01"), avc), 64, PlaybackAttemptLimit.P720,
    ) { supported }
    assertEquals(listOf(avc), result.tracks)
    assertEquals(2, result.evaluations.count { it.assessment.decision == AvcTrackDecision.NotAvc })
  }

  @Test fun requestServerAndTierLimitsNeverSelectHigherQuality() {
    val tracks = listOf(track(80, 1920, 1080), track(64, 1280, 720), track(32, 854, 480))
    assertEquals(listOf(64), selectBoundedHardwareAvcTracks(tracks, 80, PlaybackAttemptLimit.P720) { supported }.tracks.map { it.id })
    assertEquals(listOf(32), selectBoundedHardwareAvcTracks(tracks, 32, PlaybackAttemptLimit.P720) { supported }.tracks.map { it.id })
    assertTrue(selectBoundedHardwareAvcTracks(tracks.dropLast(1), 32, null) { supported }.tracks.isEmpty())
  }

  @Test fun dimensionsAreCheckedBeforeProbingHardware() {
    val tracks = listOf(track(64, 1920, 1080), track(64, 720, 1280), track(64, 0, 0), track(64, -1, 480))
    val result = selectBoundedHardwareAvcTracks(tracks, 64, PlaybackAttemptLimit.P720) { error("oversize track probed") }
    assertTrue(result.tracks.isEmpty())
    assertTrue(result.evaluations.all { it.assessment.decision == AvcTrackDecision.Dimensions })
  }

  @Test fun unknownCapabilitiesCanUseNormalPathButKnownRejectionNeedsTrial() {
    val avc = track(16, 640, 360)
    val normal = selectBoundedHardwareAvcTracks(listOf(avc), 16, PlaybackAttemptLimit.P360, trialAvailable) {
      AvcTrackAssessment(AvcTrackDecision.CapabilityUnknown)
    }
    assertEquals(listOf(avc), normal.tracks)
    assertEquals(PlaybackVideoSelectionMode.Normal, normal.mode)
    for (reason in listOf(AvcTrackDecision.NoHardwareDecoder, AvcTrackDecision.CapabilityRejected)) {
      val selected = selectBoundedHardwareAvcTracks(listOf(avc), 16, PlaybackAttemptLimit.P360, trialAvailable) { AvcTrackAssessment(reason) }
      assertTrue(selected.tracks.isEmpty())
    }
  }

  @Test fun trialRequiresExplicit360TierAndSelectsOneLowBitrateTrack() {
    val avc = track(16, 640, 360, bitrate = 5000)
    val low = avc.copy(bandwidth = 1000)
    assertTrue(selectBoundedHardwareAvcTracks(listOf(low), 32, PlaybackAttemptLimit.P480, trialAvailable) { levelOnly }.tracks.isEmpty())
    assertTrue(selectBoundedHardwareAvcTracks(listOf(low), 16, PlaybackAttemptLimit.P360) { levelOnly }.tracks.isEmpty())
    val result = selectBoundedHardwareAvcTracks(listOf(avc, low), 16, PlaybackAttemptLimit.P360, trialAvailable) { levelOnly }
    assertEquals(listOf(low), result.tracks)
    assertEquals(PlaybackVideoSelectionMode.LevelTrial, result.mode)
  }

  @Test fun trialDoesNotAdmitUnknownOrExcessiveRatesDimensionsOrMalformedCodec() {
    val base = track(16, 640, 360)
    val tracks = listOf(base.copy(frameRate = null), base.copy(frameRate = "3000001/100000"),
      base.copy(width = 641), base.copy(width = 360, height = 640), base.copy(codecs = "avc"))
    assertFalse(tracks.any(PlaybackTrack::isLevelTrialCandidate))
    assertTrue(selectBoundedHardwareAvcTracks(tracks, 16, PlaybackAttemptLimit.P360, trialAvailable) { levelOnly }.tracks.isEmpty())
    assertTrue(base.copy(frameRate = "30000/1001").isLevelTrialCandidate())
  }

  @Test fun normalCandidatesTakePriorityAndAreSortedWithinTheSelectedTier() {
    val low = track(16, 640, 360, bitrate = 1000, codec = "avc1.64001E")
    val normal = selectBoundedHardwareAvcTracks(listOf(low.copy(bandwidth = 5000), low, track(16, 640, 360)),
      16, PlaybackAttemptLimit.P360, trialAvailable) { if (it.codecs == low.codecs) supported else levelOnly }
    assertEquals(listOf(1000, 5000), normal.tracks.map { it.bandwidth })
    assertEquals(PlaybackVideoSelectionMode.Normal, normal.mode)
  }

  @Test fun usedTrialIsReportedOnlyForAnOtherwiseEligibleTrack() {
    val levelOnlyTrack = track(16, 636, 360)
    val used = LevelTrialAvailability.Used
    val blocked = selectBoundedHardwareAvcTracks(listOf(levelOnlyTrack), 16, PlaybackAttemptLimit.P360, used) { levelOnly }
    assertTrue(blocked.tracks.isEmpty())
    assertEquals(PlaybackVideoSelectionMode.LevelTrialUsed, blocked.mode)

    val normal = selectBoundedHardwareAvcTracks(listOf(levelOnlyTrack), 16, PlaybackAttemptLimit.P360, used) { supported }
    assertEquals(listOf(levelOnlyTrack), normal.tracks)
    assertEquals(PlaybackVideoSelectionMode.Normal, normal.mode)

    val unknownRate = levelOnlyTrack.copy(frameRate = null)
    val noTrack = selectBoundedHardwareAvcTracks(listOf(unknownRate), 16, PlaybackAttemptLimit.P360, used) { levelOnly }
    assertTrue(noTrack.tracks.isEmpty())
    assertEquals(PlaybackVideoSelectionMode.Normal, noTrack.mode)
  }
}
