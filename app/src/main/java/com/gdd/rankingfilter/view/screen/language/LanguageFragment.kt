package com.gdd.rankingfilter.view.screen.language

import androidx.recyclerview.widget.LinearLayoutManager
import com.gdd.rankingfilter.R
import com.gdd.rankingfilter.base.BaseFragment
import com.gdd.rankingfilter.databinding.FragmentLanguageBinding
import com.gdd.rankingfilter.data.model.Language
import com.gdd.rankingfilter.preference.MyPreferences
import com.gdd.rankingfilter.utils.LanguageUtil

class LanguageFragment : BaseFragment<FragmentLanguageBinding>(FragmentLanguageBinding::inflate) {

    private lateinit var languageAdapter: LanguageItemAdapter
    private val languageList by lazy { ArrayList<Language>() }
    private var curLanguage: String? = null

    override fun initData() {
        curLanguage = MyPreferences.read(MyPreferences.PREF_LANGUAGE, "en")
        var temp: Language? = null

        languageList.addAll(
            listOf(
                Language(0, "English", R.drawable.ic_flag_english, "en"),
                Language(1, "Español", R.drawable.ic_flag_spanish, "es"),
                Language(2, "Français", R.drawable.ic_flag_french, "fr"),
                Language(3, "Vietnam", R.drawable.ic_flag_vietnamese, "vi"),
                Language(4, "Portuguese", R.drawable.ic_flag_portuguese, "pt"),
                Language(5, "Deutsch", R.drawable.ic_flag_german, "de")
            )
        )

        for (i in languageList.indices) {
            if (languageList[i].locale == curLanguage) {
                temp = languageList[i]
                languageList.removeAt(i)
                break
            }
        }
        temp?.let {
            languageList.add(0, it)
        }
    }

    override fun setUpView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        languageAdapter = LanguageItemAdapter(languageList) { selectedLanguage ->
            curLanguage = selectedLanguage.locale
            curLanguage?.let {
                languageAdapter.updateSelectedPosition(it)
            }
        }

        curLanguage?.let {
            languageAdapter.updateSelectedPosition(it)
        }

        binding.recyclerView.adapter = languageAdapter
    }

    override fun setUpListener() {
        binding.btnDone.setOnClickListener {
            curLanguage?.let {
                MyPreferences.write(MyPreferences.PREF_LANGUAGE, it)
                LanguageUtil.setLanguage(requireContext())
                parentFragmentManager.popBackStack()
            }
        }

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}