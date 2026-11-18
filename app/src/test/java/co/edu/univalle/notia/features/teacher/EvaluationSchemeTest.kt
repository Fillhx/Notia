package co.edu.univalle.notia.features.teacher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluationSchemeTest {

    @Test
    fun `porcentaje valido entre 1 y 100`() {
        assertEquals(PercentInput.Valid(25), parsePercent("25"))
        assertEquals(PercentInput.Valid(1), parsePercent("1"))
        assertEquals(PercentInput.Valid(100), parsePercent("100"))
        assertEquals(PercentInput.Empty, parsePercent(""))
    }

    @Test
    fun `porcentaje fuera de rango o no entero es error`() {
        assertTrue(parsePercent("0") is PercentInput.Invalid)
        assertTrue(parsePercent("101") is PercentInput.Invalid)
        assertTrue(parsePercent("2.5") is PercentInput.Invalid)
    }

    @Test
    fun `sanitize deja solo digitos, maximo 3`() {
        assertEquals("25", sanitizePercentText("25 %"))
        assertEquals("100", sanitizePercentText("1000"))
    }

    @Test
    fun `RN-03 suma exacta de 100 es completa`() {
        assertEquals(SchemeTotal.Complete(), schemeTotal(listOf(25, 15, 30, 30)))
    }

    @Test
    fun `ejemplo del diseno - suma 85, faltan 15`() {
        val result = schemeTotal(listOf(25, 15, 30, 15))
        assertTrue(result is SchemeTotal.Missing)
        assertEquals(15, (result as SchemeTotal.Missing).missing)
    }

    @Test
    fun `suma mayor a 100 indica cuanto sobra`() {
        val result = schemeTotal(listOf(30, 15, 30, 30))
        assertTrue(result is SchemeTotal.Exceeds)
        assertEquals(5, (result as SchemeTotal.Exceeds).extra)
    }

    @Test
    fun `esquema vacio no es valido`() {
        assertTrue(schemeTotal(emptyList()) is SchemeTotal.Missing)
    }

    @Test
    fun `nombre obligatorio y sin repetir`() {
        val others = listOf("Parcial 1", "Taller")
        assertNull(validateActivityName("Examen final", others))
        assertEquals("Escribe el nombre de la actividad", validateActivityName("  ", others))
        assertEquals("Ya hay una actividad con ese nombre", validateActivityName("parcial 1 ", others))
    }
}
