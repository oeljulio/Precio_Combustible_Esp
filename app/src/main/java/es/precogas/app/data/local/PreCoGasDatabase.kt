package es.precogas.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import es.precogas.app.data.local.dao.PreCoGasDao
import es.precogas.app.data.local.entity.FuelPriceEntity
import es.precogas.app.data.local.entity.FuelStationEntity
import es.precogas.app.data.local.entity.ProvinceEntity

@Database(
    entities = [
        ProvinceEntity::class,
        FuelStationEntity::class,
        FuelPriceEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class PreCoGasDatabase : RoomDatabase() {

    abstract fun preCoGasDao(): PreCoGasDao

    companion object {
        @Volatile
        private var INSTANCE: PreCoGasDatabase? = null

        fun getInstance(context: Context): PreCoGasDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PreCoGasDatabase::class.java,
                    "precogas_database"
                ).build().also { database ->
                    INSTANCE = database
                }
            }
        }
    }
}
