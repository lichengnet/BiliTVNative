package com.kirin.bilitv.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.unit.Density
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.kirin.bilitv.R
import com.kirin.bilitv.core.model.VideoSummary
import com.kirin.bilitv.core.settings.AppUiSize
import com.kirin.bilitv.core.settings.AppPerformancePolicy
import com.kirin.bilitv.ui.common.VideoGridSkeleton
import com.kirin.bilitv.ui.input.DeviceClass
import com.kirin.bilitv.ui.input.InputMode
import com.kirin.bilitv.ui.input.InteractionProfile
import com.kirin.bilitv.ui.input.LocalInteractionMode
import com.kirin.bilitv.ui.input.LocalInteractionProfile
import com.kirin.bilitv.ui.settings.LocalBiliPerformancePolicy
import com.kirin.bilitv.ui.theme.UiSizeScope
import com.kirin.bilitv.ui.theme.UiMetrics
import com.kirin.bilitv.ui.theme.BiliSizing
import com.kirin.bilitv.ui.theme.BiliTvTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UiSizeVideoGridLayoutTest {
  @get:Rule val composeRule = createComposeRule()
  private val largeSpec = VideoGridLayoutSpec(3, BiliSizing.UiSizeLargeScale)

  @Test fun homeAndSearchTouchUseThreeLargeCards() = assertGrid(InputMode.Touch, VideoCardMode.Standard)
  @Test fun homeAndSearchRemoteUseThreeLargeCards() = assertGrid(InputMode.Remote, VideoCardMode.Standard)
  @Test fun dynamicTouchUsesThreeLargeCards() = assertGrid(InputMode.Touch, VideoCardMode.Dynamic)
  @Test fun dynamicRemoteUsesThreeLargeCards() = assertGrid(InputMode.Remote, VideoCardMode.Dynamic)
  @Test fun historyTouchUsesThreeLargeCards() = assertGrid(InputMode.Touch, VideoCardMode.History)
  @Test fun historyRemoteUsesThreeLargeCards() = assertGrid(InputMode.Remote, VideoCardMode.History)

  @Test fun touchSkeletonUsesThreeColumnsAndEnlargedInfo() = assertSkeleton(InputMode.Touch)
  @Test fun remoteSkeletonUsesThreeColumnsAndEnlargedInfo() = assertSkeleton(InputMode.Remote)

  @Test fun smallTouchList() = assertGrid(InputMode.Touch, VideoCardMode.Standard, AppUiSize.Small)
  @Test fun smallTouchSkeleton() = assertSkeleton(InputMode.Touch, AppUiSize.Small)
  @Test fun smallRemoteList() = assertGrid(InputMode.Remote, VideoCardMode.Standard, AppUiSize.Small)
  @Test fun smallRemoteSkeleton() = assertSkeleton(InputMode.Remote, AppUiSize.Small)
  @Test fun mediumTouchList() = assertGrid(InputMode.Touch, VideoCardMode.Standard, AppUiSize.Medium)
  @Test fun mediumTouchSkeleton() = assertSkeleton(InputMode.Touch, AppUiSize.Medium)
  @Test fun mediumRemoteList() = assertGrid(InputMode.Remote, VideoCardMode.Standard, AppUiSize.Medium)
  @Test fun mediumRemoteSkeleton() = assertSkeleton(InputMode.Remote, AppUiSize.Medium)

  @Test fun ordinaryCardKeepsOriginalSize() {
    composeRule.setContent {
      TestViewport(InputMode.Touch, AppUiSize.Medium) { VideoCard(fixture(0), modifier = Modifier.requiredSize(300.dp, 231.dp)) }
    }
    assertEquals(62f, bounds("video_info_BVfixture0").height.value, 1f)
    assertEquals(14f, textLayout(fixture(0).title).layoutInput.style.fontSize.value, 0.01f)
    assertEquals(12f, textLayout(fixture(0).ownerName).layoutInput.style.fontSize.value, 0.01f)
  }

  @Test fun failedCoverHasScaledDiagnosticTextInsideCover() {
    var failureLabel = ""
    composeRule.setContent {
      val context = LocalContext.current
      failureLabel = context.getString(R.string.video_thumbnail_load_failed)
      TestViewport(InputMode.Touch) { VideoCard(fixture(0), contentScale = largeSpec.cardContentScale) }
    }
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodesWithTag("video_failure_BVfixture0", useUnmergedTree = true)
        .fetchSemanticsNodes().isNotEmpty()
    }
    val cover = bounds("video_cover_BVfixture0")
    val failure = bounds("video_failure_BVfixture0")
    assertTrue(failure.left >= cover.left && failure.right <= cover.right)
    assertTrue(failure.top >= cover.top && failure.bottom <= cover.bottom)
    assertEquals(24f, textLayout(failureLabel).layoutInput.style.fontSize.value, 0.01f)
    val diagnostic = textLayout("诊断：", substring = true)
    assertEquals(16f, diagnostic.layoutInput.style.fontSize.value, 0.01f)
    assertTrue(!diagnostic.didOverflowHeight)
  }

  @Test fun dpadEdgesNavigationAndPlaybackReturnKeepVideoIndex() {
    val firstRequester = FocusRequester()
    val restoreKey = mutableIntStateOf(0)
    val restoredIndex = mutableIntStateOf(0)
    val showGrid = mutableStateOf(true)
    var navMoves = 0
    var selectedIndex = -1
    var restoreHandled = 0
    val shortVideos = (0..6).map { fixture(it).copy(title = "视频$it", ownerName = "作者$it") }
    composeRule.setContent {
      TestViewport(InputMode.Remote) {
        if (showGrid.value) {
          AdaptiveVideoGrid(
            videos = shortVideos,
            firstItemFocusRequester = firstRequester,
            restoredFocusIndex = restoredIndex.intValue,
            restoreFocusRequestKey = restoreKey.intValue,
            onRestoreFocusHandled = { restoreHandled = it },
            onFocusedIndexChange = { index, _ -> selectedIndex = index },
            onLoadMore = {},
            onMoveLeftToNav = { navMoves++; true },
            onVideoSelected = { video ->
              restoredIndex.intValue = shortVideos.indexOf(video)
              showGrid.value = false
            },
          )
        }
      }
    }
    composeRule.runOnIdle { firstRequester.requestFocus() }
    key(0, Key.DirectionLeft)
    composeRule.runOnIdle { assertEquals(1, navMoves) }
    key(0, Key.DirectionRight)
    key(1, Key.DirectionRight)
    key(2, Key.DirectionRight)
    composeRule.onNodeWithTag("video_card_BVfixture2").assertIsFocused()
    key(2, Key.DirectionDown)
    composeRule.onNodeWithTag("video_card_BVfixture5").assertIsFocused()
    key(5, Key.DirectionDown)
    composeRule.onNodeWithTag("video_card_BVfixture6").assertIsFocused()
    key(6, Key.DirectionDown)
    composeRule.runOnIdle { assertEquals(6, selectedIndex) }
    key(6, Key.DirectionCenter)
    composeRule.runOnIdle {
      assertEquals(6, restoredIndex.intValue)
      restoreKey.intValue = 1
      showGrid.value = true
    }
    composeRule.waitUntil(timeoutMillis = 5_000) { restoreHandled == 1 }
    composeRule.onNodeWithTag("video_card_BVfixture6").assertIsFocused()
  }

  @Test fun touchPlaybackReturnRestoresVideoWithNewColumnsAndConsumesRequest() {
    val size = mutableStateOf(AppUiSize.Medium)
    val showGrid = mutableStateOf(true)
    val restoreKey = mutableIntStateOf(0)
    val restoreIndex = mutableIntStateOf(0)
    var handled = 0
    val videos = (0..59).map { fixture(it).copy(title = "视频$it", ownerName = "作者$it") }
    composeRule.setContent {
      TestViewport(InputMode.Touch, size.value) {
        if (showGrid.value) AdaptiveVideoGrid(videos, remember { FocusRequester() }, restoreIndex.intValue,
          restoreKey.intValue, { handled++; restoreKey.intValue = 0 },
          { i, _ -> restoreIndex.intValue = i }, {}, onMoveLeftToNav = { true },
          onVideoSelected = { showGrid.value = false })
      }
    }
    composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(4)
    composeRule.onNodeWithTag("video_card_BVfixture16").performClick()
    composeRule.runOnIdle {
      assertEquals(16, restoreIndex.intValue)
      size.value = AppUiSize.Large
      restoreKey.intValue = 1
      showGrid.value = true
    }
    composeRule.waitUntil(timeoutMillis = 5000) { handled == 1 }
    composeRule.onNodeWithTag("video_card_BVfixture16").assertExists()
    assertEquals(bounds("video_card_BVfixture15").top.value, bounds("video_card_BVfixture16").top.value, 1f)
    composeRule.runOnIdle { assertEquals(0, restoreKey.intValue) }
  }

  @Test fun focusedRefinedFailureTextIsNotCoveredAndStaysOneLineAtLargeFontScale() {
    val requester = FocusRequester()
    var label = ""
    composeRule.setContent {
      label = LocalContext.current.getString(R.string.video_thumbnail_load_failed)
      val density = LocalDensity.current
      val profile = InteractionProfile(DeviceClass.Tablet, InputMode.Remote)
      BiliTvTheme { CompositionLocalProvider(
        LocalDensity provides Density(density.density, 1.5f),
        LocalInteractionProfile provides profile,
        LocalInteractionMode provides profile.legacyMode,
        LocalBiliPerformancePolicy provides AppPerformancePolicy.Refined,
      ) { UiSizeScope(AppUiSize.Large) {
        Box(Modifier.requiredSize(220.dp, 360.dp)) {
          VideoCard(fixture(0).copy(badge = ""), modifier = Modifier.focusRequester(requester))
        }
      } } }
    }
    composeRule.waitUntil(timeoutMillis = 5000) {
      composeRule.onAllNodesWithTag("video_failure_BVfixture0", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.runOnIdle { requester.requestFocus() }
    composeRule.mainClock.advanceTimeBy(500)
    val text = textLayout(label)
    assertEquals(1, text.lineCount)
    assertTrue(!text.didOverflowHeight)
    val pixels = composeRule.onNodeWithText(label, useUnmergedTree = true).captureToImage().toPixelMap()
    var brightPixels = 0
    for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
      val c = pixels[x, y]
      if (c.red > 0.7f && c.green > 0.7f && c.blue > 0.7f) brightPixels++
    }
    assertTrue("Failure glyphs must remain above the blur/error placeholder", brightPixels > 0)
  }

  @Test fun largeFontScaleBadgesGrowWithoutClippingHeight() {
    composeRule.setContent {
      val density = LocalDensity.current
      CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
        TestViewport(InputMode.Touch) { VideoCard(fixture(0)) }
      }
    }
    assertTrue(!textLayout(fixture(0).badge).didOverflowHeight)
  }

  @Test fun touchReflowKeepsPreviouslyVisibleVideoNotOldRow() {
    val size = mutableStateOf(AppUiSize.Medium)
    val videos = (0..59).map { fixture(it).copy(title = "视频$it", ownerName = "作者$it") }
    composeRule.setContent {
      TestViewport(InputMode.Touch, size.value) {
        AdaptiveVideoGrid(videos, remember { FocusRequester() }, 0, 0, {}, { _, _ -> }, {},
          onMoveLeftToNav = { true }, onVideoSelected = {})
      }
    }
    composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(4)
    composeRule.onNodeWithTag("video_card_BVfixture16").assertExists()
    composeRule.runOnIdle { size.value = AppUiSize.Large }
    composeRule.onNodeWithTag("video_card_BVfixture16").assertExists()
    assertEquals(bounds("video_card_BVfixture15").top.value, bounds("video_card_BVfixture16").top.value, 1f)
  }

  @Test fun resizingAndSizeSwitchPreserveFocusedVideo() {
    val size = mutableStateOf(AppUiSize.Medium)
    val width = mutableStateOf(1024.dp)
    val first = FocusRequester()
    val videos = (0..20).map { fixture(it).copy(title = "视频$it", ownerName = "作者$it") }
    composeRule.setContent {
      TestViewport(InputMode.Remote, size.value) {
        Box(modifier = Modifier.requiredSize(width.value, 720.dp)) {
          AdaptiveVideoGrid(videos, first, 0, 0, {}, { _, _ -> }, {},
            onMoveLeftToNav = { true }, onVideoSelected = {})
        }
      }
    }
    composeRule.runOnIdle { first.requestFocus() }
    key(0, Key.DirectionRight)
    key(1, Key.DirectionDown)
    composeRule.onNodeWithTag("video_card_BVfixture5").assertIsFocused()
    composeRule.runOnIdle { size.value = AppUiSize.Large }
    composeRule.onNodeWithTag("video_card_BVfixture5").assertIsFocused()
    composeRule.runOnIdle { width.value = 700.dp }
    composeRule.onNodeWithTag("video_card_BVfixture5").assertIsFocused()
    key(5, Key.DirectionUp)
    composeRule.onNodeWithTag("video_card_BVfixture3").assertIsFocused()
  }

  private fun assertGrid(inputMode: InputMode, mode: VideoCardMode, size: AppUiSize = AppUiSize.Large) {
    val scale = UiMetrics(size).scale
    var clicked = -1
    composeRule.setContent {
      TestViewport(inputMode, size) {
        AdaptiveVideoGrid(
          videos = (0..6).map(::fixture),
          firstItemFocusRequester = remember { FocusRequester() },
          restoredFocusIndex = 0,
          restoreFocusRequestKey = 0,
          onRestoreFocusHandled = {},
          onFocusedIndexChange = { _, _ -> },
          onLoadMore = {},
          onMoveLeftToNav = { true },
          onVideoSelected = { clicked = it.bvid.removePrefix("BVfixture").toInt() },
          cardMode = mode,
        )
      }
    }
    assertColumns("video_card_BVfixture", resolveVideoGridLayoutSpec(1024f, scale).columns)
    val cover = bounds("video_cover_BVfixture0")
    assertEquals(16f / 9f, cover.width.value / cover.height.value, 0.03f)
    assertTrue(bounds("video_info_BVfixture0").height.value >= 62f * scale - 1f)
    assertEquals(20f * scale, bounds("video_avatar_BVfixture0").width.value, 1f)
    val title = textLayout(fixture(0).title)
    val owner = textLayout(fixture(0).ownerName)
    assertEquals(14f * scale, title.layoutInput.style.fontSize.value, 0.01f)
    assertEquals(19f * scale, title.layoutInput.style.lineHeight.value, 0.01f)
    assertEquals(12f * scale, owner.layoutInput.style.fontSize.value, 0.01f)
    assertEquals(1, title.lineCount)
    assertEquals(1, owner.lineCount)
    assertTrue(!title.didOverflowHeight && !owner.didOverflowHeight)
    val badge = textLayout(fixture(0).badge)
    assertTrue(!badge.didOverflowWidth && !badge.didOverflowHeight)
    if (mode == VideoCardMode.History) {
      assertEquals(3f * scale, bounds("video_progress_BVfixture0").height.value, 1f)
    }
    composeRule.onNodeWithTag("video_card_BVfixture0").performClick()
    composeRule.runOnIdle { assertEquals(0, clicked) }
  }

  private fun assertSkeleton(inputMode: InputMode, size: AppUiSize = AppUiSize.Large) {
    val scale = UiMetrics(size).scale
    composeRule.setContent {
      TestViewport(inputMode, size) { VideoGridSkeleton(count = 7) }
    }
    assertColumns("video_skeleton_", resolveVideoGridLayoutSpec(1024f, scale).columns)
    val cover = bounds("video_skeleton_cover_0")
    assertEquals(16f / 9f, cover.width.value / cover.height.value, 0.03f)
    assertEquals(62f * scale, bounds("video_skeleton_info_0").height.value, 1f)
  }

  private fun assertColumns(prefix: String, columns: Int) {
    val first = bounds(prefix + "0")
    for (index in 1 until columns) {
      val card = bounds(prefix + index)
      assertEquals(first.top.value, card.top.value, 1f)
      assertEquals(first.width.value, card.width.value, 1f)
      assertTrue(card.left > first.left)
    }
    val fourth = bounds(prefix + columns)
    assertTrue(fourth.top > first.top)
    assertEquals(first.left.value, fourth.left.value, 1f)
  }

  private fun bounds(tag: String) = composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

  private fun textLayout(text: String, substring: Boolean = false): TextLayoutResult {
    val results = mutableListOf<TextLayoutResult>()
    composeRule.onNodeWithText(text, substring = substring, useUnmergedTree = true)
      .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
    return results.single()
  }

  private fun key(index: Int, key: Key) {
    composeRule.onNodeWithTag("video_card_BVfixture$index").performKeyInput { pressKey(key) }
  }

  @Composable
  private fun TestViewport(inputMode: InputMode, size: AppUiSize = AppUiSize.Large, content: @Composable () -> Unit) {
    val profile = InteractionProfile(DeviceClass.Tablet, inputMode)
    BiliTvTheme {
      CompositionLocalProvider(
        LocalInteractionProfile provides profile,
        LocalInteractionMode provides profile.legacyMode,
        LocalBiliPerformancePolicy provides AppPerformancePolicy.LowSpec,
      ) {
        // A stable logical viewport; does not change the device's density or fontScale.
        UiSizeScope(size) { Box(modifier = Modifier.requiredSize(1024.dp, 720.dp)) { content() } }
      }
    }
  }

  private fun fixture(index: Int) = VideoSummary(
    bvid = "BVfixture$index",
    title = "长标题 $index 用于测试单行省略与焦点跑马灯，不应挤压作者信息区",
    pic = "file:///__bilitv_layout_test_missing_cover__.jpg",
    ownerName = "很长的作者名称 $index 用于测试省略号",
    ownerFace = "",
    ownerMid = index.toLong(),
    view = 12345,
    danmaku = 2345,
    duration = 600,
    pubdate = 0,
    badge = "角标$index",
    progress = 180,
    viewAt = 0,
    historyPage = 2,
    historyVideos = 3,
  )
}
