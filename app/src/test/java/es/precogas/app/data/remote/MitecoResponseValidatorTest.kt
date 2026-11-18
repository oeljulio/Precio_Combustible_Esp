package es.precogas.app.data.remote

import es.precogas.app.data.model.FuelProduct
import es.precogas.app.data.model.FuelStation
import es.precogas.app.data.model.MitecoProvinceData
import es.precogas.app.data.model.Province
import org.junit.Assert.assertEquals
import org.junit.Test

class MitecoResponseValidatorTest {

    private val province = Province("28", "Madrid", 40.42, -3.7)

    private fun station(
        id: String = "station-1",
        provinceId: String = province.id,
        latitude: Double? = 40.4,
        longitude: Double? = -3.7,
        prices: Map<FuelProduct, Int> = mapOf(FuelProduct.DIESEL_A to 1500)
    ) = FuelStation(
        id = id,
        provinceId = provinceId,
        postalCode = "28001",
        address = "Calle de prueba",
        hours = "24H",
        locality = "Madrid",
        latitude = latitude,
        longitude = longitude,
        margin = "",
        municipality = "Madrid",
        provinceName = "Madrid",
        brand = "Prueba",
        saleType = "P",
        prices = prices
    )

    private fun response(
        provinceId: String = province.id,
        officialDate: String = "17/11/2026 12:00:00",
        downloadedAtMillis: Long = 1_800_000_000_000L,
        stations: List<FuelStation> = listOf(station())
    ) = MitecoProvinceData(
        provinceId = provinceId,
        officialDate = officialDate,
        downloadedAtMillis = downloadedAtMillis,
        stations = stations,
        note = ""
    )

    @Test
    fun acceptsValidResponseAndStationsWithoutPrices() {
        val data = response(
            stations = listOf(
                station(),
                station(id = "station-2", prices = emptyMap())
            )
        )

        MitecoResponseValidator.validate(data, province)
        assertEquals(2, data.stations.size)
        assertEquals(emptyMap<FuelProduct, Int>(), data.stations[1].prices)
    }

    @Test(expected = MitecoException::class)
    fun rejectsResponseWhenNoStationHasAnyPrice() {
        val data = response(
            stations = listOf(
                station(prices = emptyMap()),
                station(id = "station-2", prices = emptyMap())
            )
        )

        MitecoResponseValidator.validate(data, province)
    }

    @Test(expected = MitecoException::class)
    fun rejectsDifferentProvince() {
        MitecoResponseValidator.validate(
            response(provinceId = "38"), province
        )
    }

    @Test(expected = MitecoException::class)
    fun rejectsBlankOfficialDate() {
        MitecoResponseValidator.validate(
            response(officialDate = "  "), province
        )
    }

    @Test(expected = MitecoException::class)
    fun rejectsInvalidDownloadTime() {
        MitecoResponseValidator.validate(
            response(downloadedAtMillis = 0L), province
        )
    }

    @Test(expected = MitecoException::class)
    fun rejectsEmptyStationList() {
        MitecoResponseValidator.validate(
            response(stations = emptyList()), province
        )
    }

    @Test(expected = MitecoException::class)
    fun rejectsBlankStationId() {
        MitecoResponseValidator.validate(
            response(stations = listOf(station(id = " "))), province
        )
    }

    @Test(expected = MitecoException::class)
    fun rejectsStationFromDifferentProvince() {
        MitecoResponseValidator.validate(
            response(stations = listOf(station(provinceId = "38"))),
            province
        )
    }

    @Test(expected = MitecoException::class)
    fun rejectsDuplicateStationIds() {
        MitecoResponseValidator.validate(
            response(stations = listOf(station(), station())),
            province
        )
    }

    @Test(expected = MitecoException::class)
    fun rejectsInvalidLatitude() {
        MitecoResponseValidator.validate(
            response(stations = listOf(station(latitude = 91.0))),
            province
        )
    }

    @Test(expected = MitecoException::class)
    fun rejectsInvalidLongitude() {
        MitecoResponseValidator.validate(
            response(stations = listOf(station(longitude = Double.NaN))),
            province
        )
    }

    @Test(expected = MitecoException::class)
    fun rejectsNonPositivePrice() {
        MitecoResponseValidator.validate(
            response(
                stations = listOf(
                    station(prices = mapOf(FuelProduct.DIESEL_A to 0))
                )
            ),
            province
        )
    }
}
