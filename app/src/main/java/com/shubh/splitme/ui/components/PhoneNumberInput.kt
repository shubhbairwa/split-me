package com.shubh.splitme.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shubh.splitme.R
import com.shubh.splitme.data.Country
import com.shubh.splitme.data.PhoneNumbers
import com.shubh.splitme.ui.theme.AccentOrange
import com.shubh.splitme.ui.theme.TextPrimary
import com.shubh.splitme.ui.theme.TextSecondary
import com.shubh.splitme.ui.theme.glass

private val FieldShape = RoundedCornerShape(16.dp)

/**
 * Mobile number input: a country button (flag + dial code) that opens a searchable country list,
 * and the national number. [error] is a string resource id shown under the field.
 */
@Composable
fun PhoneNumberInput(
    country: Country,
    onCountryChange: (Country) -> Unit,
    number: String,
    onNumberChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: Int? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {}
) {
    var showPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedCard(
                onClick = { showPicker = true },
                enabled = enabled,
                shape = FieldShape,
                colors = CardDefaults.outlinedCardColors(containerColor = glass.surface),
                border = CardDefaults.outlinedCardBorder(enabled).copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
                    )
                ),
                modifier = Modifier.height(56.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxHeight().padding(start = 12.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(country.flag, fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+${country.dialCode}", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Icon(
                        Icons.Outlined.ArrowDropDown,
                        contentDescription = stringResource(R.string.phone_choose_country),
                        tint = TextSecondary
                    )
                }
            }

            OutlinedTextField(
                value = number,
                onValueChange = { new -> onNumberChange(new.filter { it.isDigit() || it in " -()" }.take(20)) },
                modifier = Modifier.weight(1f),
                enabled = enabled,
                singleLine = true,
                isError = error != null,
                shape = FieldShape,
                placeholder = { Text(PhoneNumbers.exampleNumber(country), color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = imeAction),
                keyboardActions = KeyboardActions(onAny = { onImeAction() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = glass.surface,
                    unfocusedContainerColor = glass.surface,
                    errorContainerColor = glass.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = AccentOrange,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        }
        if (error != null) {
            Text(
                stringResource(error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }

    if (showPicker) {
        CountryPickerDialog(
            onDismiss = { showPicker = false },
            onPick = {
                onCountryChange(it)
                showPicker = false
            }
        )
    }
}

/** Full-screen, searchable country list (names in the app's current language). */
@Composable
fun CountryPickerDialog(onDismiss: () -> Unit, onPick: (Country) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val all = remember(locale) { PhoneNumbers.countries(locale) }
    var query by remember { mutableStateOf("") }
    val filtered = remember(all, query) {
        val q = query.trim().removePrefix("+")
        if (q.isEmpty()) all
        else all.filter {
            it.displayName(locale).contains(q, ignoreCase = true) ||
                it.iso.equals(q, ignoreCase = true) ||
                it.dialCode.toString().startsWith(q)
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.phone_choose_country),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                    shape = FieldShape,
                    placeholder = { Text(stringResource(R.string.phone_search_country)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) }
                )
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered, key = { it.iso }) { country ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(country) }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(country.flag, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                country.displayName(locale),
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Text("+${country.dialCode}", color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}
