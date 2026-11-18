package es.precogas.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.precogas.app.data.local.PreCoGasDatabase
import es.precogas.app.data.model.FuelProduct
import es.precogas.app.data.model.FuelStation
import es.precogas.app.data.model.MitecoProvinceData
import es.precogas.app.data.model.Province
import es.precogas.app.data.model.ProvinceCatalog
import es.precogas.app.data.remote.MitecoDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FuelRepositoryTest {

    private lateinit var database: PreCoGasDatabase
    private lateinit var repository: FuelRepository
    private lateinit var dataSource: FakeMitecoDataSource

    private val province: Province =
        requireNotNull(ProvinceCatalog.findById("28"))

    private val oldResponse = MitecoProvinceData(
        provinceId = "28",
        officialDate = "fecha-anterior",
        downloadedAtMillis = 1000L,
        stations = listOf(
            station(
                "old-station",
                "Dirección anterior",
                mapOf(FuelProduct.GASOLINE_95_E5 to 1409)
            )
        ),
        note = "nota-anterior"
    )

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PreCoGasDatabase::class.java
        ).allowMainThreadQueries().build()

        dataSource = FakeMitecoDataSource(oldResponse)
        repository = FuelRepository(database.preCoGasDao(), dataSource)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun station(
        id: String,
        address: String,
        prices: Map<FuelProduct, Int>,
        provinceId: String = "28"
    ) = FuelStation(
        id = id,
        provinceId = provinceId,
        postalCode = "28001",
        address = address,
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

    private fun response(
        date: String,
        downloadedAt: Long,
        stations: List<FuelStation>,
        note: String,
        provinceId: String = "28"
    ) = MitecoProvinceData(
        provinceId = provinceId,
        officialDate = date,
        downloadedAtMillis = downloadedAt,
        stations = stations,
        note = note
    )

    private suspend fun assertOldDataPreserved() {
        val storedProvince = database.preCoGasDao().getProvince("28")

        assertEquals("fecha-anterior", storedProvince?.officialDate)
        assertEquals(1000L, storedProvince?.downloadedAtMillis)
        assertEquals("nota-anterior", storedProvince?.note)

        val storedStations = repository.observeStations("28").first()

        assertEquals(1, storedStations.size)
        assertEquals("old-station", storedStations.single().id)
        assertEquals(
            1409,
            storedStations.single().priceFor(FuelProduct.GASOLINE_95_E5) ?: -1
        )
    }

    @Test
    fun validResponseUpdatesProvinceStationsAndPrices() = runBlocking {
        val newResponse = response(
            "fecha-nueva",
            2000L,
            listOf(
                station(
                    "priced-station",
                    "Calle con precio",
                    mapOf(
                        FuelProduct.GASOLINE_95_E5 to 1459,
                        FuelProduct.DIESEL_A to 1559
                    )
                ),
                station("without-price", "Calle sin precio", emptyMap())
            ),
            "nota-nueva"
        )
        dataSource.response = newResponse

        val result = repository.refreshProvince(province)

        assertEquals(newResponse, result)

        val storedProvince = database.preCoGasDao().getProvince("28")
        assertEquals("fecha-nueva", storedProvince?.officialDate)
        assertEquals(2000L, storedProvince?.downloadedAtMillis)
        assertEquals("nota-nueva", storedProvince?.note)

        val storedStations = repository.observeStations("28").first()
        assertEquals(2, storedStations.size)

        val pricedStation = requireNotNull(
            storedStations.find { it.id == "priced-station" }
        )
        assertEquals(
            1459,
            pricedStation.priceFor(FuelProduct.GASOLINE_95_E5) ?: -1
        )
        assertEquals(
            1559,
            pricedStation.priceFor(FuelProduct.DIESEL_A) ?: -1
        )

        val stationWithoutPrice = requireNotNull(
            storedStations.find { it.id == "without-price" }
        )
        assertTrue(stationWithoutPrice.availableProducts.isEmpty())
    }

    @Test
    fun observesStationsFromMultipleProvincesAndHandlesEmptyList() = runBlocking {
        repository.refreshProvince(province)

        val barcelona = requireNotNull(ProvinceCatalog.findById("08"))
        dataSource.response = response(
            date = "fecha-barcelona",
            downloadedAt = 3000L,
            stations = listOf(
                station(
                    "barcelona-station",
                    "Dirección Barcelona",
                    mapOf(FuelProduct.GASOLINE_95_E5 to 1509),
                    provinceId = "08"
                )
            ),
            note = "nota-barcelona",
            provinceId = "08"
        )
        repository.refreshProvince(barcelona)

        val combined = repository.observeStations(listOf("28", "08")).first()

        assertEquals(2, combined.size)
        assertEquals(
            setOf("old-station", "barcelona-station"),
            combined.map { it.id }.toSet()
        )
        assertEquals(
            1409,
            combined.first { it.id == "old-station" }
                .priceFor(FuelProduct.GASOLINE_95_E5)
        )
        assertEquals(
            1509,
            combined.first { it.id == "barcelona-station" }
                .priceFor(FuelProduct.GASOLINE_95_E5)
        )

        assertTrue(repository.observeStations(emptyList()).first().isEmpty())
    }

    @Test
    fun downloadFailurePreservesPreviousData() = runBlocking {
        repository.refreshProvince(province)
        dataSource.failure = IllegalStateException("Error simulado de red")

        var failed = false
        try {
            repository.refreshProvince(province)
        } catch (expected: Exception) {
            failed = true
        }

        assertTrue("La descarga debía fallar", failed)
        assertOldDataPreserved()
    }

    @Test
    fun responseWithoutAnyPricesPreservesPreviousData() = runBlocking {
        repository.refreshProvince(province)

        dataSource.response = response(
            "fecha-inválida",
            3000L,
            listOf(
                station("empty-1", "Dirección 1", emptyMap()),
                station("empty-2", "Dirección 2", emptyMap())
            ),
            "respuesta sin precios"
        )

        var rejected = false
        try {
            repository.refreshProvince(province)
        } catch (expected: Exception) {
            rejected = true
        }

        assertTrue("La respuesta sin precios debía rechazarse", rejected)
        assertOldDataPreserved()
    }

    private class FakeMitecoDataSource(
        var response: MitecoProvinceData
    ) : MitecoDataSource {

        var failure: Exception? = null

        override suspend fun fetchProvince(
            province: Province
        ): MitecoProvinceData {
            failure?.let { throw it }
            return response
        }
    }
}
