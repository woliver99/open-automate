package ca.maplenetwork.openautomate

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log

class PapaModeTileService : TileService() {
    private lateinit var deviceStates: DeviceStates
    private val listeners = mutableListOf<Pair<StateManager, StateListener>>()

    override fun onCreate() {
        super.onCreate()
        deviceStates = DeviceStates(applicationContext)
    }

    override fun onClick() {
        super.onClick()
        if (isInPapaMode()) {
            revertToUserDefaults()
        } else {
            saveUserDefaults()
            applyPapaStates()
        }
        updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        listOfNotNull(
            deviceStates.airplane,
            deviceStates.wifi,
            deviceStates.bluetooth,
            deviceStates.location,
            deviceStates.wifiScanning,
            deviceStates.bluetoothScanning,
            deviceStates.mobileData
        ).forEach { stateManager ->
            val listener = StateListener { updateTile() }
            stateManager.addListener(listener)
            listeners += stateManager to listener
        }
        updateTile()
    }

    override fun onStopListening() {
        super.onStopListening()
        listeners.forEach { (stateManager, listener) ->
            stateManager.removeListener(listener)
        }
        listeners.clear()
    }


    private fun isInPapaMode(): Boolean {
        // Papa Mode = Airplane ON, everything else OFF
        val on  = deviceStates.airplane.get()
        val off = (!deviceStates.wifi.get()
                && !deviceStates.bluetooth.get()
                && !deviceStates.location.get()
                && !deviceStates.wifiScanning.get()
                && !deviceStates.bluetoothScanning.get())
                && !(deviceStates.mobileData?.get() ?: false)
        return on && off
    }

    private fun applyPapaStates() {
        deviceStates.airplane.set(true)
        deviceStates.wifi.set(false)
        deviceStates.bluetooth.set(false)
        deviceStates.location.set(false)
        deviceStates.wifiScanning.set(false)
        deviceStates.bluetoothScanning.set(false)
        deviceStates.mobileData?.set(false)
    }

    private fun saveUserDefaults() {
        val prefs = getSharedPreferences("papa_mode", MODE_PRIVATE)
        with(prefs.edit()) {
            putBoolean("def_airplane",    deviceStates.airplane.get())
            putBoolean("def_wifi",        deviceStates.wifi.get())
            putBoolean("def_bluetooth",   deviceStates.bluetooth.get())
            putBoolean("def_location",    deviceStates.location.get())
            putBoolean("def_wifiScan",    deviceStates.wifiScanning.get())
            putBoolean("def_btScan",      deviceStates.bluetoothScanning.get())
            putBoolean("def_mobileData",  deviceStates.mobileData?.get() ?: false)
            apply()
        }
    }

    private fun revertToUserDefaults() {
        val prefs = getSharedPreferences("papa_mode", MODE_PRIVATE)
        deviceStates.airplane.set(   prefs.getBoolean("def_airplane",    false))
        deviceStates.wifi.set(       prefs.getBoolean("def_wifi",        true))
        deviceStates.bluetooth.set(  prefs.getBoolean("def_bluetooth",   false))
        deviceStates.location.set(   prefs.getBoolean("def_location",    true))
        deviceStates.wifiScanning.set(prefs.getBoolean("def_wifiScan",   true))
        deviceStates.bluetoothScanning.set(prefs.getBoolean("def_btScan",true))
        deviceStates.mobileData?.set(prefs.getBoolean("def_mobileData",  true))
    }

    private fun updateTile() {
        qsTile?.apply {
            state = if (isInPapaMode()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "Papa Mode"
            updateTile()
        }
    }
}
