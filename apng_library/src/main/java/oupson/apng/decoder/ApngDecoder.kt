package oupson.apng.decoder

import android.content.Context
import android.graphics.*
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.ImageView
import androidx.annotation.RawRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import oupson.apng.BuildConfig
import oupson.apng.decoder.ApngDecoder.Companion.decodeApng
import oupson.apng.drawable.ApngDrawable
import oupson.apng.exceptions.BadApngException
import oupson.apng.exceptions.BadCRCException
import oupson.apng.utils.Loader
import oupson.apng.utils.Utils
import oupson.apng.utils.Utils.Companion.isPng
import java.io.*
import java.net.URL
import java.nio.ByteBuffer
import java.util.zip.CRC32

/**
 * An APNG Decoder.
 * Call [decodeApng]
 */
class ApngDecoder {
    interface Callback {
        /**
         * Function called when the file was successfully decoded.
         * @param drawable Can be an [AnimationDrawable] if successful and an [AnimatedImageDrawable] if the image decoded is not an APNG but a gif. If it is not an animated image, it is a [Drawable].
         */
        fun onSuccess(drawable: Drawable)

        /**
         * Function called when something gone wrong.
         * @param error The problem.
         */
        fun onError(error: Exception)
    }

    data class Config(
        val speed: Float = 1f,
        val bitmapConfig: Bitmap.Config = Bitmap.Config.ARGB_8888,
        val decodeCoverFrame: Boolean = true
    )

    companion object {
        private const val TAG = "ApngDecoder"
        private val zeroLength = byteArrayOf(0x00, 0x00, 0x00, 0x00)

        // Paint used to clear the buffer
        private val clearPaint: Paint by lazy {
            Paint().apply {
                xfermode = PorterDuffXfermode(
                    PorterDuff.Mode.CLEAR
                )
            }
        }

        /**
         * Decode Apng and return a Drawable who can be an [AnimationDrawable] if it end successfully. Can also be an [android.graphics.drawable.AnimatedImageDrawable].
         * @param context Context needed for the animation drawable
         * @param inStream Input Stream to decode. Will be closed at the end.
         * @param speed Optional parameter.
         * @param config Configuration applied to the bitmap added to the animation. Please note that the frame is decoded in ARGB_8888 and converted after, for the buffer.
         * @return [AnimationDrawable] if successful and an [AnimatedImageDrawable] if the image decoded is not an APNG but a gif. If it is not an animated image, it is a [Drawable].
         */
        // TODO DOCUMENT CONFIG
        @Suppress("MemberVisibilityCanBePrivate")
        @JvmStatic
        @JvmOverloads
        fun decodeApng(
            context: Context,
            inStream: InputStream,
            config: Config = Config()
        ): Drawable {
            val inputStream = BufferedInputStream(inStream)
            val bytes = ByteArray(8)
            inputStream.mark(8)
            inputStream.read(bytes)
            if (isPng(bytes)) {
                var png: ByteArrayOutputStream? = null
                var cover: ByteArrayOutputStream? = null
                var delay = -1f
                var yOffset = -1
                var xOffset = -1
                var plte: ByteArray? = null
                var tnrs: ByteArray? = null
                var maxWidth = 0
                var maxHeight = 0
                var blendOp: Utils.Companion.BlendOp = Utils.Companion.BlendOp.APNG_BLEND_OP_SOURCE
                var disposeOp: Utils.Companion.DisposeOp =
                    Utils.Companion.DisposeOp.APNG_DISPOSE_OP_NONE

                var ihdrOfApng = ByteArray(0)

                var isApng = false

                val drawable = ApngDrawable().apply {
                    isOneShot = false
                }

                var buffer: Bitmap? = null

                var byteRead: Int
                val lengthChunk = ByteArray(4)
                do {
                    byteRead = inputStream.read(lengthChunk)

                    if (byteRead == -1)
                        break
                    val length = Utils.uIntFromBytesBigEndian(lengthChunk)

                    val chunk = ByteArray(length + 8)
                    byteRead = inputStream.read(chunk)

                    val byteArray = lengthChunk.plus(chunk)
                    val chunkCRC = Utils.uIntFromBytesBigEndian(byteArray, byteArray.size - 4)
                    val crc = CRC32()
                    crc.update(byteArray, 4, byteArray.size - 8)
                    if (chunkCRC == crc.value.toInt()) {
                        val name = byteArray.copyOfRange(4, 8)
                        when {
                            name.contentEquals(Utils.fcTL) -> {
                                if (png == null) {
                                    if (config.decodeCoverFrame) {
                                        drawable.coverFrame = cover?.let {
                                            it.write(zeroLength)
                                            // Generate crc for IEND
                                            val crC32 = CRC32()
                                            crC32.update(Utils.IEND, 0, Utils.IEND.size)
                                            it.write(Utils.IEND)
                                            it.write(Utils.uIntToByteArray(crC32.value.toInt()))

                                            val pngBytes = it.toByteArray()
                                            BitmapFactory.decodeByteArray(
                                                pngBytes,
                                                0,
                                                pngBytes.size
                                            )
                                        }
                                    }
                                    cover = null
                                } else {
                                    // Add IEND body length : 0
                                    png.write(zeroLength)
                                    // Add IEND
                                    // Generate crc for IEND
                                    val crC32 = CRC32()
                                    crC32.update(Utils.IEND, 0, Utils.IEND.size)
                                    png.write(Utils.IEND)
                                    png.write(Utils.uIntToByteArray(crC32.value.toInt()))

                                    val btm = Bitmap.createBitmap(
                                        maxWidth,
                                        maxHeight,
                                        Bitmap.Config.ARGB_8888
                                    )

                                    val pngBytes = png.toByteArray()
                                    val decoded = BitmapFactory.decodeByteArray(
                                        pngBytes,
                                        0,
                                        pngBytes.size
                                    )
                                    val canvas = Canvas(btm)
                                    canvas.drawBitmap(buffer!!, 0f, 0f, null)

                                    if (blendOp == Utils.Companion.BlendOp.APNG_BLEND_OP_SOURCE) {
                                        canvas.drawRect(
                                            xOffset.toFloat(),
                                            yOffset.toFloat(),
                                            xOffset + decoded.width.toFloat(),
                                            yOffset + decoded.height.toFloat(),
                                            clearPaint
                                        )
                                    }

                                    canvas.drawBitmap(
                                        decoded,
                                        xOffset.toFloat(),
                                        yOffset.toFloat(),
                                        null
                                    )

                                    drawable.addFrame(
                                        BitmapDrawable(
                                            context.resources,
                                            if (btm.config != config.bitmapConfig) {
                                                if (BuildConfig.DEBUG)
                                                    Log.v(
                                                        TAG,
                                                        "Bitmap Config : ${btm.config}, Config : $config"
                                                    )
                                                btm.copy(config.bitmapConfig, btm.isMutable)
                                            } else {
                                                btm
                                            }
                                        ),
                                        (delay / config.speed).toInt()
                                    )

                                    when (disposeOp) {
                                        Utils.Companion.DisposeOp.APNG_DISPOSE_OP_PREVIOUS -> {
                                            //Do nothings
                                        }
                                        // Add current frame to bitmap buffer
                                        // APNG_DISPOSE_OP_BACKGROUND: the frame's region of the output buffer is to be cleared to fully transparent black before rendering the next frame.
                                        Utils.Companion.DisposeOp.APNG_DISPOSE_OP_BACKGROUND -> {
                                            val res = Bitmap.createBitmap(
                                                maxWidth,
                                                maxHeight,
                                                Bitmap.Config.ARGB_8888
                                            )
                                            val can = Canvas(res)
                                            can.drawBitmap(btm, 0f, 0f, null)
                                            can.drawRect(
                                                xOffset.toFloat(),
                                                yOffset.toFloat(),
                                                xOffset + decoded.width.toFloat(),
                                                yOffset + decoded.height.toFloat(),
                                                clearPaint
                                            )
                                            buffer = res
                                        }
                                        else -> buffer = btm
                                    }

                                }

                                png = ByteArrayOutputStream(4096)

                                // Parse Frame ConTroL chunk
                                // Get the width of the png
                                val width = Utils.uIntFromBytesBigEndian(
                                    byteArray, 12
                                )
                                // Get the height of the png
                                val height = Utils.uIntFromBytesBigEndian(
                                    byteArray, 16
                                )

                                /*
                                 * The `delay_num` and `delay_den` parameters together specify a fraction indicating the time to display the current frame, in seconds.
                                 * If the the value of the numerator is 0 the decoder should render the next frame as quickly as possible, though viewers may impose a reasonable lower bound.
                                 */
                                // Get delay numerator
                                val delayNum = Utils.uShortFromBytesBigEndian(
                                    byteArray, 28
                                ).toFloat()
                                // Get delay denominator
                                var delayDen = Utils.uShortFromBytesBigEndian(
                                    byteArray, 30
                                ).toFloat()

                                // If the denominator is 0, it is to be treated as if it were 100 (that is, `delay_num` then specifies 1/100ths of a second).
                                if (delayDen == 0f) {
                                    delayDen = 100f
                                }

                                delay = (delayNum / delayDen * 1000)

                                // Get x and y offsets
                                xOffset = Utils.uIntFromBytesBigEndian(
                                    byteArray, 20
                                )
                                yOffset = Utils.uIntFromBytesBigEndian(
                                    byteArray, 24
                                )
                                blendOp = Utils.decodeBlendOp(byteArray[33].toInt())
                                disposeOp = Utils.decodeDisposeOp(byteArray[32].toInt())

                                if (xOffset + width > maxWidth) {
                                    throw BadApngException("`xOffset` + `width` must be <= `IHDR` width")
                                } else if (yOffset + height > maxHeight) {
                                    throw BadApngException("`yOffset` + `height` must be <= `IHDR` height")
                                }

                                png.write(Utils.pngSignature)
                                png.write(
                                    generateIhdr(
                                        ihdrOfApng,
                                        width,
                                        height
                                    )
                                )
                                plte?.let {
                                    png.write(it)
                                }
                                tnrs?.let {
                                    png.write(it)
                                }

                            }
                            name.contentEquals(Utils.IEND) -> {
                                if (isApng && png != null) {
                                    png.write(zeroLength)
                                    // Add IEND
                                    // Generate crc for IEND
                                    val crC32 = CRC32()
                                    crC32.update(Utils.IEND, 0, Utils.IEND.size)
                                    png.write(Utils.IEND)
                                    png.write(Utils.uIntToByteArray(crC32.value.toInt()))

                                    val btm = Bitmap.createBitmap(
                                        maxWidth,
                                        maxHeight,
                                        Bitmap.Config.ARGB_8888
                                    )

                                    val pngBytes = png.toByteArray()
                                    val decoded = BitmapFactory.decodeByteArray(
                                        pngBytes,
                                        0,
                                        pngBytes.size
                                    )
                                    val canvas = Canvas(btm)
                                    canvas.drawBitmap(buffer!!, 0f, 0f, null)

                                    if (blendOp == Utils.Companion.BlendOp.APNG_BLEND_OP_SOURCE) {
                                        canvas.drawRect(
                                            xOffset.toFloat(),
                                            yOffset.toFloat(),
                                            xOffset + decoded.width.toFloat(),
                                            yOffset + decoded.height.toFloat(),
                                            clearPaint
                                        )
                                    }

                                    canvas.drawBitmap(
                                        decoded,
                                        xOffset.toFloat(),
                                        yOffset.toFloat(),
                                        null
                                    )
                                    drawable.addFrame(
                                        BitmapDrawable(
                                            context.resources,
                                            if (btm.config != config.bitmapConfig) {
                                                if (BuildConfig.DEBUG)
                                                    Log.v(
                                                        TAG,
                                                        "Bitmap Config : ${btm.config}, Config : $config"
                                                    )
                                                btm.copy(config.bitmapConfig, btm.isMutable)
                                            } else {
                                                btm
                                            }
                                        ),
                                        (delay / config.speed).toInt()
                                    )

                                    when (disposeOp) {
                                        Utils.Companion.DisposeOp.APNG_DISPOSE_OP_PREVIOUS -> {
                                            //Do nothings
                                        }
                                        // Add current frame to bitmap buffer
                                        // APNG_DISPOSE_OP_BACKGROUND: the frame's region of the output buffer is to be cleared to fully transparent black before rendering the next frame.
                                        Utils.Companion.DisposeOp.APNG_DISPOSE_OP_BACKGROUND -> {
                                            val res = Bitmap.createBitmap(
                                                maxWidth,
                                                maxHeight,
                                                Bitmap.Config.ARGB_8888
                                            )
                                            val can = Canvas(res)
                                            can.drawBitmap(btm, 0f, 0f, null)
                                            can.drawRect(
                                                xOffset.toFloat(),
                                                yOffset.toFloat(),
                                                xOffset + decoded.width.toFloat(),
                                                yOffset + decoded.height.toFloat(),
                                                clearPaint
                                            )
                                            buffer = res
                                        }
                                        else -> buffer = btm
                                    }
                                } else {
                                    cover?.let {
                                        it.write(zeroLength)
                                        // Add IEND
                                        // Generate crc for IEND
                                        val crC32 = CRC32()
                                        crC32.update(Utils.IEND, 0, Utils.IEND.size)
                                        it.write(Utils.IEND)
                                        it.write(Utils.uIntToByteArray(crC32.value.toInt()))
                                        inputStream.close()

                                        val pngBytes = it.toByteArray()
                                        return BitmapDrawable(
                                            context.resources,
                                            BitmapFactory.decodeByteArray(
                                                pngBytes,
                                                0,
                                                pngBytes.size
                                            )
                                        )
                                    }
                                }
                            }
                            name.contentEquals(Utils.IDAT) -> {
                                val w = if (png == null) {
                                    if (cover == null) {
                                        cover = ByteArrayOutputStream()
                                        cover.write(Utils.pngSignature)
                                        cover.write(
                                            generateIhdr(
                                                ihdrOfApng,
                                                maxWidth,
                                                maxHeight
                                            )
                                        )
                                    }
                                    cover
                                } else {
                                    png
                                }

                                // Find the chunk length
                                val bodySize =
                                    Utils.uIntFromBytesBigEndian(
                                        byteArray, 0
                                    )
                                w.write(byteArray.copyOfRange(0, 4))

                                val body = ByteArray(4 + bodySize)

                                System.arraycopy(Utils.IDAT, 0, body, 0, 4)

                                // Get image bytes
                                System.arraycopy(byteArray, 8, body, 4, bodySize)

                                val crC32 = CRC32()
                                crC32.update(body, 0, body.size)
                                w.write(body)
                                w.write(Utils.uIntToByteArray(crC32.value.toInt()))
                            }
                            name.contentEquals(Utils.fdAT) -> {
                                // Find the chunk length
                                val bodySize = Utils.uIntFromBytesBigEndian(byteArray, 0)
                                png?.write(Utils.uIntToByteArray(bodySize - 4))

                                val body = ByteArray(bodySize)
                                System.arraycopy(Utils.IDAT, 0, body, 0, 4)

                                // Get image bytes
                                System.arraycopy(byteArray, 12, body, 4, bodySize - 4)

                                val crC32 = CRC32()
                                crC32.update(body, 0, body.size)
                                png?.write(body)
                                png?.write(Utils.uIntToByteArray(crC32.value.toInt()))
                            }
                            name.contentEquals(Utils.plte) -> {
                                plte = byteArray
                            }
                            name.contentEquals(Utils.tnrs) -> {
                                tnrs = byteArray
                            }
                            name.contentEquals(Utils.IHDR) -> {
                                // Get length of the body of the chunk
                                val bodySize = Utils.uIntFromBytesBigEndian(byteArray, 0)
                                // Get the width of the png
                                maxWidth = Utils.uIntFromBytesBigEndian(byteArray, 8)
                                // Get the height of the png
                                maxHeight = Utils.uIntFromBytesBigEndian(byteArray, 12)
                                ihdrOfApng = byteArray.copyOfRange(4 + 4, 4 + bodySize + 4)

                                buffer = Bitmap.createBitmap(
                                    maxWidth,
                                    maxHeight,
                                    Bitmap.Config.ARGB_8888
                                )
                            }
                            name.contentEquals(Utils.acTL) -> { // TODO GET NBR REPETITIONS
                                isApng = true
                            }
                        }
                    } else throw BadCRCException()
                } while (byteRead != -1)
                inputStream.close()
                return drawable
            } else {
                if (BuildConfig.DEBUG)
                    Log.i(TAG, "Decoding non APNG stream")
                inputStream.reset()

                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val bytesRead = inputStream.readBytes()
                    inputStream.close()
                    val buf = ByteBuffer.wrap(bytesRead)
                    val source = ImageDecoder.createSource(buf)
                    ImageDecoder.decodeDrawable(source)
                } else {
                    val drawable = Drawable.createFromStream(
                        inputStream,
                        null
                    )
                    inputStream.close()
                    drawable
                }
            }
        }

        /**
         * Decode Apng and return a Drawable who can be an [AnimationDrawable] if it end successfully. Can also be an [android.graphics.drawable.AnimatedImageDrawable].
         * @param context Context needed for animation drawable.
         * @param file File to decode.
         * @param speed Optional parameter.
         * @param config Configuration applied to the bitmap added to the animation. Please note that the frame is decoded in ARGB_8888 and converted after, for the buffer.
         * @return [AnimationDrawable] if successful and an [AnimatedImageDrawable] if the image decoded is not an APNG but a gif. If it is not an animated image, it is a [Drawable].
         */
        @Suppress("unused")
        @JvmStatic
        // TODO DOCUMENT
        fun decodeApng(
            context: Context,
            file: File,
            config: Config = Config()
        ): Drawable =
            decodeApng(
                context,
                FileInputStream(file), config
            )

        /**
         * Decode Apng and return a Drawable who can be an [AnimationDrawable] if it end successfully. Can also be an [android.graphics.drawable.AnimatedImageDrawable].
         * @param context Context is needed for contentResolver and animation drawable.
         * @param uri Uri to open.
         * @param speed Optional parameter.
         * @param config Configuration applied to the bitmap added to the animation. Please note that the frame is decoded in ARGB_8888 and converted after, for the buffer.
         * @return [AnimationDrawable] if successful and an [AnimatedImageDrawable] if the image decoded is not an APNG but a gif.
         */
        @Suppress("unused")
        @JvmStatic
        fun decodeApng(
            context: Context,
            uri: Uri,
            config: Config = Config()
        ): Drawable {
            val inputStream = context.contentResolver.openInputStream(uri)!!
            return decodeApng(
                context,
                inputStream,
                config
            )
        }

        /**
         * Decode Apng and return a Drawable who can be an [AnimationDrawable] if it end successfully. Can also be an [android.graphics.drawable.AnimatedImageDrawable].
         * @param context Context is needed for contentResolver and animation drawable.
         * @param res Resource to decode.
         * @param speed Optional parameter.
         * @param config Configuration applied to the bitmap added to the animation. Please note that the frame is decoded in ARGB_8888 and converted after, for the buffer.
         * @return [AnimationDrawable] if successful and an [AnimatedImageDrawable] if the image decoded is not an APNG but a gif.
         */
        @Suppress("unused")
        @JvmStatic
        fun decodeApng(
            context: Context,
            @RawRes res: Int,
            config: Config = Config()
        ): Drawable =
            decodeApng(
                context,
                context.resources.openRawResource(res),
                config
            )

        /**
         * Decode Apng and return a Drawable who can be an [AnimationDrawable] if it end successfully. Can also be an [android.graphics.drawable.AnimatedImageDrawable].
         * @param context Context is needed for contentResolver and animation drawable.
         * @param url URL to decode.
         * @param speed Optional parameter.
         * @param config Configuration applied to the bitmap added to the animation. Please note that the frame is decoded in ARGB_8888 and converted after, for the buffer.
         * @return [AnimationDrawable] if successful and an [AnimatedImageDrawable] if the image decoded is not an APNG but a gif.
         */
        @Suppress("unused", "BlockingMethodInNonBlockingContext")
        @JvmStatic
        suspend fun decodeApng(
            context: Context,
            url: URL,
            config: Config = Config()
        ) =
            withContext(Dispatchers.IO) {
                decodeApng(
                    context,
                    ByteArrayInputStream(Loader.load(url)),
                    config
                )
            }

        /**
         * Load Apng into an imageView, asynchronously.
         * @param context Context needed for animation drawable.
         * @param file File to decode.
         * @param imageView Image View.
         * @param speed Optional parameter.
         * @param callback [ApngDecoder.Callback] to handle success and error.
         * @param config Configuration applied to the bitmap added to the animation. Please note that the frame is decoded in ARGB_8888 and converted after, for the buffer.
         */
        @Suppress("unused")
        @JvmStatic
        @JvmOverloads
        fun decodeApngAsyncInto(
            context: Context,
            file: File,
            imageView: ImageView,
            callback: Callback? = null,
            config: Config = Config()
        ) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val drawable =
                        decodeApng(
                            context,
                            FileInputStream(file),
                            config
                        )
                    withContext(Dispatchers.Main) {
                        imageView.setImageDrawable(drawable)
                        (drawable as? AnimationDrawable)?.start()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            (drawable as? AnimatedImageDrawable)?.start()
                        }
                        callback?.onSuccess(drawable)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        callback?.onError(e)
                    }
                }
            }
        }

        /**
         * Load Apng into an imageView, asynchronously.
         * @param context Context needed for animation drawable and content resolver.
         * @param uri Uri to load.
         * @param imageView Image View.
         * @param speed Optional parameter.
         * @param callback [ApngDecoder.Callback] to handle success and error.
         * @param config Configuration applied to the bitmap added to the animation. Please note that the frame is decoded in ARGB_8888 and converted after, for the buffer.
         */
        @Suppress("unused")
        @JvmStatic
        @JvmOverloads
        fun decodeApngAsyncInto(
            context: Context,
            uri: Uri,
            imageView: ImageView,
            callback: Callback? = null,
            config: Config = Config()
        ) {
            val inputStream = context.contentResolver.openInputStream(uri)!!
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val drawable =
                        decodeApng(
                            context,
                            inputStream,
                            config
                        )
                    withContext(Dispatchers.Main) {
                        imageView.setImageDrawable(drawable)
                        (drawable as? AnimationDrawable)?.start()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            (drawable as? AnimatedImageDrawable)?.start()
                        }
                        callback?.onSuccess(drawable)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        callback?.onError(e)
                    }
                }
            }
        }

        /**
         * Load Apng into an imageView, asynchronously.
         * @param context Context needed to decode the resource and for the animation drawable.
         * @param res Raw resource to load.
         * @param imageView Image View.
         * @param speed Optional parameter.
         * @param callback [ApngDecoder.Callback] to handle success and error.
         * @param config Configuration applied to the bitmap added to the animation. Please note that the frame is decoded in ARGB_8888 and converted after, for the buffer.
         */
        @Suppress("unused")
        @JvmStatic
        @JvmOverloads
        fun decodeApngAsyncInto(
            context: Context, @RawRes res: Int,
            imageView: ImageView,
            callback: Callback? = null,
            config: Config = Config()
        ) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val drawable =
                        decodeApng(
                            context,
                            context.resources.openRawResource(res),
                            config
                        )
                    withContext(Dispatchers.Main) {
                        imageView.setImageDrawable(drawable)
                        (drawable as? AnimationDrawable)?.start()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            (drawable as? AnimatedImageDrawable)?.start()
                        }
                        callback?.onSuccess(drawable)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        callback?.onError(e)
                    }
                }
            }

        }

        /**
         * Load Apng into an imageView, asynchronously.
         * @param context Context needed for the animation drawable.
         * @param url URL to load.
         * @param imageView Image View.
         * @param speed Optional parameter.
         * @param callback [ApngDecoder.Callback] to handle success and error.
         * @param config Configuration applied to the bitmap added to the animation. Please note that the frame is decoded in ARGB_8888 and converted after, for the buffer.
         */
        @Suppress("unused")
        @JvmStatic
        @JvmOverloads
        fun decodeApngAsyncInto(
            context: Context,
            url: URL,
            imageView: ImageView,
            callback: Callback? = null,
            config: Config = Config()
        ) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val drawable = decodeApng(
                        context,
                        ByteArrayInputStream(
                            Loader.load(
                                url
                            )
                        ),
                        config
                    )
                    withContext(Dispatchers.Main) {
                        imageView.setImageDrawable(drawable)
                        (drawable as? AnimationDrawable)?.start()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            (drawable as? AnimatedImageDrawable)?.start()
                        }
                        callback?.onSuccess(drawable)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        callback?.onError(e)
                    }
                }
            }
        }

        /**
         * Load Apng into an imageView, asynchronously.
         * @param context Context needed for decoding the image and creating the animation drawable.
         * @param string URL to load
         * @param imageView Image View.
         * @param speed Optional parameter.
         * @param callback [ApngDecoder.Callback] to handle success and error.
         * @param config Configuration applied to the bitmap added to the animation. Please note that the frame is decoded in ARGB_8888 and converted after, for the buffer.
         */
        @Suppress("unused")
        @JvmStatic
        @JvmOverloads
        fun decodeApngAsyncInto(
            context: Context,
            string: String,
            imageView: ImageView,
            callback: Callback? = null,
            config: Config = Config()
        ) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    if (string.startsWith("http://") || string.startsWith("https://")) {
                        decodeApngAsyncInto(
                            context,
                            URL(string),
                            imageView,
                            callback,
                            config
                        )
                    } else if (File(string).exists()) {
                        var pathToLoad =
                            if (string.startsWith("content://")) string else "file://$string"
                        pathToLoad = pathToLoad.replace("%", "%25").replace("#", "%23")
                        decodeApngAsyncInto(
                            context,
                            Uri.parse(pathToLoad),
                            imageView,
                            callback,
                            config
                        )
                    } else if (string.startsWith("file://android_asset/")) {
                        val drawable =
                            decodeApng(
                                context,
                                context.assets.open(string.replace("file:///android_asset/", "")),

                                config
                            )
                        withContext(Dispatchers.Main) {
                            imageView.setImageDrawable(drawable)
                            (drawable as? AnimationDrawable)?.start()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                (drawable as? AnimatedImageDrawable)?.start()
                            }
                            callback?.onSuccess(drawable)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            callback?.onError(java.lang.Exception("Cannot open string"))
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        callback?.onError(e)
                    }
                }
            }
        }

        /**
         * Generate a correct IHDR from the IHDR chunk of the APNG.
         * @param ihdrOfApng The IHDR of the APNG.
         * @param width The width of the frame.
         * @param height The height of the frame.
         * @return [ByteArray] The generated IHDR.
         */
        private fun generateIhdr(ihdrOfApng: ByteArray, width: Int, height: Int): ByteArray {
            val ihdr =
                ByteArray(0xD + 4 + 4 + 4) // 0xD (IHDR body length) + 4 (0x0, 0x0, 0x0, 0xD : the chunk length) + 4 : IHDR + 4 : CRC

            // Add chunk body length
            System.arraycopy(Utils.uIntToByteArray(0xD), 0, ihdr, 0, 4)

            // We need a body var to know body length and generate crc
            val ihdrBody = ByteArray(0xD + 4) // 0xD (IHDR body length) + 4 : IHDR

            // Add IHDR
            System.arraycopy(Utils.IHDR, 0, ihdrBody, 0, 4)

            // Add the max width and height
            System.arraycopy(Utils.uIntToByteArray(width), 0, ihdrBody, 4, 4)
            System.arraycopy(Utils.uIntToByteArray(height), 0, ihdrBody, 8, 4)

            // Add complicated stuff like depth color ...
            // If you want correct png you need same parameters.
            System.arraycopy(ihdrOfApng, 8, ihdrBody, 12, 5)

            // Generate CRC
            val crC32 = CRC32()
            crC32.update(ihdrBody, 0, 0xD + 4)

            System.arraycopy(ihdrBody, 0, ihdr, 4, 0xD + 4)
            System.arraycopy(Utils.uIntToByteArray(crC32.value.toInt()), 0, ihdr, 0xD + 4 + 4, 4)
            return ihdr
        }
    }
}