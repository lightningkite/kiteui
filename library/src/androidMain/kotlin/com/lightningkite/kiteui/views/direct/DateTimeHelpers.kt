package com.lightningkite.kiteui.views.direct

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.view.View
import kotlinx.datetime.*

fun View.showDatePicker(
    start: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
    min: LocalDate? = null,
    max: LocalDate? = null,
    onResult: (LocalDate) -> Unit
) {
    DatePickerDialog(context).apply {
        updateDate(start.year, start.monthNumber - 1, start.dayOfMonth)
        min?.let { datePicker.minDate = it.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds() }
        max?.let { datePicker.maxDate = it.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds() }

        setOnDateSetListener { _, year, month, dayOfMonth ->
            val selected = LocalDate(year, month + 1, dayOfMonth)
            onResult(selected)
        }
        show()
    }
}

fun View.showTimePicker(
    start: LocalTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time,
    min: LocalTime? = null,
    max: LocalTime? = null,
    onResult: (LocalTime) -> Unit
) {
    TimePickerDialog(context, { _, hourOfDay, minute ->
        val selected = LocalTime(hourOfDay, minute)

        if ((min != null && selected < min) || (max != null && selected > max)) {
            return@TimePickerDialog
        }

        onResult(selected)
    }, start.hour, start.minute, false).apply {
        this.updateTime(start.hour, start.minute)
        show()
    }
}
