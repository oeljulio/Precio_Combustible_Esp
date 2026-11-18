package es.precogas.app

import android.app.Application
import android.content.Context
import es.precogas.app.data.local.PreCoGasDatabase
import es.precogas.app.data.remote.MitecoClient
import es.precogas.app.data.repository.FuelRepository
import es.precogas.app.data.routing.OsrmRoutingClient

class PreCoGasApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(context: Context) {

    val database = PreCoGasDatabase.getInstance(context)

    val mitecoClient = MitecoClient()
    val osrmRoutingClient = OsrmRoutingClient()

    val fuelRepository = FuelRepository(
        dao = database.preCoGasDao(),
        mitecoClient = mitecoClient
    )

    val themePreferences = ThemePreferences(context)
}
