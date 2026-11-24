package com.kirin.bilitv.ui.home

import com.kirin.bilitv.ui.theme.BiliSizing
import kotlin.math.floor

internal data class VideoGridLayoutSpec(val columns: Int, val cardContentScale: Float = 1f) {
  init {
    require(columns > 0)
    require(cardContentScale.isFinite() && cardContentScale > 0f)
  }
  fun rowCount(itemCount: Int): Int = if (itemCount <= 0) 0 else (itemCount - 1) / columns + 1
  fun rowForIndex(index: Int): Int = index.coerceAtLeast(0) / columns
}

internal fun adaptiveGridColumns(widthDp: Float, minWidthDp: Float, spacingDp: Float, maxColumns: Int = 4): Int {
  require(minWidthDp.isFinite() && minWidthDp > 0f)
  require(spacingDp.isFinite() && spacingDp >= 0f)
  require(maxColumns > 0)
  val width = if (widthDp.isNaN()) 0f else widthDp.coerceAtLeast(0f)
  return floor((width + spacingDp) / (minWidthDp + spacingDp)).toInt().coerceIn(1, maxColumns)
}

internal fun resolveVideoGridLayoutSpec(widthDp: Float, contentScale: Float = 1f): VideoGridLayoutSpec {
  require(contentScale.isFinite() && contentScale > 0f)
  return VideoGridLayoutSpec(
    adaptiveGridColumns(widthDp, BiliSizing.VideoCardPreferredMinWidth.value * contentScale,
      BiliSizing.VideoGridSpacing.value * contentScale, BiliSizing.VideoGridColumns),
    contentScale,
  )
}
