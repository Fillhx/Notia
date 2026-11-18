package co.edu.univalle.notia.core.grades

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Redondeo de notas según el SRS v1.1. Los cálculos de notas deben hacerse en BigDecimal:
 * con Double, 2.95 se guarda como 2.9499… y redondearía mal.
 */

/** Definitiva y definitiva parcial (RF-15): un decimal al más cercano. 2.94 → 2.9, 2.95 → 3.0. */
fun roundGrade(value: BigDecimal): BigDecimal = value.setScale(1, RoundingMode.HALF_UP)

/** Nota necesaria del simulador (RN-05): un decimal hacia arriba. 2.083… → 2.1. */
fun roundUpGrade(value: BigDecimal): BigDecimal = value.setScale(1, RoundingMode.CEILING)

/** Promedios del reporte por materia (RF-17): dos decimales al más cercano. */
fun roundAverage(value: BigDecimal): BigDecimal = value.setScale(2, RoundingMode.HALF_UP)
