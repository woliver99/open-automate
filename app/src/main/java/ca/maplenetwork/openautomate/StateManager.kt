package ca.maplenetwork.openautomate

import android.content.Context


fun interface StateListener {
    fun onChanged(newValue: Boolean)
}


class StateManager(
    private val context: Context,
    private val getState: () -> Boolean,
    private val setState: (Boolean) -> Unit,
    private val toggleState: (() -> Boolean)? = null,
    private val source: StateChangeSource? = null
) {
    private val listeners = mutableSetOf<StateListener>()
    private var lastValue: Boolean? = null

    fun get() = getState()
    fun set(v: Boolean) = setState(v)
    fun toggle(): Boolean = toggleState?.invoke() ?: run {
        val new = !get()
        set(new)
        new
    }

    fun addListener(l: StateListener) {
        if (listeners.isEmpty()) registerListener()
        listeners += l
        l.onChanged(get())      // immediate first callback
    }

    fun removeListener(l: StateListener) {
        listeners -= l
        if (listeners.isEmpty()) unregisterListener()
    }

    private fun registerListener() =
        source?.register(context) { notifyListeners() }

    private fun unregisterListener() =
        source?.unregister(context)

    private fun notifyListeners() {
        val value = get()
        if (value == lastValue) return
        lastValue = value
        listeners.forEach { it.onChanged(value) }
    }
}

