package com.gdd.rankingfilter.view.custom.circular_spinner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gdd.rankingfilter.databinding.ItemRankingBinding
import com.gdd.rankingfilter.databinding.loadCover

class CircularSpinnerAdapter(
    private val originalList: List<String>,
    private val itemSpacing: Float,
    private val onItemClick: (view: View, position: Int) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<CircularSpinnerAdapter.RankingViewHolder>() {

    companion object {
        private const val LARGE_NUMBER = 10000
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RankingViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemRankingBinding.inflate(inflater, parent, false)
        return RankingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RankingViewHolder, position: Int) {
        if (originalList.isNotEmpty()) {
            val actualPosition = position % originalList.size
            val coverUrl = originalList[actualPosition]
            holder.bind(coverUrl, position)
        }
    }

    override fun getItemCount(): Int = if (originalList.isEmpty()) 0 else LARGE_NUMBER

    inner class RankingViewHolder(private val binding: ItemRankingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(coverUrl: String, position: Int) {
            val lp = binding.imgCover.layoutParams
            if(lp is ViewGroup.MarginLayoutParams) lp.setMargins(itemSpacing.toInt(), 0, itemSpacing.toInt(), 0)
            binding.imgCover.layoutParams = lp

            loadCover(binding.imgCover, coverUrl)
            binding.root.setOnClickListener {
                onItemClick(binding.root, position)
            }
        }
    }
}