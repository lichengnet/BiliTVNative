package com.kirin.bilitv.core.device

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.util.AtomicFile
import com.kirin.bilitv.BuildConfig
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

internal object SessionDiagnostics {
  @Volatile private var journal: BoundedSessionJournal? = null
  @Volatile private var playbackLevel = PlaybackLogLevel.Off
  private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val flushRequests = Channel<Unit>(Channel.CONFLATED)

  fun install(application: Application) {
    if (!CompatibilityDiagnostics.enabled) return
    try {
      val directory = application.filesDir.resolve("compat_session_diag")
      for (name in listOf("current.log", "previous.log")) {
        try {
          AtomicFile(directory.resolve(name)).openRead().close()
        } catch (_: FileNotFoundException) {
          // A new installation has no log to recover.
        }
      }
      val created = BoundedSessionJournal(directory, ::writeAtomically)
      journal = created
      ioScope.launch {
        for (request in flushRequests) {
          try {
            created.flush()
          } catch (_: IOException) {
            CompatibilityDiagnostics.record("session journal flush failed")
          }
        }
      }
      CompatibilityDiagnostics.record("previous session=" + created.previousOutcome)
      created.previousLines.forEach { CompatibilityDiagnostics.record("previous " + it) }
      event(SessionEvent.APP_START, mapOf(
        "version" to BuildConfig.VERSION_NAME,
        "versionCode" to BuildConfig.VERSION_CODE.toString(),
        "api" to Build.VERSION.SDK_INT.toString(),
        "jacinto6" to DeviceCompatibility.current.isJacinto6.toString(),
      ))
    } catch (_: Exception) { CompatibilityDiagnostics.record("session journal unavailable") }
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    if (previous != null) Thread.setDefaultUncaughtExceptionHandler { thread, error ->
      dispatchFatal(error, { journal?.fatal(it) }, { previous.uncaughtException(thread, it) })
    }
    var startedActivities = 0
    application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
      override fun onActivityResumed(activity: Activity) { event(SessionEvent.FOREGROUND) }
      override fun onActivityStopped(activity: Activity) {
        startedActivities = (startedActivities - 1).coerceAtLeast(0)
        if (startedActivities == 0) event(SessionEvent.BACKGROUND)
      }
      override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
      override fun onActivityStarted(activity: Activity) { startedActivities++ }
      override fun onActivityPaused(activity: Activity) = Unit
      override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
      override fun onActivityDestroyed(activity: Activity) = Unit
    })
  }

  fun observePlaybackLevel(level: Flow<PlaybackLogLevel>) {
    if (CompatibilityDiagnostics.enabled) ioScope.launch {
      level.catch { if (it is IOException) emit(PlaybackLogLevel.Info) else throw it }
        .collect { playbackLevel = it }
    }
  }

  fun entries(previous: Boolean): List<SessionLogEntry> = journal?.entries(previous).orEmpty()

  fun captures(level: PlaybackLogLevel): Boolean = CompatibilityDiagnostics.enabled && playbackLevel.includes(level)

  fun event(
    kind: SessionEvent,
    fields: Map<String, String> = emptyMap(),
    memory: Boolean = false,
    level: PlaybackLogLevel? = null,
  ) {
    if (!CompatibilityDiagnostics.enabled || (level != null && !captures(level))) return
    try {
      val values = mapOf("at" to System.currentTimeMillis().toString()) + fields +
        (if (memory) memoryFields() else emptyMap())
      val activeJournal = journal
      CompatibilityDiagnostics.record(activeJournal?.append(kind, values) ?: SafeDiagnosticFields.line(kind, values))
      if (kind == SessionEvent.EXIT_REQUESTED) activeJournal?.flush() else flushRequests.trySend(Unit)
    } catch (_: Throwable) { /* Diagnostics must not interrupt playback or replace a real fatal error. */ }
  }

  private fun memoryFields(): Map<String, String> {
    val runtime = Runtime.getRuntime()
    return mapOf("javaUsed" to (runtime.totalMemory() - runtime.freeMemory()).toString(),
      "javaMax" to runtime.maxMemory().toString(), "native" to Debug.getNativeHeapAllocatedSize().toString(), "pss" to Debug.getPss().toString())
  }

  private fun writeAtomically(file: File, value: String) {
    val atomic = AtomicFile(file)
    val output = atomic.startWrite()
    try {
      output.write(value.toByteArray(Charsets.UTF_8))
      atomic.finishWrite(output)
    } catch (error: Throwable) {
      atomic.failWrite(output)
      throw error
    }
  }
}
