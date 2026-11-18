package es.precogas.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoDistanceTest {

    @Test
    fun distanceBetweenIdenticalCoordinatesIsZero() {
        val distance = GeoDistance.distanceKm(40.4168, -3.7038, 40.4168, -3.7038)

        assertEquals(0.0, distance!!, 0.000001)
    }

    @Test
    fun calculatesDistanceForOneDegreeAtEquator() {
        val distance = GeoDistance.distanceKm(0.0, 0.0, 0.0, 1.0)

        assertEquals(111.195, distance!!, 0.01)
    }

    @Test
    fun calculatesDistanceBetweenMadridAndBarcelona() {
        val distance = GeoDistance.distanceKm(40.4168, -3.7038, 41.3874, 2.1686)

        assertEquals(504.0, distance!!, 2.0)
    }

    @Test
    fun rejectsMissingOrInvalidCoordinates() {
        assertNull(GeoDistance.distanceKm(null, 0.0, 0.0, 0.0))
        assertNull(GeoDistance.distanceKm(0.0, null, 0.0, 0.0))
        assertNull(GeoDistance.distanceKm(91.0, 0.0, 0.0, 0.0))
        assertNull(GeoDistance.distanceKm(0.0, 181.0, 0.0, 0.0))
        assertNull(GeoDistance.distanceKm(Double.NaN, 0.0, 0.0, 0.0))
        assertNull(GeoDistance.distanceKm(0.0, 0.0, 0.0, Double.POSITIVE_INFINITY))
    }

    @Test
    fun includesDistanceAtRadiusBoundary() {
        assertTrue(GeoDistance.isWithinRadius(25.0, 25))
        assertTrue(GeoDistance.isWithinRadius(24.99, 25))
    }

    @Test
    fun rejectsDistanceOutsideRadiusOrInvalidValues() {
        assertFalse(GeoDistance.isWithinRadius(25.01, 25))
        assertFalse(GeoDistance.isWithinRadius(null, 25))
        assertFalse(GeoDistance.isWithinRadius(-1.0, 25))
        assertFalse(GeoDistance.isWithinRadius(Double.NaN, 25))
        assertFalse(GeoDistance.isWithinRadius(10.0, 0))
        assertFalse(GeoDistance.isWithinRadius(10.0, -25))
    }
}
