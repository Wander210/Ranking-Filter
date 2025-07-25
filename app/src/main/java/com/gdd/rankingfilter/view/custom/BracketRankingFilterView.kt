package com.gdd.rankingfilter.view.custom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.gdd.rankingfilter.R
import com.gdd.rankingfilter.extention.dpToPx
import kotlin.math.pow

@SuppressLint("UseCompatLoadingForDrawables")
class BracketRankingFilterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var roundCount = 3
    private var backgroundWidth = 0f
    private var backgroundHeight = 0f
    private var itemSpacing = 15f.dpToPx(context)
    private var itemSize = 0f
    private var itemWinnerSize = 0f
    private var columnGap = 0f
    private var pathWidth = 0f
    // flat list: [round0 centers..., round1 centers..., ..., final center]
    private val centerYList = mutableListOf<Float>()
    private val itemDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.bg_ranking_item)

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, android.R.color.holo_green_dark)
        style = Paint.Style.FILL
    }

    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, android.R.color.white)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredW = MeasureSpec.getSize(widthMeasureSpec)
        backgroundWidth = measuredW.toFloat()
        calculateItemSize()
        calculateCenters()
        val desiredH = (backgroundHeight + paddingTop + paddingBottom).toInt()
        // if wrap_content, respect desired height; otherwise use provided spec
        val measuredH = if (layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            resolveSize(desiredH, heightMeasureSpec)
        } else {
            MeasureSpec.getSize(heightMeasureSpec)
        }
        setMeasuredDimension(measuredW, measuredH)
    }

    private fun calculateItemSize() {
        val availableWidth = backgroundWidth - paddingStart - paddingEnd
        // total width = 3.5f * roundCount * itemSize + 2 * itemSpacing
        itemSize = (availableWidth - 2f * itemSpacing) / (3.5f * roundCount)
        itemWinnerSize = 1.5f * itemSize
        columnGap = 0.75f * itemSize
        pathWidth = itemSize * 0.1f
        pathPaint.strokeWidth = pathWidth
        // background height based on outermost round and spacing
        val count = 2.0.pow(roundCount - 1).toInt()
        backgroundHeight = count * itemSize + (count + 1f) * itemSpacing
    }

    private fun calculateCenters() {
        centerYList.clear()
        // compute starting index offsets for each round
        val outerCount = 2.0.pow(roundCount - 1).toInt()
        val totalH = outerCount * itemSize + (outerCount + 1) * itemSpacing
        // outermost round: evenly spaced
        repeat(outerCount) { i ->
            val y = paddingTop + itemSpacing * (i + 1) + itemSize * i + itemSize / 2f
            centerYList.add(y)
        }
        // inner rounds: each center is average of two neighbors
        var startIndex = 0
        while (startIndex < centerYList.size - 1) {
            val centerY = (centerYList[startIndex] + centerYList[startIndex + 1]) / 2f
            centerYList.add(centerY)
            startIndex += 2
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // draw background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // helper to compute offset index for each round
        val offsets = IntArray(roundCount) { idx ->
            (0 until idx).sumOf { i -> 2.0.pow(roundCount - 1 - i).toInt() }
        }

        // draw each round's items and paths
        for (r in 0 until roundCount) {
            val count = 2.0.pow(roundCount - 1 - r).toInt()
            val xLeft = paddingStart + r * (itemSize + columnGap)
            val xRight = backgroundWidth - paddingEnd - r * (itemSize + columnGap)

            repeat(count) { j ->
                val idx = offsets[r] + j
                val centerY = centerYList[idx]
                val top = centerY - itemSize / 2f
                val bottom = centerY + itemSize / 2f

                // Vẽ drawable thay vì drawRect
                drawItemDrawable(canvas, xLeft, top, xLeft + itemSize, bottom)
                drawItemDrawable(canvas, xRight - itemSize, top, xRight, bottom)

                // draw connection paths if not the last round
                if (r < roundCount - 1) {
                    val nextIdx = offsets[r + 1] + j / 2
                    val nextY = centerYList[nextIdx]
                    drawConnectionPath(canvas, xLeft + itemSize, centerY, xLeft + itemSize + columnGap, nextY)
                    drawConnectionPath(canvas, xRight - itemSize, centerY, xRight - itemSize - columnGap, nextY)
                }
            }
        }

        // draw winner item at center
        val finalY = centerYList.last()
        val winTop = finalY - itemWinnerSize / 2f
        val winLeft = backgroundWidth / 2f - itemWinnerSize / 2f
        drawItemDrawable(canvas, winLeft, winTop, winLeft + itemWinnerSize, winTop + itemWinnerSize)
    }

    private fun drawItemDrawable(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        itemDrawable?.let { drawable ->
            drawable.setBounds(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
            drawable.draw(canvas)
        }
    }

    private fun drawConnectionPath(canvas: Canvas, startX: Float, startY: Float, endX: Float, endY: Float) {
        val path = Path()
        val midX = (startX + endX) / 2f

        path.moveTo(startX, startY)
        path.lineTo(midX, startY)
        path.lineTo(midX, endY)
        path.lineTo(endX, endY)

        canvas.drawPath(path, pathPaint)
    }
}