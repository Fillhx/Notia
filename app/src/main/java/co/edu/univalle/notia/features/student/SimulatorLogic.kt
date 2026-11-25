package co.edu.univalle.notia.features.student

import co.edu.univalle.notia.core.grades.MAX_GRADE
import co.edu.univalle.notia.core.grades.MIN_GRADE
import co.edu.univalle.notia.core.grades.PASSING_GRADE
import java.math.BigDecimal

/** Entradas del simulador (RF-16); se guardan como texto para poder validar lo que escribe el usuario. */
data class SimulatorInputs(
    val subjectId: String? = null,
    /** Nota hipotética en texto, por id de actividad. */
    val grades: Map<String, String> = emptyMap(),
    val goal: String = PASSING_GRADE.toPlainString(),
) : java.io.Serializable {

    fun withGrade(activityId: String, text: String): SimulatorInputs =
        copy(grades = grades + (activityId to text))

    /** Restablece notas hipotéticas y meta; conserva la materia elegida. */
    fun reset(): SimulatorInputs = copy(grades = emptyMap(), goal = PASSING_GRADE.toPlainString())
}

internal enum class GradeInputError(val message: String) {
    NOT_NUMERIC("Ingresa un número, por ejemplo 3.5."),
    OUT_OF_RANGE("El valor debe estar entre 0.0 y 5.0."),
    TOO_MANY_DECIMALS("Usa máximo un decimal."),
}

internal sealed interface GradeInput {
    object Empty : GradeInput
    data class Valid(val value: BigDecimal) : GradeInput
    data class Invalid(val error: GradeInputError) : GradeInput
}

/** Valida una nota o meta escrita por el usuario: número en 0.0..5.0 con máximo un decimal. Acepta coma o punto. */
internal fun parseGradeInput(text: String): GradeInput {
    val clean = text.trim().replace(',', '.')
    if (clean.isEmpty()) return GradeInput.Empty
    val value = clean.toBigDecimalOrNull() ?: return GradeInput.Invalid(GradeInputError.NOT_NUMERIC)
    if (value < MIN_GRADE || value > MAX_GRADE) return GradeInput.Invalid(GradeInputError.OUT_OF_RANGE)
    if (value.stripTrailingZeros().scale() > 1) return GradeInput.Invalid(GradeInputError.TOO_MANY_DECIMALS)
    return GradeInput.Valid(value.setScale(1))
}

/**
 * Copia de la materia con notas hipotéticas aplicadas SOLO a actividades publicadas sin nota.
 * Las notas reales no se tocan y [StudentSubjectGrades.hiddenWeight] sigue contando como pendiente.
 */
internal fun StudentSubjectGrades.withSimulated(simulated: Map<String, BigDecimal>): StudentSubjectGrades =
    copy(
        activities = activities.map { activity ->
            val hypothetical = simulated[activity.id]
            if (activity.grade == null && hypothetical != null) activity.copy(grade = hypothetical) else activity
        },
    )
