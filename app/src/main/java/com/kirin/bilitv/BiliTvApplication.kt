package com.kirin.bilitv

import android.app.Application
import android.os.Build
import coil.EventListener
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.kirin.bilitv.core.app.AppContainer
import com.kirin.bilitv.core.device.CompatibilityDiagnostics
import com.kirin.bilitv.core.device.SessionDiagnostics
import com.kirin.bilitv.core.device.SessionEvent
import java.lang.ref.WeakReference
import com.kirin.bilitv.core.device.DeviceCompatibility
import com.kirin.bilitv.core.image.ImageStageDiagnostics

class BiliTvApplication : Application(), ImageLoaderFactory {
  lateinit var appContainer: AppContainer
    private set

  private var createdImageLoader: WeakReference<ImageLoader>? = null

  override fun onCreate() {
    super.onCreate()
    SessionDiagnostics.install(this)
    appContainer = AppContainer(this)
    SessionDiagnostics.observePlaybackLevel(appContainer.appSettingsStore.playbackLogLevel)
    CompatibilityDiagnostics.record("device api=${Build.VERSION.SDK_INT} jacinto6=${DeviceCompatibility.current.isJacinto6} board=${Build.BOARD} device=${Build.DEVICE} model=${Build.MODEL}")
  }

  override fun newImageLoader(): ImageLoader {
    val networkObserverEnabled = DeviceCompatibility.current.imageNetworkObserverEnabled
    CompatibilityDiagnostics.record("imageLoader networkObserverEnabled=$networkObserverEnabled")
    return ImageLoader.Builder(this)
      .networkObserverEnabled(networkObserverEnabled)
      .eventListenerFactory { if (CompatibilityDiagnostics.enabled) ImageStageDiagnostics() else EventListener.NONE }
      .memoryCache {
        MemoryCache.Builder(this)
          .maxSizePercent(ImageMemoryCachePercent)
          .build()
      }
      .diskCache {
        DiskCache.Builder()
          .directory(cacheDir.resolve(ImageDiskCacheDirectory))
          .maxSizeBytes(ImageDiskCacheMaxBytes)
          .build()
      }
      .crossfade(false)
      .build().also { createdImageLoader = WeakReference(it) }
  }

  @Suppress("DEPRECATION")
  override fun onTrimMemory(level: Int) {
    super.onTrimMemory(level)
    if (level >= TRIM_MEMORY_RUNNING_LOW) {
      createdImageLoader?.get()?.memoryCache?.clear()
      SessionDiagnostics.event(SessionEvent.MEMORY_PRESSURE, mapOf("trim" to level.toString()), memory = true)
    }
  }

  override fun onLowMemory() {
    super.onLowMemory()
    createdImageLoader?.get()?.memoryCache?.clear()
    SessionDiagnostics.event(SessionEvent.MEMORY_PRESSURE, mapOf("trim" to "LOW_MEMORY"), memory = true)
  }

  private companion object {
    const val ImageMemoryCachePercent = 0.20
    const val ImageDiskCacheDirectory = "image_cache"
    const val ImageDiskCacheMaxBytes = 128L * 1024L * 1024L
  }
}
