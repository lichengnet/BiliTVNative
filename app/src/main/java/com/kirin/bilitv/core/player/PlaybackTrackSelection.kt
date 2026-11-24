package com.kirin.bilitv.core.player

internal fun selectPlayableVideoTracks(
  tracks: List<PlaybackTrack>,
  preference: PlaybackCodecPreference,
  capability: CodecCapability,
): List<PlaybackTrack> {
  val preferred = tracks.filter { it.isPlayable(preference, capability) }
  // A fixed H.264 setting must never silently admit HEVC/AV1.
  if (preference == PlaybackCodecPreference.H264) return preferred
  return preferred.ifEmpty {
    tracks.filter { it.isPlayable(PlaybackCodecPreference.Auto, capability) }
  }.ifEmpty { tracks }
}

private fun PlaybackTrack.isPlayable(
  codecPreference: PlaybackCodecPreference,
  capability: CodecCapability,
): Boolean {
  when (codecPreference) {
    PlaybackCodecPreference.H264 -> return isH264
    PlaybackCodecPreference.H265 -> return isH265
    PlaybackCodecPreference.Av1 -> return isAv1
    PlaybackCodecPreference.Auto -> Unit
  }
  return when {
    isAv1 -> capability.supportsAv1
    isH265 -> capability.supportsH265
    isH264 -> capability.supportsH264
    else -> true
  }
}
