package com.kirin.bilitv.ui.player

internal enum class AudioRestoreReason { TRACKS_DISABLED, TIMEOUT, INTERRUPTED }

internal class AudioStartupRecovery(
  private val stallMs: Long,
  private val trackDisableTimeoutMs: Long,
) {
  private var positionMs: Long? = null
  private var stationarySinceMs = 0L
  private var finished = false
  private var trackDisableDeadlineMs: Long? = null

  fun onAudioPositionAdvancing() {
    finished = true
  }

  fun interrupt() {
    positionMs = null
  }

  fun shouldRestart(nowMs: Long, currentPositionMs: Long, eligible: Boolean): Boolean {
    if (finished) return false
    if (!eligible || currentPositionMs < 0L) {
      interrupt()
      return false
    }
    if (positionMs != currentPositionMs) {
      positionMs = currentPositionMs
      stationarySinceMs = nowMs
      return false
    }
    if (nowMs - stationarySinceMs < stallMs) return false
    finished = true
    trackDisableDeadlineMs = nowMs + trackDisableTimeoutMs
    return true
  }

  fun restoreReason(nowMs: Long, audioSelected: Boolean, keepWaiting: Boolean): AudioRestoreReason? {
    val deadline = trackDisableDeadlineMs ?: return null
    val reason = when {
      !audioSelected -> AudioRestoreReason.TRACKS_DISABLED
      !keepWaiting -> AudioRestoreReason.INTERRUPTED
      nowMs >= deadline -> AudioRestoreReason.TIMEOUT
      else -> return null
    }
    trackDisableDeadlineMs = null
    return reason
  }
}
