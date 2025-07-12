package com.gdd.rankingfilter.view.screen.home.makeup

import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.gdd.rankingfilter.base.BaseFragment
import com.gdd.rankingfilter.data.model.Video
import com.gdd.rankingfilter.data.repository.CloudinaryRepository
import com.gdd.rankingfilter.databinding.FragmentMakeupBinding
import com.gdd.rankingfilter.epoxy.controller.VideoEpoxyController
import com.gdd.rankingfilter.viewmodel.MainViewModel
import com.gdd.rankingfilter.viewmodel.MainViewModelFactory

class MakeupFragment :
    BaseFragment<FragmentMakeupBinding>(FragmentMakeupBinding::inflate) {

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(CloudinaryRepository(requireContext()))
    }
    private lateinit var videoController: VideoEpoxyController

    override fun initData() {
        videoController = VideoEpoxyController(
            context = requireContext(),
            onVideoClick = { video -> openVideoPlayer(video) }
        )
    }

    override fun setUpView() {
        binding.epoxyRecyclerView.setController(videoController)

        viewModel.videosByTag("makeup")
            .observe(viewLifecycleOwner) { list ->
                videoController.setData(list)
                binding.loadingProgressBar.isVisible = false
                binding.epoxyRecyclerView.isVisible = true
            }
    }

    override fun setUpListener() {
    }

    private fun openVideoPlayer(video: Video) {
    }
}