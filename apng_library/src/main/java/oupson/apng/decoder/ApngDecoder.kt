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
import oupson.apng.APNGDisassembler
import oupson.apng.BuildConfig
import oupson.apng.Loader
import oupson.apng.chunks.IHDR
import oupson.apng.chunks.fcTL
import oupson.apng.exceptions.BadApng
import oupson.apng.exceptions.BadCRC
import oupson.apng.utils.Utils
import oupson.apng.utils.Utils.Companion.isPng
import java.io.*
import java.net.URL
import java.nio.ByteBuffer
import java.util.zip.CRC32

class ApngDecoder {
    interface Callback {
        fun onSuccess(drawable: Drawable)
        fun onError(error: java.lang.Exception)
    }

    companion object {
        private const val TAG = "ApngDecoder"

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
         * @param inStream Input Stream to decode. Will be close at the end.
         * @param speed Optional parameter.
         * @param config Configuration applied to the bitmap added to the animation. Please note that the frame is decoded in ARGB_8888 and converted after, for the buffer.
         */
        @Suppress("MemberVisibilityCanBePrivate")
        @JvmStatic
        @JvmOverloads
        fun decodeApng(context: Context, inStream: InputStream, speed: Float = 1f, config : Bitmap.Config = Bitmap.Config.ARGB_8888): Drawable {
            val inputStream = BufferedInputStream(inStream)
            val bytes = ByteArray(8)
            inputStream.mark(8)
            inputStream.read(bytes)
            if (isPng(bytes)) {
                var png: ArrayList<Byte>? = null
                var cover: ArrayList<Byte>? = null
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
                val ihdr = IHDR()
                var isApng = false

                val drawable = AnimationDrawable().apply {
                    isOneShot = false
                }

                var buffer: Bitmap? = null

                var byteRead: Int
                val lengthChunk = ByteArray(4)
                do {
                    byteRead = inputStream.read(lengthChunk)

                    if (byteRead == -1)
                        break
                    val length = Utils.parseLength(lengthChunk)

                    val chunk = ByteArray(length + 8)
                    byteRead = inputStream.read(chunk)

                    val byteArray = lengthChunk.plus(chunk)
                    val i = 4
                    val chunkCRC =
                        Utils.parseLength(byteArray.copyOfRange(byteArray.size - 4, byteArray.size))
                    val crc = CRC32()
                    crc.update(byteArray.copyOfRange(i, byteArray.size - 4))
                    if (chunkCRC == crc.value.toInt()) {
                        val name = byteArray.copyOfRange(i, i + 4)
                        when {
                            name.contentEquals(Utils.fcTL) -> {
                                if (png == null) {
                                    cover?.let {
                                        it.addAll(Utils.to4Bytes(0).asList())
                                        // Add IEND
                                        val iend = byteArrayOf(0x49, 0x45, 0x4E, 0x44)
                                        // Generate crc for IEND
                                        val crC32 = CRC32()
                                        crC32.update(iend, 0, iend.size)
                                        it.addAll(iend.asList())
                                        it.addAll(Utils.to4Bytes(crC32.value.toInt()).asList())
                                        APNGDisassembler.apng.cover = BitmapFactory.decodeByteArray(
                                            it.toByteArray(),
                                            0,
                                            it.size
                                        )
                                    }
                                    png = ArrayList()
                                    val fcTL = fcTL()
                                    fcTL.parse(byteArray)
                                    delay = fcTL.delay
                                    yOffset = fcTL.yOffset
                                    xOffset = fcTL.xOffset
                                    blendOp = fcTL.blendOp
                                    disposeOp = fcTL.disposeOp
                                    val width = fcTL.pngWidth
                                    val height = fcTL.pngHeight

                                    if (xOffset + width > maxWidth) {
                                        throw BadApng("`xOffset` + `width` must be <= `IHDR` width")
                                    } else if (yOffset + height > maxHeight) {
                                        throw BadApng("`yOffset` + `height` must be <= `IHDR` height")
                                    }

                                    png.addAll(Utils.pngSignature.asList())
                                    png.addAll(
                                        generateIhdr(
                                            ihdr,
                                            width,
                                            height
                                        ).asList()
                                    )
                                    plte?.let {
                                        png?.addAll(it.asList())
                                    }
                                    tnrs?.let {
                                        png?.addAll(it.asList())
                                    }
                                } else {
                                    // Add IEND body length : 0
                                    png.addAll(Utils.to4Bytes(0).asList())
                                    // Add IEND
                                    val iend = byteArrayOf(0x49, 0x45, 0x4E, 0x44)
                                    // Generate crc for IEND
                                    val crC32 = CRC32()
                                    crC32.update(iend, 0, iend.size)
                                    png.addAll(iend.asList())
                                    png.addAll(Utils.to4Bytes(crC32.value.toInt()).asList())

                                    val btm = Bitmap.createBitmap(
                                        maxWidth,
                                        maxHeight,
                                        Bitmap.Config.ARGB_8888
                                    )
                                    val decoded = BitmapFactory.decodeByteArray(
                                        png.toByteArray(),
                                        0,
                                        png.size
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
                                            if (btm.config != config) {
                                                if (BuildConfig.DEBUG)
                                                    Log.v(TAG, "Bitmap Config : ${btm.config}, Config : $config")
                                                btm.copy(config, btm.isMutable)
                                            } else {
                                                btm
                                            }
                                        ),
                                        (delay / speed).toInt()
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


                                    png = ArrayList()
                                    val fcTL = fcTL()
                                    fcTL.parse(byteArray)
                                    delay = fcTL.delay
                                    yOffset = fcTL.yOffset
                                    xOffset = fcTL.xOffset
                                    blendOp = fcTL.blendOp
                                    disposeOp = fcTL.disposeOp
                                    val width = fcTL.pngWidth
                                    val height = fcTL.pngHeight
                                    png.addAll(Utils.pngSignature.asList())
                                    png.addAll(
                                        generateIhdr(
                                            ihdr,
                                            width,
                                            height
                                        ).asList()
                                    )
                                    plte?.let {
                                        png.addAll(it.asList())
                                    }
                                    tnrs?.let {
                                        png.addAll(it.asList())
                                    }
                                }
                            }
                            name.contentEquals(Utils.IEND) -> {
                                if (isApng && png != null) {
                                    png.addAll(Utils.to4Bytes(0).asList())
                                    // Add IEND
                                    val iend = byteArrayOf(0x49, 0x45, 0x4E, 0x44)
                                    // Generate crc for IEND
                                    val crC32 = CRC32()
                                    crC32.update(iend, 0, iend.size)
                                    png.addAll(iend.asList())
                                    png.addAll(Utils.to4Bytes(crC32.value.toInt()).asList())


                                    val btm = Bitmap.createBitmap(
                                        maxWidth,
                                        maxHeight,
                                        Bitmap.Config.ARGB_8888
                                    )
                                    val decoded = BitmapFactory.decodeByteArray(
                                        png.toByteArray(),
                                        0,
                                        png.size
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
                                            if (btm.config != config) {
                                                if (BuildConfig.DEBUG)
                                                    Log.v(TAG, "Bitmap Config : ${btm.config}, Config : $config")
                                                btm.copy(config, btm.isMutable)
                                            } else {
                                                btm
                                            }
                                        ),
                                        (delay / speed).toInt()
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
                                    // TODO Check before the end
                                    cover?.let {
                                        it.addAll(Utils.to4Bytes(0).asList())
                                        // Add IEND
                                        val iend = byteArrayOf(0x49, 0x45, 0x4E, 0x44)
                                        // Generate crc for IEND
                                        val crC32 = CRC32()
                                        crC32.update(iend, 0, iend.size)
                                        it.addAll(iend.asList())
                                        it.addAll(Utils.to4Bytes(crC32.value.toInt()).asList())
                                        inputStream.close()
                                        return BitmapDrawable(
                                            context.resources,
                                            BitmapFactory.decodeByteArray(
                                                it.toByteArray(),
                                                0,
                                                it.size
                                            )
                                        )
                                    }
                                }
                            }
                            name.contentEquals(Utils.IDAT) -> {
                                if (png == null) {
                                    if (cover == null) {
                                        cover = ArrayList()
                                        cover.addAll(Utils.pngSignature.asList())
                                        cover.addAll(
                                            generateIhdr(
                                                ihdr,
                                                maxWidth,
                                                maxHeight
                                            ).asList()
                                        )
                                    }
                                    // Find the chunk length
                                    val bodySize =
                                        Utils.parseLength(byteArray.copyOfRange(i - 4, i))
                                    cover.addAll(byteArray.copyOfRange(i - 4, i).asList())
                                    val body = ArrayList<Byte>()
                                    body.addAll(byteArrayOf(0x49, 0x44, 0x41, 0x54).asList())
                                    // Get image bytes
                                    body.addAll(
                                        byteArray.copyOfRange(
                                            i + 4,
                                            i + 4 + bodySize
                                        ).asList()
                                    )
                                    val crC32 = CRC32()
                                    crC32.update(body.toByteArray(), 0, body.size)
                                    cover.addAll(body)
                                    cover.addAll(Utils.to4Bytes(crC32.value.toInt()).asList())
                                } else {
                                    // Find the chunk length
                                    val bodySize =
                                        Utils.parseLength(byteArray.copyOfRange(i - 4, i))
                                    png.addAll(byteArray.copyOfRange(i - 4, i).asList())
                                    val body = ArrayList<Byte>()
                                    body.addAll(byteArrayOf(0x49, 0x44, 0x41, 0x54).asList())
                                    // Get image bytes
                                    body.addAll(
                                        byteArray.copyOfRange(
                                            i + 4,
                                            i + 4 + bodySize
                                        ).asList()
                                    )
                                    val crC32 = CRC32()
                                    crC32.update(body.toByteArray(), 0, body.size)
                                    png.addAll(body)
                                    png.addAll(Utils.to4Bytes(crC32.value.toInt()).asList())
                                }
                            }
                            name.contentEquals(Utils.fdAT) -> {
                                // Find the chunk length
                                val bodySize = Utils.parseLength(byteArray.copyOfRange(i - 4, i))
                                png?.addAll(Utils.to4Bytes(bodySize - 4).asList())
                                val body = ArrayList<Byte>()
                                body.addAll(byteArrayOf(0x49, 0x44, 0x41, 0x54).asList())
                                // Get image bytes
                                body.addAll(byteArray.copyOfRange(i + 8, i + 4 + bodySize).asList())
                                val crC32 = CRC32()
                                crC32.update(body.toByteArray(), 0, body.size)
                                png?.addAll(body)
                                png?.addAll(Utils.to4Bytes(crC32.value.toInt()).asList())
                            }
                            name.contentEquals(Utils.plte) -> {
                                plte = byteArray
                            }
                            name.contentEquals(Utils.tnrs) -> {
                                tnrs = byteArray
                            }
                            name.contentEquals(Utils.IHDR) -> {
                                ihdr.parse(byteArray)
                                maxWidth = ihdr.pngWidth
                                maxHeight = ihdr.pngHeight
                                buffer = Bitmap.createBitmap(
                                    maxWidth,
                                    maxHeight,
                                    Bitmap.Config.ARGB_8888
                                )
                            }
                            name.contentEquals(Utils.acTL) -> {
                                isApng = true
                            }
                        }
                    } else throw BadCRC()
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
         */
        @Suppress("unused")
        @JvmStatic
        fun decodeApng(context: Context, file: File, speed: Float = 1f, config : Bitmap.Config = Bitmap.Config.ARGB_8888): Drawable =
            decodeApng(
                context,
                FileInputStream(file), speed, config
            )

        /**
         * Decode Apng and return a Drawable who can be an [AnimationDrawable] if it end successfully. Can also be an [android.graphics.drawable.AnimatedImageDrawable].
         * @param context Context is needed for contentResolver and animation drawable.
         * @param uri Uri to open.
         * @param speed Optional parameter.
         * @param config Configuration applied to the bitmap added to the animation. Please note that the frame is decoded in ARGB_8888 and converted after, for the buffer.
         */
        @Suppress("unused")
        @JvmStatic
        fun decodeApng(context: Context, uri: Uri, speed: Float = 1f, config : Bitmap.Config = Bitmap.Config.ARGB_8888): Drawable {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw Exception("Failed to open InputStream, InputStream is null")
            return decodeApng(
                context,
                inputStream,
                speed,
                config
            )
        }

        /**
         * Decode Apng and return a Drawable who can be an [AnimationDrawable] if it end successfully. Can also be an [android.graphics.drawable.AnimatedImageDrawable].
         * @param context Context is needed for contentResolver and animation drawable.
         * @param res Resource to decode.
         * @param speed Optional parameter.
         * @param config Configuration applied to the bitmap added to the animation. Please note that the frame is decoded in ARGB_8888 and converted after, for the buffer.
         */
        @Suppress("unused")
        @JvmStatic
        fun decodeApng(context: Context, @RawRes res: Int, speed: Float = 1f, config : Bitmap.Config = Bitmap.Config.ARGB_8888): Drawable =
            decodeApng(
                context,
                context.resources.openRawResource(res),
                speed,
                config
            )

        /**
         * Decode Apng and return a Drawable who can be an [AnimationDrawable] if it end successfully. Can also be an [android.graphics.drawable.AnimatedImageDrawable].
         * @param context Context is needed for contentResolver and animation drawable.
         * @param url URL to decode.
         * @param speed Optional parameter.
         * @param config Configuration applied to the bitmap added to the animation. Please note that the frame is decoded in ARGB_8888 and converted after, for the buffer.
         */
        @Suppress("unused")
        @JvmStatic
        suspend fun decodeApng(context: Context, url: URL, speed: Float = 1f, config : Bitmap.Config = Bitmap.Config.ARGB_8888) =
            withContext(Dispatchers.IO) {
                decodeApng(
                    context,
                    ByteArrayInputStream(Loader.load(url)),
                    speed,
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
            speed: Float = 1f,
            callback: Callback? = null,
            config : Bitmap.Config = Bitmap.Config.ARGB_8888
        ) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val drawable =
                        decodeApng(
                            context,
                            FileInputStream(file),
                            speed,
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
            speed: Float = 1f,
            callback: Callback? = null,
            config : Bitmap.Config = Bitmap.Config.ARGB_8888
        ) {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw Exception("Failed to open InputStream, InputStream is null")
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val drawable =
                        decodeApng(
                            context,
                            inputStream,
                            speed,
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
            speed: Float = 1f,
            callback: Callback? = null,
            config : Bitmap.Config = Bitmap.Config.ARGB_8888
        ) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val drawable =
                        decodeApng(
                            context,
                            context.resources.openRawResource(res),
                            speed,
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
            speed: Float = 1f,
            callback: Callback? = null,
            config : Bitmap.Config = Bitmap.Config.ARGB_8888
        ) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val drawable =
                        decodeApng(
                            context,
                            ByteArrayInputStream(
                                Loader.load(
                                    url
                                )
                            ),
                            speed,
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
            speed: Float = 1f,
            callback: Callback? = null,
            config : Bitmap.Config = Bitmap.Config.ARGB_8888
        ) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    if (string.startsWith("http://") || string.startsWith("https://")) {
                        decodeApngAsyncInto(
                            context,
                            URL(string),
                            imageView,
                            speed,
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
                            speed,
                            callback,
                            config
                        )
                    } else if (string.startsWith("file://android_asset/")) {
                        val drawable =
                            decodeApng(
                                context,
                                context.assets.open(string.replace("file:///android_asset/", "")),
                                speed,
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
        private fun generateIhdr(ihdrOfApng: IHDR, width: Int, height: Int): ByteArray {
            val ihdr = ArrayList<Byte>()
            // We need a body var to know body length and generate crc
            val ihdrBody = ArrayList<Byte>()
            // Add chunk body length
            ihdr.addAll(Utils.to4Bytes(ihdrOfApng.body.size).asList())
            // Add IHDR
            ihdrBody.addAll(
                byteArrayOf(
                    0x49.toByte(),
                    0x48.toByte(),
                    0x44.toByte(),
                    0x52.toByte()
                ).asList()
            )
            // Add the max width and height
            ihdrBody.addAll(Utils.to4Bytes(width).asList())
            ihdrBody.addAll(Utils.to4Bytes(height).asList())
            // Add complicated stuff like depth color ...
            // If you want correct png you need same parameters. Good solution is to create new png.
            ihdrBody.addAll(ihdrOfApng.body.copyOfRange(8, 13).asList())
            // Generate CRC
            val crC32 = CRC32()
            crC32.update(ihdrBody.toByteArray(), 0, ihdrBody.size)
            ihdr.addAll(ihdrBody)
            ihdr.addAll(Utils.to4Bytes(crC32.value.toInt()).asList())
            return ihdr.toByteArray()
        }
    }
}