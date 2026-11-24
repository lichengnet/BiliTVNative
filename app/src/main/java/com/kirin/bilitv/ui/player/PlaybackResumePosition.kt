package com.kirin.bilitv.ui.player

import com.kirin.bilitv.core.player.PlaybackRequest

internal fun resolvePlaybackStartPosition(request: PlaybackRequest, savedPositionMs: Long?, recoveryPositionMs: Long?): Long =
  (recoveryPositionMs ?: if (request.preferredQualityId != null || request.forceStartPosition) request.startPositionMs
    else savedPositionMs ?: request.startPositionMs).coerceAtLeast(0L)
