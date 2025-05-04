package ca.maplenetwork.openautomate

import android.content.Context
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds


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

suspend fun StateManager.toggleAndAwait(expected: Boolean): Boolean {
    // create a one-shot Flow from the listener
    val flow = callbackFlow<Boolean> {
        val l = StateListener { trySend(it) }
        addListener(l)
        awaitClose { removeListener(l) }
    }

    // perform the toggle *after* the listener is active
    toggle()

    // wait at most 3 s for the expected value
    return withTimeoutOrNull(5.seconds) {
        flow
            .onEach { /*   debug each step if you like   */ }
            .first { it == expected }
    } != null
}

