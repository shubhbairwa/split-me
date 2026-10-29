package com.shubh.splitme.ui.dashboard

import androidx.compose.ui.res.stringResource
import com.shubh.splitme.R
import com.shubh.splitme.ui.common.categoryLabel
import com.shubh.splitme.ui.theme.tabScreenInsets
import com.shubh.splitme.ui.theme.glassCardColors
import com.shubh.splitme.ui.theme.glassBorder
import com.shubh.splitme.ui.theme.glassShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shubh.splitme.SplitMeApplication
import com.shubh.splitme.ui.components.BalanceHeaderCard
import com.shubh.splitme.ui.components.CategorySpendCard
import com.shubh.splitme.ui.components.InitialsAvatar
import com.shubh.splitme.ui.components.SegmentedTabs
import com.shubh.splitme.ui.components.SpendLineChart
import com.shubh.splitme.ui.theme.CardElevation
import com.shubh.splitme.ui.theme.NegativeRed
import com.shubh.splitme.ui.theme.PositiveGreen
import com.shubh.splitme.ui.theme.TextPrimary
import com.shubh.splitme.ui.theme.TextSecondary
import com.shubh.splitme.ui.theme.categoryStyleFor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onNavigateToProfile: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as SplitMeApplication
    val viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(app.authRepository, app.billRepository, app.memberRepository, app.groupRepository)
    )

    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedRangeIndex by remember { mutableIntStateOf(1) }
    val ranges = TimeRange.entries
    val rangeLabels = listOf(stringResource(R.string.range_day), stringResource(R.string.range_week), stringResource(R.string.range_month), stringResource(R.string.range_year))

    LaunchedEffect(Unit) {
        viewModel.error.collect { message ->
            snackbarHostState.showSnackbar(message.resolve(context))
        }
    }

    Scaffold(
        contentWindowInsets = tabScreenInsets(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_title), fontWeight = FontWeight.Bold, color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Outlined.AccountCircle, contentDescription = stringResource(R.string.nav_profile), tint = TextPrimary)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                BalanceHeaderCard(
                    label = stringResource(R.string.dashboard_total_balance),
                    amountText = "${if (state.netBalance >= 0) "+" else ""}${"%.2f".format(state.netBalance)}",
                    amountColor = if (state.netBalance >= 0) PositiveGreen else NegativeRed,
                    subContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            Column {
                                Text(stringResource(R.string.dashboard_you_are_owed), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    "%.2f".format(state.totalOwedToMe),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PositiveGreen
                                )
                            }
                            Column {
                                Text(stringResource(R.string.dashboard_you_owe), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    "%.2f".format(state.totalIOwe),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NegativeRed
                                )
                            }
                        }
                    }
                )
            }

            item {
                Card(
                    modifier = Modifier.glassShadow(MaterialTheme.shapes.large).fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = glassCardColors(),
                    border = glassBorder(),
                    elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(stringResource(R.string.dashboard_statistic), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        SegmentedTabs(
                            options = rangeLabels,
                            selectedIndex = selectedRangeIndex,
                            onSelect = { selectedRangeIndex = it }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SpendLineChart(points = state.spendByRange[ranges[selectedRangeIndex]] ?: emptyList())
                    }
                }
            }

            item {
                Text(stringResource(R.string.dashboard_spend), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }

            if (state.categoryBreakdown.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.dashboard_no_spending), color = TextSecondary)
                    }
                }
            } else {
                item {
                    val rows = state.categoryBreakdown.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        rows.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { spend ->
                                    CategorySpendCard(
                                        style = categoryStyleFor(spend.category),
                                        category = categoryLabel(spend.category),
                                        percent = spend.percent,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    stringResource(R.string.dashboard_people_summary),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }

            if (state.individualBalances.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.dashboard_no_balances), color = TextSecondary)
                    }
                }
            } else {
                items(state.individualBalances) { summary ->
                    val color = if (summary.balance > 0) PositiveGreen else NegativeRed
                    Card(
                        modifier = Modifier.glassShadow(MaterialTheme.shapes.medium).fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = glassCardColors(),
                        border = glassBorder(),
                        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            InitialsAvatar(name = summary.memberName, size = 40, photoUrl = summary.photoUrl)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(summary.memberName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(
                                    if (summary.balance > 0) stringResource(R.string.dashboard_owes_you) else stringResource(R.string.dashboard_you_owe_lower),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                "${if (summary.balance > 0) "+" else ""}${"%.2f".format(summary.balance)}",
                                color = color,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
