package com.gdd.rankingfilter.view.custom

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.gdd.rankingfilter.R
import com.gdd.rankingfilter.databinding.SampleClipSelectionViewBinding
import kotlin.math.max
import kotlin.math.min

class ClipSelectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding : SampleClipSelectionViewBinding

    private var songDurationMs: Long = 0L
    private var currentTimeMs: Long = 0L
    private val clipDurationMs = 60000L // 1 minute
    private var trackWidth = 0

    private var isDragging = false
    private var lastTouchX = 0f

    var onSeekTo: ((timeMs: Long) -> Unit)? = null

    init {
        binding = SampleClipSelectionViewBinding.inflate(LayoutInflater.from(context), this, true)
        // Setup layout observation
        setupLayoutObserver()
        // Setup touch handling
        setupTouchHandling()
    }

    private fun setupLayoutObserver() = with(binding) {
        fullTrackBackground.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                trackWidth = fullTrackBackground.width
                if (trackWidth > 0) {
                    fullTrackBackground.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    updateClipPosition()
                }
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchHandling() = with(binding) {
        selectedClipHighlight.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = true
                    lastTouchX = event.rawX
                    parent.requestDisallowInterceptTouchEvent(true)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (isDragging && songDurationMs > 0) {
                        val deltaX = event.rawX - lastTouchX
                        val currentMarginStart = (selectedClipHighlight.layoutParams as MarginLayoutParams).marginStart
                        val maxPosition = trackWidth - selectedClipHighlight.width
                        val maxSeekTime = songDurationMs - clipDurationMs

                        // Calculate new position
                        val newPosition = (currentMarginStart + deltaX).toInt().coerceIn(0, maxPosition)
                        // Calculate corresponding time
                        val timeRatio = newPosition.toFloat() / maxPosition
                        val newTimeMs = (timeRatio * maxSeekTime).toLong().coerceAtLeast(0L)

                        // Update position and ui
                        currentTimeMs = newTimeMs
                        updateClipPosition()
                        updateTimeLabels()
                        onSeekTo?.invoke(newTimeMs)

                        lastTouchX = event.rawX
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDragging = false
                    parent.requestDisallowInterceptTouchEvent(false)
                    true
                }

                else -> false
            }
        }

        // Also handle taps on background track for seeking
        fullTrackBackground.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN && !isDragging) {
                if (trackWidth > 0 && songDurationMs > 0) {
                    val touchX = event.x
                    val timeRatio = touchX / trackWidth
                    val newTimeMs = (timeRatio * songDurationMs).toLong().coerceIn(0L, songDurationMs)

                    currentTimeMs = newTimeMs
                    updateClipPosition()
                    updateTimeLabels()
                    onSeekTo?.invoke(newTimeMs)
                }
                true
            } else false
        }
    }

    private fun updateClipPosition() = with(binding) {
        if (trackWidth <= 0 || songDurationMs <= 0) return

        // Calculate clip width based on 1 minute duration
        val clipWidthRatio = clipDurationMs.toFloat() / songDurationMs
        val clipWidth = (clipWidthRatio * trackWidth).toInt()

        // Calculate clip position based on current time
        val currentTimeRatio = currentTimeMs.toFloat() / songDurationMs
        val maxClipStart = trackWidth - clipWidth
        val clipStart = (currentTimeRatio * trackWidth).toInt()
        val adjustedClipStart = min(clipStart, maxClipStart)

        // Update selectedClipHighlight position and width
        selectedClipHighlight.layoutParams =
            (selectedClipHighlight.layoutParams as MarginLayoutParams).apply {
                marginStart = adjustedClipStart
                width = max(clipWidth, 1) // Minimum width 1px
            }
        selectedClipHighlight.requestLayout()
    }

    fun setSongDuration(durationMs: Long) {
        songDurationMs = durationMs
        updateTimeLabels()
        updateClipPosition()
    }

    fun setCurrentTime(timeMs: Long) {
        if (!isDragging) { // Don't update position if user is dragging
            currentTimeMs = timeMs
            updateClipPosition()
            updateTimeLabels()
        }
    }

    private fun updateTimeLabels() = with(binding) {
        tvStartTime.text = formatDuration(currentTimeMs)
        tvEndTime.text = formatDuration(songDurationMs)
    }

    @SuppressLint("DefaultLocale")
    private fun formatDuration(durationMillis: Long): String {
        val totalSeconds = durationMillis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}