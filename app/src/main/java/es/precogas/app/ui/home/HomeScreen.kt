package es.precogas.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import es.precogas.app.ThemeMode
import es.precogas.app.CostMode
import es.precogas.app.data.model.FuelProduct
import es.precogas.app.data.model.Province
import es.precogas.app.data.model.ProvinceCatalog
import es.precogas.app.data.model.FuelStation
import es.precogas.app.data.model.GeoDistance
import es.precogas.app.data.model.StationRoute
import es.precogas.app.location.DeviceLocationStatus
import java.text.DateFormat
import java.text.NumberFormat
import java.math.BigDecimal
import java.util.Date
import java.util.Locale

@Composable
fun ProvinceSelectionScreen(
    onProvinceSelected: (Province) -> Unit
) {
    var selectedProvince by remember { mutableStateOf<Province?>(null) }
    var provinceMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "PreCoGas",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            "Encuentra. Compara. Ahorra.",
            style = MaterialTheme.typography.bodyMedium
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Selecciona tu provincia",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    "Elige la provincia para consultar las gasolineras "
                    + "y los precios de los combustibles disponibles."
                )

                Box {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { provinceMenuExpanded = true }
                    ) {
                        Text(selectedProvince?.name ?: "Elegir provincia")
                    }

                    DropdownMenu(
                        expanded = provinceMenuExpanded,
                        onDismissRequest = { provinceMenuExpanded = false }
                    ) {
                        ProvinceCatalog.all.forEach { province ->
                            DropdownMenuItem(
                                text = { Text(province.name) },
                                onClick = {
                                    selectedProvince = province
                                    provinceMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedProvince != null,
                    onClick = {
                        selectedProvince?.let(onProvinceSelected)
                    }
                ) {
                    Text("Continuar")
                }
            }
        }
    }
}

@Composable
fun PreCoGasHomeScreen(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onProvinceChange: (Province) -> Unit,
    uiState: HomeUiState,
    onRefresh: () -> Unit,
    searchRadiusKm: Int,
    vehicleConsumption: String,
    refuelLiters: String,
    vehicleConsumptionKg: String,
    refuelKg: String,
    selectedFuelProduct: String?,
    costMode: CostMode,
    originLatitude: Double?,
    originLongitude: Double?,
    locationStatus: DeviceLocationStatus,
    routeState: RouteUiState,
    onCalculateRoutes: (Double, Double, List<FuelStation>) -> Unit,
    onClearRoutes: () -> Unit,
    onRequestLocation: () -> Unit,
    onSearchRadiusChange: (Int) -> Unit,
    onVehicleConsumptionChange: (String) -> Unit,
    onRefuelLitersChange: (String) -> Unit,
    onVehicleConsumptionKgChange: (String) -> Unit,
    onRefuelKgChange: (String) -> Unit,
    onSelectedFuelProductChange: (String?) -> Unit,
    onCostModeChange: (CostMode) -> Unit
) {
    var provinceMenuExpanded by remember { mutableStateOf(false) }
    var fuelMenuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(StationSortOrder.PRICE) }
    val selectedProduct = selectedFuelProduct?.let { FuelProduct.fromApiKey(it) }
    val selectedProductAvailable =
        selectedProduct != null && uiState.availableProducts.contains(selectedProduct)
    val refuelQuantity = if (selectedProduct?.priceUnit == "€/kg") refuelKg else refuelLiters
    val vehicleConsumptionForProduct =
        if (selectedProduct?.priceUnit == "€/kg") vehicleConsumptionKg else vehicleConsumption
    val fuelMatchingStations = selectedProduct
        ?.takeIf { selectedProductAvailable }
        ?.let { product -> uiState.stations.filter { it.priceFor(product) != null } }
        ?: emptyList()

    val matchingStations = if (costMode == CostMode.REFUEL_ONLY) {
        fuelMatchingStations
    } else if (originLatitude != null && originLongitude != null) {
        fuelMatchingStations.filter { station ->
            GeoDistance.isWithinRadius(
                GeoDistance.distanceKm(
                    originLatitude,
                    originLongitude,
                    station.latitude,
                    station.longitude
                ),
                searchRadiusKm
            )
        }
    } else {
        emptyList()
    }

    val routeSortingAvailable =
        costMode != CostMode.REFUEL_ONLY &&
            originLatitude?.let { it.isFinite() && it in -90.0..90.0 } == true &&
            originLongitude?.let { it.isFinite() && it in -180.0..180.0 } == true &&
            routeState.error == null &&
            (routeState.isLoading || routeState.routes.isNotEmpty())

    LaunchedEffect(routeSortingAvailable) {
        if (!routeSortingAvailable && sortOrder == StationSortOrder.DISTANCE) {
            sortOrder = StationSortOrder.PRICE
        }
    }

    val comparisonStation = if (
        sortOrder == StationSortOrder.TOTAL_COST &&
        routeSortingAvailable &&
        !routeState.isLoading
    ) {
        matchingStations.mapNotNull { station ->
            val distance = routeState.routes[station.id]?.distanceMeters
                ?.takeIf { it.isFinite() && it >= 0.0 }
            distance?.let { station to it }
        }.minByOrNull { it.second }?.first
    } else {
        null
    }

    val comparisonReferenceCost = comparisonStation?.let { station ->
        stationTotalCost(
            station,
            selectedProduct ?: return@let null,
            refuelQuantity,
            vehicleConsumptionForProduct,
            costMode,
            routeState.routes[station.id]
        )
    }

    val sortedStations = if (selectedProduct == null) {
        emptyList()
    } else {
        when (sortOrder) {
            StationSortOrder.PRICE -> matchingStations.sortedWith(
                compareBy<FuelStation> {
                    it.priceFor(selectedProduct) ?: Int.MAX_VALUE
                }.thenBy { it.id }
            )

            StationSortOrder.DISTANCE -> matchingStations.sortedWith(
                compareBy<FuelStation> {
                    routeState.routes[it.id]?.distanceMeters
                        ?.takeIf { distance -> distance.isFinite() && distance >= 0.0 }
                        ?: Double.POSITIVE_INFINITY
                }.thenBy { it.id }
            )

            StationSortOrder.TOTAL_COST -> matchingStations.sortedWith(
                Comparator { first, second ->
                    val firstCost = stationTotalCost(
                        first, selectedProduct, refuelQuantity,
                        vehicleConsumptionForProduct, costMode,
                        routeState.routes[first.id]
                    )
                    val secondCost = stationTotalCost(
                        second, selectedProduct, refuelQuantity,
                        vehicleConsumptionForProduct, costMode,
                        routeState.routes[second.id]
                    )

                    when {
                        firstCost == null && secondCost == null ->
                            first.id.compareTo(second.id)
                        firstCost == null -> 1
                        secondCost == null -> -1
                        else -> firstCost.compareTo(secondCost)
                    }
                }
            )
        }
    }

    LaunchedEffect(costMode, originLatitude, originLongitude, matchingStations) {
        if (
            costMode != CostMode.REFUEL_ONLY &&
            originLatitude != null && originLongitude != null &&
            matchingStations.isNotEmpty()
        ) {
            onCalculateRoutes(originLatitude, originLongitude, matchingStations)
        } else {
            onClearRoutes()
        }
    }


    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "PreCoGas",
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    "Encuentra. Compara. Ahorra.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Provincia",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        uiState.province?.name ?: "Provincia no seleccionada",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Box {
                        Button(onClick = { provinceMenuExpanded = true }) {
                            Text("Cambiar provincia")
                        }
                        DropdownMenu(
                            expanded = provinceMenuExpanded,
                            onDismissRequest = { provinceMenuExpanded = false }
                        ) {
                            ProvinceCatalog.all.forEach { province ->
                                DropdownMenuItem(
                                    text = { Text(province.name) },
                                    onClick = {
                                        provinceMenuExpanded = false
                                        onProvinceChange(province)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Apariencia", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { onThemeChange(mode) },
                                label = {
                                    Text(
                                        when (mode) {
                                            ThemeMode.LIGHT -> "Claro"
                                            ThemeMode.DARK -> "Oscuro"
                                            ThemeMode.AMOLED -> "AMOLED"
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Búsqueda y repostaje",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text("Combustible")
                    Button(
                        onClick = { fuelMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedProduct?.displayName ?: "Selecciona un combustible")
                    }
                    DropdownMenu(
                        expanded = fuelMenuExpanded,
                        onDismissRequest = { fuelMenuExpanded = false }
                    ) {
                        uiState.availableProducts.forEach { product ->
                            DropdownMenuItem(
                                text = { Text(product.displayName) },
                                onClick = {
                                    onSelectedFuelProductChange(product.apiKey)
                                    fuelMenuExpanded = false
                                }
                            )
                        }
                    }

                    if (selectedProduct == null) {
                        Text(
                            "Selecciona un combustible para comparar precios y calcular costes.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else if (!selectedProductAvailable) {
                        Text(
                            "Este combustible no tiene precios disponibles en la provincia seleccionada. " +
                                "Se conserva tu elección; puedes elegir otro producto.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Text("Radio de búsqueda")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(25, 50, 100).forEach { radius ->
                            FilterChip(
                                selected = searchRadiusKm == radius,
                                onClick = { onSearchRadiusChange(radius) },
                                label = { Text("$radius km") }
                            )
                        }
                    }

                    Text("Cálculo de costes")
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = costMode == CostMode.REFUEL_ONLY,
                            onClick = { onCostModeChange(CostMode.REFUEL_ONLY) },
                            label = { Text("Solo repostaje") }
                        )
                        FilterChip(
                            selected = costMode == CostMode.REFUEL_AND_OUTBOUND,
                            onClick = { onCostModeChange(CostMode.REFUEL_AND_OUTBOUND) },
                            label = { Text("Repostaje + ida") }
                        )
                        FilterChip(
                            selected = costMode == CostMode.REFUEL_AND_ROUND_TRIP,
                            onClick = { onCostModeChange(CostMode.REFUEL_AND_ROUND_TRIP) },
                            label = { Text("Repostaje + ida y vuelta") }
                        )
                    }

                    if (selectedProduct != null) {
                        if (selectedProduct.priceUnit == "€/kg") {
                            PersistentNumericTextField(
                                value = vehicleConsumptionKg,
                                label = "Consumo del vehículo (kg/100 km)",
                                onValueChange = onVehicleConsumptionKgChange
                            )
                            PersistentNumericTextField(
                                value = refuelKg,
                                label = "Kilogramos que vas a repostar",
                                onValueChange = onRefuelKgChange
                            )
                        } else {
                            PersistentNumericTextField(
                                value = vehicleConsumption,
                                label = "Consumo del vehículo (L/100 km)",
                                onValueChange = onVehicleConsumptionChange
                            )
                            PersistentNumericTextField(
                                value = refuelLiters,
                                label = "Litros que vas a repostar",
                                onValueChange = onRefuelLitersChange
                            )
                        }
                    }

                    if (costMode != CostMode.REFUEL_ONLY) {
                        Text(
                            "La ubicación permite filtrar las estaciones por el radio " +
                                "seleccionado. El radio representa distancia en línea recta, " +
                                "no distancia por carretera.",
                            style = MaterialTheme.typography.bodySmall
                        )

                        when (locationStatus) {
                            DeviceLocationStatus.NOT_REQUESTED ->
                                Text("Comparte tu ubicación para buscar estaciones cercanas.")

                            DeviceLocationStatus.LOADING ->
                                Text("Obteniendo ubicación del dispositivo…")

                            DeviceLocationStatus.READY ->
                                Text("Ubicación obtenida. Aplicando el radio de búsqueda.")

                            DeviceLocationStatus.DENIED ->
                                Text(
                                    "No se ha concedido permiso de ubicación. Puedes seguir " +
                                        "consultando precios y utilizar Solo repostaje.",
                                    color = MaterialTheme.colorScheme.error
                                )

                            DeviceLocationStatus.UNAVAILABLE ->
                                Text(
                                    "No se ha podido obtener una ubicación. Comprueba que " +
                                        "la ubicación del dispositivo esté activada."
                                )

                            DeviceLocationStatus.ERROR ->
                                Text(
                                    "Se ha producido un error al obtener la ubicación. " +
                                        "Puedes volver a intentarlo.",
                                    color = MaterialTheme.colorScheme.error
                                )
                        }

                        Button(
                            onClick = onRequestLocation,
                            enabled = locationStatus != DeviceLocationStatus.LOADING,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (locationStatus == DeviceLocationStatus.NOT_REQUESTED) {
                                    "Solicitar ubicación"
                                } else {
                                    "Reintentar ubicación"
                                }
                            )
                        }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Datos de MITECO", style = MaterialTheme.typography.titleMedium)

                    if (uiState.isRefreshing) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator()
                            Text("Actualizando precios y estaciones…")
                        }
                    } else {
                        Text("Consulta de datos finalizada")
                    }

                    uiState.officialDate?.let {
                        Text("Fecha oficial: $it")
                    }

                    uiState.downloadedAtMillis?.takeIf { it > 0 }?.let {
                        val date = DateFormat.getDateTimeInstance(
                            DateFormat.MEDIUM,
                            DateFormat.SHORT,
                            Locale.forLanguageTag("es-ES")
                        ).format(Date(it))
                        Text("Última descarga: $date")
                    }

                    if (uiState.refreshError != null) {
                        Text(
                            "No se pudo actualizar: ${uiState.refreshError}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (uiState.note.isNotBlank()) {
                        Text(uiState.note, style = MaterialTheme.typography.bodySmall)
                    }

                    Button(
                        onClick = onRefresh,
                        enabled = !uiState.isRefreshing
                    ) {
                        Text("Actualizar precios")
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Gasolineras (${matchingStations.size})",
                    style = MaterialTheme.typography.titleLarge
                )

                Box {
                    Button(onClick = { sortMenuExpanded = true }) {
                        Text("Ordenar")
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Precio más bajo") },
                            onClick = {
                                sortOrder = StationSortOrder.PRICE
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Distancia por carretera") },
                            enabled = routeSortingAvailable,
                            onClick = {
                                sortOrder = StationSortOrder.DISTANCE
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Coste total más bajo") },
                            onClick = {
                                sortOrder = StationSortOrder.TOTAL_COST
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Text(
                "Orden actual: ${sortOrder.label}",
                style = MaterialTheme.typography.bodySmall
            )

            if (sortOrder == StationSortOrder.DISTANCE && !routeSortingAvailable) {
                Text(
                    "La ordenación por carretera requiere ubicación y un modo de cálculo de desplazamiento.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (costMode != CostMode.REFUEL_ONLY && routeState.isLoading) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator()
                    Text("Calculando distancias y tiempos por carretera…")
                }
            }
        }
        if (costMode != CostMode.REFUEL_ONLY && routeState.error != null) {
            item {
                Text(
                    "No se pudieron calcular las rutas. Las distancias por carretera no están disponibles.",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        when {
            selectedProduct == null -> {
                item {
                    Text("Selecciona un combustible para ver las estaciones comparables.")
                }
            }

            !selectedProductAvailable -> {
                item {
                    Text("El combustible seleccionado no tiene precios disponibles en esta provincia.")
                }
            }

            costMode != CostMode.REFUEL_ONLY &&
                (originLatitude == null || originLongitude == null) -> {
                item {
                    Text(
                        "Necesitas compartir tu ubicación para mostrar las estaciones " +
                            "dentro del radio seleccionado."
                    )
                }
            }

            matchingStations.isEmpty() -> {
                item {
                    Text("No hay gasolineras con ese combustible dentro del radio seleccionado.")
                }
            }

            else -> {
                items(sortedStations, key = { it.id }) { station ->
                    StationCard(
                        station = station,
                        selectedProduct = selectedProduct,
                        refuelQuantity = refuelQuantity,
                        vehicleConsumption = vehicleConsumptionForProduct,
                        costMode = costMode,
                        route = routeState.routes[station.id],
                        showSavingsComparison = sortOrder == StationSortOrder.TOTAL_COST,
                        comparisonStationId = comparisonStation?.id,
                        comparisonReferenceCost = comparisonReferenceCost
                    )
                }
            }
        }

        if (costMode != CostMode.REFUEL_ONLY) {
            item {
                Text(
                    "Rutas: © OpenStreetMap contributors · OSRM",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private enum class StationSortOrder(val label: String) {
    PRICE("Precio más bajo"),
    DISTANCE("Distancia por carretera"),
    TOTAL_COST("Coste total más bajo")
}

private fun stationTotalCost(
    station: FuelStation,
    selectedProduct: FuelProduct,
    refuelQuantity: String,
    vehicleConsumption: String,
    costMode: CostMode,
    route: StationRoute?
): BigDecimal? {
    val selectedPrice = station.priceFor(selectedProduct) ?: return null
    val refuelCost = CostCalculator.refuelCost(refuelQuantity, selectedPrice)
        ?: return null

    if (costMode == CostMode.REFUEL_ONLY) {
        return refuelCost
    }

    val validRoute = route?.takeIf {
        it.distanceMeters.isFinite() && it.distanceMeters >= 0.0
    } ?: return null

    val travelCost = CostCalculator.travelCost(
        distanceKm = validRoute.distanceKm,
        consumption = vehicleConsumption,
        priceMilli = selectedPrice,
        roundTrip = costMode == CostMode.REFUEL_AND_ROUND_TRIP
    ) ?: return null

    return CostCalculator.totalCost(refuelCost, travelCost)
}

@Composable
private fun StationCard(
    station: FuelStation,
    selectedProduct: FuelProduct,
    refuelQuantity: String,
    vehicleConsumption: String,
    costMode: CostMode,
    route: StationRoute?,
    showSavingsComparison: Boolean,
    comparisonStationId: String?,
    comparisonReferenceCost: BigDecimal?
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                station.brand.ifBlank { "Gasolinera" },
                style = MaterialTheme.typography.titleMedium
            )

            Text(station.address)

            val location = listOf(
                station.locality,
                station.municipality,
                station.provinceName
            ).filter { it.isNotBlank() }.distinct().joinToString(", ")

            if (location.isNotBlank()) {
                Text(location, style = MaterialTheme.typography.bodySmall)
            }

            if (station.hours.isNotBlank()) {
                Text("Horario: ${station.hours}", style = MaterialTheme.typography.bodySmall)
            }

            station.priceFor(selectedProduct)?.let { price ->
                Text("Precio de ${selectedProduct.displayName}", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(selectedProduct.displayName)
                    Text(
                        String.format(
                            Locale.forLanguageTag("es-ES"),
                            "%.3f ${selectedProduct.priceUnit}",
                            price / 1000.0
                        )
                    )
                }
            }

            val selectedPrice = station.priceFor(selectedProduct)
            if (selectedPrice != null) {
                val refuelCost = CostCalculator.refuelCost(refuelQuantity, selectedPrice)
                val currencyFormat = NumberFormat.getCurrencyInstance(
                    Locale.forLanguageTag("es-ES")
                )

                if (refuelCost != null) {
                    Text(
                        if (costMode == CostMode.REFUEL_ONLY) {
                            "Coste del repostaje: ${currencyFormat.format(refuelCost)}"
                        } else {
                            "Coste del repostaje: ${currencyFormat.format(refuelCost)}"
                        },
                        style = MaterialTheme.typography.titleSmall
                    )
                } else {
                    Text(
                        "Introduce una cantidad de repostaje válida para calcular su coste.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (costMode != CostMode.REFUEL_ONLY) {
                    val validRoute = route?.takeIf {
                        it.distanceMeters.isFinite() && it.distanceMeters >= 0.0
                    }

                    if (validRoute == null) {
                        Text(
                            if (route == null) {
                                "Distancia por carretera todavía no disponible."
                            } else {
                                "La distancia de la ruta no es válida."
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        val roundTrip = costMode == CostMode.REFUEL_AND_ROUND_TRIP
                        val distanceMultiplier = if (roundTrip) 2.0 else 1.0
                        val displayedDistance = validRoute.distanceKm * distanceMultiplier
                        val distanceLabel = when {
                            validRoute.isEstimated -> "Distancia estimada"
                            roundTrip -> "Distancia por carretera (ida y vuelta)"
                            else -> "Distancia por carretera (ida)"
                        }

                        Text(
                            "$distanceLabel: ${
                                String.format(
                                    Locale.forLanguageTag("es-ES"),
                                    "%.1f km",
                                    displayedDistance
                                )
                            }"
                        )

                        validRoute.durationSeconds
                            ?.takeIf { it.isFinite() && it >= 0.0 }
                            ?.let { seconds ->
                                val minutes = kotlin.math.ceil(
                                    seconds * distanceMultiplier / 60.0
                                ).toInt()
                                val hoursPart = minutes / 60
                                val minutesPart = minutes % 60
                                val duration = if (hoursPart > 0) {
                                    "${hoursPart} h ${minutesPart} min"
                                } else {
                                    "$minutesPart min"
                                }
                                Text(
                                    if (roundTrip) {
                                        "Tiempo estimado de ida y vuelta: $duration"
                                    } else {
                                        "Tiempo estimado de ida: $duration"
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                        if (validRoute.isEstimated) {
                            Text(
                                "Ruta estimada; no se ha podido obtener la distancia real por carretera.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        val travelCost = CostCalculator.travelCost(
                            distanceKm = validRoute.distanceKm,
                            consumption = vehicleConsumption,
                            priceMilli = selectedPrice,
                            roundTrip = roundTrip
                        )

                        if (travelCost != null) {
                            Text(
                                "Coste del desplazamiento: ${currencyFormat.format(travelCost)}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            val totalCost = CostCalculator.totalCost(refuelCost, travelCost)
                            if (totalCost != null) {
                                Text(
                                    "Coste total: ${currencyFormat.format(totalCost)}",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                if (
                                    showSavingsComparison &&
                                    comparisonReferenceCost != null &&
                                    station.id != comparisonStationId
                                ) {
                                    val difference =
                                        comparisonReferenceCost.subtract(totalCost)

                                    val comparisonText = when {
                                        difference.signum() > 0 ->
                                            "Ahorras ${currencyFormat.format(difference)} frente a la gasolinera más cercana."
                                        difference.signum() < 0 ->
                                            "Cuesta ${currencyFormat.format(difference.abs())} más que ir a la gasolinera más cercana."
                                        else ->
                                            "El coste es igual al de la gasolinera más cercana."
                                    }

                                    Text(
                                        comparisonText,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            } else {
                                Text(
                                    "Introduce una cantidad de repostaje válida para calcular el coste total.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        } else {
                            Text(
                                "Introduce un consumo válido para calcular el coste del desplazamiento.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersistentNumericTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit
) {
    var localValue by remember { mutableStateOf(value) }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(value, isFocused) {
        if (!isFocused && localValue != value) {
            localValue = value
        }
    }

    OutlinedTextField(
        value = localValue,
        onValueChange = { newValue ->
            localValue = newValue
            onValueChange(newValue)
        },
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            },
        label = { Text(label) },
        singleLine = true
    )
}
