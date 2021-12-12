package com.example.livenessdetectionfinalyearproject.mlOps

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.livenessdetectionfinalyearproject.base.BitmapUtils
import com.example.livenessdetectionfinalyearproject.customViews.BoundingBoxOverlay
import com.example.livenessdetectionfinalyearproject.customViews.Prediction
import com.example.livenessdetectionfinalyearproject.mlOps.model.FaceNetModel
import com.example.livenessdetectionfinalyearproject.mlOps.model.MaskDetectionModel
import com.example.livenessdetectionfinalyearproject.mlOps.model.Models
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.sqrt

class FrameAnalyser(
    context: Context,
    private val boundingBoxOverlay: BoundingBoxOverlay,
    private val callBack: OnDetectionCallBack,
    private val onPersonIdentified: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val realTimeOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .enableTracking()
        .build()
    private val detector = FaceDetection.getClient(realTimeOpts)

    // You may the change the models here.
    // Use the model configs in Models.kt
    // Default is Models.FACENET ; Quantized models are faster
    private val model = FaceNetModel(context, Models.FACENET_QUANTIZED)

    private val nameScoreHashmap = HashMap<String, ArrayList<Float>>()
    private var subject = FloatArray(model.embeddingDim)

    // Used to determine whether the incoming frame should be dropped or processed.
    private var isProcessing = false

    // Store the face embeddings in a ( String , FloatArray ) ArrayList.
    // Where String -> name of the person and FloatArray -> Embedding of the face.
    var faceList = ArrayList<Pair<String, FloatArray>>()

    // Use any one of the two metrics, "cosine" or "l2"
    private val metricToBeUsed = "cosine"

    // Use this variable to enable/disable mask detection.
    private val isMaskDetectionOn = true
    private val maskDetectionModel = MaskDetectionModel(context)

    private var faceId: Int? = null

    private var eyesWereClosed = false
    var currentCommand: CommandType? = CommandType.DETECT_BLINK


    init {
        boundingBoxOverlay.drawMaskLabel = isMaskDetectionOn
    }

    private fun performLiveness(results: List<Face>, frameBitmap: Bitmap) {
        if (results.size != 1) {
            callBack.onMoreThan1FaceDetected()
            isProcessing = false
            return
        }
        val face = results[0]
        when (currentCommand) {
            CommandType.DETECT_BLINK -> {
                faceId = face.trackingId
                detectBlink(face)
            }
            CommandType.DETECT_BLINK_AGAIN -> {
                if (faceId != face.trackingId) {
                    callBack.onFaceIdsNotMatching()
                } else {
                    detectBlink(face)
                }
            }
            CommandType.DETECT_SMILE -> {
                if (faceId != face.trackingId) {
                    callBack.onFaceIdsNotMatching()
                } else {
                    detectSmile(face)
                }
            }
            CommandType.PERFORM_RECOGNITION -> {
                if (faceId != face.trackingId) {
                    callBack.onFaceIdsNotMatching()
                } else {
                    CoroutineScope(Dispatchers.Default).launch {
                        runModel(results, frameBitmap)
                    }
                }
            }
        }
        isProcessing = false
    }

    enum class CommandType {
        DETECT_BLINK, DETECT_BLINK_AGAIN, DETECT_SMILE, PERFORM_RECOGNITION
    }

    interface OnDetectionCallBack {
        fun onEyeBlink()
        fun onSmile()
        fun onMoreThan1FaceDetected()
        fun onFaceIdsNotMatching()
        fun onError(message: String)
        fun onComplete()
    }

    private fun detectSmile(face: Face) {
        val smileProb = face.smilingProbability
        if (smileProb != null && smileProb > 0.6) {
            callBack.onSmile()
        }
    }

    private fun detectBlink(face: Face) {
        val leftEyeOpenProb = face.leftEyeOpenProbability
        val rightEyeOpenProb = face.rightEyeOpenProbability
        if (eyesWereClosed) {
            if (leftEyeOpenProb != null && leftEyeOpenProb > .6 && rightEyeOpenProb != null && rightEyeOpenProb > .6) {
                eyesWereClosed = false
                callBack.onEyeBlink()
            }
        } else {
            if (leftEyeOpenProb != null && leftEyeOpenProb < .4 && rightEyeOpenProb != null && rightEyeOpenProb < .4) {
                eyesWereClosed = true
            }
        }
    }


    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        // If the previous frame is still being processed, then skip this frame
        if (isProcessing || faceList.size == 0) {
            image.close()
            return
        } else {
            isProcessing = true

            // Rotated bitmap for the FaceNet model
            val frameBitmap =
                BitmapUtils.imageToBitmap(image.image!!, image.imageInfo.rotationDegrees)

            // Configure frameHeight and frameWidth for output2overlay transformation matrix.
            if (!boundingBoxOverlay.areDimsInit) {
                boundingBoxOverlay.frameHeight = frameBitmap.height
                boundingBoxOverlay.frameWidth = frameBitmap.width
            }

            val inputImage = InputImage.fromMediaImage(image.image, image.imageInfo.rotationDegrees)
            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    Log.d("performLiveness", "liveness getting to onSuccess")
                    /****/
                    performLiveness(faces, frameBitmap)
                }
                .addOnCompleteListener {
                    image.close()
                }
        }
    }

    private suspend fun runModel(faces: List<Face>, cameraFrameBitmap: Bitmap) {
        withContext(Dispatchers.Default) {
            val predictions = ArrayList<Prediction>()
            for (face in faces) {
                try {
                    // Crop the frame using face.boundingBox.
                    // Convert the cropped Bitmap to a ByteBuffer.
                    // Finally, feed the ByteBuffer to the FaceNet model.
                    val croppedBitmap =
                        BitmapUtils.cropRectFromBitmap(cameraFrameBitmap, face.boundingBox)
                    subject = model.getFaceEmbedding(croppedBitmap)

                    // Perform face mask detection on the cropped frame Bitmap.
                    var maskLabel = ""
                    if (isMaskDetectionOn) {
                        maskLabel = maskDetectionModel.detectMask(croppedBitmap)
                    }

                    // Continue with the recognition if the user is not wearing a face mask
                    if (maskLabel == maskDetectionModel.NO_MASK) {
                        // Perform clustering ( grouping )
                        // Store the clusters in a HashMap. Here, the key would represent the 'name'
                        // of that cluster and ArrayList<Float> would represent the collection of all
                        // L2 norms/ cosine distances.
                        for (i in 0 until faceList.size) {
                            // If this cluster ( i.e an ArrayList with a specific key ) does not exist,
                            // initialize a new one.
                            if (nameScoreHashmap[faceList[i].first] == null) {
                                // Compute the L2 norm and then append it to the ArrayList.
                                val p = ArrayList<Float>()
                                if (metricToBeUsed == "cosine") {
                                    p.add(cosineSimilarity(subject, faceList[i].second))
                                } else {
                                    p.add(l2Norm(subject, faceList[i].second))
                                }
                                nameScoreHashmap[faceList[i].first] = p
                            }
                            // If this cluster exists, append the L2 norm/cosine score to it.
                            else {
                                if (metricToBeUsed == "cosine") {
                                    nameScoreHashmap[faceList[i].first]?.add(
                                        cosineSimilarity(
                                            subject,
                                            faceList[i].second
                                        )
                                    )
                                } else {
                                    nameScoreHashmap[faceList[i].first]?.add(
                                        l2Norm(
                                            subject,
                                            faceList[i].second
                                        )
                                    )
                                }
                            }
                        }

                        // Compute the average of all scores norms for each cluster.
                        val avgScores = nameScoreHashmap.values.map { scores ->
                            scores.toFloatArray().average()
                        }
                        Log.d("Score", "Average score for each user : $nameScoreHashmap")

                        val names = nameScoreHashmap.keys.toTypedArray()
                        nameScoreHashmap.clear()

                        // Calculate the minimum L2 distance from the stored average L2 norms.
                        val bestScoreUserName: String = if (metricToBeUsed == "cosine") {
                            // In case of cosine similarity, choose the highest value.
                            if (avgScores.maxOrNull()!! > model.model.cosineThreshold) {
                                names[avgScores.indexOf(avgScores.maxOrNull()!!)]
                            } else {
                                "Unknown"
                            }
                        } else {
                            // In case of L2 norm, choose the lowest value.
                            if (avgScores.minOrNull()!! > model.model.l2Threshold) {
                                "Unknown"
                            } else {
                                names[avgScores.indexOf(avgScores.minOrNull()!!)]
                            }
                        }
                        onPersonIdentified("Person identified as $bestScoreUserName")
                        predictions.add(
                            Prediction(
                                face.boundingBox,
                                bestScoreUserName,
                                maskLabel
                            )
                        )
                    } else {
                        // Inform the user to remove the mask
                        predictions.add(
                            Prediction(
                                face.boundingBox,
                                "Please remove the mask",
                                maskLabel
                            )
                        )
                    }
                } catch (e: Exception) {
                    // If any exception occurs with this box and continue with the next boxes.
                    Log.e("Model", "Exception in FrameAnalyser : ${e.message}")
                    continue
                }
            }
            withContext(Dispatchers.Main) {
                // Clear the BoundingBoxOverlay and set the new results ( boxes ) to be displayed.
                boundingBoxOverlay.faceBoundingBoxes = predictions
                boundingBoxOverlay.invalidate()

                isProcessing = false
            }
        }

    }

    // Compute the L2 norm of ( x2 - x1 )
    private fun l2Norm(x1: FloatArray, x2: FloatArray): Float {
        return sqrt(x1.mapIndexed { i, xi -> (xi - x2[i]).pow(2) }.sum())
    }

    // Compute the cosine of the angle between x1 and x2.
    private fun cosineSimilarity(x1: FloatArray, x2: FloatArray): Float {
        val mag1 = sqrt(x1.map { it * it }.sum())
        val mag2 = sqrt(x2.map { it * it }.sum())
        val dot = x1.mapIndexed { i, xi -> xi * x2[i] }.sum()
        return dot / (mag1 * mag2)
    }
}