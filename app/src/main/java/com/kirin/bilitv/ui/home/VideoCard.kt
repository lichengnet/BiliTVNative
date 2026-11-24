package com.kirin.bilitv.ui.home

import com.kirin.bilitv.ui.theme.UiMetrics
import com.kirin.bilitv.ui.theme.LocalUiMetrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.kirin.bilitv.R
import com.kirin.bilitv.core.device.CompatibilityDiagnostics
import com.kirin.bilitv.core.image.imageFailureDiagnosticCode
import com.kirin.bilitv.core.image.buildOwnerAvatarRequest
import com.kirin.bilitv.core.image.buildVideoThumbnailRequest
import com.kirin.bilitv.core.model.VideoCardRelativeText
import com.kirin.bilitv.core.model.VideoSummary
import com.kirin.bilitv.core.model.durationText
import com.kirin.bilitv.core.model.isWatchCompleted
import com.kirin.bilitv.core.model.pubdateText
import com.kirin.bilitv.core.model.viewAtText
import com.kirin.bilitv.core.model.watchProgressRatio
import com.kirin.bilitv.core.model.watchProgressText
import com.kirin.bilitv.ui.focus.BiliFocusableSurface
import com.kirin.bilitv.ui.glass.LocalLiquidGlassBackdrop
import com.kirin.bilitv.ui.i18n.convertChineseText
import com.kirin.bilitv.ui.settings.LocalBiliPerformancePolicy
import com.kirin.bilitv.ui.theme.BiliColors
import com.kirin.bilitv.ui.theme.BiliFocus
import com.kirin.bilitv.ui.theme.BiliMotion
import com.kirin.bilitv.ui.theme.BiliRadius
import com.kirin.bilitv.ui.theme.BiliSizing
import com.kirin.bilitv.ui.theme.LocalHomeColors
import com.kirin.bilitv.ui.transition.playbackSharedBounds
import com.kirin.bilitv.ui.transition.playbackSharedTransitionKey
import kotlinx.coroutines.delay

enum class VideoCardMode {
  Standard,
  Dynamic,
  History,
}

@Composable
fun VideoCard(
  video: VideoSummary,
  modifier: Modifier = Modifier,
  mode: VideoCardMode = VideoCardMode.Standard,
  interactionPaused: Boolean = false,
  onClick: () -> Unit = {},
  onFocused: () -> Unit = {},
  contentScale: Float = LocalUiMetrics.current.scale,
) {
  val metrics = remember(contentScale) { VideoCardMetrics(contentScale) }
  CompositionLocalProvider(LocalVideoCardMetrics provides metrics) {
    VideoCardContent(video, modifier, mode, interactionPaused, onClick, onFocused)
  }
}

@Composable
private fun VideoCardContent(
  video: VideoSummary,
  modifier: Modifier,
  mode: VideoCardMode,
  interactionPaused: Boolean,
  onClick: () -> Unit,
  onFocused: () -> Unit,
) {
  val ui = LocalUiMetrics.current
  val metrics = LocalVideoCardMetrics.current
  var focused by remember { mutableStateOf(false) }
  val performancePolicy = LocalBiliPerformancePolicy.current
  val homeColors = LocalHomeColors.current
  val focusEffectsEnabled = performancePolicy.motionEnabled
  val cinematicEffectsEnabled = performancePolicy.cinematicVisualEffectsEnabled
  val liquidGlassBackdrop = LocalLiquidGlassBackdrop.current
  val liquidGlassCardsEnabled = cinematicEffectsEnabled && performancePolicy.liquidGlassCardsEnabled
  val liquidGlassActive = liquidGlassCardsEnabled && liquidGlassBackdrop != null
  val cardSurface = if (liquidGlassActive) {
    homeColors.cardSurface.copy(alpha = BiliFocus.LiquidGlassCardSurfaceAlpha)
  } else {
    homeColors.cardSurface
  }
  val cardInfoBackground = if (liquidGlassActive) {
    homeColors.cardInfoSurface.copy(alpha = BiliFocus.LiquidGlassCardInfoSurfaceAlpha)
  } else {
    homeColors.cardInfoSurface
  }
  val title = convertChineseText(video.title)
  val titleColor = if (focusEffectsEnabled) {
    animateColorAsState(
      targetValue = if (focused) homeColors.textPrimary else homeColors.textSecondary,
      animationSpec = tween(BiliMotion.FocusMs, easing = BiliMotion.FocusEasing),
      label = "videoCardTitleColor",
    ).value
  } else {
    if (focused) homeColors.textPrimary else homeColors.textSecondary
  }

  BiliFocusableSurface(
    modifier = modifier
      .testTag("video_card_${video.bvid}")
      .fillMaxWidth()
      .then(
        if (liquidGlassActive) {
          Modifier.padding(ui.dp(BiliFocus.LiquidGlassCardFocusPadding))
        } else {
          Modifier
        },
      ),
    scaleOnFocus = focusEffectsEnabled,
    shadowOnFocus = false,
    focusedScale = if (cinematicEffectsEnabled) {
      BiliFocus.CinematicCardScale
    } else {
      BiliFocus.CardScale
    },
    focusedBorderColor = if (liquidGlassActive) {
      BiliColors.Transparent
    } else if (cinematicEffectsEnabled) {
      BiliColors.TextPrimary.copy(alpha = BiliFocus.CinematicCardGlassBorderAlpha)
    } else {
      null
    },
    restingBorderColor = if (liquidGlassActive) {
      BiliColors.Transparent
    } else if (cinematicEffectsEnabled) {
      homeColors.textPrimary.copy(alpha = BiliFocus.CinematicCardRestingBorderAlpha)
    } else {
      null
    },
    focusedBorderWidth = if (cinematicEffectsEnabled) {
      if (liquidGlassActive) {
        ui.dp(BiliFocus.LiquidGlassCardBorderWidth)
      } else {
        ui.dp(BiliFocus.RestingBorderWidth)
      }
    } else {
      ui.dp(BiliFocus.BorderWidth)
    },
    restingBorderWidth = if (liquidGlassActive) {
      ui.dp(BiliFocus.LiquidGlassCardBorderWidth)
    } else if (cinematicEffectsEnabled) {
      ui.dp(BiliFocus.LiquidGlassCardBorderWidth)
    } else {
      ui.dp(BiliFocus.RestingBorderWidth)
    },
    focusedLift = if (cinematicEffectsEnabled) {
      ui.dp(BiliFocus.CinematicCardLift)
    } else {
      ui.dp(BiliFocus.RestingLift)
    },
    restingBackgroundColor = if (cinematicEffectsEnabled) {
      cardSurface
    } else {
      null
    },
    focusedBackgroundColor = if (cinematicEffectsEnabled) {
      cardSurface
    } else {
      null
    },
    onClick = onClick,
    onFocused = onFocused,
    onFocusChanged = { focused = it },
    focusedForeground = when {
      cinematicEffectsEnabled && liquidGlassActive -> {
        { drawLiquidGlassCardFocusedOutline(ui) }
      }
      cinematicEffectsEnabled -> {
        { drawCinematicCardGlassBorder(ui) }
      }
      else -> null
    },
  ) {
    Box {
      Column {
        VideoCover(
          video = video,
          mode = mode,
          focused = focused,
          focusEffectsEnabled = focusEffectsEnabled,
          interactionPaused = interactionPaused,
        )
        Column(
          modifier = Modifier
            .testTag("video_info_${video.bvid}")
            .heightIn(min = metrics.textHeight)
            .background(cardInfoBackground)
            .padding(horizontal = metrics.smallSpacing, vertical = metrics.extraSmallSpacing),
        ) {
          Text(
            text = title,
            color = titleColor,
            fontSize = metrics.titleFontSize,
            lineHeight = metrics.titleLineHeight,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = if (focused) TextOverflow.Clip else TextOverflow.Ellipsis,
            modifier = Modifier
              .fillMaxWidth()
              .then(
                if (focused && !interactionPaused) {
                  Modifier.basicMarquee(
                    iterations = Int.MAX_VALUE,
                    repeatDelayMillis = BiliMotion.TitleMarqueeRepeatDelayMs,
                    initialDelayMillis = BiliMotion.TitleMarqueeInitialDelayMs,
                    velocity = metrics.marqueeVelocity,
                  )
                } else {
                  Modifier
                },
              ),
          )
          Spacer(modifier = Modifier.weight(1f))
          MetadataRow(
            video = video,
            mode = mode,
            focused = focused,
            focusEffectsEnabled = focusEffectsEnabled,
          )
        }
      }
      if (focused && cinematicEffectsEnabled) {
        CinematicCardShine(
          interactionPaused = interactionPaused,
          modifier = Modifier.matchParentSize(),
        )
      }
    }
  }
}

private fun DrawScope.drawCinematicCardGlassBorder(ui: UiMetrics) {
  val radius = ui.dp(BiliRadius.Card).toPx()
  val outerWidth = ui.dp(BiliFocus.CinematicCardGlassOuterWidth).toPx()
  val outerInset = outerWidth / 2f
  drawRoundRect(
    brush = Brush.linearGradient(
      colors = listOf(
        BiliColors.TextPrimary.copy(alpha = BiliFocus.CinematicCardGlassOuterSheenAlpha),
        BiliColors.TextPrimary.copy(alpha = BiliFocus.CinematicCardGlassOuterAlpha),
        BiliColors.TextPrimary.copy(alpha = BiliFocus.CinematicCardGlassOuterSheenAlpha),
        BiliColors.TextPrimary.copy(alpha = BiliFocus.CinematicCardGlassOuterAlpha),
      ),
      start = Offset.Zero,
      end = Offset(size.width, size.height),
    ),
    topLeft = Offset(outerInset, outerInset),
    size = Size(
      width = (size.width - outerWidth).coerceAtLeast(0f),
      height = (size.height - outerWidth).coerceAtLeast(0f),
    ),
    cornerRadius = CornerRadius(radius, radius),
    style = Stroke(width = outerWidth),
  )

  val innerWidth = ui.dp(BiliFocus.CinematicCardGlassInnerWidth).toPx()
  val innerInset = ui.dp(BiliFocus.CinematicCardGlassInnerInset).toPx() + innerWidth / 2f
  val innerRadius = (radius - innerInset).coerceAtLeast(0f)
  drawRoundRect(
    brush = Brush.linearGradient(
      colors = listOf(
        BiliColors.TextPrimary.copy(alpha = BiliFocus.CinematicCardGlassInnerSheenAlpha),
        BiliColors.TextPrimary.copy(alpha = BiliFocus.CinematicCardGlassInnerAlpha),
        BiliColors.TextPrimary.copy(alpha = BiliFocus.CinematicCardGlassInnerSheenAlpha),
        BiliColors.TextPrimary.copy(alpha = BiliFocus.CinematicCardGlassInnerAlpha),
      ),
      start = Offset.Zero,
      end = Offset(size.width, size.height),
    ),
    topLeft = Offset(innerInset, innerInset),
    size = Size(
      width = (size.width - innerInset * 2f).coerceAtLeast(0f),
      height = (size.height - innerInset * 2f).coerceAtLeast(0f),
    ),
    cornerRadius = CornerRadius(innerRadius, innerRadius),
    style = Stroke(width = innerWidth),
  )
}

private fun DrawScope.drawLiquidGlassCardFocusedOutline(ui: UiMetrics) {
  val outlineOutset = ui.dp(BiliFocus.LiquidGlassCardFocusPadding).toPx()
  val bandWidth = outlineOutset
  val bandInset = bandWidth / 2f
  val radius = ui.dp(BiliRadius.Card).toPx() + bandInset
  val borderBrush = Brush.linearGradient(
    colors = listOf(
      BiliColors.TextPrimary.copy(alpha = BiliFocus.LiquidGlassCardFocusedOuterAlpha),
      BiliColors.TextPrimary.copy(alpha = BiliFocus.LiquidGlassCardFocusedOuterDimAlpha),
      BiliColors.TextPrimary.copy(alpha = BiliFocus.LiquidGlassCardFocusedOuterAlpha),
      BiliColors.TextPrimary.copy(alpha = BiliFocus.LiquidGlassCardFocusedOuterDimAlpha),
    ),
    start = Offset(-outlineOutset, -outlineOutset),
    end = Offset(size.width + outlineOutset, size.height + outlineOutset),
  )
  drawRoundRect(
    brush = borderBrush,
    topLeft = Offset(-bandInset, -bandInset),
    size = Size(
      width = size.width + bandWidth,
      height = size.height + bandWidth,
    ),
    cornerRadius = CornerRadius(radius, radius),
    style = Stroke(width = bandWidth),
  )
}

@Composable
private fun MetadataRow(
  video: VideoSummary,
  mode: VideoCardMode,
  focused: Boolean,
  focusEffectsEnabled: Boolean,
) {
  val metrics = LocalVideoCardMetrics.current
  val homeColors = LocalHomeColors.current
  val ownerName = convertChineseText(video.ownerName)
  val relativeText = rememberVideoCardRelativeText()
  val trailingText = when (mode) {
    VideoCardMode.Standard,
    VideoCardMode.Dynamic -> video.pubdateText(relativeText)
    VideoCardMode.History -> video.viewAtText(relativeText)
  }
  val ownerColor = if (focusEffectsEnabled) {
    animateColorAsState(
      targetValue = if (focused) homeColors.textPrimary else homeColors.textSecondary,
      animationSpec = tween(BiliMotion.FocusMs, easing = BiliMotion.FocusEasing),
      label = "videoCardOwnerColor",
    ).value
  } else {
    homeColors.textSecondary
  }
  val trailingColor = if (focusEffectsEnabled) {
    animateColorAsState(
      targetValue = if (focused) homeColors.textSecondary else homeColors.textTertiary,
      animationSpec = tween(BiliMotion.FocusMs, easing = BiliMotion.FocusEasing),
      label = "videoCardTrailingColor",
    ).value
  } else {
    homeColors.textTertiary
  }

  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (video.ownerName.isNotBlank()) {
      OwnerAvatar(video)
      Spacer(modifier = Modifier.width(metrics.smallSpacing))
      Text(
        text = ownerName,
        color = ownerColor,
        fontSize = metrics.metaFontSize,
        lineHeight = metrics.metaLineHeight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
    } else {
      Spacer(modifier = Modifier.weight(1f))
    }

    if (trailingText.isNotBlank()) {
      Spacer(modifier = Modifier.width(metrics.smallSpacing))
      Text(
        text = trailingText,
        color = trailingColor,
        fontSize = metrics.metaFontSize,
        lineHeight = metrics.metaLineHeight,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun OwnerAvatar(video: VideoSummary) {
  val metrics = LocalVideoCardMetrics.current
  val performancePolicy = LocalBiliPerformancePolicy.current
  val homeColors = LocalHomeColors.current
  val fallbackPainter = ColorPainter(homeColors.glassSurfaceStrong)
  val modifier = Modifier
    .testTag("video_avatar_${video.bvid}")
    .size(metrics.avatarSize)
    .clip(CircleShape)
    .background(homeColors.glassSurfaceStrong)

  if (video.ownerFace.isBlank()) {
    Box(modifier = modifier)
    return
  }

  val context = LocalContext.current
  val request = remember(
    context,
    video.ownerFace,
    performancePolicy.ownerAvatarSizePx,
    performancePolicy.ownerAvatarRgb565Enabled,
    performancePolicy.imageMemoryCacheEnabled,
  ) {
    buildOwnerAvatarRequest(
      context = context,
      url = video.ownerFace,
      sizePx = performancePolicy.ownerAvatarSizePx,
      allowRgb565 = performancePolicy.ownerAvatarRgb565Enabled,
      memoryCacheEnabled = performancePolicy.imageMemoryCacheEnabled,
    )
  }

  AsyncImage(
    model = request,
    contentDescription = null,
    contentScale = ContentScale.Crop,
    placeholder = fallbackPainter,
    error = fallbackPainter,
    modifier = modifier,
  )
}

@Composable
private fun CinematicCardShine(
  interactionPaused: Boolean,
  modifier: Modifier = Modifier,
) {
  val homeColors = LocalHomeColors.current
  val shineProgress = remember { Animatable(1f) }

  LaunchedEffect(interactionPaused) {
    if (interactionPaused) {
      shineProgress.snapTo(1f)
    } else {
      delay(BiliMotion.FocusShineInitialDelayMs)
      while (true) {
        shineProgress.snapTo(0f)
        shineProgress.animateTo(
          targetValue = 1f,
          animationSpec = tween(BiliMotion.FocusShineMs, easing = BiliMotion.FocusEasing),
        )
        delay(BiliMotion.FocusShineRepeatDelayMs)
      }
    }
  }

  Box(
    modifier = modifier.drawBehind {
      if (!interactionPaused) {
        val hazeWidth = size.minDimension * BiliFocus.CinematicCardSheenHazeWidthFraction
        val coreWidth = size.minDimension * BiliFocus.CinematicCardSheenCoreWidthFraction
        val ridgeWidth = size.minDimension * BiliFocus.CinematicCardSheenRidgeWidthFraction
        val travel = size.width + size.height + hazeWidth * BiliFocus.CinematicCardSheenTravelPaddingMultiplier
        val sweep = shineProgress.value * travel - hazeWidth
        val start = Offset(sweep - size.height, size.height)
        val end = Offset(sweep, 0f)
        drawLine(
          color = homeColors.shineColor.copy(alpha = BiliFocus.CinematicCardSheenEdgeAlpha),
          start = start,
          end = end,
          strokeWidth = hazeWidth,
        )
        drawLine(
          color = homeColors.shineColor.copy(alpha = BiliFocus.CinematicCardSheenCenterAlpha),
          start = start,
          end = end,
          strokeWidth = coreWidth,
        )
        drawLine(
          color = homeColors.shineColor.copy(alpha = BiliFocus.CinematicCardSheenRidgeAlpha),
          start = start,
          end = end,
          strokeWidth = ridgeWidth,
        )
      }
    },
  )
}

@Composable
private fun VideoCover(
  video: VideoSummary,
  mode: VideoCardMode,
  focused: Boolean,
  focusEffectsEnabled: Boolean,
  interactionPaused: Boolean,
) {
  val ui = LocalUiMetrics.current
  val metrics = LocalVideoCardMetrics.current
  val context = LocalContext.current
  val inheritedLineHeight = LocalTextStyle.current.lineHeight
  val failureLineHeight = if (inheritedLineHeight.isSp) {
    metrics.scaledInheritedLineHeight(inheritedLineHeight) / ui.scale
  } else inheritedLineHeight
  val performancePolicy = LocalBiliPerformancePolicy.current
  val homeColors = LocalHomeColors.current
  val request = remember(
    context,
    video.pic,
    performancePolicy.videoThumbnailWidthPx,
    performancePolicy.videoThumbnailHeightPx,
    performancePolicy.videoThumbnailRgb565Enabled,
    performancePolicy.imageMemoryCacheEnabled,
  ) {
    buildVideoThumbnailRequest(
      context = context,
      url = video.pic,
      widthPx = performancePolicy.videoThumbnailWidthPx,
      heightPx = performancePolicy.videoThumbnailHeightPx,
      allowRgb565 = performancePolicy.videoThumbnailRgb565Enabled,
      memoryCacheEnabled = performancePolicy.imageMemoryCacheEnabled,
    )
  }
  val title = convertChineseText(video.title)
  val badge = convertChineseText(video.badge)
  val cinematicEffectsEnabled = performancePolicy.cinematicVisualEffectsEnabled
  val coverBackground = homeColors.glassSurfaceStrong
  val fallbackPainter = ColorPainter(coverBackground)
  var imageFailed by remember(request) { mutableStateOf(false) }
  var failureCode by remember(request) { mutableStateOf<String?>(null) }
  var blurFocused by remember { mutableStateOf(false) }
  LaunchedEffect(
    focused,
    focusEffectsEnabled,
    interactionPaused,
    performancePolicy.focusedCoverBlurEnabled,
    cinematicEffectsEnabled,
  ) {
    if (
      focused &&
      focusEffectsEnabled &&
      !interactionPaused &&
      performancePolicy.focusedCoverBlurEnabled &&
      !cinematicEffectsEnabled
    ) {
      delay(BiliMotion.FocusedCoverBlurDelayMs)
      blurFocused = true
    } else {
      blurFocused = false
    }
  }
  val highlightAlpha = if (focusEffectsEnabled) {
    animateFloatAsState(
      targetValue = if (focused) {
        if (cinematicEffectsEnabled) BiliFocus.CinematicCoverHighlightAlpha else BiliFocus.CoverHighlightAlpha
      } else {
        0f
      },
      animationSpec = tween(BiliMotion.FocusMs, easing = BiliMotion.FocusEasing),
      label = "videoCoverHighlight",
    ).value
  } else {
    0f
  }
  val blurAlpha = if (focusEffectsEnabled && performancePolicy.focusedCoverBlurEnabled) {
    animateFloatAsState(
      targetValue = if (blurFocused) BiliFocus.FocusedCoverBlurAlpha else 0f,
      animationSpec = tween(BiliMotion.FocusMs, easing = BiliMotion.FocusEasing),
      label = "videoCoverBlur",
    ).value
  } else {
    0f
  }
  val sharedKey = video.playbackSharedTransitionKey()

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .aspectRatio(BiliSizing.VideoThumbnailAspectRatio)
      .testTag("video_cover_${video.bvid}")
      .playbackSharedBounds(
        key = sharedKey,
        enabled = performancePolicy.motionEnabled,
      )
      .clip(RoundedCornerShape(ui.dp(BiliRadius.Card)))
      .background(coverBackground),
  ) {
    AsyncImage(
      model = request,
      contentDescription = title,
      onLoading = { imageFailed = false; failureCode = null },
      onSuccess = { imageFailed = false; failureCode = null },
      onError = { state ->
        imageFailed = true
        failureCode = if (CompatibilityDiagnostics.enabled) imageFailureDiagnosticCode(state.result.throwable) else null
      },
      contentScale = ContentScale.Crop,
      placeholder = fallbackPainter,
      error = fallbackPainter,
      modifier = Modifier.fillMaxSize(),
    )

    if (blurAlpha > 0f) {
      AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        placeholder = fallbackPainter,
        error = fallbackPainter,
        modifier = Modifier
          .fillMaxSize()
          .blur(ui.dp(BiliFocus.FocusedCoverBlurRadius))
          .graphicsLayer { alpha = blurAlpha },
      )
    }

    if (highlightAlpha > 0f) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(homeColors.textPrimary.copy(alpha = highlightAlpha)),
      )
    }

    if (focused && cinematicEffectsEnabled) {
      CinematicCoverPolish(
        modifier = Modifier.fillMaxSize(),
      )
    }

    if (badge.isNotBlank()) {
      VideoBadge(
        text = badge,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(metrics.extraSmallSpacing),
      )
    }

    BottomScrim(modifier = Modifier.align(Alignment.BottomCenter))

    when (mode) {
      VideoCardMode.Standard,
      VideoCardMode.Dynamic -> StandardCoverMetadata(
        video = video,
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(horizontal = metrics.smallSpacing, vertical = metrics.smallSpacing),
      )
      VideoCardMode.History -> HistoryCoverMetadata(
        video = video,
        modifier = Modifier.align(Alignment.BottomCenter),
      )
    }

    // Failure text must stay above the opaque blur placeholder and cover metadata.
    if (imageFailed) {
      Box(modifier = Modifier.fillMaxSize().background(coverBackground).padding(metrics.smallSpacing).testTag("video_failure_${video.bvid}"), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = stringResource(R.string.video_thumbnail_load_failed),
            color = homeColors.textPrimary,
            fontSize = metrics.failureFontSize,
            lineHeight = failureLineHeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          failureCode?.let { code ->
            Text(
              text = stringResource(R.string.video_thumbnail_diagnostic_code, code),
              color = homeColors.textPrimary,
              fontSize = metrics.metaFontSize,
              lineHeight = failureLineHeight,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }

  }
}

@Composable
private fun CinematicCoverPolish(
  modifier: Modifier = Modifier,
) {
  val homeColors = LocalHomeColors.current

  Box(
    modifier = modifier
      .background(
        Brush.verticalGradient(
          colors = listOf(
            BiliColors.Transparent,
            homeColors.textPrimary.copy(alpha = BiliFocus.CinematicFocusGlowAlpha * BiliFocus.CinematicCoverGlowAlphaMultiplier),
            BiliColors.VideoBlack.copy(alpha = BiliFocus.CinematicCoverVignetteAlpha),
          ),
        ),
      )
  )
}

@Composable
private fun VideoBadge(text: String, modifier: Modifier = Modifier) {
  val ui = LocalUiMetrics.current
  val metrics = LocalVideoCardMetrics.current
  val homeColors = LocalHomeColors.current
  Box(
    modifier = modifier
      .heightIn(min = metrics.badgeHeight)
      .clip(RoundedCornerShape(ui.dp(BiliRadius.Card)))
      .background(homeColors.accent)
      .padding(horizontal = metrics.smallSpacing),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = text,
      color = BiliColors.TextPrimary,
      fontSize = metrics.badgeFontSize,
      lineHeight = metrics.badgeLineHeight,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
    )
  }
}

@Composable
private fun BottomScrim(modifier: Modifier = Modifier) {
  val metrics = LocalVideoCardMetrics.current
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(metrics.scrimHeight)
      .background(
        Brush.verticalGradient(
          colors = listOf(
            BiliColors.OverlayTransparent,
            BiliColors.OverlayScrim,
          ),
        ),
      ),
  )
}

@Composable
private fun StandardCoverMetadata(video: VideoSummary, modifier: Modifier = Modifier) {
  val metrics = LocalVideoCardMetrics.current
  val homeColors = LocalHomeColors.current
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      VideoMetric(
        iconRes = R.drawable.ic_video_play_count,
        contentDescription = stringResource(R.string.video_play_count_content_description),
        text = video.view.formatCompactCountText(),
      )
      if (video.danmaku > 0) {
        Spacer(modifier = Modifier.width(metrics.smallSpacing))
        VideoMetric(
          iconRes = R.drawable.ic_video_danmaku_count,
          contentDescription = stringResource(R.string.video_danmaku_count_content_description),
          text = video.danmaku.formatCompactCountText(),
        )
      }
    }
    Text(
      text = video.durationText(),
      color = homeColors.textPrimary,
      fontSize = metrics.overlayFontSize,
      lineHeight = metrics.overlayLineHeight,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
    )
  }
}

@Composable
private fun VideoMetric(
  iconRes: Int,
  contentDescription: String,
  text: String,
) {
  val metrics = LocalVideoCardMetrics.current
  val homeColors = LocalHomeColors.current
  Row(
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      painter = painterResource(iconRes),
      contentDescription = contentDescription,
      tint = homeColors.textSecondary,
      modifier = Modifier.size(metrics.overlayIconSize),
    )
    Spacer(modifier = Modifier.width(metrics.extraSmallSpacing))
    Text(
      text = text,
      color = homeColors.textSecondary,
      fontSize = metrics.overlayFontSize,
      lineHeight = metrics.overlayLineHeight,
      maxLines = 1,
    )
  }
}

@Composable
private fun HistoryCoverMetadata(video: VideoSummary, modifier: Modifier = Modifier) {
  val metrics = LocalVideoCardMetrics.current
  val homeColors = LocalHomeColors.current
  val completedText = stringResource(R.string.video_watch_completed)
  Column(
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = metrics.smallSpacing),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val pageText = if (video.historyVideos > 1 && video.historyPage > 0) {
        "P${video.historyPage}"
      } else {
        ""
      }

      if (pageText.isNotBlank()) {
        HistoryOverlayBadge(
          text = pageText,
        )
        Spacer(modifier = Modifier.weight(1f))
      } else {
        Spacer(modifier = Modifier.weight(1f))
      }

      if (!video.isLive) {
        if (video.isWatchCompleted()) {
          HistoryOverlayBadge(text = completedText)
        } else {
          Text(
            text = video.watchProgressText(completedText),
            color = homeColors.textPrimary,
            fontSize = metrics.overlayFontSize,
            lineHeight = metrics.overlayLineHeight,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
          )
        }
      }
    }
    Spacer(modifier = Modifier.height(metrics.extraSmallSpacing))
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("video_progress_${video.bvid}")
        .height(metrics.progressBarHeight)
        .background(BiliColors.ProgressTrack),
    ) {
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .fillMaxWidth(video.watchProgressRatio())
          .background(homeColors.accent),
      )
    }
  }
}

@Composable
private fun HistoryOverlayBadge(text: String, modifier: Modifier = Modifier) {
  val ui = LocalUiMetrics.current
  val metrics = LocalVideoCardMetrics.current
  val homeColors = LocalHomeColors.current
  Box(
    modifier = modifier
      .heightIn(min = metrics.badgeHeight)
      .clip(RoundedCornerShape(ui.dp(BiliRadius.Card)))
      .background(homeColors.accent)
      .padding(horizontal = metrics.smallSpacing),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = text,
      color = BiliColors.TextPrimary,
      fontSize = metrics.badgeFontSize,
      lineHeight = metrics.badgeLineHeight,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
    )
  }
}

@Composable
private fun rememberVideoCardRelativeText(): VideoCardRelativeText {
  val viewedSuffixFormat = stringResource(R.string.video_viewed_suffix)
  val minutesAgoFormat = stringResource(R.string.video_relative_minutes_ago)
  val hoursAgoFormat = stringResource(R.string.video_relative_hours_ago)
  val yesterday = stringResource(R.string.video_relative_yesterday)
  val daysAgoFormat = stringResource(R.string.video_relative_days_ago)
  return remember(
    viewedSuffixFormat,
    minutesAgoFormat,
    hoursAgoFormat,
    yesterday,
    daysAgoFormat,
  ) {
    VideoCardRelativeText(
      viewedSuffixFormat = viewedSuffixFormat,
      minutesAgoFormat = minutesAgoFormat,
      hoursAgoFormat = hoursAgoFormat,
      yesterday = yesterday,
      daysAgoFormat = daysAgoFormat,
    )
  }
}

@Composable
private fun Int.formatCompactCountText(): String {
  return when {
    this >= 100_000_000 -> stringResource(R.string.video_count_yi, this / 100_000_000.0)
    this >= 10_000 -> stringResource(R.string.video_count_wan, this / 10_000.0)
    else -> toString()
  }
}
