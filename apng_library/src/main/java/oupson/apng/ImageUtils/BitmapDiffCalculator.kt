package oupson.apng.ImageUtils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class BitmapDiffCalculator(val firstBitmap: Bitmap, val secondBitmap : Bitmap) {
    val res : Bitmap
    var xOffset : Int = 0
    var yOffset : Int = 0
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
                if (difBitmap.getPixel(x, height - 1) != Color.TRANSPARENT) {
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
        val btm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas2 = Canvas(btm)
        canvas2.drawBitmap(difBitmap, -xOffset.toFloat(), -yOffset.toFloat(), Paint())
        res = btm
    }
}