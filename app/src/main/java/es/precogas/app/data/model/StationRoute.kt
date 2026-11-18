package es.precogas.app.data.model

data class StationRoute(
    val distanceMeters: Double,
    val durationSeconds: Double?,
    val isEstimated: Boolean
) {
    val distanceKm: Double
        get() = distanceMeters / 1000.0
}
