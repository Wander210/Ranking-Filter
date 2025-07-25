package com.gdd.rankingfilter.view.custom

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import androidx.core.content.withStyledAttributes
import com.gdd.rankingfilter.R
import com.gdd.rankingfilter.extention.dpToPx

class ListRankingFilterView @JvmOverloads constructor(
    context: android.content.Context,
    attrs: android.util.AttributeSet? = null,
    defStyleAttr: Int = 0
): View(context, attrs, defStyleAttr) {

    private var itemCount = 10
    private var itemSize = 20f.dpToPx(context)
    private var itemSpacing = 5f.dpToPx(context)
    private var itemRadius = 5f.dpToPx(context)
    private var itemColor = android.R.color.darker_gray
    private var textColor = android.R.color.white
    private var distanceBetweenItemX = 10f.dpToPx(context)
    private var distanceTwoCenterX = -(distanceBetweenItemX + itemSize)
    private var isSelected = MutableList(itemCount) { false }
    private val rect = RectF()

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
        attrs.let {
            context.withStyledAttributes(it, R.styleable.ListRankingFilterView, 0, 0) {
                itemCount = getInt(R.styleable.ListRankingFilterView_itemCount, itemCount)
                itemSize = getDimension(R.styleable.ListRankingFilterView_itemSize, itemSize)
                itemSize = getDimension(R.styleable.ListRankingFilterView_itemSize, itemSize)
                itemSpacing = getDimension(R.styleable.ListRankingFilterView_itemSpacing, itemSpacing)
                itemColor = getResourceId(R.styleable.ListRankingFilterView_itemColor, itemColor)
                textColor = getResourceId(R.styleable.ListRankingFilterView_textColor, textColor)
            }
            // Update paint colors based on attributes
            recPaint.color = context.getColor(itemColor)
            textPaint.color = context.getColor(textColor)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = (itemSize * 2).toInt() + distanceBetweenItemX.toInt() + paddingLeft + paddingRight
        val height = ((itemSize + itemSpacing) * itemCount - itemSpacing).toInt() + paddingTop + paddingBottom
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (i in 0 until itemCount) {
            // Draw rectangle
            val top = paddingTop + i * (itemSize + itemSpacing)
            val bottom = top + itemSize
            val left1 = paddingLeft.toFloat()
            val right1 = left1 + itemSize
            rect.set(left1, top, right1, bottom)
            canvas.drawRoundRect(rect, itemRadius, itemRadius, recPaint)

            // Draw text
            val text = (i + 1).toString()
            val x = if(!isSelected[i]) left1 + itemSize / 2 else right1 + distanceBetweenItemX + itemSize / 2
            val y = top + itemSize / 2 - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText(text, x, y, textPaint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if(event.action == MotionEvent.ACTION_DOWN) {
            for(i in 0 until itemCount) {
                val top = paddingTop + i * (itemSize + itemSpacing)
                val bottom = top + itemSize
                val left1 = paddingLeft.toFloat()
                val right1 = left1 + itemSize
                if(event.x >= left1 && event.x <= right1 &&
                   event.y >= top && event.y <= bottom) {
                    animateTextSlide(i)
                    isSelected[i] = true
                    break
                }
            }
        }
        return true
    }

    private fun animateTextSlide(index: Int) {
        val anim = ValueAnimator.ofFloat(0f, distanceTwoCenterX / 2, distanceTwoCenterX).apply{
            duration = 300L
            // Update drawing data and request a redraw every animation frame
            addUpdateListener {
                val top = (paddingTop + index * (itemSize + itemSpacing)).toInt()
                val bottom = top + itemSize.toInt()
                val left = paddingLeft
                val right = (paddingLeft + 2 * itemSize + distanceBetweenItemX).toInt()
                // update just this item area
                postInvalidateOnAnimation(left, top, right, bottom)
            }
        }
        anim.start()
    }
}