package com.autodroid.trader.ui.qrcode

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.autodroid.trader.databinding.ActivityQrCodeScannerBinding
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QrCodeScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrCodeScannerBinding
    private lateinit var cameraExecutor: ExecutorService
    
    // Activity Result API launcher for camera permission
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, start camera
            startCamera()
        } else {
            // Permission denied, show message and close activity
            Toast.makeText(this, "Camera permission is required for QR code scanning", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrCodeScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // Set up close button
        binding.closeButton.setOnClickListener {
            finish()
        }
        
        // Check camera permission
        checkCameraPermission()
    }

    /**
     * Check camera permission
     */
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            // Request camera permission using the Activity Result API
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            // Permission already granted, start camera
            startCamera()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            
            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }
            
            // Image Analyzer
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, QrCodeAnalyzer { qrCode ->
                        runOnUiThread {
                            processQRCodeResult(qrCode)
                        }
                    })
                }
            
            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
            
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun processQRCodeResult(qrCode: String) {
        Log.d(TAG, "QR Code detected: $qrCode")
        
        // Return the result to the calling activity
        val resultIntent = Intent()
        resultIntent.putExtra("QR_RESULT", qrCode)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "QrCodeScannerActivity"
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    }

    private class QrCodeAnalyzer(
        private val onQrCodeDetected: (String) -> Unit
    ) : ImageAnalysis.Analyzer {

        private val scanner = BarcodeScanning.getClient()

        @SuppressLint("UnsafeOptInUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val imageBitmap = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                
                scanner.process(imageBitmap)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            // Handle QR code
                            if (barcode.format == Barcode.FORMAT_QR_CODE) {
                                val rawValue = barcode.rawValue
                                if (rawValue != null) {
                                    onQrCodeDetected(rawValue)
                                    return@addOnSuccessListener
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "QR Code detection failed", e)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }
}