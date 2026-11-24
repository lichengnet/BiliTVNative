package com.kirin.bilitv.ui.home

import com.kirin.bilitv.ui.theme.LocalUiMetrics
import com.kirin.bilitv.ui.theme.uiDp

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kirin.bilitv.core.model.VideoSummary
import com.kirin.bilitv.ui.common.VideoThumbnailPrefetcher
import com.kirin.bilitv.ui.settings.LocalBiliPerformancePolicy
import com.kirin.bilitv.ui.theme.BiliFocus
import com.kirin.bilitv.ui.theme.BiliMotion
import com.kirin.bilitv.ui.theme.BiliSizing
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TvGridRestoreFocusRetryCount = 8

internal class TvGridFocusRestoreTargetTracker {
  private var trackedRequestKey: Int? = null
  private var trackedIndex: Int = 0

  fun targetIndex(requestKey: Int, currentIndex: Int): Int {
    if (trackedRequestKey != requestKey) {
      trackedRequestKey = requestKey
      trackedIndex = currentIndex
    }
    return trackedIndex
  }
}

internal data class TvGridVisibleRow(
  val index: Int,
  val offset: Int,
  val size: Int,
)

internal fun resolveTvGridNavigationEntryRow(
  viewportStartOffset: Int,
  viewportEndOffset: Int,
  visibleRows: List<TvGridVisibleRow>,
): Int? {
  val rowsInViewport = visibleRows.filter { row ->
    row.size > 0 && row.offset < viewportEndOffset && row.offset + row.size > viewportStartOffset
  }
  val firstFullyVisibleRow = rowsInViewport.firstOrNull { row ->
    row.offset >= viewportStartOffset && row.offset + row.size <= viewportEndOffset
  }
  return firstFullyVisibleRow?.index ?: rowsInViewport.maxByOrNull { row ->
    minOf(row.offset + row.size, viewportEndOffset) - maxOf(row.offset, viewportStartOffset)
  }?.index
}

private val TvGridBringIntoViewSpec = object : BringIntoViewSpec {
  override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
    // D-pad row scrolling is handled below. Returning 0 prevents Compose's
    // default focus relocation from doing an instant pre-scroll first.
    return 0f
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TvVideoGrid(
  videos: List<VideoSummary>,
  firstItemFocusRequester: FocusRequester,
  restoredFocusIndex: Int,
  restoreFocusRequestKey: Int,
  onRestoreFocusHandled: (Int) -> Unit,
  onFocusedIndexChange: (Int, VideoSummary) -> Unit,
  onLoadMore: () -> Unit,
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
  layoutSpec: VideoGridLayoutSpec,
  viewportWidth: Dp,
) {
  val ui = LocalUiMetrics.current
  val spec = layoutSpec
  val metrics = remember(spec.cardContentScale) { VideoCardMetrics(spec.cardContentScale) }
  val columns = spec.columns
  val rowCount = spec.rowCount(videos.size)
  val restoreTargetTracker = remember { TvGridFocusRestoreTargetTracker() }
  val frozenRestoreTargetIndex = restoreTargetTracker.targetIndex(
    requestKey = restoreFocusRequestKey,
    currentIndex = restoredFocusIndex,
  )
  val restoreTargetIndex = frozenRestoreTargetIndex.coerceIn(0, (videos.size - 1).coerceAtLeast(0))
  val restoreTargetRow = if (videos.isEmpty()) {
    0
  } else {
    spec.rowForIndex(restoreTargetIndex)
  }
  val listState = rememberLazyListState(initialFirstVisibleItemIndex = restoreTargetRow)
  val coroutineScope = rememberCoroutineScope()
  val performancePolicy = LocalBiliPerformancePolicy.current
  val density = LocalDensity.current
  val topBleedPx = with(density) { topBleed.roundToPx() }
  val focusScrollInsetPx = with(density) { topPadding.roundToPx() }
  val focusedRowTopPaddingPx = with(density) { ui.dp(BiliFocus.FocusedRowTopPadding).roundToPx() }
  val videoCardFallbackHeightPx = with(density) { metrics.fallbackHeight.roundToPx() }
  val itemFocusRequesters = remember(videos.size) {
    List(videos.size) { FocusRequester() }
  }
  var focusScrollJob by remember { mutableStateOf<Job?>(null) }
  var focusedIndex by remember { mutableIntStateOf(-1) }
  var gridHasFocus by remember { mutableStateOf(false) }
  var focusedKey by remember { mutableStateOf<Any?>(null) }
  val reflowTracker = remember { VideoGridReflowTracker() }
  val videoKeys = rememberUpdatedState(videos.mapIndexed(keyFactory))
  val reflowFocusAnchor = if (gridHasFocus && focusedIndex >= 0) VideoGridAnchor(focusedKey, focusedIndex) else null
  var navigationEntryTargetIndex by remember { mutableIntStateOf(0) }
  var rowScrollActive by remember { mutableStateOf(false) }
  var rowScrollGeneration by remember { mutableIntStateOf(0) }
  val focusScale = when {
    !performancePolicy.motionEnabled -> 1f
    performancePolicy.cinematicVisualEffectsEnabled -> BiliFocus.CinematicCardScale
    else -> BiliFocus.CardScale
  }

  VideoThumbnailPrefetcher(
    videos = videos,
    focusedIndex = if (focusedIndex >= 0) focusedIndex else restoredFocusIndex,
    enabled = !rowScrollActive,
  )

  LaunchedEffect(listState, videos.size, topBleedPx, columns) {
    snapshotFlow {
      listState.navigationEntryRow(topBleedPx)
    }
      .distinctUntilChanged()
      .collect { row ->
        if (row != null && videos.isNotEmpty()) {
          navigationEntryTargetIndex = (row * columns).coerceIn(0, videos.lastIndex)
        }
      }
  }

  suspend fun scrollRow(row: Int, smoothScroll: Boolean) {
    listState.scrollRowIntoStablePosition(
      row = row,
      totalRows = rowCount,
      fallbackItemHeightPx = videoCardFallbackHeightPx,
      scrollInsetPx = focusScrollInsetPx,
      focusedRowTopPaddingPx = focusedRowTopPaddingPx,
      focusScale = focusScale,
      smoothScroll = smoothScroll,
    )
  }

  LaunchedEffect(listState, spec, viewportWidth, density.density, density.fontScale) {
    if (reflowTracker.hasLayout && videoKeys.value.isNotEmpty() && restoreFocusRequestKey <= 0) {
      focusScrollJob?.cancel()
      rowScrollGeneration++
      rowScrollActive = false
      val anchor = reflowFocusAnchor ?: reflowTracker.anchor
      val index = resolveVideoGridAnchorIndex(anchor, videoKeys.value)
      if (reflowFocusAnchor != null) {
        scrollRow(spec.rowForIndex(index), smoothScroll = false)
        for (attempt in 0 until TvGridRestoreFocusRetryCount) {
          withFrameNanos { }
          if (runCatching { itemFocusRequesters[index].requestFocus() }.getOrDefault(false)) break
        }
      } else {
        listState.scrollToItem(spec.rowForIndex(index), anchor.scrollOffset)
      }
    }
    reflowTracker.hasLayout = true
    snapshotFlow {
      val keys = videoKeys.value
      val index = (listState.firstVisibleItemIndex * columns).coerceIn(0, keys.lastIndex.coerceAtLeast(0))
      VideoGridAnchor(keys.getOrNull(index), index, listState.firstVisibleItemScrollOffset)
    }.collect { reflowTracker.anchor = it }
  }

  LaunchedEffect(restoreFocusRequestKey, videos.size, spec) {
    if (restoreFocusRequestKey <= 0 || videos.isEmpty()) {
      return@LaunchedEffect
    }
    val targetIndex = restoreTargetIndex
    scrollRow(spec.rowForIndex(targetIndex), smoothScroll = false)
    repeat(TvGridRestoreFocusRetryCount) {
      withFrameNanos { }
      val focused = runCatching {
        itemFocusRequesters[targetIndex].requestFocus()
      }.getOrDefault(false)
      if (focused) {
        onRestoreFocusHandled(restoreFocusRequestKey)
        return@LaunchedEffect
      }
    }
    onRestoreFocusHandled(restoreFocusRequestKey)
  }

  LaunchedEffect(videos.size, requestInitialFocus) {
    if (requestInitialFocus && videos.isNotEmpty()) {
      withFrameNanos { }
      runCatching {
        firstItemFocusRequester.requestFocus()
      }
      onInitialFocusRequested()
    }
  }

  fun focusItem(index: Int): Boolean {
    return runCatching {
      itemFocusRequesters[index].requestFocus()
    }.isSuccess
  }

  fun commitFocusedItem(index: Int) {
    videos.getOrNull(index)?.let { video ->
      onFocusedIndexChange(index, video)
    }
  }

  fun scrollThenFocusItem(index: Int, row: Int) {
    focusScrollJob?.cancel()
    val scrollGeneration = ++rowScrollGeneration
    rowScrollActive = true
    focusScrollJob = coroutineScope.launch {
      val smoothScroll = performancePolicy.smoothScrollingEnabled
      try {
        if (smoothScroll) {
          val scrollJob = launch {
            scrollRow(row, smoothScroll = true)
          }
          delay(BiliMotion.FocusScrollDelayMs)
          focusItem(index)
          scrollJob.join()
          delay(BiliMotion.FocusScrollSettleMs)
        } else {
          scrollRow(row, smoothScroll = false)
          withFrameNanos { }
          focusItem(index)
        }
      } finally {
        if (rowScrollGeneration == scrollGeneration) {
          rowScrollActive = false
        }
      }
    }
  }

  fun moveFocus(fromIndex: Int, direction: Key): Boolean {
    val gridDirection = when (direction) {
      Key.DirectionUp -> VideoGridDirection.Up
      Key.DirectionDown -> VideoGridDirection.Down
      Key.DirectionLeft -> VideoGridDirection.Left
      Key.DirectionRight -> VideoGridDirection.Right
      else -> return false
    }
    return when (val move = resolveVideoGridFocusMove(fromIndex, videos.size, columns, gridDirection)) {
      VideoGridFocusMove.AboveGrid -> {
        commitFocusedItem(fromIndex)
        onMoveUpFromFirstRow()
      }
      VideoGridFocusMove.Navigation -> {
        commitFocusedItem(fromIndex)
        onMoveLeftToNav()
      }
      VideoGridFocusMove.Stay -> true
      is VideoGridFocusMove.Item -> {
        if (gridDirection == VideoGridDirection.Left || gridDirection == VideoGridDirection.Right) {
          focusItem(move.index)
        } else {
          scrollThenFocusItem(move.index, spec.rowForIndex(move.index))
          true
        }
      }
    }
  }

  CompositionLocalProvider(LocalBringIntoViewSpec provides TvGridBringIntoViewSpec) {
    LazyColumn(
      state = listState,
      modifier = modifier
        .fillMaxSize()
        .onFocusChanged { gridHasFocus = it.hasFocus }
        .focusGroup()
        .layout { measurable, constraints ->
          if (topBleedPx <= 0) {
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
              placeable.place(0, 0)
            }
          } else {
            val expandedMaxHeight = if (constraints.maxHeight == Constraints.Infinity) {
              Constraints.Infinity
            } else {
              constraints.maxHeight + topBleedPx
            }
            val placeable = measurable.measure(
              constraints.copy(maxHeight = expandedMaxHeight),
            )
            val layoutHeight = if (constraints.maxHeight == Constraints.Infinity) {
              placeable.height
            } else {
              constraints.maxHeight
            }
            layout(placeable.width, layoutHeight) {
              placeable.place(0, -topBleedPx)
            }
          }
        },
      contentPadding = PaddingValues(
        start = horizontalPadding,
        top = topPadding,
        end = horizontalPadding,
        bottom = ui.dp(BiliSizing.VideoGridBottomPadding),
      ),
      verticalArrangement = Arrangement.spacedBy(ui.dp(BiliSizing.VideoGridSpacing)),
    ) {
      items(
        count = rowCount,
        key = { row ->
          val firstIndex = row * columns
          "row-$row-${keyFactory(firstIndex, videos[firstIndex])}"
        },
        contentType = { "video-row" },
      ) { row ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .zIndex(
              if (focusedIndex >= 0 && focusedIndex / columns == row) {
                BiliFocus.FocusedZIndex
              } else {
                0f
              },
            ),
          horizontalArrangement = Arrangement.spacedBy(ui.dp(BiliSizing.VideoGridSpacing)),
        ) {
          repeat(columns) { column ->
            val index = row * columns + column
            if (index < videos.size) {
              val video = videos[index]
              VideoCard(
                video = video,
                mode = cardMode,
                contentScale = spec.cardContentScale,
                interactionPaused = rowScrollActive,
                modifier = Modifier
                  .weight(1f)
                  .then(
                    if (index == navigationEntryTargetIndex.coerceIn(0, videos.lastIndex)) {
                      Modifier.focusRequester(firstItemFocusRequester)
                    } else {
                      Modifier
                    },
                  )
                  .focusRequester(itemFocusRequesters[index])
                  .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) {
                      return@onPreviewKeyEvent false
                    }
                    when (event.key) {
                      Key.Back -> onBackKey?.invoke() ?: false
                      Key.DirectionUp,
                      Key.DirectionDown,
                      Key.DirectionLeft,
                      Key.DirectionRight -> moveFocus(index, event.key)
                      else -> false
                    }
                },
                onFocused = {
                  focusedIndex = index
                  focusedKey = keyFactory(index, video)
                  commitFocusedItem(index)
                  if (index.shouldLoadMore(
                      totalItems = videos.size,
                      threshold = performancePolicy.loadMoreFocusThreshold,
                    )
                  ) {
                    onLoadMore()
                  }
                },
                onClick = {
                  commitFocusedItem(index)
                  onVideoSelected(video)
                },
              )
            } else {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }
    }
  }
}

private fun LazyListState.navigationEntryRow(topBleedPx: Int): Int? {
  val layout = layoutInfo
  return resolveTvGridNavigationEntryRow(
    viewportStartOffset = layout.viewportStartOffset + topBleedPx,
    viewportEndOffset = layout.viewportEndOffset,
    visibleRows = layout.visibleItemsInfo.map { item ->
      TvGridVisibleRow(
        index = item.index,
        offset = item.offset,
        size = item.size,
      )
    },
  )
}

private suspend fun LazyListState.scrollRowIntoStablePosition(
  row: Int,
  totalRows: Int,
  fallbackItemHeightPx: Int,
  scrollInsetPx: Int,
  focusedRowTopPaddingPx: Int,
  focusScale: Float,
  smoothScroll: Boolean,
) {
  val safeRow = row.coerceIn(0, (totalRows - 1).coerceAtLeast(0))
  val layout = layoutInfo
  val viewportTop = layout.viewportStartOffset
  val viewportBottom = layout.viewportEndOffset
  val itemHeightPx = layout.visibleItemsInfo.firstOrNull { item -> item.index == safeRow }?.size
    ?: layout.visibleItemsInfo.firstOrNull()?.size
    ?: fallbackItemHeightPx
  val focusOverflowPx = ((itemHeightPx * (focusScale - 1f)) / 2f).roundToInt()
  val edgeInsetPx = scrollInsetPx + focusOverflowPx
  val focusedRow = layout.visibleItemsInfo.firstOrNull { item -> item.index == safeRow }

  if (focusedRow != null) {
    val targetTop = (viewportTop + focusedRowTopPaddingPx.coerceAtLeast(edgeInsetPx))
      .coerceAtMost(viewportBottom - edgeInsetPx - focusedRow.size)
      .coerceAtLeast(viewportTop + edgeInsetPx)
    val scrollDelta = focusedRow.offset - targetTop
    if (abs(scrollDelta) <= BiliMotion.FocusScrollMinDeltaPx) {
      return
    }
    if (smoothScroll) {
      animateScrollBy(
        value = scrollDelta.toFloat(),
        animationSpec = tween(
          durationMillis = BiliMotion.FocusScrollMs,
          easing = BiliMotion.FocusScrollEasing,
        ),
      )
    } else {
      scroll {
        scrollBy(scrollDelta.toFloat())
      }
    }
    return
  }

  if (smoothScroll) {
    animateScrollToItem(safeRow, scrollOffset = -focusedRowTopPaddingPx)
  } else {
    scrollToItem(safeRow, scrollOffset = -focusedRowTopPaddingPx)
  }
}

private fun Int.shouldLoadMore(totalItems: Int, threshold: Int): Boolean {
  return this >= totalItems - threshold
}
