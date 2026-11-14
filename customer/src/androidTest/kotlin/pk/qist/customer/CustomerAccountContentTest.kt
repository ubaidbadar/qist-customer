package pk.qist.customer

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pk.qist.customer.data.parseOverview
import pk.qist.customer.ui.dashboard.HomeOverview
import pk.qist.customer.ui.dashboard.Dashboard
import pk.qist.customer.ui.CustomerTab
import pk.qist.customer.ui.localization.LocalUrdu
import pk.qist.customer.ui.localization.paymentLabel
import pk.qist.customer.ui.localization.paymentMethodLabel
import pk.qist.customer.ui.profile.CustomerProfile
import pk.qist.customer.ui.profile.ProfileInformation
import pk.qist.customer.ui.plan.PlanDetails
import pk.qist.customer.ui.theme.QistTheme

/** Device-level content assertions complement the JVM state and request tests. */
@RunWith(AndroidJUnit4::class)
class CustomerAccountContentTest {
    @get:Rule val compose = createComposeRule()
    private fun account() = parseOverview(Json.parseToJsonElement("""
        {"profile":{"name":"Customer","phone":"03001234567"},"today":"2026-11-14",
         "contract":null,"contracts":[],"payments":{"items":[],"nextCursor":null}}
    """).jsonObject)

    @Test fun supportWithoutPhoneKeepsAddressAndHidesContactActions() {
        compose.setContent { QistTheme {
            ProfileInformation(account().copy(retailer = "Shop", retailerAddress = "Faisalabad shop address"), privacy = false, onBack = {})
        } }
        compose.onNodeWithText("Faisalabad shop address").assertExists()
        compose.onNodeWithText("Phone number not provided.").assertExists()
        compose.onNodeWithText("Call", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("WhatsApp", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun profileDoesNotDuplicateRetailerContactActions() {
        compose.setContent { QistTheme { Column {
            CustomerProfile("Customer", "3001234567", {}, {}, false, {}, null, {})
        } } }
        compose.onNodeWithText("Your retailer").assertDoesNotExist()
        compose.onNodeWithText("Call").assertDoesNotExist()
        compose.onNodeWithText("Help and support").assertExists()
    }

    @Test fun cancelledPlanOffersReceiptsInsteadOfPaymentInstructions() {
        compose.setContent { QistTheme {
            HomeOverview(account().copy(contractId = "plan-a", status = "cancelled", remaining = 10000, amount = "100", days = -4),
                message = null, unread = false, onHelp = {}, onPayments = {}, onPhones = {}, onInbox = {}, onContact = {})
        } }
        compose.onNodeWithText("How to pay").assertDoesNotExist()
        compose.onNodeWithText("Overdue installment").assertDoesNotExist()
        compose.onNodeWithText("View receipts").assertExists()
        compose.onNodeWithText("Contact your retailer for details about this plan.").assertExists()
    }

    @Test fun cancelledScheduleDoesNotDescribeHistoricalRowsAsOverdue() {
        val data = account().copy(contractId = "cancelled-plan", status = "cancelled", total = 1,
            schedule = listOf(pk.qist.customer.data.Installment(1, "2020-01-01", 10000, 0, 0, "pending", -100)))
        compose.setContent { QistTheme { PlanDetails(data, message = null, onChange = {}) } }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Cancelled"))
        compose.onNodeWithText("Cancelled").assertExists()
        compose.onNodeWithText("Overdue").assertDoesNotExist()
        compose.onNodeWithText("Due soon").assertDoesNotExist()
    }

    @Test fun urduReceiptsUseTranslatedPaymentDescriptions() {
        val payment = pk.qist.customer.data.PaymentSample("payment", 0, "14 Nov 2026", "100", "cash", 1, "Shop", "down_payment", "", emptyList())
        compose.setContent { QistTheme { CompositionLocalProvider(LocalUrdu provides true) { Column {
            Text(paymentLabel(payment, LocalUrdu.current))
            Text(paymentMethodLabel(payment.method, LocalUrdu.current))
        } } } }
        compose.onNodeWithText("پیشگی ادائیگی").assertIsDisplayed()
        compose.onNodeWithText("نقد").assertIsDisplayed()
    }

    @Test fun openReceiptSurvivesAnOverviewWhoseFirstPageOmitsIt() {
        val payment = pk.qist.customer.data.PaymentSample("older-payment", 1, "14 Nov 2026", "100", "cash", 12, "Shop", "installment", "", listOf(1))
        var data by mutableStateOf(account().copy(contractId = "plan-a", payments = listOf(payment)))
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = pk.qist.customer.data.CustomerPreferences(context).apply { setNotifications(false) }
        compose.setContent { QistTheme {
            Dashboard(data = data, tab = CustomerTab.Payments, onTab = {}, onHelp = {}, onSelectPhone = {},
                refreshing = false, onRefresh = {}, onMore = {}, notificationStatus = null,
                onReadNotifications = {}, onOpenNotification = {}, openInbox = false, onInboxOpened = {},
                busy = false, message = null, onDeleteAccount = {}, phone = "3001234567", preferences = preferences,
                onLanguage = {}, onLogout = {})
        } }
        compose.onNodeWithText("Installment 1").performScrollTo().performClick()
        compose.onNodeWithText("Receipt").assertIsDisplayed()
        compose.runOnIdle { data = data.copy(payments = emptyList()) }
        compose.onNodeWithText("Receipt").assertIsDisplayed()
        compose.onNodeWithText("Receipt no.").assertExists()
    }
}
