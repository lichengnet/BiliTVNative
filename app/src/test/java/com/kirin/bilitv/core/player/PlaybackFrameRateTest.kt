package com.kirin.bilitv.core.player

import org.junit.Assert.*
import org.junit.Test

class PlaybackFrameRateTest {
  @Test fun integerAndFractionHaveCanonicalDashValues() {
    assertEquals("30",PlaybackFrameRate.parse("30")?.dashValue)
    assertEquals("30",PlaybackFrameRate.parse("60/2")?.dashValue)
    assertEquals("30000/1001",PlaybackFrameRate.parse("30000/1001")?.dashValue)
  }
  @Test fun decimalDoesNotBecomeAnInvalidDashDecimalAttribute() {
    assertEquals("2997/100",PlaybackFrameRate.parse("29.97")?.dashValue)
    assertEquals(29.97f,PlaybackFrameRate.parse("29.97")!!.fps,0.001f)
  }
  @Test fun missingAndMalformedRatesRemainUnknown() {
    for (s in listOf(null,"","unknown","0","-30","30/0","0/1","30/1/1","NaN","Infinity")) assertNull(s,PlaybackFrameRate.parse(s))
  }
  @Test fun whitespaceAndIntegralDecimalAreNormalized() {
    assertEquals("30",PlaybackFrameRate.parse(" 30.00 ")?.dashValue)
    assertEquals("30",PlaybackFrameRate.parse(" 60 / 2 ")?.dashValue)
  }
  @Test fun dashAttributeHasRealQuotesAndIsOnlyUsedForVideo() {
    assertEquals(" frameRate=" + '"' + "2997/100" + '"',dashFrameRateAttribute("29.97",true))
    assertEquals("",dashFrameRateAttribute("30",false))
    assertEquals("",dashFrameRateAttribute(null,true))
  }
  @Test fun unboundedNumbersAreRejectedWithoutLargeAllocation() {
    for(s in listOf("1e100", "999999999999999999999999999999", "1/"+"9".repeat(1000))) assertNull(PlaybackFrameRate.parse(s))
  }
}
