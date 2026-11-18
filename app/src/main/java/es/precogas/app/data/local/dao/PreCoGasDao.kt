package es.precogas.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import es.precogas.app.data.local.entity.FuelPriceEntity
import es.precogas.app.data.local.entity.FuelStationEntity
import es.precogas.app.data.local.entity.ProvinceEntity
import es.precogas.app.data.local.relation.StationWithPrices
import kotlinx.coroutines.flow.Flow

@Dao
interface PreCoGasDao {

    @Query("SELECT * FROM provinces WHERE provinceId = :provinceId LIMIT 1")
    fun observeProvince(provinceId: String): Flow<ProvinceEntity?>

    @Query("SELECT * FROM provinces WHERE provinceId = :provinceId LIMIT 1")
    suspend fun getProvince(provinceId: String): ProvinceEntity?

    @Query("SELECT * FROM provinces ORDER BY provinceId")
    fun observeAllProvinces(): Flow<List<ProvinceEntity>>

    @Transaction
    @Query("""
        SELECT * FROM fuel_stations
        WHERE provinceId = :provinceId
        ORDER BY brand COLLATE NOCASE, address COLLATE NOCASE
    """)
    fun observeStationsWithPrices(
        provinceId: String
    ): Flow<List<StationWithPrices>>

    @Transaction
    @Query("""
        SELECT * FROM fuel_stations
        WHERE provinceId IN (:provinceIds)
        ORDER BY brand COLLATE NOCASE, address COLLATE NOCASE
    """)
    fun observeStationsWithPrices(
        provinceIds: List<String>
    ): Flow<List<StationWithPrices>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProvinceIfAbsent(province: ProvinceEntity)

    @Query("""
        UPDATE provinces
        SET officialDate = :officialDate,
            downloadedAtMillis = :downloadedAtMillis,
            note = :note
        WHERE provinceId = :provinceId
    """)
    suspend fun updateProvinceMetadata(
        provinceId: String,
        officialDate: String,
        downloadedAtMillis: Long,
        note: String
    )

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStations(stations: List<FuelStationEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPrices(prices: List<FuelPriceEntity>)

    @Query("DELETE FROM fuel_stations WHERE provinceId = :provinceId")
    suspend fun deleteStationsForProvince(provinceId: String)

    @Query("SELECT COUNT(*) FROM fuel_stations WHERE provinceId = :provinceId")
    suspend fun countStations(provinceId: String): Int

    @Transaction
    suspend fun replaceProvinceData(
        province: ProvinceEntity,
        stations: List<FuelStationEntity>,
        prices: List<FuelPriceEntity>
    ) {
        require(stations.isNotEmpty()) {
            "No se pueden reemplazar los datos de una provincia sin estaciones."
        }

        insertProvinceIfAbsent(province)
        updateProvinceMetadata(
            province.provinceId,
            province.officialDate,
            province.downloadedAtMillis,
            province.note
        )
        deleteStationsForProvince(province.provinceId)

        if (stations.isNotEmpty()) {
            insertStations(stations)
        }

        if (prices.isNotEmpty()) {
            insertPrices(prices)
        }
    }
}
