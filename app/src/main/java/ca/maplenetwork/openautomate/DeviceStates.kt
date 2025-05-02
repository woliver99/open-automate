package ca.maplenetwork.openautomate

import android.content.Context
import android.provider.Settings

class DeviceStates(context: Context) {
    private val appCtx = context.applicationContext      // avoid leaking Activity

    // Airplane mode
    val airplane: StateManager by lazy {
        StateManager(
            context     = appCtx,
            observeUri  = Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON),
            getState    = {
                ShizukuShell.exec("cmd connectivity airplane-mode")
                    .contains("enabled")
            },
            setState    = { on ->
                ShizukuShell.exec("cmd connectivity airplane-mode ${if (on) "enable" else "disable"}")
            }
            // toggleState = null  → StateManager flips via get()/set()
        )
    }

    // Wi-Fi
    val wifi: StateManager by lazy {
        StateManager(
            context    = appCtx,
            observeUri = Settings.Global.getUriFor(Settings.Global.WIFI_ON),
            getState   = { ShizukuShell.exec("settings get global wifi_on") == "1" },
            setState   = { on -> ShizukuShell.exec("svc wifi ${if (on) "enable" else "disable"}") }
        )
    }

    // Bluetooth
    val bluetooth: StateManager by lazy {
        StateManager(
            context    = appCtx,
            observeUri = Settings.Global.getUriFor(Settings.Global.BLUETOOTH_ON),
            getState   = { ShizukuShell.exec("settings get global bluetooth_on") == "1" },
            setState   = { on ->
                val code = if (on) 6 else 8       // 6 = enable, 8 = disable
                ShizukuShell.exec("service call bluetooth_manager $code")
            }
        )
    }

    // Location
    val location: StateManager by lazy {
        StateManager(
            context    = appCtx,
            observeUri = Settings.Secure.getUriFor(Settings.Secure.LOCATION_MODE),
            getState   = {
                ShizukuShell.exec("cmd location is-location-enabled")
                    .contains("true")
            },
            setState   = { on ->
                ShizukuShell.exec("cmd location set-location-enabled $on")
            }
        )
    }

    /* ------------------------------------------------------------------ */
    /*  Google Location Accuracy                                          */
    /* ------------------------------------------------------------------ */
    val googleAccuracy: StateManager by lazy {
        StateManager(
            context    = appCtx,
            observeUri = Settings.Global.getUriFor("assisted_gps_enabled"),
            getState   = { ShizukuShell.exec("settings get global assisted_gps_enabled") == "1" },
            setState   = { on ->
                ShizukuShell.exec("settings put secure assisted_gps_enabled ${if (on) 1 else 0}")
            }
        )
    }

    /* ------------------------------------------------------------------ */
    /*  Wi-Fi scanning (always-available)                                 */
    /* ------------------------------------------------------------------ */
    val wifiScanning: StateManager by lazy {
        StateManager(
            context    = appCtx,
            observeUri = Settings.Global.getUriFor("wifi_scan_always_enabled"),
            getState   = { ShizukuShell.exec("settings get global wifi_scan_always_enabled") == "1" },
            setState   = { on ->
                ShizukuShell.exec("settings put global wifi_scan_always_enabled ${if (on) 1 else 0}")
            }
        )
    }
}
