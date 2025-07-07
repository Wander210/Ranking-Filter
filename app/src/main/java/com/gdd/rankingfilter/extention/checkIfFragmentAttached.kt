package com.gdd.rankingfilter.extention

import android.content.Context
import androidx.fragment.app.Fragment

//Extension function
fun Fragment.checkIfFragmentAttached(operation: () -> Unit) {
    if (isAdded && context != null) {
        operation()
    }
}

fun Float.dpToPx(context: Context): Float {
    return this * context.resources.displayMetrics.density
}