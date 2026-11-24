package com.kirin.bilitv.core.image

import java.net.URI

internal enum class BiliImageFormat(val extension: String) {
  Webp("webp"), Jpeg("jpg");
}

fun String.biliCdnResizedImageUrl(widthPx: Int, heightPx: Int? = null): String {
  return resizedBiliImageUrl(widthPx, heightPx, BiliImageFormat.Webp)
}

internal fun String.resizedBiliImageUrl(widthPx: Int, heightPx: Int?, format: BiliImageFormat): String {
  val normalized = normalizedBiliImageUrl()
  if (normalized.isBlank()) return normalized
  val host = runCatching { URI(normalized).host }.getOrNull().orEmpty()
  if (!host.equals("hdslb.com", true) && !host.endsWith(".hdslb.com", true)) return normalized
  val queryStart = normalized.indexOfAny(charArrayOf('?', '#')).takeIf { it >= 0 } ?: normalized.length
  val imagePath = normalized.substring(0, queryStart)
  if (imagePath.contains('@') && format == BiliImageFormat.Webp) return normalized
  val suffix = if (heightPx != null) {
    "@${widthPx}w_${heightPx}h_1c.${format.extension}"
  } else {
    "@${widthPx}w.${format.extension}"
  }
  return imagePath.substringBefore('@') + suffix + normalized.substring(queryStart)
}

private fun String.normalizedBiliImageUrl(): String {
  val trimmed = trim()
  return when {
    trimmed.startsWith("//") -> "https:$trimmed"
    trimmed.startsWith("http://") -> "https://${trimmed.removePrefix("http://")}"
    else -> trimmed
  }
}
