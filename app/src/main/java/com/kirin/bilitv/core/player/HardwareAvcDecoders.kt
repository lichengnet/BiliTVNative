package com.kirin.bilitv.core.player

import android.content.Context
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.CodecSpecificDataUtil
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector

internal object HardwareAvcDecoders {
  val selector = MediaCodecSelector { mime, secure, tunneling ->
    if (!mime.startsWith("video/")) MediaCodecSelector.DEFAULT.getDecoderInfos(mime, secure, tunneling)
    else if (mime == MimeTypes.VIDEO_H264) hardware(mime, secure, tunneling) else emptyList()
  }
  private fun hardware(mime: String, secure: Boolean, tunneling: Boolean): List<MediaCodecInfo> =
    MediaCodecSelector.DEFAULT.getDecoderInfos(mime, secure, tunneling).filter { it.hardwareAccelerated && !it.softwareOnly }

  fun assess(context: Context, track: PlaybackTrack): AvcTrackAssessment = try {
    val format = Format.Builder().setSampleMimeType(MimeTypes.VIDEO_H264).setCodecs(track.codecs)
      .setWidth(track.width).setHeight(track.height).setAverageBitrate(track.bandwidth)
      .apply { PlaybackFrameRate.parse(track.frameRate)?.let { setFrameRate(it.fps) } }.build()
    val decoders = hardware(MimeTypes.VIDEO_H264, false, false)
    val supported = decoders.firstOrNull { it.isFormatSupported(context, format) }
    when {
      supported != null -> AvcTrackAssessment(AvcTrackDecision.Supported, supported.name)
      decoders.isEmpty() -> AvcTrackAssessment(AvcTrackDecision.NoHardwareDecoder)
      else -> assessRejection(context, track, format, decoders.singleOrNull())
    }
  } catch (_: Exception) {
    AvcTrackAssessment(AvcTrackDecision.CapabilityUnknown)
  }

  private fun assessRejection(context: Context, track: PlaybackTrack, format: Format, decoder: MediaCodecInfo?): AvcTrackAssessment {
    val rejected = AvcTrackAssessment(AvcTrackDecision.CapabilityRejected, decoder?.name)
    if (decoder == null || !track.isLevelTrialCandidate()) return rejected
    return try {
      val profileLevel = CodecSpecificDataUtil.getCodecProfileAndLevel(format) ?: return rejected
      val advertisedLevel = decoder.profileLevels.filter { it.profile == profileLevel.first && it.level > 0 }
        .maxOfOrNull { it.level } ?: return rejected
      val levelOnly = advertisedLevel < profileLevel.second &&
        decoder.isFormatFunctionallySupported(context, format) &&
        supportsTrialSizeAndRate(decoder, track)
      AvcTrackAssessment(
        if (levelOnly) AvcTrackDecision.LevelOnly else AvcTrackDecision.CapabilityRejected,
        decoder.name,
        advertisedLevel,
      )
    } catch (_: Exception) {
      // An error in the extra trial check must not undo a definite rejection.
      rejected
    }
  }

  private fun supportsTrialSizeAndRate(decoder: MediaCodecInfo, track: PlaybackTrack): Boolean {
    val rate = PlaybackFrameRate.parse(track.frameRate) ?: return false
    val size = decoder.alignVideoSizeV21(track.width, track.height) ?: return false
    // Keep codec alignment, but require the advertised rate without Media3's playback heuristics.
    return decoder.capabilities?.videoCapabilities?.areSizeAndRateSupported(
      size.x, size.y, rate.numerator.toDouble() / rate.denominator,
    ) == true
  }
}
