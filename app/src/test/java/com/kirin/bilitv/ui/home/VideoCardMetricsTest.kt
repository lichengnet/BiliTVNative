package com.kirin.bilitv.ui.home

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.kirin.bilitv.ui.theme.BiliSizing
import com.kirin.bilitv.ui.theme.BiliSpacing
import com.kirin.bilitv.ui.theme.BiliTypography
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoCardMetricsTest {
  @Test fun defaultMetricsMatchExistingTokens() {
    val metrics = VideoCardMetrics()
    assertEquals(BiliSizing.VideoTextHeight, metrics.textHeight)
    assertEquals(BiliTypography.CardTitle, metrics.titleFontSize)
    assertEquals(BiliTypography.CardTitleLineHeight, metrics.titleLineHeight)
    assertEquals(BiliTypography.CardMeta, metrics.metaFontSize)
    assertEquals(BiliTypography.CardMetaLineHeight, metrics.metaLineHeight)
    assertEquals(BiliSizing.OwnerAvatarSize, metrics.avatarSize)
    assertEquals(BiliSizing.VideoProgressBarHeight, metrics.progressBarHeight)
  }

  @Test fun enlargedTitleMetadataAndInfoUseFourThirds() {
    val metrics = VideoCardMetrics(4f / 3f)
    assertEquals(18.666666f, metrics.titleFontSize.value, 0.001f)
    assertEquals(16f, metrics.metaFontSize.value, 0.001f)
    assertEquals(16f, metrics.overlayFontSize.value, 0.001f)
    assertEquals(82.666666f, metrics.textHeight.value, 0.001f)
    assertEquals(BiliTypography.CardTitleLineHeight * (4f / 3f), metrics.titleLineHeight)
    assertEquals(BiliTypography.CardMetaLineHeight * (4f / 3f), metrics.metaLineHeight)
    assertEquals(BiliTypography.CardOverlayLineHeight * (4f / 3f), metrics.overlayLineHeight)
  }

  @Test fun allRemainingContentDimensionsScaleTogether() {
    val scale = 4f / 3f
    val metrics = VideoCardMetrics(scale)
    assertEquals(BiliSizing.OwnerAvatarSize * scale, metrics.avatarSize)
    assertEquals(BiliSizing.VideoOverlayIconSize * scale, metrics.overlayIconSize)
    assertEquals(BiliSizing.VideoBadgeMinHeight * scale, metrics.badgeHeight)
    assertEquals(BiliTypography.CardBadge * scale, metrics.badgeFontSize)
    assertEquals(BiliTypography.CardBadgeLineHeight * scale, metrics.badgeLineHeight)
    assertEquals(BiliTypography.Body * scale, metrics.failureFontSize)
    assertEquals(BiliSizing.VideoCoverGradientHeight * scale, metrics.scrimHeight)
    assertEquals(BiliSizing.VideoProgressBarHeight * scale, metrics.progressBarHeight)
    assertEquals(BiliSpacing.Sm * scale, metrics.smallSpacing)
    assertEquals(BiliSpacing.Xs * scale, metrics.extraSmallSpacing)
    assertEquals(BiliSpacing.Md * scale, metrics.skeletonLineHeight)
    assertEquals(BiliSizing.TitleMarqueeVelocity * scale, metrics.marqueeVelocity)
    assertEquals(BiliSizing.VideoCardMinHeight * scale, metrics.fallbackHeight)
  }

  @Test fun failureTextScalesInheritedLineHeightAndPreservesDefault() {
    assertEquals(24.sp, VideoCardMetrics().scaledInheritedLineHeight(24.sp))
    assertEquals(32.sp, VideoCardMetrics(4f / 3f).scaledInheritedLineHeight(24.sp))
  }

  @Test fun unspecifiedLineHeightRemainsUnspecified() {
    assertEquals(TextUnit.Unspecified, VideoCardMetrics(4f / 3f).scaledInheritedLineHeight(TextUnit.Unspecified))
  }

  @Test(expected = IllegalArgumentException::class)
  fun zeroScaleIsRejected() { VideoCardMetrics(0f) }

  @Test(expected = IllegalArgumentException::class)
  fun infiniteScaleIsRejected() { VideoCardMetrics(Float.POSITIVE_INFINITY) }
}
