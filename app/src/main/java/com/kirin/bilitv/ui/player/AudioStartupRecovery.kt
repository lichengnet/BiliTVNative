package com.kirin.bilitv.ui.player

internal class AudioStartupRecovery(private val stallMs: Long) {
  private var positionMs: Long? = null
  private var stationarySinceMs = 0L
  private var finished = false

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
    return true
  }
}
