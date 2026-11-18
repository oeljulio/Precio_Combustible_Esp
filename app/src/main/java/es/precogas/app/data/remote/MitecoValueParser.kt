package es.precogas.app.data.remote

import java.math.BigDecimal
import java.math.RoundingMode

internal object MitecoValueParser {

    fun parsePrice(raw: String): Int? {
        if (raw.isBlank()) return null

        val decimal = try {
            BigDecimal(raw.replace(',', '.'))
        } catch (error: NumberFormatException) {
            throw MitecoException(
                "MITECO ha devuelto un precio con formato no válido: $raw",
                error
            )
        }

        if (decimal <= BigDecimal.ZERO) {
            throw MitecoException(
                "MITECO ha devuelto un precio igual o inferior a cero."
            )
        }

        return try {
            decimal
                .setScale(3, RoundingMode.HALF_UP)
                .movePointRight(3)
                .intValueExact()
        } catch (error: ArithmeticException) {
            throw MitecoException(
                "No se puede representar el precio recibido.",
                error
            )
        }
    }

    fun parseCoordinate(
        raw: String,
        field: String,
        minimum: Double,
        maximum: Double
    ): Double? {
        if (raw.isBlank()) return null

        val coordinate = raw.replace(',', '.').toDoubleOrNull()
            ?: throw MitecoException(
                "MITECO ha devuelto una $field con formato no válido."
            )

        if (!coordinate.isFinite() || coordinate !in minimum..maximum) {
            throw MitecoException(
                "MITECO ha devuelto una $field fuera de rango."
            )
        }

        return coordinate
    }
}
