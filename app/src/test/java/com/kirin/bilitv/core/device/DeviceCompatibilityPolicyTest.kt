package com.kirin.bilitv.core.device

import org.junit.Assert.*
import org.junit.Test

class DeviceCompatibilityPolicyTest {
  @Test fun matchesApi23Board() {
    val policy = DeviceCompatibilityPolicy.forDevice(23, "jacinto6evm", "other", "other")
    assertTrue(policy.isJacinto6)
    assertTrue(policy.useTextureView)
    assertFalse(policy.playbackTransitionsEnabled)
  }
  @Test fun matchesDeviceAndModelIgnoringCase() {
    assertTrue(DeviceCompatibilityPolicy.forDevice(23, null, "Jacinto6", null).isJacinto6)
    assertTrue(DeviceCompatibilityPolicy.forDevice(23, null, null, " JACINTO6EVM ").isJacinto6)
  }
  @Test fun otherAndroidVersionsKeepDefaults() {
    for (api in listOf(22, 24, 26, 36)) {
      val policy = DeviceCompatibilityPolicy.forDevice(api, "jacinto6evm", null, null)
      assertFalse(policy.useTextureView)
      assertTrue(policy.playbackTransitionsEnabled)
    }
  }
  @Test fun onlyTargetDeviceBypassesImageOfflineShortCircuit() {
    assertFalse(DeviceCompatibilityPolicy.forDevice(23, "jacinto6evm", null, null).imageNetworkObserverEnabled)
    assertTrue(DeviceCompatibilityPolicy.forDevice(23, "other", null, null).imageNetworkObserverEnabled)
    assertTrue(DeviceCompatibilityPolicy.forDevice(24, "jacinto6evm", null, null).imageNetworkObserverEnabled)
  }
  @Test fun genericApi23DevicesDoNotMatch() {
    assertFalse(DeviceCompatibilityPolicy.forDevice(23, null, null, null).isJacinto6)
    assertFalse(DeviceCompatibilityPolicy.forDevice(23, "other", "other", "TI tablet").isJacinto6)
    assertFalse(DeviceCompatibilityPolicy.forDevice(23, "not-jacinto6", null, null).isJacinto6)
  }
}
