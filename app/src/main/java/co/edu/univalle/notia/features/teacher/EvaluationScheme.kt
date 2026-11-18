package co.edu.univalle.notia.features.teacher

/** Resultado de validar el porcentaje escrito para una actividad (RF-10). */
sealed interface PercentInput {
    data object Empty : PercentInput
    data class Valid(val value: Int) : PercentInput
    data class Invalid(val message: String) : PercentInput
}

/** Porcentajes enteros de 1 a 100. */
fun parsePercent(text: String): PercentInput {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return PercentInput.Empty
    val value = trimmed.toIntOrNull() ?: return PercentInput.Invalid("Escribe un número entero")
    if (value !in 1..100) return PercentInput.Invalid("Debe estar entre 1 y 100")
    return PercentInput.Valid(value)
}

fun sanitizePercentText(text: String): String = text.filter { it.isDigit() }.take(3)

/** Estado de la suma del esquema frente a RN-03 (exactamente 100 %). */
sealed interface SchemeTotal {
    val total: Int

    data class Complete(override val total: Int = 100) : SchemeTotal
    data class Missing(override val total: Int) : SchemeTotal {
        val missing: Int get() = 100 - total
    }
    data class Exceeds(override val total: Int) : SchemeTotal {
        val extra: Int get() = total - 100
    }
}

fun schemeTotal(percentages: List<Int>): SchemeTotal {
    val total = percentages.sum()
    return when {
        total == 100 -> SchemeTotal.Complete()
        total < 100 -> SchemeTotal.Missing(total)
        else -> SchemeTotal.Exceeds(total)
    }
}

/** Nombre obligatorio y sin repetir dentro de la materia (sin importar mayúsculas). */
fun validateActivityName(name: String, otherNames: List<String>): String? {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "Escribe el nombre de la actividad"
    if (otherNames.any { it.trim().equals(trimmed, ignoreCase = true) }) {
        return "Ya hay una actividad con ese nombre"
    }
    return null
}
