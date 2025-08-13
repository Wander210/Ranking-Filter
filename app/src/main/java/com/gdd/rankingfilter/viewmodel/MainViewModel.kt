package com.gdd.rankingfilter.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.gdd.rankingfilter.data.model.RankingItem
import com.gdd.rankingfilter.data.model.Song
import com.gdd.rankingfilter.data.model.Video
import com.gdd.rankingfilter.data.repository.CloudinaryRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: CloudinaryRepository
) : ViewModel() {

    // Sửa: Xóa dấu * trước tên biến
    private val _allVideos = MutableLiveData<List<Video>>(emptyList())
    val allVideos: LiveData<List<Video>> = _allVideos

    private val _allSongs = MutableLiveData<List<Song>>(emptyList())
    val allSongs: LiveData<List<Song>> = _allSongs

    private val _allRankingItems = MutableLiveData<List<RankingItem>>(emptyList())
    val allRankingItems: LiveData<List<RankingItem>> = _allRankingItems

    // Loading State
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Notify when all data is loaded
    private val _isDataLoaded = MutableLiveData(false)
    val isDataLoaded: LiveData<Boolean> = _isDataLoaded

    /**
     * Download all initial data
     * This method is called once when the app starts
     */
    fun loadAllData() {
        // Avoid reloading if already loading or data is loaded
        if (_isLoading.value == true || _isDataLoaded.value == true) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val videosDeferred = async { repository.getVideos() }
                val songsDeferred = async { repository.getSongs() }
                val rankingDeferred = async { repository.getRankingItems() }

                _allVideos.value = videosDeferred.await()
                _allSongs.value = songsDeferred.await()
                _allRankingItems.value = rankingDeferred.await()

                _isDataLoaded.value = true
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading initial data", e)
                _allVideos.value = _allVideos.value ?: emptyList()
                _allSongs.value = _allSongs.value ?: emptyList()
                _allRankingItems.value = _allRankingItems.value ?: emptyList()
                _isDataLoaded.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * @param tag  if null, returns all videos;
     *             otherwise only videos whose tags contain this value.
     * Trả LiveData non-null -> observer không cần null-check
     */
    fun videosByTag(tag: String?): LiveData<List<Video>> =
        allVideos.map { videos ->
            videos.let { list ->
                if (tag == null) list
                else list.filter { it.tags?.contains(tag) == true }
            }
                // sort by like count descending
                .sortedByDescending { it.context?.custom?.likes?.toIntOrNull() ?: 0 }
        }
}