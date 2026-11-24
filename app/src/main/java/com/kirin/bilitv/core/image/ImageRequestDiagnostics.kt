package com.kirin.bilitv.core.image

import android.graphics.drawable.BitmapDrawable
import coil.network.HttpException
import coil.request.ImageRequest
import com.kirin.bilitv.core.device.CompatibilityDiagnostics

internal fun ImageRequest.Builder.withImageDiagnostics(kind: String, width: Int, height: Int): ImageRequest.Builder {
  if (!CompatibilityDiagnostics.enabled) return this
  return listener(
    onStart = { request ->
      CompatibilityDiagnostics.record("image=${System.identityHashCode(request)} start kind=$kind host=${CompatibilityDiagnostics.imageHost(request.data.toString())} size=${width}x$height")
    },
    onCancel = { request -> CompatibilityDiagnostics.record("image=${System.identityHashCode(request)} cancel kind=$kind") },
    onSuccess = { request, result ->
      val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
      CompatibilityDiagnostics.record("image=${System.identityHashCode(request)} success kind=$kind source=${result.dataSource} bitmap=${bitmap?.width}x${bitmap?.height} config=${bitmap?.config}")
    },
    onError = { request, result ->
      val status = generateSequence(result.throwable) { it.cause }.take(5)
        .filterIsInstance<HttpException>().firstOrNull()?.response?.code
      CompatibilityDiagnostics.record("image=${System.identityHashCode(request)} error kind=$kind http=$status causes=${CompatibilityDiagnostics.failureTypes(result.throwable)}")
    },
  )
}
