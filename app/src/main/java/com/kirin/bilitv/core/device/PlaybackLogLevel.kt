package com.kirin.bilitv.core.device

enum class PlaybackLogLevel(val key: String) {
  Off("off"), Error("error"), Info("info"), Debug("debug");

  internal fun includes(level: PlaybackLogLevel): Boolean = level != Off && ordinal >= level.ordinal

  companion object {
    fun fromKey(key: String?): PlaybackLogLevel = entries.firstOrNull { it.key == key } ?: Info
  }
}
