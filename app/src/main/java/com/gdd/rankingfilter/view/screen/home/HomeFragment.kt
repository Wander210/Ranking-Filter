package com.gdd.rankingfilter.view.screen.home

import android.graphics.drawable.GradientDrawable
import android.view.ViewGroup
import com.gdd.rankingfilter.R
import com.gdd.rankingfilter.base.BaseFragment
import com.gdd.rankingfilter.data.model.TabInfo
import com.gdd.rankingfilter.databinding.FragmentHomeBinding
import com.gdd.rankingfilter.databinding.TabCustomItemBinding
import com.gdd.rankingfilter.extention.dpToPx
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private val tabs by lazy { ArrayList<TabInfo>() }

    override fun initData() {
        tabs.addAll(
            listOf(
                TabInfo(R.string.trending,  R.drawable.ic_star,     0xFFF44336.toInt()),
                TabInfo(R.string.football,  R.drawable.ic_football, 0xFFFF9800.toInt()),
                TabInfo(R.string.food,      R.drawable.ic_food,     0xFFFFEB3B.toInt()),
                TabInfo(R.string.anime,     R.drawable.ic_anime,    0xFF4CAF50.toInt()),
                TabInfo(R.string.idol,      R.drawable.ic_idol,     0xFF2196F3.toInt()),
                TabInfo(R.string.makeup,    R.drawable.ic_makeup,   0xFF3F51B5.toInt()),
                TabInfo(R.string.cartoon,   R.drawable.ic_cartoon,  0xFF9C27B0.toInt())
            )
        )
    }

    override fun setUpView(): Unit = with(binding) {
        viewPager2.adapter = FragmentPageAdapter(childFragmentManager, lifecycle)

        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            val tapInfo = tabs[position]
            val tabView = TabCustomItemBinding.inflate(layoutInflater).apply {
                tabIcon.setImageResource(tapInfo.icon)
                tabTitle.setText(tapInfo.title)
                val bg = GradientDrawable().apply {
                    cornerRadius = 20f.dpToPx(requireContext())
                    setColor(DEFAULT_BG_COLOR)
                }
                root.background = bg
            }
            tab.customView = tabView.root
        }.attach()
        tabLayout.post { addTabMargins() }
    }

    override fun setUpListener() = with(binding) {
        // Handle tab selection manually to set background
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                updateTabBackground(tab, selected = true)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                updateTabBackground(tab, selected = false)
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Set initial selected tab background
        tabLayout.getTabAt(tabLayout.selectedTabPosition)?.let {
            updateTabBackground(it, selected = true)
        }

        btnSetting.setOnClickListener {
            navigateTo(R.id.action_homeFragment_to_settingFragment)
        }
    }

    private fun addTabMargins() = with(binding) {
        val strip = tabLayout.getChildAt(0) as ViewGroup
        for (i in 0 until strip.childCount) {
            val tv = strip.getChildAt(i)
            tv.setPadding(0, 0, 0, 0) // Reset padding
            val layoutParams = tv.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.marginStart = (10 * tabLayout.context.resources.displayMetrics.density).toInt()
            tv.layoutParams = layoutParams
        }
    }

    private fun updateTabBackground(tab: TabLayout.Tab?, selected: Boolean) {
        val pos = tab?.position ?: return
        val bgDrawable = GradientDrawable().apply {
            cornerRadius = 20f.dpToPx(requireContext())
            setColor(if (selected) tabs[pos].selectedColor else DEFAULT_BG_COLOR)
        }
        tab.customView?.background = bgDrawable
    }

    companion object {
        private const val DEFAULT_BG_COLOR = 0xFFF2F3F5.toInt()
    }
}