package com.kirin.bilitv.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoGridAnchorTest {
  @Test fun reorderedListKeepsVideoIdentity() {
    assertEquals(2, resolveVideoGridAnchorIndex(VideoGridAnchor("b", 1), listOf("c", "a", "b")))
  }
  @Test fun missingVideoFallsBackToClampedIndex() {
    assertEquals(1, resolveVideoGridAnchorIndex(VideoGridAnchor("missing", 6), listOf("a", "b")))
    assertEquals(0, resolveVideoGridAnchorIndex(VideoGridAnchor(null, -1), listOf("a", "b")))
  }
  @Test fun emptyListAlwaysHasSafeAnchor() {
    assertEquals(0, resolveVideoGridAnchorIndex(VideoGridAnchor("a", 7), emptyList()))
  }
  @Test fun columnChangeUsesVideoIndexNotOldRow() {
    val keys = (0..20).map { "video-$it" }
    val anchor = VideoGridAnchor("video-16", 16, 12)
    val index = resolveVideoGridAnchorIndex(anchor, keys)
    assertEquals(4, VideoGridLayoutSpec(4).rowForIndex(index))
    assertEquals(5, VideoGridLayoutSpec(3).rowForIndex(index))
    assertEquals(8, VideoGridLayoutSpec(2).rowForIndex(index))
    assertEquals(12, anchor.scrollOffset)
  }
  @Test fun firstLayoutDoesNotInventReflow() {
    val tracker = VideoGridReflowTracker()
    assertEquals(false, tracker.hasLayout)
    assertEquals(0, tracker.anchor.index)
  }
}
