package com.gdd.rankingfilter.view.custom

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.gdd.rankingfilter.R
import kotlin.math.max
import kotlin.math.min

class ClipSelectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var fullTrackBackground: View
    private var selectedClipHighlight: View
    private var tvStartTime: TextView
    private var tvEndTime: TextView

    // Properties
    private var songDurationMs: Long = 0L
    private var currentTimeMs: Long = 0L
    private val clipDurationMs = 60000L // 1 minute
    private var trackWidth = 0

    // Touch handling
    private var isDragging = false
    private var lastTouchX = 0f

    // Callbacks
    var onSeekTo: ((timeMs: Long) -> Unit)? = null

    init {
        // Inflate layout
        LayoutInflater.from(context).inflate(R.layout.sample_clip_selection_view, this, true)

        // Find views
        fullTrackBackground = findViewById(R.id.fullTrackBackground)
        selectedClipHighlight = findViewById(R.id.selectedClipHighlight)
        tvStartTime = findViewById(R.id.tvStartTime)
        tvEndTime = findViewById(R.id.tvEndTime)

        // Setup layout observation
        setupLayoutObserver()

        // Setup touch handling
        setupTouchHandling()
    }

    private fun setupLayoutObserver() {
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
    private fun setupTouchHandling() {
        selectedClipHighlight.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = true
                    lastTouchX = event.rawX
                    parent.requestDisallowInterceptTouchEvent(true)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (isDragging && trackWidth > 0 && songDurationMs > 0) {
                        val deltaX = event.rawX - lastTouchX

                        // Calculate new position
                        val currentMarginStart = (selectedClipHighlight.layoutParams as MarginLayoutParams).marginStart
                        val clipWidth = selectedClipHighlight.width
                        val maxPosition = trackWidth - clipWidth
                        val newPosition = (currentMarginStart + deltaX).toInt().coerceIn(0, maxPosition)

                        // Calculate corresponding time
                        val timeRatio = newPosition.toFloat() / maxPosition
                        val maxSeekTime = songDurationMs - clipDurationMs
                        val newTimeMs = (timeRatio * maxSeekTime).toLong().coerceAtLeast(0L)

                        // Update position and notify
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
            } else {
                false
            }
        }
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

    private fun updateClipPosition() {
        if (trackWidth <= 0 || songDurationMs <= 0) return

        // Calculate clip width based on 1 minute duration
        val clipWidthRatio = clipDurationMs.toFloat() / songDurationMs
        val clipWidthPx = (clipWidthRatio * trackWidth).toInt()

        // Calculate clip position based on current time
        val currentTimeRatio = currentTimeMs.toFloat() / songDurationMs
        val maxStartPosition = trackWidth - clipWidthPx
        val clipStartPx = (currentTimeRatio * trackWidth).toInt()

        // Stop moving when endMs reaches end of track
        val adjustedClipStartPx = min(clipStartPx, maxStartPosition)

        // Update selectedClipHighlight position and width
        selectedClipHighlight.layoutParams =
            (selectedClipHighlight.layoutParams as MarginLayoutParams).apply {
                marginStart = adjustedClipStartPx
                width = max(clipWidthPx, 1) // Minimum width 1px
            }
        selectedClipHighlight.requestLayout()
    }

    private fun updateTimeLabels() {
        // startMs = current time (moves with music)
        tvStartTime.text = formatDuration(currentTimeMs)

        // endMs = total song duration (fixed, cached)
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