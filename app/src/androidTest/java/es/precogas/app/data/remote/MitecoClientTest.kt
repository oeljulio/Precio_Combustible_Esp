package es.precogas.app.data.remote

import es.precogas.app.data.model.FuelProduct
import es.precogas.app.data.model.Province
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MitecoClientTest {

    private val client = MitecoClient()
    private val province = Province("28", "Madrid", 40.42, -3.7)

    private fun station(
        id: String = "1234",
        provinceId: String = "28",
        extraFields: String = ""
    ) = """
        {
            "IDEESS": "$id",
            "IDProvincia": "$provinceId",
            "C.P.": "28001",
            "Dirección": "Calle de prueba",
            "Horario": "24H",
            "Localidad": "Madrid",
            "Latitud": "40,400000",
            "Longitud (WGS84)": "-3,700000",
            "Municipio": "Madrid",
            "Provincia": "Madrid",
            "Rótulo": "Estación de prueba",
            "Tipo Venta": "P"
            $extraFields
        }
    """.trimIndent()

    private fun response(stations: String, date: String = "18/11/2026") = """
        {
            "ResultadoConsulta": "OK",
            "Fecha": "$date",
            "Nota": "",
            "ListaEESSPrecio": [$stations]
        }
    """.trimIndent()

    @Test
    fun parsesStationAndAvailablePrices() {
        val jsonStation = station(
            extraFields = """
                ,"Precio Gasoleo A": "1,459"
                ,"Precio Gasolina 95 E5": "1.529"
                ,"Precio Gasoleo B": ""
            """
        )

        val result = client.parseProvince(response(jsonStation), province)
        val parsed = result.stations.single()

        assertEquals("28", result.provinceId)
        assertEquals("18/11/2026", result.officialDate)
        assertEquals("1234", parsed.id)
        assertEquals("Madrid", parsed.provinceName)
        assertEquals(40.4, parsed.latitude!!, 0.000001)
        assertEquals(-3.7, parsed.longitude!!, 0.000001)
        assertEquals(1459, parsed.prices[FuelProduct.DIESEL_A])
        assertEquals(1529, parsed.prices[FuelProduct.GASOLINE_95_E5])
        assertNull(parsed.prices[FuelProduct.DIESEL_B])
    }

    @Test
    fun acceptsStationWithoutPrices() {
        val result = client.parseProvince(
            response(station()),
            province
        )

        assertEquals(1, result.stations.size)
        assertEquals(emptyMap<FuelProduct, Int>(), result.stations.single().prices)
    }

    @Test
    fun acceptsUtf8BomAtBeginning() {
        val result = client.parseProvince(
            "\uFEFF" + response(station()),
            province
        )

        assertEquals("1234", result.stations.single().id)
    }

    @Test
    fun usesRequestedProvinceWhenApiProvinceIsMissing() {
        val result = client.parseProvince(
            response(
                """
                {
                    "IDEESS": "1234",
                    "Rótulo": "Estación de prueba"
                }
                """.trimIndent()
            ),
            province
        )

        assertEquals("28", result.stations.single().provinceId)
    }

    @Test(expected = MitecoException::class)
    fun rejectsInvalidJson() {
        client.parseProvince("{respuesta inválida", province)
    }

    @Test(expected = MitecoException::class)
    fun rejectsNonOkResult() {
        client.parseProvince(
            """{"ResultadoConsulta":"ERROR","Nota":"Error de prueba"}""",
            province
        )
    }

    @Test(expected = MitecoException::class)
    fun rejectsMissingDate() {
        client.parseProvince(
            """{"ResultadoConsulta":"OK","ListaEESSPrecio":[${station()}]}""",
            province
        )
    }

    @Test(expected = MitecoException::class)
    fun rejectsEmptyStationList() {
        client.parseProvince(response(""), province)
    }

    @Test(expected = MitecoException::class)
    fun rejectsStationWithoutIdentifier() {
        client.parseProvince(
            response("""{"IDProvincia":"28"}"""),
            province
        )
    }

    @Test(expected = MitecoException::class)
    fun rejectsStationFromAnotherProvince() {
        client.parseProvince(
            response(station(provinceId = "38")),
            province
        )
    }

    @Test(expected = MitecoException::class)
    fun rejectsMalformedStationEntry() {
        client.parseProvince(
            response("null"),
            province
        )
    }

    @Test(expected = MitecoException::class)
    fun rejectsInvalidPrice() {
        client.parseProvince(
            response(
                station(extraFields = ",\"Precio Gasoleo A\": \"precio-inválido\"")
            ),
            province
        )
    }
}
