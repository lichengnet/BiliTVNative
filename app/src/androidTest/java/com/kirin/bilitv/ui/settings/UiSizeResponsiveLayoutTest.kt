package com.kirin.bilitv.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.kirin.bilitv.core.player.CodecCapability
import com.kirin.bilitv.core.model.HomeSection
import com.kirin.bilitv.core.storage.UserSession
import com.kirin.bilitv.ui.account.AccountProfileCard
import com.kirin.bilitv.core.settings.AppSettings
import com.kirin.bilitv.core.settings.AppSettingsStore
import com.kirin.bilitv.core.settings.AppUiSize
import com.kirin.bilitv.ui.login.QrContent
import com.kirin.bilitv.ui.login.QrLoginState
import com.kirin.bilitv.ui.search.SearchKeyboardView
import com.kirin.bilitv.ui.theme.BiliTvTheme
import com.kirin.bilitv.ui.theme.UiMetrics
import com.kirin.bilitv.ui.theme.UiSizeScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UiSizeResponsiveLayoutTest {
  @get:Rule val rule = createComposeRule()

  @Test fun settingRowsGrowWithFontScaleAndRetainMinimumTouchTarget() {
    val size = mutableStateOf(AppUiSize.Medium)
    val density = mutableStateOf(Density(1f, 1f))
    rule.setContent { BiliTvTheme {
      CompositionLocalProvider(LocalDensity provides density.value) { UiSizeScope(size.value) {
        Box(Modifier.requiredSize(300.dp, 600.dp)) {
          SettingsOptionRow("长设置标题", "很长的描述文字应当换行并自然撑高，不应该被固定行高裁切。", "设置选项值",
            Modifier.testTag("row"), onClick = {})
        }
      } }
    } }
    for (uiSize in AppUiSize.entries) for (d in listOf(1f, 2f)) {
      rule.runOnIdle { size.value = uiSize; density.value = Density(d, 1.5f) }
      val bounds = rule.onNodeWithTag("row").getUnclippedBoundsInRoot()
      assertTrue(bounds.bottom.value > bounds.top.value)
      assertTrue((bounds.bottom.value - bounds.top.value) * rule.density.density >= 48f * d - 1f)
      val title = layout("长设置标题")
      val description = layout("很长的描述文字应当换行并自然撑高，不应该被固定行高裁切。")
      assertEquals(18f * UiMetrics(uiSize).scale, title.layoutInput.style.fontSize.value, 0.01f)
      assertFalse(title.didOverflowHeight)
      assertFalse(description.didOverflowHeight)
    }
  }

  @Test fun panelsReflowWithoutLosingSettingFocus() {
    val width = mutableStateOf(1000.dp)
    val size = mutableStateOf(AppUiSize.Medium)
    val requester = FocusRequester()
    rule.setContent { BiliTvTheme { UiSizeScope(size.value) {
      SettingsPanelsLayout(Modifier.requiredSize(width.value, 720.dp)) {
        UiSizeSettingsRow(size.value, { size.value = it }, Modifier.testTag("size").focusRequester(requester))
        Box(Modifier.testTag("second"))
      }
    } } }
    rule.runOnIdle { requester.requestFocus() }
    rule.runOnIdle { width.value = 380.dp }
    rule.onNodeWithTag("size").assertIsFocused()
    rule.onNodeWithTag("size").performKeyInput { pressKey(Key.DirectionCenter) }
    rule.runOnIdle { assertEquals(AppUiSize.Large, size.value) }
    rule.onNodeWithTag("size").assertIsFocused()
    val a = rule.onNodeWithTag("size").getUnclippedBoundsInRoot()
    val b = rule.onNodeWithTag("second").getUnclippedBoundsInRoot()
    assertTrue(b.top > a.top)
  }

  @Test fun realSettingsPageSizeOptionIsReachableAndKeepsFocusAfterChange() {
    val settings = mutableStateOf(AppSettings())
    rule.setContent { BiliTvTheme { UiSizeScope(settings.value.uiSize) {
      Box(Modifier.requiredSize(1000.dp, 720.dp)) {
        RenderSettings(settings.value) { settings.value = settings.value.copy(uiSize = it) }
      }
    } } }
    rule.onNodeWithTag("settings_ui_size").performScrollTo()
    rule.onNodeWithTag("settings_ui_size").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
    rule.onNodeWithTag("settings_ui_size").performKeyInput { pressKey(Key.DirectionCenter) }
    rule.runOnIdle { assertEquals(AppUiSize.Large, settings.value.uiSize) }
    rule.onNodeWithTag("settings_ui_size").assertIsFocused()
  }

  @Test fun aboutStaysDisplayOnlyAndDpadScrollsBeforeLeavingRow() {
    rule.setContent { BiliTvTheme { UiSizeScope(AppUiSize.Large) {
      Box(Modifier.requiredSize(380.dp, 720.dp)) { RenderSettings(AppSettings(uiSize = AppUiSize.Large)) }
    } } }
    rule.onNodeWithTag("settings_about").performScrollTo()
      .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
    val panel = rule.onNodeWithTag("settings_about_scroll")
    assertFalse(panel.fetchSemanticsNode().config.contains(SemanticsActions.OnClick))
    rule.onNodeWithTag("settings_about").performKeyInput { pressKey(Key.DirectionDown) }
    rule.onNodeWithTag("settings_about").assertIsFocused()
    assertTrue(panel.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value() > 0f)
    rule.onNodeWithTag("settings_about").performKeyInput { pressKey(Key.DirectionUp) }
    rule.onNodeWithTag("settings_about").assertIsFocused()
    rule.onNodeWithTag("settings_about").performKeyInput { pressKey(Key.DirectionUp) }
    rule.onNodeWithTag("settings_language").assertIsFocused()
  }

  @Test fun narrowSettingsCanEnterCategoriesAndKeepTheirFocusAcrossSizeChange() {
    val settings = mutableStateOf(AppSettings())
    rule.setContent { BiliTvTheme { UiSizeScope(settings.value.uiSize) {
      Box(Modifier.requiredSize(380.dp, 720.dp)) { RenderSettings(settings.value) }
    } } }
    rule.onNodeWithTag("settings_ui_size").performScrollTo()
      .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
    rule.onNodeWithTag("settings_ui_size").performKeyInput { pressKey(Key.DirectionRight) }
    val firstChip = "settings_home_section_" + HomeSection.DefaultOrder.first().key
    rule.onNodeWithTag(firstChip).assertIsFocused()
    rule.runOnIdle { settings.value = settings.value.copy(uiSize = AppUiSize.Large) }
    rule.onNodeWithTag(firstChip).assertIsFocused()
    rule.onNodeWithTag(firstChip).performKeyInput { pressKey(Key.DirectionLeft) }
    rule.onNodeWithTag("settings_ui_size").assertIsFocused()
  }

  @Test fun narrowAccountProfileKeepsIdentityTextReadableInsteadOfZeroWidth() {
    rule.setContent { BiliTvTheme { UiSizeScope(AppUiSize.Large) {
      Box(Modifier.requiredSize(220.dp, 600.dp)) {
        AccountProfileCard(UserSession(mid = 12345, uname = "长账号名称用于省略", isVip = true))
      }
    } } }
    val text = layout("长账号名称用于省略")
    assertTrue(text.size.width > 0)
    assertFalse(text.didOverflowHeight)
    assertEquals(1, text.lineCount)
  }

  @Test fun systemSettingsLogOpensAndRestoresTheLogRowFocus() {
    rule.setContent { BiliTvTheme {
      val context = LocalContext.current
      val logSettings = remember(context) { AppSettingsStore(context.applicationContext) }
      RenderSettings(AppSettings(), logSettings = logSettings)
    } }
    rule.onNodeWithTag("settings_behavior_list").performScrollToIndex(22)
    val row = rule.onNodeWithTag("settings_playback_log")
    row.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
    row.performKeyInput { pressKey(Key.DirectionCenter) }
    rule.onNodeWithTag("playback_log_dialog").assertExists()
    rule.onNodeWithTag("playback_log_back").performClick()
    row.assertIsFocused()
  }

  @Test fun largePortraitLoginQrRemainsSquareInsideAvailableWidth() {
    rule.setContent { BiliTvTheme { UiSizeScope(AppUiSize.Large) {
      Box(Modifier.requiredSize(180.dp, 400.dp).testTag("viewport")) { QrContent(QrLoginState.Loading) }
    } } }
    val viewport = rule.onNodeWithTag("viewport").getUnclippedBoundsInRoot()
    val qr = rule.onNodeWithTag("login_qr_container").getUnclippedBoundsInRoot()
    assertEquals(qr.right.value - qr.left.value, qr.bottom.value - qr.top.value, 1f)
    assertTrue(qr.left >= viewport.left && qr.right <= viewport.right)
  }

  @Test fun remoteKeyboardCanScrollAndSearchInNarrowLargeWindow() {
    var searched = ""
    rule.setContent { BiliTvTheme { UiSizeScope(AppUiSize.Large) {
      Box(Modifier.requiredSize(380.dp, 720.dp)) {
        SearchKeyboardView("TEST", emptyList(), emptyList(), FocusRequester(), { true }, {}, {}, { searched = it })
      }
    } } }
    rule.onNodeWithText("搜索", useUnmergedTree = true).performScrollTo().performClick()
    rule.runOnIdle { assertEquals("TEST", searched) }
  }

  @Composable
  private fun RenderSettings(
    settings: AppSettings,
    logSettings: AppSettingsStore? = null,
    onUiSizeChange: (AppUiSize) -> Unit = {},
  ) {
        SettingsScreen(settings, "0 MB", CodecCapability(true, false, false), FocusRequester(),
          onMoveLeftToNav = { true }, onUiSizeChange = onUiSizeChange,
          onVisualPerformanceModeChange = {}, liquidGlassCardsSupported = false,
          onLiquidGlassCardsEnabledChange = {}, onHomeThemeVariantChange = {}, onChineseTextVariantChange = {},
          onClearCache = {}, onSeekPreviewSpritesEnabledChange = {}, onPlaybackQualityPreferenceChange = {},
          onPlaybackCodecPreferenceChange = {}, onAirJumpAssistantEnabledChange = {}, onConfirmPlaybackExitChange = {},
          onAutoPlayNextEpisodeChange = {}, onAutoPlayRelatedVideoChange = {}, onAutoReturnHomeOnCompletionChange = {},
          onShowClockChange = {}, onShowMiniProgressBarChange = {}, onAutoConfirmOnFocusChange = {},
          onAutoRefreshOnSwitchChange = {}, onHomeSectionEnabledChange = { _, _ -> }, playbackLogSettings = logSettings)
  }

  private fun layout(text: String): TextLayoutResult {
    val results = mutableListOf<TextLayoutResult>()
    rule.onNodeWithText(text, useUnmergedTree = true)
      .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
    return results.single()
  }
}
