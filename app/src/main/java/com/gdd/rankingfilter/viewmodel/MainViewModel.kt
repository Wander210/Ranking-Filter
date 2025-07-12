package com.gdd.rankingfilter.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.gdd.rankingfilter.data.model.Video
import com.gdd.rankingfilter.data.repository.CloudinaryRepository
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: CloudinaryRepository
) : ViewModel() {

    private val _allVideos = MutableLiveData<List<Video>>()
    val allVideos: LiveData<List<Video>> = _allVideos

    init {
        loadVideos()
    }

    fun loadVideos() {
        viewModelScope.launch {
            try {
                _allVideos.value = repository.getVideos()
            } catch (e: Exception) {
                Log.e("LoadVideos", "Error loading videos", e)
            }
        }
    }

    /**
     * @param tag  if null, returns all videos;
     *             otherwise only videos whose tags contain this value.
     */
    fun videosByTag(tag: String?): LiveData<List<Video>> =
        allVideos.map { videos ->
            videos
                // filter by tag if provided
                .let { list ->
                    if (tag == null) list
                    else list.filter { it.tags?.contains(tag) == true }
                }
                // sort by like count descending
                .sortedByDescending { it.context?.custom?.likes?.toIntOrNull() ?: 0 }
        }
}

