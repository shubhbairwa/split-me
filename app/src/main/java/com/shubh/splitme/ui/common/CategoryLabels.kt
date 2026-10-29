package com.shubh.splitme.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.shubh.splitme.R

/** Bill categories are stored in Firestore as English keys; this gives the label in the current language. */
@Composable
fun categoryLabel(key: String?): String = stringResource(
    when (key?.ifBlank { null } ?: "General") {
        "Food" -> R.string.category_food
        "Transport" -> R.string.category_transport
        "Shopping" -> R.string.category_shopping
        "Entertainment" -> R.string.category_entertainment
        "SettleUp" -> R.string.category_settle_up
        else -> R.string.category_general
    }
)
