package es.precogas.app.ui.home

import java.math.BigDecimal

object CostCalculator {
    fun parsePositive(value: String): BigDecimal? {
        val normalized = value.trim().replace(',', '.')
        if (normalized.isEmpty()) return null
        val number = normalized.toBigDecimalOrNull() ?: return null
        return number.takeIf { it > BigDecimal.ZERO }
    }

    fun refuelCost(quantity: String, priceMilli: Int): BigDecimal? {
        if (priceMilli <= 0) return null
        val amount = parsePositive(quantity) ?: return null
        return amount.multiply(BigDecimal(priceMilli))
            .divide(BigDecimal(1000))
    }

    fun travelCost(
        distanceKm: Double,
        consumption: String,
        priceMilli: Int,
        roundTrip: Boolean
    ): BigDecimal? {
        if (!distanceKm.isFinite() || distanceKm < 0.0 || priceMilli <= 0) {
            return null
        }

        val consumptionValue = parsePositive(consumption) ?: return null
        var distance = BigDecimal.valueOf(distanceKm)

        if (roundTrip) {
            distance = distance.multiply(BigDecimal(2))
        }

        return distance
            .multiply(consumptionValue)
            .multiply(BigDecimal(priceMilli))
            .divide(BigDecimal(100))
            .divide(BigDecimal(1000))
    }

    fun totalCost(
        refuelCost: BigDecimal?,
        travelCost: BigDecimal?
    ): BigDecimal? {
        if (refuelCost == null || travelCost == null) return null
        return refuelCost.add(travelCost)
    }
}
