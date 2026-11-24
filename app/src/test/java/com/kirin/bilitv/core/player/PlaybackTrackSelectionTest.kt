package com.kirin.bilitv.core.player

import org.junit.Assert.*
import org.junit.Test

class PlaybackTrackSelectionTest {
  private val capability = CodecCapability(true, false, false)
  private fun track(codec: String) = PlaybackTrack(
    id = 64, baseUrl = "https://example.com/video", backupUrls = emptyList(), bandwidth = 1000,
    codecs = codec, width = 1280, height = 720, mimeType = "video/mp4",
    segmentBase = PlaybackSegmentBase("0-100", "101-200"),
  )
  @Test fun fixedH264OnlyReturnsAvcTracks() {
    val avc = track("avc1.64001F")
    assertEquals(listOf(avc), selectPlayableVideoTracks(listOf(track("hev1"), avc, track("av01")), PlaybackCodecPreference.H264, capability))
  }
  @Test fun fixedH264DoesNotFallbackToHevcAv1OrUnknown() {
    assertTrue(selectPlayableVideoTracks(listOf(track("hev1"), track("av01"), track("unknown")), PlaybackCodecPreference.H264, capability).isEmpty())
  }
  @Test fun emptyResponseRemainsEmpty() {
    assertTrue(selectPlayableVideoTracks(emptyList(), PlaybackCodecPreference.H264, capability).isEmpty())
  }
  @Test fun explicitH264RemainsAvailableToSoftwareDecoder() {
    assertEquals(1, selectPlayableVideoTracks(listOf(track("avc1")), PlaybackCodecPreference.H264, CodecCapability(false, false, false)).size)
  }
  @Test fun autoStillPrefersDeviceSupportedTracks() {
    val avc = track("avc1")
    assertEquals(listOf(avc), selectPlayableVideoTracks(listOf(avc, track("hev1")), PlaybackCodecPreference.Auto, capability))
  }
  @Test fun otherPreferencesRetainTheirExistingFallback() {
    val avc = track("avc1")
    assertEquals(listOf(avc), selectPlayableVideoTracks(listOf(avc), PlaybackCodecPreference.H265, capability))
  }
}
