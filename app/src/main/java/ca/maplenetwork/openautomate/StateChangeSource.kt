package ca.maplenetwork.openautomate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import androidx.core.content.ContextCompat

interface StateChangeSource {
    fun register(context: Context, onChange: () -> Unit)
    fun unregister(context: Context)
}

class UriSource(private val uri: Uri) : StateChangeSource {
    private var observer: ContentObserver? = null

    override fun register(context: Context, onChange: () -> Unit) {
        if (observer != null) return                       // already registered
        observer = object : ContentObserver(Handler(context.mainLooper)) {
            override fun onChange(self: Boolean) = onChange()
        }
        context.contentResolver.registerContentObserver(uri, false, observer!!)
    }

    override fun unregister(context: Context) {
        observer?.let { context.contentResolver.unregisterContentObserver(it) }
        observer = null
    }
}

class IntentSource(private val filter: IntentFilter) : StateChangeSource {
    private var receiver: BroadcastReceiver? = null

    override fun register(context: Context, onChange: () -> Unit) {
        if (receiver != null) return
        receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) = onChange()
        }
        // Use the Compat helper so it keeps working on Android 14+ :contentReference[oaicite:0]{index=0}
        ContextCompat.registerReceiver(
            context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun unregister(context: Context) {
        receiver?.let { context.unregisterReceiver(it) }
        receiver = null
    }
}