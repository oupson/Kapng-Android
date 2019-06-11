package oupson.apng

import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.RawRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import oupson.apng.exceptions.NotApngException
import oupson.apng.utils.ApngAnimatorOptions
import oupson.apng.utils.Utils
import oupson.apng.utils.Utils.Companion.isApng
import java.io.File
import java.net.URL

@Suppress("unused")
fun ImageView.loadApng(file: File, speed: Float? = null, apngAnimatorOptions: ApngAnimatorOptions? = null) = ApngAnimator(this.context).loadInto(this).apply {
    load(file, speed, apngAnimatorOptions)
}
@Suppress("unused")
fun ImageView.loadApng(uri : Uri, speed: Float? = null, apngAnimatorOptions: ApngAnimatorOptions? = null) = ApngAnimator(this.context).loadInto(this).apply {
    load(uri, speed, apngAnimatorOptions)
}
@Suppress("unused")
fun ImageView.loadApng(url: URL, speed: Float? = null, apngAnimatorOptions: ApngAnimatorOptions? = null) = ApngAnimator(this.context).loadInto(this).apply {
    loadUrl(url, speed, apngAnimatorOptions)
}
@Suppress("unused")
fun ImageView.loadApng(byteArray: ByteArray, speed: Float? = null, apngAnimatorOptions: ApngAnimatorOptions? = null) = ApngAnimator(this.context).loadInto(this).apply {
    load(byteArray, speed, apngAnimatorOptions)
}
@Suppress("unused")
fun ImageView.loadApng(string: String, speed : Float? = null, apngAnimatorOptions: ApngAnimatorOptions? = null) = ApngAnimator(this.context).loadInto(this).apply {
    load(string, speed, apngAnimatorOptions)
}
@Suppress("unused")
fun ImageView.loadApng(@RawRes res : Int, speed : Float? = null, apngAnimatorOptions: ApngAnimatorOptions? = null) = ApngAnimator(this.context).loadInto(this).apply {
    load(res, speed, apngAnimatorOptions)
}

/**
 * Class to play APNG
 */
class ApngAnimator(private val context: Context?) {
    var isPlaying = true
        private set(value) {
            field = value
        }

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
    private var imageView: ImageView? = null

    var anim: CustomAnimationDrawable? = null
    private var activeAnimation: CustomAnimationDrawable? = null

    private var doOnLoaded : (ApngAnimator) -> Unit = {}
    private var animationLoopListener : () -> Unit = {}

    private var duration : ArrayList<Float>? = null

    private var scaleType : ImageView.ScaleType? = null

    @Suppress("MemberVisibilityCanBePrivate")
    var isApng = false

    @Suppress("MemberVisibilityCanBePrivate")
    var loadNotApng = true

    private val sharedPreferences : SharedPreferences? by lazy {
        context?.getSharedPreferences("apngAnimator", Context.MODE_PRIVATE)
    }

    init {
        loadNotApng = sharedPreferences?.getBoolean("loadNotApng", true) ?: true
    }

    /**
     * Specify if the library could load non apng file
     */
    @Suppress("unused")
    @SuppressWarnings("WeakerAccess")
    fun loadNotApng(boolean: Boolean) {
        val editor = sharedPreferences?.edit()
        editor?.putBoolean("loadNotApng", boolean)
        editor?.apply()
    }

    /**
     * Load into an ImageView
     * @param imageView Image view selected.
     */
    fun loadInto(imageView: ImageView): ApngAnimator {
        this.imageView = imageView
        return this
    }

    /**
     * Load an APNG file and starts playing the animation.
     * @param file The file to load
     * @param speed The speed
     * @throws NotApngException
     */
    @Suppress("unused")
    @SuppressWarnings("WeakerAccess")
    fun load(file: File, speed: Float? = null, apngAnimatorOptions: ApngAnimatorOptions? = null) : ApngAnimator {
        GlobalScope.launch {
            val bytes = file.readBytes()
            if (isApng(bytes)) {
                isApng = true
                this@ApngAnimator.speed = speed
                scaleType = apngAnimatorOptions?.scaleType
                // Download PNG
                APNGDisassembler.disassemble(bytes).frames.also {frames ->
                    draw(frames).apply {
                        setupAnimationDrawableAndStart(this)
                    }
                }
            } else {
                if (loadNotApng) {
                    GlobalScope.launch(Dispatchers.Main) {
                        imageView?.scaleType = this@ApngAnimator.scaleType ?: ImageView.ScaleType.FIT_CENTER
                        imageView?.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                    }
                } else {
                    throw NotApngException()
                }
            }
        }
        return this
    }

    /**
     * Load an APNG file and starts playing the animation.
     * @param uri The uri to load
     * @param speed The speed
     * @throws NotApngException
     */
    fun load(uri : Uri, speed: Float? = null, apngAnimatorOptions: ApngAnimatorOptions? = null) : ApngAnimator {
        GlobalScope.launch {
            context?.contentResolver?.openInputStream(uri)?.readBytes()?.also {
                if (isApng(it)) {
                    isApng = true
                    this@ApngAnimator.speed = speed
                    scaleType = apngAnimatorOptions?.scaleType
                    // Download PNG
                    APNGDisassembler.disassemble(it).frames.also {frames ->
                        draw(frames).apply {
                            setupAnimationDrawableAndStart(this)
                        }
                    }
                } else {
                    if (loadNotApng) {
                        GlobalScope.launch(Dispatchers.Main) {
                            imageView?.scaleType = this@ApngAnimator.scaleType ?: ImageView.ScaleType.FIT_CENTER
                            imageView?.setImageBitmap(BitmapFactory.decodeByteArray(it, 0, it.size))
                        }
                    } else {
                        throw NotApngException()
                    }
                }
            }
        }
        return this
    }

    /**
     * Load an APNG file and starts playing the animation.
     * @param url URL to load.
     * @param speed The speed
     * @throws NotApngException
     */
    @SuppressWarnings("WeakerAccess")
    fun loadUrl(url: URL, speed: Float? = null, apngAnimatorOptions: ApngAnimatorOptions? = null) : ApngAnimator {
        GlobalScope.launch {
            this@ApngAnimator.speed = speed
            // Download PNG
            Loader.load(context!!, url).apply {
                if (isApng(this)) {
                    isApng = true
                    this@ApngAnimator.speed = speed
                    scaleType = apngAnimatorOptions?.scaleType
                    // Download PNG
                    APNGDisassembler.disassemble(this).frames.also { frames ->
                        draw(frames).apply {
                            setupAnimationDrawableAndStart(this)
                        }
                    }
                } else {
                    if (loadNotApng) {
                        GlobalScope.launch(Dispatchers.Main) {
                            imageView?.scaleType = this@ApngAnimator.scaleType ?: ImageView.ScaleType.FIT_CENTER
                            imageView?.setImageBitmap(BitmapFactory.decodeByteArray(this@apply, 0, this@apply.size))
                        }
                    } else {
                        throw NotApngException()
                    }
                }
            }


        }
        return this
    }

    /**
     * Load an APNG file and starts playing the animation.
     * @param byteArray ByteArray of the file
     * @param speed The speed
     * @throws NotApngException
     */
    @SuppressWarnings("WeakerAccess")
    fun load(byteArray: ByteArray, speed: Float? = null, apngAnimatorOptions: ApngAnimatorOptions? = null) : ApngAnimator {
        GlobalScope.launch {
            this@ApngAnimator.speed = speed
            if (isApng(byteArray)) {
                isApng = true
                this@ApngAnimator.speed = speed
                scaleType = apngAnimatorOptions?.scaleType
                // Download PNG
                APNGDisassembler.disassemble(byteArray).frames.also { frames ->
                    draw(frames).apply {
                        setupAnimationDrawableAndStart(this)
                    }
                }
            } else {
                if (loadNotApng) {
                    GlobalScope.launch(Dispatchers.Main) {
                        imageView?.scaleType = this@ApngAnimator.scaleType ?: ImageView.ScaleType.FIT_CENTER
                        imageView?.setImageBitmap(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size))
                    }
                } else {
                    throw NotApngException()
                }
            }
        }
        return this
    }

    /**
     * Load an APNG file
     * @param string Path of the file.
     * @param speed The speed
     * @throws NotApngException
     */
    fun load(string: String, speed : Float? = null, apngAnimatorOptions: ApngAnimatorOptions? = null) : ApngAnimator {
        GlobalScope.launch {
            this@ApngAnimator.speed = speed
            if (string.contains("http") || string.contains("https")) {
                val url = URL(string)
                loadUrl(url, speed, apngAnimatorOptions)
            } else if (File(string).exists()) {
                var pathToLoad = if (string.startsWith("content://")) string else "file://$string"
                pathToLoad = pathToLoad.replace("%", "%25").replace("#", "%23")
                val bytes = context?.contentResolver?.openInputStream(Uri.parse(pathToLoad))?.readBytes()
                bytes ?: throw Exception("File are empty")
                if (isApng(bytes)) {
                    load(bytes, speed, apngAnimatorOptions)
                } else {
                    if (loadNotApng) {
                        GlobalScope.launch(Dispatchers.Main) {
                            imageView?.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                        }
                    } else {
                        throw NotApngException()
                    }
                }
            } else if (string.startsWith("file:///android_asset/")) {
                val bytes = this@ApngAnimator.context?.assets?.open(string.replace("file:///android_asset/", ""))?.readBytes()
                bytes ?: throw Exception("File are empty")
                if (isApng(bytes)) {
                    load(bytes, speed, apngAnimatorOptions)
                } else {
                    if (loadNotApng) {
                        GlobalScope.launch(Dispatchers.Main) {
                            imageView?.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                        }
                    } else {
                        throw NotApngException()
                    }
                }
            }
        }
        return this
    }

    fun load(@RawRes res : Int, speed : Float? = null, apngAnimatorOptions: ApngAnimatorOptions? = null) : ApngAnimator {
        GlobalScope.launch {
            val byteArray = context?.resources?.openRawResource(res)?.readBytes() ?: byteArrayOf()
            this@ApngAnimator.speed = speed
            if (isApng(byteArray)) {
                isApng = true
                this@ApngAnimator.speed = speed
                scaleType = apngAnimatorOptions?.scaleType
                // Download PNG
                APNGDisassembler.disassemble(byteArray).frames.also { frames ->
                    draw(frames).apply {
                        setupAnimationDrawableAndStart(this)
                    }
                }
            } else {
                if (loadNotApng) {
                    GlobalScope.launch(Dispatchers.Main) {
                        imageView?.scaleType = this@ApngAnimator.scaleType ?: ImageView.ScaleType.FIT_CENTER
                        imageView?.setImageBitmap(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size))
                    }
                } else {
                    throw NotApngException()
                }
            }
        }
        return this
    }

    /**
     * Sets up the animation drawable and any required listeners. The animation will automatically start.
     */
    private fun setupAnimationDrawableAndStart(generatedFrame: ArrayList<Bitmap>) {
        GlobalScope.launch {
            anim = toAnimationDrawable(generatedFrame)
            activeAnimation = anim
            GlobalScope.launch(Dispatchers.Main) {
                imageView?.apply {
                    scaleType = this@ApngAnimator.scaleType ?: ImageView.ScaleType.FIT_CENTER
                    setImageDrawable(activeAnimation)
                }
                activeAnimation?.start()
                isPlaying = true
                doOnLoaded(this@ApngAnimator)
            }
        }
    }

    /**
     * Draw frames
     */
    fun draw(extractedFrame: ArrayList<Frame>) : ArrayList<Bitmap> {
        val generatedFrame = ArrayList<Bitmap>()
        // Set last frame
        duration = ArrayList()
        var bitmapBuffer = Bitmap.createBitmap(extractedFrame[0].maxWidth!!, extractedFrame[0].maxHeight!!, Bitmap.Config.ARGB_8888)
        for (i in 0 until extractedFrame.size) {
            // Iterator
            val it = extractedFrame[i]
            // Current bitmap for the frame
            val btm = Bitmap.createBitmap(extractedFrame[0].maxWidth!!, extractedFrame[0].maxHeight!!, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(btm)
            val current = BitmapFactory.decodeByteArray(it.byteArray, 0, it.byteArray.size).copy(Bitmap.Config.ARGB_8888, true)
            // Write buffer to canvas
            canvas.drawBitmap(bitmapBuffer, 0f, 0f, null)
            // Clear current frame rect
            // If `BlendOp` is APNG_BLEND_OP_SOURCE all color components of the frame, including alpha, overwrite the current contents of the frame's output buffer region.
            if (it.blendOp == Utils.Companion.BlendOp.APNG_BLEND_OP_SOURCE) {
                canvas.drawRect(it.xOffsets.toFloat(), it.yOffsets.toFloat(), it.xOffsets + current.width.toFloat(), it.yOffsets + current.height.toFloat(), { val paint = Paint(); paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR); paint }())
            }
            // Draw the bitmap
            canvas.drawBitmap(current, it.xOffsets.toFloat(), it.yOffsets.toFloat(), null)
            generatedFrame.add(btm)
            // Don't add current frame to bitmap buffer
            when {
                extractedFrame[i].disposeOp == Utils.Companion.DisposeOp.APNG_DISPOSE_OP_PREVIOUS -> {
                    //Do nothings
                }
                // Add current frame to bitmap buffer
                // APNG_DISPOSE_OP_BACKGROUND: the frame's region of the output buffer is to be cleared to fully transparent black before rendering the next frame.
                it.disposeOp == Utils.Companion.DisposeOp.APNG_DISPOSE_OP_BACKGROUND -> {
                    val res = Bitmap.createBitmap(extractedFrame[0].maxWidth!!, extractedFrame[0].maxHeight!!, Bitmap.Config.ARGB_8888)
                    val can = Canvas(res)
                    can.drawBitmap(btm, 0f, 0f, null)
                    can.drawRect(it.xOffsets.toFloat(), it.yOffsets.toFloat(), it.xOffsets + it.width.toFloat(), it.yOffsets + it.height.toFloat(), { val paint = Paint(); paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR); paint }())
                    bitmapBuffer = res
                }
                else -> bitmapBuffer = btm
            }
            duration?.add(it.delay / (speed ?: 1f))
        }
        return generatedFrame
    }

    /**
     * Pause the animation
     */
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
                    for (k in i until activeAnimation!!.numberOfFrames) {
                        val frame = activeAnimation!!.getFrame(k)
                        animResume.addFrame(frame, (duration!![k] / (speed ?: 1f)).toInt())
                        dura.add(duration!![k])
                    }
                    for (k in 0 until i) {
                        val frame = activeAnimation!!.getFrame(k)
                        animResume.addFrame(frame, (duration!![k] / (speed ?: 1f)).toInt())
                        dura.add(duration!![k])
                    }
                    activeAnimation = animResume
                    imageView?.setImageDrawable(activeAnimation)
                    activeAnimation?.setOnAnimationLoopListener(animationLoopListener)
                    imageView?.invalidate()
                    duration = dura
                    break@frameLoop
                }
            }
        }
    }

    /**
     * Play the animation
     */
    fun play() {
        if (isApng) {
            isPlaying = true
            activeAnimation?.start()
        }
    }

    /**
     * Set animation loop listener
     * @param animationLoopListener The animation loop listener.
     */
    fun setOnAnimationLoopListener(animationLoopListener : () -> Unit) {
        if (isApng) {
            this.animationLoopListener = animationLoopListener
            anim?.setOnAnimationLoopListener(animationLoopListener)
        }
    }

    /**
     * Execute on loaded
     */
    fun onLoaded(f : (ApngAnimator) -> Unit) {
        doOnLoaded = f
    }

    /**
     * Converts the generated frames into an animation drawable ([CustomAnimationDrawable])
     * in the APNG will be used instead.
     */
    private fun toAnimationDrawable( generatedFrame : ArrayList<Bitmap> ): CustomAnimationDrawable {
        if (isApng) {
            return CustomAnimationDrawable().apply {
                for (i in 0 until generatedFrame.size) {
                    addFrame(BitmapDrawable(generatedFrame[i]), ((duration!![i]) / (speed ?: 1f)).toInt())
                }
            }
        } else {
            throw NotApngException()
        }
    }
}