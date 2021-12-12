package com.example.livenessdetectionfinalyearproject.mlOps

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceDetection




class FaceDetectionForRegistration(private val context: Context) {

    fun confirmImageContainsASingleFace(
        imageUri: Uri,
        onFaceDetected: () -> Unit,
        onError: (String) -> Unit
    ){
        val inputImage = InputImage.fromFilePath(context, imageUri)
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
        val detector = FaceDetection.getClient(options)
        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                when {
                    faces.isEmpty() -> onError("No faces detected image")
                    faces.size > 1 -> onError("More than one face detected")
                    else -> onFaceDetected()
                }
            }
            .addOnFailureListener { e -> // Task failed with an exception
                onError("An error occured, please try again")
                e.printStackTrace()
            }
    }
}