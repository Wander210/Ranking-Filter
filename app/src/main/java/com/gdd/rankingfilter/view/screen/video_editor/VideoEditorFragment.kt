package com.gdd.rankingfilter.view.screen.video_editor

import com.gdd.rankingfilter.R
import com.gdd.rankingfilter.base.BaseFragment
import com.gdd.rankingfilter.databinding.FragmentVideoEditorBinding

class VideoEditorFragment : BaseFragment<FragmentVideoEditorBinding>(FragmentVideoEditorBinding::inflate) {
    override fun initData() {
    }

    override fun setUpView() {
    }

    override fun setUpListener() = with(binding) {
        tvAddSound.setOnClickListener {
            navigateTo(R.id.action_videoEditorFragment_to_addSoundFragment)
        }
    }

}