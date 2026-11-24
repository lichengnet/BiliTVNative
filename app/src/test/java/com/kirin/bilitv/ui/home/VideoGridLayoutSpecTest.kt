package com.kirin.bilitv.ui.home

import com.kirin.bilitv.core.settings.AppUiSize
import com.kirin.bilitv.ui.theme.UiMetrics
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoGridLayoutSpecTest {
  @Test fun mediumHasOneToFourColumnsAtCalculatedBoundaries() {
    for ((width, count) in listOf(0f to 1, 239f to 1, 489.99f to 1, 490f to 2, 739.99f to 2, 740f to 3, 989.99f to 3, 990f to 4, 4096f to 4)) {
      assertEquals(count, resolveVideoGridLayoutSpec(width).columns)
    }
  }
  @Test fun smallCanShowMoreCardsAndLargeFewerAtSameWidth() {
    assertEquals(4, resolveVideoGridLayoutSpec(900f, UiMetrics(AppUiSize.Small).scale).columns)
    assertEquals(3, resolveVideoGridLayoutSpec(900f, UiMetrics(AppUiSize.Medium).scale).columns)
    assertEquals(2, resolveVideoGridLayoutSpec(900f, UiMetrics(AppUiSize.Large).scale).columns)
  }
  @Test fun largeIsNotPermanentlyThreeColumns() {
    val scale = UiMetrics(AppUiSize.Large).scale
    for ((width, count) in listOf(300f to 1, 720f to 2, 1024f to 3, 1440f to 4)) assertEquals(count, resolveVideoGridLayoutSpec(width, scale).columns)
  }
  @Test fun unavailableAndVeryWideSpaceRemainSafe() {
    for (width in listOf(-1f, Float.NaN, Float.NEGATIVE_INFINITY)) assertEquals(1, resolveVideoGridLayoutSpec(width).columns)
    assertEquals(4, resolveVideoGridLayoutSpec(Float.POSITIVE_INFINITY).columns)
  }
  @Test fun settingsChipsUseTheirOwnPreferredWidth() {
    assertEquals(1, adaptiveGridColumns(70f, 80f, 12f))
    assertEquals(2, adaptiveGridColumns(200f, 80f, 12f))
    assertEquals(4, adaptiveGridColumns(600f, 80f, 12f))
  }
  @Test fun rowsHandleEmptyAndOneToSevenItems() {
    val spec = VideoGridLayoutSpec(3)
    for ((count, expected) in listOf(0,1,1,1,2,2,2,3).withIndex()) assertEquals(expected, spec.rowCount(count))
  }
  @Test fun resizingRestoresVideoIndexNotOldRow() {
    val tracker = TvGridFocusRestoreTargetTracker()
    val index = tracker.targetIndex(1, 6)
    assertEquals(6, tracker.targetIndex(1, 0))
    assertEquals(1, VideoGridLayoutSpec(4).rowForIndex(index))
    assertEquals(2, VideoGridLayoutSpec(3).rowForIndex(index))
    assertEquals(3, VideoGridLayoutSpec(2).rowForIndex(index))
  }
  @Test(expected = IllegalArgumentException::class) fun invalidScaleIsRejected() { resolveVideoGridLayoutSpec(720f, 0f) }
  @Test(expected = IllegalArgumentException::class) fun zeroColumnsAreRejected() { VideoGridLayoutSpec(0) }
}
