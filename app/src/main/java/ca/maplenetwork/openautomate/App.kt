package ca.maplenetwork.openautomate

import android.app.Application

class App : Application() {
    val deviceStates: DeviceStates by lazy { DeviceStates(this) }
}