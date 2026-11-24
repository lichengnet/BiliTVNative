package com.kirin.bilitv.ui.common

import com.kirin.bilitv.ui.theme.uiDp
import com.kirin.bilitv.ui.theme.LocalUiMetrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.kirin.bilitv.ui.home.VideoCardMetrics
import com.kirin.bilitv.ui.home.VideoGridLayoutSpec
import com.kirin.bilitv.ui.home.resolveVideoGridLayoutSpec
import com.kirin.bilitv.ui.focus.BiliFocusableSurface
import com.kirin.bilitv.ui.settings.LocalBiliPerformancePolicy
import com.kirin.bilitv.ui.theme.BiliColors
import com.kirin.bilitv.ui.theme.BiliFocus
import com.kirin.bilitv.ui.theme.BiliRadius
import com.kirin.bilitv.ui.theme.BiliSizing
import com.kirin.bilitv.ui.theme.BiliSkeleton
import com.kirin.bilitv.ui.theme.BiliSpacing
import com.kirin.bilitv.ui.theme.BiliTypography

@Composable
fun FeedStatusScreen(
  message: String,
  actionLabel: String? = null,
  onAction: () -> Unit = {},
) {
  val ui = LocalUiMetrics.current
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = message,
      color = BiliColors.TextSecondary,
      fontSize = ui.sp(BiliTypography.Body),
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = ui.dp(BiliSizing.ContentPadding)),
    )
    if (actionLabel != null) {
      BiliFocusableSurface(
        scaleOnFocus = false,
        onClick = onAction,
        modifier = Modifier.padding(top = ui.dp(BiliSpacing.Lg)),
      ) {
        Text(
          text = actionLabel,
          color = BiliColors.TextPrimary,
          fontSize = ui.sp(BiliTypography.BodySmall),
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = ui.dp(BiliSpacing.Lg), vertical = ui.dp(BiliSpacing.Md)),
        )
      }
    }
  }
}

@Composable
internal fun VideoGridSkeleton(
  modifier: Modifier = Modifier,
  count: Int? = null,
  layoutSpec: VideoGridLayoutSpec? = null,
  horizontalPadding: Dp = uiDp(BiliSizing.VideoGridHorizontalPadding),
) {
  val ui = LocalUiMetrics.current
  val performancePolicy = LocalBiliPerformancePolicy.current
  val itemCount = count ?: if (performancePolicy.lowSpecMode) {
    BiliSkeleton.LowSpecItemCount
  } else {
    BiliSkeleton.StandardItemCount
  }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
  val spec = layoutSpec ?: resolveVideoGridLayoutSpec((maxWidth - horizontalPadding * 2).value, ui.scale)
  val metrics = remember(spec.cardContentScale) { VideoCardMetrics(spec.cardContentScale) }
  LazyVerticalGrid(
    columns = GridCells.Fixed(spec.columns),
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(
      start = horizontalPadding,
      top = ui.dp(BiliFocus.ScrollInset),
      end = horizontalPadding,
      bottom = ui.dp(BiliSizing.VideoGridBottomPadding),
    ),
    horizontalArrangement = Arrangement.spacedBy(ui.dp(BiliSizing.VideoGridSpacing)),
    verticalArrangement = Arrangement.spacedBy(ui.dp(BiliSizing.VideoGridSpacing)),
    userScrollEnabled = false,
  ) {
    items((0 until itemCount.coerceAtLeast(0)).toList()) { index ->
      SkeletonVideoCard(index, metrics)
    }
  }
  }
}

@Composable
private fun SkeletonVideoCard(index: Int, metrics: VideoCardMetrics) {
  val ui = LocalUiMetrics.current
  Column(
    modifier = Modifier
      .testTag("video_skeleton_$index")
      .fillMaxWidth()
      .clip(RoundedCornerShape(ui.dp(BiliRadius.Card)))
      .background(BiliColors.Surface),
  ) {
    Box(
      modifier = Modifier
        .testTag("video_skeleton_cover_$index")
        .fillMaxWidth()
        .aspectRatio(BiliSizing.VideoThumbnailAspectRatio)
        .clip(RoundedCornerShape(ui.dp(BiliRadius.Card)))
        .background(BiliColors.SurfaceElevated),
    )
    Column(
      modifier = Modifier
        .testTag("video_skeleton_info_$index")
        .heightIn(min = metrics.textHeight)
        .padding(horizontal = metrics.smallSpacing, vertical = metrics.extraSmallSpacing),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(
        verticalArrangement = Arrangement.spacedBy(metrics.smallSpacing),
      ) {
        SkeletonLine(BiliSkeleton.TitleLongWidthFraction, metrics)
        // The enlarged profile mirrors the real card's single-line title.
        if (metrics.contentScale == 1f) {
          SkeletonLine(BiliSkeleton.TitleShortWidthFraction, metrics)
        }
      }
      SkeletonLine(BiliSkeleton.MetaWidthFraction, metrics)
    }
  }
}

@Composable
private fun SkeletonLine(widthFraction: Float, metrics: VideoCardMetrics) {
  val ui = LocalUiMetrics.current
  Spacer(
    modifier = Modifier
      .fillMaxWidth(widthFraction)
      .height(metrics.skeletonLineHeight)
      .clip(RoundedCornerShape(ui.dp(BiliRadius.Pill)))
      .background(BiliColors.SurfaceElevated),
  )
}
