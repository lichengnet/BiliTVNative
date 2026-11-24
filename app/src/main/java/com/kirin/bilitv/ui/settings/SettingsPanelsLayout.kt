package com.kirin.bilitv.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kirin.bilitv.ui.common.AdaptiveTwoPaneLayout
import com.kirin.bilitv.ui.theme.BiliSizing
import com.kirin.bilitv.ui.theme.BiliSpacing
import com.kirin.bilitv.ui.theme.LocalUiMetrics

@Composable
internal fun SettingsPanelsLayout(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  val ui = LocalUiMetrics.current
  AdaptiveTwoPaneLayout(minimumPrimaryWidth = ui.dp(BiliSizing.SettingsPanelPreferredMinWidth),
    minimumSecondaryWidth = ui.dp(BiliSizing.SettingsPanelPreferredMinWidth), spacing = ui.dp(BiliSpacing.Xl),
    modifier = modifier, content = content)
}
