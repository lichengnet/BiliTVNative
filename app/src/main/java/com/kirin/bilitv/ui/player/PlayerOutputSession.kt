package com.kirin.bilitv.ui.player

import android.graphics.SurfaceTexture
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.kirin.bilitv.core.device.CompatibilityDiagnostics
import com.kirin.bilitv.core.device.SessionEvent
import com.kirin.bilitv.core.player.PlaybackTrace
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlin.time.TimeSource

internal class PlayerOutputSession(private val player: Player) {
  private var boundView: PlayerView? = null
  private var surfaceCallback: SurfaceHolder.Callback? = null
  private var attachListener: View.OnAttachStateChangeListener? = null
  private var textureListener: TextureView.SurfaceTextureListener? = null
  private var previousTextureListener: TextureView.SurfaceTextureListener? = null
  private var stopped = false
  private val outputReady = CompletableDeferred<Unit>()
  private val firstFrame = CompletableDeferred<Unit>()

  suspend fun awaitTextureOutput() {
    outputReady.await()
    check(!isReleased && (boundView?.videoSurfaceView as? TextureView)?.isAvailable == true) { "OUTPUT_UNAVAILABLE" }
  }
  var isReleased: Boolean = false
    private set
  val diagnosticId: Int = nextId.incrementAndGet()
  var mediaGeneration: Long? = null
  var startPositionApplied: Boolean = false
  var trace: PlaybackTrace? = null
  val hasRenderedFirstFrame: Boolean get() = !isReleased && firstFrame.isCompleted

  fun onFirstFrame() {
    if (!isReleased) firstFrame.complete(Unit)
  }

  suspend fun awaitTrialFirstFrame(lifecycle: Lifecycle, timeoutMs: Long): Boolean =
    awaitTrialFirstFrame(firstFrame, lifecycle.currentStateFlow, timeoutMs)

  fun attach(view: PlayerView) {
    if (isReleased || boundView === view) return
    boundView?.let(::detach)
    boundView = view
    view.player = player
    CompatibilityDiagnostics.record("player=$diagnosticId output bind type=${view.videoSurfaceView?.javaClass?.simpleName}")
    attachListener = object : View.OnAttachStateChangeListener {
      override fun onViewAttachedToWindow(v: View) {
        CompatibilityDiagnostics.record("player=$diagnosticId output attached")
      }
      override fun onViewDetachedFromWindow(v: View) {
        CompatibilityDiagnostics.record("player=$diagnosticId output detached")
      }
    }.also(view::addOnAttachStateChangeListener)
    val surface = view.videoSurfaceView as? SurfaceView
    if (surface != null) {
      surfaceCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
          CompatibilityDiagnostics.record("player=$diagnosticId surface created")
        }
        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
          CompatibilityDiagnostics.record("player=$diagnosticId surface changed size=${width}x$height")
        }
        override fun surfaceDestroyed(holder: SurfaceHolder) {
          CompatibilityDiagnostics.record("player=$diagnosticId surface destroyed")
        }
      }.also(surface.holder::addCallback)
    }
    val texture = view.videoSurfaceView as? TextureView
    if (texture != null) {
      // Delegate every callback; diagnostics must not change Media3's Surface ownership.
      val delegate = texture.surfaceTextureListener
      previousTextureListener = delegate
      textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
          CompatibilityDiagnostics.record("player=$diagnosticId texture created size=${width}x$height")
          delegate?.onSurfaceTextureAvailable(surface, width, height)
          if (!isReleased && boundView === view) outputReady.complete(Unit)
        }
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
          CompatibilityDiagnostics.record("player=$diagnosticId texture changed size=${width}x$height")
          delegate?.onSurfaceTextureSizeChanged(surface, width, height)
        }
        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
          CompatibilityDiagnostics.record("player=$diagnosticId texture destroyed")
          return delegate?.onSurfaceTextureDestroyed(surface) ?: true
        }
        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
          delegate?.onSurfaceTextureUpdated(surface)
        }
      }.also { texture.surfaceTextureListener = it }
      if (texture.isAvailable && texture.surfaceTexture != null) outputReady.complete(Unit)
      CompatibilityDiagnostics.record("player=$diagnosticId texture bound available=${texture.isAvailable}")
    }
  }

  fun detach(view: PlayerView) {
    // AndroidView can release an obsolete view after a replacement has already bound.
    if (boundView !== view) return
    val texture = view.videoSurfaceView as? TextureView
    if (texture != null && texture.surfaceTextureListener === textureListener) {
      texture.surfaceTextureListener = previousTextureListener
    }
    textureListener = null
    previousTextureListener = null
    attachListener?.let(view::removeOnAttachStateChangeListener)
    surfaceCallback?.let { (view.videoSurfaceView as? SurfaceView)?.holder?.removeCallback(it) }
    attachListener = null
    surfaceCallback = null
    boundView = null
    if (view.player === player) {
      view.keepScreenOn = false
      view.player = null
    }
    player.clearVideoSurface()
    CompatibilityDiagnostics.record("player=$diagnosticId output unbind")
  }

  fun stop() {
    if (isReleased || stopped) return
    stopped = true
    try { player.pause() } catch (error: Exception) { cleanupFailure("pause", error) }
    try { player.stop() } catch (error: Exception) { cleanupFailure("stop", error); release() }
    CompatibilityDiagnostics.record("player=$diagnosticId stopped")
  }

  private var releaseSucceeded = true
  fun release(): Boolean {
    if (isReleased) return releaseSucceeded
    isReleased = true
    outputReady.cancel()
    firstFrame.cancel()
    try {
      if (!stopped) {
        stopped = true
        try { player.pause() } catch (error: Exception) { cleanupFailure("pause", error) }
        try { player.stop() } catch (error: Exception) { cleanupFailure("stop", error) }
      }
    } finally {
      try {
        try {
          val view = boundView
          if (view != null) detach(view) else player.clearVideoSurface()
        } catch (error: Exception) { cleanupFailure("unbind", error) }
      } finally {
        try { player.release() }
        catch (error: Exception) { releaseSucceeded = false; cleanupFailure("release", error) }
        CompatibilityDiagnostics.record("player=$diagnosticId released success=$releaseSucceeded")
        if (releaseSucceeded) trace?.event(SessionEvent.PLAY_RELEASE, "RELEASED")
      }
    }
    return releaseSucceeded
  }

  private fun cleanupFailure(phase: String, error: Exception) {
    trace?.event(SessionEvent.PLAY_FAILURE, phase, mapOf("type" to error.javaClass.simpleName, "code" to "CLEANUP_FAILED"))
  }

  private companion object {
    val nextId = AtomicInteger()
  }
}

internal suspend fun awaitTrialFirstFrame(
  firstFrame: CompletableDeferred<Unit>,
  lifecycleState: Flow<Lifecycle.State>,
  timeoutMs: Long,
): Boolean = coroutineScope {
  val expired = CompletableDeferred<Unit>()
  val timeout = launch {
    var remainingMs = timeoutMs
    lifecycleState.collectLatest { state ->
      if (state.isAtLeast(Lifecycle.State.RESUMED)) {
        val start = TimeSource.Monotonic.markNow()
        try {
          delay(remainingMs)
          expired.complete(Unit)
        } finally {
          remainingMs = (remainingMs - start.elapsedNow().inWholeMilliseconds).coerceAtLeast(0)
        }
      }
    }
  }
  try {
    select {
      firstFrame.onAwait { true }
      expired.onAwait { false }
    }
  } finally {
    timeout.cancel()
  }
}
