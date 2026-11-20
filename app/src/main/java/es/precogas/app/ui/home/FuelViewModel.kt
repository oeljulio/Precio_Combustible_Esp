package es.precogas.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import es.precogas.app.data.model.FuelCategory
import es.precogas.app.data.model.FuelProduct
import es.precogas.app.data.model.FuelStation
import es.precogas.app.data.model.Province
import es.precogas.app.data.model.StationRoute
import es.precogas.app.data.routing.OsrmRoutingClient
import es.precogas.app.data.repository.FuelRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RouteUiState(
    val routes: Map<String, StationRoute> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class HomeUiState(
    val stations: List<FuelStation> = emptyList(),
    val availableProducts: List<FuelProduct> = emptyList(),
    val isRefreshing: Boolean = false,
    val refreshError: String? = null,
    val officialDate: String? = null,
    val downloadedAtMillis: Long? = null,
    val note: String = "",
    val province: Province? = null
)

class FuelViewModel(
    private val repository: FuelRepository,
    private val routingClient: OsrmRoutingClient,
    initialProvince: Province
) : ViewModel() {
    private var routeJob: Job? = null
    private var activeRouteKey: String? = null
    private val failedRouteTracker = FailedRouteTracker()
    private val routeCache = linkedMapOf<String, Map<String, StationRoute>>()
    private val _routeState = MutableStateFlow(RouteUiState())
    val routeState: StateFlow<RouteUiState> = _routeState.asStateFlow()
    private val selectedProvince = MutableStateFlow(initialProvince)
    private val refreshing = MutableStateFlow(false)
    private val refreshError = MutableStateFlow<String?>(null)
    private var refreshJob: Job? = null
    private var refreshGeneration = 0

    val uiState: StateFlow<HomeUiState> = combine(
        selectedProvince.flatMapLatest { repository.observeStations(it.id) },
        selectedProvince.flatMapLatest { repository.observeProvince(it.id) },
        refreshing,
        refreshError,
        selectedProvince
    ) { stations, data, loading, error, province ->
        val products = FuelProduct.entries.filter { product ->
            product.category != FuelCategory.ADDITIVE &&
                stations.any { it.priceFor(product) != null }
        }
        HomeUiState(
            stations = stations,
            availableProducts = products,
            isRefreshing = loading,
            refreshError = error,
            officialDate = data?.officialDate,
            downloadedAtMillis = data?.downloadedAtMillis,
            note = data?.note.orEmpty(),
            province = province
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HomeUiState(province = initialProvince)
    )

    init {
        refresh()
    }

    fun calculateRoutes(
        originLatitude: Double,
        originLongitude: Double,
        stations: List<FuelStation>
    ) {
        if (!originLatitude.isFinite() || originLatitude !in -90.0..90.0 ||
            !originLongitude.isFinite() || originLongitude !in -180.0..180.0
        ) {
            clearRoutes()
            return
        }

        val validStations = stations.filter { station ->
            station.latitude?.let { it.isFinite() && it in -90.0..90.0 } == true &&
                station.longitude?.let { it.isFinite() && it in -180.0..180.0 } == true
        }.sortedBy { it.id }

        if (validStations.isEmpty()) {
            clearRoutes()
            return
        }

        val key = buildString {
            append(originLatitude)
            append('|')
            append(originLongitude)
            validStations.forEach { station ->
                append('|')
                append(station.id)
                append(':')
                append(station.latitude)
                append(',')
                append(station.longitude)
            }
        }

        if (key == activeRouteKey) return

        // El contexto solicitado cambió: cancelar cualquier petición anterior.
        routeJob?.cancel()
        routeJob = null
        activeRouteKey = null

        // Una petición idéntica que ya falló no se repite automáticamente al
        // recomponer, abrir/cerrar Ajustes o rotar. El reintento explícito de la
        // interfaz llama antes a clearRoutes().
        val failedError = failedRouteTracker.errorFor(key)
        if (failedError != null) {
            _routeState.value = RouteUiState(error = failedError)
            return
        }

        activeRouteKey = key

        val cached = routeCache[key]
        if (cached != null && cached.values.none { route ->
                route.outboundRequestFailed || route.returnRequestFailed
            }
        ) {
            _routeState.value = RouteUiState(routes = cached)
            routeJob = null
            return
        } else if (cached != null) {
            routeCache.remove(key)
        }

        _routeState.value = RouteUiState(isLoading = true)
        routeJob = viewModelScope.launch {
            try {
                val routes = routingClient.fetchRoutes(
                    originLatitude,
                    originLongitude,
                    validStations
                )

                if (activeRouteKey == key) {
                    failedRouteTracker.clear()
                    if (routes.values.none { route ->
                            route.outboundRequestFailed || route.returnRequestFailed
                        }
                    ) {
                        routeCache[key] = routes
                        while (routeCache.size > 3) {
                            routeCache.remove(routeCache.keys.first())
                        }
                    }
                    _routeState.value = RouteUiState(routes = routes)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (activeRouteKey == key) {
                    val message = error.message?.takeIf { it.isNotBlank() }
                        ?: "No se pudieron calcular las rutas."
                    activeRouteKey = null
                    failedRouteTracker.recordFailure(key, message)
                    _routeState.value = RouteUiState(error = message)
                }
            }
        }
    }

    fun clearRoutes() {
        routeJob?.cancel()
        routeJob = null
        activeRouteKey = null
        failedRouteTracker.clear()
        _routeState.value = RouteUiState()
    }

    override fun onCleared() {
        routeJob?.cancel()
        super.onCleared()
    }

    fun selectProvince(province: Province) {
        if (selectedProvince.value.id == province.id) return
        clearRoutes()
        routeCache.clear()
        selectedProvince.value = province
        refresh(province)
    }

    fun refresh() {
        refresh(selectedProvince.value)
    }

    private fun refresh(province: Province) {
        refreshJob?.cancel()
        val generation = ++refreshGeneration

        refreshJob = viewModelScope.launch {
            refreshing.value = true
            refreshError.value = null
            try {
                repository.refreshProvince(province)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                refreshError.value = error.message
                    ?.takeIf { it.isNotBlank() }
                    ?: "No se pudieron actualizar los datos."
            } finally {
                if (generation == refreshGeneration) {
                    refreshing.value = false
                }
            }
        }
    }
}

class FuelViewModelFactory(
    private val repository: FuelRepository,
    private val routingClient: OsrmRoutingClient,
    private val province: Province
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FuelViewModel::class.java)) {
            return FuelViewModel(repository, routingClient, province) as T
        }
        throw IllegalArgumentException("ViewModel no reconocido")
    }
}
