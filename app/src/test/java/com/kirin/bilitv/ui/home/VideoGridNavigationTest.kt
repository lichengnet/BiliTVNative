package com.kirin.bilitv.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoGridNavigationTest {
  @Test fun emptyAndOutOfRangeIndicesStay() {
    for (direction in VideoGridDirection.entries) {
      assertEquals(VideoGridFocusMove.Stay, move(0, 0, direction))
      assertEquals(VideoGridFocusMove.Stay, move(-1, 7, direction))
      assertEquals(VideoGridFocusMove.Stay, move(7, 7, direction))
    }
  }

  @Test fun horizontalMovesNeverCrossRowsForOneToSevenItems() {
    for (count in 1..7) for (index in 0 until count) {
      val left = move(index, count, VideoGridDirection.Left)
      val right = move(index, count, VideoGridDirection.Right)
      assertEquals(if (index % 3 == 0) VideoGridFocusMove.Navigation else VideoGridFocusMove.Item(index - 1), left)
      assertEquals(if (index % 3 == 2 || index == count - 1) VideoGridFocusMove.Stay else VideoGridFocusMove.Item(index + 1), right)
      for (result in listOf(left, right).filterIsInstance<VideoGridFocusMove.Item>()) {
        assertEquals(index / 3, result.index / 3)
      }
    }
  }

  @Test fun verticalMovesStayInBoundsIncludingIncompleteLastRow() {
    for (count in 1..7) for (index in 0 until count) {
      val up = move(index, count, VideoGridDirection.Up)
      val down = move(index, count, VideoGridDirection.Down)
      assertEquals(if (index < 3) VideoGridFocusMove.AboveGrid else VideoGridFocusMove.Item(index - 3), up)
      assertEquals(if (index / 3 == (count - 1) / 3) VideoGridFocusMove.Stay else VideoGridFocusMove.Item(minOf(index + 3, count - 1)), down)
      for (result in listOf(up, down).filterIsInstance<VideoGridFocusMove.Item>()) {
        assertTrue(result.index in 0 until count)
      }
    }
  }

  @Test fun firstColumnReturnsToNavigationOnEachRow() {
    for (index in listOf(0, 3, 6)) assertEquals(VideoGridFocusMove.Navigation, move(index, 7, VideoGridDirection.Left))
  }

  @Test fun downClampsToLastCardInShortRow() {
    assertEquals(VideoGridFocusMove.Item(3), move(2, 4, VideoGridDirection.Down))
    assertEquals(VideoGridFocusMove.Item(6), move(5, 7, VideoGridDirection.Down))
  }

  @Test fun ordinaryFourColumnNavigationIsUnchanged() {
    assertEquals(VideoGridFocusMove.Stay, resolveVideoGridFocusMove(3, 7, 4, VideoGridDirection.Right))
    assertEquals(VideoGridFocusMove.Navigation, resolveVideoGridFocusMove(4, 7, 4, VideoGridDirection.Left))
    assertEquals(VideoGridFocusMove.Item(6), resolveVideoGridFocusMove(3, 7, 4, VideoGridDirection.Down))
  }

  @Test(expected = IllegalArgumentException::class)
  fun invalidColumnCountIsRejected() { resolveVideoGridFocusMove(0, 1, 0, VideoGridDirection.Right) }

  private fun move(index: Int, count: Int, direction: VideoGridDirection) =
    resolveVideoGridFocusMove(index, count, 3, direction)
}
