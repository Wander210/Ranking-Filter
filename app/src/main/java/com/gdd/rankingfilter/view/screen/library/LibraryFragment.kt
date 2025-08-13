package com.gdd.rankingfilter.view.screen.library

import android.os.Environment
import android.util.Log
import androidx.recyclerview.widget.GridLayoutManager
import com.gdd.rankingfilter.base.BaseFragment
import com.gdd.rankingfilter.databinding.FragmentLibraryBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LibraryFragment : BaseFragment<FragmentLibraryBinding>(FragmentLibraryBinding::inflate) {

    private lateinit var videoAdapter: VideoAdapter
    private val videoList = mutableListOf<VideoItem>()

    override fun initData() {
        loadVideos()
    }

    override fun setUpView() {
        setupRecyclerView()
        updateUI()
    }

    override fun setUpListener() {
        binding.btnSettings.setOnClickListener {
            // Handle settings click
        }

        binding.btnHome.setOnClickListener {
            // Handle home click
        }

        binding.btnCamera.setOnClickListener {
            // Handle camera click
        }

        binding.btnPhoto.setOnClickListener {
            // Handle photo click
        }
    }

    private fun setupRecyclerView() {
        videoAdapter = VideoAdapter(videoList) { file ->
            // Handle video click
            playVideo(file)
        }

        val gridLayoutManager = GridLayoutManager(requireContext(), 2)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (videoList[position]) {
                    is VideoItem.Header -> 2 // Header spans full width
                    is VideoItem.Video -> 1  // Video items span 1 column
                }
            }
        }

        binding.recyclerViewVideos.apply {
            layoutManager = gridLayoutManager
            adapter = videoAdapter
        }
    }

    private fun loadVideos() {
        val moviesFolder = File(
            Environment.getExternalStorageDirectory(),
            "Movies/RankingFilter"
        )

        Log.d("LibraryFragment", "Looking for videos in: ${moviesFolder.absolutePath}")
        Log.d("LibraryFragment", "Folder exists: ${moviesFolder.exists()}")
        Log.d("LibraryFragment", "Is directory: ${moviesFolder.isDirectory}")

        if (!moviesFolder.exists() || !moviesFolder.isDirectory) {
            Log.w("LibraryFragment", "Movies folder not found or not a directory")
            videoList.clear()
            return
        }

        val videoFiles = moviesFolder.listFiles { file ->
            val isVideo = file.isFile && (file.extension.lowercase() in listOf("mp4", "avi", "mov", "mkv", "3gp", "webm", "flv"))
            if (isVideo) {
                Log.d("LibraryFragment", "Found video: ${file.name}, size: ${file.length()}")
            }
            isVideo
        }

        Log.d("LibraryFragment", "Total video files found: ${videoFiles?.size ?: 0}")

        if (videoFiles.isNullOrEmpty()) {
            Log.w("LibraryFragment", "No video files found")
            videoList.clear()
            return
        }

        // Group videos by month-year
        val groupedVideos = groupVideosByMonth(videoFiles.toList())
        videoList.clear()

        groupedVideos.forEach { (monthYear, files) ->
            Log.d("LibraryFragment", "Month: $monthYear, Videos: ${files.size}")
            // Add header item
            videoList.add(VideoItem.Header(monthYear))

            // Add video items
            files.forEach { file ->
                videoList.add(VideoItem.Video(file))
            }
        }

        Log.d("LibraryFragment", "Total items in list: ${videoList.size}")
    }

    private fun groupVideosByMonth(files: List<File>): Map<String, List<File>> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
        val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        return files.groupBy { file ->
            try {
                // Extract date from filename
                val fileName = file.nameWithoutExtension
                val date = dateFormat.parse(fileName)
                date?.let { monthYearFormat.format(it) } ?: "Unknown"
            } catch (e: Exception) {
                "Unknown"
            }
        }.toSortedMap(compareByDescending {
            try {
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).parse(it)
            } catch (e: Exception) {
                Date(0)
            }
        })
    }

    private fun updateUI() {
        if (videoList.isEmpty()) {
            binding.recyclerViewVideos.visibility = android.view.View.GONE
            binding.emptyStateLayout.visibility = android.view.View.VISIBLE
        } else {
            binding.emptyStateLayout.visibility = android.view.View.GONE
            binding.recyclerViewVideos.visibility = android.view.View.VISIBLE
            videoAdapter.notifyDataSetChanged()
        }
    }

    private fun playVideo(file: File) {
        // Handle video playback
        // You can implement video player here
    }

    // Data classes for video items
    sealed class VideoItem {
        data class Header(val monthYear: String) : VideoItem()
        data class Video(val file: File) : VideoItem()
    }
}