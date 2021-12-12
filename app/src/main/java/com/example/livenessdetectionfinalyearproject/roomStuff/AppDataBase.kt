package com.example.livenessdetectionfinalyearproject.roomStuff

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [LocalFace::class], version = 1)
abstract class AppDataBase : RoomDatabase() {
    abstract fun localFacesDao(): LocalFacesDao
}