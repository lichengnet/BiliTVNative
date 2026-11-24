package com.kirin.bilitv.core.image

import coil.EventListener
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.Options
import com.kirin.bilitv.core.device.CompatibilityDiagnostics

internal class ImageStageDiagnostics : EventListener {
  @Volatile private var phase = "request"
  override fun fetchStart(request: ImageRequest, fetcher: Fetcher, options: Options) {
    phase = "fetch"
    event(request, "fetch start networkRead=${options.networkCachePolicy.readEnabled} networkWrite=${options.networkCachePolicy.writeEnabled}")
  }
  override fun fetchEnd(request: ImageRequest, fetcher: Fetcher, options: Options, result: FetchResult?) {
    event(request, "fetch end result=${result?.javaClass?.simpleName} mime=${(result as? SourceResult)?.mimeType}")
  }
  override fun decodeStart(request: ImageRequest, decoder: Decoder, options: Options) {
    phase = "decode"
    event(request, "decode start")
  }
  override fun decodeEnd(request: ImageRequest, decoder: Decoder, options: Options, result: DecodeResult?) {
    event(request, "decode end drawable=${result?.drawable?.javaClass?.simpleName}")
  }
  override fun onError(request: ImageRequest, result: ErrorResult) {
    event(request, "failure phase=$phase code=${imageFailureDiagnosticCode(result.throwable)} ${imageNetworkDiagnosticState(request.context)}")
  }
  private fun event(request: ImageRequest, phase: String) {
    CompatibilityDiagnostics.record("image=${System.identityHashCode(request)} $phase host=${CompatibilityDiagnostics.imageHost(request.data.toString())}")
  }
}
