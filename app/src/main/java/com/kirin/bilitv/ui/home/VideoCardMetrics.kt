package com.kirin.bilitv.ui.home

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.TextUnit
import com.kirin.bilitv.ui.theme.BiliSizing
import com.kirin.bilitv.ui.theme.BiliSpacing
import com.kirin.bilitv.ui.theme.BiliTypography

internal class VideoCardMetrics(val contentScale: Float = 1f) {
  init { require(contentScale.isFinite() && contentScale > 0f) }

  val textHeight = BiliSizing.VideoTextHeight * contentScale
  val titleFontSize = BiliTypography.CardTitle * contentScale
  val titleLineHeight = BiliTypography.CardTitleLineHeight * contentScale
  val metaFontSize = BiliTypography.CardMeta * contentScale
  val metaLineHeight = BiliTypography.CardMetaLineHeight * contentScale
  val overlayFontSize = BiliTypography.CardOverlay * contentScale
  val overlayLineHeight = BiliTypography.CardOverlayLineHeight * contentScale
  val badgeFontSize = BiliTypography.CardBadge * contentScale
  val badgeLineHeight = BiliTypography.CardBadgeLineHeight * contentScale
  val failureFontSize = BiliTypography.Body * contentScale
  val avatarSize = BiliSizing.OwnerAvatarSize * contentScale
  val overlayIconSize = BiliSizing.VideoOverlayIconSize * contentScale
  val badgeHeight = BiliSizing.VideoBadgeMinHeight * contentScale
  val scrimHeight = BiliSizing.VideoCoverGradientHeight * contentScale
  val progressBarHeight = BiliSizing.VideoProgressBarHeight * contentScale
  val smallSpacing = BiliSpacing.Sm * contentScale
  val extraSmallSpacing = BiliSpacing.Xs * contentScale
  val skeletonLineHeight = BiliSpacing.Md * contentScale
  val marqueeVelocity = BiliSizing.TitleMarqueeVelocity * contentScale
  fun scaledInheritedLineHeight(lineHeight: TextUnit): TextUnit =
    if (lineHeight.isSp) lineHeight * contentScale else lineHeight

  // An estimate for focus scrolling only, never a fixed card layout height.
  val fallbackHeight = BiliSizing.VideoCardMinHeight * contentScale
}

internal val LocalVideoCardMetrics = staticCompositionLocalOf { VideoCardMetrics() }
