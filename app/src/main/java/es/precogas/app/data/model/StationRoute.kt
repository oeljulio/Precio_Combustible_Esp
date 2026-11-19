package es.precogas.app.data.model

data class StationRoute(
    val distanceMeters: Double,
    val durationSeconds: Double?,
    val isEstimated: Boolean,
    val returnDistanceMeters: Double? = null,
    val returnDurationSeconds: Double? = null,
    val outboundRequestFailed: Boolean = false,
    val returnRequestFailed: Boolean = false
) {
    val distanceKm: Double
        get() = distanceMeters / 1000.0

    val roundTripDistanceMeters: Double?
        get() {
            if (isEstimated) return null

            val outbound = distanceMeters
                .takeIf { it.isFinite() && it >= 0.0 }
                ?: return null
            val returning = returnDistanceMeters
                ?.takeIf { it.isFinite() && it >= 0.0 }
                ?: return null

            return outbound + returning
        }

    val roundTripDistanceKm: Double?
        get() = roundTripDistanceMeters?.div(1000.0)

    val roundTripDurationSeconds: Double?
        get() {
            if (isEstimated) return null

            val outbound = durationSeconds
                ?.takeIf { it.isFinite() && it >= 0.0 }
                ?: return null
            val returning = returnDurationSeconds
                ?.takeIf { it.isFinite() && it >= 0.0 }
                ?: return null

            return outbound + returning
        }
}
