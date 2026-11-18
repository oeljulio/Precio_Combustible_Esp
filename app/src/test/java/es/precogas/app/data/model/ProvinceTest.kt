package es.precogas.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ProvinceTest {

    @Test
    fun catalogContainsFiftyTwoProvinces() {
        assertEquals(52, ProvinceCatalog.all.size)
    }

    @Test
    fun provinceIdsAreUnique() {
        assertEquals(
            ProvinceCatalog.all.size,
            ProvinceCatalog.all.map { it.id }.toSet().size
        )
    }

    @Test
    fun provinceIdsHaveTwoDigits() {
        ProvinceCatalog.all.forEach { province ->
            assertEquals(2, province.id.length)
            assertEquals(true, province.id.all { it.isDigit() })
        }
    }

    @Test
    fun findsKnownProvince() {
        val madrid = ProvinceCatalog.findById("28")
        assertNotNull(madrid)
        assertEquals("Madrid", madrid?.name)
    }

    @Test
    fun unknownProvinceReturnsNull() {
        assertNull(ProvinceCatalog.findById("99"))
    }
}
