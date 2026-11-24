package com.kirin.bilitv.ui.home

import com.kirin.bilitv.ui.theme.LocalUiMetrics
import com.kirin.bilitv.ui.theme.uiDp

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kirin.bilitv.core.model.VideoSummary
import com.kirin.bilitv.ui.input.InputMode
import com.kirin.bilitv.ui.input.LocalInteractionProfile
import com.kirin.bilitv.ui.theme.BiliFocus
import com.kirin.bilitv.ui.theme.BiliSizing

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AdaptiveVideoGrid(
  videos: List<VideoSummary>,
  firstItemFocusRequester: FocusRequester,
  restoredFocusIndex: Int,
  restoreFocusRequestKey: Int,
  onRestoreFocusHandled: (Int) -> Unit,
  onFocusedIndexChange: (Int, VideoSummary) -> Unit,
  onLoadMore: () -> Unit,
  onRefresh: (() -> Unit)? = null,
  onMoveLeftToNav: () -> Boolean,
  onVideoSelected: (VideoSummary) -> Unit,
  modifier: Modifier = Modifier,
  cardMode: VideoCardMode = VideoCardMode.Standard,
  requestInitialFocus: Boolean = false,
  onInitialFocusRequested: () -> Unit = {},
  onMoveUpFromFirstRow: () -> Boolean = { true },
  onBackKey: (() -> Boolean)? = null,
  horizontalPadding: Dp = uiDp(BiliSizing.VideoGridHorizontalPadding),
  topPadding: Dp = uiDp(BiliFocus.ScrollInset),
  topBleed: Dp = 0.dp,
  keyFactory: (Int, VideoSummary) -> Any = { _, video -> video.bvid },
  layoutSpec: VideoGridLayoutSpec? = null,
) {
  val ui = LocalUiMetrics.current
  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val spec = layoutSpec ?: resolveVideoGridLayoutSpec((maxWidth - horizontalPadding * 2).value, ui.scale)
  when (LocalInteractionProfile.current.inputMode) {
    InputMode.Touch -> TouchVideoGrid(
      videos = videos,
      restoredFocusIndex = restoredFocusIndex,
      restoreFocusRequestKey = restoreFocusRequestKey,
      onRestoreFocusHandled = onRestoreFocusHandled,
      onFocusedIndexChange = onFocusedIndexChange,
      onLoadMore = onLoadMore,
      onRefresh = onRefresh,
      onVideoSelected = onVideoSelected,
      modifier = Modifier.fillMaxSize(),
      cardMode = cardMode,
      horizontalPadding = horizontalPadding,
      topPadding = topPadding,
      topBleed = topBleed,
      keyFactory = keyFactory,
      layoutSpec = spec,
    )

    InputMode.Remote -> TvVideoGrid(
      videos = videos,
      firstItemFocusRequester = firstItemFocusRequester,
      restoredFocusIndex = restoredFocusIndex,
      restoreFocusRequestKey = restoreFocusRequestKey,
      onRestoreFocusHandled = onRestoreFocusHandled,
      onFocusedIndexChange = onFocusedIndexChange,
      onLoadMore = onLoadMore,
      onMoveLeftToNav = onMoveLeftToNav,
      onVideoSelected = onVideoSelected,
      modifier = Modifier.fillMaxSize(),
      cardMode = cardMode,
      requestInitialFocus = requestInitialFocus,
      onInitialFocusRequested = onInitialFocusRequested,
      onMoveUpFromFirstRow = onMoveUpFromFirstRow,
      onBackKey = onBackKey,
      horizontalPadding = horizontalPadding,
      topPadding = topPadding,
      topBleed = topBleed,
      keyFactory = keyFactory,
      layoutSpec = spec,
      viewportWidth = maxWidth,
    )
  }
  }
}
