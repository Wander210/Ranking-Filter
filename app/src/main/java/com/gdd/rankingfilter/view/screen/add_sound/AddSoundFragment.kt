package com.gdd.rankingfilter.view.screen.add_sound

import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gdd.rankingfilter.R
import com.gdd.rankingfilter.base.BaseFragment
import com.gdd.rankingfilter.data.model.Song
import com.gdd.rankingfilter.data.repository.CloudinaryRepository
import com.gdd.rankingfilter.databinding.FragmentAddSoundBinding
import com.gdd.rankingfilter.utils.VideoPlayerManager
import com.gdd.rankingfilter.viewmodel.MainViewModel
import com.gdd.rankingfilter.viewmodel.MainViewModelFactory

class AddSoundFragment : BaseFragment<FragmentAddSoundBinding>(FragmentAddSoundBinding::inflate) {

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(CloudinaryRepository(requireContext()))
    }
    private var songList: List<Song> = emptyList()
    private lateinit var soundAdapter : SoundAdapter
    private lateinit var playerManager: VideoPlayerManager

    override fun initData() {
        playerManager = VideoPlayerManager.getInstance(requireContext())
    }

    override fun setUpView() = with(binding){
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        soundAdapter = SoundAdapter()
        recyclerView.adapter = soundAdapter

        viewModel.allSongs.observe(viewLifecycleOwner) { songs ->
            val sortedSongs = songs.sortedByDescending { it.public_id }
            val headerSong = Song("", "", 0L, true)
            songList = listOf(headerSong) + sortedSongs

            soundAdapter.setData(songList as MutableList<Song>)
        }
    }

    override fun setUpListener() {
        binding.btnBack.setOnClickListener { navigateBack() }

        binding.btnDone.setOnClickListener {
            val selectionData = soundAdapter.getSelectionData()
            if (selectionData.selectedPosition != RecyclerView.NO_POSITION)
                navigateBackWithResult("soundSelectionData", selectionData)
            else
                Toast.makeText(requireContext(), getString(R.string.please_select_an_item), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        soundAdapter.stopCurrentAudio()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        soundAdapter.stopCurrentAudio()
    }
}