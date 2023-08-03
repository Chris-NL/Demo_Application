package com.example.demoapplication

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import com.example.demoapplication.databinding.FragmentCameraBinding
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {
    val TAG = "CameraFragment"

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!
    private var navigatedBack = false

    private val _scanResult: MutableLiveData<String?> = MutableLiveData()
    private val scanResult: LiveData<String?>
        get() = _scanResult

    private lateinit var previewView: PreviewView
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageAnalysis: ImageAnalysis

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)

        previewView = binding.cameraPreviewView
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireActivity())
        requestCamera()

        cameraExecutor = Executors.newSingleThreadExecutor()

        scanResult.observe(requireActivity(), Observer {
            if (!navigatedBack) {
                binding.tvMessage.text = getString(R.string.camera_scan_check)
                //viewModel.scannedTVCode = it
                val cameraProvider = cameraProviderFuture.get()
                imageAnalysis.clearAnalyzer()
                cameraProvider.unbindAll()
                cameraExecutor.shutdown()
                navigatedBack = true
                findNavController().navigateUp()
            }
        })

        return binding.root
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.camera_permission_denied))
            .setPositiveButton(R.string.generic_ok) { _, _ ->
                findNavController().navigate(R.id.action_cameraFragment_to_FirstFragment)
            }
            .create()
            .show()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                showPermissionDeniedDialog()
            }
        }

    private fun requestCamera() {
        when {
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showPermissionDeniedDialog()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                bindCameraPreview(cameraProvider)
            } catch (e: ExecutionException) {
                Toast.makeText(context, "Error starting camera " + e.message, Toast.LENGTH_SHORT)
                    .show()
            } catch (e: InterruptedException) {
                Toast.makeText(context, "Error starting camera " + e.message, Toast.LENGTH_SHORT)
                    .show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraPreview(cameraProvider: ProcessCameraProvider) {
        previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        val preview: Preview = Preview.Builder()
            .build()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview.setSurfaceProvider(previewView.surfaceProvider)

        imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(
            cameraExecutor,
            QRCodeImageAnalyzer(object : QRCodeFoundListener {
                override fun onQRCodeFound(qrCode: String?) {
                    Log.d(TAG, "QRCode found $qrCode")
                    if (qrCode != null) {
                            _scanResult.postValue(qrCode)
                    }
                }

                override fun qrCodeNotFound() {
                    Log.d(TAG, "QRCode not found")
                }
            })
        )

        val camera: Camera =
            cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, imageAnalysis, preview)

    }

}
