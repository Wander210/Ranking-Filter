package com.gdd.rankingfilter.view.screen.video_player

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.gdd.rankingfilter.base.BaseFragment
import com.gdd.rankingfilter.data.model.Video
import com.gdd.rankingfilter.data.repository.CloudinaryRepository
import com.gdd.rankingfilter.databinding.FragmentVideoPlayerBinding
import com.gdd.rankingfilter.utils.VideoPlayerManager
import com.gdd.rankingfilter.viewmodel.MainViewModel
import com.gdd.rankingfilter.viewmodel.MainViewModelFactory

class VideoPlayerFragment : BaseFragment<FragmentVideoPlayerBinding>(FragmentVideoPlayerBinding::inflate) {

    private val args: VideoPlayerFragmentArgs by navArgs()
    private var videoList: List<Video> = emptyList()
    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(CloudinaryRepository(requireContext()))
    }
    private var currentPosition = -1
    private lateinit var videoAdapter: VideoFeedAdapter
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var playerManager: VideoPlayerManager

    companion object {
        private const val TAG = "VideoPlayerFragment"
        private const val PRELOAD_DISTANCE = 2     // items ahead to preload
        private const val CLEANUP_DISTANCE = 4     // items past which to clean up
        private const val PRELOAD_DELAY = 100L     // ms before preloading
    }

    override fun initData() {
        playerManager = VideoPlayerManager.getInstance(requireContext())
        videoAdapter = VideoFeedAdapter(this)
    }

    override fun setUpView() = with(binding) {
        setupViewPager()
        observeVideos()
    }

    override fun setUpListener() {}

    private fun setupViewPager() = with(binding.videoViewPager) {
        adapter = videoAdapter
        orientation = ViewPager2.ORIENTATION_VERTICAL
        offscreenPageLimit = 1 // keep 2 nearby pages loaded off-screen

        val recyclerView = getChildAt(0) as RecyclerView
        recyclerView.apply {
            // disable automatic item prefetching so we control when data/video loads
            layoutManager?.isItemPrefetchEnabled = false
            // tell RecyclerView its size won’t change — improves layout performance
            setHasFixedSize(true)
        }

        registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                handlePageChange(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                when (state) {
                    ViewPager2.SCROLL_STATE_DRAGGING -> { schedulePreloadAdjacent() }
                    ViewPager2.SCROLL_STATE_SETTLING -> {}
                    ViewPager2.SCROLL_STATE_IDLE -> { scheduleCleanup() }
                }
            }
        })
    }

    private fun getFragmentAt(position: Int): SingleVideoFragment? {
        return if (position >= 0 && position < videoList.size) {
            videoAdapter.getFragmentAt(position)
        } else null
    }

    private fun handlePageChange(newPosition: Int) {
        if (newPosition == currentPosition) return

        val oldPosition = currentPosition
        currentPosition = newPosition
        // Pause previous video
        if (oldPosition >= 0) getFragmentAt(oldPosition)?.pauseVideo()
        // Setup and play the current video
        getFragmentAt(newPosition)?.let { fragment ->
            if (fragment.isAdded) {
                fragment.setupPlayer()
                fragment.playVideo()
            }
        }
    }

    private fun schedulePreloadAdjacent() {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            preloadAdjacentVideos(currentPosition)
        }, PRELOAD_DELAY)
    }

    private fun preloadAdjacentVideos(centerPosition: Int) {
        if (centerPosition < 0) return

        val startPos = maxOf(0, centerPosition - PRELOAD_DISTANCE)
        val endPos = minOf(videoList.size - 1, centerPosition + PRELOAD_DISTANCE)

        for (position in startPos..endPos) {
            if (position != centerPosition) {
                val fragment = getFragmentAt(position)
                if (fragment != null && fragment.isAdded) {
                    fragment.setupPlayer()
                    Log.d(TAG, "Ensured player setup for position: $position")
                }
            }
        }
    }

    private fun scheduleCleanup() {
        handler.postDelayed({
            cleanupDistantVideos()
        }, 500)
    }

    private fun cleanupDistantVideos() {
        if (currentPosition < 0) return

        for (i in videoList.indices) {
            val distance = kotlin.math.abs(i - currentPosition)
            if (distance > CLEANUP_DISTANCE) {
                getFragmentAt(i)?.stopAndRelease()
                Log.d(TAG, "Cleaned up video at position: $i")
            }
        }
    }

    private fun observeVideos() {
        viewModel.videosByTag(args.tag).observe(viewLifecycleOwner) { videos ->
            videoList = videos
            videoAdapter.setList(videos)

            if (args.currentIndex < videos.size) {
                currentPosition = args.currentIndex
                binding.videoViewPager.setCurrentItem(args.currentIndex, false)

                // Delay đủ lâu để đảm bảo fragment đã được tạo và attached
                handler.postDelayed({
                    // Đảm bảo fragment đã sẵn sàng trước khi setup và play
                    getFragmentAt(args.currentIndex)?.let { fragment ->
                        if (fragment.isAdded) {
                            fragment.setupPlayer()
                            fragment.playVideo()
                        }
                    }
                    // Preload adjacent videos
                    preloadAdjacentVideos(args.currentIndex)
                }, 300) // Tăng delay lên 300ms để đảm bảo fragment đã được tạo
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Resume current video khi fragment resume
        if (currentPosition >= 0) {
            getFragmentAt(currentPosition)?.let { fragment ->
                if (fragment.isAdded) {
                    fragment.setupPlayer()
                    fragment.playVideo()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        getFragmentAt(currentPosition)?.pauseVideo()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        // Cleanup all fragments
        for (i in videoList.indices) {
            getFragmentAt(i)?.stopAndRelease()
        }
    }
}