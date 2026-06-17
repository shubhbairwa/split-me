package com.shubh.splitme.ui.bill

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shubh.splitme.domain.model.ExpenseShare
import com.shubh.splitme.domain.model.Member

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillEntryScreen(
    groupId: String?,
    groupName: String,
    members: List<Member>,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String, List<ExpenseShare>) -> Unit
) {
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
        topBar = {
            TopAppBar(
                title = { Text("Add Bill to $groupName") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                        enabled = title.isNotBlank() && totalAmount > 0 && payer != null && 
                            (!isManualSplit || (totalAmount - manualShares.values.sumOf { it.toDoubleOrNull() ?: 0.0 }).let { kotlin.math.abs(it) < 0.01 })
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amountText = it },
                    label = { Text("Total Amount") },
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
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
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
                            value = payer?.name ?: "Select Payer",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Paid By") },
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
                    Text("Split Type:", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.width(16.dp))
                    FilterChip(
                        selected = !isManualSplit,
                        onClick = { isManualSplit = false },
                        label = { Text("Equal") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = isManualSplit,
                        onClick = { isManualSplit = true },
                        label = { Text("Manual") }
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
                        text = if (isBalanced) "Balanced!" else "Remaining: ${"%.2f".format(remaining)}",
                        color = if (isBalanced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                item {
                    val share = if (members.isNotEmpty()) totalAmount / members.size else 0.0
                    Text("Each person pays: ${"%.2f".format(share)}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
