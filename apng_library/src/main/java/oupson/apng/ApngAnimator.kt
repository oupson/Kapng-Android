package oupson.apng

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Environment
import android.os.Handler
import android.widget.ImageView
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.File
import java.net.URL

class ApngAnimator(val imageView : ImageView) {
    var play = true
    var Frames = ArrayList<Frame>()
    var myHandler: Handler
    var counter = 0

    val generatedFrame = ArrayList<Bitmap>()

    init {
        myHandler = Handler()


    }

    fun load(file: File) {
        val extractedFrame = APNGDisassembler(file.readBytes()).pngList
        Frames = extractedFrame

        Frames.forEach {
            val btm = Bitmap.createBitmap(Frames[0].maxWidth, Frames[0].maxHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(btm)
            canvas.drawBitmap(BitmapFactory.decodeByteArray(it.byteArray, 0, it.byteArray.size), it.x_offsets.toFloat(), it.y_offsets.toFloat(), null)
            generatedFrame.add(btm)
        }

        nextFrame()
    }

    fun load(string: String) {
        if (string.contains("http") || string.contains("https")) {
            val url = URL(string)
            doAsync {
                val extractedFrame = APNGDisassembler(Loader().load(url)).pngList
                Frames = extractedFrame

                Frames.forEach {
                    val btm = Bitmap.createBitmap(Frames[0].maxWidth, Frames[0].maxHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(btm)
                    canvas.drawBitmap(BitmapFactory.decodeByteArray(it.byteArray, 0, it.byteArray.size), it.x_offsets.toFloat(), it.y_offsets.toFloat(), null)
                    generatedFrame.add(btm)
                }
                uiThread {
                    nextFrame()
                }
            }
        } else if (File(string).exists()) {
            val extractedFrame = APNGDisassembler(Loader().load(File(string))).pngList
            Frames = extractedFrame

            Frames.forEach {
                val btm = Bitmap.createBitmap(Frames[0].maxWidth, Frames[0].maxHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(btm)
                canvas.drawBitmap(BitmapFactory.decodeByteArray(it.byteArray, 0, it.byteArray.size), it.x_offsets.toFloat(), it.y_offsets.toFloat(), null)
                generatedFrame.add(btm)
            }
            nextFrame()
        }
    }

    fun load(byteArray: ByteArray) {
        val extractedFrame = APNGDisassembler(byteArray).pngList
        Frames = extractedFrame

        Frames.forEach {
            val btm = Bitmap.createBitmap(Frames[0].maxWidth, Frames[0].maxHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(btm)
            canvas.drawBitmap(BitmapFactory.decodeByteArray(it.byteArray, 0, it.byteArray.size), it.x_offsets.toFloat(), it.y_offsets.toFloat(), null)
            generatedFrame.add(btm)
        }

        nextFrame()
    }

    fun nextFrame() {
        if (counter == Frames.size) {
            counter = 0
        }
        val delay = Frames[counter].delay
        imageView.setImageBitmap(generatedFrame[counter])
        counter++
        myHandler.postDelayed({
            mustPlay()
        }, delay.toLong())
    }

    fun pause() {
        play = false

    }
    fun play() {
        if (!play) {
            play = true
            mustPlay()
        }
    }

    private fun mustPlay() {
        if (play) {
            nextFrame()
        }
    }


}