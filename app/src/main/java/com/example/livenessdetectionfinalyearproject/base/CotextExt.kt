package com.example.livenessdetectionfinalyearproject.base

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

fun Context.toast(message: String?, length: Int = Toast.LENGTH_LONG): Toast? {
    return if (message?.isNotBlank() == true) {
        Toast.makeText(this, message, length)
    } else {
        null
    }
}

fun <T> Context.launchActivity(
    shouldInvalidatePrevActivities: Boolean = false,
    activityClass: Class<T>,
    extras: Bundle.() -> Unit = {}
) {
    val intent = Intent(this, activityClass)
    intent.putExtras(Bundle().apply(extras))
    if (shouldInvalidatePrevActivities)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    startActivity(intent)
}