package ca.maplenetwork.openautomate

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper

/** Callback for state changes.  */
fun interface StateListener {
    fun onChanged(newValue: Boolean)
}

class StateManager(
    private val context: Context,
    private val observeUri: Uri,
    private val getState: () -> Boolean,
    private val setState: (Boolean) -> Unit,
    private val toggleState: (() -> Boolean)? = null
) {
    private val listeners = mutableSetOf<StateListener>()
    private var observer: ContentObserver? = null
    private var lastObserveState: Boolean? = null

    fun get() = getState()
    fun set(v: Boolean) = setState(v)
    fun toggle(): Boolean {
        toggleState?.let { return it() }
        val new = !get()
        set(new)
        return new
    }

    /* ------------- subscription ------------ */
    fun addListener(l: StateListener) {
        if (listeners.isEmpty()) registerObserver()
        listeners += l
        l.onChanged(get())        // immediate callback
    }

    fun removeListener(l: StateListener) {
        listeners -= l
        if (listeners.isEmpty()) unregisterObserver()
    }

    /* ----------- internal plumbing ---------- */
    private fun registerObserver() {
        observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                val v = get()
                println("onChange: $v, lastObserveState=$lastObserveState")
                if (v == lastObserveState) return
                lastObserveState = v
                listeners.forEach { it.onChanged(v) }
            }
        }.also {
            context.contentResolver.registerContentObserver(observeUri, false, it)
        }
    }

    private fun unregisterObserver() {
        observer?.let { context.contentResolver.unregisterContentObserver(it) }
        observer = null
    }
}
