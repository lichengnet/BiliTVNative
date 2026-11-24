package com.kirin.bilitv.ui.search

import com.kirin.bilitv.core.settings.AppUiSize
import com.kirin.bilitv.ui.theme.BiliSizing
import com.kirin.bilitv.ui.theme.BiliSpacing
import com.kirin.bilitv.ui.theme.UiMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTouchInputLayoutSpecTest {
  @Test fun allProfilesKeepWideInputInline() {
    for (size in AppUiSize.entries) {
      val scale = UiMetrics(size).scale
      for (count in 1..2) assertEquals(SearchTouchInputLayoutSpec(true, count), resolveSearchTouchInputLayoutSpec(900f, scale, count))
    }
  }
  @Test fun inlineBoundaryReservesDesiredInputWidthAndAllGaps() {
    for (size in AppUiSize.entries) for (count in 1..2) {
      val scale = UiMetrics(size).scale
      val actionWidth = (BiliSizing.SearchTouchActionButtonWidth.value * scale).coerceAtLeast(BiliSizing.MinimumTouchTarget.value)
      val threshold = BiliSizing.SearchTouchInputPreferredMinWidth.value * scale + count * (actionWidth + BiliSpacing.Md.value * scale)
      assertTrue(resolveSearchTouchInputLayoutSpec(threshold, scale, count).inline)
      assertFalse(resolveSearchTouchInputLayoutSpec(threshold - 0.1f, scale, count).inline)
    }
  }
  @Test fun narrowTwoActionsDoNotReduceInputToZeroWidth() {
    assertEquals(SearchTouchInputLayoutSpec(false, 2), resolveSearchTouchInputLayoutSpec(200f, 1f, 2))
    assertEquals(SearchTouchInputLayoutSpec(false, 1), resolveSearchTouchInputLayoutSpec(180f, BiliSizing.UiSizeLargeScale, 2))
  }
  @Test fun oneActionHasExactlyOneColumnInBothOrientations() {
    assertEquals(SearchTouchInputLayoutSpec(false, 1), resolveSearchTouchInputLayoutSpec(180f, 1f, 1))
    assertEquals(SearchTouchInputLayoutSpec(true, 1), resolveSearchTouchInputLayoutSpec(900f, 1f, 1))
  }
  @Test fun unavailableWidthFallsBackSafely() {
    for (width in listOf(0f, -1f, Float.NaN, Float.NEGATIVE_INFINITY)) {
      assertEquals(SearchTouchInputLayoutSpec(false, 1), resolveSearchTouchInputLayoutSpec(width, 1f, 2))
    }
    assertEquals(SearchTouchInputLayoutSpec(true, 2), resolveSearchTouchInputLayoutSpec(Float.POSITIVE_INFINITY, 1f, 2))
  }
  @Test(expected = IllegalArgumentException::class) fun invalidScaleIsRejected() { resolveSearchTouchInputLayoutSpec(900f, 0f, 2) }
  @Test(expected = IllegalArgumentException::class) fun noActionIsRejected() { resolveSearchTouchInputLayoutSpec(900f, 1f, 0) }
  @Test(expected = IllegalArgumentException::class) fun unsupportedActionCountIsRejected() { resolveSearchTouchInputLayoutSpec(900f, 1f, 3) }
}
