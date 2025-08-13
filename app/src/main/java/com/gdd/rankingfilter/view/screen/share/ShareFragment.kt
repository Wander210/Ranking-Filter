package com.gdd.rankingfilter.view.screen.share

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.MediaController
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.navigation.fragment.navArgs
import com.gdd.rankingfilter.R
import com.gdd.rankingfilter.base.BaseFragment
import com.gdd.rankingfilter.databinding.FragmentShareBinding
import java.io.File

class ShareFragment : BaseFragment<FragmentShareBinding>(FragmentShareBinding::inflate) {

    private val args: ShareFragmentArgs by navArgs()
    private var videoUri: Uri? = null

    override fun initData() {
        videoUri = args.videoUrl?.toUri()

        Log.d("ShareFragment", "initData: raw=${args.videoUrl}")
        Log.d("ShareFragment", "initData: normalized=$videoUri")

        if (videoUri == null) {
            Log.e("ShareFragment", "videoUri is null after parsing!")
        } else {
            Log.d("ShareFragment", "videoUri scheme: ${videoUri!!.scheme}")
            Log.d("ShareFragment", "videoUri path: ${videoUri!!.path}")
            if (videoUri!!.scheme == "file" || videoUri!!.scheme == null) {
                val filePath = videoUri!!.path ?: videoUri.toString()
                val file = File(filePath)
                Log.d("ShareFragment", "Checking file exists: ${file.absolutePath} = ${file.exists()}")
            }
        }
    }


    override fun setUpView(): Unit = with(binding) {
        Log.d("ShareFragment", "setUpView called")

        // Set up video player
        videoUri?.let { uri ->
            Log.d("ShareFragment", "Setting up video with URI: $uri")
            videoView.setVideoURI(uri)
            videoView.requestFocus()

            val mediaController = MediaController(requireContext())
            mediaController.setAnchorView(videoView)
            videoView.setMediaController(mediaController)

            videoView.setOnPreparedListener { mediaPlayer ->
                btnPlayPause.visibility = View.VISIBLE
                btnPlayPause.setImageResource(R.drawable.ic_pause) // Thay đổi thành pause vì video sẽ tự động phát
                mediaPlayer.isLooping = true

                // Show first frame and start video
                try {
                    videoView.seekTo(1)
                    videoView.start() // ← THÊM DÒNG NÀY để tự động phát video
                } catch (e: Exception) {
                    Log.w("ShareFragment", "seekTo(1) or start() failed", e)
                }
            }

            videoView.setOnCompletionListener {
                videoView.seekTo(0)
                videoView.start()
            }

            // Thêm error listener để debug
            videoView.setOnErrorListener { mediaPlayer, what, extra ->
                Log.e("ShareFragment", "VideoView error: what=$what, extra=$extra")
                Toast.makeText(requireContext(), "Error playing video", Toast.LENGTH_SHORT).show()
                false // return false to let default error handling proceed
            }
        } ?: run {
            Log.e("ShareFragment", "videoUri is null")
            Toast.makeText(requireContext(), "Video URI is null", Toast.LENGTH_SHORT).show()
        }
    }
    override fun setUpListener(): Unit = with(binding) {
        btnBack.setOnClickListener { navigateBack() }

        btnPlayPause.setOnClickListener {
            if (videoView.isPlaying) {
                videoView.pause()
                btnPlayPause.setImageResource(R.drawable.ic_play)
                btnPlayPause.visibility = View.VISIBLE
            } else {
                videoView.start()
                btnPlayPause.setImageResource(R.drawable.ic_pause)
                // Keep play button visible for share screen
                btnPlayPause.visibility = View.VISIBLE
            }
        }

        // Share buttons
        btnFacebook.setOnClickListener {
            shareToApp("com.facebook.katana", "Facebook")
        }

        btnInstagram.setOnClickListener {
            shareToApp("com.instagram.android", "Instagram")
        }

        btnTiktok.setOnClickListener {
            shareToApp("com.zhiliaoapp.musically", "TikTok")
        }

        btnOther.setOnClickListener {
            shareToOtherApps()
        }

        btnAllChallenge.setOnClickListener {
            popBackStackTo(R.id.resultFragment, true)
            popBackStackTo(R.id.videoEditorFragment, true)
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun shareToApp(packageName: String, appName: String) {
        Log.d("ShareFragment", "shareToApp called for $appName")

        videoUri?.let { uri ->
            try {
                val shareUri = getContentUriForSharing(uri)
                Log.d("ShareFragment", "Share URI: $shareUri")

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "video/*"
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    setPackage(packageName)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                if (shareIntent.resolveActivity(requireContext().packageManager) != null) {
                    Log.d("ShareFragment", "$appName found, starting share activity")
                    startActivity(shareIntent)
                } else {
                    Log.d("ShareFragment", "$appName not found, showing toast and opening Play Store")
                    showToast("$appName is not installed")
                    openPlayStore(packageName)
                }
            } catch (e: Exception) {
                Log.e("ShareFragment", "Error sharing to $appName", e)
                showToast("Error sharing to $appName: ${e.message}")
            }
        } ?: run {
            Log.e("ShareFragment", "videoUri is null when trying to share to $appName")
            showToast("Video not available")
        }
    }

    private fun shareToOtherApps() {
        Log.d("ShareFragment", "shareToOtherApps called")

        videoUri?.let { uri ->
            try {
                val shareUri = getContentUriForSharing(uri)
                Log.d("ShareFragment", "Share URI for other apps: $shareUri")

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "video/*"
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(shareIntent, "Share video via")
                if (chooser.resolveActivity(requireContext().packageManager) != null) {
                    startActivity(chooser)
                } else {
                    showToast("No apps available to share video")
                }
            } catch (e: Exception) {
                Log.e("ShareFragment", "Error sharing video", e)
                showToast("Error sharing video: ${e.message}")
            }
        } ?: run {
            Log.e("ShareFragment", "videoUri is null when trying to share to other apps")
            showToast("Video not available")
        }
    }

    private fun openPlayStore(packageName: String) {
        Log.d("ShareFragment", "Opening Play Store for $packageName")
        try {
            val intent = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {
                // Fallback to web browser
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    "https://play.google.com/store/apps/details?id=$packageName".toUri()
                )
                startActivity(webIntent)
            }
        } catch (e: Exception) {
            Log.e("ShareFragment", "Error opening Play Store for $packageName", e)
            showToast("Cannot open Play Store")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        // Pause video when fragment is paused
        if (binding.videoView.isPlaying) {
            binding.videoView.pause()
            binding.btnPlayPause.setImageResource(R.drawable.ic_play)
        }
    }

    override fun onResume() {
        super.onResume()
        // Always show play button when resuming
        binding.btnPlayPause.visibility = View.VISIBLE
        if (!binding.videoView.isPlaying) {
            binding.btnPlayPause.setImageResource(R.drawable.ic_play)
        }
    }

    private fun getContentUriForSharing(uri: Uri): Uri {
        Log.d("ShareFragment", "getContentUriForSharing input: $uri")

        return try {
            when (uri.scheme) {
                "content" -> {
                    Log.d("ShareFragment", "Already content URI, returning as-is")
                    uri
                }
                "file" -> {
                    val file = File(uri.path!!)
                    Log.d("ShareFragment", "Converting file URI to content URI: ${file.absolutePath}")
                    if (file.exists()) {
                        val contentUri = FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.provider",
                            file
                        )
                        Log.d("ShareFragment", "Generated content URI: $contentUri")
                        contentUri
                    } else {
                        Log.e("ShareFragment", "File does not exist: ${file.absolutePath}")
                        uri // fallback
                    }
                }
                null -> {
                    // Assume it's a file path
                    val file = File(uri.toString())
                    Log.d("ShareFragment", "Treating as file path: ${file.absolutePath}")
                    if (file.exists()) {
                        val contentUri = FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.provider",
                            file
                        )
                        Log.d("ShareFragment", "Generated content URI from path: $contentUri")
                        contentUri
                    } else {
                        Log.e("ShareFragment", "File does not exist: ${file.absolutePath}")
                        uri // fallback
                    }
                }
                else -> {
                    Log.d("ShareFragment", "Unknown scheme: ${uri.scheme}, returning as-is")
                    uri // fallback for http, https, etc.
                }
            }
        } catch (e: Exception) {
            Log.e("ShareFragment", "Error converting URI: $uri", e)
            uri // fallback
        }
    }

}