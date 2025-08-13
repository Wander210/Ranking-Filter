package com.gdd.rankingfilter.view.screen.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.gdd.rankingfilter.databinding.ItemVideoBinding
import com.gdd.rankingfilter.databinding.ItemVideoHeaderBinding
import com.gdd.rankingfilter.extention.dpToPx
import com.gdd.rankingfilter.view.screen.library.LibraryFragment.VideoItem
import java.io.File

class VideoAdapter(
    private val items: List<VideoItem>,
    private val onVideoClick: (File) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_VIDEO = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is VideoItem.Header -> TYPE_HEADER
            is VideoItem.Video -> TYPE_VIDEO
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemVideoHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                HeaderViewHolder(binding)
            }
            TYPE_VIDEO -> {
                val binding = ItemVideoBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                VideoViewHolder(binding, onVideoClick)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                val header = items[position] as VideoItem.Header
                holder.bind(header.monthYear)
            }
            is VideoViewHolder -> {
                val video = items[position] as VideoItem.Video
                holder.bind(video.file)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    private class HeaderViewHolder(
        private val binding: ItemVideoHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(monthYear: String) {
            binding.tvMonthYear.text = monthYear
        }
    }

    private class VideoViewHolder(
        private val binding: ItemVideoBinding,
        private val onVideoClick: (File) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(file: File) {
            binding.title = file.nameWithoutExtension
            binding.onClick = View.OnClickListener { onVideoClick(file) }

            val context = binding.imgThumbnail.context

            Glide.with(context)
                .load(file)
                .apply(
                    RequestOptions()
                        .placeholder(android.R.color.darker_gray)
                        .error(android.R.color.darker_gray)
                        .priority(Priority.HIGH)
                        .override(200, 200)
                )
                .transform(RoundedCorners(20f.dpToPx(context).toInt()))
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(binding.imgThumbnail)

            binding.executePendingBindings()
        }
    }
}
