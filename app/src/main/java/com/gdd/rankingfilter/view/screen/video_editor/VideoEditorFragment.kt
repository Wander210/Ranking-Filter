package com.gdd.rankingfilter.view.screen.video_editor

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.CountDownTimer
import android.view.View
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import com.gdd.rankingfilter.R
import com.gdd.rankingfilter.base.BaseFragment
import com.gdd.rankingfilter.databinding.FragmentVideoEditorBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VideoEditorFragment : BaseFragment<FragmentVideoEditorBinding>(FragmentVideoEditorBinding::inflate) {

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var currentDelayTime = 0
    private var countDownTimer: CountDownTimer? = null
    private var isRecording = false

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }

    override fun initData() {
        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun setUpView() {
        // Check permissions first
        if (allPermissionsGranted()) {
            initializeCamera()
        } else {
            requestCameraPermissions()
        }

        // Setup UI elements
        setupDelayButtons()
        resetDelayButtons()
    }

    override fun setUpListener() = with(binding) {
        // Existing listener
        tvAddSound.setOnClickListener {
            navigateTo(R.id.action_videoEditorFragment_to_addSoundFragment)
        }

        // Camera controls
        btnSwitchCamera.setOnClickListener {
            switchCamera()
        }

        btnRecord.setOnClickListener {
            handleRecordButtonClick()
        }

        // Delay buttons
        btn1s.setOnClickListener { selectDelayTime(1, btn1s) }
        btn2s.setOnClickListener { selectDelayTime(2, btn2s) }
        btn3s.setOnClickListener { selectDelayTime(3, btn3s) }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermissions() {
        requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                initializeCamera()
                showToast("Permissions granted, initializing camera...")
            } else {
                showToast("Camera và audio permissions cần thiết để sử dụng tính năng này")
            }
        }
    }

    private fun initializeCamera() {
        if (!isAdded || context == null) {
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
                showToast("Camera initialized successfully")
            } catch (exc: Exception) {
                showToast("Camera initialization failed: ${exc.message}")
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return

        try {
            // Unbind all previous use cases
            cameraProvider.unbindAll()

            // Setup preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            // Setup video capture
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            // Select camera
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            // Bind to lifecycle
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                videoCapture
            )

        } catch (exc: Exception) {
            showToast("Camera binding failed: ${exc.message}")
            exc.printStackTrace()
        }
    }

    private fun setupDelayButtons() {
        // Set initial background for delay buttons
        val grayColor = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)

        binding.apply {
            btn1s.setBackgroundColor(grayColor)
            btn2s.setBackgroundColor(grayColor)
            btn3s.setBackgroundColor(grayColor)
        }
    }

    private fun resetDelayButtons() {
        val grayColor = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)

        binding.apply {
            btn1s.setBackgroundColor(grayColor)
            btn2s.setBackgroundColor(grayColor)
            btn3s.setBackgroundColor(grayColor)
        }

        currentDelayTime = 0
    }

    private fun selectDelayTime(seconds: Int, selectedButton: View) {
        // Reset all buttons to gray
        resetDelayButtons()

        // Set selected button to white
        val whiteColor = ContextCompat.getColor(requireContext(), android.R.color.white)
        selectedButton.setBackgroundColor(whiteColor)

        currentDelayTime = seconds
        showToast("Delay set to ${seconds}s")
    }

    private fun handleRecordButtonClick() {
        if (isRecording) {
            stopVideoRecording()
        } else {
            if (currentDelayTime > 0) {
                startCountdownAndRecord()
            } else {
                startVideoRecording()
            }
        }
    }

    private fun startCountdownAndRecord() {
        // Disable record button during countdown
        binding.btnRecord.isEnabled = false
        binding.countdownText.visibility = View.VISIBLE

        countDownTimer = object : CountDownTimer((currentDelayTime * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                binding.countdownText.text = secondsLeft.toString()
            }

            override fun onFinish() {
                binding.countdownText.visibility = View.GONE
                binding.btnRecord.isEnabled = true
                startVideoRecording()
            }
        }.start()
    }

    @SuppressLint("MissingPermission")
    private fun startVideoRecording() {
        val videoCapture = this.videoCapture ?: run {
            showToast("Camera not initialized")
            return
        }

        binding.btnRecord.isEnabled = false

        // Check if already recording
        val curRecording = recording
        if (curRecording != null) {
            curRecording.stop()
            recording = null
            return
        }

        // Create output file
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, "Movies/VideoEditor")
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(
                requireActivity().contentResolver,
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            )
            .setContentValues(contentValues)
            .build()

        // Start recording
        recording = videoCapture.output
            .prepareRecording(requireActivity(), mediaStoreOutputOptions)
            .apply {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(requireContext())) { recordEvent ->
                handleRecordEvent(recordEvent)
            }
    }

    private fun handleRecordEvent(recordEvent: VideoRecordEvent) {
        when (recordEvent) {
            is VideoRecordEvent.Start -> {
                binding.btnRecord.apply {
                    isEnabled = true
                    setBackgroundColor(
                        ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
                    )
                }
                isRecording = true
                showToast("Recording started")
            }

            is VideoRecordEvent.Finalize -> {
                if (!recordEvent.hasError()) {
                    showToast("Video saved successfully")
                } else {
                    recording?.close()
                    recording = null
                    showToast("Video recording failed: ${recordEvent.error}")
                }

                binding.btnRecord.apply {
                    isEnabled = true
                    setBackgroundColor(
                        ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)
                    )
                }
                isRecording = false
            }
        }
    }

    private fun stopVideoRecording() {
        recording?.stop()
        recording = null
        isRecording = false
    }

    private fun switchCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }

        // Rebind camera with new lens facing
        bindCameraUseCases()
        showToast("Camera switched")
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clean up resources
        countDownTimer?.cancel()
        recording?.close()
        cameraExecutor.shutdown()

        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()

        // Stop recording if active
        if (isRecording) {
            stopVideoRecording()
        }

        // Cancel countdown
        countDownTimer?.cancel()
        binding.countdownText.visibility = View.GONE
    }
}