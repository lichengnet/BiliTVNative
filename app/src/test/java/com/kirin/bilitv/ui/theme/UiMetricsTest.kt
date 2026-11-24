package com.kirin.bilitv.ui.theme

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kirin.bilitv.core.settings.AppUiSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class UiMetricsTest {
  @Test fun exactThreeFactors() {
    assertEquals(0.85f, UiMetrics(AppUiSize.Small).scale, 0f)
    assertEquals(1f, UiMetrics(AppUiSize.Medium).scale, 0f)
    assertEquals(4f / 3f, UiMetrics(AppUiSize.Large).scale, 0f)
  }
  @Test fun dimensionsAndFontsScaleWithoutDensityChanges() {
    val ui = UiMetrics(AppUiSize.Large)
    assertEquals(32.dp, ui.dp(24.dp))
    assertEquals(16.sp, ui.sp(12.sp))
  }
  @Test fun defaultScopeLeavesPlayerUntouched() {
    val ui = UiMetrics()
    assertFalse(ui.active)
    assertEquals(24.dp, ui.dp(24.dp))
    assertEquals(22.sp, ui.sp(22.sp))
  }
  @Test fun relativeTypographyDoesNotScaleTwice() { assertEquals(1.5.em, UiMetrics(AppUiSize.Large).sp(1.5.em)) }
  @Test fun unspecifiedTypographyIsSafe() { assertEquals(TextUnit.Unspecified, UiMetrics(AppUiSize.Large).sp(TextUnit.Unspecified)) }
}
