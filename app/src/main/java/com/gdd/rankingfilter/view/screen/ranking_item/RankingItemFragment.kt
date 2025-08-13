package com.gdd.rankingfilter.view.screen.ranking_item

import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gdd.rankingfilter.R
import com.gdd.rankingfilter.base.BaseFragment
import com.gdd.rankingfilter.data.repository.CloudinaryRepository
import com.gdd.rankingfilter.databinding.FragmentRankingItemBinding
import com.gdd.rankingfilter.viewmodel.MainViewModel
import com.gdd.rankingfilter.viewmodel.MainViewModelFactory

class RankingItemFragment :
    BaseFragment<FragmentRankingItemBinding>(FragmentRankingItemBinding::inflate) {

    private var adapter = RankingAdapter()
    private var selectedPosition: Int = RecyclerView.NO_POSITION


    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(CloudinaryRepository(requireContext()))
    }

    override fun initData() {
        viewModel.allRankingItems.observe(viewLifecycleOwner) { list ->
            adapter.updateItems(list)
        }

    }

    override fun setUpView() {
        binding.rvRanking.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = this@RankingItemFragment.adapter
        }
        adapter.onItemSelected = { pos ->
            selectedPosition = if (pos != RecyclerView.NO_POSITION ) pos
                                else RecyclerView.NO_POSITION
        }

    }

    override fun setUpListener() = with(binding) {
        btnBack.setOnClickListener { navigateBack() }

        binding.btnCheck.setOnClickListener {
            val pos = adapter.getSelectedPosition()
            if (pos != RecyclerView.NO_POSITION) {
                // return selected position to previous fragment
                navigateBackWithResult("selectedPosition", pos)
            } else {
                Toast.makeText(requireContext(), getString(R.string.please_select_an_item), Toast.LENGTH_SHORT).show()
            }
        }
    }
}