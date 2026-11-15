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
import pk.qist.customer.ui.dashboard.PlanSkeleton
import pk.qist.customer.ui.plan.PlanDetails
import pk.qist.customer.ui.theme.QistTheme

/** The plan skeleton must occupy the same card and fact-row bounds as real content. */
@RunWith(AndroidJUnit4::class)
class PlanSkeletonLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun summaryCardAndRowsKeepTheirBoundsWhenPlanLoads() {
        var loading by mutableStateOf(true)
        val data = DashboardSample(
            name = "Customer", phone = "", model = "Test device", amount = "7,500",
            dueDate = "15 Nov 2026", days = 5, paid = 1, total = 6,
            retailer = "Test shop", advance = 10000, installment = 7500,
            schedule = emptyList(), payments = emptyList(), totalPrice = 55000,
            paidTotal = 17500, remaining = 37500, frequency = "monthly",
            status = "active", lockState = null, retailerPhone = "", retailerAddress = "",
            paymentMethods = emptyList(), contractId = "test-contract", contracts = emptyList(),
            nextCursor = null,
        )
        compose.setContent { QistTheme {
            if (loading) PlanSkeleton() else PlanDetails(data, null) {}
        } }

        val before = compose.onNodeWithTag("plan-summary-card").getUnclippedBoundsInRoot()
        val beforeRows = (0 until 4).map {
            compose.onAllNodesWithTag("plan-fact-row")[it].getUnclippedBoundsInRoot()
        }

        compose.runOnIdle { loading = false }

        val after = compose.onNodeWithTag("plan-summary-card").getUnclippedBoundsInRoot()
        val afterRows = (0 until 4).map {
            compose.onAllNodesWithTag("plan-fact-row")[it].getUnclippedBoundsInRoot()
        }
        assertEquals(before.top, after.top)
        assertEquals(before.height, after.height)
        beforeRows.zip(afterRows).forEach { (skeleton, loaded) ->
            assertEquals(skeleton.top, loaded.top)
            assertEquals(skeleton.height, loaded.height)
        }
    }
}
