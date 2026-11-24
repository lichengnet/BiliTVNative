package com.kirin.bilitv.ui.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.kirin.bilitv.core.settings.AppUiSize
import com.kirin.bilitv.ui.theme.BiliTvTheme
import com.kirin.bilitv.ui.theme.UiSizeScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SearchTouchInputLayoutTest {
  @get:Rule val rule = createComposeRule()

  @Test fun allSizesLeaveRoomForResultsWithAndWithoutClearButton() {
    val size = mutableStateOf(AppUiSize.Medium)
    val query = mutableStateOf("")
    var searched = ""
    rule.setContent { BiliTvTheme { UiSizeScope(size.value) {
      Column(Modifier.requiredSize(700.dp, 600.dp)) {
        SearchTouchInputBar(query.value, FocusRequester(), { query.value = it }, { query.value = "" }, { searched = it })
        Box(Modifier.fillMaxWidth().weight(1f).testTag("results"))
      }
    } } }
    for (profile in AppUiSize.entries) for (text in listOf("", "TEST")) {
      rule.runOnIdle { size.value = profile; query.value = text }
      assertTrue(bounds("search_touch_input_bar").height.value < 150f)
      assertTrue(bounds("results").height.value > 400f)
      rule.onNodeWithTag("search_touch_search").performClick()
      rule.runOnIdle { assertEquals(text, searched) }
      if (text.isNotEmpty()) {
        rule.onNodeWithTag("search_touch_clear").performClick()
        rule.runOnIdle { assertEquals("", query.value) }
      }
    }
  }

  @Test fun narrowLargeBarKeepsInputAndBothActionsInsideWindow() {
    rule.setContent { BiliTvTheme { UiSizeScope(AppUiSize.Large) {
      Column(Modifier.requiredSize(180.dp, 720.dp).testTag("viewport")) {
        SearchTouchInputBar("TEST", FocusRequester(), {}, {}, {})
        Box(Modifier.fillMaxWidth().weight(1f).testTag("results"))
      }
    } } }
    val viewport = bounds("viewport")
    for (tag in listOf("search_touch_input", "search_touch_clear", "search_touch_search")) {
      val child = bounds(tag)
      assertTrue(child.left >= viewport.left && child.right <= viewport.right)
      assertTrue(child.width.value >= 48f && child.height.value >= 48f)
    }
    assertTrue(bounds("search_touch_clear").top >= bounds("search_touch_input").bottom)
    assertTrue(bounds("search_touch_search").top >= bounds("search_touch_clear").bottom)
    assertTrue(bounds("results").height.value > 300f)
  }

  @Test fun reflowAndSizeChangesKeepInputFocusAndText() {
    val width = mutableStateOf(700.dp)
    val size = mutableStateOf(AppUiSize.Medium)
    val requester = FocusRequester()
    rule.setContent { BiliTvTheme { UiSizeScope(size.value) {
      Column(Modifier.requiredSize(width.value, 720.dp)) {
        SearchTouchInputBar("测试内容", requester, {}, {}, {})
        Box(Modifier.fillMaxWidth().weight(1f))
      }
    } } }
    rule.onNodeWithTag("search_touch_input").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
    rule.runOnIdle { width.value = 180.dp }
    rule.onNodeWithTag("search_touch_input").assertIsFocused().assertTextContains("测试内容")
    rule.runOnIdle { size.value = AppUiSize.Large }
    rule.onNodeWithTag("search_touch_input").assertIsFocused().assertTextContains("测试内容")
    rule.runOnIdle { width.value = 700.dp }
    rule.onNodeWithTag("search_touch_input").assertIsFocused().assertTextContains("测试内容")
  }

  @Test fun systemLargeFontGrowsNaturallyWithoutHeightClipping() {
    rule.setContent {
      val density = LocalDensity.current
      BiliTvTheme { CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) { UiSizeScope(AppUiSize.Large) {
        Column(Modifier.requiredSize(300.dp, 720.dp)) {
          SearchTouchInputBar("TEST", FocusRequester(), {}, {}, {})
          Box(Modifier.fillMaxWidth().weight(1f).testTag("results"))
        }
      } } }
    }
    for (tag in listOf("search_touch_input", "search_touch_clear", "search_touch_search")) {
      assertTrue(bounds(tag).height.value >= 48f)
    }
    for (label in listOf("清空", "搜索")) {
      val results = mutableListOf<TextLayoutResult>()
      rule.onNodeWithText(label, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
      assertFalse(results.single().didOverflowHeight)
    }
    assertTrue(bounds("results").height.value > 300f)
  }

  private fun bounds(tag: String) = rule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()
}
