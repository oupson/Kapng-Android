package oupson.apng

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import oupson.apng.chunks.IDAT
import oupson.apng.exceptions.NoFrameException
import oupson.apng.imageUtils.BitmapDiffCalculator
import oupson.apng.imageUtils.PngEncoder
import oupson.apng.imageUtils.PnnQuantizer
import oupson.apng.utils.Utils
import oupson.apng.utils.Utils.Companion.encodeBlendOp
import oupson.apng.utils.Utils.Companion.encodeDisposeOp
import oupson.apng.utils.Utils.Companion.pngSignature
import java.io.File
import java.util.zip.CRC32


// TODO CREATE A BETTER CLASS
/**
 * Create an APNG file
 * If you want to create an APNG, use ApngEncoder instead
 */
@Suppress("unused")
@Deprecated("Deprecated, Use ApngEncoder and ApngDecoder instead", level = DeprecationLevel.WARNING)
class Apng {
    @Suppress("MemberVisibilityCanBePrivate")
    var maxWidth : Int? = null
    @Suppress("MemberVisibilityCanBePrivate")
    var maxHeight : Int? = null

    /**
     * Image that will display in non compatible reader
     * It's not necessary if the first frame is the biggest image.
     * If it's null the library generate a cover with the first frame
     */
    var cover : Bitmap? = null

    var frames : ArrayList<Frame> = ArrayList()

    var isApng = true

    // region addFrames
    /**
     * Add a frame to the APNG
     * @param bitmap The bitmap to add
     * @param index Index of the frame in the animation
     * @param delay Delay of the frame
     * @param xOffset The X offset where the frame should be rendered
     * @param yOffset The Y offset where the frame should be rendered
     * @param disposeOp `DisposeOp` specifies how the output buffer should be changed at the end of the delay (before rendering the next frame).
     * @param blendOp `BlendOp` specifies whether the frame is to be alpha blended into the current output buffer content, or whether it should completely replace its region in the output buffer.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun addFrames(bitmap : Bitmap, index : Int? = null, delay : Float = 1000f, xOffset : Int = 0, yOffset : Int = 0, disposeOp: Utils.Companion.DisposeOp = Utils.Companion.DisposeOp.APNG_DISPOSE_OP_NONE, blendOp: Utils.Companion.BlendOp = Utils.Companion.BlendOp.APNG_BLEND_OP_SOURCE) {
        if (index == null)
            frames.add(Frame(PngEncoder().encode(bitmap, true), delay, xOffset, yOffset, blendOp, disposeOp))
        else
            frames.add(index, Frame(PngEncoder().encode(bitmap, true), delay, xOffset, yOffset, blendOp, disposeOp))
    }

    /**
     * Add a frame to the APNG
     * @param frame The frame to add
     * @param index Index of the frame in the animation
     */
    fun addFrames(frame : Frame, index: Int? = null) {
        if (index == null)
            frames.add(frame)
        else
            frames.add(index, frame)
    }
    //endregion

    /**
     * Generate a Bytes Array of the APNG
     * @return [ByteArray] The Bytes Array of the APNG
     */
    fun toByteArray() : ByteArray {
        var seq = 0
        val res = ArrayList<Byte>()
        // Add PNG signature
        res.addAll(pngSignature.asList())
        // Add Image Header
        res.addAll(generateIhdr().asList())

        // Add Animation Controller
        res.addAll(generateACTL())

        // Get max height and max width
        maxHeight = frames.sortedByDescending { it.height }[0].height
        maxWidth = frames.sortedByDescending { it.width }[0].width

        if (cover == null) {
            val framesByte = ArrayList<Byte>()
            // region fcTL
            // Create the fcTL
            val fcTL = ArrayList<Byte>()

            // Add the length of the chunk body
            framesByte.addAll(byteArrayOf(0x00, 0x00, 0x00, 0x1A).asList())

            // Add fcTL
            fcTL.addAll(byteArrayOf(0x66, 0x63, 0x54, 0x4c).asList())

            // Add the frame number
            fcTL.addAll(Utils.uIntToByteArray(seq).asList())

            // foreach fcTL or fdAT we must increment seq
            seq++

            // Add width and height
            fcTL.addAll(Utils.uIntToByteArray(frames[0].width).asList())
            fcTL.addAll(Utils.uIntToByteArray(frames[0].height).asList())

            // Add offsets
            fcTL.addAll(Utils.uIntToByteArray(frames[0].xOffsets).asList())
            fcTL.addAll(Utils.uIntToByteArray(frames[0].yOffsets).asList())

            // Set frame delay
            fcTL.addAll(Utils.uShortToArray(frames[0].delay.toInt()).asList())
            fcTL.addAll(Utils.uShortToArray(1000).asList())

            // Add DisposeOp and BlendOp
            fcTL.add(encodeDisposeOp(frames[0].disposeOp).toByte())
            fcTL.add(encodeBlendOp(frames[0].blendOp).toByte())

            // Create CRC
            val crc = CRC32()
            crc.update(fcTL.toByteArray(), 0, fcTL.size)
            framesByte.addAll(fcTL)
            framesByte.addAll(Utils.uIntToByteArray(crc.value.toInt()).asList())
            // endregion

            // region idat
            frames[0].idat.IDATBody.forEach {
                val idat = ArrayList<Byte>()
                // Add the chunk body length
                framesByte.addAll(Utils.uIntToByteArray(it.size).asList())
                // Add IDAT
                idat.addAll(byteArrayOf(0x49, 0x44, 0x41, 0x54).asList())
                // Add chunk body
                idat.addAll(it.asList())
                // Generate CRC
                val crc1 = CRC32()
                crc1.update(idat.toByteArray(), 0, idat.size)
                framesByte.addAll(idat)
                framesByte.addAll(Utils.uIntToByteArray(crc1.value.toInt()).asList())
            }
            // endregion
            res.addAll(framesByte)
        } else {
            val framesByte = ArrayList<Byte>()
            // Add cover image : Not part of animation
            // region IDAT
            val idat = IDAT()
            idat.parse(PngEncoder().encode(cover!!, true, 1))
            idat.IDATBody.forEach {
                val idatByteArray = ArrayList<Byte>()
                framesByte.addAll(Utils.uIntToByteArray(it.size).asList())
                idatByteArray.addAll(byteArrayOf(0x49, 0x44, 0x41, 0x54).asList())
                idatByteArray.addAll(it.asList())
                val crc1 = CRC32()
                crc1.update(idatByteArray.toByteArray(), 0, idatByteArray.size)
                framesByte.addAll(idatByteArray)
                framesByte.addAll(Utils.uIntToByteArray(crc1.value.toInt()).asList())
            }
            // endregion

            //region fcTL
            val fcTL = ArrayList<Byte>()
            // Add the length of the chunk body
            framesByte.addAll(byteArrayOf(0x00, 0x00, 0x00, 0x1A).asList())
            // Add fcTL
            fcTL.addAll(byteArrayOf(0x66, 0x63, 0x54, 0x4c).asList())

            // Add the frame number
            fcTL.addAll(Utils.uIntToByteArray(seq).asList())
            seq++

            // Add width and height
            fcTL.addAll(Utils.uIntToByteArray(frames[0].width).asList())
            fcTL.addAll(Utils.uIntToByteArray(frames[0].height).asList())


            fcTL.addAll(Utils.uIntToByteArray(frames[0].xOffsets).asList())
            fcTL.addAll(Utils.uIntToByteArray(frames[0].yOffsets).asList())

            // Set frame delay
            fcTL.addAll(Utils.uShortToArray(frames[0].delay.toInt()).asList())
            fcTL.addAll(Utils.uShortToArray(1000).asList())

            // Add DisposeOp and BlendOp
            fcTL.add(encodeDisposeOp(frames[0].disposeOp).toByte())
            fcTL.add(encodeBlendOp(frames[0].blendOp).toByte())

            // Generate CRC
            val crc = CRC32()
            crc.update(fcTL.toByteArray(), 0, fcTL.size)
            framesByte.addAll(fcTL)
            framesByte.addAll(Utils.uIntToByteArray(crc.value.toInt()).asList())
            // endregion

            // region fdat
            frames[0].idat.IDATBody.forEach {
                val fdat = ArrayList<Byte>()
                // Add the chunk body length
                framesByte.addAll(Utils.uIntToByteArray(it.size + 4).asList())
                // Add fdat
                fdat.addAll(byteArrayOf(0x66, 0x64, 0x41, 0x54).asList())
                fdat.addAll(Utils.uIntToByteArray(seq).asList())
                seq++
                // Add chunk body
                fdat.addAll(it.asList())
                // Generate CRC
                val crc1 = CRC32()
                crc1.update(fdat.toByteArray(), 0, fdat.size)
                framesByte.addAll(fdat)
                framesByte.addAll(Utils.uIntToByteArray(crc1.value.toInt()).asList())
            }
            // endregion
            res.addAll(framesByte)
        }

        for (i in 1 until frames.size) {
            // If it's the first frame
            val framesByte = ArrayList<Byte>()
            val fcTL = ArrayList<Byte>()
            // region fcTL
            framesByte.addAll(byteArrayOf(0x00, 0x00, 0x00, 0x1A).asList())

            fcTL.addAll(byteArrayOf(0x66, 0x63, 0x54, 0x4c).asList())

            // Frame number
            fcTL.addAll(Utils.uIntToByteArray(seq).asList())
            seq++
            // width and height
            fcTL.addAll(Utils.uIntToByteArray(frames[i].width).asList())
            fcTL.addAll(Utils.uIntToByteArray(frames[i].height).asList())

            fcTL.addAll(Utils.uIntToByteArray(frames[i].xOffsets).asList())
            fcTL.addAll(Utils.uIntToByteArray(frames[i].yOffsets).asList())

            // Set frame delay
            fcTL.addAll(Utils.uShortToArray(frames[i].delay.toInt()).asList())
            fcTL.addAll(Utils.uShortToArray(1000).asList())

            fcTL.add(encodeDisposeOp(frames[i].disposeOp).toByte())
            fcTL.add(encodeBlendOp(frames[i].blendOp).toByte())

            val crc = CRC32()
            crc.update(fcTL.toByteArray(), 0, fcTL.size)
            framesByte.addAll(fcTL)
            framesByte.addAll(Utils.uIntToByteArray(crc.value.toInt()).asList())
            // endregion

            // region fdAT
            // Write fdAT
            frames[i].idat.IDATBody.forEach {
                val fdat = ArrayList<Byte>()
                // Add IDAT size of frame + 4 byte of the seq
                framesByte.addAll(Utils.uIntToByteArray(it.size + 4).asList())
                // Add fdAT
                fdat.addAll(byteArrayOf(0x66, 0x64, 0x41, 0x54).asList())
                // Add Sequence number
                // ! THIS IS NOT FRAME NUMBER
                fdat.addAll(Utils.uIntToByteArray(seq).asList())
                // Increase seq
                seq++
                fdat.addAll(it.asList())
                // Generate CRC
                val crc1 = CRC32()
                crc1.update(fdat.toByteArray(), 0, fdat.size)
                framesByte.addAll(fdat)
                framesByte.addAll(Utils.uIntToByteArray(crc1.value.toInt()).asList())
            }
            // endregion
            res.addAll(framesByte)
        }
        if (frames.isNotEmpty()) {

            // Add IEND body length : 0
            res.addAll(Utils.uIntToByteArray(0).asList())
            // Add IEND
            val iend = byteArrayOf(0x49, 0x45, 0x4E, 0x44)
            // Generate crc for IEND
            val crC32 = CRC32()
            crC32.update(iend, 0, iend.size)
            res.addAll(iend.asList())
            res.addAll(Utils.uIntToByteArray(crC32.value.toInt()).asList())
            return res.toByteArray()
        } else {
            throw NoFrameException()
        }
    }

    /**
     * Generate a cover image that have the max width and height.
     * You could also set yours
     * @param bitmap The bitmap of the cover
     * @param maxWidth Max width of APNG
     * @param maxHeight Max height of the APNG
     * @return [Bitmap] An image cover
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun generateCover(bitmap: Bitmap, maxWidth : Int, maxHeight : Int) : Bitmap {
        return Bitmap.createScaledBitmap(bitmap, maxWidth, maxHeight, false)
    }

    /**
     * Generate the IHDR chunks.
     * @return [ByteArray] The byteArray generated
     */
    private fun generateIhdr(): ByteArray {
        val ihdr = ArrayList<Byte>()

        // We need a body var to know body length and generate crc
        val ihdrBody = ArrayList<Byte>()

        // Get max height and max width of all the frames
        maxHeight = frames.sortedByDescending { it.height }[0].height
        maxWidth = frames.sortedByDescending { it.width }[0].width

        if (((maxWidth != frames[0].width) && (maxHeight != frames[0].height)) && cover == null) {
            cover = generateCover(BitmapFactory.decodeByteArray(frames[0].byteArray, 0, frames[0].byteArray.size), maxWidth!!, maxHeight!!)
        }

        // Add chunk body length
        ihdr.addAll(Utils.uIntToByteArray(frames[0].ihdr.body.size).asList())
        // Add IHDR
        ihdrBody.addAll(byteArrayOf(0x49.toByte(), 0x48.toByte(), 0x44.toByte(), 0x52.toByte()).asList())

        // Add the max width and height
        ihdrBody.addAll(Utils.uIntToByteArray(maxWidth!!).asList())
        ihdrBody.addAll(Utils.uIntToByteArray(maxHeight!!).asList())

        // Add complicated stuff like depth color ...
        // If you want correct png you need same parameters. Good solution is to create new png.
        ihdrBody.addAll(frames[0].ihdr.body.copyOfRange(8, 13).asList())

        // Generate CRC
        val crC32 = CRC32()
        crC32.update(ihdrBody.toByteArray(), 0, ihdrBody.size)
        ihdr.addAll(ihdrBody)
        ihdr.addAll(Utils.uIntToByteArray(crC32.value.toInt()).asList())
        return ihdr.toByteArray()
    }

    /**
     * Generate the animation control chunk
     * @return [ArrayList] The byteArray generated
     */
    private fun generateACTL(): ArrayList<Byte> {
        val res = ArrayList<Byte>()
        val actl = ArrayList<Byte>()

        // Add length bytes
        res.addAll(Utils.uIntToByteArray(8).asList())

        // Add acTL
        actl.addAll(byteArrayOf(0x61, 0x63, 0x54, 0x4c).asList())

        // Add number of frames
        actl.addAll(Utils.uIntToByteArray(frames.size).asList())

        // Number of repeat, 0 to infinite
        actl.addAll(Utils.uIntToByteArray(0).asList())
        res.addAll(actl)

        // generate crc
        val crc = CRC32()
        crc.update(actl.toByteArray(), 0, actl.size)
        res.addAll(Utils.uIntToByteArray(crc.value.toInt()).asList())
        return res
    }


    /**
     * Reduce the apng size
     * @param maxColor Max color you want in the image
     * @param keepCover Keep the cover
     * @param sizePercent Reduce image width/height by percents.
     */
    fun reduceSize(maxColor : Int, keepCover : Boolean? = null, sizePercent : Int? = null) {
        val apng = Apng()
        if (keepCover != false) {
            if (cover != null) {
                if (sizePercent != null) {
                    cover = Bitmap.createScaledBitmap(cover!!, (cover!!.width.toFloat() * sizePercent.toFloat() / 100f).toInt(), (cover!!.height.toFloat() * sizePercent.toFloat() / 100f).toInt(), false)
                    val pnn = PnnQuantizer(cover)
                    cover = pnn.convert(maxColor, false)
                }
            }
        } else {
            cover = null
        }
        frames.forEach {
            var btm = BitmapFactory.decodeByteArray(it.byteArray, 0, it.byteArray.size)
            if (sizePercent != null) {
                btm = Bitmap.createScaledBitmap(btm, (btm!!.width.toFloat() * sizePercent.toFloat() / 100f).toInt(), (btm.height.toFloat() * sizePercent.toFloat() / 100f).toInt(), false)
            }
            val pnn = PnnQuantizer(btm)
            val btmOptimised = pnn.convert(maxColor, false)
            if (sizePercent != null) {
                apng.addFrames(btmOptimised, 0, it.delay, (it.xOffsets.toFloat() * sizePercent.toFloat() / 100f).toInt(), (it.yOffsets.toFloat() * sizePercent.toFloat() / 100f).toInt(), it.disposeOp, it.blendOp)
            } else {
                apng.addFrames(btmOptimised, 0, it.delay, it.xOffsets, it.yOffsets, it.disposeOp, it.blendOp)
            }
        }
        frames = apng.frames
    }

    /**
     * A function to optimise Frame
     * WIP !
     */
    fun optimiseFrame() {
        maxHeight = frames.sortedByDescending { it.height }[0].height
        maxWidth = frames.sortedByDescending { it.width }[0].width
        frames.forEach {
            it.maxWidth = maxWidth
            it.maxHeight = maxHeight
        }
        val drawedFrame = ApngAnimator(null).draw(frames)
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "frame0.png").writeBytes(PngEncoder().encode(drawedFrame[0]))
        for (i in 1 until frames.size) {
            val diffCalculator = BitmapDiffCalculator(drawedFrame[i - 1], drawedFrame[i])
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "frame$i.png").writeBytes(PngEncoder().encode(diffCalculator.res, true))
            frames[i].byteArray = PngEncoder().encode(diffCalculator.res, true)
            frames[i].xOffsets = diffCalculator.xOffset
            frames[i].yOffsets = diffCalculator.yOffset
            frames[i].blendOp = Utils.Companion.BlendOp.APNG_BLEND_OP_OVER
        }
    }
}