package com.example.livenessdetectionfinalyearproject

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import coil.load
import com.example.livenessdetectionfinalyearproject.SignUpNameFragment.Companion.USER_NAME_PARAM
import com.example.livenessdetectionfinalyearproject.base.CameraUtility
import com.example.livenessdetectionfinalyearproject.base.hide
import com.example.livenessdetectionfinalyearproject.base.launchActivity
import com.example.livenessdetectionfinalyearproject.base.show
import com.example.livenessdetectionfinalyearproject.base.toast
import com.example.livenessdetectionfinalyearproject.databinding.ActivityCameraBinding
import com.example.livenessdetectionfinalyearproject.mlOps.FaceDetectionForRegistration
import com.example.livenessdetectionfinalyearproject.roomStuff.DBHelperImpl
import com.example.livenessdetectionfinalyearproject.roomStuff.DatabaseBuilder
import com.example.livenessdetectionfinalyearproject.viewModels.SignUpCameraViewModel
import com.example.livenessdetectionfinalyearproject.viewModels.LivenessViewModelFactory
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

typealias LumaListener = (luma: Double) -> Unit

class SignUpCameraActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private val factory by lazy {
        LivenessViewModelFactory(
            DBHelperImpl(
                DatabaseBuilder.getInstance(this.applicationContext)
            )
        )
    }
    private val signUpCameraViewModel by viewModels<SignUpCameraViewModel> {factory}
    // Select back camera as a default
    private var cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

    private val username by lazy { intent.getStringExtra(USER_NAME_PARAM).orEmpty() }


    lateinit var binding: ActivityCameraBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestPermission()
        binding.flCapture.setOnClickListener {
            takePhoto()
        }
        binding.flCancel.setOnClickListener {
            binding.clCameraState.show()
            binding.clPreviewState.hide()
        }

        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()
        signUpCameraViewModel.isSuccessfulLiveData.observe(this) {
            if (it) {
                launchActivity(activityClass = SuccessActivity::class.java)
                finish()
            } else {
                toast("Couldn't register face, please retry")?.show()
            }
        }
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    processImage(savedUri)
                }
            }
        )
    }

    private fun processImage(savedUri: Uri) {
        binding.apply {
            clPreviewState.show()
            clCameraState.hide()
            btnSave.show()
            ivSelectedImage.load(savedUri)
            btnSave.setOnClickListener {
                FaceDetectionForRegistration(this@SignUpCameraActivity).confirmImageContainsASingleFace(
                    imageUri = savedUri,
                    onFaceDetected = {
                        if (username.isNotBlank()) {
                            signUpCameraViewModel.saveUser(username, savedUri)
                        } else {
                            toast("please go back and input username")?.show()
                        }
                    },
                    onError = {
                        toast(it)?.show()
                    }
                )
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.flCameraView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                        Log.d(TAG, "Average luminosity: $luma")
                    })
                }
            bindCameraUseCases(cameraProvider, preview, imageAnalyzer)
            binding.flRotateCamera.setOnClickListener {
                cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA)
                    CameraSelector.DEFAULT_BACK_CAMERA
                else CameraSelector.DEFAULT_FRONT_CAMERA
                bindCameraUseCases(cameraProvider, preview, imageAnalyzer)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases(
        cameraProvider: ProcessCameraProvider,
        preview: Preview,
        imageAnalyzer: ImageAnalysis
    ) {
        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to camera
            cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture, imageAnalyzer
            )

        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun requestPermission() {

        if (CameraUtility.hasCameraPermissions(this)) {
            startCamera()
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            EasyPermissions.requestPermissions(
                this,
                "You need to accept the camera permission to use this app",
                REQUEST_CODE_CAMERA_PERMISSION,
                Manifest.permission.CAMERA
            )
        } else {
            EasyPermissions.requestPermissions(
                this,
                "You need to accept the camera permission to use this app",
                REQUEST_CODE_CAMERA_PERMISSION,
                Manifest.permission.CAMERA

            )
        }

    }


    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        startCamera()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            requestPermission()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }


    private class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        override fun analyze(image: ImageProxy) {

            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()
            val pixels = data.map { it.toInt() and 0xFF }
            val luma = pixels.average()

            listener(luma)

            image.close()
        }
    }

    companion object {
        const val REQUEST_CODE_CAMERA_PERMISSION = 0
        const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val TAG = "CameraXLiveness"
    }
}