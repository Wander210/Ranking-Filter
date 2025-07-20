package com.gdd.rankingfilter.view.screen.home

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.gdd.rankingfilter.view.screen.home.anime.AnimeFragment
import com.gdd.rankingfilter.view.screen.home.cartoon.CartoonFragment
import com.gdd.rankingfilter.view.screen.home.food.FoodFragment
import com.gdd.rankingfilter.view.screen.home.football.FootballFragment
import com.gdd.rankingfilter.view.screen.home.idol.IdolFragment
import com.gdd.rankingfilter.view.screen.home.makeup.MakeupFragment
import com.gdd.rankingfilter.view.screen.home.trending.TrendingFragment

class FragmentPageAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {
    private val fragments = listOf(
        TrendingFragment(),
        FootballFragment(),
        FoodFragment(),
        AnimeFragment(),
        IdolFragment(),
        MakeupFragment(),
        CartoonFragment()
    )

    override fun getItemCount() = fragments.size
    override fun createFragment(position: Int) = fragments[position]
}