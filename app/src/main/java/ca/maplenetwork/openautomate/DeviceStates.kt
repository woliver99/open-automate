package ca.maplenetwork.openautomate

import android.content.Context
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.net.toUri

class DeviceStates(context: Context) {
    private val appContext = context.applicationContext

    val airplane by lazy {
        val key = Settings.Global.AIRPLANE_MODE_ON
        StateManager(
            context   = appContext,
            getState  = { Shell.exec("cmd connectivity airplane-mode").contains("enabled") },
            setState  = { on -> Shell.exec("cmd connectivity airplane-mode ${if (on) "enable" else "disable"}") },
            source    = UriSource(Settings.Global.getUriFor(key))
        )
    }

    val wifi by lazy {
        val key = Settings.Global.WIFI_ON
        Log.d(TAG, "wifi: $key")
        StateManager(
            context   = appContext,
            getState  = {
                val value =Shell.exec("settings get global $key")
                (value == "1") || (value == "2")
            },
            setState  = { on -> Shell.exec("svc wifi ${if (on) "enable" else "disable"}") },
            source    = UriSource(Settings.Global.getUriFor(key))
        )
    }

    var mobileData: StateManager? = null

    val bluetooth by lazy {
        val key = Settings.Global.BLUETOOTH_ON
        StateManager(
            context   = appContext,
            getState  = { Shell.exec("settings get global $key") == "1" },
            setState  = { on ->
                Shell.exec("svc bluetooth ${if (on) "enable" else "disable"}")
            },
            source    = UriSource(Settings.Global.getUriFor(Settings.Global.BLUETOOTH_ON))
        )
    }

    val location by lazy {
        val filter = IntentFilter().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                addAction(LocationManager.MODE_CHANGED_ACTION)
            } else {
                addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
            }
        }
        StateManager(
            context   = appContext,
            getState  = { Shell.exec("cmd location is-location-enabled").contains("true") },
            setState  = { on -> Shell.exec("cmd location set-location-enabled $on") },
            source    = IntentSource(filter)
        )
    }

    val googleAccuracy by lazy {
        StateManager(
            context = appContext,

            // ─── read current “network_location_opt_in” value ───
            getState = {
                // run the same “content query” you did in ADB
                val out = Shell.exec(
                    "content query --uri content://com.google.settings/partner --projection name,value"
                )
                // find the line for our key and parse the “value=…” tail
                out.lineSequence()
                    .firstOrNull { it.contains("network_location_opt_in") }
                    ?.substringAfter("value=")
                    ?.toIntOrNull() == 1
            },

            // ─── write new value (0 or 1) ───
            setState = { on ->
                val v = if (on) 1 else 0
                // delete any existing row first, so insert won’t duplicate
                Shell.exec("su -c content delete --uri content://com.google.settings/partner --where \"name='network_location_opt_in'\"")
                // now insert the new value
                Shell.exec("su -c content insert --uri content://com.google.settings/partner --bind name:s:network_location_opt_in --bind value:i:$v")
            },

            // ─── observe changes via a ContentObserver on the partner URI ───
            source = UriSource("content://com.google.settings/partner".toUri())
        )
    }

    val wifiScanning by lazy {
        val key = "wifi_scan_always_enabled"
        StateManager(
            context    = appContext,
            getState   = { Shell.exec("settings get global $key") == "1" },
            setState   = { on ->
                Shell.exec("settings put global $key ${if (on) 1 else 0}")
            },
            source     = UriSource(Settings.Global.getUriFor(key))
        )
    }

    val bluetoothScanning by lazy {
        val key = "ble_scan_always_enabled"
        StateManager(
            context = appContext,
            getState = { Shell.exec("settings get global $key") == "1" },
            setState = { on ->
                Shell.exec("settings put global $key ${if (on) 1 else 0}")
            },
            source  = UriSource(Settings.Global.getUriFor(key))
        )
    }

    init {
        val mobileDataOptions = listMobileDataOptions()

        if (mobileDataOptions.isEmpty()) {
            mobileData = null
        } else if (mobileDataOptions.size == 1) {
            val key = mobileDataOptions[0]
            setMobileDataVariable(key)
        } else {
            var key = "mobile_data"
            val suffix = getDataSimSuffix()

            if (mobileDataOptions.contains(key + suffix)) {
                key += suffix
            }
            setMobileDataVariable(key)
        }
    }

    private fun setMobileDataVariable(key: String) {
        mobileData = StateManager(
            context  = appContext,
            getState = { Shell.exec("settings get global $key") == "1" },
            setState = { on ->
                Shell.exec("svc data ${if (on) "enable" else "disable"}")
            },
            source   = UriSource(Settings.Global.getUriFor(key))
        )
    }

    private fun listMobileDataOptions(): List<String> {
        val raw = Shell.exec("settings list global mobile_data")

        val prefix = "mobile_data"
        return raw.lineSequence()
            .map   { it.substringBefore('=') }              // keep only the key
            .filter { it.startsWith(prefix) &&              // key begins with prefix
                    (it.length == prefix.length           //   a) exact match
                            || it[prefix.length] != '_') }       //   b) next char ≠ '_'
            .toList()
    }

    private fun getDataSimSuffix(): String {
        val raw = Shell.exec("settings get global multi_sim_data_call")
        return raw
    }
}
