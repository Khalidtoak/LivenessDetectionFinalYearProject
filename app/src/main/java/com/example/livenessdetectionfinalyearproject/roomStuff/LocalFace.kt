package com.example.livenessdetectionfinalyearproject.roomStuff

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "face")
data class LocalFace(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userName: String,
    val photoUri: String
)