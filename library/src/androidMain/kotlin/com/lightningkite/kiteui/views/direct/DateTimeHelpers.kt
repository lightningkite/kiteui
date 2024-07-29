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
        this.updateDate(start.year, start.monthNumber - 1, start.dayOfMonth)
        setOnDateSetListener { _, year, month, dayOfMonth ->
            onResult(LocalDate(year, month + 1, dayOfMonth))
        }
    }.apply {
//        this.datePicker.minDate =
//            min?.atTime(LocalTime(12, 0))?.toInstant(TimeZone.currentSystemDefault())?.toEpochMilliseconds()
//                ?: Long.MIN_VALUE
//        this.datePicker.maxDate =
//            max?.atTime(LocalTime(12, 0))?.toInstant(TimeZone.currentSystemDefault())?.toEpochMilliseconds()
//                ?: Long.MAX_VALUE
        show()
    }
}

fun View.showTimePicker(
    start: LocalTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time,
    min: LocalTime? = null,
    max: LocalTime? = null,
    onResult: (LocalTime) -> Unit
) {
    TimePickerDialog(context, TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
        onResult(LocalTime(hourOfDay, minute))
    }, start.hour, start.minute, false).apply {
        this.updateTime(start.hour, start.minute)
        show()
    }
}
