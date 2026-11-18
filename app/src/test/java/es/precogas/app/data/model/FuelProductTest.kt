package es.precogas.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class FuelProductTest {

    @Test
    fun everyProductHasUniqueApiKey() {
        assertEquals(
            FuelProduct.entries.size,
            FuelProduct.entries.map { it.apiKey }.toSet().size
        )
    }

    @Test
    fun apiKeyResolvesToItsProduct() {
        FuelProduct.entries.forEach { product ->
            assertSame(product, FuelProduct.fromApiKey(product.apiKey))
        }
    }

    @Test
    fun unknownApiKeyReturnsNull() {
        assertNull(FuelProduct.fromApiKey("Precio inexistente"))
    }

    @Test
    fun productsHaveTheirOfficialPriceUnits() {
        val productsPerKilogram = setOf(
            FuelProduct.AMMONIA,
            FuelProduct.BIOGAS_CNG,
            FuelProduct.BIOGAS_LNG,
            FuelProduct.CNG,
            FuelProduct.LNG,
            FuelProduct.HYDROGEN,
            FuelProduct.METHANOL
        )

        assertEquals(
            productsPerKilogram,
            FuelProduct.entries.filter { it.priceUnit == "€/kg" }.toSet()
        )
        assertEquals(
            16,
            FuelProduct.entries.count { it.priceUnit == "€/l" }
        )
    }

    @Test
    fun catalogContainsTwentyThreeProducts() {
        assertEquals(23, FuelProduct.entries.size)
    }
}
