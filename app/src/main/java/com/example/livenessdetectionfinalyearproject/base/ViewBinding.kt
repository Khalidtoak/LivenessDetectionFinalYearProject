package com.example.livenessdetectionfinalyearproject.base

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty


/**
 * A property delegate that sets a Fragment's ViewBinding to null in ON_DESTROY in the ViewLifecycleOwner,
 * such that it is triggered during onDestroyView. This is advised in the Android documentation and prevents memory leaks.
 */
class ViewBinding<T>(private val createBinding: (view: View) -> T) : ReadOnlyProperty<Fragment, T>,
    LifecycleObserver {

    private var binding: T? = null

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        binding = null
    }

    override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        return binding ?: let {
            thisRef.viewLifecycleOwner.lifecycle.addObserver(this)
            createBinding(thisRef.requireView()).also { binding = it }
        }
    }
}

inline fun <T> viewBinding(crossinline bindFunc: (View) -> T): ViewBinding<T> {
    return ViewBinding { bindFunc.invoke(it) }
}
