package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.Debt
import com.example.ui.DebtListScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DebtListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDebtListScreen_rendersEmptyState() {
        composeTestRule.setContent {
            MyApplicationTheme {
                DebtListScreen(
                    debts = emptyList(),
                    totalFact = 0.0,
                    totalPlan = 0.0,
                    totalToCancel = 0.0,
                    onAddClick = {},
                    onEditClick = {},
                    onDeleteClick = {},
                    onProtectionClick = {},
                    onStrategiesClick = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Check top bar title
        composeTestRule.onNodeWithText("Реестр задолженностей").assertExists()

        // Check empty state image and text
        composeTestRule.onNodeWithContentDescription("Пусто").assertExists()
        composeTestRule.onNodeWithText("Реестр чист.", substring = true).assertExists()
    }

    @Test
    fun testDebtListScreen_rendersDebtsAndTotals() {
        val testDebts = listOf(
            Debt(
                id = "d1",
                creditorName = "Банк Тинькофф",
                contractDate = "12.01.2023",
                factAmount = 150000.0,
                penaltyAmount = 30000.0,
                insuranceAmount = 10000.0,
                planAmount = 110000.0,
                status = "В работе",
                isMortgage = false
            )
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                DebtListScreen(
                    debts = testDebts,
                    totalFact = 150000.0,
                    totalPlan = 110000.0,
                    totalToCancel = 40000.0,
                    onAddClick = {},
                    onEditClick = {},
                    onDeleteClick = {},
                    onProtectionClick = {},
                    onStrategiesClick = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Check creditor name is displayed (with substring to match "№1 | Банк Тинькофф")
        composeTestRule.onNodeWithText("Банк Тинькофф", substring = true).assertExists()
        // Check top bar title is displayed
        composeTestRule.onNodeWithText("Реестр задолженностей").assertExists()
        // Check protection button is displayed
        composeTestRule.onNodeWithTag("protection_button_d1").assertExists()
    }
}
