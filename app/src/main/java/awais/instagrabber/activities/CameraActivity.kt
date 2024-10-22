package awais.instagrabber.activities

import android.content.Intent
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayManager.DisplayListener
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import awais.instagrabber.databinding.ActivityCameraBinding
import awais.instagrabber.utils.DownloadUtils
import awais.instagrabber.utils.PermissionUtils
import awais.instagrabber.utils.Utils
import awais.instagrabber.utils.extensions.TAG
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : BaseLanguageActivity() {
    private lateinit var binding: ActivityCameraBinding
    private lateinit var displayManager: DisplayManager
    private lateinit var cameraExecutor: ExecutorService

    private var outputDirectory: DocumentFile? = null
    private var imageCapture: ImageCapture? = null
    private var displayId = -1
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK // Default to back camera

    private val cameraRequestCode = 100
    private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)

    private val displayListener: DisplayListener = object : DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}
        override fun onDisplayChanged(displayId: Int) {
            if (displayId == this@CameraActivity.displayId) {
                imageCapture?.targetRotation = binding.viewFinder.display.rotation
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        Utils.transparentStatusBar(this, true, false)
        initializeCameraComponents()
        setupUI()
    }

    private fun initializeCameraComponents() {
        displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager
        outputDirectory = DownloadUtils.cameraDir
        cameraExecutor = Executors.newSingleThreadExecutor()
        displayManager.registerDisplayListener(displayListener, null)

        binding.viewFinder.post {
            displayId = binding.viewFinder.display.displayId
            checkPermissionsAndSetupCamera()
        }
    }

    private fun setupUI() {
        binding.cameraCaptureButton.setOnClickListener { takePhoto() }
        binding.switchCamera.setOnClickListener { toggleCamera() }
        binding.close.setOnClickListener { finishWithCancel() }
    }

    private fun finishWithCancel() {
        setResult(RESULT_CANCELED)
        finish()
    }

    private fun toggleCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) 
            CameraSelector.LENS_FACING_BACK 
        else 
            CameraSelector.LENS_FACING_FRONT
        
        bindCameraUseCases()
    }

    override fun onResume() {
        super.onResume()
        if (!PermissionUtils.hasCameraPerms(this)) {
            PermissionUtils.requestCameraPerms(this, cameraRequestCode)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateUi()
        updateCameraSwitchButton()
    }

    override fun onDestroy() {
        super.onDestroy()
        Utils.transparentStatusBar(this, false, false)
        cameraExecutor.shutdown()
        displayManager.unregisterDisplayListener(displayListener)
    }

    private fun checkPermissionsAndSetupCamera() {
        if (PermissionUtils.hasCameraPerms(this)) {
            setupCamera()
        } else {
            PermissionUtils.requestCameraPerms(this, cameraRequestCode)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraRequestCode && PermissionUtils.hasCameraPerms(this)) {
            setupCamera()
        }
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                setupLensFacing()
                updateCameraSwitchButton()
                bindCameraUseCases()
            } catch (e: Exception) {
                Log.e(TAG, "setupCamera: ", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun setupLensFacing() {
        lensFacing = when {
            hasBackCamera() -> CameraSelector.LENS_FACING_BACK
            hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
            else -> -1
        }
        check(lensFacing != -1) { "No available camera found" }
    }

    private fun bindCameraUseCases() {
        val rotation = binding.viewFinder.display.rotation
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        val preview = Preview.Builder().setTargetRotation(rotation).build()
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(rotation)
            .build()

        cameraProvider?.unbindAll()
        cameraProvider?.bindToLifecycle(this, cameraSelector, preview, imageCapture)
        preview.setSurfaceProvider(binding.viewFinder.surfaceProvider)
    }

    private fun takePhoto() {
        imageCapture?.let {
            val fileName = "${simpleDateFormat.format(System.currentTimeMillis())}.jpg"
            val mimeType = "image/jpg"
            val photoFile = outputDirectory?.createFile(mimeType, fileName) ?: return
            
            val outputStream = contentResolver.openOutputStream(photoFile.uri) ?: return
            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(outputStream).build()

            it.takePicture(
                outputFileOptions,
                cameraExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        handleImageSaved(photoFile, outputStream)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "onError: ", exception)
                        closeStream(outputStream)
                    }
                }
            )
        }
    }

    private fun handleImageSaved(photoFile: DocumentFile, outputStream: OutputStream) {
        closeStream(outputStream)
        val intent = Intent().apply {
            data = photoFile.uri
        }
        setResult(RESULT_OK, intent)
        finish()
        Log.d(TAG, "Image saved: ${photoFile.uri}")
    }

    private fun closeStream(outputStream: OutputStream) {
        try {
            outputStream.close()
        } catch (ignored: IOException) {}
    }

    private fun updateCameraSwitchButton() {
        binding.switchCamera.isEnabled = hasBackCamera() && hasFrontCamera()
    }

    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }
}
