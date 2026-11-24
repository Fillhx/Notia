package co.edu.univalle.notia.core.grades

import java.math.BigDecimal
import java.math.RoundingMode

val MIN_GRADE: BigDecimal = BigDecimal("0.0")
val MAX_GRADE: BigDecimal = BigDecimal("5.0")
val PASSING_GRADE: BigDecimal = BigDecimal("3.0")

/** Actividad calificable: porcentaje entero y nota (null = aún sin evaluar). */
data class GradedItem(val percentage: Int, val grade: BigDecimal? = null) {
    init {
        require(percentage in 1..TOTAL_WEIGHT) { "percentage fuera de 1..100: $percentage" }
        require(grade == null || grade in MIN_GRADE..MAX_GRADE) { "nota fuera de 0.0..5.0: $grade" }
    }
}

/** Σ(nota × %) sin dividir entre 100 (exacto, en "puntos porcentuales"). */
internal fun weightedPoints(items: List<GradedItem>): BigDecimal =
    items.sumOf { (it.grade ?: BigDecimal.ZERO).multiply(BigDecimal(it.percentage)) }

/** Σ de porcentajes de las actividades que ya tienen nota. */
fun evaluatedWeight(items: List<GradedItem>): Int =
    items.filter { it.grade != null }.sumOf { it.percentage }

/** Σ(nota × %) en escala 0–5, exacto y sin redondear (usado por el simulador). */
fun accumulatedGrade(items: List<GradedItem>): BigDecimal = weightedPoints(items).movePointLeft(2)

/** RN-04/RF-15: Σ(nota × %) ÷ Σ(% evaluado), con [roundGrade]. Null si no hay notas. */
fun partialGrade(items: List<GradedItem>): BigDecimal? {
    val evaluated = evaluatedWeight(items)
    if (evaluated == 0) return null
    return roundGrade(weightedPoints(items).divide(BigDecimal(evaluated), 10, RoundingMode.HALF_UP))
}

/** RN-04/RF-15: Σ(nota × %) con el 100 % evaluado, con [roundGrade]. Null si falta algo. */
fun finalGrade(items: List<GradedItem>): BigDecimal? {
    val complete = isValidWeightSum(items.map { it.percentage }) && evaluatedWeight(items) == TOTAL_WEIGHT
    return if (complete) roundGrade(accumulatedGrade(items)) else null
}
