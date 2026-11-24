package com.kirin.bilitv.core.player

internal enum class PlaybackFailureKind { Decoder, UnsupportedVideo, VideoTracks, Output, Network, Audio, Other }

internal data class PlaybackAttemptLimit(val qualityId: Int, val longEdge: Int, val shortEdge: Int) {
  fun permits(track: PlaybackTrack): Boolean = track.width > 0 && track.height > 0 &&
    maxOf(track.width, track.height) <= longEdge && minOf(track.width, track.height) <= shortEdge
  companion object {
    val P720 = PlaybackAttemptLimit(64, 1280, 720)
    val P480 = PlaybackAttemptLimit(32, 854, 480)
    val P360 = PlaybackAttemptLimit(16, 640, 360)
    val RecoveryTiers = listOf(P720, P480, P360)
  }
}

internal class PlaybackRecoveryPolicy {
  var generation = 0L
    private set
  var attemptCount = 1
    private set
  var limit: PlaybackAttemptLimit? = null
    private set
  var qualityId: Int? = null
    private set
  var videoId: String? = null
    private set
  var resolvedCid: Long? = null
  val recoveryCid: Long? get() = if (resumeRequested) resolvedCid else null
  var lastPositionMs = 0L
    private set
  var pausedByUser = false
  private var resumeRequested = false
  val resumePositionOverride: Long? get() = if (resumeRequested) lastPositionMs else null
  private var firstSelectedQualityId: Int? = null
  private var sameQualityTried = false
  private val levelTrials = mutableSetOf<Pair<String, Long>>()
  private var levelTrialSubmitted = false

  fun start(quality: Int? = null, positionMs: Long = 0L, bvid: String? = null, cid: Long? = null) {
    val nextCid = cid?.takeIf { it > 0L }
    pausedByUser = pausedByUser && bvid == videoId && nextCid != null && nextCid == resolvedCid
    generation++
    attemptCount = 1
    limit = null
    qualityId = quality
    videoId = bvid
    resolvedCid = nextCid
    lastPositionMs = positionMs.coerceAtLeast(0L)
    resumeRequested = false
    firstSelectedQualityId = null
    sameQualityTried = false
    levelTrialSubmitted = false
  }

  fun updatePosition(positionMs: Long) {
    if (positionMs >= 0L) lastPositionMs = positionMs
  }

  fun accepted(info: PlaybackInfo) {
    videoId = info.bvid
    resolvedCid = info.cid.takeIf { it > 0L }
    qualityId = info.selectedQuality.id
    if (firstSelectedQualityId == null) firstSelectedQualityId = qualityId
    limit = PlaybackAttemptLimit.RecoveryTiers.firstOrNull { it.qualityId == qualityId }
  }

  fun canTryLevel(bvid: String, cid: Long): Boolean =
    qualityId == PlaybackAttemptLimit.P360.qualityId && bvid.isNotBlank() && cid > 0 && (bvid to cid) !in levelTrials

  fun submitLevelTrial(token: Long, info: PlaybackInfo): Boolean {
    if (!isCurrent(token) || info.videoSelectionMode != PlaybackVideoSelectionMode.LevelTrial ||
      !canTryLevel(info.bvid, info.cid)
    ) return false
    levelTrials.add(info.bvid to info.cid)
    levelTrialSubmitted = true
    return true
  }

  fun isCurrent(token: Long): Boolean = token == generation
  fun invalidate() { generation++ }

  fun next(failure: PlaybackFailureKind): Boolean {
    if (failure !in setOf(PlaybackFailureKind.Decoder, PlaybackFailureKind.UnsupportedVideo, PlaybackFailureKind.VideoTracks)) return false
    if (levelTrialSubmitted || attemptCount >= 5) return false
    if (failure == PlaybackFailureKind.Decoder && firstSelectedQualityId != null &&
      qualityId == firstSelectedQualityId && !sameQualityTried
    ) {
      sameQualityTried = true
    } else {
      val nextLimit = PlaybackAttemptLimit.RecoveryTiers.firstOrNull { candidate ->
        candidate.qualityId < (qualityId ?: Int.MAX_VALUE)
      } ?: return false
      limit = nextLimit
      qualityId = nextLimit.qualityId
    }
    resumeRequested = true
    attemptCount++
    generation++
    return true
  }

  fun manualRetry() {
    resumeRequested = true
    generation++
    attemptCount = 1
    firstSelectedQualityId = null
    sameQualityTried = false
    levelTrialSubmitted = false
  }
}
