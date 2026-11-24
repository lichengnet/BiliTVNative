package com.kirin.bilitv.ui.player

import androidx.media3.common.Format
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.analytics.AnalyticsListener
import com.kirin.bilitv.core.device.PlaybackLogLevel
import com.kirin.bilitv.core.device.SessionEvent
import com.kirin.bilitv.core.player.PlaybackTrace
import com.kirin.bilitv.core.player.playbackFormatFields

internal fun playerDiagnostics(trace: () -> PlaybackTrace?, isCurrent: () -> Boolean): AnalyticsListener = object : AnalyticsListener {
  override fun onVideoDecoderInitialized(eventTime: AnalyticsListener.EventTime, decoderName: String, initializedTimestampMs: Long, initializationDurationMs: Long) {
    if (isCurrent()) trace()?.event(SessionEvent.PLAY_ATTEMPT, "DECODER_READY", mapOf("decoder" to decoderName))
  }
  override fun onVideoDecoderReleased(eventTime: AnalyticsListener.EventTime, decoderName: String) {
    if (isCurrent()) trace()?.event(SessionEvent.PLAY_RELEASE, "DECODER_RELEASED", mapOf("decoder" to decoderName), PlaybackLogLevel.Debug)
  }
  override fun onVideoInputFormatChanged(eventTime: AnalyticsListener.EventTime, format: Format, decoderReuseEvaluation: DecoderReuseEvaluation?) {
    if (isCurrent()) trace()?.event(SessionEvent.PLAY_ATTEMPT, "INPUT_FORMAT",
      playbackFormatFields(format.codecs, format.width, format.height, format.frameRate))
  }
  override fun onVideoCodecError(eventTime: AnalyticsListener.EventTime, videoCodecError: Exception) {
    if (isCurrent()) trace()?.event(SessionEvent.PLAY_FAILURE, "VIDEO_CODEC", playerFailureFields(videoCodecError))
  }
  override fun onAudioDecoderInitialized(eventTime: AnalyticsListener.EventTime, decoderName: String, initializedTimestampMs: Long, initializationDurationMs: Long) {
    if (isCurrent()) trace()?.event(SessionEvent.PLAY_ATTEMPT, "AUDIO_DECODER_READY", mapOf("decoder" to decoderName), PlaybackLogLevel.Debug)
  }
  override fun onAudioInputFormatChanged(eventTime: AnalyticsListener.EventTime, format: Format, decoderReuseEvaluation: DecoderReuseEvaluation?) {
    if (isCurrent()) trace()?.event(SessionEvent.PLAY_ATTEMPT, "AUDIO_FORMAT", mapOf(
      "codec" to (format.codecs ?: format.sampleMimeType?.substringAfter('/') ?: "UNKNOWN"),
      "sampleRate" to format.sampleRate.toString(), "channels" to format.channelCount.toString(),
    ), PlaybackLogLevel.Debug)
  }
  override fun onAudioPositionAdvancing(eventTime: AnalyticsListener.EventTime, playoutStartSystemTimeMs: Long) {
    if (isCurrent()) trace()?.event(SessionEvent.PLAY_ATTEMPT, "AUDIO_ADVANCING", level = PlaybackLogLevel.Debug)
  }
  override fun onAudioSinkError(eventTime: AnalyticsListener.EventTime, audioSinkError: Exception) {
    if (isCurrent()) trace()?.event(SessionEvent.PLAY_FAILURE, "AUDIO_SINK", playerFailureFields(audioSinkError))
  }
  override fun onVideoSizeChanged(eventTime: AnalyticsListener.EventTime, videoSize: VideoSize) {
    if (isCurrent()) dimensions("VIDEO_SIZE", videoSize.width, videoSize.height)
  }
  override fun onSurfaceSizeChanged(eventTime: AnalyticsListener.EventTime, width: Int, height: Int) {
    if (isCurrent()) dimensions("SURFACE_SIZE", width, height)
  }
  override fun onRenderedFirstFrame(eventTime: AnalyticsListener.EventTime, output: Any, renderTimeMs: Long) {
    if (isCurrent()) trace()?.event(SessionEvent.FIRST_FRAME, "RENDERED", mapOf("source" to output.javaClass.simpleName))
  }

  private fun dimensions(phase: String, width: Int, height: Int) {
    trace()?.event(SessionEvent.PLAY_ATTEMPT, phase,
      mapOf("width" to width.toString(), "height" to height.toString()), PlaybackLogLevel.Debug)
  }
}
