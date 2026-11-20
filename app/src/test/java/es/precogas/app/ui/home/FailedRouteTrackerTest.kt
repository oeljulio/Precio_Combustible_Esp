package es.precogas.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FailedRouteTrackerTest {

    @Test
    fun sameFailedKeyRemainsBlocked() {
        val tracker = FailedRouteTracker()
        tracker.recordFailure("K1", "Error K1")

        assertEquals("Error K1", tracker.errorFor("K1"))
        assertEquals("Error K1", tracker.errorFor("K1"))
    }

    @Test
    fun differentKeyInvalidatesPreviousFailure() {
        val tracker = FailedRouteTracker()
        tracker.recordFailure("K1", "Error K1")

        assertNull(tracker.errorFor("K2"))
        assertNull(tracker.errorFor("K1"))
    }

    @Test
    fun newFailureReplacesPreviousKey() {
        val tracker = FailedRouteTracker()
        tracker.recordFailure("K1", "Error K1")

        assertNull(tracker.errorFor("K2"))
        tracker.recordFailure("K2", "Error K2")

        assertEquals("Error K2", tracker.errorFor("K2"))
        assertNull(tracker.errorFor("K1"))
    }

    @Test
    fun explicitClearAllowsRetryOfSameKey() {
        val tracker = FailedRouteTracker()
        tracker.recordFailure("K1", "Error K1")

        tracker.clear()

        assertNull(tracker.errorFor("K1"))
    }
}
