package pk.qist.customer

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pk.qist.customer.data.DashboardSample
import pk.qist.customer.data.PaymentSample
import pk.qist.customer.ui.dashboard.PaymentsSkeleton
import pk.qist.customer.ui.payments.PaymentHistory
import pk.qist.customer.ui.theme.QistTheme

/** Confirms Payments loading placeholders preserve the real summary and history geometry. */
@RunWith(AndroidJUnit4::class)
class PaymentsSkeletonLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun summaryCardsAndHistoryRowsStayInPlaceAfterLoading() {
        var loading by mutableStateOf(true)
        val payments = listOf(
            PaymentSample("payment-1", 1, "16 Nov 2026", "90,000", "cash", 1L,
                "Shop", "installment", "", listOf(1)),
            PaymentSample("payment-2", 2, "16 Nov 2026", "10,000", "cash", 2L,
                "Shop", "down_payment", "", emptyList()),
        )
        val data = DashboardSample(
            name = "Customer", phone = "", model = "Phone", amount = "0",
            dueDate = "—", days = 0, paid = 1, total = 1,
            retailer = "Shop", advance = 10000, installment = 90000,
            schedule = emptyList(), payments = payments, totalPrice = 100000,
            paidTotal = 100000, remaining = 0, frequency = "monthly",
            status = "completed", lockState = null, retailerPhone = "", retailerAddress = "",
            paymentMethods = emptyList(), contractId = "test-contract", contracts = emptyList(),
            nextCursor = null,
        )
        compose.setContent { QistTheme {
            if (loading) PaymentsSkeleton()
            else PaymentHistory(data, busy = false, message = null, onMore = {}, onReceipt = {})
        } }

        val paidBefore = compose.onNodeWithTag("payments-paid-card").getUnclippedBoundsInRoot()
        val remainingBefore = compose.onNodeWithTag("payments-remaining-card").getUnclippedBoundsInRoot()
        val rowsBefore = (0 until 2).map {
            compose.onAllNodesWithTag("payments-history-row")[it].getUnclippedBoundsInRoot()
        }

        compose.runOnIdle { loading = false }

        val paidAfter = compose.onNodeWithTag("payments-paid-card").getUnclippedBoundsInRoot()
        val remainingAfter = compose.onNodeWithTag("payments-remaining-card").getUnclippedBoundsInRoot()
        val rowsAfter = (0 until 2).map {
            compose.onAllNodesWithTag("payments-history-row")[it].getUnclippedBoundsInRoot()
        }
        assertEquals(paidBefore.top, paidAfter.top)
        assertEquals(paidBefore.height, paidAfter.height)
        assertEquals(remainingBefore.top, remainingAfter.top)
        assertEquals(remainingBefore.height, remainingAfter.height)
        rowsBefore.zip(rowsAfter).forEach { (skeleton, loaded) ->
            assertEquals(skeleton.top, loaded.top)
            assertEquals(skeleton.height, loaded.height)
        }
    }
}
