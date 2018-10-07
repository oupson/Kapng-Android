package oupson.apng

import android.graphics.*
import android.os.Environment
import android.os.Handler
import android.widget.ImageView
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL


class ApngAnimator(val imageView : ImageView) {
    var play = true

    var Frames = ArrayList<Frame>()

    var myHandler: Handler

    var counter = 0

    val generatedFrame = ArrayList<Bitmap>()

    var speed = 1

    var lastFrame : Frame? = null
    var bitmapBuffer : Bitmap? = null

    var background : Bitmap? = null

    var isDebug = false

    init {
        myHandler = Handler()
    }


    fun load(file: File) {
        // Download PNG
        val extractedFrame = APNGDisassembler(file.readBytes()).pngList

        // Set last frame
        lastFrame = extractedFrame[0]

        // Init image buffer
        bitmapBuffer = BitmapFactory.decodeByteArray(lastFrame?.byteArray, 0, lastFrame?.byteArray!!.size)
        generatedFrame.add(BitmapFactory.decodeByteArray(lastFrame?.byteArray, 0, lastFrame?.byteArray!!.size))
        Frames = extractedFrame
        for (i in 1 until Frames.size) {
            // Iterator
            val it = Frames.get(i)

            // Current bitmap for the frame
            val btm = Bitmap.createBitmap(Frames[0].maxWidth, Frames[0].maxHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(btm)

            val current = BitmapFactory.decodeByteArray(it.byteArray, 0, it.byteArray.size).copy(Bitmap.Config.ARGB_8888, true)

            // Write buffer to canvas
            canvas.drawBitmap(bitmapBuffer, 0f, 0f, null)



            // Clear current frame rect
            // If `blend_op` is APNG_BLEND_OP_SOURCE all color components of the frame, including alpha, overwrite the current contents of the frame's output buffer region.
            if (it.blend_op == Utils.Companion.blend_op.APNG_BLEND_OP_SOURCE) {
                canvas.drawRect(it.x_offsets.toFloat(), it.y_offsets.toFloat(), it.x_offsets + current.width.toFloat(), it.y_offsets + current.height.toFloat(), { val paint = Paint(); paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR); paint }())
            }

            // Draw the bitmap
            canvas.drawBitmap(current, it.x_offsets.toFloat(), it.y_offsets.toFloat(), null)
            generatedFrame.add(btm)

            // Don't add current frame to bitmap buffer
            if (Frames[i].dispose_op == Utils.Companion.dispose_op.APNG_DISPOSE_OP_PREVIOUS) {
                //Do nothings
            }
            // Add current frame to bitmap buffer
            // APNG_DISPOSE_OP_BACKGROUND: the frame's region of the output buffer is to be cleared to fully transparent black before rendering the next frame.
            else if (it.dispose_op == Utils.Companion.dispose_op.APNG_DISPOSE_OP_BACKGROUND){
                canvas.drawRect(lastFrame!!.x_offsets.toFloat(), lastFrame!!.y_offsets.toFloat(), lastFrame!!.x_offsets + lastFrame!!.width.toFloat(), lastFrame!!.y_offsets + lastFrame!!.height.toFloat(), { val paint = Paint(); paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR); paint }())
                bitmapBuffer = btm
            }
            else {
                bitmapBuffer = btm
            }
        }
        // DEBUG FUNCTION : WRITE RENDERED FRAME TO EXTERNAL STORAGE
        if (isDebug) {
            for (i in 0 until generatedFrame.size) {
                try {
                    FileOutputStream(File(File(Environment.getExternalStorageDirectory(), "Documents"), "image_$i.png")).use { out ->
                        generatedFrame[i].compress(Bitmap.CompressFormat.PNG, 100, out) // bmp is your Bitmap instance
                        // PNG is a lossless format, the compression factor (100) is ignored
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        nextFrame()
    }

    private fun loadUrl(url : URL) {
        doAsync(exceptionHandler = {e -> e.printStackTrace()}) {
            // Download PNG
            val extractedFrame = APNGDisassembler(Loader().load(url)).pngList

            // Set last frame
            lastFrame = extractedFrame[0]

            // Init image buffer
            bitmapBuffer = BitmapFactory.decodeByteArray(lastFrame?.byteArray, 0, lastFrame?.byteArray!!.size)
            generatedFrame.add(BitmapFactory.decodeByteArray(lastFrame?.byteArray, 0, lastFrame?.byteArray!!.size))
            Frames = extractedFrame
            for (i in 1 until Frames.size) {
                // Iterator
                val it = Frames.get(i)

                // Current bitmap for the frame
                val btm = Bitmap.createBitmap(Frames[0].maxWidth, Frames[0].maxHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(btm)

                val current = BitmapFactory.decodeByteArray(it.byteArray, 0, it.byteArray.size).copy(Bitmap.Config.ARGB_8888, true)

                // Write buffer to canvas
                canvas.drawBitmap(bitmapBuffer, 0f, 0f, null)



                // Clear current frame rect
                // If `blend_op` is APNG_BLEND_OP_SOURCE all color components of the frame, including alpha, overwrite the current contents of the frame's output buffer region.
                if (it.blend_op == Utils.Companion.blend_op.APNG_BLEND_OP_SOURCE) {
                    canvas.drawRect(it.x_offsets.toFloat(), it.y_offsets.toFloat(), it.x_offsets + current.width.toFloat(), it.y_offsets + current.height.toFloat(), { val paint = Paint(); paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR); paint }())
                }

                // Draw the bitmap
                canvas.drawBitmap(current, it.x_offsets.toFloat(), it.y_offsets.toFloat(), null)
                generatedFrame.add(btm)

                // Don't add current frame to bitmap buffer
                if (Frames[i].dispose_op == Utils.Companion.dispose_op.APNG_DISPOSE_OP_PREVIOUS) {
                    //Do nothings
                }
                // Add current frame to bitmap buffer
                // APNG_DISPOSE_OP_BACKGROUND: the frame's region of the output buffer is to be cleared to fully transparent black before rendering the next frame.
                else if (it.dispose_op == Utils.Companion.dispose_op.APNG_DISPOSE_OP_BACKGROUND){
                    val res =  Bitmap.createBitmap(Frames[0].maxWidth, Frames[0].maxHeight, Bitmap.Config.ARGB_8888)
                    val can = Canvas(res)
                    can.drawBitmap(btm, 0f, 0f, null)
                    can.drawRect(lastFrame!!.x_offsets.toFloat(), lastFrame!!.y_offsets.toFloat(), lastFrame!!.x_offsets + lastFrame!!.width.toFloat(), lastFrame!!.y_offsets + lastFrame!!.height.toFloat(), { val paint = Paint(); paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR); paint }())
                    bitmapBuffer = res
                }
                else {
                    bitmapBuffer = btm
                }
            }
            // DEBUG FUNCTION : WRITE RENDERED FRAME TO EXTERNAL STORAGE
            if (isDebug) {
                for (i in 0 until generatedFrame.size) {
                    try {
                        FileOutputStream(File(File(Environment.getExternalStorageDirectory(), "Documents"), "image_$i.png")).use { out ->
                            generatedFrame[i].compress(Bitmap.CompressFormat.PNG, 100, out) // bmp is your Bitmap instance
                            // PNG is a lossless format, the compression factor (100) is ignored
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
            uiThread {
                nextFrame()
            }
        }
    }

    fun load(byteArray: ByteArray) {
        // Download PNG
        val extractedFrame = APNGDisassembler(byteArray).pngList

        // Set last frame
        lastFrame = extractedFrame[0]

        // Init image buffer
        bitmapBuffer = BitmapFactory.decodeByteArray(lastFrame?.byteArray, 0, lastFrame?.byteArray!!.size)
        generatedFrame.add(BitmapFactory.decodeByteArray(lastFrame?.byteArray, 0, lastFrame?.byteArray!!.size))
        Frames = extractedFrame
        for (i in 1 until Frames.size) {
            // Iterator
            val it = Frames.get(i)

            // Current bitmap for the frame
            val btm = Bitmap.createBitmap(Frames[0].maxWidth, Frames[0].maxHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(btm)

            val current = BitmapFactory.decodeByteArray(it.byteArray, 0, it.byteArray.size).copy(Bitmap.Config.ARGB_8888, true)

            // Write buffer to canvas
            canvas.drawBitmap(bitmapBuffer, 0f, 0f, null)

            // APNG_DISPOSE_OP_BACKGROUND: the frame's region of the output buffer is to be cleared to fully transparent black before rendering the next frame.
            if (lastFrame!!.dispose_op == Utils.Companion.dispose_op.APNG_DISPOSE_OP_BACKGROUND) {
                canvas.drawRect(lastFrame!!.x_offsets.toFloat(), lastFrame!!.y_offsets.toFloat(), lastFrame!!.x_offsets + lastFrame!!.width.toFloat(), lastFrame!!.y_offsets + lastFrame!!.height.toFloat(), { val paint = Paint(); paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR); paint }())
            }

            // Clear current frame rect
            // If `blend_op` is APNG_BLEND_OP_SOURCE all color components of the frame, including alpha, overwrite the current contents of the frame's output buffer region.
            if (it.blend_op == Utils.Companion.blend_op.APNG_BLEND_OP_SOURCE) {
                canvas.drawRect(it.x_offsets.toFloat(), it.y_offsets.toFloat(), it.x_offsets + current.width.toFloat(), it.y_offsets + current.height.toFloat(), { val paint = Paint(); paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR); paint }())
            }

            // Draw the bitmap
            canvas.drawBitmap(current, it.x_offsets.toFloat(), it.y_offsets.toFloat(), null)
            generatedFrame.add(btm)

            // Don't add current frame to bitmap buffer
            if (Frames[i].dispose_op == Utils.Companion.dispose_op.APNG_DISPOSE_OP_PREVIOUS) {
                //Do nothings
            }
            // Add current frame to bitmap buffer
            else {
                bitmapBuffer = btm
            }
        }
        // DEBUG FUNCTION : WRITE RENDERED FRAME TO EXTERNAL STORAGE
        if (isDebug) {
            for (i in 0 until generatedFrame.size) {
                try {
                    FileOutputStream(File(File(Environment.getExternalStorageDirectory(), "Documents"), "image_$i.png")).use { out ->
                        generatedFrame[i].compress(Bitmap.CompressFormat.PNG, 100, out) // bmp is your Bitmap instance
                        // PNG is a lossless format, the compression factor (100) is ignored
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        nextFrame()
    }

    fun load(string: String) {
        if (string.contains("http") || string.contains("https")) {
            val url = URL(string)
            loadUrl(url)
        } else if (File(string).exists()) {
            load(File(string))
        }
    }

    fun nextFrame() {
        if (counter == Frames.size) {
            counter = 0
        }
        val delay = Frames[counter].delay
        imageView.setImageBitmap(generatedFrame[counter])
        counter++
        myHandler.postDelayed({
            ifmustPlay()
        }, delay.toLong() * speed)
    }

    fun pause() {
        play = false

    }
    fun play() {
        if (!play) {
            play = true
            ifmustPlay()
        }
    }

    private fun ifmustPlay() {
        if (play) {
            nextFrame()
        }
    }

    fun setFrameSpeed(speed : Int) {
        this.speed = speed
    }
}