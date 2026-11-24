package com.kirin.bilitv.core.image

import coil.network.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.cert.CertificateExpiredException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ImageFailureDiagnosticCodeTest {
  private val secret = "SESSDATA=private&token=private"
  private fun httpError(code: Int) = HttpException(
    Response.Builder()
      .request(Request.Builder().url("https://example.invalid/image.jpg?$secret").build())
      .protocol(Protocol.HTTP_1_1).code(code).message(secret).build(),
  )

  @Test fun wrappedHttpStatusIsPreservedWithoutUrlOrMessage() {
    val code = imageFailureDiagnosticCode(IllegalStateException(secret, httpError(504)))
    assertEquals("HTTP_504", code)
    assertFalse(code.contains("private"))
  }
  @Test fun certificateFailureTakesPriorityOverGenericHandshake() {
    val error = SSLHandshakeException(secret).apply { initCause(CertificateExpiredException(secret)) }
    assertEquals("TLS_CERT", imageFailureDiagnosticCode(error))
  }
  @Test fun handshakeAndHostnameVerificationRemainDistinct() {
    assertEquals("TLS_HANDSHAKE", imageFailureDiagnosticCode(SSLHandshakeException(secret)))
    assertEquals("TLS_VERIFY", imageFailureDiagnosticCode(SSLPeerUnverifiedException(secret)))
  }
  @Test fun dnsAndTimeoutAreSafeShortCodes() {
    assertEquals("DNS", imageFailureDiagnosticCode(UnknownHostException(secret)))
    assertEquals("TIMEOUT", imageFailureDiagnosticCode(SocketTimeoutException(secret)))
  }
  @Test fun connectionAndMemoryAreDistinguished() {
    assertEquals("CONNECT", imageFailureDiagnosticCode(ConnectException(secret)))
    assertEquals("MEMORY", imageFailureDiagnosticCode(OutOfMemoryError(secret)))
  }
  @Test fun unclassifiedErrorDoesNotExposeItsMessage() {
    assertEquals("IllegalArgumentException", imageFailureDiagnosticCode(IllegalArgumentException(secret)))
    assertEquals("HTTP_403", imageFailureDiagnosticCode(httpError(403)))
  }
  @Test fun cyclicCauseChainIsBounded() {
    val first = IllegalStateException(secret)
    val second = IllegalArgumentException(secret, first)
    first.initCause(second)
    assertEquals("IllegalStateException", imageFailureDiagnosticCode(first))
  }
}
