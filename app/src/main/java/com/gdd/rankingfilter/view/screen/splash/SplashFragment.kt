package com.gdd.rankingfilter.view.screen.splash

import android.os.Handler
import android.os.Looper
import com.gdd.rankingfilter.R
import com.gdd.rankingfilter.base.BaseFragment
import com.gdd.rankingfilter.databinding.FragmentSplashBinding

class SplashFragment : BaseFragment<FragmentSplashBinding>(FragmentSplashBinding::inflate) {
    override fun initData() {
    }

    override fun setUpView() {
        Handler(Looper.getMainLooper()).postDelayed({
            navigateTo(R.id.action_splashFragment_to_homeFragment)
        }, 3000)
    }

    override fun setUpListener() {
    }

}