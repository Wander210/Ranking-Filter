package com.gdd.rankingfilter.view.custom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.gdd.rankingfilter.R
import com.gdd.rankingfilter.data.model.Image
import com.gdd.rankingfilter.data.model.RankingItem
import com.gdd.rankingfilter.extention.dpToPx
import kotlin.random.Random

class ListRankingFilterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): ViewGroup(context, attrs, defStyleAttr) {

    private var itemCount = 10
    private var itemSize = 40f.dpToPx(context)
    private var itemSpacing = 15f.dpToPx(context)
    private var itemRadius = 5f.dpToPx(context)
    private var itemColor = android.R.color.darker_gray
    private var textColor = android.R.color.white
    private var distanceBetweenItemX = 10f.dpToPx(context)
    private var distanceTwoCenterX = -(distanceBetweenItemX + itemSize)
    private var isSelected = MutableList(itemCount) { false }
    private val rect = RectF()

    // Profile view
    private lateinit var profileCard: CardView
    private lateinit var profileImageView: ImageView
    private lateinit var profileTextView: TextView
    private var profileCardSize = 150f.dpToPx(context)

    // Random image variables
    private lateinit var imageList: List<Image>
    private var isRandomizing = false
    private var randomHandler = Handler(Looper.getMainLooper())
    private var randomRunnable: Runnable? = null
    private var currentImageIndex = 1 // Start from index 1 to skip first element
    private var usedImages = mutableSetOf<Int>()
    private var placedImages = mutableListOf<ImageView?>() // Store placed images
    private var availableImages = mutableListOf<Int>() // Cache available image indices

    // Random settings - slower for smoother loading
    private val randomInterval = 150L // Slower to ensure images load properly
    private val randomDuration = 3000L // Longer duration

    private var recPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(itemColor)
        style = Paint.Style.FILL
    }
    private var textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(textColor)
        textSize = 20f.dpToPx(context)
        textAlign = Paint.Align.CENTER
    }

    init {
        setWillNotDraw(false)
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.ListRankingFilterView, 0, 0) {
                itemCount = getInt(R.styleable.ListRankingFilterView_itemCount, itemCount)
                itemSize = getDimension(R.styleable.ListRankingFilterView_itemSize, itemSize)
                itemSpacing = getDimension(R.styleable.ListRankingFilterView_itemSpacing, itemSpacing)
                itemColor = getResourceId(R.styleable.ListRankingFilterView_itemColor, itemColor)
                textColor = getResourceId(R.styleable.ListRankingFilterView_textColor, textColor)
            }
            // Update paint colors based on attributes
            recPaint.color = context.getColor(itemColor)
            textPaint.color = context.getColor(textColor)
        }
        initProfileCard()
        initPlacedImagesList()
    }

    private fun initPlacedImagesList() {
        // Initialize placedImages list with nulls
        placedImages = MutableList(itemCount) { null }
    }

    private fun initProfileCard() {
        // Create CardView - remove radius to avoid rounded corners
        profileCard = CardView(context).apply {
            cardElevation = 4f.dpToPx(context)
            radius = 0f // No rounded corners
            setCardBackgroundColor(Color.WHITE)
            useCompatPadding = true
        }

        // Create CardView layout
        val containerLayout = FrameLayout(context).apply {
            setPadding(
                8f.dpToPx(context).toInt(),
                8f.dpToPx(context).toInt(),
                8f.dpToPx(context).toInt(),
                8f.dpToPx(context).toInt()
            )
        }

        // Create ImageView - remove clipToOutline to avoid rounding
        profileImageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = false // Remove this to avoid rounded corners
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Create TextView
        profileTextView = TextView(context).apply {
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            maxLines = 1
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (20f.dpToPx(context)).toInt()
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
        }

        // Add views into Cardview
        containerLayout.addView(profileImageView)
        containerLayout.addView(profileTextView)
        profileCard.addView(containerLayout)

        // Add profile card into custom view
        addView(profileCard)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // đo card thành chính xác kích thước profileCardSize
        val cardSpec = MeasureSpec.makeMeasureSpec(profileCardSize.toInt(), MeasureSpec.EXACTLY)
        profileCard.measure(cardSpec, cardSpec)

        // Measure placed images
        placedImages.forEach { imageView ->
            imageView?.let {
                val imageSpec = MeasureSpec.makeMeasureSpec(itemSize.toInt(), MeasureSpec.EXACTLY)
                it.measure(imageSpec, imageSpec)
            }
        }

        // chiều cao mong muốn từ các item + padding
        val desiredH = ((itemSize + itemSpacing) * itemCount - itemSpacing).toInt()
            .let { it + paddingTop + paddingBottom }
        val measuredH = if (layoutParams.height == RecyclerView.LayoutParams.WRAP_CONTENT)
            resolveSize(desiredH, heightMeasureSpec)
        else MeasureSpec.getSize(heightMeasureSpec)

        // chiều rộng match parent
        val measuredW = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(measuredW, measuredH)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // layout card ở giữa ngang, ở trên cùng
        val cardSize = profileCard.measuredWidth
        val cardLeft = (width - cardSize) / 2
        val cardTop = paddingTop
        profileCard.layout(
            cardLeft,
            cardTop,
            cardLeft + cardSize,
            cardTop + profileCard.measuredHeight
        )

        // Layout placed images - đặt chính xác tại vị trí ô
        placedImages.forEachIndexed { index, imageView ->
            imageView?.let {
                val top = (paddingTop + index * (itemSize + itemSpacing)).toInt()
                val left = paddingLeft.toInt()
                it.layout(
                    left,
                    top,
                    left + itemSize.toInt(),
                    top + itemSize.toInt()
                )
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw ranking items chỉ khi không có ảnh tại vị trí đó
        for (i in 0 until itemCount) {
            if (placedImages[i] == null) {
                // Draw rectangle
                val top = paddingTop + i * (itemSize + itemSpacing)
                val bottom = top + itemSize
                val left1 = paddingLeft.toFloat()
                val right1 = left1 + itemSize
                rect.set(left1, top, right1, bottom)
                canvas.drawRoundRect(rect, itemRadius, itemRadius, recPaint)

                // Draw text chỉ khi chưa có ảnh
                val text = (i + 1).toString()
                val x = left1 + itemSize / 2
                val y = top + itemSize / 2 - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(text, x, y, textPaint)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Disable touch events while randomizing
        if (isRandomizing) {
            return true
        }

        if(event.action == MotionEvent.ACTION_DOWN) {
            for(i in 0 until itemCount) {
                val top = paddingTop + i * (itemSize + itemSpacing)
                val bottom = top + itemSize
                val left1 = paddingLeft.toFloat()
                val right1 = left1 + itemSize
                if(event.x >= left1 && event.x <= right1 &&
                    event.y >= top && event.y <= bottom &&
                    !isSelected[i] && placedImages[i] == null) {

                    // Place current image at selected position
                    placeImageAtPosition(i)
                    isSelected[i] = true

                    // Continue randomizing if there are more images
                    updateAvailableImages()
                    if (availableImages.isNotEmpty()) {
                        startRandomizing()
                    }
                    break
                }
            }
        }
        return true
    }

    private fun placeImageAtPosition(position: Int) {
        if (::imageList.isInitialized && currentImageIndex < imageList.size) {
            val imageView = ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                clipToOutline = false // No rounded corners
                layoutParams = LayoutParams(itemSize.toInt(), itemSize.toInt())
            }

            val url = imageList[currentImageIndex].secure_url

            // Load image without any rounding effects
            Glide.with(context)
                .load(url)
                .apply(RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(Color.LTGRAY.toDrawable())
                    .error(Color.RED.toDrawable())
                    .override(itemSize.toInt(), itemSize.toInt()))
                .into(imageView)

            // Add to used images
            usedImages.add(currentImageIndex)

            // Add image view to layout
            addView(imageView)

            // Store for later layout
            placedImages[position] = imageView

            Log.d("RankingFilter", "Placed image ${currentImageIndex} at position ${position}")

            // Invalidate to redraw
            invalidate()
            requestLayout()
        }
    }

    private fun updateAvailableImages() {
        // Update list of available images (skip index 0, exclude used images)
        availableImages.clear()
        for (i in 1 until imageList.size) {
            if (i !in usedImages) {
                availableImages.add(i)
            }
        }
    }

    // Public function to start the random process
    fun startRandomizing() {
        if (!::imageList.isInitialized || imageList.isEmpty()) {
            return
        }

        updateAvailableImages()
        if (availableImages.isEmpty()) {
            return
        }

        isRandomizing = true

        // Preload available images
        preloadAvailableImages()

        // Wait a bit for preloading then start randomizing
        randomHandler.postDelayed({
            startRandomAnimation()
        }, 200)
    }

    private fun preloadAvailableImages() {
        availableImages.forEach { index ->
            Glide.with(context)
                .load(imageList[index].secure_url)
                .apply(RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(profileCardSize.toInt(), profileCardSize.toInt()))
                .preload()
        }
    }

    private fun startRandomAnimation() {
        if (availableImages.isEmpty()) {
            isRandomizing = false
            return
        }

        // Create runnable for random image switching
        randomRunnable = object : Runnable {
            override fun run() {
                if (availableImages.isNotEmpty()) {
                    currentImageIndex = availableImages[Random.nextInt(availableImages.size)]
                    loadImageToProfile(currentImageIndex)

                    if (isRandomizing) {
                        randomHandler.postDelayed(this, randomInterval)
                    }
                }
            }
        }

        // Start randomizing
        randomHandler.post(randomRunnable!!)

        // Stop randomizing after specified duration
        randomHandler.postDelayed({
            stopRandomizing()
        }, randomDuration)
    }

    private fun stopRandomizing() {
        isRandomizing = false
        randomRunnable?.let { randomHandler.removeCallbacks(it) }

        // Set final random image
        if (availableImages.isNotEmpty()) {
            currentImageIndex = availableImages[Random.nextInt(availableImages.size)]
            loadImageToProfile(currentImageIndex)
        }

        Log.d("RankingFilter", "Stopped randomizing. Current image: $currentImageIndex")
    }

    private fun loadImageToProfile(imageIndex: Int) {
        if (::imageList.isInitialized && imageIndex < imageList.size) {
            val url = imageList[imageIndex].secure_url

            // Load image to profile without any rounding
            Glide.with(profileImageView.context)
                .load(url)
                .apply(RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(Color.LTGRAY.toDrawable())
                    .error(Color.RED.toDrawable())
                    .override(profileCardSize.toInt(), profileCardSize.toInt()))
                .into(profileImageView)
        }
    }

    // Public function to set profile data
    fun setProfileData(rankingItem: RankingItem) {
        imageList = rankingItem.imageList
        usedImages.clear()

        // Skip first element - mark it as used but don't include in random
        if (imageList.isNotEmpty()) {
            usedImages.add(0) // Mark first element as used
            currentImageIndex = if (imageList.size > 1) 1 else 0
        }

        // Clear previous placed images
        placedImages.forEach { it?.let { removeView(it) } }
        placedImages = MutableList(itemCount) { null }

        // Reset selection state
        isSelected = MutableList(itemCount) { false }

        if(imageList.isNotEmpty()) {
            // Always show first image (index 0) in profile initially
            loadImageToProfile(0)
            profileTextView.text = "Name"
            Log.d("RankingFilter", "Profile data set with ${imageList.size} images, first element skipped from random")
        }

        updateAvailableImages()
    }

    // Public function to check if all images are placed (excluding first element)
    fun isGameComplete(): Boolean {
        return availableImages.isEmpty()
    }

    // Public function to reset the game
    fun resetGame() {
        // Stop any running random
        isRandomizing = false
        randomRunnable?.let { randomHandler.removeCallbacks(it) }

        // Clear used images but keep first element marked as used
        usedImages.clear()
        if (::imageList.isInitialized && imageList.isNotEmpty()) {
            usedImages.add(0) // Always skip first element
        }

        // Reset states
        isSelected = MutableList(itemCount) { false }

        // Remove placed images
        placedImages.forEach { it?.let { removeView(it) } }
        placedImages = MutableList(itemCount) { null }

        // Reset current image index
        if (::imageList.isInitialized && imageList.isNotEmpty()) {
            currentImageIndex = if (imageList.size > 1) 1 else 0
            loadImageToProfile(0) // Always show first image in profile
        }

        updateAvailableImages()
        invalidate()
        requestLayout()

        Log.d("RankingFilter", "Game reset. Available images: ${availableImages.size}")
    }
}