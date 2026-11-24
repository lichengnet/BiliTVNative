package com.kirin.bilitv.core.player

import com.kirin.bilitv.core.device.PlaybackLogLevel
import com.kirin.bilitv.core.device.SessionDiagnostics
import com.kirin.bilitv.core.device.SessionEvent

internal data class PlaybackTrace(
  val session: Int,
  val player: Int,
  val generation: Long,
  val attempt: Int,
  val bvid: String,
  val cid: Long,
) {
  fun event(
    event: SessionEvent,
    phase: String,
    fields: Map<String, String> = emptyMap(),
    level: PlaybackLogLevel = if (event == SessionEvent.PLAY_FAILURE) PlaybackLogLevel.Error else PlaybackLogLevel.Info,
  ) {
    if (!SessionDiagnostics.captures(level)) return
    val identity = mapOf(
      "session" to session.toString(), "player" to player.toString(), "generation" to generation.toString(),
      "attempt" to attempt.toString(), "bvid" to bvid, "phase" to phase,
    )
    val episode = if (cid > 0) mapOf("cid" to cid.toString()) else emptyMap()
    SessionDiagnostics.event(event, identity + episode + fields, level = level)
  }
}

internal fun playbackFormatFields(codec: String?, width: Int, height: Int, fps: Float?): Map<String, String> = buildMap {
  codec?.takeIf { it.isNotBlank() }?.let {
    put("codec", it)
    if (it.startsWith("avc1.", ignoreCase = true) || it.startsWith("avc3.", ignoreCase = true)) {
      val avc = it.substringAfter('.')
      if (avc.length == 6) {
        avc.take(2).toIntOrNull(16)?.let { profile -> put("profile", profile.toString()) }
        avc.takeLast(2).toIntOrNull(16)?.let { level -> put("level", "${level / 10}.${level % 10}") }
      }
    }
  }
  if (width > 0) put("width", width.toString())
  if (height > 0) put("height", height.toString())
  put("fps", fps?.takeIf { it > 0 }?.toString() ?: "UNKNOWN")
}
