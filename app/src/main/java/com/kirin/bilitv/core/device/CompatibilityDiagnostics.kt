package com.kirin.bilitv.core.device

import android.util.Log
import com.kirin.bilitv.BuildConfig
import java.net.URI

internal object CompatibilityDiagnostics {
  val enabled: Boolean get() = BuildConfig.COMPATIBILITY_DIAGNOSTICS

  fun record(event: String) {
    if (enabled) Log.d("BiliTVNative:Compat", event)
  }

  fun imageHost(url: String): String = runCatching { URI(url).host }.getOrNull() ?: "unknown"

  // Exception messages may contain signed URLs, so only record class names.
  fun failureTypes(error: Throwable): String = generateSequence(error) { it.cause }
    .take(5).joinToString("/") { it.javaClass.simpleName }
}
