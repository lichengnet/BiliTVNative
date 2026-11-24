package com.kirin.bilitv.ui.search

import com.kirin.bilitv.ui.theme.LocalUiMetrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kirin.bilitv.R
import com.kirin.bilitv.core.model.VideoSummary
import com.kirin.bilitv.ui.common.AdaptiveTwoPaneLayout
import com.kirin.bilitv.ui.home.adaptiveGridColumns
import com.kirin.bilitv.ui.common.FeedStatusScreen
import com.kirin.bilitv.ui.common.VideoGridSkeleton
import com.kirin.bilitv.ui.focus.BiliFocusableSurface
import com.kirin.bilitv.ui.home.AdaptiveVideoGrid
import com.kirin.bilitv.ui.i18n.convertChineseText
import com.kirin.bilitv.ui.input.InteractionMode
import com.kirin.bilitv.ui.input.LocalInteractionMode
import com.kirin.bilitv.ui.settings.LocalBiliPerformancePolicy
import com.kirin.bilitv.ui.theme.BiliColors
import com.kirin.bilitv.ui.theme.BiliFocus
import com.kirin.bilitv.ui.theme.BiliMotion
import com.kirin.bilitv.ui.theme.BiliRadius
import com.kirin.bilitv.ui.theme.BiliSizing
import com.kirin.bilitv.ui.theme.BiliSpacing
import com.kirin.bilitv.ui.theme.BiliTypography
import com.kirin.bilitv.ui.theme.LocalHomeColors

@Stable
internal class SearchFocusState {
  var focusFirstResult by mutableStateOf(true)
  var focusedResultIndex by mutableIntStateOf(0)
  var focusedResultKey by mutableStateOf("")

  fun clear() {
    focusFirstResult = true
    focusedResultIndex = 0
    focusedResultKey = ""
  }

  fun resetForSortChange() {
    focusFirstResult = false
    focusedResultIndex = 0
    focusedResultKey = ""
  }
}

@Composable
internal fun SearchScreen(
  viewModel: SearchViewModel,
  focusState: SearchFocusState,
  firstItemFocusRequester: FocusRequester,
  restoreFocusRequestKey: Int,
  onRestoreFocusHandled: (Int) -> Unit,
  onMoveLeftToNav: () -> Boolean,
  onVideoSelected: (VideoSummary) -> Unit,
) {
  val viewState by viewModel.viewState.collectAsStateWithLifecycle()
  val interactionMode = LocalInteractionMode.current
  if (interactionMode == InteractionMode.Touch) {
    SearchTouchScreen(
      viewModel = viewModel,
      viewState = viewState,
      focusState = focusState,
      restoreFocusRequestKey = restoreFocusRequestKey,
      onRestoreFocusHandled = onRestoreFocusHandled,
      onVideoSelected = onVideoSelected,
    )
    return
  }

  var returnFocusToKeyboard by remember { mutableStateOf(false) }
  val screenFocusRequester = remember { FocusRequester() }

  LaunchedEffect(viewState.activeQuery, returnFocusToKeyboard) {
    if (viewState.activeQuery == null && returnFocusToKeyboard) {
      withFrameNanos { }
      runCatching {
        firstItemFocusRequester.requestFocus()
      }
      returnFocusToKeyboard = false
    }
  }

  val query = viewState.activeQuery
  Box(
    modifier = Modifier
      .fillMaxSize()
      .focusRequester(screenFocusRequester)
      .focusable(),
  ) {
    if (query == null) {
      SearchKeyboardView(
        searchText = viewState.searchText,
        suggestions = viewState.suggestions,
        searchHistory = viewState.searchHistory,
        keyboardFocusRequester = firstItemFocusRequester,
        onMoveLeftToNav = onMoveLeftToNav,
        onTextChange = { nextText ->
          viewModel.updateSearchText(nextText)
        },
        onClearSearchHistory = {
          runCatching {
            firstItemFocusRequester.requestFocus()
          }
          viewModel.clearSearchHistory()
        },
        onSearch = { text ->
          val trimmed = text.trim()
          if (trimmed.isNotEmpty()) {
            runCatching {
              screenFocusRequester.requestFocus()
            }
            focusState.clear()
            viewModel.startSearch(trimmed)
          }
        },
      )
    } else {
      SearchResultsView(
        query = query,
        viewModel = viewModel,
        viewState = viewState,
        focusState = focusState,
        firstResultFocusRequester = firstItemFocusRequester,
        restoreFocusRequestKey = restoreFocusRequestKey,
        onRestoreFocusHandled = onRestoreFocusHandled,
        onMoveLeftToNav = onMoveLeftToNav,
        onBackToKeyboard = {
          viewModel.backToKeyboard()
          returnFocusToKeyboard = true
        },
        onVideoSelected = onVideoSelected,
      )
    }
  }
}

@Composable
private fun SearchTouchScreen(
  viewModel: SearchViewModel,
  viewState: SearchViewState,
  focusState: SearchFocusState,
  restoreFocusRequestKey: Int,
  onRestoreFocusHandled: (Int) -> Unit,
  onVideoSelected: (VideoSummary) -> Unit,
) {
  val ui = LocalUiMetrics.current
  val inputFocusRequester = remember { FocusRequester() }
  val focusManager = LocalFocusManager.current

  fun submitSearch(text: String) {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return
    focusState.clear()
    viewModel.startSearch(trimmed)
    focusManager.clearFocus()
  }

  LaunchedEffect(viewState.activeQuery, restoreFocusRequestKey) {
    if (viewState.activeQuery != null) return@LaunchedEffect
    withFrameNanos { }
    runCatching { inputFocusRequester.requestFocus() }
    if (restoreFocusRequestKey > 0) onRestoreFocusHandled(restoreFocusRequestKey)
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(
        start = ui.dp(BiliSizing.ContentPadding),
        top = ui.dp(BiliSizing.SearchTouchTopPadding),
        end = ui.dp(BiliSizing.ContentPadding),
        bottom = ui.dp(BiliSizing.ContentPadding),
      ),
  ) {
    SearchTouchInputBar(
      searchText = viewState.searchText,
      inputFocusRequester = inputFocusRequester,
      onTextChange = viewModel::updateSearchText,
      onClear = {
        viewModel.clear()
        runCatching {
          inputFocusRequester.requestFocus()
        }
      },
      onSearch = ::submitSearch,
    )
    Spacer(modifier = Modifier.height(ui.dp(BiliSizing.SearchTouchContentTopPadding)))
    val query = viewState.activeQuery
    if (query == null) {
      SearchTouchSuggestionContent(
        searchText = viewState.searchText,
        suggestions = viewState.suggestions,
        searchHistory = viewState.searchHistory,
        onSuggestionSelected = ::submitSearch,
        onClearSearchHistory = viewModel::clearSearchHistory,
        modifier = Modifier.weight(1f),
      )
    } else {
      SearchTouchResultsView(
        query = query,
        viewModel = viewModel,
        viewState = viewState,
        focusState = focusState,
        restoreFocusRequestKey = restoreFocusRequestKey,
        onRestoreFocusHandled = onRestoreFocusHandled,
        onBackToSuggestions = viewModel::backToKeyboard,
        onVideoSelected = onVideoSelected,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
internal fun SearchTouchInputBar(
  searchText: String,
  inputFocusRequester: FocusRequester,
  onTextChange: (String) -> Unit,
  onClear: () -> Unit,
  onSearch: (String) -> Unit,
) {
  val ui = LocalUiMetrics.current
  val homeColors = LocalHomeColors.current
  val shape = RoundedCornerShape(ui.dp(BiliRadius.Card))
  SearchTouchInputLayout(modifier = Modifier.fillMaxWidth().testTag("search_touch_input_bar")) {
    BasicTextField(
      value = searchText,
      onValueChange = onTextChange,
      modifier = Modifier
        .testTag("search_touch_input")
        .fillMaxWidth()
        .heightIn(min = ui.dp(BiliSizing.SearchTouchInputHeight).coerceAtLeast(BiliSizing.MinimumTouchTarget))
        .focusRequester(inputFocusRequester)
        .clip(shape)
        .background(homeColors.glassSurfaceStrong, shape)
        .border(BorderStroke(ui.dp(BiliFocus.RestingBorderWidth), homeColors.glassBorder), shape)
        .padding(horizontal = ui.dp(BiliSpacing.Lg), vertical = ui.dp(BiliSpacing.Sm)),
      singleLine = true,
      textStyle = TextStyle(
        color = homeColors.textPrimary,
        fontSize = ui.sp(BiliTypography.SearchInput),
        fontWeight = FontWeight.Bold,
      ),
      cursorBrush = SolidColor(homeColors.accent),
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
      keyboardActions = KeyboardActions(
        onSearch = {
          onSearch(searchText)
        },
      ),
      decorationBox = { innerTextField ->
        Box(
          modifier = Modifier.fillMaxWidth(),
          contentAlignment = Alignment.CenterStart,
        ) {
          if (searchText.isBlank()) {
            Text(
              text = stringResource(R.string.search_input_placeholder),
              color = homeColors.textTertiary,
              fontSize = ui.sp(BiliTypography.SearchInput),
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          innerTextField()
        }
      },
    )
    if (searchText.isNotBlank()) {
      SearchTouchActionButton(
        label = stringResource(R.string.search_action_clear),
        modifier = Modifier.testTag("search_touch_clear"),
        onClick = onClear,
      )
    }
    SearchTouchActionButton(
      label = stringResource(R.string.search_action_search),
      modifier = Modifier.testTag("search_touch_search"),
      action = true,
      onClick = {
        onSearch(searchText)
      },
    )
  }
}

@Composable
private fun SearchTouchActionButton(
  label: String,
  action: Boolean = false,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  val ui = LocalUiMetrics.current
  val homeColors = LocalHomeColors.current
  val shape = RoundedCornerShape(ui.dp(BiliRadius.Card))
  Box(
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = ui.dp(BiliSizing.SearchTouchInputHeight).coerceAtLeast(BiliSizing.MinimumTouchTarget))
      .clip(shape)
      .background(
        color = if (action) homeColors.accent else homeColors.glassSurfaceStrong,
        shape = shape,
      )
      .border(
        BorderStroke(
          ui.dp(BiliFocus.RestingBorderWidth),
          if (action) BiliColors.Transparent else homeColors.glassBorder,
        ),
        shape,
      )
      .clickable(onClick = onClick)
      .padding(horizontal = ui.dp(BiliSpacing.Sm), vertical = ui.dp(BiliSpacing.Sm)),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      color = if (action) BiliColors.TextPrimary else homeColors.textSecondary,
      fontSize = ui.sp(BiliTypography.BodySmall),
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun SearchTouchSuggestionContent(
  searchText: String,
  suggestions: List<String>,
  searchHistory: List<String>,
  onSuggestionSelected: (String) -> Unit,
  onClearSearchHistory: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val ui = LocalUiMetrics.current
  Box(modifier = modifier.fillMaxSize()) {
    when {
      searchText.isBlank() && searchHistory.isEmpty() -> SearchHintText(
        text = stringResource(R.string.search_empty_prompt),
      )
      searchText.isBlank() -> SearchHistoryList(
        history = searchHistory,
        onHistorySelected = onSuggestionSelected,
        onClearSearchHistory = onClearSearchHistory,
      )
      suggestions.isEmpty() -> SearchHintText(
        text = stringResource(R.string.search_no_suggestions),
      )
      else -> {
        val homeColors = LocalHomeColors.current
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(vertical = ui.dp(BiliSpacing.Md)),
          verticalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Sm)),
        ) {
          item {
            Text(
              text = stringResource(R.string.search_suggestions_title),
              color = homeColors.textSecondary,
              fontSize = ui.sp(BiliTypography.SectionTitle),
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(bottom = ui.dp(BiliSpacing.Sm)),
            )
          }
          items(suggestions, key = { suggestion -> suggestion }) { suggestion ->
            SearchSuggestionItem(
              text = suggestion,
              displayText = convertChineseText(suggestion),
              onClick = {
                onSuggestionSelected(suggestion)
              },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SearchTouchResultsView(
  query: String,
  viewModel: SearchViewModel,
  viewState: SearchViewState,
  focusState: SearchFocusState,
  restoreFocusRequestKey: Int,
  onRestoreFocusHandled: (Int) -> Unit,
  onBackToSuggestions: () -> Unit,
  onVideoSelected: (VideoSummary) -> Unit,
  modifier: Modifier = Modifier,
) {
  val ui = LocalUiMetrics.current
  val firstResultFocusRequester = remember { FocusRequester() }
  val selectedSortFocusRequester = remember { FocusRequester() }
  Column(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Lg)),
  ) {
    SearchTouchResultsHeader(
      query = query,
      selectedOrderKey = viewState.selectedOrderKey,
      onOrderSelected = { orderKey ->
        if (orderKey != viewState.selectedOrderKey) {
          focusState.resetForSortChange()
        }
        viewModel.selectOrder(orderKey)
      },
    )
    Box(modifier = Modifier.fillMaxSize()) {
      when (val currentState = viewState.resultState) {
        SearchResultState.Loading -> VideoGridSkeleton(horizontalPadding = ui.dp(BiliSizing.SearchVideoGridHorizontalPadding))
        SearchResultState.Empty -> FeedStatusScreen(message = stringResource(R.string.search_empty))
        is SearchResultState.Failed -> FeedStatusScreen(
          message = stringResource(R.string.search_failed_with_message, currentState.message),
          actionLabel = stringResource(R.string.action_retry),
          onAction = {
            viewModel.retry()
          },
        )
        is SearchResultState.Success -> SearchResultGrid(
          videos = currentState.videos,
          firstResultFocusRequester = firstResultFocusRequester,
          selectedSortFocusRequester = selectedSortFocusRequester,
          restoredFocusIndex = currentState.videos.resolveFocusIndex(
            focusKey = focusState.focusedResultKey,
            fallbackIndex = focusState.focusedResultIndex,
          ),
          restoreFocusRequestKey = restoreFocusRequestKey,
          onRestoreFocusHandled = onRestoreFocusHandled,
          focusFirstResult = false,
          onFirstResultFocused = {},
          onFocusedIndexChange = { index, video ->
            focusState.focusedResultIndex = index
            focusState.focusedResultKey = video.focusRestoreKey()
          },
          onLoadMore = viewModel::loadNextPage,
          onRefresh = {
            focusState.clear()
            viewModel.retry()
          },
          onMoveLeftToNav = { false },
          onBackToKeyboard = onBackToSuggestions,
          onVideoSelected = onVideoSelected,
        )
      }
    }
  }
}

@Composable
private fun SearchTouchResultsHeader(
  query: String,
  selectedOrderKey: String,
  onOrderSelected: (String) -> Unit,
) {
  val ui = LocalUiMetrics.current
  val homeColors = LocalHomeColors.current
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Md)),
  ) {
    Text(
      text = stringResource(R.string.search_results_title, convertChineseText(query)),
      color = homeColors.textPrimary,
      fontSize = ui.sp(BiliTypography.SectionTitle),
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    LazyRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Md)),
      contentPadding = PaddingValues(horizontal = ui.dp(BiliSpacing.Xs)),
    ) {
      items(SearchSortOptions, key = { option -> option.key }) { option ->
        SearchTouchSortButton(
          option = option,
          selected = selectedOrderKey == option.key,
          onSelected = {
            onOrderSelected(option.key)
          },
        )
      }
    }
  }
}

@Composable
private fun SearchTouchSortButton(
  option: SearchSortOption,
  selected: Boolean,
  onSelected: () -> Unit,
) {
  val ui = LocalUiMetrics.current
  val homeColors = LocalHomeColors.current
  val shape = RoundedCornerShape(ui.dp(BiliRadius.Pill))
  Box(
    modifier = Modifier
      .heightIn(min = ui.dp(BiliSizing.HomeSectionTabHeight).coerceAtLeast(BiliSizing.MinimumTouchTarget))
      .widthIn(min = ui.dp(BiliSizing.HomeSectionTabMinWidth))
      .clip(shape)
      .background(
        color = if (selected) homeColors.accent else homeColors.glassSurfaceStrong,
        shape = shape,
      )
      .border(
        BorderStroke(
          ui.dp(BiliFocus.RestingBorderWidth),
          if (selected) BiliColors.Transparent else homeColors.glassBorder,
        ),
        shape,
      )
      .clickable(onClick = onSelected)
      .padding(horizontal = ui.dp(BiliSpacing.Lg)),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = stringResource(option.titleRes),
      color = if (selected) BiliColors.TextPrimary else homeColors.textSecondary,
      fontSize = ui.sp(BiliTypography.HomeSectionTab),
      lineHeight = ui.sp(BiliTypography.HomeSectionTabLineHeight),
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      maxLines = 1,
      style = TextStyle(
        platformStyle = PlatformTextStyle(includeFontPadding = false),
      ),
    )
  }
}

@Composable
internal fun SearchKeyboardView(
  searchText: String,
  suggestions: List<String>,
  searchHistory: List<String>,
  keyboardFocusRequester: FocusRequester,
  onMoveLeftToNav: () -> Boolean,
  onTextChange: (String) -> Unit,
  onClearSearchHistory: () -> Unit,
  onSearch: (String) -> Unit,
) {
  val ui = LocalUiMetrics.current
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(ui.dp(BiliSizing.ContentPadding)),
  ) {
    AdaptiveTwoPaneLayout(
      minimumPrimaryWidth = ui.dp(BiliSizing.SearchKeyboardPanelWidth),
      minimumSecondaryWidth = ui.dp(BiliSizing.VideoCardPreferredMinWidth),
      preferredPrimaryWidth = ui.dp(BiliSizing.SearchKeyboardPanelWidth),
      spacing = ui.dp(BiliSpacing.Xl),
      modifier = Modifier.fillMaxSize(),
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
      ) {
        SearchInputText(searchText = searchText)
        Spacer(modifier = Modifier.height(ui.dp(BiliSpacing.Md)))
        Row(
          horizontalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Md)),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ui.dp(BiliSizing.SearchKeyboardButtonHeight).coerceAtLeast(BiliSizing.MinimumTouchTarget)),
        ) {
          SearchKeyboardButton(
            label = stringResource(R.string.search_action_clear),
            modifier = Modifier
              .weight(1f)
              .focusRequester(keyboardFocusRequester),
            onMoveLeft = onMoveLeftToNav,
            onClick = {
              onTextChange("")
            },
          )
          SearchKeyboardButton(
            label = stringResource(R.string.search_action_backspace),
            modifier = Modifier.weight(1f),
            onClick = {
              if (searchText.isNotEmpty()) {
                onTextChange(searchText.dropLast(1))
              }
            },
          )
        }
        Spacer(modifier = Modifier.height(ui.dp(BiliSpacing.Md)))
        SearchKeyGrid(
          onKeyClick = { key ->
            onTextChange(searchText + key)
          },
          onMoveLeftToNav = onMoveLeftToNav,
        )
        Spacer(modifier = Modifier.height(ui.dp(BiliSpacing.Lg)))
        SearchKeyboardButton(
          label = stringResource(R.string.search_action_search),
          action = true,
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ui.dp(BiliSizing.SearchKeyboardButtonHeight).coerceAtLeast(BiliSizing.MinimumTouchTarget)),
          onMoveLeft = onMoveLeftToNav,
          onClick = {
            onSearch(searchText)
          },
        )
      }
      SearchSuggestionPanel(
        searchText = searchText,
        suggestions = suggestions,
        searchHistory = searchHistory,
        onSuggestionSelected = { suggestion ->
          onSearch(suggestion)
        },
        onClearSearchHistory = onClearSearchHistory,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}

@Composable
private fun SearchInputText(searchText: String) {
  val ui = LocalUiMetrics.current
  val homeColors = LocalHomeColors.current
  val placeholder = stringResource(R.string.search_input_placeholder)
  val displayText = if (searchText.isBlank()) placeholder else convertChineseText(searchText)

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = ui.dp(BiliSizing.SearchInputHeight).coerceAtLeast(BiliSizing.MinimumTouchTarget))
      .background(homeColors.glassSurfaceStrong, RoundedCornerShape(ui.dp(BiliRadius.Card)))
      .padding(horizontal = ui.dp(BiliSpacing.Lg)),
    contentAlignment = Alignment.CenterStart,
  ) {
    Text(
      text = displayText,
      color = if (searchText.isBlank()) homeColors.textTertiary else homeColors.textPrimary,
      fontSize = ui.sp(BiliTypography.SearchInput),
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun SearchKeyGrid(
  onKeyClick: (String) -> Unit,
  onMoveLeftToNav: () -> Boolean,
) {
  val ui = LocalUiMetrics.current
  BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
  val columns = adaptiveGridColumns(maxWidth.value, ui.dp(BiliSizing.MinimumTouchTarget).coerceAtLeast(BiliSizing.MinimumTouchTarget).value,
    ui.dp(BiliSpacing.Sm).value, SearchKeyboardRows.first().size)
  Column(verticalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Sm)), modifier = Modifier.fillMaxWidth()) {
    SearchKeyboardRows.flatten().chunked(columns).forEach { row ->
      Row(horizontalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Sm)), modifier = Modifier.fillMaxWidth()) {
        row.forEachIndexed { columnIndex, key ->
          SearchKeyboardButton(label = key,
            modifier = Modifier.weight(1f).heightIn(min = ui.dp(BiliSizing.SearchKeyboardButtonHeight).coerceAtLeast(BiliSizing.MinimumTouchTarget)),
            onMoveLeft = if (columnIndex == 0) onMoveLeftToNav else null,
            onClick = { onKeyClick(key) })
        }
        repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
      }
    }
  }
  }
}

@Composable
private fun SearchKeyboardButton(
  label: String,
  modifier: Modifier = Modifier,
  action: Boolean = false,
  onMoveLeft: (() -> Boolean)? = null,
  onClick: () -> Unit,
) {
  val ui = LocalUiMetrics.current
  val homeColors = LocalHomeColors.current
  BiliFocusableSurface(
    scaleOnFocus = false,
    shape = RoundedCornerShape(ui.dp(BiliRadius.Card)),
    onClick = onClick,
    modifier = modifier
      .onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft && onMoveLeft != null) {
          onMoveLeft()
        } else {
          false
        }
      },
  ) {
    Box(
      modifier = Modifier.fillMaxWidth().heightIn(min = ui.dp(BiliSizing.SearchKeyboardButtonHeight).coerceAtLeast(BiliSizing.MinimumTouchTarget))
        .padding(vertical = ui.dp(BiliSpacing.Sm)),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = label,
        color = if (action) homeColors.accent else homeColors.textSecondary,
        fontSize = ui.sp(BiliTypography.Body),
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
private fun SearchSuggestionPanel(
  searchText: String,
  suggestions: List<String>,
  searchHistory: List<String>,
  onSuggestionSelected: (String) -> Unit,
  onClearSearchHistory: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val ui = LocalUiMetrics.current
  val homeColors = LocalHomeColors.current
  Box(
    modifier = modifier
      .fillMaxHeight()
      .padding(vertical = ui.dp(BiliSpacing.Xs)),
  ) {
    if (searchText.isBlank()) {
      if (searchHistory.isEmpty()) {
        SearchHintText(text = stringResource(R.string.search_empty_prompt))
      } else {
        SearchHistoryList(
          history = searchHistory,
          onHistorySelected = onSuggestionSelected,
          onClearSearchHistory = onClearSearchHistory,
        )
      }
    } else if (suggestions.isEmpty()) {
      SearchHintText(text = stringResource(R.string.search_no_suggestions))
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = ui.dp(BiliSpacing.Md)),
        verticalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Sm)),
      ) {
        item {
          Text(
            text = stringResource(R.string.search_suggestions_title),
            color = homeColors.textSecondary,
            fontSize = ui.sp(BiliTypography.SectionTitle),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = ui.dp(BiliSpacing.Sm)),
          )
        }
        items(suggestions, key = { suggestion -> suggestion }) { suggestion ->
          SearchSuggestionItem(
            text = suggestion,
            displayText = convertChineseText(suggestion),
            onClick = {
              onSuggestionSelected(suggestion)
            },
          )
        }
      }
    }
  }
}

@Composable
private fun SearchHistoryList(
  history: List<String>,
  onHistorySelected: (String) -> Unit,
  onClearSearchHistory: () -> Unit,
) {
  val ui = LocalUiMetrics.current
  val homeColors = LocalHomeColors.current
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(vertical = ui.dp(BiliSpacing.Md)),
    verticalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Sm)),
  ) {
    item {
      Text(
        text = stringResource(R.string.search_history_title),
        color = homeColors.textSecondary,
        fontSize = ui.sp(BiliTypography.SectionTitle),
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = ui.dp(BiliSpacing.Sm)),
      )
    }
    items(history, key = { item -> item }) { item ->
      SearchSuggestionItem(
        text = item,
        displayText = convertChineseText(item),
        onClick = {
          onHistorySelected(item)
        },
      )
    }
    item {
      SearchSuggestionItem(
        text = stringResource(R.string.search_history_clear),
        onClick = onClearSearchHistory,
      )
    }
  }
}

@Composable
private fun SearchHintText(text: String) {
  val ui = LocalUiMetrics.current
  val homeColors = LocalHomeColors.current
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = text,
      color = homeColors.textTertiary,
      fontSize = ui.sp(BiliTypography.Body),
      fontWeight = FontWeight.Medium,
    )
  }
}

@Composable
private fun SearchSuggestionItem(
  text: String,
  displayText: String = text,
  onClick: () -> Unit,
) {
  val ui = LocalUiMetrics.current
  val homeColors = LocalHomeColors.current
  BiliFocusableSurface(
    scaleOnFocus = false,
    shape = RoundedCornerShape(ui.dp(BiliRadius.Card)),
    onClick = onClick,
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = ui.dp(BiliSizing.SearchKeyboardButtonHeight).coerceAtLeast(BiliSizing.MinimumTouchTarget)),
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = ui.dp(BiliSpacing.Lg)),
      contentAlignment = Alignment.CenterStart,
    ) {
      Text(
        text = displayText,
        color = homeColors.textSecondary,
        fontSize = ui.sp(BiliTypography.Body),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun SearchResultsView(
  query: String,
  viewModel: SearchViewModel,
  viewState: SearchViewState,
  focusState: SearchFocusState,
  firstResultFocusRequester: FocusRequester,
  restoreFocusRequestKey: Int,
  onRestoreFocusHandled: (Int) -> Unit,
  onMoveLeftToNav: () -> Boolean,
  onBackToKeyboard: () -> Unit,
  onVideoSelected: (VideoSummary) -> Unit,
) {
  val ui = LocalUiMetrics.current
  val sortFocusRequesters = remember {
    SearchSortOptions.associate { option -> option.key to FocusRequester() }
  }
  val selectedOrderKey = viewState.selectedOrderKey

  Column(
    modifier = Modifier
      .fillMaxSize()
      .onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown && event.key == Key.Back) {
          onBackToKeyboard()
          true
        } else {
          false
        }
      },
  ) {
    SearchResultsHeader(
      query = query,
      selectedOrderKey = selectedOrderKey,
      sortFocusRequesters = sortFocusRequesters,
      firstResultFocusRequester = firstResultFocusRequester,
      onMoveLeftToNav = onMoveLeftToNav,
      onOrderSelected = { orderKey ->
        if (orderKey != viewState.selectedOrderKey) {
          focusState.resetForSortChange()
        }
        viewModel.selectOrder(orderKey)
      },
    )
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(top = ui.dp(BiliSpacing.Lg)),
    ) {
      when (val currentState = viewState.resultState) {
        SearchResultState.Loading -> VideoGridSkeleton(horizontalPadding = ui.dp(BiliSizing.SearchVideoGridHorizontalPadding))
        SearchResultState.Empty -> FeedStatusScreen(message = stringResource(R.string.search_empty))
        is SearchResultState.Failed -> FeedStatusScreen(
          message = stringResource(R.string.search_failed_with_message, currentState.message),
          actionLabel = stringResource(R.string.action_retry),
          onAction = {
            viewModel.retry()
          },
        )
        is SearchResultState.Success -> SearchResultGrid(
          videos = currentState.videos,
          firstResultFocusRequester = firstResultFocusRequester,
          selectedSortFocusRequester = sortFocusRequesters.getValue(selectedOrderKey),
          restoredFocusIndex = currentState.videos.resolveFocusIndex(
            focusKey = focusState.focusedResultKey,
            fallbackIndex = focusState.focusedResultIndex,
          ),
          restoreFocusRequestKey = restoreFocusRequestKey,
          onRestoreFocusHandled = onRestoreFocusHandled,
          focusFirstResult = focusState.focusFirstResult,
          onFirstResultFocused = {
            focusState.focusFirstResult = false
          },
          onFocusedIndexChange = { index, video ->
            focusState.focusedResultIndex = index
            focusState.focusedResultKey = video.focusRestoreKey()
          },
          onLoadMore = viewModel::loadNextPage,
          onMoveLeftToNav = onMoveLeftToNav,
          onBackToKeyboard = onBackToKeyboard,
          onVideoSelected = onVideoSelected,
        )
      }
    }
  }
}

@Composable
private fun SearchResultsHeader(
  query: String,
  selectedOrderKey: String,
  sortFocusRequesters: Map<String, FocusRequester>,
  firstResultFocusRequester: FocusRequester,
  onMoveLeftToNav: () -> Boolean,
  onOrderSelected: (String) -> Unit,
) {
  val ui = LocalUiMetrics.current
  val homeColors = LocalHomeColors.current
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Md)),
  ) {
    Text(
      text = stringResource(R.string.search_results_title, convertChineseText(query)),
      color = homeColors.textPrimary,
      fontSize = ui.sp(BiliTypography.SectionTitle),
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = ui.dp(BiliSizing.SearchVideoGridHorizontalPadding)),
    )
    LazyRow(
      modifier = Modifier
        .padding(horizontal = ui.dp(BiliSizing.SearchVideoGridHorizontalPadding))
        .fillMaxWidth()
        .heightIn(min = ui.dp(BiliSizing.HomeSectionTabHeight).coerceAtLeast(BiliSizing.MinimumTouchTarget) + ui.dp(BiliSpacing.Xs))
        .padding(ui.dp(BiliSpacing.Xs)),
      horizontalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Lg)),
      contentPadding = PaddingValues(horizontal = ui.dp(BiliSpacing.Xs)),
    ) {
      itemsIndexed(SearchSortOptions, key = { _, option -> option.key }) { index, option ->
        val selected = selectedOrderKey == option.key
        SearchSortButton(
          option = option,
          selected = selected,
          modifier = Modifier.focusRequester(sortFocusRequesters.getValue(option.key)),
          onMoveLeftToNav = if (index == 0) onMoveLeftToNav else null,
          onMoveDownToResults = {
            runCatching {
              firstResultFocusRequester.requestFocus()
            }.isSuccess
          },
          onSelected = {
            onOrderSelected(option.key)
          },
        )
      }
    }
  }
}

@Composable
private fun SearchSortButton(
  option: SearchSortOption,
  selected: Boolean,
  modifier: Modifier = Modifier,
  onMoveLeftToNav: (() -> Boolean)? = null,
  onMoveDownToResults: () -> Boolean,
  onSelected: () -> Unit,
) {
  val ui = LocalUiMetrics.current
  var focused by remember { mutableStateOf(false) }
  val performancePolicy = LocalBiliPerformancePolicy.current
  val homeColors = LocalHomeColors.current
  val shape = RoundedCornerShape(ui.dp(BiliRadius.Pill))
  val targetBorderColor = if (focused) homeColors.accent else BiliColors.Transparent
  val targetTextColor = when {
    selected -> homeColors.accent
    focused -> homeColors.textPrimary
    else -> homeColors.textSecondary
  }
  val borderWidth = if (performancePolicy.motionEnabled) {
    animateDpAsState(
      targetValue = if (focused) ui.dp(BiliFocus.BorderWidth) else ui.dp(BiliFocus.RestingBorderWidth),
      animationSpec = androidx.compose.animation.core.tween(BiliMotion.FocusMs, easing = BiliMotion.FocusEasing),
      label = "searchSortBorderWidth",
    ).value
  } else {
    if (focused) ui.dp(BiliFocus.BorderWidth) else ui.dp(BiliFocus.RestingBorderWidth)
  }
  val borderColor = if (performancePolicy.motionEnabled) {
    animateColorAsState(
      targetValue = targetBorderColor,
      animationSpec = androidx.compose.animation.core.tween(BiliMotion.FocusMs, easing = BiliMotion.FocusEasing),
      label = "searchSortBorder",
    ).value
  } else {
    targetBorderColor
  }
  val textColor = if (performancePolicy.motionEnabled) {
    animateColorAsState(
      targetValue = targetTextColor,
      animationSpec = androidx.compose.animation.core.tween(BiliMotion.FocusMs, easing = BiliMotion.FocusEasing),
      label = "searchSortText",
    ).value
  } else {
    targetTextColor
  }
  val interactionSource = remember { MutableInteractionSource() }

  Box(
    modifier = modifier
      .heightIn(min = ui.dp(BiliSizing.HomeSectionTabHeight).coerceAtLeast(BiliSizing.MinimumTouchTarget))
      .widthIn(min = ui.dp(BiliSizing.HomeSectionTabCompactMinWidth))
      .clip(shape)
      .border(BorderStroke(borderWidth, borderColor), shape)
      .onFocusChanged { focusState ->
        focused = focusState.isFocused
        if (focusState.isFocused && !selected) {
          onSelected()
        }
      }
      .onPreviewKeyEvent { event ->
        when {
          event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft ->
            if (onMoveLeftToNav != null) onMoveLeftToNav() else false
          event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown -> onMoveDownToResults()
          event.type == KeyEventType.KeyUp && event.key.isConfirmKey() -> {
            onSelected()
            true
          }
          else -> false
        }
      }
      .focusable(interactionSource = interactionSource)
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onSelected,
      )
      .padding(horizontal = ui.dp(BiliSpacing.Sm)),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = stringResource(option.titleRes),
      color = textColor,
      fontSize = ui.sp(BiliTypography.HomeSectionTab),
      lineHeight = ui.sp(BiliTypography.HomeSectionTabLineHeight),
      fontWeight = if (selected || focused) FontWeight.Bold else FontWeight.Medium,
      textAlign = TextAlign.Center,
      maxLines = 1,
      style = TextStyle(
        platformStyle = PlatformTextStyle(includeFontPadding = false),
      ),
    )
  }
}

@Composable
private fun SearchResultGrid(
  videos: List<VideoSummary>,
  firstResultFocusRequester: FocusRequester,
  selectedSortFocusRequester: FocusRequester,
  restoredFocusIndex: Int,
  restoreFocusRequestKey: Int,
  onRestoreFocusHandled: (Int) -> Unit,
  focusFirstResult: Boolean,
  onFirstResultFocused: () -> Unit,
  onFocusedIndexChange: (Int, VideoSummary) -> Unit,
  onLoadMore: () -> Unit,
  onRefresh: (() -> Unit)? = null,
  onMoveLeftToNav: () -> Boolean,
  onBackToKeyboard: () -> Unit,
  onVideoSelected: (VideoSummary) -> Unit,
) {
  val ui = LocalUiMetrics.current
  LaunchedEffect(videos, focusFirstResult) {
    if (videos.isNotEmpty() && focusFirstResult) {
      withFrameNanos { }
      runCatching {
        firstResultFocusRequester.requestFocus()
      }
      onFirstResultFocused()
    }
  }

  AdaptiveVideoGrid(
    videos = videos,
    firstItemFocusRequester = firstResultFocusRequester,
    restoredFocusIndex = restoredFocusIndex,
    restoreFocusRequestKey = restoreFocusRequestKey,
    onRestoreFocusHandled = onRestoreFocusHandled,
    onFocusedIndexChange = onFocusedIndexChange,
    onLoadMore = onLoadMore,
    onRefresh = onRefresh,
    onMoveLeftToNav = onMoveLeftToNav,
    onMoveUpFromFirstRow = {
      runCatching {
        selectedSortFocusRequester.requestFocus()
      }.isSuccess
    },
    onBackKey = {
      onBackToKeyboard()
      true
    },
    onVideoSelected = onVideoSelected,
    horizontalPadding = ui.dp(BiliSizing.SearchVideoGridHorizontalPadding),
  )
}

private fun Key.isConfirmKey(): Boolean {
  return this == Key.Enter || this == Key.NumPadEnter || this == Key.DirectionCenter
}

private fun List<VideoSummary>.resolveFocusIndex(focusKey: String, fallbackIndex: Int): Int {
  val keyIndex = focusKey
    .takeIf { key -> key.isNotBlank() }
    ?.let { key -> indexOfFirst { video -> video.focusRestoreKey() == key } }
    ?.takeIf { index -> index >= 0 }
  return keyIndex ?: fallbackIndex.coerceIn(0, lastIndex)
}

private fun VideoSummary.focusRestoreKey(): String {
  return bvid.ifBlank {
    when {
      cid > 0L -> "cid-$cid"
      historyPage > 0 -> "p-$historyPage"
      else -> ""
    }
  }
}

private val SearchKeyboardRows = listOf(
  listOf("A", "B", "C", "D", "E", "F"),
  listOf("G", "H", "I", "J", "K", "L"),
  listOf("M", "N", "O", "P", "Q", "R"),
  listOf("S", "T", "U", "V", "W", "X"),
  listOf("Y", "Z", "1", "2", "3", "4"),
  listOf("5", "6", "7", "8", "9", "0"),
)
