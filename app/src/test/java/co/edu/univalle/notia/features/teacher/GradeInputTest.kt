package co.edu.univalle.notia.features.teacher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class GradeInputTest {

    private fun valid(text: String) = (parseGrade(text) as GradeInput.Valid).value

    @Test
    fun `vacio es nota sin registrar`() {
        assertEquals(GradeInput.Empty, parseGrade(""))
        assertEquals(GradeInput.Empty, parseGrade("  "))
    }

    @Test
    fun `acepta punto o coma y normaliza a un decimal`() {
        assertEquals(BigDecimal("4.5"), valid("4.5"))
        assertEquals(BigDecimal("4.5"), valid("4,5"))
        assertEquals(BigDecimal("4.0"), valid("4"))
        assertEquals(BigDecimal("4.5"), valid("4.50"))
        assertEquals(BigDecimal("0.5"), valid(".5"))
    }

    @Test
    fun `limites de la escala son validos`() {
        assertEquals(BigDecimal("0.0"), valid("0"))
        assertEquals(BigDecimal("5.0"), valid("5.0"))
    }

    @Test
    fun `fuera de rango es error (ejemplo del diseno 5_4)`() {
        val result = parseGrade("5.4")
        assertTrue(result is GradeInput.Invalid)
        assertEquals("La nota debe estar entre 0.0 y 5.0", (result as GradeInput.Invalid).message)
        assertTrue(parseGrade("5.01") is GradeInput.Invalid)
    }

    @Test
    fun `mas de un decimal es error`() {
        assertTrue(parseGrade("4.55") is GradeInput.Invalid)
    }

    @Test
    fun `texto que no es numero es error`() {
        assertTrue(parseGrade("4..5") is GradeInput.Invalid)
        assertTrue(parseGrade(".") is GradeInput.Invalid)
    }

    @Test
    fun `sanitize deja solo digitos y separador`() {
        assertEquals("4.5", sanitizeGradeText("4.5a"))
        assertEquals("4,55", sanitizeGradeText("4,555"))
    }
}
