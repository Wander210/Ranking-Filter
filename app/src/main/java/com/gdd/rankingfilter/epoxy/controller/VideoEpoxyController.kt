package com.gdd.rankingfilter.epoxy.controller

import android.content.Context
import android.view.View
import com.airbnb.epoxy.EpoxyController
import com.gdd.rankingfilter.R
import com.gdd.rankingfilter.data.model.Video
import com.gdd.rankingfilter.itemVideo

class VideoEpoxyController(
    private val context: Context,
    private val onVideoClick: (Video) -> Unit
) : EpoxyController() {

    private var videos: List<Video> = emptyList()

    fun setData(newVideos: List<Video>) {
        videos = newVideos.sortedByDescending {
            it.context?.custom?.likes?.toIntOrNull() ?: 0
        }
        requestModelBuild()
    }

    override fun buildModels() {
        videos.forEach { video ->
            val thumbnailUrl = video.secure_url.replace(".mp4", ".jpg")
            val likesText = likeCounts(video.context?.custom?.likes.toString(), context)

            itemVideo {
                id(video.public_id)
                title(video.public_id)
                thumbnailUrl(thumbnailUrl)
                author(video.context?.custom?.author)
                likesCount(likesText)
                isSelected(false)
                onClick(View.OnClickListener {
                    this@VideoEpoxyController.onVideoClick(video)
                })
            }
        }
    }

    private fun likeCounts(str: String?, context: Context): String {
        val heart = "\u2764\uFE0F" // ❤️
        if (str.isNullOrEmpty()) return context.getString(R.string.no_likes)
        return when (str.length) {
            in 1..3 -> "$heart $str"
            in 4..6 -> "$heart ${str[0]},${str[1]} K"
            in 7..8 -> "$heart ${str[0]},${str[1]} M"
            else -> "$heart ${str[0]},${str[1]} B"
        }
    }
}