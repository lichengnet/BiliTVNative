package com.kirin.bilitv.ui.settings

import com.kirin.bilitv.ui.theme.LocalUiMetrics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.kirin.bilitv.R
import com.kirin.bilitv.core.i18n.ChineseTextVariant
import com.kirin.bilitv.core.player.CodecCapability
import com.kirin.bilitv.core.player.PlaybackCodecPreference
import com.kirin.bilitv.core.player.PlaybackQualityPreference
import com.kirin.bilitv.core.settings.AppUiSize
import com.kirin.bilitv.core.settings.AppVisualPerformanceMode
import com.kirin.bilitv.core.settings.HomeThemeVariant
import com.kirin.bilitv.ui.focus.BiliFocusableSurface
import com.kirin.bilitv.ui.theme.BiliRadius
import com.kirin.bilitv.ui.theme.BiliSizing
import com.kirin.bilitv.ui.theme.BiliSpacing
import com.kirin.bilitv.ui.theme.BiliTypography
import com.kirin.bilitv.ui.theme.LocalHomeColors

@Composable
internal fun SettingsOptionRow(
  title: String,
  description: String,
  value: String,
  modifier: Modifier = Modifier,
  onFocused: () -> Unit = {},
  onClick: () -> Unit,
) {
  val ui = LocalUiMetrics.current
  val homeColors = LocalHomeColors.current
  BiliFocusableSurface(
    scaleOnFocus = false,
    shadowOnFocus = false,
    shape = RoundedCornerShape(ui.dp(BiliRadius.Panel)),
    onClick = onClick,
    onFocused = onFocused,
    modifier = modifier.fillMaxWidth().heightIn(min = ui.dp(BiliSizing.SettingsRowHeight)),
  ) {
    SettingsRowContent(title, description) { inline ->
      if (value.isNotEmpty()) Text(
        text = value,
        color = homeColors.accent,
        fontSize = ui.sp(BiliTypography.Body),
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.End,
        modifier = if (inline) Modifier.width(ui.dp(BiliSizing.SettingsCodecValueWidth)) else Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
internal fun SettingsActionRow(
  title: String,
  description: String,
  value: String,
  modifier: Modifier = Modifier,
  onFocused: () -> Unit = {},
  onClick: () -> Unit,
) = SettingsOptionRow(title, description, value, modifier, onFocused, onClick)

@Composable
private fun SettingsRowContent(
  title: String,
  description: String,
  enabled: Boolean = true,
  trailing: @Composable (Boolean) -> Unit,
) {
  val ui = LocalUiMetrics.current
  val homeColors = LocalHomeColors.current
  BoxWithConstraints(modifier = Modifier.fillMaxWidth().heightIn(min = ui.dp(BiliSizing.SettingsRowHeight))) {
    val inline = maxWidth >= ui.dp(BiliSizing.SettingsRowInlineMinWidth)
    val label: @Composable (Modifier) -> Unit = { labelModifier ->
      Column(modifier = labelModifier) {
        Text(title, color = if (enabled) homeColors.textPrimary else homeColors.textTertiary,
          fontSize = ui.sp(BiliTypography.Body), fontWeight = FontWeight.Bold)
        Text(description, color = if (enabled) homeColors.textSecondary else homeColors.textTertiary,
          fontSize = ui.sp(BiliTypography.BodySmall), modifier = Modifier.padding(top = ui.dp(BiliSpacing.Xs)))
      }
    }
    if (inline) {
      Row(modifier = Modifier.fillMaxWidth().align(Alignment.Center)
        .padding(horizontal = ui.dp(BiliSpacing.Lg), vertical = ui.dp(BiliSpacing.Md)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(ui.dp(BiliSpacing.Lg))) {
        label(Modifier.weight(1f))
        trailing(true)
      }
    } else {
      Column(modifier = Modifier.fillMaxWidth().align(Alignment.Center)
        .padding(horizontal = ui.dp(BiliSpacing.Lg), vertical = ui.dp(BiliSpacing.Md)),
        horizontalAlignment = Alignment.End,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(ui.dp(BiliSpacing.Sm))) {
        label(Modifier.fillMaxWidth())
        trailing(false)
      }
    }
  }
}

@Composable
internal fun SettingsToggleRow(
  title: String,
  description: String,
  checked: Boolean,
  enabled: Boolean = true,
  modifier: Modifier = Modifier,
  onFocused: () -> Unit = {},
  onCheckedChange: (Boolean) -> Unit,
) {
  val ui = LocalUiMetrics.current
  val homeColors = LocalHomeColors.current
  BiliFocusableSurface(
    scaleOnFocus = false,
    shadowOnFocus = false,
    shape = RoundedCornerShape(ui.dp(BiliRadius.Panel)),
    onClick = { if (enabled) onCheckedChange(!checked) },
    onFocused = onFocused,
    modifier = modifier.fillMaxWidth().heightIn(min = ui.dp(BiliSizing.SettingsRowHeight))
      .semantics {
        role = Role.Switch
        toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
        if (!enabled) disabled()
      },
  ) {
    SettingsRowContent(title, description, enabled) {
      Box(modifier = Modifier.size(ui.dp(BiliSizing.ToggleTrackWidth), ui.dp(BiliSizing.ToggleTrackHeight))
        .background(if (enabled && checked) homeColors.accent else homeColors.glassSurfaceStrong, RoundedCornerShape(ui.dp(BiliRadius.Pill)))
        .padding(ui.dp(BiliSpacing.Xs)),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart) {
        Box(modifier = Modifier.size(ui.dp(BiliSizing.ToggleThumbSize))
          .background(if (!enabled) homeColors.textTertiary else homeColors.textPrimary, RoundedCornerShape(ui.dp(BiliRadius.Pill))))
      }
    }
  }
}

internal fun CodecCapability.playbackCodecOptions(): List<PlaybackCodecPreference> {
  return buildList {
    add(PlaybackCodecPreference.Auto)
    if (supportsAv1) add(PlaybackCodecPreference.Av1)
    if (supportsH265) add(PlaybackCodecPreference.H265)
    if (supportsH264) add(PlaybackCodecPreference.H264)
  }
}

@Composable
internal fun ChineseTextVariant.languageLabel(): String {
  return when (this) {
    ChineseTextVariant.Simplified -> stringResource(R.string.settings_language_simplified)
    ChineseTextVariant.HongKong -> stringResource(R.string.settings_language_hong_kong)
    ChineseTextVariant.Taiwan -> stringResource(R.string.settings_language_taiwan)
  }
}

@Composable
internal fun AppVisualPerformanceMode.visualPerformanceLabel(): String {
  return when (this) {
    AppVisualPerformanceMode.Smooth -> stringResource(R.string.settings_visual_performance_smooth)
    AppVisualPerformanceMode.Balanced -> stringResource(R.string.settings_visual_performance_balanced)
    AppVisualPerformanceMode.Refined -> stringResource(R.string.settings_visual_performance_refined)
  }
}

@Composable
internal fun HomeThemeVariant.homeThemeLabel(): String {
  return when (this) {
    HomeThemeVariant.Pink -> stringResource(R.string.settings_home_theme_pink)
    HomeThemeVariant.Black -> stringResource(R.string.settings_home_theme_black)
    HomeThemeVariant.Gray -> stringResource(R.string.settings_home_theme_gray)
    HomeThemeVariant.BlueGray -> stringResource(R.string.settings_home_theme_blue_gray)
  }
}

@Composable
internal fun PlaybackQualityPreference.qualityLabel(): String {
  return when (this) {
    PlaybackQualityPreference.Highest -> stringResource(R.string.settings_playback_quality_highest)
    PlaybackQualityPreference.Q1080 -> stringResource(R.string.settings_playback_quality_1080)
    PlaybackQualityPreference.Q720 -> stringResource(R.string.settings_playback_quality_720)
    PlaybackQualityPreference.Q480 -> stringResource(R.string.settings_playback_quality_480)
  }
}

@Composable
internal fun PlaybackCodecPreference.codecLabel(): String {
  return when (this) {
    PlaybackCodecPreference.Auto -> stringResource(R.string.settings_playback_codec_auto)
    PlaybackCodecPreference.H264 -> stringResource(R.string.settings_playback_codec_h264)
    PlaybackCodecPreference.H265 -> stringResource(R.string.settings_playback_codec_h265)
    PlaybackCodecPreference.Av1 -> stringResource(R.string.settings_playback_codec_av1)
  }
}

@Composable
internal fun AppUiSize.uiSizeLabel(): String = stringResource(when (this) {
  AppUiSize.Small -> R.string.settings_ui_size_small
  AppUiSize.Medium -> R.string.settings_ui_size_medium
  AppUiSize.Large -> R.string.settings_ui_size_large
})

@Composable
internal fun UiSizeSettingsRow(
  size: AppUiSize,
  onUiSizeChange: (AppUiSize) -> Unit,
  modifier: Modifier = Modifier,
  onFocused: () -> Unit = {},
) {
  SettingsOptionRow(
    title = stringResource(R.string.settings_ui_size_title),
    description = stringResource(R.string.settings_ui_size_description),
    value = size.uiSizeLabel(),
    modifier = modifier,
    onFocused = onFocused,
    onClick = { onUiSizeChange(size.next()) },
  )
}
