package com.shubh.splitme.ui.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.shubh.splitme.SplitMeApplication
import com.shubh.splitme.domain.model.BillWithShares
import com.shubh.splitme.domain.model.GroupWithMembers
import com.shubh.splitme.domain.model.Member
import com.shubh.splitme.ui.bill.BillEntryScreen
import com.shubh.splitme.ui.bill.BillViewModel
import com.shubh.splitme.ui.components.BalanceHeaderCard
import com.shubh.splitme.ui.components.InitialsAvatar
import com.shubh.splitme.ui.member.ContactSelectionScreen
import com.shubh.splitme.ui.theme.CardElevation
import com.shubh.splitme.ui.theme.NegativeRed
import com.shubh.splitme.ui.theme.PositiveGreen
import com.shubh.splitme.ui.theme.TextPrimary
import com.shubh.splitme.ui.theme.TextSecondary
import com.shubh.splitme.ui.theme.categoryStyleFor
import kotlin.math.abs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupWithMembers: GroupWithMembers,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as SplitMeApplication
    val billViewModel: BillViewModel = viewModel(factory = BillViewModel.Factory(app.billRepository))
    val groupViewModel: GroupViewModel = viewModel(factory = GroupViewModel.Factory(app.authRepository, app.groupRepository))

    // groupWithMembers is a snapshot from the moment this screen was opened (GroupListScreen
    // passes it in directly, not as a live flow). Re-derive it from the live groups list so
    // member changes (e.g. adding a contact) are reflected here without navigating back and forth.
    val liveGroups by groupViewModel.groupsWithMembers.collectAsState()
    val currentGroupWithMembers = liveGroups.find { it.group.id == groupWithMembers.group.id } ?: groupWithMembers

    val bills by billViewModel.getBillsByGroup(currentGroupWithMembers.group.id).collectAsState()
    val settleUpViewModel: SettleUpViewModel = viewModel(
        key = currentGroupWithMembers.group.id,
        factory = SettleUpViewModel.Factory(app.billRepository, app.groupRepository, currentGroupWithMembers.group.id)
    )
    val balances by settleUpViewModel.memberBalances.collectAsState()
    var showBillEntry by remember { mutableStateOf(false) }
    var showSettleUp by remember { mutableStateOf(false) }
    var showAddMember by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        groupViewModel.error.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    
    LaunchedEffect(Unit) {
        billViewModel.error.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    if (showSettleUp) {
        SettleUpScreen(
            groupId = currentGroupWithMembers.group.id,
            onBack = { showSettleUp = false }
        )
    } else if (showAddMember) {
        ContactSelectionScreen(
            selectedMembers = currentGroupWithMembers.members,
            onAddContact = { name, email, phone ->
                groupViewModel.viewModelScope.launch {
                    try {
                        // Firestore handles member lookups
                        // For now, let's keep it simple: Add by ID if known, or search by phone
                        // This logic should ideally be in GroupViewModel or a UseCase
                        // Simplifying for the refactor:
                        val members = app.memberRepository.getAllMembers().first()
                        val existingMember = members.find {
                            (phone != null && it.phoneNumber == phone) || (email != null && it.email == email)
                        }

                        if (existingMember != null) {
                            groupViewModel.addMemberToGroup(currentGroupWithMembers.group.id, existingMember.id)
                        } else {
                            // Create new member (Simplified, usually invite system)
                            val savedMember = app.memberRepository.saveMember(Member(name = name, email = email, phoneNumber = phone))
                            groupViewModel.addMemberToGroup(currentGroupWithMembers.group.id, savedMember.id)
                        }
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Failed to add member: ${e.message}")
                    }
                }
            },
            onRemoveMember = { member ->
                if (currentGroupWithMembers.members.size <= 1) {
                    groupViewModel.viewModelScope.launch {
                        snackbarHostState.showSnackbar("A group must have at least one member.")
                    }
                } else {
                    groupViewModel.removeMemberFromGroup(currentGroupWithMembers.group.id, member.id)
                }
            },
            onBack = { showAddMember = false }
        )
    } else if (showBillEntry) {
        BillEntryScreen(
            groupId = currentGroupWithMembers.group.id,
            groupName = currentGroupWithMembers.group.name,
            members = currentGroupWithMembers.members,
            onDismiss = { showBillEntry = false },
            onSave = { title, amount, category, payerId, shares ->
                billViewModel.addBill(currentGroupWithMembers.group.id, title, amount, category, payerId, shares)
                showBillEntry = false
            }
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(currentGroupWithMembers.group.name, fontWeight = FontWeight.Bold, color = TextPrimary) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAddMember = true }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Manage Members", tint = TextPrimary)
                        }
                        TextButton(onClick = { showSettleUp = true }) {
                            Text("Settle Up", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showBillEntry = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Bill")
                }
            }
        ) { padding ->
            val totalSpent = bills.sumOf { it.bill.totalAmount }
            val settlements = remember(balances) { suggestedSettlements(balances) }

            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                item {
                    BalanceHeaderCard(
                        label = "Total Group Spending",
                        amountText = "%.2f".format(totalSpent)
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
                            Text("Group Summary", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                            Spacer(modifier = Modifier.height(14.dp))

                            if (balances.isEmpty()) {
                                Text("No members yet.", color = TextSecondary)
                            } else {
                                balances.forEachIndexed { index, memberBalance ->
                                    val balance = memberBalance.balance
                                    val color = when {
                                        balance > 0.01 -> PositiveGreen
                                        balance < -0.01 -> NegativeRed
                                        else -> TextSecondary
                                    }
                                    val statusText = when {
                                        balance > 0.01 -> "is owed"
                                        balance < -0.01 -> "owes"
                                        else -> "is settled up"
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        InitialsAvatar(name = memberBalance.member.name, size = 36)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            memberBalance.member.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = TextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            if (abs(balance) > 0.01) "$statusText ${"%.2f".format(abs(balance))}" else statusText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = color,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (index != balances.lastIndex) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                }

                                if (settlements.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Who Owes Whom", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    settlements.forEach { (from, to, amount) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "${from.name} owes ${to.name}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextPrimary,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                "%.2f".format(amount),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = NegativeRed,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        "Recent Bills",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }

                if (bills.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No bills yet.", color = TextSecondary)
                        }
                    }
                } else {
                    items(bills) { billWithShares ->
                        val payerName = currentGroupWithMembers.members.find { it.id == billWithShares.bill.payerId }?.name ?: "Unknown"
                        val style = categoryStyleFor(billWithShares.bill.category)
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
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(style.badgeColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(style.icon, contentDescription = null, tint = style.badgeColor)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(billWithShares.bill.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text(
                                        "Paid by $payerName",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                                Text(
                                    "%.2f".format(billWithShares.bill.totalAmount),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Greedy debt-settlement matching: pairs the largest creditor with the largest debtor until everyone nets to zero. */
private fun suggestedSettlements(balances: List<MemberBalance>): List<Triple<Member, Member, Double>> {
    val creditors = balances.filter { it.balance > 0.01 }
        .sortedByDescending { it.balance }
        .map { it.member to it.balance }
        .toMutableList()
    val debtors = balances.filter { it.balance < -0.01 }
        .sortedBy { it.balance }
        .map { it.member to -it.balance }
        .toMutableList()

    val settlements = mutableListOf<Triple<Member, Member, Double>>()
    var ci = 0
    var di = 0
    while (ci < creditors.size && di < debtors.size) {
        val (creditor, creditAmount) = creditors[ci]
        val (debtor, debtAmount) = debtors[di]
        val amount = minOf(creditAmount, debtAmount)
        settlements.add(Triple(debtor, creditor, amount))
        creditors[ci] = creditor to (creditAmount - amount)
        debtors[di] = debtor to (debtAmount - amount)
        if (creditors[ci].second < 0.01) ci++
        if (debtors[di].second < 0.01) di++
    }
    return settlements
}
