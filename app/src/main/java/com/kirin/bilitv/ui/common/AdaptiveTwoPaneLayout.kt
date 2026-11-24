package com.kirin.bilitv.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

@Composable
internal fun AdaptiveTwoPaneLayout(
  minimumPrimaryWidth: Dp,
  minimumSecondaryWidth: Dp,
  spacing: Dp,
  modifier: Modifier = Modifier,
  preferredPrimaryWidth: Dp? = null,
  content: @Composable () -> Unit,
) {
  // Measurement changes orientation without recreating either pane's composition or focus targets.
  Layout(content = content, modifier = modifier) { measurables, constraints ->
    val width = constraints.maxWidth
    val height = constraints.maxHeight
    val gap = spacing.roundToPx().coerceAtMost(minOf(width, height))
    val horizontal = width >= minimumPrimaryWidth.roundToPx() + minimumSecondaryWidth.roundToPx() + gap
    val primaryWidth = if (horizontal) preferredPrimaryWidth?.roundToPx() ?: ((width - gap) / 2) else width
    val primaryHeight = if (horizontal) height else (height - gap) / 2
    val secondaryWidth = if (horizontal) width - gap - primaryWidth else width
    val secondaryHeight = if (horizontal) height else height - gap - primaryHeight
    val primary = measurables[0].measure(Constraints.fixed(primaryWidth, primaryHeight))
    val secondary = measurables[1].measure(Constraints.fixed(secondaryWidth, secondaryHeight))
    layout(width, height) {
      primary.placeRelative(0, 0)
      secondary.placeRelative(if (horizontal) primaryWidth + gap else 0, if (horizontal) 0 else primaryHeight + gap)
    }
  }
}
