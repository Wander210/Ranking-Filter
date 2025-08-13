package com.gdd.rankingfilter.view.screen.language

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.gdd.rankingfilter.R
import com.gdd.rankingfilter.databinding.ItemLanguageBinding
import com.gdd.rankingfilter.data.model.Language

class LanguageItemAdapter(
    private val languageList: List<Language>,
    private val clickListener: (Language) -> Unit
) : RecyclerView.Adapter<LanguageItemViewHolder>() {

    private var selectedPosition: Int = -1

    fun updateSelectedPosition(selectedLanguage: String) {
        val previousPosition = selectedPosition
        for(i in languageList.indices) {
            if(languageList[i].locale == selectedLanguage) {
                selectedPosition = i
                break
            }
        }

        if (previousPosition != -1) {
            notifyItemChanged(previousPosition)
        }
        if (selectedPosition != -1) {
            notifyItemChanged(selectedPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageItemViewHolder {
        val binding = ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LanguageItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageItemViewHolder, position: Int) {
        val language = languageList[position]
        holder.bind(language, position == selectedPosition, clickListener)
    }

    override fun getItemCount(): Int = languageList.size
}

class LanguageItemViewHolder(private val binding: ItemLanguageBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(language: Language, isSelected: Boolean, onLanguageSelected: (Language) -> Unit) {
        binding.apply {
            imgLanguageFlag.setImageResource(language.icon)
            tvLanguageName.text = language.name

            if (isSelected) {
                // Selected state - blue background, white text
                root.setBackgroundResource(R.drawable.bg_selected_language_item)
                tvLanguageName.setTextColor(Color.WHITE)
                rbLanguageSelect.isChecked = true
            } else {
                // Normal state - white background, dark text
                root.setBackgroundResource(R.drawable.bg_language_item)
                tvLanguageName.setTextColor(ContextCompat.getColor(root.context, R.color.black))
                rbLanguageSelect.isChecked = false
            }

            root.setOnClickListener {
                onLanguageSelected(language)
            }
        }
    }
}