package es.precogas.app.data.remote

import es.precogas.app.data.model.MitecoProvinceData
import es.precogas.app.data.model.Province

internal object MitecoResponseValidator {

    fun validate(
        response: MitecoProvinceData,
        requestedProvince: Province
    ) {
        if (response.provinceId != requestedProvince.id) {
            throw MitecoException(
                "MITECO devolvió datos de una provincia diferente."
            )
        }

        if (response.officialDate.isBlank()) {
            throw MitecoException(
                "MITECO no indicó la fecha oficial de los datos."
            )
        }

        if (response.downloadedAtMillis <= 0L) {
            throw MitecoException(
                "La fecha de descarga de MITECO no es válida."
            )
        }

        if (response.stations.isEmpty()) {
            throw MitecoException(
                "MITECO devolvió una provincia sin estaciones. Se conservarán los datos anteriores."
            )
        }

        if (response.stations.none { it.prices.isNotEmpty() }) {
            throw MitecoException(
                "MITECO no ha devuelto ningún precio para la provincia. Se conservarán los datos anteriores."
            )
        }

        if (response.stations.any {
                it.id.isBlank() || it.provinceId != requestedProvince.id
            }) {
            throw MitecoException(
                "La respuesta contiene estaciones sin identificar o de otra provincia."
            )
        }

        if (response.stations.map { it.id }.distinct().size != response.stations.size) {
            throw MitecoException(
                "La respuesta contiene identificadores de estaciones duplicados."
            )
        }

        if (response.stations.any { station ->
                station.latitude?.let {
                    !it.isFinite() || it !in -90.0..90.0
                } == true ||
                station.longitude?.let {
                    !it.isFinite() || it !in -180.0..180.0
                } == true
            }) {
            throw MitecoException(
                "La respuesta contiene coordenadas geográficas no válidas."
            )
        }

        if (response.stations.any { station ->
                station.prices.values.any { it <= 0 }
            }) {
            throw MitecoException(
                "La respuesta contiene precios no válidos."
            )
        }
    }
}
