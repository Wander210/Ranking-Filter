package com.gdd.rankingfilter.view.screen.home.trending

import android.util.Log
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.gdd.rankingfilter.base.BaseFragment
import com.gdd.rankingfilter.data.model.Video
import com.gdd.rankingfilter.data.repository.CloudinaryRepository
import com.gdd.rankingfilter.databinding.FragmentTrendingBinding
import com.gdd.rankingfilter.view.screen.home.VideoAdapter
import kotlinx.coroutines.launch

class TrendingFragment :
    BaseFragment<FragmentTrendingBinding>(FragmentTrendingBinding::inflate) {

    private lateinit var videoAdapter: VideoAdapter
    private lateinit var repository: CloudinaryRepository

    override fun initData() {
    }

    override fun setUpView() {
        setupRecyclerView()
        repository = CloudinaryRepository(requireContext())
        loadVideos()
    }

    override fun setUpListener() {
    }

    private fun setupRecyclerView() {
        videoAdapter = VideoAdapter(mutableListOf()) { video ->
            openVideoPlayer(video)
        }
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = videoAdapter
        }
    }

    private fun openVideoPlayer(video: Video) {

    }

    private fun loadVideos() = with(binding) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val videos = repository.getVideos()
                Log.d("TrendingFragment", "Fetched ${videos.size} videos")
                videos.forEach { video ->
                    Log.d("VideoDebug", "tags = ${video.tags}")
                    Log.d("VideoDebug", "context = ${video.context}")
                    Log.d("VideoDebug", "custom = ${video.context?.custom}")
                    val custom = video.context?.custom
                    if (custom != null) {

                            Log.d("CustomField", "${custom.author} = ${custom.likes}")

                    }
                }
                videoAdapter.updateVideos(videos)
            } catch (e: Exception) {
                Log.e("TrendingFragment", "Error loading videos", e)
                Toast.makeText(requireContext(), "Lỗi tải video", Toast.LENGTH_SHORT).show()
            } finally {
                loadingProgressBar.isVisible = false
                recyclerView.isVisible = true
            }
        }
    }

}