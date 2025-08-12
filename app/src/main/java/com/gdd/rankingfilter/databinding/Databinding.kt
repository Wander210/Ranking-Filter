package com.gdd.rankingfilter.databinding

import android.graphics.Color
import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.gdd.rankingfilter.extention.dpToPx

@BindingAdapter("bind:thumbnailUrl")
fun loadImage(view: ImageView, thumbnailUrl: String?) {
    Glide.with(view.context)
        .load(thumbnailUrl)
        .transform(RoundedCorners( 20f.dpToPx(view.context).toInt()))
        .placeholder(Color.LTGRAY.toDrawable())
        .into(view)
}

@BindingAdapter("bind:coverUrl")
fun loadCover(view: ImageView, coverUrl: String?) {
    Glide.with(view.context)
        .load(coverUrl)
        .circleCrop()
        .placeholder(Color.LTGRAY.toDrawable())
        .into(view)
}

