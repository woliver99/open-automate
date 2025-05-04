package ca.maplenetwork.openautomate

import android.content.Context
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import android.util.Log

class DeviceStates(context: Context) {
    private val appContext = context.applicationContext

    val airplane by lazy {
        val key = Settings.Global.AIRPLANE_MODE_ON
        StateManager(
            context   = appContext,
            getState  = { ShizukuShell.exec("cmd connectivity airplane-mode").contains("enabled") },
            setState  = { on -> ShizukuShell.exec("cmd connectivity airplane-mode ${if (on) "enable" else "disable"}") },
            source    = UriSource(Settings.Global.getUriFor(key))
        )
    }

    val wifi by lazy {
        val key = Settings.Global.WIFI_ON
        StateManager(
            context   = appContext,
            getState  = { ShizukuShell.exec("settings get global $key") == "1" },
            setState  = { on -> ShizukuShell.exec("svc wifi ${if (on) "enable" else "disable"}") },
            source    = UriSource(Settings.Global.getUriFor(key))
        )
    }

    var mobileData: StateManager? = null

    val bluetooth by lazy {
        val key = Settings.Global.BLUETOOTH_ON
        StateManager(
            context   = appContext,
            getState  = { ShizukuShell.exec("settings get global $key") == "1" },
            setState  = { on ->
                val code = if (on) 6 else 8
                ShizukuShell.exec("service call bluetooth_manager $code")
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
            getState  = { ShizukuShell.exec("cmd location is-location-enabled").contains("true") },
            setState  = { on -> ShizukuShell.exec("cmd location set-location-enabled $on") },
            source    = IntentSource(filter)
        )
    }

    val googleAccuracy by lazy {
        val key = "assisted_gps_enabled"
        StateManager(
            context   = appContext,
            getState  = { ShizukuShell.exec("settings get global $key") == "1" },
            setState  = { on ->
                ShizukuShell.exec("settings put secure $key ${if (on) 1 else 0}")
            },
            source    = UriSource(Settings.Global.getUriFor(key))
        )
    }

    val wifiScanning by lazy {
        val key = "wifi_scan_always_enabled"
        StateManager(
            context    = appContext,
            getState   = { ShizukuShell.exec("settings get global $key") == "1" },
            setState   = { on ->
                ShizukuShell.exec("settings put global $key ${if (on) 1 else 0}")
            },
            source     = UriSource(Settings.Global.getUriFor(key))
        )
    }

    val bluetoothScanning by lazy {
        val key = "ble_scan_always_enabled"
        StateManager(
            context = appContext,
            getState = { ShizukuShell.exec("settings get global $key") == "1" },
            setState = { on ->
                ShizukuShell.exec("settings put global $key ${if (on) 1 else 0}")
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
            getState = { ShizukuShell.exec("settings get global $key") == "1" },
            setState = { on ->
                ShizukuShell.exec("svc data ${if (on) "enable" else "disable"}")
            },
            source   = UriSource(Settings.Global.getUriFor(key))
        )
    }

    private fun listMobileDataOptions(): List<String> {
        val raw = ShizukuShell.exec("settings list global mobile_data")

        val prefix = "mobile_data"
        return raw.lineSequence()
            .map   { it.substringBefore('=') }              // keep only the key
            .filter { it.startsWith(prefix) &&              // key begins with prefix
                    (it.length == prefix.length           //   a) exact match
                            || it[prefix.length] != '_') }       //   b) next char ≠ '_'
            .toList()
    }

    private fun getDataSimSuffix(): String {
        val raw = ShizukuShell.exec("settings get global multi_sim_data_call")
        return raw
    }
}
