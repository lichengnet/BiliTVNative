package com.kirin.bilitv.core.player

internal enum class AvcTrackDecision {
  Supported, CapabilityUnknown, NotAvc, QualityLimit, Dimensions, NoHardwareDecoder, CapabilityRejected, LevelOnly,
}

internal data class AvcTrackAssessment(
  val decision: AvcTrackDecision,
  val decoder: String? = null,
  val advertisedLevel: Int? = null,
)

internal data class AvcTrackEvaluation(val track: PlaybackTrack, val assessment: AvcTrackAssessment)

internal data class BoundedAvcTrackSelection(
  val tracks: List<PlaybackTrack>,
  val evaluations: List<AvcTrackEvaluation>,
  val mode: PlaybackVideoSelectionMode = PlaybackVideoSelectionMode.Normal,
)

internal fun selectBoundedHardwareAvcTracks(
  tracks: List<PlaybackTrack>,
  selectedQualityId: Int,
  limit: PlaybackAttemptLimit?,
  levelTrial: LevelTrialAvailability = LevelTrialAvailability.Unavailable,
  assessHardware: (PlaybackTrack) -> AvcTrackAssessment,
): BoundedAvcTrackSelection {
  val maxQuality = minOf(selectedQualityId, limit?.qualityId ?: selectedQualityId)
  val evaluations = tracks.map { track ->
    val assessment = when {
      !track.isH264 -> AvcTrackAssessment(AvcTrackDecision.NotAvc)
      track.id !in 1..maxQuality -> AvcTrackAssessment(AvcTrackDecision.QualityLimit)
      track.width !in 1..1920 || track.height !in 1..1088 || limit?.permits(track) == false ->
        AvcTrackAssessment(AvcTrackDecision.Dimensions)
      else -> assessHardware(track)
    }
    AvcTrackEvaluation(track, assessment)
  }
  val regular = evaluations.filter {
    it.assessment.decision == AvcTrackDecision.Supported || it.assessment.decision == AvcTrackDecision.CapabilityUnknown
  }
  val highest = regular.maxOfOrNull { it.track.id }
  if (highest != null) {
    val selected = regular.map { it.track }.filter { it.id == highest }.sortedBy { it.bandwidth }
    return BoundedAvcTrackSelection(selected, evaluations)
  }
  if (levelTrial != LevelTrialAvailability.Unavailable && limit == PlaybackAttemptLimit.P360) {
    val trial = evaluations.asSequence().filter { it.assessment.decision == AvcTrackDecision.LevelOnly }
      .map { it.track }.filter(PlaybackTrack::isLevelTrialCandidate)
      .minByOrNull { it.bandwidth.takeIf { rate -> rate > 0 } ?: Int.MAX_VALUE }
    if (trial != null) {
      return if (levelTrial == LevelTrialAvailability.Available) {
        BoundedAvcTrackSelection(listOf(trial), evaluations, PlaybackVideoSelectionMode.LevelTrial)
      } else {
        BoundedAvcTrackSelection(emptyList(), evaluations, PlaybackVideoSelectionMode.LevelTrialUsed)
      }
    }
  }
  return BoundedAvcTrackSelection(emptyList(), evaluations)
}

private val AvcCodecString = Regex("avc[13]\\.[0-9a-f]{6}", RegexOption.IGNORE_CASE)

internal fun PlaybackTrack.isLevelTrialCandidate(): Boolean {
  val frameRate = PlaybackFrameRate.parse(frameRate) ?: return false
  return id == PlaybackAttemptLimit.P360.qualityId &&
    width in 1..PlaybackAttemptLimit.P360.longEdge && height in 1..PlaybackAttemptLimit.P360.shortEdge &&
    AvcCodecString.matches(codecs) && frameRate.numerator.toLong() <= 30L * frameRate.denominator
}
