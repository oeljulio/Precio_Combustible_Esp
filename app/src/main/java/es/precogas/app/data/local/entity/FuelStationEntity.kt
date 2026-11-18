package es.precogas.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fuel_stations",
    foreignKeys = [
        ForeignKey(
            entity = ProvinceEntity::class,
            parentColumns = ["provinceId"],
            childColumns = ["provinceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["provinceId"])]
)
data class FuelStationEntity(
    @PrimaryKey
    val stationId: String,
    val provinceId: String,
    val postalCode: String,
    val address: String,
    val hours: String,
    val locality: String,
    val latitude: Double?,
    val longitude: Double?,
    val margin: String,
    val municipality: String,
    val provinceName: String,
    val brand: String,
    val saleType: String
)
