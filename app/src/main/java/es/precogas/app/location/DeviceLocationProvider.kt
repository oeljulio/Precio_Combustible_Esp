package es.precogas.app.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

enum class DeviceLocationStatus {
    NOT_REQUESTED,
    LOADING,
    READY,
    DENIED,
    UNAVAILABLE,
    ERROR
}

class DeviceLocationProvider(context: Context) {

    private val appContext = context.applicationContext

    @Suppress("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        val fineGranted = appContext.checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = appContext.checkSelfPermission(
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            throw SecurityException("No se ha concedido permiso de ubicación.")
        }

        val manager = appContext.getSystemService(
            Context.LOCATION_SERVICE
        ) as LocationManager

        val allowedProviders = if (fineGranted) {
            setOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER
            )
        } else {
            setOf(LocationManager.NETWORK_PROVIDER)
        }

        val providers = manager.getProviders(true).filter { provider ->
            provider in allowedProviders
        }

        if (providers.isEmpty()) return null

        val now = System.currentTimeMillis()

        val recentLocation = providers.mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }
            .filter { now - it.time in 0..120_000 }
            .minByOrNull { it.accuracy }

        if (recentLocation != null) return recentLocation

        return withTimeoutOrNull(15_000L) {
            suspendCancellableCoroutine { continuation ->
                val registrationLock = Any()
                var cancelled = false

                lateinit var listener: LocationListener
                listener = LocationListener { location ->
                    synchronized(registrationLock) {
                        cancelled = true
                        runCatching { manager.removeUpdates(listener) }
                    }
                    if (continuation.isActive) {
                        continuation.resume(location)
                    }
                }

                continuation.invokeOnCancellation {
                    synchronized(registrationLock) {
                        cancelled = true
                        runCatching { manager.removeUpdates(listener) }
                    }
                }

                try {
                    synchronized(registrationLock) {
                        if (!cancelled) {
                            providers.forEach { provider ->
                                manager.requestLocationUpdates(
                                    provider,
                                    1_000L,
                                    0f,
                                    listener,
                                    Looper.getMainLooper()
                                )
                            }
                        }
                    }
                } catch (error: SecurityException) {
                    synchronized(registrationLock) {
                        cancelled = true
                        runCatching { manager.removeUpdates(listener) }
                    }
                    throw error
                }
            }
        }
    }
}
