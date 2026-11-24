package com.kirin.bilitv.core.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppUiSizeTest {
  @Test fun everyDeviceDefaultsToMedium() { assertEquals(AppUiSize.Medium, AppSettings().uiSize) }
  @Test fun missingAndInvalidSettingsFallBackToMedium() {
    for (key in listOf(null, "", "jacinto6", "unknown")) assertEquals(AppUiSize.Medium, AppUiSize.fromKey(key))
  }
  @Test fun stableKeysRoundTripAllThreeSizes() {
    for (size in AppUiSize.entries) assertEquals(size, AppUiSize.fromKey(size.key))
  }
  @Test fun explicitConfirmCyclesSmallMediumLarge() {
    assertEquals(AppUiSize.Medium, AppUiSize.Small.next())
    assertEquals(AppUiSize.Large, AppUiSize.Medium.next())
    assertEquals(AppUiSize.Small, AppUiSize.Large.next())
  }
  @Test fun settingSizePreservesOtherAppSettings() {
    val original = AppSettings(showClock = false, autoConfirmOnFocus = true)
    val updated = original.copy(uiSize = AppUiSize.Large)
    assertEquals(original.showClock, updated.showClock)
    assertEquals(original.autoConfirmOnFocus, updated.autoConfirmOnFocus)
  }
}
