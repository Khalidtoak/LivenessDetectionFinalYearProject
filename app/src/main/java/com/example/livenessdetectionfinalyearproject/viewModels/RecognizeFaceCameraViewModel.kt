package com.example.livenessdetectionfinalyearproject.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livenessdetectionfinalyearproject.roomStuff.DBHelper
import com.example.livenessdetectionfinalyearproject.roomStuff.LocalFace
import kotlinx.coroutines.launch

class RecognizeFaceCameraViewModel(private val dbHelper: DBHelper): ViewModel() {
    private val _localFacesLiveData = MutableLiveData<List<LocalFace>>()
    val localFacesLiveData : LiveData<List<LocalFace>> = _localFacesLiveData
    fun getLocalFaces() {
        kotlin.runCatching {
            viewModelScope.launch {
                dbHelper.getLocalFaces().also {
                    Log.d("faces uri", it.first().photoUri)
                    Log.d("faces name", it.first().userName)
                    _localFacesLiveData.value = it
                }
            }
        }.onFailure {
            Log.e("error", "error getting faces ${it.message}")
            it.printStackTrace()
            _localFacesLiveData.value = emptyList()
        }
    }
}