package com.example.livenessdetectionfinalyearproject.roomStuff

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocalFacesDao {
    @Query("SELECT * FROM face")
    suspend fun getAll(): List<LocalFace>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(localFaces: LocalFace)

    @Delete
    suspend fun delete(localFace: LocalFace)
}