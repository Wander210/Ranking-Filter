package com.gdd.rankingfilter.view.custom.circular_spinner

import android.content.Context
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.gdd.rankingfilter.extention.dpToPx
import kotlin.math.abs

class CircularSpinnerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private var coverUrlList: List<String> = emptyList()
    var onItemSelectedListener: ((position: Int) -> Unit)? = null
    private val snapHelper = PagerSnapHelper()
    private lateinit var spinnerAdapter: CircularSpinnerAdapter
    private val linearLayoutManager =
        LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    private var isScrolling = false
    private var isInitialized = false

    private val screenWidth = resources.displayMetrics.widthPixels
    private val itemSize = 80f.dpToPx(context)
    private var itemSpacing = 0f

    init {
        setupRecyclerView()
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        // Tính height cần thiết cho item scale 1.2x + thêm padding
        val desiredH = (itemSize * 1.2f + paddingTop + paddingBottom).toInt()
        val measuredH = if (layoutParams.height == LayoutParams.WRAP_CONTENT)
            resolveSize(desiredH, heightSpec)
        else MeasureSpec.getSize(heightSpec)
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), measuredH)
    }

    private fun setupRecyclerView() {
        layoutManager = linearLayoutManager
        itemSpacing = (screenWidth - paddingEnd - paddingStart - itemSize * 4f) / 3f
        snapHelper.attachToRecyclerView(this)
        clipToPadding = false
        clipChildren = false

        addOnScrollListener(object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                updateItemScalesAndSizes()
                updateItemPositionY()
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                isScrolling = newState != SCROLL_STATE_IDLE

                if (newState == SCROLL_STATE_IDLE) {
                    val centerView = snapHelper.findSnapView(linearLayoutManager)
                    centerView?.let { view ->
                        val position = getChildAdapterPosition(view)
                        if (position != NO_POSITION && coverUrlList.isNotEmpty()) {
                            val actualPosition = position % coverUrlList.size
                            onItemSelectedListener?.invoke(actualPosition)
                        }
                    }

                    // FORCE REBIND all current ViewHolders to ensure clickListener is active
                    if (!isInitialized) {
                        forceRebindVisibleViewHolders()
                        isInitialized = true
                    }
                }
            }
        })
    }

    private fun updateItemScalesAndSizes() {
        val centerX = width / 2f
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val childCenterX = child.left + child.width / 2f
            val distanceFromCenterX = abs(centerX - childCenterX)
            val threshold = itemSize / 2f + itemSpacing
            if (distanceFromCenterX < threshold) {
                child.scaleX = 1.2f
                child.scaleY = 1.2f
            } else {
                child.scaleX = 1.0f
                child.scaleY = 1.0f
            }
        }
    }

    private fun updateItemPositionY() {
        val centerY = height / 2f
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val childCenterX = child.top + child.height / 2f
            child.translationY = centerY - childCenterX
        }
    }

    private fun forceRebindVisibleViewHolders() {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val viewHolder = getChildViewHolder(child)
            if (viewHolder is CircularSpinnerAdapter.RankingViewHolder) {
                val position = getChildAdapterPosition(child)
                if (position != NO_POSITION && coverUrlList.isNotEmpty()) {
                    val actualPosition = position % coverUrlList.size
                    val coverUrl = coverUrlList[actualPosition]
                    viewHolder.bind(coverUrl, position)
                }
            }
        }
        post {
            updateItemScalesAndSizes()
            updateItemPositionY()
        }
    }

    fun setItems(newItems: List<String>) {
        coverUrlList = newItems
        if (coverUrlList.isNotEmpty()) {
            spinnerAdapter =
                CircularSpinnerAdapter(coverUrlList, itemSpacing) { clickedView, position ->
                    handleItemClick(clickedView, position)
                }
            adapter = spinnerAdapter

            post {
                val middleIndex = coverUrlList.size / 2
                val startPosition = 5000 + middleIndex
                // Scroll to the position near the middle
                scrollToPosition(startPosition - 2)
                /// Smooth scroll to the middle
                postDelayed({
                    smoothScrollToPosition(startPosition)
                }, 50)
            }
        }
    }

    private fun handleItemClick(clickedView: View, position: Int) {
        if (!isInitialized || isScrolling || coverUrlList.isEmpty()) return

        // Calculate the distance from the clicked item to the center of the RecyclerView
        val clickedItemCenter = clickedView.left + clickedView.width / 2f
        val distanceToCenter = clickedItemCenter - width / 2f
        onItemSelectedListener?.invoke(position % coverUrlList.size)
        smoothScrollBy(distanceToCenter.toInt(), 0)
    }

    override fun smoothScrollToPosition(position: Int) {
        val smoothScroller = object : LinearSmoothScroller(context) {
            override fun getVerticalSnapPreference(): Int = SNAP_TO_START
            override fun getHorizontalSnapPreference(): Int = SNAP_TO_START

            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?): Float {
                return 80f / (displayMetrics?.densityDpi ?: 160) // Slower for better UX
            }
        }
        smoothScroller.targetPosition = position - 1
        linearLayoutManager.startSmoothScroll(smoothScroller)
    }
}