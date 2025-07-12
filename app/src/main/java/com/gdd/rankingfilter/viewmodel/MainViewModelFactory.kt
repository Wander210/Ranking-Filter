package com.gdd.rankingfilter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gdd.rankingfilter.data.repository.CloudinaryRepository

class MainViewModelFactory(
    private val repository: CloudinaryRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            val constructor = modelClass.getConstructor(CloudinaryRepository::class.java)
            return constructor.newInstance(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
