package com.kirin.bilitv.core.image

import android.content.Context
import com.kirin.bilitv.core.device.DeviceCompatibility
import com.kirin.bilitv.core.network.BiliHeaders
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision

object BiliImageSizing {
  const val StandardVideoThumbnailWidthPx = 480
  const val StandardVideoThumbnailHeightPx = 270
  const val StandardOwnerAvatarSizePx = 72
  const val AccountAvatarSizePx = 96
  const val AccountProfileAvatarSizePx = 192
}

fun buildVideoThumbnailRequest(
  context: Context,
  url: String,
  widthPx: Int = BiliImageSizing.StandardVideoThumbnailWidthPx,
  heightPx: Int = BiliImageSizing.StandardVideoThumbnailHeightPx,
  allowRgb565: Boolean = false,
  memoryCacheEnabled: Boolean = true,
): ImageRequest {
  val compatibility = DeviceCompatibility.current
  val requestWidth = if (compatibility.isJacinto6) widthPx.coerceAtMost(320) else widthPx
  val requestHeight = if (compatibility.isJacinto6) heightPx.coerceAtMost(180) else heightPx
  val format = if (compatibility.isJacinto6) BiliImageFormat.Jpeg else BiliImageFormat.Webp
  return ImageRequest.Builder(context)
    .data(url.resizedBiliImageUrl(requestWidth, requestHeight, format))
    .addBiliImageHeaders()
    .size(requestWidth, requestHeight)
    .precision(Precision.INEXACT)
    .allowRgb565(allowRgb565 || compatibility.isJacinto6)
    .memoryCachePolicy(if (memoryCacheEnabled) CachePolicy.ENABLED else CachePolicy.DISABLED)
    .crossfade(false)
    .withImageDiagnostics("thumbnail", requestWidth, requestHeight)
    .build()
}

fun buildOwnerAvatarRequest(
  context: Context,
  url: String,
  sizePx: Int = BiliImageSizing.StandardOwnerAvatarSizePx,
  allowRgb565: Boolean = false,
  memoryCacheEnabled: Boolean = true,
): ImageRequest {
  return ImageRequest.Builder(context)
    .data(url.biliCdnResizedImageUrl(sizePx, sizePx))
    .addBiliImageHeaders()
    .size(sizePx, sizePx)
    .precision(Precision.INEXACT)
    .allowRgb565(allowRgb565)
    .memoryCachePolicy(if (memoryCacheEnabled) CachePolicy.ENABLED else CachePolicy.DISABLED)
    .crossfade(false)
    .withImageDiagnostics("avatar", sizePx, sizePx)
    .build()
}

private fun ImageRequest.Builder.addBiliImageHeaders(): ImageRequest.Builder {
  return addHeader("User-Agent", BiliHeaders.UserAgent)
    .addHeader("Referer", BiliHeaders.Referer)
}
