package es.precogas.app.data.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoDistance {

    private const val EARTH_RADIUS_KM = 6371.0088

    fun distanceKm(
        latitude1: Double?,
        longitude1: Double?,
        latitude2: Double?,
        longitude2: Double?
    ): Double? {
        if (
            !isValidCoordinate(latitude1, longitude1) ||
            !isValidCoordinate(latitude2, longitude2)
        ) {
            return null
        }

        val lat1 = Math.toRadians(latitude1!!)
        val lon1 = Math.toRadians(longitude1!!)
        val lat2 = Math.toRadians(latitude2!!)
        val lon2 = Math.toRadians(longitude2!!)

        val deltaLat = lat2 - lat1
        val deltaLon = lon2 - lon1

        val haversine = sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(lat1) * cos(lat2) *
            sin(deltaLon / 2) * sin(deltaLon / 2)

        val centralAngle = 2 * atan2(
            sqrt(haversine.coerceIn(0.0, 1.0)),
            sqrt((1.0 - haversine).coerceIn(0.0, 1.0))
        )

        return EARTH_RADIUS_KM * centralAngle
    }

    fun isWithinRadius(distanceKm: Double?, radiusKm: Int): Boolean {
        return distanceKm != null &&
            distanceKm.isFinite() &&
            distanceKm >= 0.0 &&
            radiusKm > 0 &&
            distanceKm <= radiusKm.toDouble()
    }

    private fun isValidCoordinate(
        latitude: Double?,
        longitude: Double?
    ): Boolean {
        return latitude != null &&
            longitude != null &&
            latitude.isFinite() &&
            longitude.isFinite() &&
            latitude in -90.0..90.0 &&
            longitude in -180.0..180.0
    }
}
