package es.precogas.app.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.precogas.app.data.local.entity.FuelPriceEntity
import es.precogas.app.data.local.entity.FuelStationEntity
import es.precogas.app.data.local.entity.ProvinceEntity
import es.precogas.app.data.model.FuelProduct
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreCoGasDaoTest {

    private lateinit var database: PreCoGasDatabase
    private lateinit var dao: es.precogas.app.data.local.dao.PreCoGasDao

    private val oldProvince = ProvinceEntity(
        provinceId = "28",
        officialDate = "fecha-anterior",
        downloadedAtMillis = 1000L,
        note = "nota-anterior"
    )

    private val newProvince = ProvinceEntity(
        provinceId = "28",
        officialDate = "fecha-nueva",
        downloadedAtMillis = 2000L,
        note = "nota-nueva"
    )

    private fun station(id: String) = FuelStationEntity(
        stationId = id,
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
        saleType = "P"
    )

    private fun price(stationId: String) = FuelPriceEntity(
        stationId = stationId,
        productKey = FuelProduct.GASOLINE_95_E5.name,
        priceMilli = 1409
    )

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PreCoGasDatabase::class.java
        ).allowMainThreadQueries().build()

        dao = database.preCoGasDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun insertPreviousData() {
        dao.replaceProvinceData(
            province = oldProvince,
            stations = listOf(station("old-station")),
            prices = listOf(price("old-station"))
        )
    }

    @Test
    fun emptyStationListDoesNotDeletePreviousData() = runBlocking {
        insertPreviousData()

        var rejected = false
        try {
            dao.replaceProvinceData(
                province = newProvince,
                stations = emptyList(),
                prices = emptyList()
            )
        } catch (expected: IllegalArgumentException) {
            rejected = true
        }

        assertEquals(true, rejected)
        assertEquals(oldProvince, dao.getProvince("28"))
        assertEquals(1, dao.countStations("28"))

        val stored = dao.observeStationsWithPrices("28").first()
        assertEquals(1, stored.size)
        assertEquals("old-station", stored.single().station.stationId)
        assertEquals(1409, stored.single().prices.single().priceMilli)
    }

    @Test
    fun observesStationsFromMultipleProvinces() = runBlocking {
        insertPreviousData()

        val barcelonaProvince = ProvinceEntity(
            provinceId = "08",
            officialDate = "fecha-barcelona",
            downloadedAtMillis = 3000L,
            note = "nota-barcelona"
        )

        dao.replaceProvinceData(
            province = barcelonaProvince,
            stations = listOf(
                station("barcelona-station").copy(provinceId = "08")
            ),
            prices = listOf(price("barcelona-station"))
        )

        val stored = dao.observeStationsWithPrices(
            listOf("28", "08")
        ).first()

        assertEquals(2, stored.size)
        assertEquals(
            setOf("old-station", "barcelona-station"),
            stored.map { it.station.stationId }.toSet()
        )
        assertEquals(
            1409,
            stored.first { it.station.stationId == "old-station" }
                .prices.single().priceMilli
        )
        assertEquals(
            1409,
            stored.first { it.station.stationId == "barcelona-station" }
                .prices.single().priceMilli
        )
    }

    @Test
    fun failedReplacementRollsBackAndPreservesPreviousData() = runBlocking {
        insertPreviousData()

        var failed = false
        try {
            dao.replaceProvinceData(
                province = newProvince,
                stations = listOf(
                    station("duplicate-station"),
                    station("duplicate-station")
                ),
                prices = emptyList()
            )
        } catch (expected: SQLiteConstraintException) {
            failed = true
        }

        assertEquals(true, failed)

        val storedProvince = dao.getProvince("28")
        assertNotNull(storedProvince)
        assertEquals(oldProvince, storedProvince)
        assertEquals(1, dao.countStations("28"))

        val storedStations = dao.observeStationsWithPrices("28").first()
        assertEquals(1, storedStations.size)
        assertEquals("old-station", storedStations.single().station.stationId)
        assertEquals(1, storedStations.single().prices.size)
        assertEquals(1409, storedStations.single().prices.single().priceMilli)
    }
}
