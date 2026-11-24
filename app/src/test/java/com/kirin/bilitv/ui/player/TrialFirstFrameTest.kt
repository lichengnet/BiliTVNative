package com.kirin.bilitv.ui.player

import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrialFirstFrameTest {
  @Test fun foregroundWithoutFrameTimesOut() = runBlocking {
    assertFalse(withTimeout(2000) {
      awaitTrialFirstFrame(CompletableDeferred(), flowOf(Lifecycle.State.RESUMED), 5)
    })
  }

  @Test fun firstFrameWinsAndCancelsTheWaitingTimer() = runBlocking {
    val frame = CompletableDeferred(Unit)
    assertTrue(withTimeout(2000) {
      awaitTrialFirstFrame(frame, flowOf(Lifecycle.State.RESUMED), 1000)
    })
  }

  @Test fun backgroundDoesNotExpireTheForegroundAttempt() = runBlocking {
    val lifecycle = MutableStateFlow(Lifecycle.State.STARTED)
    val frame = CompletableDeferred<Unit>()
    val attempt = async { awaitTrialFirstFrame(frame, lifecycle, 5) }
    assertFalse(withTimeout(2000) {
      delay(30)
      assertFalse(attempt.isCompleted)
      lifecycle.value = Lifecycle.State.RESUMED
      attempt.await()
    })
  }
}
