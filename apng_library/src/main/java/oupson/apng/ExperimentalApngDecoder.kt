package oupson.apng

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import androidx.annotation.RawRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import oupson.apng.chunks.IHDR
import oupson.apng.chunks.fcTL
import oupson.apng.exceptions.BadApng
import oupson.apng.exceptions.BadCRC
import oupson.apng.utils.Utils
import oupson.apng.utils.Utils.Companion.isPng
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URL
import java.util.zip.CRC32

// TODO DOC CODE
class ExperimentalApngDecoder {
    companion object {
        // TODO Change TAG
        private const val TAG = "ExperimentalApngDecoder"

        private val clearPaint : Paint by lazy {
            Paint().apply {
                xfermode = PorterDuffXfermode(
                    PorterDuff.Mode.CLEAR
                )
            }
        }

        /**
         * Decode Apng and return a Drawable who can be an [CustomAnimationDrawable] if it end successfully.
         * @param inStream Input Stream to decode. Will be close at the end
         * @param speed Optional parameter.
         */
        @Suppress("MemberVisibilityCanBePrivate")
        @JvmStatic
        fun decodeApng(inStream: InputStream, speed : Float = 1f) : Drawable {
            val inputStream = BufferedInputStream(inStream)
            val bytes = ByteArray(8)
            inputStream.mark(0)
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
                var disposeOp: Utils.Companion.DisposeOp = Utils.Companion.DisposeOp.APNG_DISPOSE_OP_NONE
                val ihdr = IHDR()
                var isApng = false

                val drawable = CustomAnimationDrawable()

                var buffer : Bitmap? = null

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
                                        APNGDisassembler.apng.cover = BitmapFactory.decodeByteArray(it.toByteArray(), 0, it.size)
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
                                        throw BadApng("`yOffset` + `height` must be <= `IHDR` height")
                                    } else if (yOffset + height > maxHeight) {
                                        throw BadApng("`yOffset` + `height` must be <= `IHDR` height")
                                    }

                                    png.addAll(Utils.pngSignature.asList())
                                    png.addAll(generateIhdr(ihdr, width, height).asList())
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

                                    val btm = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888)
                                    val decoded = BitmapFactory.decodeByteArray(png.toByteArray(), 0, png.size)
                                    val canvas = Canvas(btm)
                                    canvas.drawBitmap(buffer!!, 0f, 0f, null)

                                    if (blendOp == Utils.Companion.BlendOp.APNG_BLEND_OP_SOURCE) {
                                        canvas.drawRect(xOffset.toFloat(), yOffset.toFloat(), xOffset+ decoded.width.toFloat(), yOffset + decoded.height.toFloat(), clearPaint)
                                    }

                                    canvas.drawBitmap(decoded, xOffset.toFloat(), yOffset.toFloat(), null)
                                    drawable.addFrame(BitmapDrawable(btm), (delay / speed).toInt())

                                    when(disposeOp) {
                                        Utils.Companion.DisposeOp.APNG_DISPOSE_OP_PREVIOUS -> {
                                            //Do nothings
                                        }
                                        // Add current frame to bitmap buffer
                                        // APNG_DISPOSE_OP_BACKGROUND: the frame's region of the output buffer is to be cleared to fully transparent black before rendering the next frame.
                                        Utils.Companion.DisposeOp.APNG_DISPOSE_OP_BACKGROUND -> {
                                            val res = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888)
                                            val can = Canvas(res)
                                            can.drawBitmap(btm, 0f, 0f, null)
                                            can.drawRect(xOffset.toFloat(), yOffset.toFloat(), xOffset + decoded.width.toFloat(), yOffset + decoded.height.toFloat(), clearPaint)
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
                                    png.addAll(generateIhdr(ihdr, width, height).asList())
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


                                    val btm = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888)
                                    val decoded = BitmapFactory.decodeByteArray(png.toByteArray(), 0, png.size)
                                    val canvas = Canvas(btm)
                                    canvas.drawBitmap(buffer!!, 0f, 0f, null)

                                    if (blendOp == Utils.Companion.BlendOp.APNG_BLEND_OP_SOURCE) {
                                        canvas.drawRect(xOffset.toFloat(), yOffset.toFloat(), xOffset+ decoded.width.toFloat(), yOffset + decoded.height.toFloat(), clearPaint)
                                    }

                                    canvas.drawBitmap(decoded, xOffset.toFloat(), yOffset.toFloat(), null)
                                    drawable.addFrame(BitmapDrawable(btm), (delay / speed).toInt())

                                    when(disposeOp) {
                                        Utils.Companion.DisposeOp.APNG_DISPOSE_OP_PREVIOUS -> {
                                            //Do nothings
                                        }
                                        // Add current frame to bitmap buffer
                                        // APNG_DISPOSE_OP_BACKGROUND: the frame's region of the output buffer is to be cleared to fully transparent black before rendering the next frame.
                                        Utils.Companion.DisposeOp.APNG_DISPOSE_OP_BACKGROUND -> {
                                            val res = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888)
                                            val can = Canvas(res)
                                            can.drawBitmap(btm, 0f, 0f, null)
                                            can.drawRect(xOffset.toFloat(), yOffset.toFloat(), xOffset + decoded.width.toFloat(), yOffset + decoded.height.toFloat(), clearPaint)
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
                                        return BitmapDrawable(BitmapFactory.decodeByteArray(it.toByteArray(), 0, it.size))
                                    }
                                }
                            }
                            name.contentEquals(Utils.IDAT) -> {
                                if (png == null) {
                                    if (cover == null) {
                                        cover = ArrayList()
                                        cover.addAll(Utils.pngSignature.asList())
                                        cover.addAll(generateIhdr(
                                            ihdr,
                                            maxWidth,
                                            maxHeight
                                        ).asList())
                                    }
                                    // Find the chunk length
                                    val bodySize = Utils.parseLength(byteArray.copyOfRange(i - 4, i))
                                    cover.addAll(byteArray.copyOfRange(i - 4, i).asList())
                                    val body = ArrayList<Byte>()
                                    body.addAll(byteArrayOf(0x49, 0x44, 0x41, 0x54).asList())
                                    // Get image bytes
                                    body.addAll(byteArray.copyOfRange(i + 4, i + 4 + bodySize).asList())
                                    val crC32 = CRC32()
                                    crC32.update(body.toByteArray(), 0, body.size)
                                    cover.addAll(body)
                                    cover.addAll(Utils.to4Bytes(crC32.value.toInt()).asList())
                                } else {
                                    // Find the chunk length
                                    val bodySize = Utils.parseLength(byteArray.copyOfRange(i - 4, i))
                                    png.addAll(byteArray.copyOfRange(i - 4, i).asList())
                                    val body = ArrayList<Byte>()
                                    body.addAll(byteArrayOf(0x49, 0x44, 0x41, 0x54).asList())
                                    // Get image bytes
                                    body.addAll(byteArray.copyOfRange(i + 4, i + 4 + bodySize).asList())
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
                                maxHeight =ihdr.pngHeight
                                buffer = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888)
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
                return Drawable.createFromStream(
                    inputStream,
                    null
                )
            }
        }

        /**
         * Decode Apng and return a Drawable who can be an [CustomAnimationDrawable] if it end successfully.
         * @param file File to decode.
         * @param speed Optional parameter.
         */
        @Suppress("unused")
        @JvmStatic
        fun decodeApng(file : File, speed: Float = 1f) : Drawable = decodeApng(FileInputStream(file), speed)

        /**
         * Decode Apng and return a Drawable who can be an [CustomAnimationDrawable] if it end successfully.
         * @param context Context is needed for contentResolver
         * @param uri Uri to open.
         * @param speed Optional parameter.
         */
        @JvmStatic
        fun decodeApng(context : Context, uri : Uri, speed: Float = 1f) : Drawable {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw Exception("Failed to open InputStream, InputStream is null")
            return decodeApng(inputStream, speed)
        }

        /**
         * Decode Apng and return a Drawable who can be an [CustomAnimationDrawable] if it end successfully.
         * @param context Context is needed for contentResolver
         * @param res Resource to decode.
         * @param speed Optional parameter.
         */
        @Suppress("unused")
        @JvmStatic
        fun decodeApng(context : Context, @RawRes res : Int, speed: Float = 1f) : Drawable = decodeApng(context.resources.openRawResource(res), speed)

        /**
         * Decode Apng and return a Drawable who can be an [CustomAnimationDrawable] if it end successfully.
         * @param context Context is needed for contentResolver
         * @param url URL to decode.
         * @param speed Optional parameter.
         */
        @Suppress("unused")
        @JvmStatic
        suspend fun decodeApng(context : Context, url : URL, speed: Float = 1f) = withContext(Dispatchers.IO) {
            decodeApng(FileInputStream(Loader.load(context, url)), speed)
        }

        /**
         * Generate a correct IHDR from the IHDR chunk of the APNG
         * @param ihdrOfApng The IHDR of the APNG
         * @param width The width of the frame
         * @param height The height of the frame
         * @return [ByteArray] The generated IHDR
         */
        private fun generateIhdr(ihdrOfApng: IHDR, width : Int, height : Int) : ByteArray {
            val ihdr = ArrayList<Byte>()
            // We need a body var to know body length and generate crc
            val ihdrBody = ArrayList<Byte>()
            // Add chunk body length
            ihdr.addAll(Utils.to4Bytes(ihdrOfApng.body.size).asList())
            // Add IHDR
            ihdrBody.addAll(byteArrayOf(0x49.toByte(), 0x48.toByte(), 0x44.toByte(), 0x52.toByte()).asList())
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