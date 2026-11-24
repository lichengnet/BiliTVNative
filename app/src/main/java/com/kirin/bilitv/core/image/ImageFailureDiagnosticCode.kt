package com.kirin.bilitv.core.image

import coil.network.HttpException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.UnknownHostException
import java.security.cert.CertPathValidatorException
import java.security.cert.CertificateException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException

// Never return exception messages: they can contain signed URLs or credentials.
internal fun imageFailureDiagnosticCode(error: Throwable): String {
  val causes = generateSequence(error) { it.cause }.take(5).toList()
  causes.filterIsInstance<HttpException>().firstOrNull()?.let { return "HTTP_${it.response.code}" }
  return when {
    causes.any { it is CertificateException || it is CertPathValidatorException } -> "TLS_CERT"
    causes.any { it is SSLPeerUnverifiedException } -> "TLS_VERIFY"
    causes.any { it is SSLHandshakeException } -> "TLS_HANDSHAKE"
    causes.any { it is SSLException } -> "TLS"
    causes.any { it is UnknownHostException } -> "DNS"
    causes.any { it is InterruptedIOException } -> "TIMEOUT"
    causes.any { it is ConnectException } -> "CONNECT"
    causes.any { it is OutOfMemoryError } -> "MEMORY"
    else -> error.javaClass.simpleName.ifBlank { "UNKNOWN" }.take(48)
  }
}
