package com.shubh.splitme.ui.common

import android.content.Context
import androidx.annotation.StringRes
import com.shubh.splitme.R

/**
 * A user-facing message from a ViewModel. ViewModels don't hold a Context (and outlive a language
 * change), so they send the string resource + arguments and the screen turns it into text in the
 * current language.
 */
sealed interface UiText {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    /** "<what failed>: <technical reason>", e.g. "Failed to save profile: network unavailable". */
    data class Failure(@StringRes val id: Int, val reason: String?, val args: List<Any> = emptyList()) : UiText

    fun resolve(context: Context): String = when (this) {
        is Res -> context.getString(id, *args.toTypedArray())
        is Failure -> {
            val what = context.getString(id, *args.toTypedArray())
            if (reason.isNullOrBlank()) what else context.getString(R.string.error_with_reason, what, reason)
        }
    }

    companion object {
        fun of(@StringRes id: Int, vararg args: Any): UiText = Res(id, args.toList())
        fun failure(@StringRes id: Int, error: Throwable, vararg args: Any): UiText = Failure(id, error.message, args.toList())
    }
}
