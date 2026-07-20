package com.docufind.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.docufind.app.ui.components.HowToUseSection
import com.docufind.app.ui.theme.DocuFindTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HowToUseSectionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun howToUse_collapsedThenExpanded_showsSteps() {
        composeRule.setContent {
            DocuFindTheme {
                HowToUseSection(expanded = false, onToggle = {})
            }
        }
        composeRule.onNodeWithText("How to Use DocuFind").assertIsDisplayed()
    }

    @Test
    fun howToUse_expanded_showsFirstStep() {
        composeRule.setContent {
            DocuFindTheme {
                HowToUseSection(expanded = true, onToggle = {})
            }
        }
        composeRule.onNodeWithText("Add").assertIsDisplayed()
    }

    @Test
    fun howToUse_toggleCallback_firesOnClick() {
        var expanded = false
        composeRule.setContent {
            DocuFindTheme {
                HowToUseSection(
                    expanded = expanded,
                    onToggle = { expanded = !expanded }
                )
            }
        }
        composeRule.onNodeWithText("How to Use DocuFind").performClick()
        assert(expanded)
    }
}
