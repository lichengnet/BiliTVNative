package com.kirin.bilitv.ui.player

import android.content.Context
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.kirin.bilitv.core.player.HardwareAvcDecoders
import com.kirin.bilitv.core.player.createTvPlaybackLoadControl

internal fun createCompatiblePlayer(context: Context, constrainedDevice: Boolean): ExoPlayer {
  val builder = ExoPlayer.Builder(context).setLoadControl(createTvPlaybackLoadControl(constrainedDevice))
  if (constrainedDevice) builder.setRenderersFactory(DefaultRenderersFactory(context)
    .setMediaCodecSelector(HardwareAvcDecoders.selector).setEnableDecoderFallback(false))
  return builder.build()
}
