package com.shubh.splitme.ui.preview

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shubh.splitme.data.Country
import com.shubh.splitme.data.ContactInfo
import com.shubh.splitme.domain.model.Bill
import com.shubh.splitme.domain.model.BillWithShares
import com.shubh.splitme.domain.model.Group
import com.shubh.splitme.domain.model.GroupWithMembers
import com.shubh.splitme.domain.model.Member
import com.shubh.splitme.ui.dashboard.CategorySpend
import com.shubh.splitme.ui.dashboard.ChartLabel
import com.shubh.splitme.ui.dashboard.ChartPoint
import com.shubh.splitme.ui.dashboard.DashboardState
import com.shubh.splitme.ui.dashboard.MemberSummary
import com.shubh.splitme.ui.dashboard.TimeRange
import com.shubh.splitme.ui.group.GroupSummary
import com.shubh.splitme.ui.group.MemberBalance
import com.shubh.splitme.ui.theme.GlassBackground
import com.shubh.splitme.ui.theme.SplitMeTheme
import java.util.Calendar

/** Full-screen previews in light and dark mode, on a phone-sized canvas. */
@Preview(name = "Light", group = "Screen", device = "spec:width=411dp,height=891dp", showSystemUi = false)
@Preview(
    name = "Dark",
    group = "Screen",
    device = "spec:width=411dp,height=891dp",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
annotation class ScreenPreviews

/** Previews of a single component, sized to fit it, in light and dark mode. */
@Preview(name = "Light", group = "Component", widthDp = 380)
@Preview(
    name = "Dark",
    group = "Component",
    widthDp = 380,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
annotation class ComponentPreviews

/** The app's theme over the glass backdrop, the same way MainActivity sets up every screen. */
@Composable
fun PreviewScreen(content: @Composable () -> Unit) {
    SplitMeTheme {
        GlassBackground { content() }
    }
}

/** Like [PreviewScreen], with some padding around a single component. */
@Composable
fun PreviewComponent(content: @Composable () -> Unit) {
    SplitMeTheme {
        GlassBackground {
            Box(modifier = Modifier.padding(16.dp)) { content() }
        }
    }
}

/** Sample data for previews only. Never used by the running app. */
object PreviewData {
    val india = Country(iso = "IN", dialCode = 91)

    val me = Member(id = "me", name = "Dhruv Rawat", phoneNumber = "+919876543210", location = "Delhi", dateOfBirth = "1998-04-12", hasAccount = true)
    val aisha = Member(id = "aisha", name = "Aisha Khan", phoneNumber = "+919812345678")
    val rohan = Member(id = "rohan", name = "Rohan Mehta", phoneNumber = "+919898989898")
    val priya = Member(id = "priya", name = "Priya Sharma", phoneNumber = "+919900112233")
    val members = listOf(me, aisha, rohan, priya)

    val goaTrip = GroupWithMembers(Group(id = "goa", name = "Goa Trip", description = "Beach weekend"), members)
    val flat = GroupWithMembers(Group(id = "flat", name = "Flat 402"), listOf(me, rohan))
    val office = GroupWithMembers(Group(id = "office", name = "Office Lunch"), listOf(me, aisha, priya))
    val groups = listOf(goaTrip, flat, office)

    val groupSummaries = mapOf(
        "goa" to GroupSummary(totalSpent = 12450.0, myBalance = 2300.0),
        "flat" to GroupSummary(totalSpent = 8200.0, myBalance = -1450.0),
        "office" to GroupSummary(totalSpent = 0.0, myBalance = 0.0)
    )

    val balances = listOf(
        MemberBalance(me, 2300.0),
        MemberBalance(aisha, -1200.0),
        MemberBalance(rohan, -1100.0),
        MemberBalance(priya, 0.0)
    )

    val bills = listOf(
        BillWithShares(Bill(id = "b1", groupId = "goa", title = "Beach shack dinner", totalAmount = 3200.0, category = "Food", payerId = "me"), emptyList()),
        BillWithShares(Bill(id = "b2", groupId = "goa", title = "Scooter rental", totalAmount = 1800.0, category = "Transport", payerId = "aisha"), emptyList()),
        BillWithShares(Bill(id = "b3", groupId = "goa", title = "Souvenirs", totalAmount = 950.0, category = "Shopping", payerId = "rohan"), emptyList()),
        BillWithShares(Bill(id = "b4", groupId = "goa", title = "", totalAmount = 500.0, category = "SettleUp", payerId = "priya"), emptyList())
    )

    val contacts = listOf(
        ContactInfo(id = "c1", name = "Aisha Khan", phone = "+91 98123 45678", email = null, photoUri = null),
        ContactInfo(id = "c2", name = "Kabir Singh", phone = "+91 99887 76655", email = null, photoUri = null),
        ContactInfo(id = "c3", name = "Neha Gupta", phone = "+91 91234 56789", email = null, photoUri = null),
        ContactInfo(id = "c4", name = "Vikram Rao", phone = "+91 90000 11111", email = null, photoUri = null)
    )

    private val week = listOf(
        Calendar.MONDAY to 420.0, Calendar.TUESDAY to 1250.0, Calendar.WEDNESDAY to 380.0,
        Calendar.THURSDAY to 2100.0, Calendar.FRIDAY to 900.0, Calendar.SATURDAY to 3200.0, Calendar.SUNDAY to 1500.0
    ).map { (day, value) -> ChartPoint(ChartLabel.Weekday(day), value) }

    val chartPoints = week

    val dashboard = DashboardState(
        totalOwedToMe = 4250.0,
        totalIOwe = 1450.0,
        netBalance = 2800.0,
        individualBalances = listOf(
            MemberSummary("Aisha Khan", null, 1200.0),
            MemberSummary("Rohan Mehta", null, -1450.0),
            MemberSummary("Priya Sharma", null, 3050.0)
        ),
        spendByRange = mapOf(
            TimeRange.DAY to listOf(9, 12, 15, 18, 21).mapIndexed { i, h -> ChartPoint(ChartLabel.Hour(h), listOf(120.0, 450.0, 80.0, 900.0, 300.0)[i]) },
            TimeRange.WEEK to week,
            TimeRange.MONTH to (1..4).map { ChartPoint(ChartLabel.WeekOfMonth(it), listOf(4200.0, 2600.0, 5100.0, 3300.0)[it - 1]) },
            TimeRange.YEAR to (0..11).map { ChartPoint(ChartLabel.Month(it), 2000.0 + (it * 731 % 5000)) }
        ),
        categoryBreakdown = listOf(
            CategorySpend("Food", 5200.0, 42f),
            CategorySpend("Transport", 3100.0, 25f),
            CategorySpend("Shopping", 2400.0, 19f),
            CategorySpend("Entertainment", 1750.0, 14f)
        )
    )
}
