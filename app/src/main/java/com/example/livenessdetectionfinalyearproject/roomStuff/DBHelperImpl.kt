package com.example.livenessdetectionfinalyearproject.roomStuff

class DBHelperImpl(private val appDataBase: AppDataBase) : DBHelper {
    override suspend fun getLocalFaces(): List<LocalFace> {
        return appDataBase.localFacesDao().getAll()
    }

    override suspend fun insert(localFace: LocalFace) {
        return appDataBase.localFacesDao().insert(localFace)
    }
}