package es.precogas.app.data.repository

import es.precogas.app.data.local.dao.PreCoGasDao
import es.precogas.app.data.local.entity.FuelPriceEntity
import es.precogas.app.data.local.entity.FuelStationEntity
import es.precogas.app.data.local.entity.ProvinceEntity
import es.precogas.app.data.local.relation.StationWithPrices
import es.precogas.app.data.model.FuelProduct
import es.precogas.app.data.model.FuelStation
import es.precogas.app.data.model.MitecoProvinceData
import es.precogas.app.data.model.Province
import es.precogas.app.data.remote.MitecoDataSource
import es.precogas.app.data.remote.MitecoResponseValidator
import es.precogas.app.data.remote.MitecoException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FuelRepository(
    private val dao: PreCoGasDao,
    private val mitecoClient: MitecoDataSource
) {

    fun observeProvince(provinceId: String): Flow<ProvinceEntity?> =
        dao.observeProvince(provinceId)

    fun observeStations(provinceId: String): Flow<List<FuelStation>> =
        dao.observeStationsWithPrices(provinceId).map { stations ->
            stations.map { it.toFuelStation() }
        }

    fun observeStations(provinceIds: List<String>): Flow<List<FuelStation>> =
        if (provinceIds.isEmpty()) {
            flowOf(emptyList())
        } else {
            dao.observeStationsWithPrices(provinceIds).map { stations ->
                stations.map { it.toFuelStation() }
            }
        }

    fun observeAllProvinces(): Flow<List<ProvinceEntity>> =
        dao.observeAllProvinces()

    suspend fun refreshProvince(province: Province): MitecoProvinceData {
        val response = mitecoClient.fetchProvince(province)
        MitecoResponseValidator.validate(response, province)

        val stationEntities = response.stations.map { station ->
            FuelStationEntity(
                stationId = station.id,
                provinceId = station.provinceId,
                postalCode = station.postalCode,
                address = station.address,
                hours = station.hours,
                locality = station.locality,
                latitude = station.latitude,
                longitude = station.longitude,
                margin = station.margin,
                municipality = station.municipality,
                provinceName = station.provinceName,
                brand = station.brand,
                saleType = station.saleType
            )
        }

        val priceEntities = response.stations.flatMap { station ->
            station.prices.map { (product, price) ->
                FuelPriceEntity(
                    stationId = station.id,
                    productKey = product.name,
                    priceMilli = price
                )
            }
        }

        val provinceEntity = ProvinceEntity(
            provinceId = response.provinceId,
            officialDate = response.officialDate,
            downloadedAtMillis = response.downloadedAtMillis,
            note = response.note
        )

        dao.replaceProvinceData(
            province = provinceEntity,
            stations = stationEntities,
            prices = priceEntities
        )

        return response
    }

    private fun StationWithPrices.toFuelStation(): FuelStation {
        val productPrices = prices.mapNotNull { price ->
            val product = FuelProduct.entries.firstOrNull {
                it.name == price.productKey
            } ?: return@mapNotNull null

            if (price.priceMilli <= 0) {
                return@mapNotNull null
            }

            product to price.priceMilli
        }.toMap()

        return FuelStation(
            id = station.stationId,
            provinceId = station.provinceId,
            postalCode = station.postalCode,
            address = station.address,
            hours = station.hours,
            locality = station.locality,
            latitude = station.latitude,
            longitude = station.longitude,
            margin = station.margin,
            municipality = station.municipality,
            provinceName = station.provinceName,
            brand = station.brand,
            saleType = station.saleType,
            prices = productPrices
        )
    }
}
