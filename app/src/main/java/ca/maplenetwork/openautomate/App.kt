package ca.maplenetwork.openautomate

import android.app.Application
import com.google.android.material.color.DynamicColors

class App : Application() {
    val deviceStates: DeviceStates by lazy { DeviceStates(this) }

    override fun onCreate() {
        super.onCreate()
        // This hooks up all Activities (including those hosting PreferenceFragmentCompat)
        DynamicColors.applyToActivitiesIfAvailable(this)
    }

    override fun onTerminate() {
        deviceStates.close()
        super.onTerminate()
    }
}