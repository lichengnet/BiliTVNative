package com.kirin.bilitv.ui.player

import com.kirin.bilitv.core.player.PlaybackFailureKind
import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerFailureDetailsTest {
  @Test fun decoderInitializationAndRuntimeErrorsAreRecoverableVideoFailures() {
    for (code in listOf(PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
      PlaybackException.ERROR_CODE_DECODING_FAILED, PlaybackException.ERROR_CODE_DECODING_RESOURCES_RECLAIMED))
      assertEquals(PlaybackFailureKind.Decoder,videoFailureKind(code,true,false))
  }
  @Test fun unsupportedVideoSkipsSameFormatRetry() {
    assertEquals(PlaybackFailureKind.UnsupportedVideo,videoFailureKind(PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,true,false))
    assertEquals(PlaybackFailureKind.UnsupportedVideo,videoFailureKind(PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,true,false))
  }
  @Test fun audioFailureNeverTriggersVideoDowngrade() { assertEquals(PlaybackFailureKind.Audio,videoFailureKind(PlaybackException.ERROR_CODE_DECODING_FAILED,false,true)) }
  @Test fun networkAndAuthErrorsNeverTriggerVideoDowngrade() {
    for(code in listOf(2001,2002,2004)) assertEquals(PlaybackFailureKind.Network,videoFailureKind(code,false,false))
  }
  @Test fun unknownRendererIsNotAssumedToBeVideo() { assertEquals(PlaybackFailureKind.Other,videoFailureKind(PlaybackException.ERROR_CODE_DECODING_FAILED,false,false)) }
}
