package com.kirin.bilitv.ui.search

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.constrainHeight
import com.kirin.bilitv.ui.home.adaptiveGridColumns
import com.kirin.bilitv.ui.theme.BiliSizing
import com.kirin.bilitv.ui.theme.BiliSpacing
import com.kirin.bilitv.ui.theme.LocalUiMetrics

internal data class SearchTouchInputLayoutSpec(val inline: Boolean, val actionColumns: Int)

internal fun resolveSearchTouchInputLayoutSpec(widthDp: Float, scale: Float, actionCount: Int): SearchTouchInputLayoutSpec {
  require(scale.isFinite() && scale > 0f)
  require(actionCount in 1..2)
  val width = if (widthDp.isNaN()) 0f else widthDp.coerceAtLeast(0f)
  val actionWidth = (BiliSizing.SearchTouchActionButtonWidth.value * scale).coerceAtLeast(BiliSizing.MinimumTouchTarget.value)
  val spacing = BiliSpacing.Md.value * scale
  val inline = width >= BiliSizing.SearchTouchInputPreferredMinWidth.value * scale + actionCount * (actionWidth + spacing)
  return SearchTouchInputLayoutSpec(inline, if (inline) actionCount else adaptiveGridColumns(width, actionWidth, spacing, actionCount))
}

@Composable
internal fun SearchTouchInputLayout(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  val ui = LocalUiMetrics.current
  // Keep the same input composition/focus target when only the measurement orientation changes.
  Layout(content = content, modifier = modifier) { measurables, constraints ->
    val actionCount = measurables.size - 1
    val width = constraints.maxWidth
    val spec = resolveSearchTouchInputLayoutSpec(width.toDp().value, ui.scale, actionCount)
    val spacing = ui.dp(BiliSpacing.Md).roundToPx().coerceAtMost(width)
    val actionWidth = if (spec.inline) {
      ui.dp(BiliSizing.SearchTouchActionButtonWidth).coerceAtLeast(BiliSizing.MinimumTouchTarget).roundToPx()
    } else {
      ((width - spacing * (spec.actionColumns - 1)) / spec.actionColumns).coerceAtLeast(0)
    }
    val inputWidth = if (spec.inline) (width - actionCount * (actionWidth + spacing)).coerceAtLeast(0) else width
    val input = measurables.first().measure(constraints.copy(minWidth = inputWidth, maxWidth = inputWidth, minHeight = 0))
    val actionRows = (actionCount - 1) / spec.actionColumns + 1
    val verticalSpacing = if (!spec.inline && constraints.hasBoundedHeight) {
      spacing.coerceAtMost((constraints.maxHeight - input.height).coerceAtLeast(0) / actionRows)
    } else spacing
    val actionMaxHeight = if (!spec.inline && constraints.hasBoundedHeight) {
      ((constraints.maxHeight - input.height - verticalSpacing * actionRows) / actionRows).coerceAtLeast(0)
    } else constraints.maxHeight
    val actions = measurables.drop(1).map { it.measure(constraints.copy(
      minWidth = actionWidth, maxWidth = actionWidth, minHeight = 0, maxHeight = actionMaxHeight,
    )) }
    val rows = actions.chunked(spec.actionColumns)
    val rowHeights = rows.map { row -> row.maxOf { it.height } }
    val contentHeight = if (spec.inline) maxOf(input.height, rowHeights.first())
      else input.height + rowHeights.sum() + verticalSpacing * actionRows
    layout(width, constraints.constrainHeight(contentHeight)) {
      if (spec.inline) {
        input.placeRelative(0, (contentHeight - input.height) / 2)
        actions.forEachIndexed { index, action ->
          action.placeRelative(inputWidth + spacing + index * (actionWidth + spacing), (contentHeight - action.height) / 2)
        }
      } else {
        input.placeRelative(0, 0)
        var y = input.height + verticalSpacing
        rows.forEachIndexed { rowIndex, row ->
          row.forEachIndexed { column, action ->
            action.placeRelative(column * (actionWidth + spacing), y + (rowHeights[rowIndex] - action.height) / 2)
          }
          y += rowHeights[rowIndex] + verticalSpacing
        }
      }
    }
  }
}
