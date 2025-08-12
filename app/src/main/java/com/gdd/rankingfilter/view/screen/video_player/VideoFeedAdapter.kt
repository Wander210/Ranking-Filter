package com.gdd.rankingfilter.view.screen.video_player

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.gdd.rankingfilter.data.model.Video
import com.gdd.rankingfilter.utils.VideoPlayerManager

class VideoFeedAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    private var videos: List<Video> = emptyList()
    private val fragmentMap = mutableMapOf<Int, SingleVideoFragment>()
    private val playerManager by lazy { VideoPlayerManager.getInstance(fragment.requireContext()) }

    @SuppressLint("NotifyDataSetChanged")
    fun setList(newVideoList: List<Video>) {
        videos = newVideoList
        fragmentMap.clear()
        notifyDataSetChanged()
        // Preload URLs into cache
        newVideoList.take(5).forEach { video ->
            playerManager.preloadVideo(video.secure_url)
        }
    }

    override fun getItemCount(): Int = videos.size

    override fun createFragment(position: Int): Fragment {
        val fragment = SingleVideoFragment.newInstance(videos[position])
        fragmentMap[position] = fragment
        return fragment
    }

    fun getFragmentAt(position: Int): SingleVideoFragment? {
        val fragment = fragmentMap[position]
        return fragment
    }
}