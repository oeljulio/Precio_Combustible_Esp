package es.precogas.app.ui.home

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CostCalculatorTest {
    @Test
    fun parsePositiveAcceptsDecimalAndComma() {
        assertEquals(
            BigDecimal("12.5"),
            CostCalculator.parsePositive("12,5")
        )
    }

    @Test
    fun parsePositiveRejectsInvalidValues() {
        listOf("", " ", "0", "-2", "abc", "1.2.3").forEach { value ->
            assertNull(CostCalculator.parsePositive(value))
        }
    }

    @Test
    fun refuelCostCalculatesLiters() {
        val result = CostCalculator.refuelCost("50", 1500)
        assertEquals(0, BigDecimal("75").compareTo(result))
    }

    @Test
    fun refuelCostCalculatesKilograms() {
        val result = CostCalculator.refuelCost("5", 2400)
        assertEquals(0, BigDecimal("12").compareTo(result))
    }

    @Test
    fun refuelCostAcceptsDecimalComma() {
        val result = CostCalculator.refuelCost("12,5", 1600)
        assertEquals(0, BigDecimal("20").compareTo(result))
    }

    @Test
    fun refuelCostRejectsInvalidQuantityOrPrice() {
        assertNull(CostCalculator.refuelCost("", 1500))
        assertNull(CostCalculator.refuelCost("0", 1500))
        assertNull(CostCalculator.refuelCost("-5", 1500))
        assertNull(CostCalculator.refuelCost("10", 0))
        assertNull(CostCalculator.refuelCost("10", -100))
    }

    @Test
    fun travelCostCalculatesOutboundDistance() {
        val result = CostCalculator.travelCost(10.0, "10", 1500, false)
        assertEquals(0, BigDecimal("1.5").compareTo(result))
    }

    @Test
    fun travelCostCalculatesRoundTrip() {
        val result = CostCalculator.travelCost(10.0, "10", 1500, true)
        assertEquals(0, BigDecimal("3").compareTo(result))
    }

    @Test
    fun travelCostSupportsKilogramsAndDecimalComma() {
        val result = CostCalculator.travelCost(20.0, "5,0", 2400, false)
        assertEquals(0, BigDecimal("2.4").compareTo(result))
    }

    @Test
    fun travelCostRejectsInvalidValues() {
        assertNull(CostCalculator.travelCost(Double.NaN, "10", 1500, false))
        assertNull(CostCalculator.travelCost(Double.POSITIVE_INFINITY, "10", 1500, false))
        assertNull(CostCalculator.travelCost(-1.0, "10", 1500, false))
        assertNull(CostCalculator.travelCost(10.0, "", 1500, false))
        assertNull(CostCalculator.travelCost(10.0, "0", 1500, false))
        assertNull(CostCalculator.travelCost(10.0, "10", 0, false))
    }

    @Test
    fun totalCostRequiresBothCosts() {
        assertNull(CostCalculator.totalCost(BigDecimal("20"), null))
        assertNull(CostCalculator.totalCost(null, BigDecimal("2")))
        assertEquals(
            0,
            BigDecimal("22").compareTo(
                CostCalculator.totalCost(BigDecimal("20"), BigDecimal("2"))
            )
        )
    }
}
