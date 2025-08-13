package com.gdd.rankingfilter.view.screen.video_preview

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import androidx.navigation.fragment.navArgs
import com.gdd.rankingfilter.R
import com.gdd.rankingfilter.base.BaseFragment
import com.gdd.rankingfilter.databinding.FragmentVideoPreviewBinding
import java.io.File

class VideoPreviewFragment : BaseFragment<FragmentVideoPreviewBinding>(FragmentVideoPreviewBinding::inflate) {

    private var videoFile: File? = null
    private lateinit var mediaController: MediaController
    private val args: VideoPreviewFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Lấy path từ args và tạo File
        videoFile = File(args.videoPath)
    }

    override fun initData() {
        videoFile?.let { file ->
            if (file.exists()) {
                setupVideoView(file)
            } else {
                // File không tồn tại: có thể show thông báo hoặc navigate back
            }
        }
    }

    override fun setUpView() {
        setupMediaController()
    }

    override fun setUpListener() = with(binding) {
        btnBack.setOnClickListener { navigateBack() }
        btnDelete.setOnClickListener { showDeleteDialog() }
        btnShare.setOnClickListener {
            val action = VideoPreviewFragmentDirections
                .actionVideoPreviewFragmentToShareFragment(videoFile?.absolutePath)
            navigateWithAction(action)


        }
    }

    private fun setupMediaController() {
        mediaController = MediaController(requireContext())
        mediaController.setAnchorView(binding.videoView)
        binding.videoView.setMediaController(mediaController)
    }

    private fun setupVideoView(file: File) {
        try {
            val uri = Uri.fromFile(file)
            binding.videoView.setVideoURI(uri)

            binding.videoView.setOnPreparedListener {
                binding.videoView.start()
            }

            binding.videoView.setOnErrorListener { _, _, _ ->
                // Xử lý lỗi nếu cần
                true
            }

            binding.videoView.setOnCompletionListener {
                // Video kết thúc
            }

        } catch (e: Exception) {
            // Xử lý ngoại lệ nếu cần
        }
    }

    private fun showDeleteDialog() {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Do you want to delete this video?")
            .setMessage("This video will be permanently deleted and cannot be restored.")
            .setPositiveButton("Delete") { _, _ -> deleteVideo() }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
            resources.getColor(R.color.clip_dark, null)
        )
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
            resources.getColor(R.color.gray_400, null)
        )
    }

    private fun deleteVideo() {
        videoFile?.let { file ->
            try {
                if (file.exists() && file.delete()) {
                    binding.videoView.stopPlayback()
                    navigateBack()
                } else {
                    // Xóa thất bại
                }
            } catch (e: Exception) {
                // Xử lý lỗi khi xóa
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (binding.videoView.isPlaying) binding.videoView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.videoView.stopPlayback()
    }
}
