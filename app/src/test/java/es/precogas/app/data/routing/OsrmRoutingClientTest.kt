package es.precogas.app.data.routing

import es.precogas.app.data.model.FuelStation
import es.precogas.app.data.model.GeoDistance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class OsrmRoutingClientTest {

    private fun station(
        id: String = "station-1",
        latitude: Double? = 40.1,
        longitude: Double? = -3.1
    ) = FuelStation(
        id = id,
        provinceId = "28",
        postalCode = "28001",
        address = "Calle de prueba",
        hours = "",
        locality = "Madrid",
        latitude = latitude,
        longitude = longitude,
        margin = "",
        municipality = "Madrid",
        provinceName = "Madrid",
        brand = "Prueba",
        saleType = "",
        prices = emptyMap()
    )

    private class FakeConnection(
        url: URL,
        private val status: Int = 200,
        private val body: String = """{"code":"Ok","distances":[[1234.5]],"durations":[[90.0]]}""",
        private val entered: CountDownLatch? = null,
        private val release: CountDownLatch? = null
    ) : HttpURLConnection(url) {

        var disconnected = false
            private set

        override fun connect() = Unit

        override fun disconnect() {
            disconnected = true
            release?.countDown()
        }

        override fun usingProxy(): Boolean = false

        override fun getResponseCode(): Int {
            entered?.countDown()
            release?.await(5, TimeUnit.SECONDS)
            return status
        }

        override fun getInputStream() =
            ByteArrayInputStream(body.toByteArray(Charsets.UTF_8))
    }

    @Test
    fun parsesValidOsrmResponse() = runBlocking {
        val client = OsrmRoutingClient { url -> FakeConnection(url) }

        val routes = client.fetchRoutes(40.0, -3.0, listOf(station()))

        val route = routes["station-1"]
        assertTrue(route != null)
        assertEquals(1234.5, route!!.distanceMeters, 0.001)
        assertEquals(90.0, route.durationSeconds!!, 0.001)
        assertFalse(route.isEstimated)
    }

    @Test
    fun calculatesAsymmetricRoundTripFromBothOsrmDirections() = runBlocking {
        val client = OsrmRoutingClient { url ->
            val reverse = url.query?.contains("sources=1") == true
            val body = if (reverse) {
                """{"code":"Ok","distances":[[1450.0]],"durations":[[100.0]]}"""
            } else {
                """{"code":"Ok","distances":[[1234.5]],"durations":[[90.0]]}"""
            }
            FakeConnection(url, body = body)
        }

        val routes = client.fetchRoutes(40.0, -3.0, listOf(station()))
        val route = routes["station-1"]

        assertTrue(route != null)
        assertEquals(1234.5, route!!.distanceMeters, 0.001)
        assertEquals(90.0, route.durationSeconds!!, 0.001)
        assertEquals(1450.0, route.returnDistanceMeters!!, 0.001)
        assertEquals(100.0, route.returnDurationSeconds!!, 0.001)
        assertEquals(2684.5, route.roundTripDistanceMeters!!, 0.001)
        assertEquals(2.6845, route.roundTripDistanceKm!!, 0.000001)
        assertEquals(190.0, route.roundTripDurationSeconds!!, 0.001)
        assertFalse(route.outboundRequestFailed)
        assertFalse(route.returnRequestFailed)
    }

    @Test
    fun keepsReturnDistanceWhenReturnDurationIsMissing() = runBlocking {
        val client = OsrmRoutingClient { url ->
            val reverse = url.query?.contains("sources=1") == true
            val body = if (reverse) {
                """{"code":"Ok","distances":[[1450.0]]}"""
            } else {
                """{"code":"Ok","distances":[[1234.5]],"durations":[[90.0]]}"""
            }
            FakeConnection(url, body = body)
        }

        val routes = client.fetchRoutes(40.0, -3.0, listOf(station()))
        val route = routes["station-1"]

        assertTrue(route != null)
        assertEquals(2684.5, route!!.roundTripDistanceMeters!!, 0.001)
        assertNull(route.returnDurationSeconds)
        assertNull(route.roundTripDurationSeconds)
    }

    @Test
    fun keepsOutboundRouteWhenReturnRequestFails() = runBlocking {
        val calls = AtomicInteger()
        val client = OsrmRoutingClient { url ->
            calls.incrementAndGet()
            if (url.query?.contains("sources=1") == true) {
                FakeConnection(url, status = 503)
            } else {
                FakeConnection(url)
            }
        }

        val routes = client.fetchRoutes(40.0, -3.0, listOf(station()))
        val route = routes["station-1"]

        assertEquals(4, calls.get())
        assertTrue(route != null)
        assertEquals(1234.5, route!!.distanceMeters, 0.001)
        assertEquals(90.0, route.durationSeconds!!, 0.001)
        assertNull(route.returnDistanceMeters)
        assertNull(route.returnDurationSeconds)
        assertNull(route.roundTripDistanceMeters)
        assertNull(route.roundTripDurationSeconds)
        assertFalse(route.isEstimated)
        assertFalse(route.outboundRequestFailed)
        assertTrue(route.returnRequestFailed)
    }

    @Test
    fun retriesHttpErrorsAndUsesEstimatedRoute() = runBlocking {
        val calls = AtomicInteger()
        val client = OsrmRoutingClient { url ->
            calls.incrementAndGet()
            FakeConnection(url, status = 503)
        }

        val routes = client.fetchRoutes(40.0, -3.0, listOf(station()))

        assertEquals(3, calls.get())
        val route = routes["station-1"]
        assertTrue(route != null)
        val straightDistance = GeoDistance.distanceKm(
            40.0, -3.0, 40.1, -3.1
        )!!
        assertEquals(straightDistance * 1300.0, route!!.distanceMeters, 0.01)
        assertTrue(route.isEstimated)
        assertNull(route.durationSeconds)
        assertTrue(route.outboundRequestFailed)
        assertFalse(route.returnRequestFailed)
    }

    @Test
    fun retriesInvalidJsonAndUsesEstimatedRoute() = runBlocking {
        val calls = AtomicInteger()
        val client = OsrmRoutingClient { url ->
            calls.incrementAndGet()
            FakeConnection(url, body = """{"code":"Ok"}""")
        }

        val routes = client.fetchRoutes(40.0, -3.0, listOf(station()))

        assertEquals(3, calls.get())
        assertTrue(routes["station-1"]?.isEstimated == true)
        assertTrue(routes["station-1"]?.outboundRequestFailed == true)
        assertFalse(routes["station-1"]?.returnRequestFailed == true)
    }

    @Test
    fun ignoresStationsWithInvalidCoordinates() = runBlocking {
        val calls = AtomicInteger()
        val client = OsrmRoutingClient { url ->
            calls.incrementAndGet()
            FakeConnection(url)
        }

        val routes = client.fetchRoutes(
            40.0,
            -3.0,
            listOf(station(latitude = null))
        )

        assertTrue(routes.isEmpty())
        assertEquals(0, calls.get())
    }

    @Test
    fun cancellationDisconnectsActiveConnection() = runBlocking {
        val entered = CountDownLatch(1)
        val disconnected = CountDownLatch(1)

        val client = OsrmRoutingClient { url ->
            FakeConnection(
                url = url,
                entered = entered,
                release = disconnected
            )
        }

        val request = async {
            client.fetchRoutes(40.0, -3.0, listOf(station()))
        }

        val requestStarted = withContext(Dispatchers.IO) {
            entered.await(5, TimeUnit.SECONDS)
        }

        assertTrue("La petición no llegó a iniciarse", requestStarted)

        request.cancelAndJoin()

        assertTrue(
            "La conexión activa no fue desconectada",
            disconnected.await(2, TimeUnit.SECONDS)
        )
    }
}
