package co.edu.univalle.notia.core.grades

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightRulesTest {

    // --- RN-03: suma de porcentajes ---

    @Test
    fun sum_equal_to_100_is_valid() {
        assertTrue(isValidWeightSum(listOf(25, 15, 30, 30)))
        assertTrue(isValidWeightSum(listOf(100)))
    }

    @Test
    fun sum_below_100_is_invalid() {
        assertFalse(isValidWeightSum(listOf(25, 15, 30, 20)))
    }

    @Test
    fun sum_above_100_is_invalid() {
        assertFalse(isValidWeightSum(listOf(60, 50)))
    }

    @Test
    fun empty_list_is_invalid() {
        assertFalse(isValidWeightSum(emptyList()))
    }

    @Test
    fun total_weight_adds_percentages() {
        assertEquals(70, totalWeight(listOf(25, 15, 30)))
        assertEquals(0, totalWeight(emptyList()))
    }

    @Test
    fun remaining_weight_is_the_missing_part_up_to_100() {
        assertEquals(60, remainingWeight(listOf(25, 15)))
        assertEquals(100, remainingWeight(emptyList()))
        assertEquals(0, remainingWeight(listOf(40, 60)))
    }

    @Test
    fun remaining_weight_is_negative_when_exceeding_100() {
        assertEquals(-10, remainingWeight(listOf(60, 50)))
    }

    // --- GradedItem: casos borde ---

    @Test
    fun graded_item_accepts_boundary_values() {
        GradedItem(1, BigDecimal("0.0"))
        GradedItem(100, BigDecimal("5.0"))
        GradedItem(50, null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun graded_item_rejects_zero_percentage() {
        GradedItem(0, BigDecimal("3.0"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun graded_item_rejects_negative_percentage() {
        GradedItem(-5, null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun graded_item_rejects_percentage_above_100() {
        GradedItem(101, null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun graded_item_rejects_grade_above_5() {
        GradedItem(50, BigDecimal("5.1"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun graded_item_rejects_negative_grade() {
        GradedItem(50, BigDecimal("-0.1"))
    }
}
