package com.shubh.splitme.ui.group

import androidx.compose.ui.res.stringResource
import com.shubh.splitme.R
import com.shubh.splitme.ui.common.categoryLabel
import com.shubh.splitme.ui.theme.tabScreenInsets
import com.shubh.splitme.ui.theme.glassCardColors
import com.shubh.splitme.ui.theme.glassBorder
import com.shubh.splitme.ui.theme.glassShadow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PersonAdd
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupWithMembers: GroupWithMembers,
    onBack: () -> Unit,
    /** Open straight onto Add Contacts (from the group list menu); Back then returns to the list. */
    startWithAddMembers: Boolean = false
) {
    val context = LocalContext.current
    val app = context.applicationContext as SplitMeApplication
    val billViewModel: BillViewModel = viewModel(factory = BillViewModel.Factory(app.billRepository))
    val groupViewModel: GroupViewModel = viewModel(factory = GroupViewModel.Factory(app.authRepository, app.groupRepository, app.billRepository))

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
    var showAddMember by remember { mutableStateOf(startWithAddMembers) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        groupViewModel.error.collect { message ->
            snackbarHostState.showSnackbar(message.resolve(context))
        }
    }
    
    LaunchedEffect(Unit) {
        groupViewModel.info.collect { message ->
            snackbarHostState.showSnackbar(message.resolve(context))
        }
    }

    LaunchedEffect(Unit) {
        billViewModel.error.collect { message ->
            snackbarHostState.showSnackbar(message.resolve(context))
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
                groupViewModel.addContactToGroup(currentGroupWithMembers.group.id, name, email, phone)
            },
            onRemoveMember = { member ->
                if (currentGroupWithMembers.members.size <= 1) {
                    groupViewModel.viewModelScope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.group_min_one_member))
                    }
                } else {
                    groupViewModel.removeMemberFromGroup(currentGroupWithMembers.group.id, member.id)
                }
            },
            onBack = { if (startWithAddMembers) onBack() else showAddMember = false },
            snackbarHostState = snackbarHostState
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
        contentWindowInsets = tabScreenInsets(),
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(currentGroupWithMembers.group.name, fontWeight = FontWeight.Bold, color = TextPrimary) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = TextPrimary)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAddMember = true }) {
                            Icon(Icons.Outlined.PersonAdd, contentDescription = stringResource(R.string.group_manage_members), tint = TextPrimary)
                        }
                        TextButton(onClick = { showSettleUp = true }) {
                            Text(stringResource(R.string.group_settle_up), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
                    Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.group_add_bill))
                }
            }
        ) { padding ->
            val totalSpent = bills.sumOf { it.bill.totalAmount }
            val settlements = remember(balances) { suggestedSettlements(balances) }

            LazyColumn(
                modifier = Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = padding.calculateBottomPadding() + 12.dp)
            ) {
                item {
                    BalanceHeaderCard(
                        label = stringResource(R.string.group_total_spending),
                        amountText = "%.2f".format(totalSpent)
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
                            Text(stringResource(R.string.group_summary), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                            Spacer(modifier = Modifier.height(14.dp))

                            if (balances.isEmpty()) {
                                Text(stringResource(R.string.group_no_members), color = TextSecondary)
                            } else {
                                balances.forEachIndexed { index, memberBalance ->
                                    val balance = memberBalance.balance
                                    val color = when {
                                        balance > 0.01 -> PositiveGreen
                                        balance < -0.01 -> NegativeRed
                                        else -> TextSecondary
                                    }
                                    val statusText = when {
                                        balance > 0.01 -> stringResource(R.string.balance_is_owed, "%.2f".format(balance))
                                        balance < -0.01 -> stringResource(R.string.balance_owes, "%.2f".format(abs(balance)))
                                        else -> stringResource(R.string.balance_settled)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        InitialsAvatar(name = memberBalance.member.name, size = 36, photoUrl = memberBalance.member.photoUrl)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            memberBalance.member.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = TextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            statusText,
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
                                    Text(stringResource(R.string.group_who_owes_whom), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    settlements.forEach { (from, to, amount) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                stringResource(R.string.group_x_owes_y, from.name, to.name),
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
                        stringResource(R.string.group_recent_bills),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }

                if (bills.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.group_no_bills_yet), color = TextSecondary)
                        }
                    }
                } else {
                    items(bills) { billWithShares ->
                        val payerName = currentGroupWithMembers.members.find { it.id == billWithShares.bill.payerId }?.name ?: stringResource(R.string.unknown_person)
                        val style = categoryStyleFor(billWithShares.bill.category)
                        Card(
                            modifier = Modifier.glassShadow(MaterialTheme.shapes.medium).fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = glassCardColors(),
                            border = glassBorder(),
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
                                    Text(if (billWithShares.bill.category == "SettleUp") categoryLabel("SettleUp") else billWithShares.bill.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text(
                                        stringResource(R.string.group_paid_by, payerName),
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
