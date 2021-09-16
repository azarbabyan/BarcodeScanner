package com.artzarbo.barcodescanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.Image
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.artzarbo.barcodescanner.databinding.ActivityMainBinding
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    private var processingBarcode = AtomicBoolean(false)
    private lateinit var cameraExecutor: ExecutorService
    private var binding: ActivityMainBinding? = null
    private var camera:Camera? = null
    private var isFlashOn:Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        cameraExecutor = Executors.newSingleThreadExecutor()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        binding!!.flashButton.setOnClickListener {
            if (isFlashOn){
                isFlashOn = false
                camera?.cameraControl?.enableTorch(false)
                binding!!.flashButton.setImageResource(R.drawable.ic_baseline_flash_on_24)
            }else{
                isFlashOn = true
                camera?.cameraControl?.enableTorch(true)
                binding!!.flashButton.setImageResource(R.drawable.ic_baseline_flash_off_24)
            }
        }

    }

    override fun onResume() {
        super.onResume()
        processingBarcode.set(false)
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding!!.previewView.surfaceProvider)
                }
            val imageAnalysis = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor,BarcodeAnalyzer{ barcode ->
                        binding!!.scanResult.text = barcode
                        Log.d("valodik","Barcode: "+barcode)
                    })
                }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                // Unbind any bound use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to lifecycleOwner
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
                camera?.let { camera ->
                    if ( camera.cameraInfo.hasFlashUnit() ) {
                        binding?.flashButton?.visibility = View.VISIBLE
                        binding?.flashButton?.setImageResource(R.drawable.ic_baseline_flash_on_24)
                        isFlashOn = false
                        camera.cameraControl.enableTorch(false)
                    }
                }
            } catch (e: Exception) {
                Log.e("PreviewUseCase", "Binding failed! :(", e)
            }
        },ContextCompat.getMainExecutor(this))
    }





    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
}