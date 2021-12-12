package com.example.livenessdetectionfinalyearproject

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.view.View
import android.view.WindowInsets
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.livenessdetectionfinalyearproject.base.BitmapUtils
import com.example.livenessdetectionfinalyearproject.base.hide
import com.example.livenessdetectionfinalyearproject.base.show
import com.example.livenessdetectionfinalyearproject.base.toast
import com.example.livenessdetectionfinalyearproject.databinding.ActivityLoginCameraBinding
import com.example.livenessdetectionfinalyearproject.mlOps.DatabaseImageScanner
import com.example.livenessdetectionfinalyearproject.mlOps.FrameAnalyser
import com.example.livenessdetectionfinalyearproject.roomStuff.DBHelperImpl
import com.example.livenessdetectionfinalyearproject.roomStuff.DatabaseBuilder
import com.example.livenessdetectionfinalyearproject.viewModels.LivenessViewModelFactory
import com.example.livenessdetectionfinalyearproject.viewModels.RecognizeFaceCameraViewModel
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executors

class RecognizeFaceCameraActivity : AppCompatActivity(), FrameAnalyser.OnDetectionCallBack {
    lateinit var binding: ActivityLoginCameraBinding
    private lateinit var frameAnalyser: FrameAnalyser
    private lateinit var databaseImageScanner: DatabaseImageScanner
    var toast : Toast? = null
    var blinkCount = 0

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private val factory by lazy {
        LivenessViewModelFactory(
            DBHelperImpl(
                DatabaseBuilder.getInstance(this.applicationContext)
            )
        )
    }
    private val viewModel by viewModels<RecognizeFaceCameraViewModel> { factory }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.windowInsetsController!!
                .hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        }
        binding = ActivityLoginCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.apply {
            bboxOverlay.setWillNotDraw(false)
            bboxOverlay.setZOrderOnTop(true)
            frameAnalyser = FrameAnalyser(
                this@RecognizeFaceCameraActivity,
                bboxOverlay,
                this@RecognizeFaceCameraActivity) {
                binding.logTextView.show()
                binding.logTextView.text = it
            }
        }
        databaseImageScanner = DatabaseImageScanner(this)
        // Check if camera permissions were previously granted
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // request permission if previously granted
            requestCameraPermission()
        } else {
            startCameraPreview()
        }
        viewModel.getLocalFaces()
        viewModel.localFacesLiveData.observe(this) {
            val images = ArrayList(
                it.map { localFace ->
                    Pair(localFace.userName, getFixedBitmap(Uri.parse(localFace.photoUri)))
                }
            )
            databaseImageScanner.run(images, onLocalImageScanned)
        }
    }

    private val onLocalImageScanned = object : DatabaseImageScanner.ProcessCallback {
        override fun onProcessCompleted(data: ArrayList<Pair<String, FloatArray>>, numImagesWithNoFaces: Int) {
            frameAnalyser.faceList = data
        }
    }


    // Attach the camera stream to the PreviewView.
    private fun startCameraPreview() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()
                bindPreview(cameraProvider)
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    // Get the image as a Bitmap from given Uri and fix the rotation using the Exif interface
    // Source -> https://stackoverflow.com/questions/14066038/why-does-an-image-captured-using-camera-intent-gets-rotated-on-some-devices-on-a
    private fun getFixedBitmap(imageFileUri: Uri): Bitmap {
        var imageBitmap = BitmapUtils.getBitmapFromUri(contentResolver, imageFileUri)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val exifInterface = ExifInterface(contentResolver.openInputStream(imageFileUri)!!)
            imageBitmap = when (exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> BitmapUtils.rotateBitmap(
                        imageBitmap,
                        90f
                    )
                    ExifInterface.ORIENTATION_ROTATE_180 -> BitmapUtils.rotateBitmap(
                        imageBitmap,
                        180f
                    )
                    ExifInterface.ORIENTATION_ROTATE_270 -> BitmapUtils.rotateBitmap(
                        imageBitmap,
                        270f
                    )
                    else -> imageBitmap
                }
        } else {
            return imageBitmap
        }
        return imageBitmap
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview: Preview = Preview.Builder().build()
        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()
        preview.setSurfaceProvider(binding.previewView.surfaceProvider)
        val imageFrameAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(480, 640))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageFrameAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), frameAnalyser)
        cameraProvider.bindToLifecycle(
            this as LifecycleOwner,
            cameraSelector,
            preview,
            imageFrameAnalysis
        )
    }

    private fun requestCameraPermission() {
        // launch permission intent
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // if permission is granted by user, launch camera
                startCameraPreview()
            } else {
                // if permission is not granted, show a dialog that tells them the app can't function without the camera
                showPermissionDeniedDialog()
            }
        }

    private fun showPermissionDeniedDialog() {
        val alertDialog = AlertDialog.Builder(this).apply {
            setTitle("Camera Permission")
            setMessage("The app couldn't function without the camera permission.")
            setCancelable(false)
            setPositiveButton("ALLOW") { dialog, which ->
                dialog.dismiss()
                requestCameraPermission()
            }
            setNegativeButton("CLOSE") { dialog, which ->
                dialog.dismiss()
                finish()
            }
            create()
        }
        alertDialog.show()
    }

    override fun onEyeBlink() {
        if (blinkCount < 2) {
            binding.livenessActionText.text = getString(R.string.blink_again_text)
            blinkCount++
            frameAnalyser.currentCommand = FrameAnalyser.CommandType.DETECT_BLINK_AGAIN
        } else {
            showSmileAnim()
            binding.livenessActionText.text = getString(R.string.smileText)
            frameAnalyser.currentCommand =  FrameAnalyser.CommandType.DETECT_SMILE
        }

    }

    override fun onSmile() {
        hideAimViews()
        binding.livenessActionText.text = getString(R.string.alive_text)
        frameAnalyser.currentCommand = FrameAnalyser.CommandType.PERFORM_RECOGNITION
    }

    private fun hideAimViews() {
        binding.apply {
            blinkLottieAnimView.hide(View.INVISIBLE)
            smileLottieAnimView.hide(View.INVISIBLE)
        }
    }

    override fun onMoreThan1FaceDetected() {
        toast?.cancel()
        toast = toast("Only one face should be in the camera", Toast.LENGTH_SHORT)
        toast?.show()
        blinkCount = 0
        // restart liveness
        showBlinkAnim()
        frameAnalyser.currentCommand = FrameAnalyser.CommandType.DETECT_BLINK
        binding.livenessActionText.text = getString(R.string.blinkText)
    }

    override fun onFaceIdsNotMatching() {
        // start the process again
        toast?.cancel()
        toast = toast("Face in frane has changed, please restart liveness process")
        toast?.show()
        blinkCount = 0
        showBlinkAnim()
        frameAnalyser.currentCommand = FrameAnalyser.CommandType.DETECT_BLINK
        binding.livenessActionText.text = getString(R.string.blinkText)
    }

    override fun onError(message: String) {
        toast?.cancel()
        toast = toast("An error occured, please restart")
        toast?.show()
    }

    override fun onComplete() {
       // binding.livenessActionText.text = ""
    }
    private fun showBlinkAnim() {
        binding.apply {
            blinkLottieAnimView.show()
            smileLottieAnimView.hide()
            blinkLottieAnimView.playAnimation()
        }
    }

    private fun showSmileAnim() {
        binding.apply {
            blinkLottieAnimView.hide()
            smileLottieAnimView.show()
            smileLottieAnimView.playAnimation()
        }
    }
}