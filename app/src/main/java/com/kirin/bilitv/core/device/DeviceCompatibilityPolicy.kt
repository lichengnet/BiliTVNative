package com.kirin.bilitv.core.device

import android.os.Build

internal data class DeviceCompatibilityPolicy(val isJacinto6: Boolean = false) {
  val useTextureView: Boolean get() = isJacinto6
  val imageNetworkObserverEnabled: Boolean get() = !isJacinto6
  val playbackTransitionsEnabled: Boolean get() = !isJacinto6

  companion object {
    fun forDevice(api: Int, board: String?, device: String?, model: String?): DeviceCompatibilityPolicy {
      return DeviceCompatibilityPolicy(
        isJacinto6 = api == 23 && listOf(board, device, model).any {
          it.orEmpty().trim().startsWith("jacinto6", ignoreCase = true)
        },
      )
    }
  }
}

internal object DeviceCompatibility {
  val current: DeviceCompatibilityPolicy by lazy {
    DeviceCompatibilityPolicy.forDevice(Build.VERSION.SDK_INT, Build.BOARD, Build.DEVICE, Build.MODEL)
  }
}
