package oupson.apng.encoder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import oupson.apng.chunks.IDAT
import oupson.apng.imageUtils.PngEncoder
import oupson.apng.utils.Utils
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.CRC32

// TODO DOCUMENTATION
// TODO BITMAP ENCODING
// TODO BUFFER AND BUFFER DEACTIVATION WHEN BITMAP CONFIG DOES NOT CONTAIN AN ALPHA CHANNEL
class ApngEncoder(
    private val outputStream: OutputStream,
    private val width : Int,
    private val height : Int,
    numberOfFrames : Int) {
    private var frameIndex = 0
    private var seq = 0

    private val idatName : List<Byte> by lazy {
        listOf(0x49.toByte(), 0x44.toByte(), 0x41.toByte(), 0x54.toByte())
    }

    init {
        outputStream.write(Utils.pngSignature)
        outputStream.write(generateIhdr())
        outputStream.write(generateACTL(numberOfFrames))
    }

    // TODO ADD SUPPORT FOR FIRST FRAME NOT IN ANIM
    // TODO OPTIMISE APNG
    @JvmOverloads
    fun writeFrame(
        inputStream: InputStream,
        delay: Float = 1000f,
        xOffsets: Int = 0,
        yOffsets: Int = 0,
        blendOp: Utils.Companion.BlendOp = Utils.Companion.BlendOp.APNG_BLEND_OP_SOURCE,
        disposeOp: Utils.Companion.DisposeOp = Utils.Companion.DisposeOp.APNG_DISPOSE_OP_NONE,
        usePngEncoder: Boolean = true
    ) {
        val btm = BitmapFactory.decodeStream(inputStream).let {
            if (it.config != Bitmap.Config.ARGB_8888)
                it.copy(Bitmap.Config.ARGB_8888, it.isMutable)
            else
                it
        }
        inputStream.close()

        if (frameIndex == 0) {
            if (btm.width != width)
                throw Exception("Width of first frame must be equal to width of APNG. (${btm.width} != $width)")
            if (btm.height != height)
                throw Exception("Height of first frame must be equal to height of APNG. (${btm.height} != $height)")
        }

        generateFCTL(btm, delay, disposeOp, blendOp, xOffsets, yOffsets)

        val idat = IDAT().apply {
            val byteArray = if (usePngEncoder) {
                PngEncoder().encode(btm, true)
            } else {
                val outputStream = ByteArrayOutputStream()
                btm.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.toByteArray()
            }
            var cursor = 8
            while (cursor < byteArray.size) {
                val chunk = byteArray.copyOfRange(cursor, cursor + Utils.parseLength(byteArray.copyOfRange(cursor, cursor + 4)) + 12)
                parse(chunk)

                cursor += Utils.parseLength(byteArray.copyOfRange(cursor, cursor + 4)) + 12
            }
        }

        idat.IDATBody.forEach { idatBody ->
            if (frameIndex == 0) {
                val idatChunk = ArrayList<Byte>().let { i ->
                    // Add IDAT
                    i.addAll(idatName)
                    // Add chunk body
                    i.addAll(idatBody.asList())
                    i.toByteArray()
                }
                // Add the chunk body length
                outputStream.write(Utils.to4BytesArray(idatBody.size))

                // Generate CRC
                val crc1 = CRC32()
                crc1.update(idatChunk, 0, idatChunk.size)
                outputStream.write(idatChunk)
                outputStream.write(Utils.to4BytesArray(crc1.value.toInt()))
            } else {
                val fdat = ArrayList<Byte>().let { fdat ->
                    fdat.addAll(byteArrayOf(0x66, 0x64, 0x41, 0x54).asList())
                    // Add fdat
                    fdat.addAll(Utils.to4Bytes(seq++).asList())
                    // Add chunk body
                    fdat.addAll(idatBody.asList())
                    fdat.toByteArray()
                }
                // Add the chunk body length
                outputStream.write(Utils.to4BytesArray(idatBody.size + 4))

                // Generate CRC
                val crc1 = CRC32()
                crc1.update(fdat, 0, fdat.size)
                outputStream.write(fdat)
                outputStream.write(Utils.to4BytesArray(crc1.value.toInt()))
            }
        }
        frameIndex++
        /**if (usePngEncoder) {
            PngEncoder.release()
        }*/
    }

    fun writeEnd() {
        // Add IEND body length : 0
        outputStream.write(Utils.to4BytesArray(0))
        // Add IEND
        val iend = byteArrayOf(0x49, 0x45, 0x4E, 0x44)
        // Generate crc for IEND
        val crC32 = CRC32()
        crC32.update(iend, 0, iend.size)
        outputStream.write(iend)
        outputStream.write(Utils.to4BytesArray(crC32.value.toInt()))
    }

    /**
     * Generate the IHDR chunks.
     * @return [ByteArray] The byteArray generated
     */
    private fun generateIhdr(): ByteArray {
        val ihdr = ArrayList<Byte>()

        // We need a body var to know body length and generate crc
        val ihdrBody = ArrayList<Byte>()

        /**
        if (((maxWidth != frames[0].width) && (maxHeight != frames[0].height)) && cover == null) {
        cover = generateCover(BitmapFactory.decodeByteArray(frames[0].byteArray, 0, frames[0].byteArray.size), maxWidth!!, maxHeight!!)
        }*/


        // Add chunk body length
        ihdr.addAll(arrayListOf(0x00, 0x00, 0x00, 0x0d))
        ihdrBody.addAll(arrayListOf(0x49, 0x48, 0x44, 0x52))

        // Add the max width and height
        ihdrBody.addAll(Utils.to4Bytes(width).asList())
        ihdrBody.addAll(Utils.to4Bytes(height).asList())

        ihdrBody.add(8.toByte())
        ihdrBody.add(6.toByte())
        ihdrBody.add(0.toByte())
        ihdrBody.add(0.toByte())
        ihdrBody.add(0.toByte())


        // Generate CRC
        val crC32 = CRC32()
        crC32.update(ihdrBody.toByteArray(), 0, ihdrBody.size)
        ihdr.addAll(ihdrBody)
        ihdr.addAll(Utils.to4Bytes(crC32.value.toInt()).asList())
        return ihdr.toByteArray()
    }

    /**
     * Generate the animation control chunk
     * @return [ArrayList] The byteArray generated
     */
    private fun generateACTL(num: Int): ByteArray {
        val res = ArrayList<Byte>()
        val actl = ArrayList<Byte>()

        // Add length bytes
        res.addAll(arrayListOf(0, 0, 0, 0x08))

        // Add acTL
        actl.addAll(byteArrayOf(0x61, 0x63, 0x54, 0x4c).asList())

        // Add number of frames
        actl.addAll(Utils.to4Bytes(num).asList())

        // Number of repeat, 0 to infinite
        actl.addAll(Utils.to4Bytes(0).asList())
        res.addAll(actl)

        // generate crc
        val crc = CRC32()
        crc.update(actl.toByteArray(), 0, actl.size)
        res.addAll(Utils.to4Bytes(crc.value.toInt()).asList())
        return res.toByteArray()
    }

    private fun generateFCTL(btm : Bitmap, delay: Float, disposeOp: Utils.Companion.DisposeOp, blendOp: Utils.Companion.BlendOp, xOffsets: Int, yOffsets: Int) {
        val fcTL = ArrayList<Byte>()

        // Add the length of the chunk body
        outputStream.write(byteArrayOf(0x00, 0x00, 0x00, 0x1A))

        // Add fcTL
        fcTL.addAll(byteArrayOf(0x66, 0x63, 0x54, 0x4c).asList())

        // Add the frame number
        fcTL.addAll(Utils.to4Bytes(seq++).asList())

        // Add width and height
        fcTL.addAll(Utils.to4Bytes(btm.width).asList())
        fcTL.addAll(Utils.to4Bytes(btm.height).asList())

        // Add offsets
        fcTL.addAll(Utils.to4Bytes(xOffsets).asList())
        fcTL.addAll(Utils.to4Bytes(yOffsets).asList())

        // Set frame delay
        fcTL.addAll(Utils.to2Bytes(delay.toInt()).asList())
        fcTL.addAll(Utils.to2Bytes(1000).asList())

        // Add DisposeOp and BlendOp
        fcTL.add(Utils.getDisposeOp(disposeOp).toByte())
        fcTL.add(Utils.getBlendOp(blendOp).toByte())

        // Create CRC
        val crc = CRC32()
        crc.update(fcTL.toByteArray(), 0, fcTL.size)
        outputStream.write(fcTL.toByteArray())
        outputStream.write(Utils.to4BytesArray(crc.value.toInt()))
    }
}