package es.precogas.app.data.routing

import es.precogas.app.data.model.FuelStation
import es.precogas.app.data.model.GeoDistance
import es.precogas.app.data.model.StationRoute
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class OsrmRoutingClient(
    private val connectionFactory: (URL) -> HttpURLConnection = { url ->
        url.openConnection() as HttpURLConnection
    }
) {

    companion object {
        private const val BASE_URL = "https://router.project-osrm.org/table/v1/driving/"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 20_000
        private const val MAX_DESTINATIONS_PER_REQUEST = 40
        private const val MAX_ATTEMPTS = 3
        private const val MIN_REQUEST_INTERVAL_MS = 1_000L
        private const val ESTIMATION_FACTOR = 1.3

        private val requestMutex = kotlinx.coroutines.sync.Mutex()
        private var lastRequestAtNanos = 0L
    }

    suspend fun fetchRoutes(
        originLatitude: Double,
        originLongitude: Double,
        stations: List<FuelStation>
    ): Map<String, StationRoute> = withContext(Dispatchers.IO) {
        require(
            originLatitude.isFinite() && originLatitude in -90.0..90.0 &&
                originLongitude.isFinite() && originLongitude in -180.0..180.0
        ) { "Las coordenadas de origen no son válidas." }

        val validStations = stations.filter { station ->
            station.latitude?.let { it.isFinite() && it in -90.0..90.0 } == true &&
                station.longitude?.let { it.isFinite() && it in -180.0..180.0 } == true
        }

        val result = linkedMapOf<String, StationRoute>()

        validStations.chunked(MAX_DESTINATIONS_PER_REQUEST).forEach { batch ->
            var response: Map<String, StationRoute>? = null
            var attempt = 0

            while (attempt < MAX_ATTEMPTS && response == null) {
                currentCoroutineContext().ensureActive()
                attempt++

                try {
                    requestMutex.lock()
                    try {
                        if (lastRequestAtNanos != 0L) {
                            val elapsed = (System.nanoTime() - lastRequestAtNanos) / 1_000_000L
                            val waitMillis = MIN_REQUEST_INTERVAL_MS - elapsed
                            if (waitMillis > 0) delay(waitMillis)
                        }

                        lastRequestAtNanos = System.nanoTime()
                        response = fetchBatch(
                            originLatitude,
                            originLongitude,
                            batch
                        )
                        currentCoroutineContext().ensureActive()
                    } finally {
                        requestMutex.unlock()
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    if (attempt == MAX_ATTEMPTS) {
                        response = emptyMap()
                    }
                }
            }

            batch.forEach { station ->
                val route = response?.get(station.id)
                    ?: estimatedRoute(
                        originLatitude,
                        originLongitude,
                        station
                    )

                if (route != null) {
                    result[station.id] = route
                }
            }
        }

        result
    }

    private suspend fun fetchBatch(
        originLatitude: Double,
        originLongitude: Double,
        stations: List<FuelStation>
    ): Map<String, StationRoute> = suspendCancellableCoroutine { continuation ->
        val activeConnection = AtomicReference<HttpURLConnection?>(null)

        continuation.invokeOnCancellation {
            activeConnection.getAndSet(null)?.disconnect()
        }

        try {
            val coordinates = buildList {
                add("$originLongitude,$originLatitude")
                stations.forEach { station ->
                    add("${station.longitude},${station.latitude}")
                }
            }.joinToString(";")

            val destinations = stations.indices.joinToString(";") { (it + 1).toString() }
            val url = URL(
                "$BASE_URL$coordinates?sources=0" +
                    "&destinations=$destinations" +
                    "&annotations=distance,duration"
            )

            val connection = connectionFactory(url).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "PreCoGas/1.0 Android")
            }

            activeConnection.set(connection)

            if (!continuation.isActive) {
                activeConnection.compareAndSet(connection, null)
                connection.disconnect()
                return@suspendCancellableCoroutine
            }

            val result = try {
                val status = connection.responseCode
                if (status !in 200..299) {
                    throw IOException("OSRM respondió con HTTP $status")
                }

                val body = connection.inputStream
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }

                val root = JSONObject(body)
                if (root.optString("code") != "Ok") {
                    throw IOException("OSRM devolvió una respuesta no válida.")
                }

                val distances = root.optJSONArray("distances")
                    ?.optJSONArray(0)
                    ?: throw IOException("OSRM no devolvió las distancias.")

                val durations = root.optJSONArray("durations")
                    ?.optJSONArray(0)

                buildMap {
                    stations.forEachIndexed { index, station ->
                        val distance = distances.optDouble(index, Double.NaN)
                        if (distance.isFinite() && distance >= 0.0) {
                            val duration = durations
                                ?.optDouble(index, Double.NaN)
                                ?.takeIf { it.isFinite() && it >= 0.0 }

                            put(
                                station.id,
                                StationRoute(
                                    distanceMeters = distance,
                                    durationSeconds = duration,
                                    isEstimated = false
                                )
                            )
                        }
                    }
                }
            } finally {
                activeConnection.compareAndSet(connection, null)
                connection.disconnect()
            }

            continuation.resume(result)
        } catch (error: Exception) {
            if (continuation.isActive) {
                continuation.resumeWith(Result.failure(error))
            }
        }
    }

    private fun estimatedRoute(
        originLatitude: Double,
        originLongitude: Double,
        station: FuelStation
    ): StationRoute? {
        val straightDistance = GeoDistance.distanceKm(
            originLatitude,
            originLongitude,
            station.latitude,
            station.longitude
        ) ?: return null

        return StationRoute(
            distanceMeters = straightDistance * 1000.0 * ESTIMATION_FACTOR,
            durationSeconds = null,
            isEstimated = true
        )
    }
}
