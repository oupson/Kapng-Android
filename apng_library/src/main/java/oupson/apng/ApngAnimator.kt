package oupson.apng

import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import androidx.annotation.RawRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import oupson.apng.exceptions.NotApngException
import oupson.apng.exceptions.NotPngException
import oupson.apng.utils.ApngAnimatorOptions
import oupson.apng.utils.Utils
import oupson.apng.utils.Utils.Companion.isApng
import oupson.apng.utils.Utils.Companion.isPng
import java.io.File
import java.net.URL

// TODO REWRITE WITH CALLBACKS
// TODO REWRITE

/**
 * Class to play APNG
 * For better performance but lesser features using [oupson.apng.decoder.ApngDecoder] is strongly recommended.
 */
@Deprecated("Deprecated, Use ApngEncoder and ApngDecoder instead", level = DeprecationLevel.WARNING)
class ApngAnimator(private val context: Context?) {
    companion object {
        /**
         * @param file The APNG to load
         * @param speed The speed of the APNG
         * @param apngAnimatorOptions Options of the animator
         * @return [ApngAnimator] The animator
         */
        @Suppress("unused")
        @JvmOverloads
        fun ImageView.loadApng(file: File, speed: Float? = null, apngAnimatorOptions: ApngAnimatorOptions? = null) = ApngAnimator(this.context).loadInto(this).apply {
            load(file, speed, apngAnimatorOptions)
        }

        /**
         * @param uri The APNG to load
         * @param speed The speed of the APNG
         * @param apngAnimatorOptions Options of the animator
         * @return [ApngAnimator] The animator
         */
        @Suppress("unused")
        @JvmOverloads
        fun ImageView.loadApng(uri : Uri, speed: Float? = null, apngAnimatorOptions: ApngAnimatorOptions? = null) = ApngAnimator(this.context).loadInto(this).apply {
            load(uri, speed, apngAnimatorOptions)
        }

        /**
         * @param url The url of the APNG to load
         * @param speed The speed of the APNG
         * @param apngAnimatorOptions Options of the animator
         * @return [ApngAnimator] The animator
         */
        @Suppress("unused")
        @JvmOverloads
        fun ImageView.loadApng(url: URL, speed: Float? = null, apngAnimatorOptions: ApngAnimatorOptions? = null) = ApngAnimator(this.context).loadInto(this).apply {
            loadUrl(url, speed, apngAnimatorOptions)
        }

        /**
         * @param byteArray The APNG to load
         * @param speed The speed of the APNG
         * @param apngAnimatorOptions Options of the animator
         * @return [ApngAnimator] The animator
         */
        @Suppress("unused")
        @JvmOverloads
        fun ImageView.loadApng(byteArray: ByteArray, speed: Float? = null, apngAnimatorOptions: ApngAnimatorOptions? = null) = ApngAnimator(this.context).loadInto(this).apply {
            load(byteArray, speed, apngAnimatorOptions)
        }

        /**
         * @param string The path APNG to load
         * @param speed The speed of the APNG
         * @param apngAnimatorOptions Options of the animator
         * @return [ApngAnimator] The animator
         */
        @Suppress("unused")
        @JvmOverloads
        fun ImageView.loadApng(string: String, speed : Float? = null, apngAnimatorOptions: ApngAnimatorOptions? = null) = ApngAnimator(this.context).loadInto(this).apply {
            load(string, speed, apngAnimatorOptions)
        }

        /**
         * @param res The Resource Int of the APNG to load, must be in the raw folder
         * @param speed The speed of the APNG
         * @param apngAnimatorOptions Options of the animator
         * @return [ApngAnimator] The animator
         */
        @Suppress("unused")
        @JvmOverloads
        fun ImageView.loadApng(@RawRes res : Int, speed : Float? = null, apngAnimatorOptions: ApngAnimatorOptions? = null) = ApngAnimator(this.context).loadInto(this).apply {
            load(res, speed, apngAnimatorOptions)
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    var isPlaying = true
        private set

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

    @Suppress("MemberVisibilityCanBePrivate")
    var anim: CustomAnimationDrawable? = null
    private var activeAnimation: CustomAnimationDrawable? = null

    private var doOnLoaded : (ApngAnimator) -> Unit = {}
    private var frameChangeLister: (index : Int) -> Unit? = {}

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
     * @param boolean If true the file will be loaded even if it is not an APNG
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
     * @return [ApngAnimator] The Animator
     */
    fun loadInto(imageView: ImageView): ApngAnimator {
        this.imageView = imageView
        return this
    }

    /**
     * Load an APNG file and starts playing the animation.
     * @param file The file to load
     * @param speed The speed
     * @return [ApngAnimator] The Animator
     * @throws NotApngException
     */
    @JvmOverloads
    fun load(file: File, speed: Float? = null, apngAnimatorOptions: ApngAnimatorOptions? = null) : ApngAnimator {
        GlobalScope.launch(Dispatchers.IO) {
            val input = file.inputStream()
            val bytes = ByteArray(8)
            input.read(bytes)
            input.close()
            if (isPng(bytes)) {
                isApng = true
                this@ApngAnimator.speed = speed
                scaleType = apngAnimatorOptions?.scaleType
                // Download PNG

                val inputStream = file.inputStream()
                APNGDisassembler().disassemble(inputStream).also {
                    inputStream.close()
                    if (it.isApng) {
                        it.frames.also {frames ->
                            draw(frames).apply {
                                setupAnimationDrawableAndStart(this)
                            }
                        }
                    } else {
                        GlobalScope.launch {
                            imageView?.setImageBitmap(it.cover)
                        }
                    }
                }
            } else {
                if (loadNotApng) {
                    GlobalScope.launch(Dispatchers.Main) {
                        imageView?.scaleType = this@ApngAnimator.scaleType ?: ImageView.ScaleType.FIT_CENTER
                        imageView?.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                    }
                } else {
                    throw NotPngException()
                }
            }
        }
        return this
    }

    /**
     * Load an APNG file and starts playing the animation.
     * @param uri The uri to load
     * @param speed The speed
     * @return [ApngAnimator] The Animator
     * @throws NotApngException
     */
    @JvmOverloads
    fun load(uri : Uri, speed: Float? = null, apngAnimatorOptions: ApngAnimatorOptions? = null) : ApngAnimator {
        GlobalScope.launch(Dispatchers.IO) {
            val input = context!!.contentResolver.openInputStream(uri)!!
            val bytes = ByteArray(8)
            input.read(bytes)
            input.close()
            if (isPng(bytes)) {
                this@ApngAnimator.speed = speed
                scaleType = apngAnimatorOptions?.scaleType
                // Download PNG

                val inputStream = context.contentResolver.openInputStream(uri)!!
                APNGDisassembler().disassemble(inputStream).also {
                    inputStream.close()
                    if (it.isApng) {
                        isApng = true
                        it.frames.also {frames ->
                            draw(frames).apply {
                                setupAnimationDrawableAndStart(this)
                            }
                        }
                    } else {
                        isApng = false
                        GlobalScope.launch(Dispatchers.Main) {
                            imageView?.setImageBitmap(it.cover)
                        }
                    }
                }
            } else {
                if (loadNotApng) {
                    GlobalScope.launch(Dispatchers.Main) {
                        imageView?.scaleType = this@ApngAnimator.scaleType ?: ImageView.ScaleType.FIT_CENTER
                        imageView?.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                    }
                } else {
                    throw NotPngException()
                }
            }
        }
        return this
    }

    /**
     * Load an APNG file and starts playing the animation.
     * @param url URL to load.
     * @param speed The speed
     * @return [ApngAnimator] The Animator
     * @throws NotApngException
     */
    @JvmOverloads
    fun loadUrl(url: URL, speed: Float? = null, apngAnimatorOptions: ApngAnimatorOptions? = null) : ApngAnimator {
        GlobalScope.launch(Dispatchers.IO) {
            this@ApngAnimator.speed = speed
            // Download PNG
            try {
                Loader.load(url).apply {
                    try {
                        this@ApngAnimator.load(this, speed, apngAnimatorOptions)
                    } catch (e: NotPngException) {
                        if (loadNotApng) {
                            GlobalScope.launch(Dispatchers.Main) {
                                imageView?.scaleType =
                                    this@ApngAnimator.scaleType ?: ImageView.ScaleType.FIT_CENTER
                                imageView?.setImageBitmap(
                                    BitmapFactory.decodeByteArray(
                                        this@apply,
                                        0,
                                        this@apply.size
                                    )
                                )
                            }
                        } else {
                            throw NotApngException()
                        }
                    }
                }
            } catch (e : java.lang.Exception) {
                if (BuildConfig.DEBUG)
                    Log.e("ApngAnimator", "Error : $e")
            }
        }
        return this
    }

    /**
     * Load an APNG file and starts playing the animation.
     * @param byteArray ByteArray of the file
     * @param speed The speed
     * @return [ApngAnimator] The Animator
     * @throws NotApngException
     */
    @JvmOverloads
    fun load(byteArray: ByteArray, speed: Float? = null, apngAnimatorOptions: ApngAnimatorOptions? = null) : ApngAnimator {
        GlobalScope.launch {
            this@ApngAnimator.speed = speed
            if (isApng(byteArray)) {
                isApng = true
                this@ApngAnimator.speed = speed
                scaleType = apngAnimatorOptions?.scaleType
                // Download PNG
                APNGDisassembler().disassemble(byteArray).frames.also { frames ->
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
     * @return [ApngAnimator] The Animator
     * @throws NotApngException
     */
    @JvmOverloads
    fun load(string: String, speed : Float? = null, apngAnimatorOptions: ApngAnimatorOptions? = null) : ApngAnimator {
        GlobalScope.launch(Dispatchers.IO) {
            this@ApngAnimator.speed = speed
            if (string.contains("http") || string.contains("https")) {
                val url = URL(string)
                loadUrl(url, speed, apngAnimatorOptions)
            } else if (File(string).exists()) {
                var pathToLoad = if (string.startsWith("content://")) string else "file://$string"
                pathToLoad = pathToLoad.replace("%", "%25").replace("#", "%23")
                this@ApngAnimator.load(Uri.parse(pathToLoad), speed, apngAnimatorOptions)
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
    /**
     * Load an APNG file
     * @param res The res of the file
     * @param speed The speed
     * @return [ApngAnimator] The Animator
     * @throws NotApngException
     */
    @JvmOverloads
    fun load(@RawRes res : Int, speed : Float? = null, apngAnimatorOptions: ApngAnimatorOptions? = null) : ApngAnimator {
        GlobalScope.launch {
            val byteArray = context?.resources?.openRawResource(res)?.readBytes() ?: byteArrayOf()
            this@ApngAnimator.speed = speed
            if (isApng(byteArray)) {
                isApng = true
                this@ApngAnimator.speed = speed
                scaleType = apngAnimatorOptions?.scaleType
                // Download PNG
                APNGDisassembler().disassemble(byteArray).frames.also { frames ->
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
     * @param generatedFrame The frames generated by draw function
     */
    private fun setupAnimationDrawableAndStart(generatedFrame: ArrayList<Bitmap>) {
        GlobalScope.launch {
            anim = toAnimationDrawable(generatedFrame)
            activeAnimation = anim
            GlobalScope.launch(Dispatchers.Main) {
                imageView?.apply {
                    scaleType = this@ApngAnimator.scaleType ?: ImageView.ScaleType.FIT_CENTER
                    clearAnimation()
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
     * @param extractedFrame The frames extracted by the disassembler
     * @return [ArrayList] The drawed frames
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
            val durations = ArrayList<Float>()
            frameLoop@ for (i in 0 until anim?.numberOfFrames!!) {
                val checkFrame = activeAnimation!!.getFrame(i)
                if (checkFrame === currentFrame) {
                    for (k in i until activeAnimation!!.numberOfFrames) {
                        val frame = activeAnimation!!.getFrame(k)
                        animResume.addFrame(frame, (duration!![k] / (speed ?: 1f)).toInt())
                        durations.add(duration!![k])
                    }
                    for (k in 0 until i) {
                        val frame = activeAnimation!!.getFrame(k)
                        animResume.addFrame(frame, (duration!![k] / (speed ?: 1f)).toInt())
                        durations.add(duration!![k])
                    }
                    activeAnimation = animResume
                    imageView?.setImageDrawable(activeAnimation)
                    activeAnimation?.setOnFrameChangeListener(frameChangeLister)
                    imageView?.invalidate()
                    duration = durations
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
     * @param frameChangeListener The listener.
     */
    @Suppress("unused")
    fun setOnFrameChangeLister(frameChangeListener : (index : Int) -> Unit?) {
        if (isApng) {
            this.frameChangeLister = frameChangeListener
            anim?.setOnFrameChangeListener(frameChangeListener)
        }
    }

    /**
     * Execute on loaded
     */
    @Suppress("unused")
    fun onLoaded(f : (ApngAnimator) -> Unit) {
        doOnLoaded = f
    }

    /**
     * Converts the generated frames into an animation drawable ([CustomAnimationDrawable])
     * in the APNG will be used instead.
     * @param generatedFrame The frames
     * @return [CustomAnimationDrawable] The animation drawable
     */
    private fun toAnimationDrawable( generatedFrame : ArrayList<Bitmap> ): CustomAnimationDrawable {
        if (isApng) {
            return CustomAnimationDrawable().apply {
                isOneShot = false
                for (i in 0 until generatedFrame.size) {
                    addFrame(BitmapDrawable(generatedFrame[i]), ((duration!![i]) / (speed ?: 1f)).toInt())
                }
            }
        } else {
            throw NotApngException()
        }
    }
}