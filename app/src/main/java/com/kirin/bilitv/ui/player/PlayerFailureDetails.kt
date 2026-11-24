package com.kirin.bilitv.ui.player

import android.media.MediaCodec
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.StuckPlayerException
import androidx.media3.common.util.Util
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecDecoderException
import com.kirin.bilitv.core.player.PlaybackFailureKind
import com.kirin.bilitv.core.player.playbackFormatFields

internal fun videoFailureKind(errorCode: Int, videoRenderer: Boolean, audioRenderer: Boolean): PlaybackFailureKind {
  if (audioRenderer) return PlaybackFailureKind.Audio
  if (!videoRenderer) return if (errorCode in 2000..2999) PlaybackFailureKind.Network else PlaybackFailureKind.Other
  return when (errorCode) {
    PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
    PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> PlaybackFailureKind.UnsupportedVideo
    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
    PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
    PlaybackException.ERROR_CODE_DECODING_RESOURCES_RECLAIMED,
    PlaybackException.ERROR_CODE_DECODING_FAILED -> PlaybackFailureKind.Decoder
    else -> PlaybackFailureKind.Other
  }
}

internal fun playerFailureKind(error: PlaybackException): PlaybackFailureKind {
  if (error.errorCode == PlaybackException.ERROR_CODE_TIMEOUT && causeChain(error).any { it is StuckPlayerException }) {
    return PlaybackFailureKind.Stalled
  }
  val renderer = error as? ExoPlaybackException
  val mime = renderer?.rendererFormat?.sampleMimeType.orEmpty()
  return videoFailureKind(error.errorCode, mime.startsWith("video/") || renderer?.rendererName?.contains("VideoRenderer") == true,
    mime.startsWith("audio/") || renderer?.rendererName?.contains("AudioRenderer") == true)
}

internal fun playerFailureFields(error: Throwable): Map<String, String> = buildMap {
  if (error is PlaybackException) put("errorName", error.errorCodeName)
  if (error is ExoPlaybackException && error.type == ExoPlaybackException.TYPE_RENDERER) {
    put("formatSupport", Util.getFormatSupportString(error.rendererFormatSupport))
    error.rendererName?.let { put("source", it) }
    error.rendererFormat?.let { putAll(playbackFormatFields(it.codecs, it.width, it.height, it.frameRate)) }
  }
  val causes = causeChain(error).toList()
  put("type", causes.joinToString("-") { cause ->
    when (cause) {
      is ExoPlaybackException -> "ExoPlaybackException"
      is StuckPlayerException -> "StuckPlayerException"
      else -> cause.javaClass.simpleName
    }
  })
  for (cause in causes) {
    if (cause is StuckPlayerException) {
      put("stuckType", when (cause.stuckType) {
        StuckPlayerException.STUCK_BUFFERING_NOT_LOADING -> "BUFFERING_NOT_LOADING"
        StuckPlayerException.STUCK_BUFFERING_NO_PROGRESS -> "BUFFERING_NO_PROGRESS"
        StuckPlayerException.STUCK_PLAYING_NO_PROGRESS -> "PLAYING_NO_PROGRESS"
        StuckPlayerException.STUCK_PLAYING_NOT_ENDING -> "PLAYING_NOT_ENDING"
        StuckPlayerException.STUCK_SUPPRESSED -> "SUPPRESSED"
        else -> "UNKNOWN_${cause.stuckType}"
      })
      put("timeoutMs", cause.timeoutMs.toString())
    }
    val decoder = (cause as? MediaCodecRenderer.DecoderInitializationException)?.codecInfo
      ?: (cause as? MediaCodecDecoderException)?.codecInfo
    decoder?.let { put("decoder", it.name) }
    if (cause is MediaCodec.CodecException) {
      cause.diagnosticInfo?.let { put("diagnostic", it) }
      put("recoverable", cause.isRecoverable.toString())
      put("transient", cause.isTransient.toString())
    }
    if (cause is HttpDataSource.InvalidResponseCodeException) put("http", cause.responseCode.toString())
  }
}

private fun causeChain(error: Throwable) = generateSequence(error) { it.cause }.take(5)

internal fun playbackSignalFields(player: Player): Map<String, String> = mapOf(
  "state" to when (player.playbackState) {
    Player.STATE_IDLE -> "IDLE"
    Player.STATE_BUFFERING -> "BUFFERING"
    Player.STATE_READY -> "READY"
    Player.STATE_ENDED -> "ENDED"
    else -> "UNKNOWN"
  },
  "playWhenReady" to player.playWhenReady.toString(),
  "isLoading" to player.isLoading.toString(),
  "suppression" to player.playbackSuppressionReason.toString(),
  "positionMs" to player.currentPosition.toString(),
  "bufferedMs" to player.totalBufferedDuration.toString(),
)
