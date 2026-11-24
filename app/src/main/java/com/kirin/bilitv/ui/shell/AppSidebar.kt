package com.kirin.bilitv.ui.shell

import com.kirin.bilitv.ui.theme.LocalUiMetrics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import com.kirin.bilitv.R
import com.kirin.bilitv.core.image.BiliImageSizing
import com.kirin.bilitv.core.image.buildOwnerAvatarRequest
import com.kirin.bilitv.core.storage.UserSession
import com.kirin.bilitv.ui.focus.BiliFocusableSurface
import com.kirin.bilitv.ui.glass.LocalLiquidGlassBackdrop
import com.kirin.bilitv.ui.glass.biliLiquidGlassSurface
import com.kirin.bilitv.ui.i18n.convertChineseText
import com.kirin.bilitv.ui.settings.LocalBiliPerformancePolicy
import com.kirin.bilitv.ui.theme.BiliColors
import com.kirin.bilitv.ui.theme.BiliFocus
import com.kirin.bilitv.ui.theme.BiliRadius
import com.kirin.bilitv.ui.theme.BiliSizing
import com.kirin.bilitv.ui.theme.BiliSpacing
import com.kirin.bilitv.ui.theme.BiliTypography
import com.kirin.bilitv.ui.theme.LocalHomeColors

@Composable
internal fun AppSidebar(
  selectedDestination: AppDestination,
  accountSelected: Boolean,
  userSession: UserSession,
  autoConfirmOnFocus: Boolean,
  accountFocusRequester: FocusRequester,
  navFocusRequesters: Map<AppDestination, FocusRequester>,
  onAccountSelected: () -> Unit,
  onExitApp: () -> Unit,
  onDestinationSelected: (AppDestination) -> Unit,
  shouldAutoConfirmDestination: (AppDestination) -> Boolean,
  onMoveRight: (AppDestination) -> Boolean,
) {
  val ui = LocalUiMetrics.current
  val exitFocusRequester = remember { FocusRequester() }
  val homeColors = LocalHomeColors.current
  val performancePolicy = LocalBiliPerformancePolicy.current
  val cinematicVisualsEnabled = performancePolicy.cinematicVisualEffectsEnabled
  val liquidGlassBackdrop = LocalLiquidGlassBackdrop.current
  val liquidGlassEnabled = cinematicVisualsEnabled && performancePolicy.liquidGlassCardsEnabled && liquidGlassBackdrop != null
  val sidebarShape = RoundedCornerShape(
    topStart = ui.dp(BiliRadius.Sidebar),
    topEnd = ui.dp(BiliRadius.Sidebar),
    bottomEnd = ui.dp(BiliRadius.Sidebar),
    bottomStart = ui.dp(BiliRadius.Sidebar),
  )
  val sidebarBackground = if (cinematicVisualsEnabled) {
    Brush.horizontalGradient(
      colors = listOf(
        homeColors.sidebarSurface.copy(alpha = BiliFocus.HomeSidebarCinematicStartAlpha),
        homeColors.sidebarSurface.copy(alpha = BiliFocus.HomeSidebarCinematicMidAlpha),
        homeColors.sidebarSurface.copy(alpha = BiliFocus.HomeSidebarCinematicEndAlpha),
      ),
    )
  } else {
    Brush.horizontalGradient(
      colors = listOf(
        homeColors.sidebarSurface.copy(alpha = BiliFocus.HomeSidebarRefinedStartAlpha),
        homeColors.sidebarSurface.copy(alpha = BiliFocus.HomeSidebarRefinedMidAlpha),
        homeColors.sidebarSurface.copy(alpha = BiliFocus.HomeSidebarRefinedEndAlpha),
      ),
    )
  }
  val sidebarBorder = if (cinematicVisualsEnabled) {
    homeColors.textPrimary.copy(alpha = BiliFocus.HomeSidebarCinematicBorderAlpha)
  } else {
    homeColors.glassBorder
  }
  Column(
    modifier = Modifier
      .width(ui.dp(BiliSizing.SidebarWidth).coerceAtLeast(BiliSizing.MinimumTouchTarget + ui.dp(BiliSpacing.Md) * 2))
      .fillMaxHeight()
      .clip(sidebarShape)
      .then(
        if (liquidGlassEnabled) {
          Modifier.biliLiquidGlassSurface(
            enabled = true,
            shape = sidebarShape,
            surfaceColor = homeColors.sidebarSurface.copy(alpha = BiliFocus.HomeSidebarLiquidGlassSurfaceAlpha),
            borderColor = sidebarBorder,
            borderWidth = ui.dp(BiliFocus.RestingBorderWidth),
          )
        } else {
          Modifier
            .background(sidebarBackground)
            .border(BorderStroke(ui.dp(BiliFocus.RestingBorderWidth), sidebarBorder), sidebarShape)
        },
      )
      .padding(horizontal = ui.dp(BiliSpacing.Md), vertical = ui.dp(BiliSizing.ContentPadding)),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (userSession.isLoggedIn) {
      AccountStatusAvatar(userSession = userSession)
    } else {
      AccountNavItem(
        selected = accountSelected,
        userSession = userSession,
        autoConfirmOnFocus = autoConfirmOnFocus,
        modifier = Modifier.focusRequester(accountFocusRequester),
        onClick = onAccountSelected,
        onMoveRight = {
          onMoveRight(selectedDestination)
        },
      )
    }
    Spacer(modifier = Modifier.height(ui.dp(BiliSizing.SidebarNavGroupTopPadding)))
    Column(
      modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(ui.dp(BiliSizing.SidebarNavGroupSpacing)),
    ) {
      AppDestination.entries.forEach { destination ->
        AppNavItem(
          destination = destination,
          selected = !accountSelected && selectedDestination == destination,
          autoConfirmOnFocus = shouldAutoConfirmDestination(destination),
          modifier = Modifier
            .focusRequester(navFocusRequesters.getValue(destination))
            .focusProperties {
              if (destination == AppDestination.Settings) down = exitFocusRequester
            },
          onClick = {
            onDestinationSelected(destination)
          },
          onMoveRight = {
            onMoveRight(destination)
          },
        )
      }
    }
    AppExitButton(
      onExitApp = onExitApp,
      modifier = Modifier.focusRequester(exitFocusRequester).focusProperties {
        up = navFocusRequesters.getValue(AppDestination.Settings)
      },
      onMoveRight = { onMoveRight(selectedDestination) },
    )
  }
}

@Composable
private fun AccountNavItem(
  selected: Boolean,
  userSession: UserSession,
  autoConfirmOnFocus: Boolean,
  modifier: Modifier,
  onClick: () -> Unit,
  onMoveRight: () -> Boolean,
) {
  val ui = LocalUiMetrics.current
  val performancePolicy = LocalBiliPerformancePolicy.current
  val homeColors = LocalHomeColors.current
  val cinematicVisualsEnabled = performancePolicy.cinematicVisualEffectsEnabled
  BiliFocusableSurface(
    shape = CircleShape,
    shadowOnFocus = !cinematicVisualsEnabled,
    focusedScale = if (cinematicVisualsEnabled) BiliFocus.CinematicNavScale else BiliFocus.CardScale,
    focusedBorderColor = if (cinematicVisualsEnabled) {
      homeColors.textPrimary.copy(alpha = BiliFocus.CinematicFocusedBorderAlpha)
    } else {
      null
    },
    restingBorderColor = if (cinematicVisualsEnabled) {
      homeColors.textPrimary.copy(alpha = BiliFocus.CinematicRestingBorderAlpha)
    } else {
      null
    },
    focusedBorderWidth = if (cinematicVisualsEnabled) ui.dp(BiliFocus.RestingBorderWidth) else ui.dp(BiliFocus.BorderWidth),
    focusedBackgroundColor = if (cinematicVisualsEnabled) {
      homeColors.textPrimary.copy(alpha = BiliFocus.CinematicFocusedBackgroundAlpha)
    } else {
      null
    },
    restingBackgroundColor = if (cinematicVisualsEnabled) {
      homeColors.textPrimary.copy(
        alpha = if (selected) {
          BiliFocus.CinematicSelectedBackgroundAlpha
        } else {
          BiliFocus.CinematicRestingBackgroundAlpha
        },
      )
    } else {
      null
    },
    modifier = modifier
      .fillMaxWidth()
      .height(ui.dp(BiliSizing.NavItemHeight))
      .onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) {
          onMoveRight()
        } else {
          false
        }
      },
    onClick = onClick,
    onFocused = {
      if (autoConfirmOnFocus && !selected) {
        onClick()
      }
    },
  ) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      AccountAvatar(userSession = userSession)
    }
  }
}

@Composable
private fun AccountStatusAvatar(userSession: UserSession) {
  val ui = LocalUiMetrics.current
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(ui.dp(BiliSizing.NavItemHeight))
      .focusProperties { canFocus = false },
    contentAlignment = Alignment.Center,
  ) {
    AccountAvatar(userSession = userSession)
  }
}

@Composable
private fun AccountAvatar(userSession: UserSession) {
  val ui = LocalUiMetrics.current
  val context = LocalContext.current
  val performancePolicy = LocalBiliPerformancePolicy.current
  val homeColors = LocalHomeColors.current
  val accountAvatarRequestSizePx = if (performancePolicy.lowSpecMode) {
    performancePolicy.ownerAvatarSizePx
  } else {
    BiliImageSizing.AccountAvatarSizePx
  }
  val fallbackPainter = ColorPainter(BiliColors.SurfaceElevated)
  val face = userSession.face.orEmpty()

  Box(
    modifier = Modifier.size(ui.dp(BiliSizing.AccountAvatarContainerSize)),
    contentAlignment = Alignment.Center,
  ) {
    if (userSession.isLoggedIn && face.isNotBlank()) {
      val request = remember(
        context,
        face,
        accountAvatarRequestSizePx,
        performancePolicy.ownerAvatarRgb565Enabled,
        performancePolicy.imageMemoryCacheEnabled,
      ) {
        buildOwnerAvatarRequest(
          context = context,
          url = face,
          sizePx = accountAvatarRequestSizePx,
          allowRgb565 = performancePolicy.ownerAvatarRgb565Enabled,
          memoryCacheEnabled = performancePolicy.imageMemoryCacheEnabled,
        )
      }
      AsyncImage(
        model = request,
        contentDescription = userSession.uname?.let { name -> convertChineseText(name) } ?: stringResource(R.string.account_logged_in_default),
        contentScale = ContentScale.Crop,
        placeholder = fallbackPainter,
        error = fallbackPainter,
        modifier = Modifier
          .align(Alignment.Center)
          .size(ui.dp(BiliSizing.AccountAvatarSize))
          .clip(CircleShape)
          .background(BiliColors.SurfaceElevated),
      )
    } else {
      Icon(
        painter = painterResource(R.drawable.ic_nav_account),
        contentDescription = stringResource(R.string.nav_login),
        tint = if (userSession.isLoggedIn) homeColors.accent else homeColors.textSecondary,
        modifier = Modifier.size(ui.dp(BiliSizing.NavIconSize)),
      )
    }

    if (userSession.isLoggedIn && userSession.isVip) {
      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .offset(x = ui.dp(BiliSpacing.Xs), y = ui.dp(BiliSpacing.Xs))
          .size(ui.dp(BiliSizing.AccountVipBadgeSize))
          .clip(CircleShape)
          .background(BiliColors.BiliPink),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = stringResource(R.string.account_vip_badge),
          color = BiliColors.TextPrimary,
          fontSize = ui.sp(BiliTypography.AccountVipBadge),
          lineHeight = ui.sp(BiliTypography.AccountVipBadgeLineHeight),
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}

@Composable
private fun AppNavItem(
  destination: AppDestination,
  selected: Boolean,
  autoConfirmOnFocus: Boolean,
  modifier: Modifier,
  onClick: () -> Unit,
  onMoveRight: () -> Boolean,
) {
  val ui = LocalUiMetrics.current
  var focused by remember { mutableStateOf(false) }
  val homeColors = LocalHomeColors.current
  val performancePolicy = LocalBiliPerformancePolicy.current
  val cinematicVisualsEnabled = performancePolicy.cinematicVisualEffectsEnabled

  BiliFocusableSurface(
    shape = RoundedCornerShape(ui.dp(BiliRadius.Pill)),
    shadowOnFocus = !cinematicVisualsEnabled,
    focusedScale = if (cinematicVisualsEnabled) BiliFocus.CinematicNavScale else BiliFocus.CardScale,
    focusedBorderColor = if (cinematicVisualsEnabled) {
      homeColors.textPrimary.copy(alpha = BiliFocus.CinematicFocusedBorderAlpha)
    } else {
      null
    },
    restingBorderColor = if (cinematicVisualsEnabled) {
      homeColors.textPrimary.copy(alpha = BiliFocus.CinematicRestingBorderAlpha)
    } else {
      null
    },
    focusedBorderWidth = if (cinematicVisualsEnabled) ui.dp(BiliFocus.RestingBorderWidth) else ui.dp(BiliFocus.BorderWidth),
    focusedBackgroundColor = if (cinematicVisualsEnabled) {
      homeColors.textPrimary.copy(alpha = BiliFocus.CinematicFocusedBackgroundAlpha)
    } else {
      null
    },
    restingBackgroundColor = if (cinematicVisualsEnabled) {
      homeColors.textPrimary.copy(
        alpha = if (selected) {
          BiliFocus.CinematicSelectedBackgroundAlpha
        } else {
          BiliFocus.CinematicRestingBackgroundAlpha
        },
      )
    } else {
      null
    },
    modifier = modifier
      .fillMaxWidth()
      .height(ui.dp(BiliSizing.NavItemHeight))
      .onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) {
          onMoveRight()
        } else {
          false
        }
      },
    onFocusChanged = { focused = it },
    onClick = onClick,
    onFocused = {
      if (autoConfirmOnFocus && !selected) {
        onClick()
      }
    },
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(ui.dp(BiliSpacing.Sm)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        painter = painterResource(destination.iconRes),
        contentDescription = stringResource(destination.titleRes),
        tint = if (selected || focused) homeColors.accent else homeColors.textSecondary,
        modifier = Modifier
          .width(ui.dp(BiliSizing.NavIconSize))
          .height(ui.dp(BiliSizing.NavIconSize)),
      )
    }
  }
}
