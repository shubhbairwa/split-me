package com.shubh.splitme.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shubh.splitme.SplitMeApplication
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onNavigateToProfile: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as SplitMeApplication
    val viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(app.billRepository, app.memberRepository)
    )

    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("splitMe Dashboard") },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                BalanceCard(state)
            }
            
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        title = "You are owed",
                        amount = state.totalOwedToMe,
                        icon = Icons.Default.ArrowDownward,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    StatCard(
                        title = "You owe",
                        amount = state.totalIOwe,
                        icon = Icons.Default.ArrowUpward,
                        color = Color(0xFFF44336),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text(
                    "People Summary",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.Bold
                )
            }

            if (state.individualBalances.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No active balances with others.")
                    }
                }
            } else {
                items(state.individualBalances) { summary ->
                    val color = if (summary.balance > 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    ListItem(
                        headlineContent = { Text(summary.memberName) },
                        supportingContent = { 
                            Text(if (summary.balance > 0) "owes you" else "you owe") 
                        },
                        trailingContent = {
                            Text(
                                "${if (summary.balance > 0) "+" else ""}${"%.2f".format(summary.balance)}",
                                color = color,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingContent = {
                            Icon(
                                if (summary.balance > 0) Icons.Default.PersonOutline else Icons.Default.Person,
                                contentDescription = null,
                                tint = color
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun BalanceCard(state: DashboardState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Total Balance", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${if (state.netBalance >= 0) "+" else ""}${"%.2f".format(state.netBalance)}",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp
                ),
                color = if (state.netBalance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    amount: Double,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(
                "${"%.2f".format(amount)}",
                style = MaterialTheme.typography.titleLarge,
                color = color
            )
        }
    }
}
