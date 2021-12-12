package com.example.livenessdetectionfinalyearproject.viewModels

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livenessdetectionfinalyearproject.roomStuff.DBHelper
import com.example.livenessdetectionfinalyearproject.roomStuff.LocalFace
import kotlinx.coroutines.launch

class SignUpCameraViewModel(private val dbHelper: DBHelper) : ViewModel() {
    private val _isSuccessfulLiveData = MutableLiveData<Boolean>()
    val isSuccessfulLiveData: LiveData<Boolean> = _isSuccessfulLiveData
    fun saveUser(userName: String, faceUri: Uri) {
        kotlin.runCatching {
            viewModelScope.launch {
                dbHelper.insert(LocalFace(userName = userName, photoUri = faceUri.toString())).also {
                    _isSuccessfulLiveData.value = true
                }
            }
        }.onFailure {
            _isSuccessfulLiveData.value = false
        }
    }
}