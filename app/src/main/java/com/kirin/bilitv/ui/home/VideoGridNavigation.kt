package com.kirin.bilitv.ui.home

internal enum class VideoGridDirection { Up, Down, Left, Right }

internal sealed interface VideoGridFocusMove {
  data class Item(val index: Int) : VideoGridFocusMove
  data object AboveGrid : VideoGridFocusMove
  data object Navigation : VideoGridFocusMove
  data object Stay : VideoGridFocusMove
}

internal fun resolveVideoGridFocusMove(
  index: Int,
  itemCount: Int,
  columns: Int,
  direction: VideoGridDirection,
): VideoGridFocusMove {
  require(columns > 0)
  if (index < 0 || index >= itemCount) return VideoGridFocusMove.Stay
  val row = index / columns
  val column = index % columns
  val lastIndex = itemCount - 1
  return when (direction) {
    VideoGridDirection.Up -> if (row == 0) VideoGridFocusMove.AboveGrid else VideoGridFocusMove.Item(index - columns)
    VideoGridDirection.Down -> if (row == lastIndex / columns) VideoGridFocusMove.Stay
      else VideoGridFocusMove.Item((index + columns).coerceAtMost(lastIndex))
    VideoGridDirection.Left -> if (column == 0) VideoGridFocusMove.Navigation else VideoGridFocusMove.Item(index - 1)
    VideoGridDirection.Right -> if (column == columns - 1 || index == lastIndex) VideoGridFocusMove.Stay
      else VideoGridFocusMove.Item(index + 1)
  }
}
