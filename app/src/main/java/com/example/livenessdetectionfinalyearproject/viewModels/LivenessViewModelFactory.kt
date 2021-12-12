package com.example.livenessdetectionfinalyearproject.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.livenessdetectionfinalyearproject.roomStuff.DBHelper

class LivenessViewModelFactory(private val dbHelper: DBHelper): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(DBHelper::class.java).newInstance(dbHelper)
    }
}