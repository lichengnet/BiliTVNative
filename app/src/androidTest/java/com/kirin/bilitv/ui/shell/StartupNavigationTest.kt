package com.kirin.bilitv.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.kirin.bilitv.core.storage.UserSession
import com.kirin.bilitv.ui.input.DeviceClass
import com.kirin.bilitv.ui.input.InputMode
import com.kirin.bilitv.ui.input.InteractionProfile
import com.kirin.bilitv.ui.input.LocalInteractionMode
import com.kirin.bilitv.ui.input.LocalInteractionProfile
import com.kirin.bilitv.ui.theme.BiliTvTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StartupNavigationTest {
  @get:Rule val rule = createComposeRule()

  @Test fun touchUnvisitedSearchFocusDoesNotHijackHomeButClickWorks() = verify(InputMode.Touch)
  @Test fun remoteStartupFocusDoesNotHijackHomeButClickWorks() = verify(InputMode.Remote)

  private fun verify(mode: InputMode) {
    val selected = mutableStateOf(AppDestination.Recommend)
    val navigation = AppDestination.entries.associateWith { FocusRequester() }
    val profile = InteractionProfile(if (mode == InputMode.Remote) DeviceClass.Tv else DeviceClass.Tablet, mode)
    rule.setContent { BiliTvTheme { CompositionLocalProvider(LocalInteractionProfile provides profile, LocalInteractionMode provides profile.legacyMode) {
      AdaptiveAppScaffold(selected.value, false, UserSession(), false, FocusRequester(), navigation, false,
        onAccountSelected = {}, onExitApp = {}, onDestinationSelected = { selected.value = it },
        shouldAutoConfirmDestination = { shouldAutoConfirmDestinationOnFocus(false, false, true, mode == InputMode.Remote) },
        onMoveRight = { false }) { Box(Modifier.fillMaxSize()) }
    } } }
    rule.runOnIdle { navigation.getValue(AppDestination.Search).requestFocus() }
    rule.runOnIdle { assertEquals(AppDestination.Recommend, selected.value) }
    rule.onNodeWithContentDescription("搜索").performClick()
    rule.runOnIdle { assertEquals(AppDestination.Search, selected.value) }
  }
}
