package co.edu.univalle.notia.core.grades

import java.math.BigDecimal
import java.math.RoundingMode

/** Resultado de RN-05 para el simulador. */
sealed interface RequiredGrade {
    /** Nota promedio necesaria en lo pendiente (ya con [roundUpGrade]). */
    data class Needed(val grade: BigDecimal) : RequiredGrade

    /** La meta ya está asegurada con lo acumulado. */
    object Achieved : RequiredGrade

    /** La nota necesaria supera 5.0: "fuera de alcance". */
    object OutOfReach : RequiredGrade

    /** No hay actividades pendientes que simular. */
    object NoPending : RequiredGrade
}

/** RN-05: (meta − acumulado) ÷ % pendiente, redondeado hacia arriba a un decimal. */
fun requiredGrade(items: List<GradedItem>, goal: BigDecimal = PASSING_GRADE): RequiredGrade {
    val pending = items.filter { it.grade == null }.sumOf { it.percentage }
    if (pending == 0) return RequiredGrade.NoPending

    val missing = goal.multiply(BigDecimal(TOTAL_WEIGHT)).subtract(weightedPoints(items))
    if (missing.signum() <= 0) return RequiredGrade.Achieved

    // CEILING también en la división intermedia para no perder un 0.1 al redondear dos veces.
    val needed = roundUpGrade(missing.divide(BigDecimal(pending), 10, RoundingMode.CEILING))
    return if (needed > MAX_GRADE) RequiredGrade.OutOfReach else RequiredGrade.Needed(needed)
}
