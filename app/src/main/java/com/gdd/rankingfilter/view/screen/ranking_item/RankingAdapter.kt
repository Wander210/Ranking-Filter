package com.gdd.rankingfilter.view.screen.ranking_item

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.gdd.rankingfilter.data.model.RankingItem
import com.gdd.rankingfilter.databinding.ItemRankingGridBinding
import com.gdd.rankingfilter.databinding.loadImage

class RankingAdapter() : RecyclerView.Adapter<RankingAdapter.RankingViewHolder>() {
    private val items: MutableList<RankingItem> = mutableListOf()
    private var selectedPosition: Int = RecyclerView.NO_POSITION
    var onItemSelected: ((Int) -> Unit)? = null

    @SuppressLint("NotifyDataSetChanged")
    fun updateItems(newItems: List<RankingItem>) {
        items.clear()
        items.addAll(newItems)
        selectedPosition = RecyclerView.NO_POSITION // Reset selection
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RankingViewHolder {
        val binding =
            ItemRankingGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RankingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RankingViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount() = items.size

    // MAIN FIX: Move click handling to Adapter level, not ViewHolder level
    private fun onItemClick(clickedPosition: Int) {
        if (clickedPosition == RecyclerView.NO_POSITION) return

        val previousPosition = selectedPosition
        // Toggle selection: deselect if same item clicked, otherwise select new item
        selectedPosition = if (clickedPosition == selectedPosition) RecyclerView.NO_POSITION
                            else clickedPosition
        // Notify changes
        if (previousPosition != RecyclerView.NO_POSITION) notifyItemChanged(previousPosition)

        notifyItemChanged(clickedPosition)
        onItemSelected?.invoke(selectedPosition)
    }

    fun getSelectedPosition(): Int = selectedPosition

    inner class RankingViewHolder(private val binding: ItemRankingGridBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RankingItem, pos: Int) = with(binding) {
            // Set data
            tvTitle.text = item.id
            loadImage(imgCover, item.coverUrl)
            updateTextColor(pos)
            tvTitle.setOnClickListener { onItemClick(bindingAdapterPosition) }
            imgCover.setOnClickListener { onItemClick(bindingAdapterPosition) }
        }

        private fun updateTextColor(pos: Int) = with(binding) {
            val isSelected = pos == selectedPosition
            val color = if (isSelected) ContextCompat.getColor(root.context, android.R.color.holo_orange_light)
                        else ContextCompat.getColor(root.context, android.R.color.black)
            tvTitle.setTextColor(color)
        }
    }
}