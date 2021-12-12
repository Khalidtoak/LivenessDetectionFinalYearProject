package com.example.livenessdetectionfinalyearproject.roomStuff

interface DBHelper {
    suspend fun getLocalFaces(): List<LocalFace>
    suspend fun insert(localFace: LocalFace)
}