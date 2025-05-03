package ca.maplenetwork.openautomate

import android.content.Context
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Build
import android.provider.Settings

class DeviceStates(context: Context) {
    private val appContext = context.applicationContext

    val airplane by lazy {
        StateManager(
            context   = appContext,
            getState  = { ShizukuShell.exec("cmd connectivity airplane-mode").contains("enabled") },
            setState  = { on -> ShizukuShell.exec("cmd connectivity airplane-mode ${if (on) "enable" else "disable"}") },
            source    = UriSource(Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON)) // changes via Settings provider
        )
    }

    val wifi by lazy {
        StateManager(
            context   = appContext,
            getState  = { ShizukuShell.exec("settings get global wifi_on") == "1" },
            setState  = { on -> ShizukuShell.exec("svc wifi ${if (on) "enable" else "disable"}") },
            source    = UriSource(Settings.Global.getUriFor(Settings.Global.WIFI_ON))
        )
    }

    val bluetooth by lazy {
        StateManager(
            context   = appContext,
            getState  = { ShizukuShell.exec("settings get global bluetooth_on") == "1" },
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
            source    = IntentSource(filter)   // delivers broadcast when user flips switch
        )
    }

    val googleAccuracy: StateManager by lazy {
        StateManager(
            context   = appContext,
            getState  = { ShizukuShell.exec("settings get global assisted_gps_enabled") == "1" },
            setState  = { on ->
                ShizukuShell.exec("settings put secure assisted_gps_enabled ${if (on) 1 else 0}")
            },
            source    = UriSource(Settings.Global.getUriFor("assisted_gps_enabled"))
        )
    }

    val wifiScanning: StateManager by lazy {
        StateManager(
            context    = appContext,
            getState   = { ShizukuShell.exec("settings get global wifi_scan_always_enabled") == "1" },
            setState   = { on ->
                ShizukuShell.exec("settings put global wifi_scan_always_enabled ${if (on) 1 else 0}")
            },
            source     = UriSource(Settings.Global.getUriFor("wifi_scan_always_enabled"))
        )
    }
}
