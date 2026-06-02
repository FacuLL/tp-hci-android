package tp3.grupo1.hci.itba.edu.ar

import android.app.Application

class LuminaApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notificationHelper.createChannels()
    }
}
