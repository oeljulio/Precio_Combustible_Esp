package es.precogas.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "fuel_prices",
    primaryKeys = ["stationId", "productKey"],
    foreignKeys = [
        ForeignKey(
            entity = FuelStationEntity::class,
            parentColumns = ["stationId"],
            childColumns = ["stationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["stationId"])]
)
data class FuelPriceEntity(
    val stationId: String,
    val productKey: String,
    val priceMilli: Int
)
