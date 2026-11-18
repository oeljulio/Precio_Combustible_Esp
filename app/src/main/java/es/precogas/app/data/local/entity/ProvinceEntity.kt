package es.precogas.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "provinces")
data class ProvinceEntity(
    @PrimaryKey
    val provinceId: String,
    val officialDate: String,
    val downloadedAtMillis: Long,
    val note: String
)
