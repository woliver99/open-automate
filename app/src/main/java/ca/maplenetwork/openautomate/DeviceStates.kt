package ca.maplenetwork.openautomate

import android.content.ContentValues
import android.content.Context
import android.content.IntentFilter
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.net.toUri
import rikka.shizuku.Shizuku

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

    // Would probably need to use logcat to listen for changes
    /*val googleAccuracy by lazy {
        StateManager(
            context = appContext,

            /* ── read the single row only ── */
            getState = {
                val out = Shell.exec(
                    "content query " +
                            "--uri content://com.google.settings/partner " +
                            "--projection value " +
                            "--where \"name='network_location_opt_in'\""
                )
                // output looks like: Row: 0 value=1
                out.contains("value=1")
            },

            /* ── write via update or insert ── */
            setState = { on ->
                val v = if (on) 1 else 0
                // try update first (faster, no dup rows). Falls back to insert = upsert.
                val upd = Shell.exec(
                    "content update " +
                            "--uri content://com.google.settings/partner " +
                            "--bind value:i:$v " +
                            "--where \"name='network_location_opt_in'\""
                )
                if (upd.contains("Updated 0 rows")) {
                    Shell.exec(
                        "content insert --uri content://com.google.settings/partner " +
                                "--bind name:s:network_location_opt_in --bind value:i:$v"
                    )
                }
            },

            /* ── observe provider changes ── */
            source = UriSource("content://com.google.settings/partner".toUri())
        )
    }*/

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

        if (mobileDataOptions.isEmpty() || !hasAnySim()) {
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

    private fun hasAnySim(): Boolean {
        val simStates = Shell.exec("getprop gsm.sim.state")
            .split(",")
            .map(String::trim)

        // Define which states count as “SIM present”
        val presentStates = setOf("READY", "LOADED", "PIN_REQUIRED", "PUK_REQUIRED")

        // Return true if any slot is in a “present” state
        return simStates.any { it.uppercase() in presentStates }
    }
}
