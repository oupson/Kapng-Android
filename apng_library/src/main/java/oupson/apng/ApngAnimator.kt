package oupson.apng

import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.net.Uri
import android.widget.ImageView
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.uiThread
import oupson.apng.Utils.Companion.isApng
import oupson.apng.exceptions.NotApngException
import java.io.File
import java.net.URL

/**
 * Class to play APNG
 */
class ApngAnimator(private val context: Context) {
    var isPlaying = true
        private set(value) {
            field = value
        }
    private var frames = ArrayList<Frame>()
    private val generatedFrame = ArrayList<Bitmap>()
    var speed: Float? = null
        set(value)  {
            if (isApng) {
                field = value
                try {
                    pause()
                    play()
                } catch (e: Exception) {
                }
            }
        }
    private var bitmapBuffer: Bitmap? = null
    private var imageView: ImageView? = null
    private var anim: CustomAnimationDrawable? = null
    private var activeAnimation: CustomAnimationDrawable? = null
    private var doOnLoaded : (ApngAnimator) -> Unit = {}
    private var AnimationLoopListener : () -> Unit = {}
    private var duration : ArrayList<Float>? = null

    var isApng = false
    var loadNoApng = true
    private val sharedPreferences : SharedPreferences = context.getSharedPreferences("apngAnimator", Context.MODE_PRIVATE)

    init {
        loadNoApng = sharedPreferences.getBoolean("loadNoApng", true)
    }

    fun loadNoApng(boolean: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("loadNoApng", boolean)
        editor.apply()
    }

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
    fun load(file: File, speed: Float? = null) {
        doAsync {
            val bytes = file.readBytes()
            if (isApng(bytes)) {
                isApng = true
                this@ApngAnimator.speed = speed
                // Download PNG
                APNGDisassembler.disassemble(file.readBytes()).frames.apply {
                    draw(this)
                }
                setupAnimationDrawableAndStart()
            } else {
                if (loadNoApng) {
                    context.runOnUiThread {
                        imageView?.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                    }
                } else {
                    throw NotApngException()
                }
            }
        }
    }

    fun load(uri : Uri, speed: Float? = null) {
        doAsync {
            val bytes = context.contentResolver.openInputStream(uri).readBytes()
            if (isApng(bytes)) {
                isApng = true
                this@ApngAnimator.speed = speed
                // Download PNG
                APNGDisassembler.disassemble(bytes).frames.apply {
                    draw(this)
                }
                setupAnimationDrawableAndStart()
            } else {
                if (loadNoApng) {
                    context.runOnUiThread {
                        imageView?.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                    }
                } else {
                    throw NotApngException()
                }
            }
        }
    }

    /**
     * Load an APNG file and starts playing the animation.
     * @param context The current context.
     * @param url URL to load.
     * @throws NotApngException
     */
    fun loadUrl(url: URL, speed: Float? = null) {
        doAsync(exceptionHandler = { e -> e.printStackTrace() }) {
            this@ApngAnimator.speed = speed
            // Download PNG
            Loader.load(context, url).apply {
                if (isApng(this)) {
                    isApng = true
                    APNGDisassembler.disassemble(this).frames.apply {
                        draw(this)
                    }
                    setupAnimationDrawableAndStart()
                } else {
                    if (loadNoApng) {
                        context.runOnUiThread {
                            imageView?.setImageBitmap(BitmapFactory.decodeByteArray(this@apply, 0, this@apply.size))
                        }
                    } else {
                        throw NotApngException()
                    }
                }
            }


        }
    }


    /**
     * Load an APNG file and starts playing the animation.
     * @param byteArray ByteArray of the file
     * @throws NotApngException
     */
    fun load(byteArray: ByteArray, speed: Float? = null) {
        doAsync {
            this@ApngAnimator.speed = speed
            if (isApng(byteArray)) {
                isApng = true
                APNGDisassembler.disassemble(byteArray).frames.apply {
                    draw(this)
                }
                setupAnimationDrawableAndStart()
            } else {
                if (loadNoApng) {
                    context.runOnUiThread {
                        imageView?.setImageBitmap(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size))
                    }
                } else {
                    throw NotApngException()
                }
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
    fun load(string: String, speed : Float? = null) {
        doAsync {
            this@ApngAnimator.speed = speed
            if (string.contains("http") || string.contains("https")) {
                val url = URL(string)
                loadUrl(url, speed)
            } else if (File(string).exists()) {
                var pathToLoad = if (string.startsWith("content://")) string else "file://$string"
                pathToLoad = pathToLoad.replace("%", "%25").replace("#", "%23")
                val bytes = context.contentResolver.openInputStream(Uri.parse(pathToLoad)).readBytes()
                if (isApng(bytes)) {
                    load(bytes, speed)
                } else {
                    if (loadNoApng) {
                        context.runOnUiThread {
                            imageView?.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                        }
                    } else {
                        throw NotApngException()
                    }
                }
            }
        }
    }

    /**
     * Sets up the animation drawable and any required listeners. The animation will automatically start.
     * @param animationListener The listener that will be invoked when there are specific animation events.
     * @param frameDuration The duration to show each frame. If this is null then the duration specified
     * in the APNG will be used instead.
     */
    private fun setupAnimationDrawableAndStart() {
        doAsync {
            anim = toAnimationDrawable()
            activeAnimation = anim
            uiThread {
                imageView?.apply {
                    setImageBitmap(generatedFrame[0])
                    setImageDrawable(activeAnimation)
                }
                activeAnimation?.start()
                doOnLoaded(this@ApngAnimator)
            }
        }
    }

    /**
     * Draw frames
     */
    private fun draw(extractedFrame: ArrayList<Frame>) {
        // Set last frame
        duration = ArrayList()
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
            duration?.add(it.delay / (speed ?: 1f))

        }
    }

    fun pause() {
        if (isApng) {
            isPlaying = false
            val animResume = CustomAnimationDrawable()
            activeAnimation?.stop()
            val currentFrame = activeAnimation!!.current
            val dura = ArrayList<Float>()
            frameLoop@ for (i in 0 until anim?.numberOfFrames!!) {
                val checkFrame = activeAnimation!!.getFrame(i)
                if (checkFrame === currentFrame) {
                    val frameIndex = i
                    for (k in frameIndex until activeAnimation!!.numberOfFrames) {
                        val frame = activeAnimation!!.getFrame(k)
                        animResume.addFrame(frame, (duration!![k] / (speed ?: 1f)).toInt())
                        dura.add(duration!![k])
                    }
                    for (k in 0 until frameIndex) {
                        val frame = activeAnimation!!.getFrame(k)
                        animResume.addFrame(frame, (duration!![k] / (speed ?: 1f)).toInt())
                        dura.add(duration!![k])
                    }
                    activeAnimation = animResume
                    imageView?.setImageDrawable(activeAnimation)
                    activeAnimation?.setOnAnimationLoopListener(AnimationLoopListener)
                    imageView?.invalidate()
                    duration = dura
                    break@frameLoop
                }
            }
        }
    }

    fun play() {
        if (isApng) {
            isPlaying = true
            activeAnimation?.start()
        }
    }

    fun setOnAnimationLoopListener(animationLoopListener : () -> Unit) {
        if (isApng) {
            AnimationLoopListener = animationLoopListener
            anim?.setOnAnimationLoopListener(animationLoopListener)
        }
    }

    fun onLoaded(f : (ApngAnimator) -> Unit) {
        doOnLoaded = f
    }

    /**
     * Converts the generated frames into an animation drawable ([CustomAnimationDrawable])
     * in the APNG will be used instead.
     */
    private fun toAnimationDrawable( ): CustomAnimationDrawable {
        if (isApng) {
            return CustomAnimationDrawable().apply {
                for (i in 0 until generatedFrame.size) {
                    addFrame(BitmapDrawable(generatedFrame[i]), ((frames[i].delay).toInt() / (speed
                            ?: 1f)).toInt())
                }
            }
        } else {
            throw NotApngException()
        }
    }
}