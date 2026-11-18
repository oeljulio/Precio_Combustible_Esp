package es.precogas.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MitecoValueParserTest {

    @Test
    fun convertsCommaDecimalPriceToThousandths() {
        assertEquals(1409, MitecoValueParser.parsePrice("1,409"))
    }

    @Test
    fun convertsDotDecimalPriceToThousandths() {
        assertEquals(1609, MitecoValueParser.parsePrice("1.609"))
    }

    @Test
    fun roundsPriceToThreeDecimalsHalfUp() {
        assertEquals(1410, MitecoValueParser.parsePrice("1,4095"))
    }

    @Test
    fun emptyPriceIsUnavailable() {
        assertNull(MitecoValueParser.parsePrice(""))
        assertNull(MitecoValueParser.parsePrice("   "))
    }

    @Test(expected = MitecoException::class)
    fun rejectsZeroPrice() {
        MitecoValueParser.parsePrice("0")
    }

    @Test(expected = MitecoException::class)
    fun rejectsNegativePrice() {
        MitecoValueParser.parsePrice("-1,20")
    }

    @Test(expected = MitecoException::class)
    fun rejectsInvalidPrice() {
        MitecoValueParser.parsePrice("abc")
    }

    @Test(expected = MitecoException::class)
    fun rejectsPriceOutsideIntegerRange() {
        MitecoValueParser.parsePrice("999999999999")
    }

    @Test
    fun parsesCommaDecimalCoordinate() {
        assertEquals(
            28.4636,
            MitecoValueParser.parseCoordinate(
                "28,4636", "latitud", -90.0, 90.0
            )!!,
            0.000001
        )
    }

    @Test
    fun emptyCoordinateIsNull() {
        assertNull(
            MitecoValueParser.parseCoordinate(
                "", "latitud", -90.0, 90.0
            )
        )
    }

    @Test
    fun acceptsCoordinateRangeBoundaries() {
        assertEquals(
            90.0,
            MitecoValueParser.parseCoordinate(
                "90", "latitud", -90.0, 90.0
            )!!,
            0.0
        )
        assertEquals(
            -180.0,
            MitecoValueParser.parseCoordinate(
                "-180", "longitud", -180.0, 180.0
            )!!,
            0.0
        )
    }

    @Test(expected = MitecoException::class)
    fun rejectsLatitudeOutsideRange() {
        MitecoValueParser.parseCoordinate(
            "90,1", "latitud", -90.0, 90.0
        )
    }

    @Test(expected = MitecoException::class)
    fun rejectsLongitudeOutsideRange() {
        MitecoValueParser.parseCoordinate(
            "-180,1", "longitud", -180.0, 180.0
        )
    }

    @Test(expected = MitecoException::class)
    fun rejectsNonNumericCoordinate() {
        MitecoValueParser.parseCoordinate(
            "Norte", "latitud", -90.0, 90.0
        )
    }

    @Test(expected = MitecoException::class)
    fun rejectsInfiniteCoordinate() {
        MitecoValueParser.parseCoordinate(
            "Infinity", "latitud", -90.0, 90.0
        )
    }
}
