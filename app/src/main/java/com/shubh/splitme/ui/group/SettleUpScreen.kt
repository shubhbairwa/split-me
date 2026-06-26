package com.shubh.splitme.ui.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shubh.splitme.SplitMeApplication
import com.shubh.splitme.domain.model.Member
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleUpScreen(
    groupId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as SplitMeApplication
    val viewModel: SettleUpViewModel = viewModel(
        key = groupId,
        factory = SettleUpViewModel.Factory(app.billRepository, app.groupRepository, groupId)
    )

    val balances by viewModel.memberBalances.collectAsState()
    var showSettleDialog by remember { mutableStateOf<Pair<Member, Member>?>(null) } // From owes To
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.error.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settle Up") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text(
                "Net Balances",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            if (balances.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No balances to show.")
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(balances) { memberBalance ->
                        val balance = memberBalance.balance
                        val color = when {
                            balance > 0.01 -> Color(0xFF4CAF50) // Positive: Owed money
                            balance < -0.01 -> Color(0xFFF44336) // Negative: Owes money
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val text = when {
                            balance > 0.01 -> "is owed ${"%.2f".format(balance)}"
                            balance < -0.01 -> "owes ${"%.2f".format(abs(balance))}"
                            else -> "is settled up"
                        }

                        ListItem(
                            headlineContent = { Text(memberBalance.member.name, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(text) },
                            trailingContent = {
                                Text(
                                    "${if (balance > 0) "+" else ""}${"%.2f".format(balance)}",
                                    color = color,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Suggested Settlements",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    val creditors = balances.filter { it.balance > 0.01 }.sortedByDescending { it.balance }
                    val debtors = balances.filter { it.balance < -0.01 }.sortedBy { it.balance }

                    if (debtors.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(48.dp))
                                    Text("Everyone is settled up!", style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    } else {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    debtors.forEach { debtor ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("${debtor.member.name} owes money")
                                            Button(onClick = { 
                                                if (creditors.isNotEmpty()) {
                                                    showSettleDialog = debtor.member to creditors.first().member
                                                }
                                            }) {
                                                Icon(Icons.Default.Payment, contentDescription = null)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Settle Up")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showSettleDialog != null) {
            val from = showSettleDialog!!.first
            val to = showSettleDialog!!.second
            val fromBalance = abs(balances.find { it.member.id == from.id }?.balance ?: 0.0)
            val toBalance = balances.find { it.member.id == to.id }?.balance ?: 0.0
            val maxAmount = minOf(fromBalance, toBalance)

            SettleUpDialog(
                from = from,
                to = to,
                suggestedAmount = maxAmount,
                onDismiss = { showSettleDialog = null },
                onConfirm = { amount ->
                    viewModel.settleUp(from.id, to.id, amount)
                    showSettleDialog = null
                }
            )
        }
    }
}

@Composable
fun SettleUpDialog(
    from: Member,
    to: Member,
    suggestedAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf("%.2f".format(suggestedAmount)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment") },
        text = {
            Column {
                Text("${from.name} is paying ${to.name}")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amountText = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { 
                val amount = amountText.toDoubleOrNull() ?: 0.0
                if (amount > 0) onConfirm(amount)
            }) {
                Text("Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
