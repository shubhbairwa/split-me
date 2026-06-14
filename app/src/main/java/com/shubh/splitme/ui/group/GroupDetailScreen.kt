package com.shubh.splitme.ui.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shubh.splitme.SplitMeApplication
import com.shubh.splitme.data.entity.GroupWithMembers
import com.shubh.splitme.ui.bill.BillEntryScreen
import com.shubh.splitme.ui.bill.BillViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupWithMembers: GroupWithMembers,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as SplitMeApplication
    val billViewModel: BillViewModel = viewModel(factory = BillViewModel.Factory(app.billRepository))
    
    val bills by billViewModel.getBillsByGroup(groupWithMembers.group.id).collectAsState()
    var showBillEntry by remember { mutableStateOf(false) }
    var showSettleUp by remember { mutableStateOf(false) }

    if (showSettleUp) {
        SettleUpScreen(
            groupId = groupWithMembers.group.id,
            onBack = { showSettleUp = false }
        )
    } else if (showBillEntry) {
        BillEntryScreen(
            groupId = groupWithMembers.group.id,
            groupName = groupWithMembers.group.name,
            members = groupWithMembers.members,
            onDismiss = { showBillEntry = false },
            onSave = { title, amount, category, payerId, shares ->
                billViewModel.addBill(groupWithMembers.group.id, title, amount, category, payerId, shares)
                showBillEntry = false
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(groupWithMembers.group.name) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(onClick = { showSettleUp = true }) {
                            Text("Settle Up", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showBillEntry = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Bill")
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                // Summary Card
                val totalSpent = bills.sumOf { it.bill.totalAmount }
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total Group Spending", style = MaterialTheme.typography.titleMedium)
                        Text("${"%.2f".format(totalSpent)}", style = MaterialTheme.typography.headlineLarge)
                    }
                }

                Text(
                    "Recent Bills",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleSmall
                )

                if (bills.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No bills yet.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        items(bills) { billWithShares ->
                            ListItem(
                                headlineContent = { Text(billWithShares.bill.title) },
                                supportingContent = { 
                                    val payerName = groupWithMembers.members.find { it.id == billWithShares.bill.payerId }?.name ?: "Unknown"
                                    Text("Paid by $payerName • ${billWithShares.bill.category}") 
                                },
                                trailingContent = { Text("${"%.2f".format(billWithShares.bill.totalAmount)}", style = MaterialTheme.typography.bodyLarge) }
                            )
                        }
                    }
                }
            }
        }
    }
}
