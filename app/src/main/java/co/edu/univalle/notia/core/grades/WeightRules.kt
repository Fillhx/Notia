package co.edu.univalle.notia.core.grades

/** Suma obligatoria de los porcentajes de las actividades de una materia (RN-03). */
const val TOTAL_WEIGHT = 100

fun totalWeight(percentages: List<Int>): Int = percentages.sum()

/** Porcentaje que aún se puede asignar a nuevas actividades. */
fun remainingWeight(percentages: List<Int>): Int = TOTAL_WEIGHT - totalWeight(percentages)

/** RN-03: los porcentajes deben sumar exactamente 100 %. */
fun isValidWeightSum(percentages: List<Int>): Boolean = totalWeight(percentages) == TOTAL_WEIGHT
