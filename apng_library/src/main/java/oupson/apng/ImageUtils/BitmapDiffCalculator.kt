package oupson.apng.ImageUtils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.support.annotation.ColorInt
import oupson.apng.utils.Utils
import java.util.*

class BitmapDiffCalculator(firstBitmap: Bitmap, secondBitmap : Bitmap) {
    val res : Bitmap
    var xOffset : Int = 0
    var yOffset : Int = 0
    var blend_op = Utils.Companion.blend_op.APNG_BLEND_OP_OVER
    init {
        val difBitmap = Bitmap.createBitmap(firstBitmap.width, firstBitmap.height, Bitmap.Config.ARGB_8888)
        val difCanvas = Canvas(difBitmap)
        for (y in 0 until firstBitmap.height) {
            for (x in 0 until firstBitmap.width) {
                if (firstBitmap.getPixel(x, y) != secondBitmap.getPixel(x, y)) {
                    val colour = secondBitmap.getPixel(x, y)
                    val paint = Paint().apply {
                        this.color = colour
                    }
                    difCanvas.drawPoint(x.toFloat(), y.toFloat(), paint)
                }
            }
        }
        var width = difBitmap.width
        var height = difBitmap.height
        topLoop@for (y in 0 until difBitmap.height){
            for (x in 0 until difBitmap.width) {
                if (difBitmap.getPixel(x, y) != Color.TRANSPARENT) {
                    break@topLoop
                }
            }
            yOffset += 1
        }
        bottomLoop@ while (true) {
            for (x in 0 until difBitmap.width) {
                if (height < 0) {
                  break@bottomLoop
                } else if (difBitmap.getPixel(x, height - 1) != Color.TRANSPARENT) {
                    break@bottomLoop
                }
            }
            height -= 1
        }
        leftLoop@for (x in 0 until difBitmap.width) {
            for (y in 0 until difBitmap.height) {
                if (difBitmap.getPixel(x, y) != Color.TRANSPARENT) {
                    break@leftLoop
                }
            }
            xOffset += 1
        }
        rightLoop@ while (true) {
            for (y in 0 until difBitmap.height) {
                if (difBitmap.getPixel(width - 1, y) != Color.TRANSPARENT) {
                    break@rightLoop
                }
            }
            width -= 1
        }
        val btm = Bitmap.createBitmap(difBitmap, xOffset, yOffset, width - xOffset, height - yOffset)
        res = btm
    }

    fun Bitmap.trim(@ColorInt color: Int = Color.TRANSPARENT): Bitmap {

        var top = height
        var bottom = 0
        var right = width
        var left = 0

        var colored = IntArray(width, { color })
        var buffer = IntArray(width)

        for (y in bottom until top) {
            getPixels(buffer, 0, width, 0, y, width, 1)
            if (!Arrays.equals(colored, buffer)) {
                bottom = y
                break
            }
        }

        for (y in top - 1 downTo bottom) {
            getPixels(buffer, 0, width, 0, y, width, 1)
            if (!Arrays.equals(colored, buffer)) {
                top = y
                break
            }
        }

        val heightRemaining = top - bottom
        colored = IntArray(heightRemaining, { color })
        buffer = IntArray(heightRemaining)

        for (x in left until right) {
            getPixels(buffer, 0, 1, x, bottom, 1, heightRemaining)
            if (!Arrays.equals(colored, buffer)) {
                left = x
                break
            }
        }

        for (x in right - 1 downTo left) {
            getPixels(buffer, 0, 1, x, bottom, 1, heightRemaining)
            if (!Arrays.equals(colored, buffer)) {
                right = x
                break
            }
        }
        return Bitmap.createBitmap(this, left, bottom, right - left, top - bottom)
    }
}