package com.kirin.bilitv.ui.common

import com.kirin.bilitv.ui.theme.LocalUiMetrics

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.kirin.bilitv.ui.theme.BiliColors
import com.kirin.bilitv.ui.theme.BiliTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ClockOverlay(
  clockText: String,
  modifier: Modifier = Modifier,
) {
  val ui = LocalUiMetrics.current
  Text(
    text = clockText,
    color = BiliColors.TextPrimary,
    fontSize = ui.sp(BiliTypography.PlayerMeta),
    fontWeight = FontWeight.Bold,
    modifier = modifier,
  )
}

fun currentClockText(): String {
  return SimpleDateFormat("HH:mm", Locale.US).format(Date())
}

fun currentClockMinuteKey(): Long {
  return System.currentTimeMillis() / MillisPerMinute
}

private const val MillisPerMinute = 60_000L
