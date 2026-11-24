package com.kirin.bilitv.ui.shell

import com.kirin.bilitv.ui.theme.LocalUiMetrics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kirin.bilitv.R
import com.kirin.bilitv.ui.focus.BiliFocusableSurface
import com.kirin.bilitv.ui.theme.BiliRadius
import com.kirin.bilitv.ui.theme.BiliSizing
import com.kirin.bilitv.ui.theme.BiliTypography
import com.kirin.bilitv.ui.theme.LocalHomeColors

internal const val AppExitButtonTestTag = "app_exit_button"

@Composable
internal fun AppExitButton(onExitApp: () -> Unit, modifier: Modifier = Modifier, onMoveRight: () -> Boolean = { false }) {
  val ui = LocalUiMetrics.current
  val exitLabel = stringResource(R.string.app_exit)
  // Exit is an action, never a focus-auto-confirm navigation destination.
  BiliFocusableSurface(
    modifier = modifier.fillMaxWidth().height(ui.dp(BiliSizing.NavItemHeight))
      .testTag(AppExitButtonTestTag)
      .semantics { contentDescription = exitLabel }
      .onPreviewKeyEvent {
        it.type == KeyEventType.KeyDown && it.key == Key.DirectionRight && onMoveRight()
      },
    shape = RoundedCornerShape(ui.dp(BiliRadius.Pill)),
    onClick = onExitApp,
  ) {
    Box(modifier = Modifier.fillMaxWidth().height(ui.dp(BiliSizing.NavItemHeight)), contentAlignment = Alignment.Center) {
      Text(text = stringResource(R.string.app_exit_short), color = LocalHomeColors.current.textPrimary, fontSize = ui.sp(BiliTypography.Body))
    }
  }
}
