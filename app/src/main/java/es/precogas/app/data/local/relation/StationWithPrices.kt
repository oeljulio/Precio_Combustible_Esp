package es.precogas.app.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import es.precogas.app.data.local.entity.FuelPriceEntity
import es.precogas.app.data.local.entity.FuelStationEntity

data class StationWithPrices(
    @Embedded
    val station: FuelStationEntity,

    @Relation(
        parentColumn = "stationId",
        entityColumn = "stationId"
    )
    val prices: List<FuelPriceEntity>
)
