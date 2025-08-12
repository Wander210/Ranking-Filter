package com.gdd.rankingfilter.view.screen.video_player

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.gdd.rankingfilter.R
import com.gdd.rankingfilter.base.BaseFragment
import com.gdd.rankingfilter.data.model.Video
import com.gdd.rankingfilter.databinding.FragmentSingleVideoBinding
import com.gdd.rankingfilter.utils.VideoPlayerManager

/**
 * A fragment that displays and controls playback of a single video.
 */
class SingleVideoFragment : BaseFragment<FragmentSingleVideoBinding>(FragmentSingleVideoBinding::inflate) {
    private var exoPlayer: ExoPlayer? = null
    private lateinit var video: Video
    private val playerManager: VideoPlayerManager by lazy {
        VideoPlayerManager.getInstance(requireContext())
    }
    private var isPlayerReady = false
    private var shouldAutoPlay = false
    private var isSetupComplete = false

    companion object {
        private const val ARG_VIDEO = "video"
        private const val TAG = "SingleVideoFragment"

        fun newInstance(video: Video) = SingleVideoFragment().apply {
            arguments = Bundle().apply { putParcelable(ARG_VIDEO, video) }
        }
    }

    override fun initData() {
        video = requireArguments().getParcelable(ARG_VIDEO)
            ?: throw IllegalArgumentException("Video argument is required")
    }

    override fun setUpView(): Unit = with(binding) {
        playerView.player = null
        // Display video information
        tvTitle.text = video.public_id
        tvAuthor.text = video.context?.custom?.author.orEmpty()
        tvLikesCount.text = formatLikes(video.context?.custom?.likes)
        tvTags.text = video.tags?.joinToString(" ") { "#$it" }.orEmpty()
        progressBar.visibility = View.VISIBLE
    }


    override fun setUpListener() {
        binding.playerView.setOnClickListener { togglePlayPause() }
    }

    /**
     * Format likes count into human-readable string.
     */
    private fun formatLikes(likes: String?): String {
        if (likes.isNullOrEmpty()) return getString(R.string.no_likes)
        return when (likes.length) {
            in 1..3 -> likes
            in 4..6 -> "${likes.dropLast(3)}.${likes.takeLast(3).take(1)}K"
            in 7..8 -> "${likes.dropLast(6)}.${likes.takeLast(6).take(1)}M"
            else -> "${likes.dropLast(9)}.${likes.takeLast(9).take(1)}B"
        }
    }

    /**
     * Toggle between play and pause states on user tap.
     */
    private fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (isPlayerReady) {
                player.playWhenReady = !player.playWhenReady
            } else {
                shouldAutoPlay = true
                setupPlayer()
            }
        }
    }

    /**
     * Initialize ExoPlayer, attach to the view, and prepare media.
     * Only called once when needed.
     */
    fun setupPlayer() {
        // Guard to prevent repeated or premature setup
        if (isSetupComplete || !isAdded || !::binding.isInitialized) return

        try {
            if (exoPlayer == null) {
                // Obtain a shared ExoPlayer instance
                exoPlayer = playerManager.getPlayer().apply {
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            when (state) {
                                // The player has loaded enough data to start playing without interruption.
                                Player.STATE_READY -> onPlayerReady()
                                // The player is buffering—waiting for more data to arrive.
                                Player.STATE_BUFFERING -> binding.progressBar.visibility = View.VISIBLE
                                // Playback has finished and reached the end of the media.
                                Player.STATE_ENDED -> loopVideo()
                                // The player is idle (no media loaded or reset to idle).
                                Player.STATE_IDLE -> binding.progressBar.visibility = View.VISIBLE
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            Log.e(TAG, "Playback error: ${error.message}")
                            isPlayerReady = false
                            shouldAutoPlay = false
                        }
                    })
                    // Prepare media but do not start playback yet
                    setMediaItem(MediaItem.fromUri(video.secure_url))
                    prepare()
                    playWhenReady = false
                }
                binding.playerView.player = exoPlayer
                isSetupComplete = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up player: ${e.message}")
            isSetupComplete = false
        }
    }

    private fun onPlayerReady() {
        isPlayerReady = true
        binding.progressBar.visibility = View.GONE
        if (shouldAutoPlay) {
            exoPlayer?.playWhenReady = true
            shouldAutoPlay = false
        }
    }

    private fun loopVideo() {
        exoPlayer?.let {
            it.seekTo(0)
            if (isPlayerReady) it.playWhenReady = true
        }
    }

    /**
     * Request playback: starts immediately if ready or marks for auto-play when ready.
     */
    fun playVideo() {
        if (!isAdded || !isSetupComplete) return
        exoPlayer?.let {
            if (isPlayerReady) it.playWhenReady = true
            else shouldAutoPlay = true
        } ?: run {
            shouldAutoPlay = true
            setupPlayer()
        }
    }

    fun pauseVideo() {
        exoPlayer?.playWhenReady = false
        shouldAutoPlay = false
    }

    fun stopAndRelease() {
        exoPlayer?.let { player ->
            binding.playerView.player = null
            try {
                playerManager.returnPlayer(player)
            } catch (e: Exception) {
                player.release()
                Log.e(TAG, "Error returning player: ${e.message}")
            }
            exoPlayer = null
            isPlayerReady = false
            shouldAutoPlay = false
            isSetupComplete = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.progressBar.visibility = View.GONE
        stopAndRelease()
    }
}
