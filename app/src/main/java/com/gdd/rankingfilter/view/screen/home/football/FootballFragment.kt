package com.gdd.rankingfilter.view.screen.home.football

import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.gdd.rankingfilter.base.BaseFragment
import com.gdd.rankingfilter.data.model.Video
import com.gdd.rankingfilter.data.repository.CloudinaryRepository
import com.gdd.rankingfilter.databinding.FragmentFootballBinding
import com.gdd.rankingfilter.epoxy.controller.VideoListController
import com.gdd.rankingfilter.view.screen.home.HomeFragmentDirections
import com.gdd.rankingfilter.viewmodel.MainViewModel
import com.gdd.rankingfilter.viewmodel.MainViewModelFactory

class FootballFragment : BaseFragment<FragmentFootballBinding>(FragmentFootballBinding::inflate) {

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(CloudinaryRepository(requireContext()))
    }
    private lateinit var videoController: VideoListController
    private var currentVideoList: List<Video> = emptyList()

    override fun initData() {
        videoController = VideoListController(
            context = requireContext(),
            onVideoClick = { video -> openVideoPlayer(video) }
        )
    }

    override fun setUpView() = with (binding) {
        epoxyRecyclerView.setController(videoController)

        viewModel.videosByTag("football").observe(viewLifecycleOwner) { list ->
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
            tag = "football"
        )
        navigateWithAction(action)
    }
}