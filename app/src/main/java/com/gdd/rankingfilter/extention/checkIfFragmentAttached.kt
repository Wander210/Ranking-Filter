package com.gdd.rankingfilter.extention

import androidx.fragment.app.Fragment

//Extension function
fun Fragment.checkIfFragmentAttached(operation: () -> Unit) {
    if (isAdded && context != null) {
        operation()
    }
}