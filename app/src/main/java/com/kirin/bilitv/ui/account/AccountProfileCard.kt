package com.kirin.bilitv.ui.account

import com.kirin.bilitv.ui.theme.LocalUiMetrics

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.kirin.bilitv.R
import com.kirin.bilitv.core.image.BiliImageSizing
import com.kirin.bilitv.core.image.buildOwnerAvatarRequest
import com.kirin.bilitv.core.storage.UserSession
import com.kirin.bilitv.ui.i18n.convertChineseText
import com.kirin.bilitv.ui.settings.LocalBiliPerformancePolicy
import com.kirin.bilitv.ui.theme.BiliColors
import com.kirin.bilitv.ui.theme.BiliRadius
import com.kirin.bilitv.ui.theme.BiliSizing
import com.kirin.bilitv.ui.theme.BiliSpacing
import com.kirin.bilitv.ui.theme.BiliTypography

@Composable
fun AccountProfileCard(
  userSession: UserSession,
  modifier: Modifier = Modifier,
  action: (@Composable () -> Unit)? = null,
) {
  val ui = LocalUiMetrics.current
  Column(
    modifier = modifier
      .background(BiliColors.SurfaceElevated, RoundedCornerShape(ui.dp(BiliRadius.Panel)))
      .padding(ui.dp(BiliSpacing.Xl)),
    verticalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Lg)),
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
      val inlineMinWidth = ui.dp(BiliSizing.AccountProfileAvatarSize + BiliSpacing.Xl + BiliSizing.AccountProfileIdentityMinWidth)
      if (maxWidth >= inlineMinWidth) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Xl))) {
          ProfileAvatar(userSession = userSession)
          ProfileIdentity(userSession, Modifier.weight(1f))
        }
      } else {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Xl))) {
          ProfileAvatar(userSession = userSession)
          ProfileIdentity(userSession, Modifier.fillMaxWidth())
        }
      }
    }
    if (action != null) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
      ) {
        action()
      }
    }
  }
}

@Composable
private fun ProfileIdentity(userSession: UserSession, modifier: Modifier) {
  val ui = LocalUiMetrics.current
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(ui.dp(BiliSpacing.Md)),
  ) {
    Text(
      text = userSession.uname?.let { name -> convertChineseText(name) }
        ?: userSession.mid?.let { stringResource(R.string.account_logged_in_default) }
        ?: stringResource(R.string.account_profile_fallback),
      color = BiliColors.TextPrimary,
      fontSize = ui.sp(BiliTypography.ScreenTitle),
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = stringResource(R.string.account_uid, userSession.mid?.toString().orEmpty()),
      color = BiliColors.TextSecondary,
      fontSize = ui.sp(BiliTypography.Body),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = if (userSession.isVip) {
        stringResource(R.string.account_vip_status)
      } else {
        stringResource(R.string.account_normal_status)
      },
      color = if (userSession.isVip) BiliColors.BiliPink else BiliColors.TextTertiary,
      fontSize = ui.sp(BiliTypography.BodySmall),
      fontWeight = FontWeight.Medium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun ProfileAvatar(userSession: UserSession) {
  val ui = LocalUiMetrics.current
  val context = LocalContext.current
  val performancePolicy = LocalBiliPerformancePolicy.current
  val profileAvatarRequestSizePx = if (performancePolicy.lowSpecMode) {
    BiliImageSizing.AccountAvatarSizePx
  } else {
    BiliImageSizing.AccountProfileAvatarSizePx
  }
  val fallbackPainter = ColorPainter(BiliColors.Surface)
  val face = userSession.face.orEmpty()

  Box(
    modifier = Modifier.size(ui.dp(BiliSizing.AccountProfileAvatarSize)),
    contentAlignment = Alignment.Center,
  ) {
    if (face.isNotBlank()) {
      val request = remember(
        context,
        face,
        profileAvatarRequestSizePx,
        performancePolicy.ownerAvatarRgb565Enabled,
        performancePolicy.imageMemoryCacheEnabled,
      ) {
        buildOwnerAvatarRequest(
          context = context,
          url = face,
          sizePx = profileAvatarRequestSizePx,
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
          .size(ui.dp(BiliSizing.AccountProfileAvatarSize))
          .clip(CircleShape)
          .background(BiliColors.Surface),
      )
    } else {
      Box(
        modifier = Modifier
          .size(ui.dp(BiliSizing.AccountProfileAvatarSize))
          .clip(CircleShape)
          .background(BiliColors.Surface),
        contentAlignment = Alignment.Center,
      ) {
        Image(
          painter = painterResource(R.drawable.ic_nav_account),
          contentDescription = stringResource(R.string.nav_login),
          colorFilter = ColorFilter.tint(BiliColors.BiliPink),
          modifier = Modifier.size(ui.dp(BiliSizing.AccountAvatarSize)),
        )
      }
    }

    if (userSession.isVip) {
      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .offset(x = ui.dp(BiliSpacing.Sm), y = ui.dp(BiliSpacing.Sm))
          .size(ui.dp(BiliSizing.AccountProfileVipBadgeSize))
          .clip(CircleShape)
          .background(BiliColors.BiliPink),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = stringResource(R.string.account_vip_badge),
          color = BiliColors.TextPrimary,
          fontSize = ui.sp(BiliTypography.AccountProfileVipBadge),
          lineHeight = ui.sp(BiliTypography.AccountProfileVipBadgeLineHeight),
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}
