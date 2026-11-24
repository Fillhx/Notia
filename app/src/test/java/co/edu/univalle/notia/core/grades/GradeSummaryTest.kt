package co.edu.univalle.notia.core.grades

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private fun item(percentage: Int, grade: String? = null) =
    GradedItem(percentage, grade?.let { BigDecimal(it) })

class GradeSummaryTest {

    // --- classifySubject ---

    @Test
    fun no_grades_when_list_is_empty_or_all_pending() {
        assertEquals(SubjectStatus.NO_GRADES, classifySubject(emptyList()))
        assertEquals(SubjectStatus.NO_GRADES, classifySubject(listOf(item(50), item(50))))
    }

    @Test
    fun in_progress_when_graded_but_not_100_percent_evaluated() {
        assertEquals(SubjectStatus.IN_PROGRESS, classifySubject(listOf(item(50, "1.0"), item(50))))
    }

    @Test
    fun in_progress_even_if_partial_is_already_above_passing() {
        assertEquals(SubjectStatus.IN_PROGRESS, classifySubject(listOf(item(50, "5.0"), item(50))))
    }

    @Test
    fun passed_when_final_equals_passing_after_rounding() {
        // 2.95 -> 3.0 -> aprobado
        val items = listOf(item(50, "3.5"), item(50, "2.4"))
        assertEquals(SubjectStatus.PASSED, classifySubject(items))
    }

    @Test
    fun failed_when_final_is_below_passing() {
        // 2.94 -> 2.9 -> reprobado
        val items = listOf(item(40, "3.0"), item(60, "2.9"))
        assertEquals(SubjectStatus.FAILED, classifySubject(items))
    }

    @Test
    fun classification_uses_custom_passing_grade() {
        val items = listOf(item(100, "3.2"))
        assertEquals(SubjectStatus.PASSED, classifySubject(items))
        assertEquals(SubjectStatus.FAILED, classifySubject(items, BigDecimal("3.5")))
    }

    // --- buildGradeReport ---

    @Test
    fun report_counts_every_category() {
        val report = buildGradeReport(sampleSubjects())
        val expected = mapOf(
            SubjectStatus.PASSED to 2,
            SubjectStatus.FAILED to 1,
            SubjectStatus.IN_PROGRESS to 1,
            SubjectStatus.NO_GRADES to 1,
        )
        assertEquals(expected, report.counts)
    }

    @Test
    fun report_partial_average_includes_closed_and_in_progress_subjects() {
        // parciales: 3.0, 4.0, 2.0, 4.0 -> 13 / 4 = 3.25
        assertEquals(BigDecimal("3.25"), buildGradeReport(sampleSubjects()).partialAverage)
    }

    @Test
    fun report_final_average_uses_only_closed_subjects() {
        // definitivas: 3.0, 4.0, 2.0 -> 9 / 3 = 3.00
        assertEquals(BigDecimal("3.00"), buildGradeReport(sampleSubjects()).finalAverage)
    }

    @Test
    fun report_average_rounds_to_two_decimals_down() {
        // (3.0 + 3.0 + 4.0) / 3 = 3.333... -> 3.33
        val subjects = listOf(
            listOf(item(100, "3.0")),
            listOf(item(100, "3.0")),
            listOf(item(100, "4.0")),
        )
        val report = buildGradeReport(subjects)
        assertEquals(BigDecimal("3.33"), report.finalAverage)
        assertEquals(BigDecimal("3.33"), report.partialAverage)
    }

    @Test
    fun report_average_rounds_to_two_decimals_up() {
        // (4.0 + 4.0 + 3.0) / 3 = 3.666... -> 3.67
        val subjects = listOf(
            listOf(item(100, "4.0")),
            listOf(item(100, "4.0")),
            listOf(item(100, "3.0")),
        )
        assertEquals(BigDecimal("3.67"), buildGradeReport(subjects).finalAverage)
    }

    @Test
    fun report_final_average_is_null_when_no_subject_is_closed() {
        val report = buildGradeReport(listOf(listOf(item(50, "4.0"), item(50))))
        assertNull(report.finalAverage)
        assertEquals(BigDecimal("4.00"), report.partialAverage)
    }

    @Test
    fun report_of_empty_list_has_zero_counts_and_null_averages() {
        val report = buildGradeReport(emptyList())
        assertEquals(SubjectStatus.values().associateWith { 0 }, report.counts)
        assertNull(report.partialAverage)
        assertNull(report.finalAverage)
    }

    @Test
    fun report_ignores_subjects_without_grades_in_averages() {
        val report = buildGradeReport(listOf(listOf(item(100)), listOf(item(100, "3.0"))))
        assertEquals(BigDecimal("3.00"), report.partialAverage)
        assertEquals(BigDecimal("3.00"), report.finalAverage)
    }

    private fun sampleSubjects(): List<List<GradedItem>> = listOf(
        listOf(item(100, "3.0")),                    // PASSED, definitiva 3.0
        listOf(item(50, "4.0"), item(50, "4.0")),    // PASSED, definitiva 4.0
        listOf(item(100, "2.0")),                    // FAILED, definitiva 2.0
        listOf(item(50, "4.0"), item(50)),           // IN_PROGRESS, parcial 4.0
        listOf(item(50), item(50)),                  // NO_GRADES
    )
}
