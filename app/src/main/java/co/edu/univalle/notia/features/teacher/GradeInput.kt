package co.edu.univalle.notia.features.teacher

import java.math.BigDecimal

/** Resultado de validar lo que el docente escribe en una celda de nota (RN-01). */
sealed interface GradeInput {
    data object Empty : GradeInput
    data class Valid(val value: BigDecimal) : GradeInput
    data class Invalid(val message: String) : GradeInput
}

private val MAX_GRADE = BigDecimal("5.0")

/** Acepta coma o punto: "4,5" y "4.5" son 4.5. Escala 0.0–5.0 con un decimal. */
fun parseGrade(text: String): GradeInput {
    val normalized = text.trim().replace(',', '.')
    if (normalized.isEmpty()) return GradeInput.Empty

    val value = normalized.toBigDecimalOrNull()
        ?: return GradeInput.Invalid("Escribe un número, ej. 4.5")
    if (value < BigDecimal.ZERO || value > MAX_GRADE) {
        return GradeInput.Invalid("La nota debe estar entre 0.0 y 5.0")
    }
    if (value.stripTrailingZeros().scale() > 1) {
        return GradeInput.Invalid("Usa un solo decimal, ej. 4.5")
    }
    return GradeInput.Valid(value.setScale(1))
}

/** Solo deja escribir dígitos y un separador decimal, máximo 4 caracteres ("4.55" se marca como error). */
fun sanitizeGradeText(text: String): String =
    text.filter { it.isDigit() || it == '.' || it == ',' }.take(4)
