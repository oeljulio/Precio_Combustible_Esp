package es.precogas.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import es.precogas.app.data.model.ProvinceCatalog
import es.precogas.app.location.DeviceLocationProvider
import es.precogas.app.location.DeviceLocationStatus
import es.precogas.app.ui.home.FuelViewModel
import es.precogas.app.ui.home.FuelViewModelFactory
import es.precogas.app.ui.home.PreCoGasHomeScreen
import es.precogas.app.ui.home.ProvinceSelectionScreen
import es.precogas.app.ui.theme.PreCoGasTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var themePreferences: ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as PreCoGasApplication).container
        themePreferences = container.themePreferences

        setContent {
            val themeMode by themePreferences.themeMode.collectAsState(initial = ThemeMode.LIGHT)
            val provinceId by themePreferences.provinceId.collectAsState(initial = "__LOADING__")
            val searchRadiusKm by themePreferences.searchRadiusKm.collectAsState(initial = 25)
            val vehicleConsumption by themePreferences.vehicleConsumption.collectAsState(initial = "")
            val refuelLiters by themePreferences.refuelLiters.collectAsState(initial = "")
            val vehicleConsumptionKg by themePreferences.vehicleConsumptionKg.collectAsState(initial = "")
            val refuelKg by themePreferences.refuelKg.collectAsState(initial = "")
            val selectedFuelProduct by themePreferences.selectedFuelProduct.collectAsState(initial = null)
            val costMode by themePreferences.costMode.collectAsState(initial = CostMode.REFUEL_ONLY)
            val coroutineScope = rememberCoroutineScope()
            val locationProvider = remember {
                DeviceLocationProvider(this@MainActivity)
            }
            var originLatitude by remember { mutableStateOf<Double?>(null) }
            var originLongitude by remember { mutableStateOf<Double?>(null) }
            var locationStatus by remember {
                mutableStateOf(DeviceLocationStatus.NOT_REQUESTED)
            }
            var locationJob by remember { mutableStateOf<Job?>(null) }
            var locationGeneration by remember { mutableStateOf(0L) }
            var permissionRequestGeneration by remember { mutableStateOf<Long?>(null) }
            var locationRequired by remember {
                mutableStateOf(costMode != CostMode.REFUEL_ONLY)
            }

            fun invalidateLocationRequest(status: DeviceLocationStatus) {
                locationGeneration += 1
                locationJob?.cancel()
                locationJob = null
                permissionRequestGeneration = null
                originLatitude = null
                originLongitude = null
                locationStatus = status
            }

            fun loadCurrentLocation() {
                if (!locationRequired) return

                locationGeneration += 1
                val requestId = locationGeneration
                locationJob?.cancel()
                locationStatus = DeviceLocationStatus.LOADING

                locationJob = coroutineScope.launch {
                    try {
                        val location = locationProvider.getCurrentLocation()

                        if (requestId == locationGeneration && locationRequired) {
                            if (location != null) {
                                originLatitude = location.latitude
                                originLongitude = location.longitude
                                locationStatus = DeviceLocationStatus.READY
                            } else {
                                originLatitude = null
                                originLongitude = null
                                locationStatus = DeviceLocationStatus.UNAVAILABLE
                            }
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: SecurityException) {
                        if (requestId == locationGeneration && locationRequired) {
                            originLatitude = null
                            originLongitude = null
                            locationStatus = DeviceLocationStatus.DENIED
                        }
                    } catch (error: Exception) {
                        if (requestId == locationGeneration && locationRequired) {
                            originLatitude = null
                            originLongitude = null
                            locationStatus = DeviceLocationStatus.ERROR
                        }
                    } finally {
                        if (requestId == locationGeneration) {
                            locationJob = null
                        }
                    }
                }
            }

            LaunchedEffect(costMode) {
                val required = costMode != CostMode.REFUEL_ONLY
                locationRequired = required
                if (!required) {
                    invalidateLocationRequest(DeviceLocationStatus.NOT_REQUESTED)
                }
            }

            val locationPermissionLauncher =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val requestId = permissionRequestGeneration
                    permissionRequestGeneration = null

                    if (
                        requestId == null ||
                        requestId != locationGeneration ||
                        !locationRequired
                    ) {
                        return@rememberLauncherForActivityResult
                    }

                    val granted =
                        permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                        checkSelfPermission(
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED ||
                        checkSelfPermission(
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                    if (granted) {
                        loadCurrentLocation()
                    } else {
                        originLatitude = null
                        originLongitude = null
                        locationStatus = DeviceLocationStatus.DENIED
                    }
                }

            val requestLocation: () -> Unit = {
                locationRequired = true
                if (
                    locationRequired &&
                    locationStatus != DeviceLocationStatus.LOADING
                ) {
                    val alreadyGranted =
                        checkSelfPermission(
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED ||
                        checkSelfPermission(
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                    if (alreadyGranted) {
                        loadCurrentLocation()
                    } else {
                        locationGeneration += 1
                        val requestId = locationGeneration
                        locationJob?.cancel()
                        locationJob = null
                        permissionRequestGeneration = requestId
                        locationStatus = DeviceLocationStatus.LOADING

                        try {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            )
                        } catch (error: Exception) {
                            if (requestId == locationGeneration) {
                                permissionRequestGeneration = null
                                locationStatus = DeviceLocationStatus.ERROR
                            }
                        }
                    }
                }
            }

            val province = if (provinceId == "__LOADING__") {
                null
            } else {
                provinceId?.let { ProvinceCatalog.findById(it) }
            }

            PreCoGasTheme(themeMode) {
                when {
                    provinceId == "__LOADING__" -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    province == null -> {
                        ProvinceSelectionScreen(
                            onProvinceSelected = { selected ->
                                coroutineScope.launch {
                                    themePreferences.setProvinceId(selected.id)
                                }
                            }
                        )
                    }

                    else -> {
                        val factory = remember {
                            FuelViewModelFactory(
                                container.fuelRepository,
                                container.osrmRoutingClient,
                                province
                            )
                        }
                        val fuelViewModel: FuelViewModel = viewModel(factory = factory)

                        LaunchedEffect(province.id) {
                            fuelViewModel.selectProvince(province)
                        }

                        val uiState by fuelViewModel.uiState.collectAsState()
                        val routeState by fuelViewModel.routeState.collectAsState()

                        PreCoGasHomeScreen(
                            themeMode = themeMode,
                            onThemeChange = { selected ->
                                coroutineScope.launch { themePreferences.setThemeMode(selected) }
                            },
                            onProvinceChange = { selected ->
                                fuelViewModel.selectProvince(selected)
                                coroutineScope.launch { themePreferences.setProvinceId(selected.id) }
                            },
                            uiState = uiState,
                            onRefresh = fuelViewModel::refresh,
                            searchRadiusKm = searchRadiusKm,
                            vehicleConsumption = vehicleConsumption,
                            refuelLiters = refuelLiters,
                            vehicleConsumptionKg = vehicleConsumptionKg,
                            refuelKg = refuelKg,
                            selectedFuelProduct = selectedFuelProduct,
                            costMode = costMode,
                            originLatitude = originLatitude,
                            originLongitude = originLongitude,
                            locationStatus = locationStatus,
                            routeState = routeState,
                            onCalculateRoutes = fuelViewModel::calculateRoutes,
                            onClearRoutes = fuelViewModel::clearRoutes,
                            onRequestLocation = requestLocation,
                            onSearchRadiusChange = { value ->
                                coroutineScope.launch { themePreferences.setSearchRadiusKm(value) }
                            },
                            onVehicleConsumptionChange = { value ->
                                coroutineScope.launch { themePreferences.setVehicleConsumption(value) }
                            },
                            onRefuelLitersChange = { value ->
                                coroutineScope.launch { themePreferences.setRefuelLiters(value) }
                            },
                            onVehicleConsumptionKgChange = { value ->
                                coroutineScope.launch { themePreferences.setVehicleConsumptionKg(value) }
                            },
                            onRefuelKgChange = { value ->
                                coroutineScope.launch { themePreferences.setRefuelKg(value) }
                            },
                            onSelectedFuelProductChange = { value ->
                                coroutineScope.launch { themePreferences.setSelectedFuelProduct(value) }
                            },
                            onCostModeChange = { value ->
                                coroutineScope.launch {
                                    themePreferences.setCostMode(value)
                                }

                                locationRequired = value != CostMode.REFUEL_ONLY

                                if (locationRequired) {
                                    requestLocation()
                                } else {
                                    invalidateLocationRequest(
                                        DeviceLocationStatus.NOT_REQUESTED
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
