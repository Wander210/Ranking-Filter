package com.gdd.rankingfilter.view.screen.home

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.gdd.rankingfilter.R
import com.gdd.rankingfilter.data.model.Video
import com.gdd.rankingfilter.databinding.ItemVideoBinding
import com.gdd.rankingfilter.extention.dpToPx

class VideoAdapter(
    private val videos: MutableList<Video>,
    private val onVideoClick: (Video) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    /** ViewHolder giữ trực tiếp class binding thay vì findViewById */
    inner class VideoViewHolder(val binding: ItemVideoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoBinding.inflate(
            LayoutInflater.from(parent.context), parent, /* attachToParent = */ false
        )
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]

        // Thumbnail: Cloudinary auto‑generate *.jpg khi thay đuôi .mp4
        val thumbnailUrl = video.secure_url.replace(".mp4", ".jpg")

        with(holder.binding) {
            /* Load thumbnail */
            Glide.with(root.context)
                .load(thumbnailUrl)
                .transform(RoundedCorners( 20f.dpToPx(holder.itemView.context).toInt()))
                .placeholder(Color.LTGRAY.toDrawable())
                .into(imgThumbnail)

            tvTitle.text = video.public_id
            tvAuthor.text = video.context?.custom?.author
            tvLikesCount.text = likeCounts(video.context?.custom?.likes.toString(), root.context)
            root.setOnClickListener { onVideoClick(video) }
        }
        Log.d("VideoAdapter", "bind pos=$position id=${video.public_id}")
    }

    override fun getItemCount() = videos.size.also {
        Log.d("VideoAdapter", "getItemCount = $it")
    }

    /** Cập nhật toàn bộ danh sách video */
    fun updateVideos(newVideos: List<Video>) {
        videos.clear()
        videos.addAll(newVideos)
        notifyDataSetChanged()
    }

    fun likeCounts(str: String, context : Context): String {
        val heart = "\u2764\uFE0F" // ❤️

        if (str.isEmpty()) return context.getString(R.string.no_likes)

        return when (str.length) {
            in 1..3 -> "$heart $str"
            in 4..6 -> "$heart ${str[0]},${str[1]} K"
            in 7..8 -> "$heart ${str[0]},${str[1]} M"
            else -> "$heart ${str[0]},${str[1]} B"
        }
    }
}
