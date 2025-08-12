package com.gdd.rankingfilter.view.screen

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.gdd.rankingfilter.R
import com.gdd.rankingfilter.data.repository.CloudinaryRepository
import com.gdd.rankingfilter.utils.VideoPlayerManager
import com.gdd.rankingfilter.viewmodel.MainViewModel
import com.gdd.rankingfilter.viewmodel.MainViewModelFactory

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(CloudinaryRepository(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        enableEdgeToEdge()
    }

    override fun onDestroy() {
        super.onDestroy()
        VideoPlayerManager.getInstance(this).releaseAll()
    }
}