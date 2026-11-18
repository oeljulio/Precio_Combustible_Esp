package es.precogas.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FuelStationTest {

    private fun station(prices: Map<FuelProduct, Int>) = FuelStation(
        id = "station-1",
        provinceId = "28",
        postalCode = "28001",
        address = "Calle de prueba",
        hours = "24H",
        locality = "Madrid",
        latitude = 40.4,
        longitude = -3.7,
        margin = "",
        municipality = "Madrid",
        provinceName = "Madrid",
        brand = "Prueba",
        saleType = "P",
        prices = prices
    )

    @Test
    fun exposesOnlyProductsWithAvailablePrices() {
        val fuelStation = station(
            mapOf(
                FuelProduct.DIESEL_A to 1500,
                FuelProduct.GASOLINE_95_E5 to 1400
            )
        )

        assertEquals(
            setOf(FuelProduct.DIESEL_A, FuelProduct.GASOLINE_95_E5),
            fuelStation.availableProducts.toSet()
        )
        assertEquals(1500, fuelStation.priceFor(FuelProduct.DIESEL_A))
        assertNull(fuelStation.priceFor(FuelProduct.LPG))
    }

    @Test
    fun stationWithoutPricesHasNoAvailableProducts() {
        val fuelStation = station(emptyMap())

        assertEquals(emptyList<FuelProduct>(), fuelStation.availableProducts)
        assertNull(fuelStation.priceFor(FuelProduct.DIESEL_A))
    }
}
