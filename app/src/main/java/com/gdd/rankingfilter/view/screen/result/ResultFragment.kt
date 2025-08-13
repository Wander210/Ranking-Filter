package com.gdd.rankingfilter.view.screen.result

import android.net.Uri
import android.util.Log
import android.widget.MediaController
import androidx.core.net.toUri
import androidx.navigation.fragment.navArgs
import com.gdd.rankingfilter.R
import com.gdd.rankingfilter.base.BaseFragment
import com.gdd.rankingfilter.databinding.FragmentResultBinding

class ResultFragment : BaseFragment<FragmentResultBinding>(FragmentResultBinding::inflate) {

    private val args: ResultFragmentArgs by navArgs()
    private var videoUri: Uri? = null
    private var isVideoSaved = false

    override fun initData() {
        videoUri = args.videoUrl?.toUri()
        Log.d("ResultFragment", "Video URI: $videoUri")
        Log.d("ResultFragment", "Original video URL: ${args.videoUrl}")
    }

    override fun setUpView(): Unit = with(binding) {
        // Set up video player
        videoUri?.let { uri ->
            videoView.setVideoURI(uri)

            // Set up media controller
            val mediaController = MediaController(requireContext())
            mediaController.setAnchorView(videoView)
            videoView.setMediaController(mediaController)

            // Prepare video
            videoView.setOnPreparedListener { mediaPlayer ->
                // Hide play button when video is ready
                btnPlayPause.visibility = android.view.View.GONE
                // Auto play
                videoView.start()

                // Enable looping
                mediaPlayer.isLooping = false
            }

            // Show play button and restart video when completes
            videoView.setOnCompletionListener {
                // Restart video from beginning
                videoView.seekTo(0)
                videoView.start()

                // Optional: Show play button briefly
                btnPlayPause.visibility = android.view.View.VISIBLE
                btnPlayPause.setImageResource(R.drawable.ic_pause)

                // Hide play button after a short delay
                btnPlayPause.postDelayed({
                    if (videoView.isPlaying) {
                        btnPlayPause.visibility = android.view.View.GONE
                    }
                }, 1000)
            }
        }
    }

    override fun setUpListener() = with(binding) {
        btnBack.setOnClickListener {
            // Make sure to delete video before navigating back
            deleteVideoIfNotSaved()
            popBackStackTo(R.id.videoEditorFragment)
        }

        btnPlayPause.setOnClickListener {
            if (videoView.isPlaying) {
                videoView.pause()
                btnPlayPause.setImageResource(R.drawable.ic_play)
                btnPlayPause.visibility = android.view.View.VISIBLE
            } else {
                videoView.start()
                btnPlayPause.setImageResource(R.drawable.ic_pause)
                btnPlayPause.visibility = android.view.View.GONE
            }
        }

        btnSave.setOnClickListener {
            // Mark video as saved
            isVideoSaved = true
            // Navigate to ShareFragment
            navigateTo(R.id.action_resultFragment_to_shareFragment)
        }
    }

    override fun onPause() {
        super.onPause()
        // Pause video when fragment is paused
        if (binding.videoView.isPlaying) {
            binding.videoView.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        // Show play button when resuming if video is paused
        if (!binding.videoView.isPlaying) {
            binding.btnPlayPause.visibility = android.view.View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        deleteVideoIfNotSaved()
    }

    override fun onDestroy() {
        super.onDestroy()
        deleteVideoIfNotSaved()
    }

    private fun deleteVideoIfNotSaved() {
        // Delete video file if user didn't save it
        if (!isVideoSaved && videoUri != null) {
            try {
                Log.d("ResultFragment", "Attempting to delete video: $videoUri")
                val deleted = requireContext().contentResolver.delete(videoUri!!, null, null)
                if (deleted > 0) {
                    Log.d("ResultFragment", "Video deleted successfully from MediaStore")
                } else {
                    Log.w("ResultFragment", "Failed to delete video from MediaStore")
                }
            } catch (e: Exception) {
                Log.e("ResultFragment", "Error deleting video", e)
            }
        } else {
            Log.d("ResultFragment", "Not deleting video - isVideoSaved: $isVideoSaved, videoUri: $videoUri")
        }
    }
}