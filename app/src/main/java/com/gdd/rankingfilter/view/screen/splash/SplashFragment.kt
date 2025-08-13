package com.gdd.rankingfilter.view.screen.splash

import androidx.lifecycle.ViewModelProvider
import com.gdd.rankingfilter.R
import com.gdd.rankingfilter.base.BaseFragment
import com.gdd.rankingfilter.data.repository.CloudinaryRepository
import com.gdd.rankingfilter.databinding.FragmentSplashBinding
import com.gdd.rankingfilter.viewmodel.MainViewModel
import com.gdd.rankingfilter.viewmodel.MainViewModelFactory

class SplashFragment : BaseFragment<FragmentSplashBinding>(FragmentSplashBinding::inflate) {

    // Thay thế hàm này bằng implementation thực tế
    private fun provideRepository(): CloudinaryRepository {
        return CloudinaryRepository(requireContext())
    }

    private val mainViewModel: MainViewModel by lazy {
        val repo = provideRepository()
        val factory = MainViewModelFactory(repo)
        ViewModelProvider(requireActivity(), factory)[MainViewModel::class.java]
    }

    override fun initData() {
        mainViewModel.loadAllData()
    }

    override fun setUpView() { }

    override fun setUpListener() {
        mainViewModel.isDataLoaded.observe(viewLifecycleOwner) { loaded ->
            if (loaded) navigateTo(R.id.action_splashFragment_to_homeFragment, true)
        }
    }
}