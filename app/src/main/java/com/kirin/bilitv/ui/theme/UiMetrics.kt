package com.kirin.bilitv.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.kirin.bilitv.core.settings.AppUiSize

internal class UiMetrics(val size: AppUiSize = AppUiSize.Medium, val active: Boolean = false) {
  val scale = when (size) {
    AppUiSize.Small -> BiliSizing.UiSizeSmallScale
    AppUiSize.Medium -> BiliSizing.UiSizeMediumScale
    AppUiSize.Large -> BiliSizing.UiSizeLargeScale
  }
  fun dp(value: Dp): Dp = value * scale
  fun sp(value: TextUnit): TextUnit = if (value.isSp) value * scale else value
}

internal val LocalUiMetrics = staticCompositionLocalOf { UiMetrics() }

@Composable
internal fun UiSizeScope(size: AppUiSize, content: @Composable () -> Unit) {
  val metrics = remember(size) { UiMetrics(size, active = true) }
  val baseTextStyle = LocalTextStyle.current
  CompositionLocalProvider(
    LocalUiMetrics provides metrics,
    LocalTextStyle provides baseTextStyle.copy(fontSize = metrics.sp(baseTextStyle.fontSize), lineHeight = metrics.sp(baseTextStyle.lineHeight)),
    content = content,
  )
}

@Composable
internal fun uiDp(value: Dp): Dp = LocalUiMetrics.current.dp(value)

@Composable
internal fun uiSp(value: TextUnit): TextUnit = LocalUiMetrics.current.sp(value)
