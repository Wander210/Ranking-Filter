package com.gdd.rankingfilter.view.screen.add_sound

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.gdd.rankingfilter.R
import com.gdd.rankingfilter.data.model.Song
import com.gdd.rankingfilter.data.model.SoundSelectionData
import com.gdd.rankingfilter.databinding.ItemSoundBinding
import java.io.IOException

class SoundAdapter : RecyclerView.Adapter<SoundAdapter.SoundViewHolder>() {

    companion object {
        private const val TAG = "SoundAdapter"
    }

    private var songList: MutableList<Song> = mutableListOf()
    private var currentPosition = 0
    private var currentPlayer: MediaPlayer? = null
    private var progressHandler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null
    private var currentPlayingPosition = -1
    private var currentPlayingViewHolder: SoundViewHolder? = null
    private var isLoadingAudio = false
    private var isActuallyPlaying = false // Track if audio is actually playing (not paused)
    private var shouldAutoPlay = false // Flag to control when to auto-play

    @SuppressLint("NotifyDataSetChanged")
    fun setData(newSongs: MutableList<Song>) {
        songList = newSongs
        notifyDataSetChanged()
    }

    fun updatePosition(position: Int) {
        if (position == currentPosition) return

        val oldPosition = currentPosition
        currentPosition = position

        stopCurrentAudio()

        // Update selection state
        songList[oldPosition].isSelected = false
        songList[position].isSelected = true

        shouldAutoPlay = true

        notifyItemChanged(oldPosition)
        notifyItemChanged(position)
    }

    private fun post(action: () -> Unit) {
        Handler(Looper.getMainLooper()).post(action)
    }

    internal fun stopCurrentAudio() {
        currentPlayer?.let { player ->
            try {
                if (player.isPlaying) player.stop()
                player.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping MediaPlayer", e)
            }
            currentPlayer = null
        }

        progressRunnable?.let { progressHandler.removeCallbacks(it) }
        progressRunnable = null

        if (currentPlayingPosition != -1) {
            currentPlayingViewHolder?.updatePlayButton(false, false)
            currentPlayingPosition = -1
            currentPlayingViewHolder = null
        }

        isLoadingAudio = false
        isActuallyPlaying = false
        shouldAutoPlay = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SoundViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return SoundViewHolder(ItemSoundBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: SoundViewHolder, position: Int) {
        val item = songList[position]
        holder.bind(item, position)
    }

    override fun getItemCount(): Int = songList.size

    fun getSelectionData(): SoundSelectionData {
        return SoundSelectionData(
            selectedPosition = currentPosition - 1,
            clipStartTimeMs = currentPlayingViewHolder?.getCurrentTimeMs() ?: 0L,
            clipDurationMs = if (currentPosition < songList.size) {
                songList[currentPosition].duration ?: 0L
            } else 0L
        )
    }


    inner class SoundViewHolder(private val binding: ItemSoundBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var songDurationMs: Long = 0L
        private var currentTimeMs: Long = 0L
        private var currentItemPosition = -1

        fun getCurrentTimeMs(): Long = currentTimeMs

        fun bind(item: Song, position: Int) = with(binding) {
            currentItemPosition = position

            // Update UI based on selection state
            tvTitle.setTextColor(
                if (item.isSelected) "#FFFFFFFF".toColorInt() else "#FF000000".toColorInt()
            )
            lSongTitle.setBackgroundColor(
                if (item.isSelected) "#FF03A9F4".toColorInt() else "#FFFFFFFF".toColorInt()
            )

            if (item.public_id != "") {
                tvTitle.text = item.public_id
                imgMusicNote.setImageResource(R.drawable.ic_music_note_blue)
                btnPlay.visibility = android.view.View.VISIBLE

                // Show clip selection only for selected items
                clipSelectionView.visibility = if(item.isSelected) android.view.View.VISIBLE else android.view.View.GONE

                // Update play button icon and state
                updatePlayButton(currentPlayingPosition == position && isActuallyPlaying, !(isLoadingAudio && item.isSelected))

                // Play button logic - only enabled when not loading
                btnPlay.setOnClickListener {
                    if (isLoadingAudio) return@setOnClickListener

                    val pos = bindingAdapterPosition
                    // true nếu chính xác là item đang chơi (đang được chọn)
                    val isSelectedNow = (pos == currentPlayingPosition)

                    if (!isSelectedNow) {
                        // chưa chọn => chọn và auto-play
                        updatePosition(pos)
                        return@setOnClickListener
                    }

                    // đã chọn rồi, giờ toggle play/pause dựa trên biến thành viên
                    if (isActuallyPlaying) {
                        pauseAudio()
                    } else {
                        resumeAudio()
                    }
                }

                // Setup custom clip view
                if (item.isSelected) {
                    setupClipView(item)

                    // Auto-play ONLY if shouldAutoPlay flag is set (new selection)
                    if (shouldAutoPlay && position == currentPosition) {
                        shouldAutoPlay = false // Clear flag immediately
                        Log.d(TAG, "=== TRIGGERING AUTO-PLAY for item: ${item.public_id} ===")
                        post {
                            autoPlayAudio(item, position)
                        }
                    } else {
                        Log.d(TAG, "NOT auto-playing: shouldAutoPlay=$shouldAutoPlay, position=$position, currentPosition=$currentPosition")
                    }
                }

            } else {
                tvTitle.text = root.context.getString(R.string.no_sound)
                imgMusicNote.setImageResource(R.drawable.ic_prohibited)
                btnPlay.visibility = android.view.View.GONE
                clipSelectionView.visibility = android.view.View.GONE
            }

            // Item selection click
            root.setOnClickListener {
                if (!isLoadingAudio) {
                    updatePosition(bindingAdapterPosition)
                }
            }
        }

        fun autoPlayAudio(song: Song, position: Int) {
            if (song.public_id.isNotEmpty() && !isLoadingAudio) {
                playAudio(song, position)
            }
        }

        private fun setupClipView(song: Song) = with(binding) {
            // Use cached duration if available, otherwise use 0 (will be set when playing)
            songDurationMs = song.duration ?: 0L

            // Setup clip view with cached or default duration
            if (songDurationMs > 0) {
                clipSelectionView.setSongDuration(songDurationMs)
            }

            // Set current time
            clipSelectionView.setCurrentTime(currentTimeMs)

            // Handle seek events from custom view
            clipSelectionView.onSeekTo = { timeMs ->
                currentTimeMs = timeMs

                // If currently playing, seek the MediaPlayer
                if (currentPlayingPosition == currentItemPosition && !isLoadingAudio) {
                    currentPlayer?.let { player ->
                        try {
                            player.seekTo(timeMs.toInt())
                        } catch (e: Exception) {
                            Log.e(TAG, "Error seeking MediaPlayer", e)
                        }
                    }
                }
            }
        }

        private fun playAudio(song: Song, position: Int) {
            Log.d(TAG, "Starting playback for: ${song.public_id}")

            // Stop current audio
            stopCurrentAudio()

            // Set loading state
            isLoadingAudio = true
            currentPlayingPosition = position
            currentPlayingViewHolder = this

            // Update UI to show loading state (btnPlay disabled, still showing play icon)
            updatePlayButton(false, false)

            try {
                currentPlayer = MediaPlayer().apply {
                    setDataSource(song.secure_url)

                    setOnPreparedListener { mp ->
                        Log.d(TAG, "MediaPlayer prepared for: ${song.public_id}")

                        // Clear loading state
                        isLoadingAudio = false

                        songDurationMs = mp.duration.toLong()

                        // Cache duration in song object for future use
                        song.duration = songDurationMs

                        // Update clip view with actual duration
                        binding.clipSelectionView.setSongDuration(songDurationMs)

                        // Start from current time (or beginning if first play)
                        mp.seekTo(currentTimeMs.toInt())
                        mp.start()

                        // Set playing state
                        isActuallyPlaying = true

                        // Update UI to show playing state (btnPlay enabled, showing pause icon)
                        updatePlayButton(true, true)

                        // Start progress monitoring
                        startProgressMonitoring(mp)
                    }

                    setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                        isLoadingAudio = false
                        isActuallyPlaying = false
                        handlePlaybackEnd()
                        true
                    }

                    setOnCompletionListener {
                        Log.d(TAG, "Playback completed")
                        isActuallyPlaying = false
                        handlePlaybackEnd()
                    }

                    prepareAsync()
                }

            } catch (e: IOException) {
                Log.e(TAG, "Error preparing MediaPlayer", e)
                isLoadingAudio = false
                handlePlaybackEnd()
            }
        }

        private fun startProgressMonitoring(player: MediaPlayer) {
            progressRunnable = object : Runnable {
                override fun run() {
                    try {
                        if (player.isPlaying) {
                            currentTimeMs = player.currentPosition.toLong()

                            // Update custom clip view with current time
                            binding.clipSelectionView.setCurrentTime(currentTimeMs)

                            // Continue monitoring
                            progressHandler.postDelayed(this, 100) // Update every 100ms
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in progress monitoring", e)
                    }
                }
            }
            progressRunnable?.let { progressHandler.post(it) }
        }

        private fun resumeAudio() {
            Log.d(TAG, "Resuming audio")
            try {
                currentPlayer?.let { player ->
                    player.start()
                    isActuallyPlaying = true
                    updatePlayButton(true, true)

                    // Restart progress monitoring
                    startProgressMonitoring(player)

                    Log.d(TAG, "Audio resumed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error resuming MediaPlayer", e)
                isActuallyPlaying = false
                handlePlaybackEnd()
            }
        }

        private fun pauseAudio() {
            Log.d(TAG, "=== Pausing audio ===")
            try {
                currentPlayer?.let { player ->
                    currentTimeMs = player.currentPosition.toLong()
                    player.pause()
                    isActuallyPlaying = false
                    Log.d(TAG, "MediaPlayer paused, isActuallyPlaying = false")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error pausing MediaPlayer", e)
            }

            // Stop progress monitoring
            progressRunnable?.let { progressHandler.removeCallbacks(it) }
            progressRunnable = null

            // Update button to show paused state (DON'T reset currentPlayingPosition)
            updatePlayButton(false, true)

            Log.d(TAG, "Audio paused successfully, currentPlayingPosition = $currentPlayingPosition, shouldAutoPlay = $shouldAutoPlay")
        }

        private fun handlePlaybackEnd() {
            currentPlayingPosition = -1
            currentPlayingViewHolder = null
            isLoadingAudio = false
            isActuallyPlaying = false
            updatePlayButton(false, true)
        }

        fun updatePlayButton(playing: Boolean, enabled: Boolean) {
            binding.btnPlay.setImageResource(
                if (playing) R.drawable.ic_pause else R.drawable.ic_play
            )
            binding.btnPlay.isEnabled = enabled
            binding.btnPlay.alpha = if (enabled) 1.0f else 0.5f // Visual feedback for disabled state
        }
    }
}