package ca.maplenetwork.openautomate

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.IOException

class PapaModeTileService : TileService() {
    private val deviceStates by lazy { DeviceStates(applicationContext) }
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val listeners = mutableListOf<Pair<StateManager, StateListener>>()

    private val prefs by lazy { getSharedPreferences("papa_mode", MODE_PRIVATE) }
    private val disableResponse : PapaModeDisableResponse
        get() {
            val mode = prefs.getString("disable_response", "predefined")
            return when (mode) {
                "predefined" -> PapaModeDisableResponse.REVERT_TO_PREDEFINED_STATES
                "previous" -> PapaModeDisableResponse.REVERT_TO_PREVIOUS_STATES
                else -> PapaModeDisableResponse.REVERT_TO_PREDEFINED_STATES
            }
        }


    override fun onClick() {
        super.onClick()
        qsTile?.apply {
            state = Tile.STATE_UNAVAILABLE
            updateTile()
        }

        ioScope.launch {
            try {
                if (isInPapaMode()) {
                    when (disableResponse) {
                        PapaModeDisableResponse.REVERT_TO_PREDEFINED_STATES -> revertToPredefinedStates()
                        PapaModeDisableResponse.REVERT_TO_PREVIOUS_STATES -> revertToPreviousStates()
                    }
                } else {
                    if (disableResponse == PapaModeDisableResponse.REVERT_TO_PREVIOUS_STATES) {
                        saveCurrentStates()
                    }
                    applyPapaStates()
                }
            } catch (e: IOException) {
                Log.e("PapaModeTile", "Shell.exec failed", e)
            }
        }
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

    private fun revertToPredefinedStates() {
        DeviceStates.getAvailableStateOptions().forEach { option ->
            val value = prefs.getBoolean("default_${option.key}", false)
            when (option.key) {
                "airplane" -> deviceStates.airplane.set(value)
                "wifi"     -> deviceStates.wifi.set(value)
                "bluetooth" -> deviceStates.bluetooth.set(value)
                "location" -> deviceStates.location.set(value)
                "wifiScan" -> deviceStates.wifiScanning.set(value)
                "btScan" -> deviceStates.bluetoothScanning.set(value)
                "mobileData" -> deviceStates.mobileData?.set(value)
            }
        }
    }

    private fun revertToPreviousStates() {
        val prefs = getSharedPreferences("papa_mode", MODE_PRIVATE)
        deviceStates.airplane.set(prefs.getBoolean("previous_airplane",false))
        deviceStates.wifi.set(prefs.getBoolean("previous_wifi",true))
        deviceStates.bluetooth.set(prefs.getBoolean("previous_bluetooth",false))
        deviceStates.location.set(prefs.getBoolean("previous_location",true))
        deviceStates.wifiScanning.set(prefs.getBoolean("previous_wifiScan",true))
        deviceStates.bluetoothScanning.set(prefs.getBoolean("previous_btScan",true))
        deviceStates.mobileData?.set(prefs.getBoolean("previous_mobileData",true))
    }

    private fun saveCurrentStates() {
        val prefs = getSharedPreferences("papa_mode", MODE_PRIVATE)
        with(prefs.edit()) {
            putBoolean("previous_airplane",    deviceStates.airplane.get())
            putBoolean("previous_wifi",        deviceStates.wifi.get())
            putBoolean("previous_bluetooth",   deviceStates.bluetooth.get())
            putBoolean("previous_location",    deviceStates.location.get())
            putBoolean("previous_wifiScan",    deviceStates.wifiScanning.get())
            putBoolean("previous_btScan",      deviceStates.bluetoothScanning.get())
            putBoolean("previous_mobileData",  deviceStates.mobileData?.get() ?: false)
            apply()
        }
    }



    private fun updateTile() {
        val inPapaMode = isInPapaMode()
        qsTile?.apply {
            state = if (inPapaMode) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            icon = Icon.createWithResource(this@PapaModeTileService,
                if (isInPapaMode())
                    R.drawable.cloud_off_24dp_e8eaed_fill0_wght400_grad0_opsz24
                else
                    R.drawable.cloud_24dp_e8eaed_fill0_wght400_grad0_opsz24
            )
            updateTile()
        }
    }
}

enum class PapaModeDisableResponse {
    REVERT_TO_PREVIOUS_STATES,
    REVERT_TO_PREDEFINED_STATES
}
