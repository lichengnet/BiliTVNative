package com.kirin.bilitv.ui.settings

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import com.kirin.bilitv.core.settings.AppUiSize
import com.kirin.bilitv.ui.theme.BiliTvTheme
import com.kirin.bilitv.ui.theme.LocalUiMetrics
import com.kirin.bilitv.ui.theme.UiSizeScope
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class UiSizeSettingsTest {
  @get:Rule val rule = createComposeRule()

  @Test fun focusingDoesNotChangeSizeAndConfirmKeepsFocus() {
    val size = mutableStateOf(AppUiSize.Medium)
    val requester = FocusRequester()
    rule.setContent { BiliTvTheme { UiSizeScope(size.value) {
      UiSizeSettingsRow(size.value, { size.value = it }, Modifier.focusRequester(requester).testTag("size"))
    } } }
    rule.runOnIdle { requester.requestFocus() }
    rule.runOnIdle { assertEquals(AppUiSize.Medium, size.value) }
    rule.onNodeWithTag("size").performKeyInput { pressKey(Key.DirectionCenter) }
    rule.runOnIdle { assertEquals(AppUiSize.Large, size.value) }
    rule.onNodeWithTag("size").assertIsFocused()
  }

  @Test fun touchCyclesAllSizes() {
    val size = mutableStateOf(AppUiSize.Small)
    rule.setContent { BiliTvTheme { UiSizeScope(size.value) {
      UiSizeSettingsRow(size.value, { size.value = it }, Modifier.testTag("size"))
    } } }
    for (next in listOf(AppUiSize.Medium, AppUiSize.Large, AppUiSize.Small)) {
      rule.onNodeWithTag("size").performTouchInput { click() }
      rule.runOnIdle { assertEquals(next, size.value) }
    }
  }

  @Test fun scaledCustomToggleRetainsCheckedAndDisabledSemantics() {
    rule.setContent { BiliTvTheme { UiSizeScope(AppUiSize.Large) {
      SettingsToggleRow("开关", "测试", checked = true, enabled = false,
        modifier = Modifier.testTag("toggle"), onCheckedChange = {})
    } } }
    rule.onNodeWithTag("toggle").assertIsOn().assertIsNotEnabled()
  }

  @Test fun nonPlayerScopeDoesNotLeakIntoSiblingPlayerScope() {
    var inside = 0f
    var outside = 0f
    rule.setContent { BiliTvTheme {
      UiSizeScope(AppUiSize.Large) { inside = LocalUiMetrics.current.scale }
      outside = LocalUiMetrics.current.scale
    } }
    rule.runOnIdle { assertEquals(4f / 3f, inside, 0f); assertEquals(1f, outside, 0f) }
  }
}
