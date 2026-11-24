package com.kirin.bilitv.core.player

import java.math.BigDecimal
import java.math.BigInteger

internal data class PlaybackFrameRate(val numerator: Int, val denominator: Int) {
  val fps: Float get() = numerator.toFloat() / denominator
  val dashValue: String get() = if (denominator == 1) numerator.toString() else "$numerator/$denominator"
  companion object {
    fun parse(value: String?): PlaybackFrameRate? = try {
      val text = value.orEmpty().trim()
      require(text.length in 1..64)
      val parts = text.split('/')
      val (n, d) = if (parts.size == 2) {
        parts[0].trim().toBigInteger() to parts[1].trim().toBigInteger()
      } else {
        require(parts.size == 1 && text.length <= 32)
        val decimal = BigDecimal(text).stripTrailingZeros()
        require(decimal.scale() in -6..6)
        if (decimal.scale() < 0) decimal.unscaledValue() * BigInteger.TEN.pow(-decimal.scale()) to BigInteger.ONE
        else decimal.unscaledValue() to BigInteger.TEN.pow(decimal.scale())
      }
      require(n.signum() > 0 && d.signum() > 0)
      val divisor = n.gcd(d)
      val numerator = n / divisor
      val denominator = d / divisor
      require(numerator.bitLength() <= 31 && denominator.bitLength() <= 31)
      PlaybackFrameRate(numerator.toInt(), denominator.toInt())
    } catch (_: Exception) { null }
  }
}

internal fun dashFrameRateAttribute(value: String?, video: Boolean): String {
  if (!video) return ""
  val rate = PlaybackFrameRate.parse(value) ?: return ""
  return " frameRate=" + '"' + rate.dashValue + '"'
}
