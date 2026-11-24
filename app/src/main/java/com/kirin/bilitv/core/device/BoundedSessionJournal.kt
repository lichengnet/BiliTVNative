package com.kirin.bilitv.core.device

import java.io.File

internal enum class SessionEvent {
  APP_START, FOREGROUND, BACKGROUND, EXIT_REQUESTED, JAVA_FATAL, JAVA_STACK,
  PLAY_ATTEMPT, PLAY_TRACKS, PLAY_RECOVERY, FIRST_FRAME, PLAY_FAILURE, PLAY_RELEASE, MEMORY_PRESSURE,
}

internal data class SessionLogEntry(val event: SessionEvent, val fields: Map<String, String>)

internal object SafeDiagnosticFields {
  private val videoId = Regex("BV[A-Za-z0-9]{10}")
  fun isPublicVideoId(value: String): Boolean = videoId.matches(value)

  private val keys = setOf(
    "version", "versionCode", "api", "jacinto6", "at", "generation", "session", "player", "attempt", "bvid", "cid",
    "quality", "requestedQn", "returnedQn", "selectedQn", "nextQn", "rawVideo", "usableVideo", "rawAudio", "usableAudio",
    "retainedVideo", "avc", "result", "mode", "phase", "code", "type", "decoder", "codec", "width", "height", "fps",
    "profile", "level", "levelLimit", "recoverable", "transient", "diagnostic", "source", "errorName", "formatSupport",
    "reason", "http", "apiCode", "javaUsed", "javaMax", "native", "pss", "trim", "class", "method", "line",
  )

  private fun safeValue(key: String, value: String): String {
    val safeCharacters = value.length in 1..192 && value.all { it.isLetterOrDigit() || it in "._:$-" }
    val valid = when (key) {
      "bvid" -> isPublicVideoId(value)
      "cid" -> value.toLongOrNull()?.let { it > 0 } == true
      else -> safeCharacters
    }
    return if (valid) value else "REDACTED"
  }

  fun line(event: SessionEvent, fields: Map<String, String>): String {
    val safe = fields.entries.filter { it.key in keys }.take(28).joinToString(" ") { (key, raw) ->
      "$key=${safeValue(key, raw)}"
    }
    return event.name + if (safe.isEmpty()) "" else " " + safe
  }

  fun entry(line: String): SessionLogEntry? {
    val event = SessionEvent.entries.firstOrNull { it.name == line.substringBefore(' ') } ?: return null
    val fields = line.substringAfter(' ', "").split(' ').mapNotNull { field ->
      val key = field.substringBefore('=')
      key.takeIf { it in keys }?.let { it to safeValue(it, field.substringAfter('=', "")) }
    }.toMap()
    return SessionLogEntry(event, fields)
  }

  fun previousOutcome(text: String): String {
    val events = text.lineSequence().map { it.substringBefore(' ') }.toList()
    if (SessionEvent.JAVA_FATAL.name in events) return "JAVA_FATAL"
    if (events.lastIndexOf("EXIT_REQUESTED") > events.lastIndexOf("FOREGROUND")) return "EXPLICIT_EXIT"
    return when (events.lastOrNull { it in setOf("FOREGROUND", "BACKGROUND", "EXIT_REQUESTED", "APP_START") }) {
      "BACKGROUND" -> "BACKGROUND_TERMINATION_UNKNOWN"
      "EXIT_REQUESTED" -> "EXPLICIT_EXIT"
      null -> "NO_PREVIOUS_SESSION"
      else -> "UNCLEAN_REASON_UNKNOWN"
    }
  }
}

internal class BoundedSessionJournal(
  directory: File,
  private val writeSnapshot: (File, String) -> Unit = { file, text -> file.writeText(text, Charsets.UTF_8) },
) {
  companion object { const val MaxBytes = 16 * 1024 }
  private val current = File(directory, "current.log")
  private val previous = File(directory, "previous.log")
  val previousLines: List<String>
  val previousOutcome: String
  private val previousContent: String
  private val diskLock = Any()
  private var content = ""

  init {
    directory.mkdirs()
    val prior = if (current.exists()) current else previous
    previousContent = readSafeSnapshot(prior)
    previousLines = previousContent.lineSequence().filter { it.isNotBlank() }.toList().takeLast(24)
    previousOutcome = SafeDiagnosticFields.previousOutcome(previousContent)
    if (prior.exists()) writeSnapshot(previous, previousContent)
    writeSnapshot(current, "")
  }

  @Synchronized fun append(event: SessionEvent, fields: Map<String, String>): String {
    val line = SafeDiagnosticFields.line(event, fields)
    content = boundedTail("$content$line\n")
    return line
  }

  private fun readSafeSnapshot(file: File): String {
    if (!file.isFile || file.length() > MaxBytes) return ""
    val safe = buildString {
      file.readText(Charsets.UTF_8).lineSequence().mapNotNull(SafeDiagnosticFields::entry).forEach { entry ->
        appendLine(SafeDiagnosticFields.line(entry.event, entry.fields))
      }
    }
    return boundedTail(safe)
  }

  private fun boundedTail(text: String): String {
    var result = text
    while (result.toByteArray(Charsets.UTF_8).size > MaxBytes) {
      result = result.substringAfter('\n', "")
    }
    return result
  }

  fun flush() = synchronized(diskLock) {
    writeSnapshot(current, synchronized(this) { content })
  }

  fun entries(previousSession: Boolean): List<SessionLogEntry> {
    val text = if (previousSession) previousContent else synchronized(this) { content }
    return text.lineSequence().mapNotNull(SafeDiagnosticFields::entry).toList()
  }

  fun record(event: SessionEvent, fields: Map<String, String> = emptyMap()) {
    append(event, fields)
    flush()
  }

  fun fatal(error: Throwable) {
    append(SessionEvent.JAVA_FATAL, mapOf("type" to error.javaClass.name))
    error.stackTrace.take(8).forEach { frame -> append(SessionEvent.JAVA_STACK,
      mapOf("class" to frame.className, "method" to frame.methodName, "line" to frame.lineNumber.toString())) }
    flush()
  }
}

internal fun dispatchFatal(error: Throwable, report: (Throwable) -> Unit, delegate: (Throwable) -> Unit) {
  try { report(error) } catch (_: Throwable) { /* Reporting cannot replace the original failure. */ }
  finally { delegate(error) }
}
