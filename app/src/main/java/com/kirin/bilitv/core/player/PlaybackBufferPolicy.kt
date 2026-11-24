package com.kirin.bilitv.core.player

internal data class PlaybackBufferPolicy(
  val minMs: Int, val maxMs: Int, val playbackMs: Int, val rebufferMs: Int,
  val targetBytes: Int? = null, val prioritizeTime: Boolean = true,
)

internal fun playbackBufferPolicy(constrainedDevice: Boolean): PlaybackBufferPolicy =
  if (constrainedDevice) PlaybackBufferPolicy(5_000, 15_000, 1_000, 2_500, 8 * 1024 * 1024, false)
  else PlaybackBufferPolicy(30_000, 90_000, 1_500, 4_000)
