package co.edu.univalle.notia.core.grades

import java.math.BigDecimal
import java.math.RoundingMode

enum class SubjectStatus { PASSED, FAILED, IN_PROGRESS, NO_GRADES }

/** RF-17: Sin notas / En curso (parcial) / Aprobado o Reprobado (100 % evaluado). */
fun classifySubject(items: List<GradedItem>, passing: BigDecimal = PASSING_GRADE): SubjectStatus {
    if (evaluatedWeight(items) == 0) return SubjectStatus.NO_GRADES
    val final = finalGrade(items) ?: return SubjectStatus.IN_PROGRESS
    return if (final >= passing) SubjectStatus.PASSED else SubjectStatus.FAILED
}

/**
 * Reporte de RF-17.
 * [partialAverage]: promedio de las definitivas parciales de las materias con notas.
 * [finalAverage]: promedio de las definitivas de las materias ya cerradas.
 * Ambos con [roundAverage]; null si no hay materias para promediar.
 */
data class GradeReport(
    val counts: Map<SubjectStatus, Int>,
    val partialAverage: BigDecimal?,
    val finalAverage: BigDecimal?,
)

fun buildGradeReport(
    subjects: List<List<GradedItem>>,
    passing: BigDecimal = PASSING_GRADE,
): GradeReport {
    val counts = SubjectStatus.values().associateWith { status ->
        subjects.count { classifySubject(it, passing) == status }
    }
    return GradeReport(
        counts = counts,
        partialAverage = average(subjects.mapNotNull { partialGrade(it) }),
        finalAverage = average(subjects.mapNotNull { finalGrade(it) }),
    )
}

private fun average(values: List<BigDecimal>): BigDecimal? {
    if (values.isEmpty()) return null
    val sum = values.fold(BigDecimal.ZERO) { acc, v -> acc.add(v) }
    return roundAverage(sum.divide(BigDecimal(values.size), 10, RoundingMode.HALF_UP))
}
