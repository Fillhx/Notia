package co.edu.univalle.notia.core.grades

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class GradeRoundingTest {

    private fun bd(s: String) = BigDecimal(s)

    @Test
    fun `definitiva redondea al decimal mas cercano (RF-15)`() {
        assertEquals(bd("2.9"), roundGrade(bd("2.94")))
        assertEquals(bd("3.0"), roundGrade(bd("2.95")))
        assertEquals(bd("5.0"), roundGrade(bd("4.99")))
    }

    @Test
    fun `2_95 calculado con BigDecimal no se pierde por punto flotante`() {
        // 2.95 como Double es 2.9499999…; con BigDecimal la suma ponderada es exacta.
        val ponderada = bd("3.5").multiply(bd("0.5")).add(bd("2.4").multiply(bd("0.5")))
        assertEquals(bd("2.95"), ponderada.stripTrailingZeros())
        assertEquals(bd("3.0"), roundGrade(ponderada))
    }

    @Test
    fun `nota necesaria redondea hacia arriba (ejemplo RN-05)`() {
        val acumulado = bd("3.5").multiply(bd("0.25"))
            .add(bd("4.2").multiply(bd("0.15")))
            .add(bd("2.9").multiply(bd("0.30")))
        assertEquals(0, bd("2.375").compareTo(acumulado))

        val necesaria = bd("3.0").subtract(acumulado).divide(bd("0.30"), 10, RoundingMode.HALF_UP)
        assertEquals(bd("2.1"), roundUpGrade(necesaria))
    }

    @Test
    fun `promedios del reporte con dos decimales (ejemplo RF-17)`() {
        val definitivas = bd("3.4").add(bd("2.6")).divide(bd("2"), 10, RoundingMode.HALF_UP)
        val parciales = bd("3.4").add(bd("2.6")).add(bd("4.0")).divide(bd("3"), 10, RoundingMode.HALF_UP)
        assertEquals(bd("3.00"), roundAverage(definitivas))
        assertEquals(bd("3.33"), roundAverage(parciales))
    }
}
