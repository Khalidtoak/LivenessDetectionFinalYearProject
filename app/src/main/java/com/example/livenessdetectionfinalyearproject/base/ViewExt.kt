package com.example.livenessdetectionfinalyearproject.base

import android.view.View

fun View.hide(visibility: Int = View.GONE) {
    this.visibility = visibility
}

fun View.show() {
    visibility = View.VISIBLE
}