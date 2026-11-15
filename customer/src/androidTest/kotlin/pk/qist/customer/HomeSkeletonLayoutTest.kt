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
import pk.qist.customer.ui.dashboard.HomeOverview
import pk.qist.customer.ui.dashboard.HomeSkeleton
import pk.qist.customer.ui.theme.QistTheme

/** Regression coverage for the loading-to-content jump on the Home screen. */
@RunWith(AndroidJUnit4::class)
class HomeSkeletonLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun headerAndInstallmentCardStayAnchoredWhenDataAppears() {
        var loading by mutableStateOf(true)
        val data = DashboardSample(
            name = "Rana Umar", phone = "", model = "Phone", amount = "7,500",
            dueDate = "15 Nov 2026", days = 5, paid = 1, total = 4,
            retailer = "Retailer", advance = 0, installment = 7500,
            schedule = emptyList(), payments = emptyList(), totalPrice = 30000,
            paidTotal = 7500, remaining = 22500, frequency = "monthly",
            status = "active", lockState = null, retailerPhone = "", retailerAddress = "",
            paymentMethods = emptyList(), contractId = "test-contract", contracts = emptyList(),
            nextCursor = null,
        )
        compose.setContent {
            QistTheme {
                if (loading) HomeSkeleton()
                else HomeOverview(data, null, false, {}, {}, {}, {}, {})
            }
        }

        val beforeCard = compose.onNodeWithTag("home-first-card").getUnclippedBoundsInRoot()
        val beforeHeader = compose.onNodeWithTag("home-navy-header").getUnclippedBoundsInRoot()
        compose.runOnIdle { loading = false }
        val afterCard = compose.onNodeWithTag("home-first-card").getUnclippedBoundsInRoot()
        val afterHeader = compose.onNodeWithTag("home-navy-header").getUnclippedBoundsInRoot()

        assertEquals(beforeHeader.height, afterHeader.height)
        assertEquals(beforeCard.top, afterCard.top)
        assertEquals(beforeCard.left, afterCard.left)
    }
}
