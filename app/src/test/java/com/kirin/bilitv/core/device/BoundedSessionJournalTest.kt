package com.kirin.bilitv.core.device

import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class BoundedSessionJournalTest {
  @Test fun filesRemainBoundedAndLatestEventIsRetained() {
    val directory = Files.createTempDirectory("bilitv-journal-test").toFile()
    try {
      val journal = BoundedSessionJournal(directory)
      repeat(700) { journal.record(SessionEvent.PLAY_ATTEMPT, mapOf("generation" to it.toString(), "phase" to "DECODER_INITIALIZATION")) }
      val file = directory.resolve("current.log")
      assertTrue(file.length() <= BoundedSessionJournal.MaxBytes)
      assertTrue(file.readText().contains("generation=699"))
    } finally { directory.listFiles()?.forEach { assertTrue(it.isFile); assertTrue(it.delete()) }; assertTrue(directory.delete()) }
  }
  @Test fun restartRotatesOnlyCurrentAndPreviousFiles() {
    val directory = Files.createTempDirectory("bilitv-journal-test").toFile()
    try {
      BoundedSessionJournal(directory).record(SessionEvent.FOREGROUND)
      val next = BoundedSessionJournal(directory)
      assertEquals("UNCLEAN_REASON_UNKNOWN",next.previousOutcome)
      next.record(SessionEvent.APP_START)
      BoundedSessionJournal(directory)
      assertEquals(setOf("current.log","previous.log"),directory.listFiles()!!.map { it.name }.toSet())
    } finally { directory.listFiles()?.forEach { assertTrue(it.isFile); assertTrue(it.delete()) }; assertTrue(directory.delete()) }
  }
  @Test fun fatalNeverStoresExceptionMessageOrCredentials() {
    val directory = Files.createTempDirectory("bilitv-journal-test").toFile()
    try {
      BoundedSessionJournal(directory).fatal(IllegalStateException("https://video.example/a?sign=secret Cookie SESSDATA=private-token"))
      val text = directory.resolve("current.log").readText()
      for (secret in listOf("https://","sign=secret","SESSDATA","private-token","Cookie")) assertFalse(text.contains(secret))
      assertEquals("JAVA_FATAL",SafeDiagnosticFields.previousOutcome(text))
    } finally { directory.listFiles()?.forEach { assertTrue(it.isFile); assertTrue(it.delete()) }; assertTrue(directory.delete()) }
  }
  @Test fun fieldAllowlistRedactsSignedUrlsAndRejectsPrivateKeys() {
    val text = SafeDiagnosticFields.line(SessionEvent.PLAY_FAILURE,mapOf("code" to "https://example.invalid?sign=secret","Cookie" to "SESSDATA=private","token" to "private","type" to "CodecException"))
    assertTrue(text.contains("code=REDACTED")); assertTrue(text.contains("type=CodecException"))
    assertFalse(text.contains("SESSDATA")); assertFalse(text.contains("secret")); assertFalse(text.contains("private"))
  }
  @Test fun diagnosticSnapshotRechecksStoredFieldsAndPublicVideoIdentity() {
    val entry = SafeDiagnosticFields.entry(
      "PLAY_FAILURE bvid=BV1GtH16MEjj cid=42544399782 Cookie=SECRET code=https://cdn.example/signed?token=private phase=Decoder",
    )!!
    assertEquals("BV1GtH16MEjj", entry.fields["bvid"])
    assertEquals("42544399782", entry.fields["cid"])
    assertEquals("REDACTED", entry.fields["code"])
    assertFalse(entry.fields.containsKey("Cookie"))
    val redacted = SafeDiagnosticFields.line(SessionEvent.PLAY_FAILURE, mapOf("bvid" to "secret", "cid" to "notId"))
    assertTrue(redacted.contains("bvid=REDACTED cid=REDACTED"))
  }

  @Test fun rotationSanitizesLegacyRecordsBeforeWritingOrShowingThem() {
    val directory = Files.createTempDirectory("bilitv-journal-legacy").toFile()
    try {
      directory.resolve("current.log").writeText("PLAY_FAILURE code=https://cdn.example/?sign=secret Cookie=private\n")
      val journal = BoundedSessionJournal(directory)
      assertEquals(listOf("PLAY_FAILURE code=REDACTED"), journal.previousLines)
      assertEquals("PLAY_FAILURE code=REDACTED\n", directory.resolve("previous.log").readText())
    } finally { directory.listFiles()?.forEach { assertTrue(it.isFile); assertTrue(it.delete()) }; assertTrue(directory.delete()) }
  }

  @Test fun journalShowsAppendedRecordsBeforeTheBackgroundWriterAndRestoresAfterRotation() {
    val directory = Files.createTempDirectory("bilitv-journal-snapshot").toFile()
    try {
      val journal = BoundedSessionJournal(directory)
      journal.append(SessionEvent.PLAY_FAILURE, mapOf("code" to "VIDEO_TRACKS", "attempt" to "4"))
      assertEquals("4", journal.entries(false).single().fields["attempt"])
      assertEquals(0L, directory.resolve("current.log").length())
      journal.flush()
      val current = BoundedSessionJournal(directory)
      assertEquals("VIDEO_TRACKS", current.entries(true).single().fields["code"])
      assertTrue(current.entries(false).isEmpty())
    } finally { directory.listFiles()?.forEach { assertTrue(it.isFile); assertTrue(it.delete()) }; assertTrue(directory.delete()) }
  }

  @Test fun playbackLogLevelsAreOrderedAndMissingSettingsUseInfo() {
    assertEquals(PlaybackLogLevel.Info, PlaybackLogLevel.fromKey(null))
    assertEquals(PlaybackLogLevel.Info, PlaybackLogLevel.fromKey("invalid"))
    for (level in PlaybackLogLevel.entries) assertEquals(level, PlaybackLogLevel.fromKey(level.key))
    assertFalse(PlaybackLogLevel.Off.includes(PlaybackLogLevel.Error))
    assertTrue(PlaybackLogLevel.Error.includes(PlaybackLogLevel.Error))
    assertFalse(PlaybackLogLevel.Error.includes(PlaybackLogLevel.Info))
    assertTrue(PlaybackLogLevel.Info.includes(PlaybackLogLevel.Error))
    assertFalse(PlaybackLogLevel.Info.includes(PlaybackLogLevel.Debug))
    assertTrue(PlaybackLogLevel.Debug.includes(PlaybackLogLevel.Info))
  }
  @Test fun nativeOrSystemKillIsNotMisreportedAsJavaCrash() {
    assertEquals("UNCLEAN_REASON_UNKNOWN",SafeDiagnosticFields.previousOutcome("APP_START\nFOREGROUND\nPLAY_ATTEMPT phase=DECODER_INIT\n"))
  }
  @Test fun backgroundTerminationIsNotMisreportedAsCrash() {
    assertEquals("BACKGROUND_TERMINATION_UNKNOWN",SafeDiagnosticFields.previousOutcome("APP_START\nFOREGROUND\nBACKGROUND\n"))
  }
  @Test fun explicitExitSurvivesLaterStopCallback() {
    assertEquals("EXPLICIT_EXIT",SafeDiagnosticFields.previousOutcome("FOREGROUND\nEXIT_REQUESTED source=NAV_BUTTON\nBACKGROUND\n"))
  }
  @Test fun fatalAlwaysDelegatesTheOriginalException() {
    val error = IllegalStateException("private-token")
    var reports = 0; var delegates = 0
    dispatchFatal(error,{ reports++; assertSame(error,it) },{ delegates++; assertSame(error,it) })
    assertEquals(1,reports); assertEquals(1,delegates)
  }
  @Test fun reportingFailureIncludingOomCannotSwallowOriginalFatal() {
    val original = IllegalStateException("original")
    var delegates = 0
    dispatchFatal(original,{ throw OutOfMemoryError("reporter") },{ assertSame(original,it); delegates++ })
    assertEquals(1,delegates)
  }
  @Test(expected = IllegalStateException::class) fun defaultHandlerFailureStillPropagates() {
    dispatchFatal(IllegalArgumentException(),{}, { throw IllegalStateException("delegate") })
  }
  @Test fun resumedActivityOverridesOldExplicitExit() {
    assertEquals("UNCLEAN_REASON_UNKNOWN",SafeDiagnosticFields.previousOutcome("FOREGROUND\nEXIT_REQUESTED\nBACKGROUND\nFOREGROUND\n"))
  }
}
