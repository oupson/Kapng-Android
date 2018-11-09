package oupson.apng

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.widget.ImageView
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import oupson.apng.exceptions.NotApngException
import java.io.File
import java.net.URL

/**
 * Class to play APNG
 */
class ApngAnimator {
    var isPlaying = true
        private set(value) {
            field = value
        }

    private var frames = ArrayList<Frame>()
    private var myHandler: Handler = Handler()
    private var counter = 0
    private val generatedFrame = ArrayList<Bitmap>()
    private var speed: Int? = null
    private var lastFrame: Frame? = null
    private var bitmapBuffer: Bitmap? = null
    private var background: Bitmap? = null
    private var imageView: ImageView? = null
    private var anim: CustomAnimationDrawable? = null
    private var activeAnimation: CustomAnimationDrawable? = null
    private var currentDrawable = 0
    private var animationLoopListener: AnimationListener? = null

    /**
     * Load into an imageview
     * @param imageView Image view selected.
     */
    fun loadInto(imageView: ImageView): ApngAnimator {
        this.imageView = imageView
        return this
    }

    /**
     * Load an APNG file and starts playing the animation.
     * @param file The file to load
     * @throws NotApngException
     */
    fun load(file: File, frameDuration: Int? = null, animationListener: AnimationListener? = null) {
        doAsync {
            // Download PNG
            APNGDisassembler(file.readBytes()).pngList.apply {
                draw(this)
            }

            setupAnimationDrawableAndStart(frameDuration, animationListener)
        }
    }

    /**
     * Load an APNG file and starts playing the animation.
     * @param context The current context.
     * @param url URL to load.
     * @param animationListener The listener that will be invoked when there are specific animation events.
     * @param frameDuration The duration to show each frame. If this is null then the duration specified
     * in the APNG will be used instead.
     * @throws NotApngException
     */
    fun loadUrl(context: Context, url: URL, frameDuration: Int? = null, animationListener: AnimationListener? = null) {
        doAsync(exceptionHandler = { e -> e.printStackTrace() }) {
            // Download PNG
            APNGDisassembler(Loader().load(context, url)).pngList.apply {
                draw(this)
            }

            setupAnimationDrawableAndStart(frameDuration, animationListener)
        }
    }


    /**
     * Load an APNG file and starts playing the animation.
     * @param byteArray ByteArray of the file
     * @param animationListener The listener that will be invoked when there are specific animation events.
     * @param frameDuration The duration to show each frame. If this is null then the duration specified
     * in the APNG will be used instead.
     * @throws NotApngException
     */
    fun load(byteArray: ByteArray, frameDuration: Int? = null, animationListener: AnimationListener? = null) {
        doAsync {
            APNGDisassembler(byteArray).pngList.apply {
                draw(this)
            }

            setupAnimationDrawableAndStart(frameDuration, animationListener)
        }
    }

    /**
     * Sets up the animation drawable and any required listeners. The animation will automatically start.
     * @param animationListener The listener that will be invoked when there are specific animation events.
     * @param frameDuration The duration to show each frame. If this is null then the duration specified
     * in the APNG will be used instead.
     */
    private fun setupAnimationDrawableAndStart(frameDuration: Int? = null, animationListener: AnimationListener? = null) {
        doAsync {
            var innerAnimationListener: CustomAnimationDrawable.AnimationListener? = null
            animationListener?.apply {
                innerAnimationListener = object : CustomAnimationDrawable.AnimationListener {
                    override fun onAnimationLooped() {
                        animationListener.onAnimationLooped()
                    }
                }
            }

            anim = toAnimationDrawable(innerAnimationListener, frameDuration)
            activeAnimation = anim
            uiThread {
                imageView?.apply {
                    setImageBitmap(generatedFrame[0])
                    setImageDrawable(activeAnimation)
                }
                activeAnimation?.start()
            }
        }
    }

    /**
     * Load an APNG file
     * @param context The current context.
     * @param string Path of the file.
     * @param animationListener The listener that will be invoked when there are specific animation events.
     * @param frameDuration The duration to show each frame. If this is null then the duration specified
     * in the APNG will be used instead.
     * @throws NotApngException
     */
    fun load(context: Context, string: String, frameDuration: Int? = null, animationListener: AnimationListener? = null) {
        if (string.contains("http") || string.contains("https")) {
            val url = URL(string)
            loadUrl(context, url, frameDuration, animationListener)
        } else if (File(string).exists()) {
            load(File(string), frameDuration, animationListener)
        }
    }

    /**
     * Draw frames
     */
    private fun draw(extractedFrame: ArrayList<Frame>) {
        // Set last frame
        frames = extractedFrame
        bitmapBuffer = Bitmap.createBitmap(frames[0].maxWidth!!, frames[0].maxHeight!!, Bitmap.Config.ARGB_8888)
        for (i in 0 until frames.size) {
            // Iterator
            val it = frames[i]
            // Current bitmap for the frame
            val btm = Bitmap.createBitmap(frames[0].maxWidth!!, frames[0].maxHeight!!, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(btm)
            val current = BitmapFactory.decodeByteArray(it.byteArray, 0, it.byteArray.size).copy(Bitmap.Config.ARGB_8888, true)
            // Write buffer to canvas
            canvas.drawBitmap(bitmapBuffer, 0f, 0f, null)
            // Clear current frame rect
            // If `blend_op` is APNG_BLEND_OP_SOURCE all color components of the frame, including alpha, overwrite the current contents of the frame's output buffer region.
            if (it.blend_op == Utils.Companion.blend_op.APNG_BLEND_OP_SOURCE) {
                canvas.drawRect(it.x_offsets!!.toFloat(), it.y_offsets!!.toFloat(), it.x_offsets!! + current.width.toFloat(), it.y_offsets!! + current.height.toFloat(), { val paint = Paint(); paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR); paint }())
            }
            // Draw the bitmap
            canvas.drawBitmap(current, it.x_offsets!!.toFloat(), it.y_offsets!!.toFloat(), null)
            generatedFrame.add(btm)
            // Don't add current frame to bitmap buffer
            if (frames[i].dispose_op == Utils.Companion.dispose_op.APNG_DISPOSE_OP_PREVIOUS) {
                //Do nothings
            }
            // Add current frame to bitmap buffer
            // APNG_DISPOSE_OP_BACKGROUND: the frame's region of the output buffer is to be cleared to fully transparent black before rendering the next frame.
            else if (it.dispose_op == Utils.Companion.dispose_op.APNG_DISPOSE_OP_BACKGROUND) {
                val res = Bitmap.createBitmap(frames[0].maxWidth!!, frames[0].maxHeight!!, Bitmap.Config.ARGB_8888)
                val can = Canvas(res)
                can.drawBitmap(btm, 0f, 0f, null)
                can.drawRect(it.x_offsets!!.toFloat(), it.y_offsets!!.toFloat(), it.x_offsets!! + it.width.toFloat(), it.y_offsets!! + it.height.toFloat(), { val paint = Paint(); paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR); paint }())
                bitmapBuffer = res
            } else {
                bitmapBuffer = btm
            }

        }
    }

    fun pause() {
        isPlaying = false
        val animResume = CustomAnimationDrawable()
        activeAnimation?.stop()
        val currentFrame = activeAnimation!!.current

        frameLoop@ for (i in 0 until anim?.numberOfFrames!!) {
            val checkFrame = activeAnimation!!.getFrame(i)

            if (checkFrame === currentFrame) {
                val frameIndex = i
                for (k in frameIndex until activeAnimation!!.numberOfFrames) {
                    val frame = activeAnimation!!.getFrame(k)
                    animResume.addFrame(frame, activeAnimation?.getDuration(k)!!)
                }
                for (k in 0 until frameIndex) {
                    val frame = activeAnimation!!.getFrame(k)
                    animResume.addFrame(frame, activeAnimation?.getDuration(k)!!)
                }
                activeAnimation = animResume
                imageView?.setImageDrawable(activeAnimation)
                imageView?.invalidate()
                break@frameLoop
            }
        }
    }

    fun play() {
        isPlaying = true
        activeAnimation?.start()
    }

    /**
     * Converts the generated frames into an animation drawable ([CustomAnimationDrawable])
     *
     * @param animationListener The listener that will be invoked when there are specific animation events.
     * @param frameDuration The duration to show each frame. If this is null then the duration specified
     * in the APNG will be used instead.
     */
    private fun toAnimationDrawable(animationListener: CustomAnimationDrawable.AnimationListener? = null,
                                    frameDuration: Int? = null): CustomAnimationDrawable {

        return CustomAnimationDrawable().apply {
            for (i in 0 until generatedFrame.size) {
                addFrame(BitmapDrawable(generatedFrame[i]), frameDuration
                        ?: frames[i].delay.toInt())
            }

            animationListener?.let { listener ->
                this.setAnimationListener(listener)
            }
        }
    }

    /**
     * Interface that exposes callbacks for events during the animation.
     */
    interface AnimationListener {

        /**
         * The animation has performed a loop.
         */
        fun onAnimationLooped()
    }
}