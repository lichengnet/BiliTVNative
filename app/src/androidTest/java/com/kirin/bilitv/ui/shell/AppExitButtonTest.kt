package com.kirin.bilitv.ui.shell

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import com.kirin.bilitv.ui.theme.BiliTvTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppExitButtonTest {
  @get:Rule val composeRule = createComposeRule()

  @Test fun touchClickInvokesExitOnce() {
    var exits = 0
    composeRule.setContent { BiliTvTheme { AppExitButton(onExitApp = { exits++ }) } }
    composeRule.onNodeWithTag(AppExitButtonTestTag).performTouchInput { click() }
    composeRule.runOnIdle { assertEquals(1, exits) }
  }

  @Test fun focusDoesNotExitAndDpadConfirmExitsOnce() {
    var exits = 0
    lateinit var requester: FocusRequester
    composeRule.setContent {
      requester = remember { FocusRequester() }
      BiliTvTheme { AppExitButton(onExitApp = { exits++ }, modifier = Modifier.focusRequester(requester)) }
    }
    composeRule.runOnIdle { requester.requestFocus() }
    composeRule.runOnIdle { assertEquals(0, exits) }
    composeRule.onNodeWithTag(AppExitButtonTestTag).performKeyInput { pressKey(Key.DirectionCenter) }
    composeRule.runOnIdle { assertEquals(1, exits) }
  }
}
