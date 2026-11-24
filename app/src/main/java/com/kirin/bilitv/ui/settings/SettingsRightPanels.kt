package com.kirin.bilitv.ui.settings

import com.kirin.bilitv.ui.theme.LocalUiMetrics

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.kirin.bilitv.R
import com.kirin.bilitv.core.model.HomeSection
import com.kirin.bilitv.core.settings.AppSettings
import com.kirin.bilitv.ui.focus.BiliFocusableSurface
import com.kirin.bilitv.ui.glass.biliLiquidGlassSurface
import com.kirin.bilitv.ui.home.adaptiveGridColumns
import com.kirin.bilitv.ui.home.titleRes
import com.kirin.bilitv.ui.login.createQrCodeBitmap
import com.kirin.bilitv.ui.theme.BiliFocus
import com.kirin.bilitv.ui.theme.BiliRadius
import com.kirin.bilitv.ui.theme.BiliSizing
import com.kirin.bilitv.ui.theme.BiliSpacing
import com.kirin.bilitv.ui.theme.BiliTypography
import com.kirin.bilitv.ui.theme.LocalHomeColors

@Composable
internal fun SettingsHomeSectionsColumn(
  settings: AppSettings,
  gridState: LazyGridState,
  firstItemFocusRequester: FocusRequester,
  onMoveLeftToSettings: () -> Boolean,
  onHomeSectionEnabledChange: (HomeSection, Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val ui = LocalUiMetrics.current
  val homeColors = LocalHomeColors.current
  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
  val columns = adaptiveGridColumns(maxWidth.value, ui.dp(BiliSizing.SettingsSectionPreferredMinWidth).value, ui.dp(BiliSpacing.Md).value)
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Md)),
  ) {
    Text(
      text = stringResource(R.string.settings_home_sections_section),
      color = homeColors.textSecondary,
      fontSize = ui.sp(BiliTypography.SectionTitle),
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = stringResource(R.string.settings_home_sections_description),
      color = homeColors.textSecondary,
      fontSize = ui.sp(BiliTypography.BodySmall),
    )
    LazyVerticalGrid(
      state = gridState,
      columns = GridCells.Fixed(columns),
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      horizontalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Md)),
      verticalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Md)),
      userScrollEnabled = true,
    ) {
      itemsIndexed(HomeSection.DefaultOrder, key = { _, section -> section.key }) { index, section ->
        HomeSectionChip(
          title = stringResource(section.titleRes()),
          selected = section in settings.enabledHomeSections,
          modifier = Modifier
            .testTag("settings_home_section_${section.key}")
            .then(if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier)
            .settingsSectionChipBoundaryKeys(
            index = index,
            columns = columns,
            onMoveLeftToSettings = onMoveLeftToSettings,
          ),
          onClick = {
            onHomeSectionEnabledChange(section, section !in settings.enabledHomeSections)
          },
        )
      }
    }
  }
  }
}

@Composable
internal fun SettingsAboutColumn(
  scrollState: ScrollState,
  modifier: Modifier = Modifier,
) {
  val ui = LocalUiMetrics.current
  val homeColors = LocalHomeColors.current
  val performancePolicy = LocalBiliPerformancePolicy.current
  val panelShape = RoundedCornerShape(ui.dp(BiliRadius.Panel))
  val projectUrl = SettingsAboutProjectUrl
  val qrBitmap = remember(projectUrl) {
    createQrCodeBitmap(projectUrl, SettingsAboutQrSizePx)
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .biliLiquidGlassSurface(
        enabled = performancePolicy.cinematicVisualEffectsEnabled &&
          performancePolicy.liquidGlassCardsEnabled,
        shape = panelShape,
        surfaceColor = homeColors.cardSurface,
        borderColor = homeColors.glassBorder,
        borderWidth = ui.dp(BiliFocus.RestingBorderWidth),
      ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .testTag("settings_about_scroll")
        .focusProperties { canFocus = false }
        .verticalScroll(scrollState)
        .padding(ui.dp(BiliSpacing.Lg)),
      verticalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Lg)),
    ) {
      Text(
        text = stringResource(R.string.settings_about_title),
        color = homeColors.textSecondary,
        fontSize = ui.sp(BiliTypography.SectionTitle),
        fontWeight = FontWeight.Bold,
      )
      BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val availableWidth = maxWidth
        if (maxWidth >= ui.dp(BiliSizing.LoginQrImageSize) * 2) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Xl)),
        verticalAlignment = Alignment.Top,
      ) {
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Md)),
        ) {
          Text(
            text = stringResource(R.string.settings_about_project_name),
            color = homeColors.textPrimary,
            fontSize = ui.sp(BiliTypography.AboutProjectTitle),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          AboutTextBlock(
            title = stringResource(R.string.settings_about_project_intro_title),
            body = stringResource(R.string.settings_about_project_intro),
          )
        }
        Box(
          modifier = Modifier
            .width(ui.dp(BiliSizing.LoginQrImageSize)),
          contentAlignment = Alignment.TopStart,
        ) {
          Box(
            modifier = Modifier
              .size(ui.dp(BiliSizing.LoginQrImageSize) - ui.dp(BiliSpacing.Md))
              .clip(RoundedCornerShape(ui.dp(BiliRadius.Card)))
              .background(Color.White)
              .padding(ui.dp(BiliSpacing.Sm)),
            contentAlignment = Alignment.Center,
          ) {
            Image(
              bitmap = qrBitmap.asImageBitmap(),
              contentDescription = stringResource(R.string.settings_about_project_url_qr_content_description),
              modifier = Modifier.fillMaxSize(),
            )
          }
        }
      }
        } else {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Xl)),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Md)),
        ) {
          Text(
            text = stringResource(R.string.settings_about_project_name),
            color = homeColors.textPrimary,
            fontSize = ui.sp(BiliTypography.AboutProjectTitle),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          AboutTextBlock(
            title = stringResource(R.string.settings_about_project_intro_title),
            body = stringResource(R.string.settings_about_project_intro),
          )
        }
        Box(
          modifier = Modifier
            .width(ui.dp(BiliSizing.LoginQrImageSize).coerceAtMost(availableWidth)),
          contentAlignment = Alignment.TopStart,
        ) {
          Box(
            modifier = Modifier
              .width((ui.dp(BiliSizing.LoginQrImageSize) - ui.dp(BiliSpacing.Md)).coerceAtMost(availableWidth))
              .aspectRatio(1f)
              .clip(RoundedCornerShape(ui.dp(BiliRadius.Card)))
              .background(Color.White)
              .padding(ui.dp(BiliSpacing.Sm)),
            contentAlignment = Alignment.Center,
          ) {
            Image(
              bitmap = qrBitmap.asImageBitmap(),
              contentDescription = stringResource(R.string.settings_about_project_url_qr_content_description),
              modifier = Modifier.fillMaxSize(),
            )
          }
        }
      }
        }
      }
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Xl)),
      ) {
        AboutTextBlock(
          title = stringResource(R.string.settings_about_project_url_title),
          body = projectUrl,
          bodyMaxLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
        AboutTextBlock(
          title = stringResource(R.string.settings_about_license_title),
          body = stringResource(R.string.settings_about_license_value),
          bodyMaxLines = 1,
          modifier = Modifier.fillMaxWidth(),
        )
      }
      Text(
        text = stringResource(R.string.settings_about_libraries_title),
        color = homeColors.textPrimary,
        fontSize = ui.sp(BiliTypography.Body),
        fontWeight = FontWeight.Bold,
      )
      BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val count = adaptiveGridColumns(maxWidth.value, ui.dp(BiliSizing.AboutLibraryPreferredMinWidth).value,
          ui.dp(BiliSpacing.Xl).value, SettingsAboutLibraryColumnCount)
        val libraryColumns = SettingsAboutLibraries.chunked((SettingsAboutLibraries.size + count - 1) / count)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Xl))) {
        libraryColumns.forEach { libraries ->
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Sm)),
          ) {
            libraries.forEach { library ->
              AboutLibraryLine(library = library)
            }
          }
        }
        }
      }
    }
  }
}

@Composable
private fun AboutTextBlock(
  title: String,
  body: String,
  bodyMaxLines: Int = Int.MAX_VALUE,
  modifier: Modifier = Modifier,
) {
  val ui = LocalUiMetrics.current
  val homeColors = LocalHomeColors.current
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Xs)),
  ) {
    Text(
      text = title,
      color = homeColors.textPrimary,
      fontSize = ui.sp(BiliTypography.Body),
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = body,
      color = homeColors.textSecondary,
      fontSize = ui.sp(BiliTypography.BodySmall),
      maxLines = bodyMaxLines,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun AboutLibraryLine(
  library: SettingsAboutLibrary,
  modifier: Modifier = Modifier,
) {
  val ui = LocalUiMetrics.current
  val homeColors = LocalHomeColors.current
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Xs)),
  ) {
    Text(
      text = library.name,
      color = homeColors.textPrimary,
      fontSize = ui.sp(BiliTypography.BodySmall),
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = stringResource(library.descriptionRes),
      color = homeColors.textSecondary,
      fontSize = ui.sp(BiliTypography.CardMeta),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = library.url,
      color = homeColors.textTertiary,
      fontSize = ui.sp(BiliTypography.CardBadge),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun HomeSectionChip(
  title: String,
  selected: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  val ui = LocalUiMetrics.current
  val homeColors = LocalHomeColors.current
  val chipShape = RoundedCornerShape(ui.dp(BiliRadius.Pill))
  BiliFocusableSurface(
    scaleOnFocus = false,
    shadowOnFocus = false,
    shape = chipShape,
    onClick = onClick,
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = ui.dp(BiliSizing.SettingsChipHeight).coerceAtLeast(BiliSizing.MinimumTouchTarget)),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = ui.dp(BiliSizing.SettingsChipHeight).coerceAtLeast(BiliSizing.MinimumTouchTarget))
        .clip(chipShape)
        .background(
          color = if (selected) {
            homeColors.accent.copy(alpha = BiliFocus.SettingsChipSelectedBackgroundAlpha)
          } else {
            Color.Transparent
          },
          shape = chipShape,
        )
        .padding(horizontal = ui.dp(BiliSpacing.Sm), vertical = ui.dp(BiliSpacing.Sm)),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = title,
        color = if (selected) homeColors.textPrimary else homeColors.textTertiary,
        fontSize = ui.sp(BiliTypography.BodySmall),
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      )
    }
  }
}

private fun Modifier.settingsSectionChipBoundaryKeys(
  index: Int,
  columns: Int,
  onMoveLeftToSettings: () -> Boolean,
): Modifier {
  return onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) {
      return@onPreviewKeyEvent false
    }
    when (event.key) {
      Key.DirectionUp -> index < columns
      Key.DirectionLeft -> if (index % columns == 0) onMoveLeftToSettings() else false
      else -> false
    }
  }
}

private data class SettingsAboutLibrary(
  val name: String,
  val descriptionRes: Int,
  val url: String,
)

private const val SettingsAboutProjectUrl = "https://github.com/Hyper-Beast/BiliTVNative"
private const val SettingsAboutQrSizePx = 320
private const val SettingsAboutLibraryColumnCount = 2

private val SettingsAboutLibraries = listOf(
  SettingsAboutLibrary("OkHttp", R.string.settings_about_library_okhttp, "https://square.github.io/okhttp/"),
  SettingsAboutLibrary("Coil", R.string.settings_about_library_coil, "https://coil-kt.github.io/coil/"),
  SettingsAboutLibrary(
    "DanmakuRenderEngine",
    R.string.settings_about_library_danmaku_render_engine,
    "https://github.com/bytedance/DanmakuRenderEngine",
  ),
  SettingsAboutLibrary("OpenCC4J", R.string.settings_about_library_opencc4j, "https://github.com/houbb/opencc4j"),
  SettingsAboutLibrary(
    "AndroidLiquidGlass / Backdrop",
    R.string.settings_about_library_liquid_glass,
    "https://github.com/Kyant0/AndroidLiquidGlass",
  ),
  SettingsAboutLibrary("ZXing", R.string.settings_about_library_zxing, "https://github.com/zxing/zxing"),
)
