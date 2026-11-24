package com.kirin.bilitv.ui.home

internal data class VideoGridAnchor(val key: Any?, val index: Int, val scrollOffset: Int = 0)

internal fun resolveVideoGridAnchorIndex(anchor: VideoGridAnchor, keys: List<Any>): Int {
  if (keys.isEmpty()) return 0
  val matchingIndex = anchor.key?.let { keys.indexOf(it) } ?: -1
  return if (matchingIndex >= 0) matchingIndex else anchor.index.coerceIn(0, keys.lastIndex)
}

internal class VideoGridReflowTracker {
  var hasLayout = false
  var anchor = VideoGridAnchor(null, 0)
}
