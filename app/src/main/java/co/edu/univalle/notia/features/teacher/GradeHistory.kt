package co.edu.univalle.notia.features.teacher

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** RF-19: historial de cambios de notas publicadas, agrupado por día (más reciente primero). */
fun groupHistoryByDay(changes: List<GradeChange>, zone: ZoneId): List<Pair<LocalDate, List<GradeChange>>> =
    changes
        .sortedByDescending { it.timestamp }
        .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
        .toList()

private val MONTHS = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
)
private val WEEKDAYS = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")

/** "HOY · 18 DE NOVIEMBRE", "AYER · 17 DE NOVIEMBRE", "LUNES · 10 DE NOVIEMBRE" (año si no es el actual). */
fun historyDayLabel(date: LocalDate, today: LocalDate): String {
    val prefix = when (date) {
        today -> "hoy"
        today.minusDays(1) -> "ayer"
        else -> WEEKDAYS[date.dayOfWeek.value - 1]
    }
    val year = if (date.year != today.year) " de ${date.year}" else ""
    return "$prefix · ${date.dayOfMonth} de ${MONTHS[date.monthValue - 1]}$year".uppercase()
}

/** Hora como se lee en Colombia: "10:42 a. m.", "4:05 p. m.", "12:00 p. m.". */
fun formatHistoryTime(time: LocalTime): String {
    val hour12 = (time.hour % 12).let { if (it == 0) 12 else it }
    val period = if (time.hour < 12) "a. m." else "p. m."
    return "%d:%02d %s".format(hour12, time.minute, period)
}

/** Nota con un decimal, o "—" si no había (nota agregada) o se quitó. */
fun formatHistoryGrade(value: BigDecimal?): String = value?.setScale(1)?.toPlainString() ?: "—"
