package oupson.apng.customView

import android.content.Context
import android.widget.ImageView
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.util.AttributeSet
import oupson.apng.extractedFrame


class ApngImageView(context: Context, attrs: AttributeSet) : ImageView(context, attrs) {
    var Frames = ArrayList<extractedFrame>()
    var myHandler: Handler
    var counter = 0

    val generatedFrame = ArrayList<Bitmap>()

    init {
        myHandler = Handler()
    }
    fun load(frames : ArrayList<extractedFrame>) {
        Frames = frames

        Frames.forEach {
            val btm = Bitmap.createBitmap(Frames[0].maxWidth, Frames[0].maxHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(btm)
            canvas.drawBitmap(it.bitmap, it.xoffset.toFloat(), it.yoffset.toFloat(), null)
            generatedFrame.add(btm)
        }

        nextFrame()
    }

    fun nextFrame() {
        if (counter == Frames.size) {
            counter = 0
        }
        val delay = Frames[counter].delay
        this.setImageBitmap(generatedFrame[counter])
        counter++
        myHandler.postDelayed({
            nextFrame()
        }, delay.toLong())
    }
}