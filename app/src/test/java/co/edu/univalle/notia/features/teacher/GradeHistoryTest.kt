package co.edu.univalle.notia.features.teacher

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class GradeHistoryTest {

    private val bogota = ZoneId.of("America/Bogota")
    private val today = LocalDate.of(2026, 11, 18)

    private fun change(at: LocalDateTime, student: String = "s1") = GradeChange(
        activityId = "p1",
        studentId = student,
        oldValue = BigDecimal("3.5"),
        newValue = BigDecimal("4.0"),
        author = "Laura Gómez",
        timestamp = at.atZone(bogota).toInstant().toEpochMilli()
    )

    @Test
    fun `etiquetas de dia como en el diseno`() {
        assertEquals("HOY · 18 DE NOVIEMBRE", historyDayLabel(today, today))
        assertEquals("AYER · 17 DE NOVIEMBRE", historyDayLabel(today.minusDays(1), today))
        assertEquals("LUNES · 16 DE NOVIEMBRE", historyDayLabel(LocalDate.of(2026, 11, 16), today))
        assertEquals("MIÉRCOLES · 31 DE DICIEMBRE DE 2025", historyDayLabel(LocalDate.of(2025, 12, 31), today))
    }

    @Test
    fun `hora en formato colombiano, bordes de medianoche y mediodia`() {
        assertEquals("10:42 a. m.", formatHistoryTime(LocalTime.of(10, 42)))
        assertEquals("4:05 p. m.", formatHistoryTime(LocalTime.of(16, 5)))
        assertEquals("12:00 a. m.", formatHistoryTime(LocalTime.MIDNIGHT))
        assertEquals("12:30 p. m.", formatHistoryTime(LocalTime.of(12, 30)))
        assertEquals("11:59 p. m.", formatHistoryTime(LocalTime.of(23, 59)))
    }

    @Test
    fun `agrupa por dia local, mas reciente primero`() {
        val groups = groupHistoryByDay(
            listOf(
                change(LocalDateTime.of(2026, 11, 17, 16, 5), "s3"),
                change(LocalDateTime.of(2026, 11, 18, 9, 15), "s4"),
                change(LocalDateTime.of(2026, 11, 18, 10, 42), "s1"),
                // 23:59 del 17 en Bogotá ya es 18 en UTC: debe quedar el 17.
                change(LocalDateTime.of(2026, 11, 17, 23, 59), "s5"),
            ),
            bogota
        )
        assertEquals(listOf(today, today.minusDays(1)), groups.map { it.first })
        assertEquals(listOf("s1", "s4"), groups[0].second.map { it.studentId })
        assertEquals(listOf("s5", "s3"), groups[1].second.map { it.studentId })
    }

    @Test
    fun `nota agregada o quitada se muestra con raya`() {
        assertEquals("—", formatHistoryGrade(null))
        assertEquals("3.8", formatHistoryGrade(BigDecimal("3.8")))
        assertEquals("4.0", formatHistoryGrade(BigDecimal("4")))
    }
}
