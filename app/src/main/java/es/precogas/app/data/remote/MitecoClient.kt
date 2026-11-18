package es.precogas.app.data.remote

import es.precogas.app.data.model.FuelProduct
import es.precogas.app.data.model.FuelStation
import es.precogas.app.data.model.MitecoProvinceData
import es.precogas.app.data.model.Province
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MitecoClient : MitecoDataSource {

    companion object {
        private const val BASE_URL =
            "https://sedeaplicaciones.minetur.gob.es/" +
            "ServiciosRESTCarburantes/PreciosCarburantes/"

        private const val CONNECT_TIMEOUT_MS = 20_000
        private const val READ_TIMEOUT_MS = 30_000
    }

    override suspend fun fetchProvince(province: Province): MitecoProvinceData =
        withContext(Dispatchers.IO) {
            val url = URL(
                BASE_URL + "EstacionesTerrestres/FiltroProvincia/" +
                    province.id
            )

            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "PreCoGas/1.0 Android")
            }

            try {
                val status = connection.responseCode
                if (status !in 200..299) {
                    throw MitecoException(
                        "MITECO respondió con HTTP $status"
                    )
                }

                val body = connection.inputStream
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }

                parseProvince(body, province)
            } catch (error: CancellationException) {
                throw error
            } catch (error: MitecoException) {
                throw error
            } catch (error: Exception) {
                throw MitecoException(
                    "No se pudieron descargar los datos de MITECO",
                    error
                )
            } finally {
                connection.disconnect()
            }
        }

    internal fun parseProvince(
        body: String,
        requestedProvince: Province
    ): MitecoProvinceData {
        val root = try {
            JSONObject(body.removePrefix("\uFEFF"))
        } catch (error: Exception) {
            throw MitecoException("La respuesta de MITECO no es JSON válido", error)
        }

        val result = jsonText(root, "ResultadoConsulta")
        if (!result.equals("OK", ignoreCase = true)) {
            val detail = jsonText(root, "Nota")
            throw MitecoException(
                if (detail.isNotBlank()) {
                    "MITECO no pudo completar la consulta: $detail"
                } else {
                    "MITECO devolvió un resultado no válido: $result"
                }
            )
        }

        val array = root.optJSONArray("ListaEESSPrecio")
            ?: throw MitecoException(
                "La respuesta de MITECO no contiene la lista de estaciones"
            )

        val officialDate = jsonText(root, "Fecha")
        if (officialDate.isBlank()) {
            throw MitecoException(
                "MITECO no ha indicado la fecha de los datos."
            )
        }

        if (array.length() == 0) {
            throw MitecoException(
                "MITECO ha devuelto una lista vacía. Se conservarán los datos anteriores."
            )
        }

        val stations = buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index)
                    ?: throw MitecoException(
                        "La estación número ${index + 1} no tiene un formato válido."
                    )

                add(parseStation(item, requestedProvince))
            }
        }

        return MitecoProvinceData(
            provinceId = requestedProvince.id,
            officialDate = officialDate,
            downloadedAtMillis = System.currentTimeMillis(),
            stations = stations,
            note = jsonText(root, "Nota")
        )
    }

    private fun parseStation(
        item: JSONObject,
        requestedProvince: Province
    ): FuelStation {
        val id = jsonText(item, "IDEESS")
        if (id.isBlank()) {
            throw MitecoException(
                "MITECO ha devuelto una estación sin identificador."
            )
        }

        val apiProvinceId = jsonText(item, "IDProvincia")
        if (apiProvinceId.isNotBlank() && apiProvinceId != requestedProvince.id) {
            throw MitecoException(
                "Una estación pertenece a una provincia diferente de la solicitada."
            )
        }

        val prices = buildMap {
            for (product in FuelProduct.entries) {
                val rawPrice = jsonText(item, product.apiKey)
                val price = MitecoValueParser.parsePrice(rawPrice)
                if (price != null) {
                    put(product, price)
                }
            }
        }

        return FuelStation(
            id = id,
            provinceId = apiProvinceId.ifBlank { requestedProvince.id },
            postalCode = jsonText(item, "C.P."),
            address = jsonText(item, "Dirección"),
            hours = jsonText(item, "Horario"),
            locality = jsonText(item, "Localidad"),
            latitude = MitecoValueParser.parseCoordinate(
                jsonText(item, "Latitud"),
                "latitud",
                -90.0,
                90.0
            ),
            longitude = MitecoValueParser.parseCoordinate(
                jsonText(item, "Longitud (WGS84)"),
                "longitud",
                -180.0,
                180.0
            ),
            margin = jsonText(item, "Margen"),
            municipality = jsonText(item, "Municipio"),
            provinceName = jsonText(item, "Provincia")
                .ifBlank { requestedProvince.name },
            brand = jsonText(item, "Rótulo"),
            saleType = jsonText(item, "Tipo Venta"),
            prices = prices
        )
    }

    private fun jsonText(json: JSONObject, key: String): String {
        if (!json.has(key) || json.isNull(key)) return ""
        return json.optString(key, "").trim()
    }

}

class MitecoException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
