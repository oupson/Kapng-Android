package oupson.apng

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

internal class BitmapDrawable(private val bitmap: Bitmap) : Drawable() {

    override fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, null)
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(cf: ColorFilter?) {}

    override fun getIntrinsicWidth(): Int {
        return bitmap.width
    }

    override fun getIntrinsicHeight(): Int {
        return bitmap.height
    }

    override fun getMinimumWidth(): Int {
        return bitmap.width
    }

    override fun getMinimumHeight(): Int {
        return bitmap.height
    }
}