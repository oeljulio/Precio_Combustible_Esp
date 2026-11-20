package es.precogas.app

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import es.precogas.app.data.model.FuelProduct
import es.precogas.app.data.model.FuelCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class ThemeMode {
    LIGHT,
    DARK,
    AMOLED;

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: LIGHT
    }
}

enum class CostMode {
    REFUEL_ONLY,
    REFUEL_AND_OUTBOUND,
    REFUEL_AND_ROUND_TRIP;

    companion object {
        fun fromStorage(value: String?, legacyRoundTrip: Boolean): CostMode =
            entries.firstOrNull { it.name == value }
                ?: if (legacyRoundTrip) REFUEL_AND_ROUND_TRIP else REFUEL_ONLY
    }
}

private val Context.preCoGasDataStore by preferencesDataStore(
    name = "precogas_preferences"
)

private val themeKey = stringPreferencesKey("theme_mode")
private val provinceKey = stringPreferencesKey("province_id")
private val searchRadiusKey = intPreferencesKey("search_radius_km")
private val vehicleConsumptionKey = stringPreferencesKey("vehicle_consumption")
private val refuelLitersKey = stringPreferencesKey("refuel_liters")
private val vehicleConsumptionKgKey = stringPreferencesKey("vehicle_consumption_kg")
private val refuelKgKey = stringPreferencesKey("refuel_kg")
private val roundTripKey = booleanPreferencesKey("round_trip")
private val costModeKey = stringPreferencesKey("cost_mode")
private val selectedFuelProductKey = stringPreferencesKey("selected_fuel_product")
private val includeRestrictedStationsKey = booleanPreferencesKey("include_restricted_stations")

class ThemePreferences(context: Context) {
    private val dataStore = context.applicationContext.preCoGasDataStore
    private val preferences = dataStore.data.catch { error ->
        if (error is IOException) {
            emit(emptyPreferences())
        } else {
            throw error
        }
    }

    val themeMode: Flow<ThemeMode> = preferences.map { values ->
        ThemeMode.fromStorage(values[themeKey])
    }

    val provinceId: Flow<String?> = preferences.map { values ->
        values[provinceKey]
    }

    val searchRadiusKm: Flow<Int> = preferences.map { values ->
        values[searchRadiusKey]?.takeIf { it in listOf(25, 50, 100) } ?: 25
    }

    val vehicleConsumption: Flow<String> = preferences.map { values ->
        values[vehicleConsumptionKey].orEmpty()
    }

    val refuelLiters: Flow<String> = preferences.map { values ->
        values[refuelLitersKey].orEmpty()
    }

    val vehicleConsumptionKg: Flow<String> = preferences.map { values ->
        values[vehicleConsumptionKgKey].orEmpty()
    }

    val refuelKg: Flow<String> = preferences.map { values ->
        values[refuelKgKey].orEmpty()
    }

    val selectedFuelProduct: Flow<String?> = preferences.map { values ->
        values[selectedFuelProductKey]
    }

    val includeRestrictedStations: Flow<Boolean> = preferences.map { values ->
        values[includeRestrictedStationsKey] ?: false
    }

    val costMode: Flow<CostMode> = preferences.map { values ->
        CostMode.fromStorage(values[costModeKey], values[roundTripKey] ?: false)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { values ->
            values[themeKey] = mode.name
        }
    }

    suspend fun setProvinceId(id: String) {
        require(es.precogas.app.data.model.ProvinceCatalog.findById(id) != null) {
            "Código de provincia no válido."
        }
        dataStore.edit { values ->
            values[provinceKey] = id
        }
    }

    suspend fun setSearchRadiusKm(radius: Int) {
        require(radius in listOf(25, 50, 100)) {
            "Radio de búsqueda no válido."
        }
        dataStore.edit { values ->
            values[searchRadiusKey] = radius
        }
    }

    suspend fun setVehicleConsumption(value: String) {
        dataStore.edit { values ->
            values[vehicleConsumptionKey] = value
        }
    }

    suspend fun setRefuelLiters(value: String) {
        dataStore.edit { values ->
            values[refuelLitersKey] = value
        }
    }

    suspend fun setVehicleConsumptionKg(value: String) {
        dataStore.edit { values ->
            values[vehicleConsumptionKgKey] = value
        }
    }

    suspend fun setRefuelKg(value: String) {
        dataStore.edit { values ->
            values[refuelKgKey] = value
        }
    }

    suspend fun setSelectedFuelProduct(apiKey: String?) {
        require(
            apiKey == null || FuelProduct.fromApiKey(apiKey)?.let { it.category != FuelCategory.ADDITIVE } == true
        ) {
            "Combustible no válido."
        }
        dataStore.edit { values ->
            if (apiKey == null) {
                values.remove(selectedFuelProductKey)
            } else {
                values[selectedFuelProductKey] = apiKey
            }
        }
    }

    suspend fun setIncludeRestrictedStations(include: Boolean) {
        dataStore.edit { values ->
            values[includeRestrictedStationsKey] = include
        }
    }

    suspend fun setCostMode(mode: CostMode) {
        dataStore.edit { values ->
            values[costModeKey] = mode.name
            values[roundTripKey] = mode == CostMode.REFUEL_AND_ROUND_TRIP
        }
    }
}
