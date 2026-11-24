package com.kirin.bilitv.ui.player

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kirin.bilitv.BuildConfig
import com.kirin.bilitv.R
import com.kirin.bilitv.core.device.DeviceCompatibility
import com.kirin.bilitv.core.device.PlaybackLogLevel
import com.kirin.bilitv.core.device.SafeDiagnosticFields
import com.kirin.bilitv.core.device.SessionDiagnostics
import com.kirin.bilitv.core.device.SessionEvent
import com.kirin.bilitv.core.device.SessionLogEntry
import com.kirin.bilitv.core.player.PlaybackTrace
import com.kirin.bilitv.core.settings.AppSettingsStore
import com.kirin.bilitv.ui.focus.BiliFocusableSurface
import com.kirin.bilitv.ui.input.InteractionMode
import com.kirin.bilitv.ui.input.LocalInteractionMode
import com.kirin.bilitv.ui.theme.BiliColors
import com.kirin.bilitv.ui.theme.BiliSizing
import com.kirin.bilitv.ui.theme.BiliSpacing
import com.kirin.bilitv.ui.theme.BiliTypography
import com.kirin.bilitv.ui.theme.LocalUiMetrics
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlaybackLogDialog(
  settings: AppSettingsStore,
  onDismiss: () -> Unit,
  trace: PlaybackTrace? = null,
) {
  val ui = LocalUiMetrics.current
  val tv = LocalInteractionMode.current == InteractionMode.Tv
  val scope = rememberCoroutineScope()
  val level by settings.playbackLogLevel.collectAsState(initial = PlaybackLogLevel.Info)
  var previous by remember { mutableStateOf(false) }
  var refresh by remember { mutableIntStateOf(0) }
  var entries by remember { mutableStateOf<List<SessionLogEntry>>(emptyList()) }
  var expanded by remember { mutableIntStateOf(-1) }
  var savingLevel by remember { mutableStateOf(false) }
  var levelSaveFailed by remember { mutableStateOf(false) }
  var initialSnapshotLoaded by remember { mutableStateOf(false) }
  val levelFocus = remember { FocusRequester() }
  val listState = rememberLazyListState()
  val clock = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

  Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
    LaunchedEffect(trace, previous, refresh) {
      val snapshot = withContext(Dispatchers.Default) {
        SessionDiagnostics.entries(previous).asReversed().filter { entry ->
          previous || trace == null || entry.belongsTo(trace)
        }
      }
      expanded = if (!initialSnapshotLoaded && !previous && trace != null) {
        snapshot.indexOfFirst { it.event == SessionEvent.PLAY_FAILURE }
      } else -1
      entries = snapshot
      if (snapshot.isNotEmpty()) listState.requestScrollToItem(expanded.coerceAtLeast(0))
      initialSnapshotLoaded = true
    }
    LaunchedEffect(tv) {
      if (tv) {
        withFrameNanos { }
        levelFocus.requestFocus()
      }
    }

    Column(
      modifier = Modifier.fillMaxSize().background(BiliColors.PlayerPanel)
        .padding(ui.dp(BiliSizing.ContentPadding)).testTag("playback_log_dialog"),
      verticalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Md)),
    ) {
      Text(stringResource(R.string.playback_log_title), color = BiliColors.TextPrimary, fontSize = ui.sp(BiliTypography.SectionTitle))
      Text(
        stringResource(R.string.playback_log_build, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, Build.VERSION.SDK_INT,
          stringResource(if (DeviceCompatibility.current.isJacinto6) R.string.playback_log_device_chevy else R.string.playback_log_device_default)),
        color = BiliColors.TextSecondary, fontSize = ui.sp(BiliTypography.BodySmall),
      )
      FlowRow(horizontalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Md)),
        verticalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Sm))) {
        PlaybackLogButton(stringResource(R.string.playback_log_level, stringResource(level.label())),
          Modifier.focusRequester(levelFocus).testTag("playback_log_level")) {
          if (!savingLevel) {
            savingLevel = true
            scope.launch {
              try {
                settings.setPlaybackLogLevel(PlaybackLogLevel.entries[(level.ordinal + 1) % PlaybackLogLevel.entries.size])
                levelSaveFailed = false
              } catch (_: IOException) {
                levelSaveFailed = true
              } finally {
                savingLevel = false
              }
            }
          }
        }
        val sessionLabel = if (previous) R.string.playback_log_previous else R.string.playback_log_current
        PlaybackLogButton(stringResource(sessionLabel), Modifier.testTag("playback_log_session")) { previous = !previous }
        PlaybackLogButton(stringResource(R.string.playback_log_refresh)) { refresh++ }
        PlaybackLogButton(stringResource(R.string.player_control_back), Modifier.testTag("playback_log_back"), onDismiss)
      }
      if (levelSaveFailed) {
        Text(stringResource(R.string.playback_log_save_failed), color = BiliColors.TextSecondary, fontSize = ui.sp(BiliTypography.BodySmall))
      }
      if (!previous && trace != null && SafeDiagnosticFields.isPublicVideoId(trace.bvid)) {
        Text(stringResource(R.string.playback_log_video, trace.bvid, trace.cid),
          color = BiliColors.TextSecondary, fontSize = ui.sp(BiliTypography.BodySmall))
      }
      if (entries.isEmpty()) {
        Text(stringResource(R.string.playback_log_empty), color = BiliColors.TextSecondary, fontSize = ui.sp(BiliTypography.Body))
      }
      LazyColumn(
        state = listState,
        modifier = Modifier.weight(1f).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Sm)),
      ) {
        itemsIndexed(entries) { index, entry ->
          BiliFocusableSurface(
            modifier = Modifier.fillMaxWidth(),
            scaleOnFocus = false,
            onClick = { expanded = if (expanded == index) -1 else index },
          ) {
            Column(Modifier.fillMaxWidth().padding(ui.dp(BiliSpacing.Md))) {
              Text(entry.heading(clock), color = BiliColors.TextPrimary, fontSize = ui.sp(BiliTypography.BodySmall))
              if (expanded == index) {
                Text(entry.detail(), color = BiliColors.TextSecondary, fontSize = ui.sp(BiliTypography.BodySmall),
                  modifier = Modifier.padding(top = ui.dp(BiliSpacing.Sm)))
              }
            }
          }
        }
      }
    }
  }
}

@Composable
internal fun PlayerFailureLogActions(message: String, settings: AppSettingsStore, trace: PlaybackTrace?, onRetry: () -> Unit) {
  val ui = LocalUiMetrics.current
  val tv = LocalInteractionMode.current == InteractionMode.Tv
  val retryFocus = remember { FocusRequester() }
  val logFocus = remember { FocusRequester() }
  var showingLog by remember { mutableStateOf(false) }
  var returnToLog by remember { mutableStateOf(false) }
  LaunchedEffect(showingLog, tv) {
    if (tv && !showingLog) {
      withFrameNanos { }
      (if (returnToLog) logFocus else retryFocus).requestFocus()
    }
  }
  Column(Modifier.fillMaxSize().padding(horizontal = ui.dp(BiliSizing.ContentPadding)),
    verticalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Lg), Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally) {
    Text(message, color = BiliColors.TextSecondary, fontSize = ui.sp(BiliTypography.Body), textAlign = TextAlign.Center)
    Row(horizontalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Lg))) {
      PlaybackLogButton(stringResource(R.string.action_retry),
        Modifier.focusRequester(retryFocus).focusProperties { right = logFocus }.testTag("playback_failed_retry"), onRetry)
      PlaybackLogButton(stringResource(R.string.playback_log_open),
        Modifier.focusRequester(logFocus).focusProperties { left = retryFocus }.testTag("playback_failed_log")) {
        returnToLog = true
        showingLog = true
      }
    }
  }
  if (showingLog) PlaybackLogDialog(settings, onDismiss = { showingLog = false }, trace = trace)
}

@Composable
private fun PlaybackLogButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
  val ui = LocalUiMetrics.current
  BiliFocusableSurface(modifier = modifier, scaleOnFocus = false, onClick = onClick) {
    Text(text, color = BiliColors.TextPrimary, fontSize = ui.sp(BiliTypography.BodySmall),
      modifier = Modifier.padding(horizontal = ui.dp(BiliSpacing.Lg), vertical = ui.dp(BiliSpacing.Md)))
  }
}

private fun PlaybackLogLevel.label(): Int = when (this) {
  PlaybackLogLevel.Off -> R.string.playback_log_level_off
  PlaybackLogLevel.Error -> R.string.playback_log_level_error
  PlaybackLogLevel.Info -> R.string.playback_log_level_info
  PlaybackLogLevel.Debug -> R.string.playback_log_level_debug
}

private fun SessionLogEntry.belongsTo(trace: PlaybackTrace): Boolean =
  fields["session"] == trace.session.toString() && fields["bvid"] == trace.bvid &&
    (trace.cid <= 0 || fields["cid"] == null || fields["cid"] == trace.cid.toString())

private fun SessionLogEntry.heading(clock: SimpleDateFormat): String = listOfNotNull(
  fields["at"]?.toLongOrNull()?.let { clock.format(Date(it)) },
  fields["bvid"], fields["attempt"]?.let { "#$it" }, fields["generation"]?.let { "g$it" }, event.name, fields["phase"], fields["code"],
).joinToString("  ")

private fun SessionLogEntry.detail(): String = fields.entries.filterNot { it.key == "at" }
  .chunked(3).joinToString("\n") { row -> row.joinToString("   ") { (key, value) -> "$key=$value" } }
