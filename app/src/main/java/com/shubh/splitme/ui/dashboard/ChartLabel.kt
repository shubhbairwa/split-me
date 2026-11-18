package com.shubh.splitme.ui.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.shubh.splitme.R
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import com.shubh.splitme.ui.preview.ComponentPreviews
import com.shubh.splitme.ui.preview.PreviewComponent
import java.text.DateFormatSymbols
import java.util.Calendar

/**
 * What a chart point stands for. The ViewModel says which hour/day/month it is; the screen turns
 * it into text in the current language (the ViewModel survives a language change, so it can't).
 */
sealed interface ChartLabel {
    data class Hour(val hour: Int) : ChartLabel
    /** [calendarDay] is a java.util.Calendar day constant, e.g. Calendar.MONDAY. */
    data class Weekday(val calendarDay: Int) : ChartLabel
    data class WeekOfMonth(val week: Int) : ChartLabel
    /** [month] is 0-based, as in java.util.Calendar. */
    data class Month(val month: Int) : ChartLabel
}

@Composable
fun ChartLabel.text(): String {
    val locale = LocalConfiguration.current.locales[0]
    return when (this) {
        is ChartLabel.Hour -> String.format(locale, "%02d:00", hour)
        is ChartLabel.Weekday -> DateFormatSymbols.getInstance(locale).shortWeekdays[calendarDay]
        is ChartLabel.WeekOfMonth -> stringResource(R.string.chart_week_of_month, week)
        is ChartLabel.Month -> DateFormatSymbols.getInstance(locale).shortMonths[month]
    }
}

@ComponentPreviews
@Composable
private fun ChartLabelPreview() {
    PreviewComponent {
        Column {
            listOf(
                ChartLabel.Hour(9),
                ChartLabel.Weekday(Calendar.MONDAY),
                ChartLabel.WeekOfMonth(2),
                ChartLabel.Month(Calendar.MARCH)
            ).forEach { Text(it.text()) }
        }
    }
}
