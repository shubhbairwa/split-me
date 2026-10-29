package com.shubh.splitme.ui.bill

import androidx.compose.ui.res.stringResource
import com.shubh.splitme.R
import com.shubh.splitme.ui.common.categoryLabel
import com.shubh.splitme.ui.theme.tabScreenInsets
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shubh.splitme.domain.model.ExpenseShare
import com.shubh.splitme.domain.model.Member
import com.shubh.splitme.ui.theme.TextPrimary
import com.shubh.splitme.ui.theme.TextSecondary
import com.shubh.splitme.ui.theme.categoryStyleFor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillEntryScreen(
    groupId: String?,
    groupName: String,
    members: List<Member>,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String, List<ExpenseShare>) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as com.shubh.splitme.SplitMeApplication
    val billViewModel: BillViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = BillViewModel.Factory(app.billRepository))
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        billViewModel.error.collect { message ->
            snackbarHostState.showSnackbar(message.resolve(context))
        }
    }

    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var payer by remember { 
        mutableStateOf<Member?>(
            members.find { it.name.equals("Me", ignoreCase = true) } ?: members.firstOrNull()
        ) 
    }
    var isManualSplit by remember { mutableStateOf(false) }
    
    val manualShares = remember { mutableStateMapOf<String, String>() }
    
    val categories = listOf("General", "Food", "Transport", "Shopping", "Entertainment")
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showPayerMenu by remember { mutableStateOf(false) }

    val totalAmount = amountText.toDoubleOrNull() ?: 0.0

    Scaffold(
        contentWindowInsets = tabScreenInsets(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.bill_title_for_group, groupName), color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val finalShares = if (isManualSplit) {
                                members.map { member ->
                                    ExpenseShare(
                                        billId = "",
                                        memberId = member.id,
                                        amount = manualShares[member.id]?.toDoubleOrNull() ?: 0.0
                                    )
                                }
                            } else {
                                val equalShare = if (members.isNotEmpty()) totalAmount / members.size else 0.0
                                members.map { member ->
                                    ExpenseShare(billId = "", memberId = member.id, amount = equalShare)
                                }
                            }
                            
                            onSave(title, totalAmount, category, payer!!.id, finalShares)
                        },
                        enabled = title.isNotBlank() && totalAmount > 0 && payer != null
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = stringResource(R.string.action_save), tint = MaterialTheme.colorScheme.primary)
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
            contentPadding = PaddingValues(top = 16.dp, bottom = padding.calculateBottomPadding() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.bill_field_title)) },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amountText = it },
                    label = { Text(stringResource(R.string.bill_field_amount)) },
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = showCategoryMenu,
                        onExpandedChange = { showCategoryMenu = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = categoryLabel(category),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.bill_field_category)) },
                            shape = MaterialTheme.shapes.medium,
                            leadingIcon = {
                                val style = categoryStyleFor(category)
                                Icon(style.icon, contentDescription = null, tint = style.badgeColor)
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false }
                        ) {
                            categories.forEach { cat ->
                                val style = categoryStyleFor(cat)
                                DropdownMenuItem(
                                    text = { Text(categoryLabel(cat)) },
                                    leadingIcon = { Icon(style.icon, contentDescription = null, tint = style.badgeColor) },
                                    onClick = {
                                        category = cat
                                        showCategoryMenu = false
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = showPayerMenu,
                        onExpandedChange = { showPayerMenu = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = payer?.name ?: stringResource(R.string.bill_select_payer),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.bill_field_paid_by)) },
                            shape = MaterialTheme.shapes.medium,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPayerMenu) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = showPayerMenu,
                            onDismissRequest = { showPayerMenu = false }
                        ) {
                            members.forEach { member ->
                                DropdownMenuItem(
                                    text = { Text(member.name) },
                                    onClick = {
                                        payer = member
                                        showPayerMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.bill_split_type), style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    Spacer(modifier = Modifier.width(16.dp))
                    FilterChip(
                        selected = !isManualSplit,
                        onClick = { isManualSplit = false },
                        label = { Text(stringResource(R.string.bill_split_equal)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = isManualSplit,
                        onClick = { isManualSplit = true },
                        label = { Text(stringResource(R.string.bill_split_manual)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            if (isManualSplit) {
                items(members) { member ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(member.name, modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            value = manualShares[member.id] ?: "",
                            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) manualShares[member.id] = it },
                            placeholder = { Text("0.0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.width(100.dp)
                        )
                    }
                }
                
                item {
                    val currentTotal = manualShares.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
                    val remaining = totalAmount - currentTotal
                    val isBalanced = kotlin.math.abs(remaining) < 0.01
                    Text(
                        text = if (isBalanced) stringResource(R.string.bill_balanced) else stringResource(R.string.bill_remaining, "%.2f".format(remaining)),
                        color = if (isBalanced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                item {
                    val share = if (members.isNotEmpty()) totalAmount / members.size else 0.0
                    Text(stringResource(R.string.bill_each_pays, "%.2f".format(share)), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }
    }
}
