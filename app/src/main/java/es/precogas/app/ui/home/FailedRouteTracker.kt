package es.precogas.app.ui.home

/**
 * Gestiona el bloqueo de una clave de rutas que ya ha fallado.
 *
 * Una clave distinta libera el fallo anterior antes de que el llamador
 * consulte la caché o inicie una nueva petición.
 */
internal class FailedRouteTracker {

    private var failedKey: String? = null
    private var failedError: String? = null

    fun errorFor(key: String): String? {
        if (key == failedKey) {
            return failedError ?: "No se pudieron calcular las rutas."
        }

        failedKey = null
        failedError = null
        return null
    }

    fun recordFailure(key: String, error: String) {
        failedKey = key
        failedError = error
    }

    fun clear() {
        failedKey = null
        failedError = null
    }
}
