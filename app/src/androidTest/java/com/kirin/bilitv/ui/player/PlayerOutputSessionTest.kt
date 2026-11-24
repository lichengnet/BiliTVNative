package com.kirin.bilitv.ui.player

import android.content.Context
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.TextureView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.kirin.bilitv.R
import org.junit.Assert.*
import androidx.media3.common.Player
import com.kirin.bilitv.core.player.HardwareAvcDecoders
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import org.junit.Rule
import org.junit.Test

class PlayerOutputSessionTest {
  @get:Rule val composeRule = createComposeRule()

  private fun context(): Context {
    lateinit var context: Context
    composeRule.setContent { context = LocalContext.current }
    composeRule.waitForIdle()
    return context
  }

  @Test fun frameRateParsingUsesLegacyAndroidMathMethods() {
    assertEquals("30",com.kirin.bilitv.core.player.PlaybackFrameRate.parse("60/2")?.dashValue)
    assertEquals("2997/100",com.kirin.bilitv.core.player.PlaybackFrameRate.parse("29.97")?.dashValue)
  }

  @Test fun releaseCancelsPendingTextureWait() {
    val context = context()
    composeRule.runOnIdle {
      val player = createCompatiblePlayer(context, true)
      val session = PlayerOutputSession(player)
      runBlocking {
        val wait = launch(start = CoroutineStart.UNDISPATCHED) { session.awaitTextureOutput() }
        session.release()
        wait.join()
        assertTrue(wait.isCancelled)
        assertTrue(session.isReleased)
      }
    }
  }

  @Test fun textureWaitTimesOutWithoutPreparingOrPlaying() {
    val context = context()
    composeRule.runOnIdle {
      val player = createCompatiblePlayer(context, true)
      val session = PlayerOutputSession(player)
      try {
        var timedOut = false
        runBlocking {
          try { withTimeout(30) { session.awaitTextureOutput() } }
          catch (_: TimeoutCancellationException) { timedOut = true }
        }
        assertTrue(timedOut)
        assertEquals(Player.STATE_IDLE, player.playbackState)
        assertFalse(player.playWhenReady)
      } finally { session.release() }
    }
  }

  @Test fun rebuiltSessionUsesDifferentPlayerAndOldReleaseCannotUnbindIt() {
    val context = context()
    composeRule.runOnIdle {
      val oldPlayer = createCompatiblePlayer(context, true)
      val oldSession = PlayerOutputSession(oldPlayer)
      oldSession.release()
      val nextPlayer = createCompatiblePlayer(context, true)
      val nextSession = PlayerOutputSession(nextPlayer)
      val view = LayoutInflater.from(context).inflate(R.layout.player_view_texture, null, false) as PlayerView
      try {
        nextSession.attach(view)
        oldSession.release()
        oldSession.detach(view)
        assertNotSame(oldPlayer, nextPlayer)
        assertSame(nextPlayer, view.player)
        assertTrue(oldSession.isReleased)
      } finally { nextSession.release() }
    }
  }

  @Test fun videoDecoderSelectorNeverAdmitsSoftwareAndAudioRemainsDefault() {
    context()
    composeRule.runOnIdle {
      val video = HardwareAvcDecoders.selector.getDecoderInfos("video/avc", false, false)
      assertTrue(video.all { it.hardwareAccelerated && !it.softwareOnly })
      assertTrue(HardwareAvcDecoders.selector.getDecoderInfos("video/hevc", false, false).isEmpty())
      assertEquals(MediaCodecSelector.DEFAULT.getDecoderInfos("audio/mp4a-latm", false, false).map { it.name },
        HardwareAvcDecoders.selector.getDecoderInfos("audio/mp4a-latm", false, false).map { it.name })
    }
  }

  @Test fun textureXmlSelectsTextureViewAndDefaultRemainsSurfaceView() {
    val context = context()
    composeRule.runOnIdle {
      val view = LayoutInflater.from(context).inflate(R.layout.player_view_texture, null, false) as PlayerView
      assertTrue(view.videoSurfaceView is TextureView)
      assertTrue(PlayerView(context).videoSurfaceView is SurfaceView)
    }
  }

  @Test fun textureListenerIsRemovedAfterDetach() {
    val context = context()
    composeRule.runOnIdle {
      val player = ExoPlayer.Builder(context).build()
      val session = PlayerOutputSession(player)
      val view = LayoutInflater.from(context).inflate(R.layout.player_view_texture, null, false) as PlayerView
      try {
        session.attach(view)
        val texture = view.videoSurfaceView as TextureView
        assertNotNull(texture.surfaceTextureListener)
        session.detach(view)
        assertNull(view.player)
        assertNull(texture.surfaceTextureListener)
      } finally {
        session.release()
      }
    }
  }

  @Test fun oldSessionCannotClearViewOwnedByNewPlayer() {
    val context = context()
    composeRule.runOnIdle {
      val oldPlayer = ExoPlayer.Builder(context).build()
      val newPlayer = ExoPlayer.Builder(context).build()
      val oldSession = PlayerOutputSession(oldPlayer)
      val newSession = PlayerOutputSession(newPlayer)
      val view = PlayerView(context)
      try {
        oldSession.attach(view)
        newSession.attach(view)
        view.keepScreenOn = true
        oldSession.release()
        assertSame(newPlayer, view.player)
        assertTrue(view.keepScreenOn)
      } finally {
        oldSession.release()
        newSession.release()
      }
    }
  }

  @Test fun staleViewReleaseDoesNotDetachNewViewAndCleanupIsIdempotent() {
    val context = context()
    composeRule.runOnIdle {
      val player = ExoPlayer.Builder(context).build()
      val session = PlayerOutputSession(player)
      val oldView = PlayerView(context)
      val newView = PlayerView(context)
      try {
        session.attach(oldView)
        session.attach(newView)
        session.detach(oldView)
        assertNull(oldView.player)
        assertSame(player, newView.player)
        session.stop()
        session.stop()
        session.release()
        session.release()
        assertTrue(session.isReleased)
        assertNull(newView.player)
        session.attach(newView)
        assertNull(newView.player)
      } finally {
        session.release()
      }
    }
  }
}
