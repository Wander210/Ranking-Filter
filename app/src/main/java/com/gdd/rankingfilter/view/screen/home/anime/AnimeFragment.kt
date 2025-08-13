package com.gdd.rankingfilter.view.screen.home.anime

import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.gdd.rankingfilter.base.BaseFragment
import com.gdd.rankingfilter.data.model.Video
import com.gdd.rankingfilter.data.repository.CloudinaryRepository
import com.gdd.rankingfilter.databinding.FragmentAnimeBinding
import com.gdd.rankingfilter.epoxy.controller.VideoListController
import com.gdd.rankingfilter.view.screen.home.HomeFragmentDirections
import com.gdd.rankingfilter.viewmodel.MainViewModel
import com.gdd.rankingfilter.viewmodel.MainViewModelFactory

class AnimeFragment : BaseFragment<FragmentAnimeBinding>(FragmentAnimeBinding::inflate) {

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(CloudinaryRepository(requireContext()))
    }
    private lateinit var videoController: VideoListController
    private var currentVideoList: List<Video> = emptyList()

    override fun initData() {
        viewModel.loadVideos()
        videoController = VideoListController(
            context = requireContext(),
            onVideoClick = { video -> openVideoPlayer(video) }
        )
    }

    override fun setUpView() = with (binding) {
        epoxyRecyclerView.setController(videoController)

        viewModel.videosByTag("anime").observe(viewLifecycleOwner) { list ->
            currentVideoList = list
            videoController.setData(list)
            loadingProgressBar.isVisible = false
            epoxyRecyclerView.isVisible = true
        }
    }

    override fun setUpListener() {
    }

    private fun openVideoPlayer(video: Video) {
        val currentIndex = currentVideoList.indexOfFirst { it.public_id == video.public_id }
        val action = HomeFragmentDirections.actionHomeFragmentToVideoPlayerFragment(
            currentIndex = currentIndex,
            tag = "anime"
        )
        navigateWithAction(action)
    }
}