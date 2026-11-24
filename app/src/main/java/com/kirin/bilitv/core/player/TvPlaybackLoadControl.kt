package com.kirin.bilitv.core.player

import androidx.media3.exoplayer.DefaultLoadControl

fun createTvPlaybackLoadControl(constrainedDevice: Boolean = false): DefaultLoadControl {
  val policy = playbackBufferPolicy(constrainedDevice)
  return DefaultLoadControl.Builder()
    .setBufferDurationsMs(policy.minMs, policy.maxMs, policy.playbackMs, policy.rebufferMs)
    .setPrioritizeTimeOverSizeThresholds(policy.prioritizeTime)
    .apply { policy.targetBytes?.let { setTargetBufferBytes(it); setBackBuffer(0, false) } }
    .build()
}
