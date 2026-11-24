package com.kirin.bilitv.core.image

import org.junit.Assert.assertEquals
import org.junit.Test

class BiliImageUrlTest {
  private val image = "https://i0.hdslb.com/bfs/archive/test.jpg"
  @Test fun defaultThumbnailsRemainWebp() {
    assertEquals("$image@480w_270h_1c.webp", image.biliCdnResizedImageUrl(480, 270))
  }
  @Test fun jpegUsesRequestedDimensions() {
    assertEquals("$image@320w_180h_1c.jpg", image.resizedBiliImageUrl(320, 180, BiliImageFormat.Jpeg))
  }
  @Test fun normalizesHttpAndProtocolRelativeUrls() {
    assertEquals("$image@320w_180h_1c.jpg", image.removePrefix("https:").resizedBiliImageUrl(320, 180, BiliImageFormat.Jpeg))
    assertEquals("$image@320w_180h_1c.jpg", image.replace("https:", "http:").resizedBiliImageUrl(320, 180, BiliImageFormat.Jpeg))
  }
  @Test fun jpegReplacesExistingCdnTransformation() {
    assertEquals("$image@320w_180h_1c.jpg?sample=1#part", "$image@640w_360h_1c.webp?sample=1#part".resizedBiliImageUrl(320, 180, BiliImageFormat.Jpeg))
  }
  @Test fun webpPreservesExistingTransformationAndTransparentAvatarFormat() {
    val existing = "$image@96w_96h_1c.png"
    assertEquals(existing, existing.biliCdnResizedImageUrl(96, 96))
  }
  @Test fun queryAtSignDoesNotPreventResizing() {
    assertEquals("$image@320w_180h_1c.jpg?test=a@b", "$image?test=a@b".resizedBiliImageUrl(320, 180, BiliImageFormat.Jpeg))
  }
  @Test fun nonCdnUrlsAndEmptyValuesAreNotTransformed() {
    val external = "https://example.com/image.png"
    assertEquals(external, external.resizedBiliImageUrl(320, 180, BiliImageFormat.Jpeg))
    assertEquals("", " ".biliCdnResizedImageUrl(320, 180))
    assertEquals("https://nothdslb.com/image.jpg", "https://nothdslb.com/image.jpg".biliCdnResizedImageUrl(320, 180))
  }
}
