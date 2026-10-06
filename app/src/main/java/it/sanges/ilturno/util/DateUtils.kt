package it.sanges.ilturno.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object DateUtils {
    val locale: Locale = Locale.ITALIAN
    fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    fun weekEnd(date: LocalDate): LocalDate = weekStart(date).plusDays(6)
    fun weekDays(date: LocalDate): List<LocalDate> = (0L..6L).map { weekStart(date).plusDays(it) }
    fun dayTitle(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("EEEE d", locale))
    fun monthYear(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
    fun fullDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", locale))
    fun shortDay(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("EEE d", locale))
    fun pickerMillis(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    fun pickerDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
    fun weekLabel(date: LocalDate): String {
        val start = weekStart(date)
        val end = weekEnd(date)
        val pattern = when {
            start.year != end.year -> "d MMMM yyyy"
            start.month != end.month -> "d MMMM"
            else -> "d"
        }
        return "${start.format(DateTimeFormatter.ofPattern(pattern, locale))} – ${end.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale))}"
    }
}
