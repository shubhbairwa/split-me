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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shubh.splitme.SplitMeApplication
import com.shubh.splitme.domain.model.Member
import com.shubh.splitme.ui.components.InitialsAvatar
import com.shubh.splitme.ui.theme.CardElevation
import com.shubh.splitme.ui.theme.NegativeRed
import com.shubh.splitme.ui.theme.PositiveGreen
import com.shubh.splitme.ui.theme.TextPrimary
import com.shubh.splitme.ui.theme.TextSecondary
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
                title = { Text("Settle Up", fontWeight = FontWeight.Bold, color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            Text(
                "Net Balances",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            if (balances.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No balances to show.", color = TextSecondary)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(balances) { memberBalance ->
                        val balance = memberBalance.balance
                        val color = when {
                            balance > 0.01 -> PositiveGreen
                            balance < -0.01 -> NegativeRed
                            else -> TextSecondary
                        }
                        val text = when {
                            balance > 0.01 -> "is owed ${"%.2f".format(balance)}"
                            balance < -0.01 -> "owes ${"%.2f".format(abs(balance))}"
                            else -> "is settled up"
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                InitialsAvatar(name = memberBalance.member.name, size = 40)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(memberBalance.member.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                                Text(
                                    "${if (balance > 0) "+" else ""}${"%.2f".format(balance)}",
                                    color = color,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Suggested Settlements",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }

                    val creditors = balances.filter { it.balance > 0.01 }.sortedByDescending { it.balance }
                    val debtors = balances.filter { it.balance < -0.01 }.sortedBy { it.balance }

                    if (debtors.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PositiveGreen, modifier = Modifier.size(48.dp))
                                    Text("Everyone is settled up!", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                                }
                            }
                        }
                    } else {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    debtors.forEach { debtor ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("${debtor.member.name} owes money", color = TextPrimary)
                                            Button(
                                                onClick = {
                                                    if (creditors.isNotEmpty()) {
                                                        showSettleDialog = debtor.member to creditors.first().member
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                            ) {
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
