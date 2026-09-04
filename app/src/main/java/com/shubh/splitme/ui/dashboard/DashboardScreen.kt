package com.shubh.splitme.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
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
        factory = DashboardViewModel.Factory(app.authRepository, app.billRepository, app.memberRepository)
    )

    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedRangeIndex by remember { mutableIntStateOf(1) }
    val ranges = TimeRange.entries
    val rangeLabels = listOf("Day", "Week", "Month", "Year")

    LaunchedEffect(Unit) {
        viewModel.error.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", fontWeight = FontWeight.Bold, color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = TextPrimary)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                BalanceHeaderCard(
                    label = "Total Balance",
                    amountText = "${if (state.netBalance >= 0) "+" else ""}${"%.2f".format(state.netBalance)}",
                    amountColor = if (state.netBalance >= 0) PositiveGreen else NegativeRed,
                    subContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            Column {
                                Text("You are owed", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    "%.2f".format(state.totalOwedToMe),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PositiveGreen
                                )
                            }
                            Column {
                                Text("You owe", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
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
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Statistic", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
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
                Text("Spend", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }

            if (state.categoryBreakdown.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No spending yet.", color = TextSecondary)
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
                                        category = spend.category,
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
                    "People Summary",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }

            if (state.individualBalances.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No active balances with others.", color = TextSecondary)
                    }
                }
            } else {
                items(state.individualBalances) { summary ->
                    val color = if (summary.balance > 0) PositiveGreen else NegativeRed
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            InitialsAvatar(name = summary.memberName, size = 40)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(summary.memberName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(
                                    if (summary.balance > 0) "owes you" else "you owe",
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
