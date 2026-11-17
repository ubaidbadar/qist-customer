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
import pk.qist.customer.data.PlanChoice
import pk.qist.customer.ui.dashboard.HomeOverview
import pk.qist.customer.ui.dashboard.HomeSkeleton
import pk.qist.customer.ui.theme.QistTheme

/** Regression coverage for the loading-to-content jump on the Home screen. */
@RunWith(AndroidJUnit4::class)
class HomeSkeletonLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun completedPlanKeepsAllThreeCardsAlignedWhenDataAppears() {
        var loading by mutableStateOf(true)
        val payments = listOf(
            PaymentSample("one", 5, "17 Nov 2026", "20,000", "cash", 1,
                "", "installment", "", listOf(5, 6)),
            PaymentSample("two", 4, "17 Nov 2026", "10,000", "cash", 2,
                "", "installment", "", listOf(4)),
        )
        val data = DashboardSample(
            name = "Ubaid Badar", phone = "", model = "Vivo Y01", amount = "0",
            dueDate = "—", days = 0, paid = 6, total = 6,
            retailer = "Retailer", advance = 0, installment = 10000,
            schedule = emptyList(), payments = payments, totalPrice = 60000,
            paidTotal = 60000, remaining = 0, frequency = "monthly",
            status = "completed", lockState = null, retailerPhone = "", retailerAddress = "",
            paymentMethods = emptyList(), contractId = "test-contract",
            contracts = listOf(PlanChoice("test-contract", "Vivo Y01", "Retailer", "completed",
                "—", "0", 0, 6, "17 Nov 2026")),
            nextCursor = null,
        )
        compose.setContent {
            QistTheme {
                if (loading) HomeSkeleton()
                else HomeOverview(data, null, false, {}, {}, {}, {}, {})
            }
        }
        val tags = listOf("home-navy-header", "home-first-card", "home-phone-card", "home-payments-card")
        val before = tags.map { compose.onNodeWithTag(it).getUnclippedBoundsInRoot() }

        compose.runOnIdle { loading = false }

        val after = tags.map { compose.onNodeWithTag(it).getUnclippedBoundsInRoot() }
        before.zip(after).forEach { (skeleton, loaded) ->
            assertEquals(skeleton.top, loaded.top)
            assertEquals(skeleton.left, loaded.left)
            assertEquals(skeleton.height, loaded.height)
        }
    }
}
