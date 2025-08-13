package com.gdd.rankingfilter.view.screen.video_editor

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.CountDownTimer
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.PermissionChecker
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.gdd.rankingfilter.R
import com.gdd.rankingfilter.base.BaseFragment
import com.gdd.rankingfilter.data.model.RankingItem
import com.gdd.rankingfilter.data.model.Song
import com.gdd.rankingfilter.data.model.SoundSelectionData
import com.gdd.rankingfilter.data.repository.CloudinaryRepository
import com.gdd.rankingfilter.databinding.FragmentVideoEditorBinding
import com.gdd.rankingfilter.view.custom.BracketRankingFilterView
import com.gdd.rankingfilter.view.custom.ListRankingFilterView
import com.gdd.rankingfilter.view.custom.circular_spinner.CircularSpinnerView
import com.gdd.rankingfilter.view.screen.home.HomeFragmentDirections
import com.gdd.rankingfilter.viewmodel.MainViewModel
import com.gdd.rankingfilter.viewmodel.MainViewModelFactory
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.random.Random

class VideoEditorFragment :
    BaseFragment<FragmentVideoEditorBinding>(FragmentVideoEditorBinding::inflate) {

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_FRONT
    private var delayingTime = 0
    private var recordingTime = 0
    private var recordingTimer: CountDownTimer? = null
    private var delayTimer: CountDownTimer? = null
    private var isRecording = false
    private var currentRankingItemPos = -1

    // Audio playback variables
    private var currentPlayer: MediaPlayer? = null
    private var availableSongs: List<Song> = emptyList()
    private var isAudioLoading = false

    // Selected sound data from AddSoundFragment
    private var selectedSoundData: SoundSelectionData? = null
    private var currentSelectedSong: Song? = null
    private var audioStartPosition: Int = 0 // Position to start audio playback

    private var currentPlayingSongData: SoundSelectionData? = null
    private var currentPlayingSong: Song? = null

    private lateinit var rankingItemList: List<RankingItem>
    private lateinit var circularSpinner: CircularSpinnerView
    private lateinit var listView: ListRankingFilterView
    private lateinit var bracketView: BracketRankingFilterView
    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(CloudinaryRepository(requireContext()))
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }

    override fun initData() = with(binding) {
        // Check and request camera permissions
        if (allPermissionsGranted()) initializeCamera()
        else permissionLauncher.launch(REQUIRED_PERMISSIONS)

        // Observe the selected position from the RankingItemFragment
        val savedStateHandle = findNavController().currentBackStackEntry?.savedStateHandle
        savedStateHandle?.getLiveData<Int>("selectedPosition")?.observe(viewLifecycleOwner) { pos ->
            currentRankingItemPos = pos
        }

        // Observe the selected sound data from the AddSoundFragment
        savedStateHandle?.getLiveData<SoundSelectionData>("soundSelectionData")
            ?.observe(viewLifecycleOwner) { soundData ->
                Log.e(
                    "flower",
                    "Selected sound: ${soundData.selectedPosition} | Start time: ${soundData.clipStartTimeMs}"
                )
                selectedSoundData = soundData

                // Find the selected song and play it
                if (soundData.selectedPosition >= 0 && soundData.selectedPosition < availableSongs.size) {
                    currentSelectedSong = availableSongs[soundData.selectedPosition]
                    audioStartPosition = soundData.clipStartTimeMs.toInt()

                    // Stop current audio and play the selected one
                    stopCurrentAudio()
                    playSelectedAudio()
                }
            }

        //Load all songs
        viewModel.allSongs.observe(viewLifecycleOwner) { songs ->
            availableSongs =
                songs.filter { it.public_id.isNotEmpty() && it.secure_url.isNotEmpty() }
        }

        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()
        viewModel.allRankingItems.observe(viewLifecycleOwner) { list ->
            rankingItemList = list
            var coverUrlList = mutableListOf<String>()
            list.forEach { it -> coverUrlList.add(it.coverUrl) }
            circularSpinner.setItems(coverUrlList, currentRankingItemPos)
            stopCurrentAudio()
            bindCustomViewAndPlayAudio(list[0])
        }
    }

    override fun setUpView() = with(binding) {
        listView = listRankingFilterView
        bracketView = bracketRankingFilterView
        circularSpinner = circularSpinnerView

        resetDelayButtons()
        resetRecordButtons()
    }

    override fun setUpListener() = with(binding) {
        //Sound
        tvAddSound.setOnClickListener { navigateTo(R.id.action_videoEditorFragment_to_addSoundFragment) }
        btnCancel.setOnClickListener {
            stopCurrentAudio()
            // Clear selected sound data
            selectedSoundData = null
            currentSelectedSong = null
            currentPlayingSongData = null
            currentPlayingSong = null
            audioStartPosition = 0
            tvAddSound.text = getString(R.string.add_sound)
            btnCancel.visibility = View.GONE
        }
        btnSwitchCamera.setOnClickListener { switchCamera() }
        btnClock.setOnClickListener { lDelay.visibility = View.VISIBLE }
        btnFilter.setOnClickListener { navigateTo(R.id.action_videoEditorFragment_to_rankingItemFragment) }
        btnRecord.setOnClickListener { handleRecordButtonClick() }

        // Delay buttons
        btn0s.setOnClickListener { selectDelayTime(0, btn0s, tv0s) }
        btn3s.setOnClickListener { resetDelayButtons() }
        btn5s.setOnClickListener { selectDelayTime(5, btn5s, tv5s) }
        btn10s.setOnClickListener { selectDelayTime(10, btn10s, tv10s) }
        btn15s.setOnClickListener { selectDelayTime(15, btn15s, tv15s) }

        //record time button
        btn30s.setOnClickListener { selectRecordTime(30, btn30s, tv30s) }
        btn1m.setOnClickListener { selectRecordTime(60, btn1m, tv1m) }
        btn2m.setOnClickListener { selectRecordTime(120, btn2m, tv2m) }

        circularSpinner.onItemSelectedListener = { position ->
            if (!isRecording && ::rankingItemList.isInitialized && position < rankingItemList.size)
                bindCustomViewAndPlayAudio(rankingItemList[position])
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.all { it }) {
            initializeCamera()
        }
    }

    private fun bindCustomViewAndPlayAudio(rankingItem: RankingItem) {
        try {
            // Stop current audio before binding new view
            stopCurrentAudio()

            if (rankingItem.type == "list") {
                listView.visibility = View.VISIBLE
                bracketView.visibility = View.GONE
                binding.listRankingFilterView.setProfileData(rankingItem)
            } else {
                listView.visibility = View.GONE
                bracketView.visibility = View.VISIBLE
            }
        } catch (_: Exception) {
        }

        // Play audio after binding view
        if (currentSelectedSong != null) {
            // Play the selected song if available
            playSelectedAudio()
        } else {
            // Play random audio if no song is selected
            playRandomAudio()
        }
    }

    private fun playSelectedAudio() {
        currentSelectedSong?.let { song ->
            currentPlayingSong = song
            currentPlayingSongData = selectedSoundData
            playAudio(song, audioStartPosition)
        }
    }

    private fun playRandomAudio() {
        if (availableSongs.isEmpty() || isAudioLoading) return

        // Select random song
        val randomSong = availableSongs[Random.nextInt(availableSongs.size)]
        // Play the selected song from beginning
        currentPlayingSong = randomSong
        currentPlayingSongData = SoundSelectionData(
            selectedPosition = availableSongs.indexOf(randomSong),
            clipStartTimeMs = 0L
        )

        // Play the selected song from beginning
        playAudio(randomSong, 0)
    }

    private fun playAudio(song: Song, startPositionMs: Int = 0) {
        Log.d("flower", "Starting playback for: ${song.public_id} from ${startPositionMs}ms")

        // Stop current player if exists
        if (currentPlayer != null) {
            stopCurrentAudio()
        }

        isAudioLoading = true

        try {
            currentPlayer = MediaPlayer().apply {
                setDataSource(song.secure_url)

                setOnPreparedListener { mp ->
                    Log.d("flower", "MediaPlayer prepared for: ${song.public_id}")
                    isAudioLoading = false

                    // Update UI to show song name
                    binding.tvAddSound.text = song.public_id
                    binding.btnCancel.visibility = View.VISIBLE

                    // Seek to the specified position and start playback
                    mp.seekTo(startPositionMs)
                    mp.start()

                    Log.d("flower", "Audio playback started from ${startPositionMs}ms")
                }

                setOnErrorListener { _, what, extra ->
                    Log.e("flower", "MediaPlayer error: what=$what, extra=$extra")
                    isAudioLoading = false
                    stopCurrentAudio()
                    // Reset UI on error
                    binding.tvAddSound.text = getString(R.string.add_sound)
                    binding.btnCancel.visibility = View.GONE
                    true
                }

                setOnCompletionListener {
                    Log.d("flower", "Playback completed")
                    stopCurrentAudio()
                    // Reset UI on completion
                    binding.tvAddSound.text = getString(R.string.add_sound)
                    binding.btnCancel.visibility = View.GONE
                }

                prepareAsync()
            }

        } catch (e: IOException) {
            Log.e("flower", "Error preparing MediaPlayer", e)
            isAudioLoading = false
            stopCurrentAudio()
            // Reset UI on error
            binding.tvAddSound.text = getString(R.string.add_sound)
            binding.btnCancel.visibility = View.GONE
        }
    }

    private fun stopCurrentAudio() {
        currentPlayer?.let { player ->
            try {
                if (player.isPlaying) player.stop()
                player.release()
            } catch (e: Exception) {
                Log.e("flower", "Error stopping MediaPlayer", e)
            }
        }
        currentPlayer = null
        isAudioLoading = false
    }

    private fun resumeAudioAfterCountdown() {
        // Resume audio after countdown based on current selection
        currentPlayingSong?.let { song ->
            val startPosition = currentPlayingSongData?.clipStartTimeMs?.toInt() ?: 0
            if (!isRecording) {
                playAudio(song, startPosition)
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
                    it.surfaceProvider = binding.viewFinder.surfaceProvider
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

    private fun resetDelayButtons() = with(binding) {
        btn0s.setBackgroundResource(R.drawable.bg_black)
        btn3s.setBackgroundResource(R.drawable.bg_white)
        btn5s.setBackgroundResource(R.drawable.bg_black)
        btn10s.setBackgroundResource(R.drawable.bg_black)
        btn15s.setBackgroundResource(R.drawable.bg_black)

        tv0s.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        tv3s.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        tv5s.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        tv10s.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        tv15s.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

        delayingTime = 3
        lDelay.visibility = View.GONE
    }

    private fun resetRecordButtons() = with(binding) {
        btn30s.setBackgroundResource(R.drawable.bg_dark_grey)
        btn1m.setBackgroundResource(R.drawable.bg_dark_grey)
        btn2m.setBackgroundResource(R.drawable.bg_dark_grey)

        tv30s.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        tv1m.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        tv2m.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

        recordingTime = 0
    }

    private fun selectDelayTime(seconds: Int, selectedButton: View, selectedTextView: TextView) {
        resetDelayButtons()
        //Reset btn3s
        binding.btn3s.setBackgroundResource(R.drawable.bg_black)
        binding.tv3s.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        // Set selected button to white
        selectedButton.setBackgroundResource(R.drawable.bg_white)
        selectedTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))

        delayingTime = seconds
    }

    private fun selectRecordTime(seconds: Int, selectedButton: View, selectedTextView: TextView) {
        resetRecordButtons()
        // Set selected button to white
        selectedButton.setBackgroundResource(R.drawable.bg_white)
        selectedTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        recordingTime = seconds
    }

    private fun handleRecordButtonClick() {
        if (isRecording) {
            stopVideoRecording()
        } else {
            // Stop audio when starting to record
            stopCurrentAudio()

            if (delayingTime > 0) {
                startCountdownAndDelay()
            } else {
                if (recordingTime > 0) startCountdownAndRecord()
                startVideoRecording()
            }
        }
    }

    private fun startCountdownAndDelay() {
        // Disable record button during countdown
        binding.btnRecord.isEnabled = false
        binding.countdownText.visibility = View.VISIBLE

        delayTimer = object : CountDownTimer((delayingTime * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                binding.countdownText.text = secondsLeft.toString()
            }

            override fun onFinish() {
                binding.countdownText.visibility = View.GONE
                binding.btnRecord.isEnabled = true

                // Resume audio after countdown
                resumeAudioAfterCountdown()

                if (recordingTime > 0) startCountdownAndRecord()
                startVideoRecording()
            }
        }.start()
    }

    private fun startCountdownAndRecord() {
        recordingTimer = object : CountDownTimer((recordingTime * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                stopVideoRecording()
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
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/RankingFilter")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(
                requireActivity().contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            )
            .setContentValues(contentValues)
            .build()

        // Start recording
        recording = videoCapture.output
            .prepareRecording(requireActivity(), mediaStoreOutputOptions)
            .apply {
                if (PermissionChecker.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.RECORD_AUDIO
                    ) ==
                    PermissionChecker.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(requireContext())) { recordEvent ->
                handleRecordEvent(recordEvent)
            }
    }

    private fun handleRecordEvent(recordEvent: VideoRecordEvent) {
        // Check if fragment is still attached to avoid crashes
        if (!isAdded || context == null) {
            Log.w("VideoEditor", "Fragment not attached, ignoring record event")
            return
        }

        with(binding) {
            when (recordEvent) {
                is VideoRecordEvent.Start -> {
                    btnFilter.visibility = View.GONE
                    cFilter.visibility = View.GONE
                    btnSwitchCamera.visibility = View.GONE
                    cSwitchCamera.visibility = View.GONE
                    btnClock.visibility = View.GONE
                    cClock.visibility = View.GONE
                    btn1m.visibility = View.GONE
                    tv1m.visibility = View.GONE
                    btn30s.visibility = View.GONE
                    tv30s.visibility = View.GONE
                    btn2m.visibility = View.GONE
                    tv2m.visibility = View.GONE
                    circularSpinnerView.visibility = View.INVISIBLE
                    lAddSound.visibility = View.GONE
                    binding.btnRecord.apply {
                        isEnabled = true
                        setImageResource(R.drawable.bg_record_button_selected)
                    }
                    isRecording = true
                    showToast("Recording started")
                }

                is VideoRecordEvent.Finalize -> {
                    if (!recordEvent.hasError()) {
                        val action = VideoEditorFragmentDirections.actionVideoEditorFragmentToResultFragment(
                            videoUrl = recordEvent.outputResults.outputUri.toString()
                        )
                        navigateWithAction(action)
                    } else {
                        recording?.close()
                        recording = null
                        showToast("Video recording failed: ${recordEvent.error}")
                    }

                    binding.btnRecord.apply {
                        isEnabled = true
                        setImageResource(R.drawable.bg_record_button_selected)
                    }
                    isRecording = false

                    // Show hidden UI elements again
                    btnFilter.visibility = View.VISIBLE
                    cFilter.visibility = View.VISIBLE
                    btnSwitchCamera.visibility = View.VISIBLE
                    cSwitchCamera.visibility = View.VISIBLE
                    btnClock.visibility = View.VISIBLE
                    cClock.visibility = View.VISIBLE
                    btn1m.visibility = View.VISIBLE
                    tv1m.visibility = View.VISIBLE
                    btn30s.visibility = View.VISIBLE
                    tv30s.visibility = View.VISIBLE
                    btn2m.visibility = View.VISIBLE
                    tv2m.visibility = View.VISIBLE
                    circularSpinnerView.visibility = View.VISIBLE
                    lAddSound.visibility = View.VISIBLE
                    btnRecord.setImageResource(R.drawable.bg_record_button_unselected)
                    // Resume audio after recording stops
                    resumeAudioAfterCountdown()
                }
            }
        }
    }

    private fun stopVideoRecording() {
        recording?.let { activeRecording ->
            try {
                activeRecording.stop()
                Log.d("VideoEditor", "Recording stopped successfully")
            } catch (e: Exception) {
                Log.e("VideoEditor", "Error stopping recording", e)
            }
        }
        recording = null
        isRecording = false

        // Cancel recording timer if active
        recordingTimer?.cancel()
        recordingTimer = null
    }

    private fun switchCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
            CameraSelector.LENS_FACING_BACK
        else
            CameraSelector.LENS_FACING_FRONT

        // Rebind camera with new lens facing
        bindCameraUseCases()
        showToast("Camera switched")
    }

    private fun showToast(message: String) {
        if (isAdded && context != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        stopCurrentAudio()
        // Stop recording if active
        if (isRecording) {
            stopVideoRecording()
        }
        // Cancel countdown
        delayTimer?.cancel()
        recordingTimer?.cancel()
        binding.countdownText.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up resources
        stopCurrentAudio()
        delayTimer?.cancel()
        recordingTimer?.cancel()
        recording?.close()
        cameraExecutor.shutdown()

        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}