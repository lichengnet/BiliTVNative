package com.kirin.bilitv.core.device

import org.junit.Assert.*
import org.junit.Test

class CompatibilityDiagnosticsTest {
  @Test fun hostLoggingDoesNotExposeCredentialsOrSignedParameters() {
    assertEquals("i0.hdslb.com", CompatibilityDiagnostics.imageHost("https://user:password@i0.hdslb.com/image.jpg?token=secret#private"))
    assertEquals("unknown", CompatibilityDiagnostics.imageHost("not a url"))
  }
  @Test fun errorLoggingUsesTypesNotMessages() {
    val error = IllegalStateException("SESSDATA=secret", java.io.IOException("https://example.com?token=secret"))
    assertEquals("IllegalStateException/IOException", CompatibilityDiagnostics.failureTypes(error))
  }
}
