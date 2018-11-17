package oupson.apng

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import oupson.apng.ImageUtils.PnnQuantizer
import oupson.apng.utils.Utils.Companion.getBlend_op
import oupson.apng.utils.Utils.Companion.getDispose_op
import oupson.apng.utils.Utils.Companion.pngSignature
import oupson.apng.utils.Utils.Companion.to2Bytes
import oupson.apng.utils.Utils.Companion.to4Bytes
import oupson.apng.utils.Utils.Companion.toByteArray
import oupson.apng.chunks.IDAT
import oupson.apng.exceptions.NoFrameException
import oupson.apng.utils.Utils
import java.util.zip.CRC32


/**
 * Create an APNG file
 */
class Apng {

    var maxWidth : Int? = null
    var maxHeight : Int? = null

    /**
     * Image that will display in non compatible reader
     * It's not necessary if the first frame is the biggest image.
     * If it's null the library generate a cover with the first frame
     */
    var cover : Bitmap? = null

    var frames : ArrayList<Frame> = ArrayList()

    // region addFrames

    /**
     * Add a frame to the APNG
     * @param bitmap The bitamp to add
     */
    fun addFrames(bitmap: Bitmap) {
        frames.add(Frame(toByteArray(bitmap)))
    }

    /**
     * Add a frame to the APNG
     * @param bitmap The bitamp to add
     * @param delay Delay of the frame
     */
    fun addFrames(bitmap: Bitmap, delay : Float) {
        frames.add(Frame(toByteArray(bitmap), delay))
    }

    /**
     * Add a frame to the APNG
     * @param bitmap The bitamp to add
     * @param delay Delay of the frame
     * @param dispose_op `dispose_op` specifies how the output buffer should be changed at the end of the delay (before rendering the next frame).
     * @param blend_op `blend_op` specifies whether the frame is to be alpha blended into the current output buffer content, or whether it should completely replace its region in the output buffer.
     */
    fun addFrames(bitmap: Bitmap, delay: Float, dispose_op: Utils.Companion.dispose_op, blend_op: Utils.Companion.blend_op) {
        frames.add(Frame(toByteArray(bitmap), delay, blend_op, dispose_op))
    }

    /**
     * Add a frame to the APNG
     * @param bitmap The bitamp to add
     * @param delay Delay of the frame
     * @param xOffset The X offset where the frame should be rendered
     * @param yOffset The Y offset where the frame should be rendered
     * @param dispose_op `dispose_op` specifies how the output buffer should be changed at the end of the delay (before rendering the next frame).
     * @param blend_op `blend_op` specifies whether the frame is to be alpha blended into the current output buffer content, or whether it should completely replace its region in the output buffer.
     */
    fun addFrames(bitmap: Bitmap, delay: Float, xOffset : Int, yOffset : Int, dispose_op: Utils.Companion.dispose_op, blend_op: Utils.Companion.blend_op) {
        frames.add(Frame(toByteArray(bitmap), delay, xOffset, yOffset, blend_op, dispose_op))
    }

    /**
     * Add a frame to the APNG
     * @param index Index where we add the frame
     * @param bitmap The bitamp to add
     */
    fun addFrames(index : Int, bitmap: Bitmap) {
        frames.add(index, Frame(toByteArray(bitmap)))
    }

    /**
     * Add a frame to the APNG
     * @param index Index where we add the frame
     * @param bitmap The bitamp to add
     * @param delay Delay of the frame
     */
    fun addFrames(index : Int, bitmap: Bitmap, delay : Float) {
        frames.add(index, Frame(toByteArray(bitmap), delay))
    }

    /**
     * Add a frame to the APNG
     * @param index Index where we add the frame
     * @param bitmap The bitamp to add
     * @param delay Delay of the frame
     * @param dispose_op `dispose_op` specifies how the output buffer should be changed at the end of the delay (before rendering the next frame).
     * @param blend_op `blend_op` specifies whether the frame is to be alpha blended into the current output buffer content, or whether it should completely replace its region in the output buffer.
     */
    fun addFrames(index: Int, bitmap: Bitmap, delay: Float, dispose_op: Utils.Companion.dispose_op, blend_op: Utils.Companion.blend_op) {
        frames.add(index, Frame(toByteArray(bitmap), delay, blend_op, dispose_op))
    }

    /**
     * Add a frame to the APNG
     * @param index Index where we add the frame
     * @param bitmap The bitamp to add
     * @param delay Delay of the frame
     * @param xOffset The X offset where the frame should be rendered
     * @param yOffset The Y offset where the frame should be rendered
     * @param dispose_op `dispose_op` specifies how the output buffer should be changed at the end of the delay (before rendering the next frame).
     * @param blend_op `blend_op` specifies whether the frame is to be alpha blended into the current output buffer content, or whether it should completely replace its region in the output buffer.
     */
    fun addFrames(index: Int, bitmap: Bitmap, delay: Float, xOffset : Int, yOffset : Int, dispose_op: Utils.Companion.dispose_op, blend_op: Utils.Companion.blend_op) {
        frames.add(index, Frame(toByteArray(bitmap), delay, xOffset, yOffset, blend_op, dispose_op))
    }

    fun addFrames(frame : Frame) {
        frames.add(frame)
    }

    fun addFrames(index: Int,frame : Frame) {
        frames.add(index, frame)
    }
    //endregion

    /**
     * Generate a Bytes Array of the APNG
     * @return The Bytes Array of the APNG
     */
    fun toByteArray() : ByteArray {
        var seq = 0
        val res = ArrayList<Byte>()
        // Add PNG signature
        res.addAll(pngSignature.toList())

        // Add Image Header
        res.addAll(generateIhdr().toList())

        // Add Animation Controller
        res.addAll(generateACTL())

        // Get max height and max width
        val maxHeight = frames.sortedByDescending { it.height }[0].height
        val maxWitdh = frames.sortedByDescending { it.width }[0].width

        if (cover == null) {
            val framesByte = ArrayList<Byte>()
            // region fcTL
            // Create the fcTL
            val fcTL = ArrayList<Byte>()

            // Add the length of the chunk body
            framesByte.addAll(byteArrayOf(0x00, 0x00, 0x00, 0x1A).toList())

            // Add fcTL
            fcTL.addAll(byteArrayOf(0x66, 0x63, 0x54, 0x4c).toList())

            // Add the frame number
            fcTL.addAll(to4Bytes(seq).toList())
            // foreach fcTL or fdAT we must increment seq
            seq++

            // Add width and height
            fcTL.addAll(to4Bytes(frames[0].width).toList())
            fcTL.addAll(to4Bytes(frames[0].height).toList())

            // Calculate offset
            if (frames[0].x_offsets == null) {
                if (frames[0].width < maxWitdh) {
                    val xOffset = (maxWitdh / 2) - (frames[0].width / 2)
                    fcTL.addAll(to4Bytes(xOffset).toList())
                } else {
                    fcTL.addAll(to4Bytes(0).toList())
                }
                if (frames[0].height < maxHeight) {
                    val xOffset = (maxHeight / 2) - (frames[0].height / 2)
                    fcTL.addAll(to4Bytes(xOffset).toList())
                } else {
                    fcTL.addAll(to4Bytes(0).toList())
                }
            } else {
                fcTL.addAll(to4Bytes(frames[0].x_offsets!!).toList())
                fcTL.addAll(to4Bytes(frames[0].y_offsets!!).toList())
            }

            // Set frame delay
            fcTL.addAll(to2Bytes(frames[0].delay.toInt()).toList())
            fcTL.addAll(to2Bytes(1000).toList())

            // Add dispose_op and blend_op
            fcTL.add(getDispose_op(frames[0].dispose_op).toByte())
            fcTL.add(getBlend_op(frames[0].blend_op).toByte())

            // Create CRC
            val crc = CRC32()
            crc.update(fcTL.toByteArray(), 0, fcTL.size)
            framesByte.addAll(fcTL)
            framesByte.addAll(to4Bytes(crc.value.toInt()).toList())
            // endregion

            // region idat
            frames[0].idat.IDATBody.forEach {
                val idat = ArrayList<Byte>()
                // Add the chunk body length
                framesByte.addAll(to4Bytes(it.size).toList())
                // Add IDAT
                idat.addAll(byteArrayOf(0x49, 0x44, 0x41, 0x54).toList())
                // Add chunk body
                idat.addAll(it.toList())
                // Generate CRC
                val crc1 = CRC32()
                crc1.update(idat.toByteArray(), 0, idat.size)
                framesByte.addAll(idat)
                framesByte.addAll(to4Bytes(crc1.value.toInt()).toList())
            }
            // endregion
            res.addAll(framesByte)
        } else {
            val framesByte = ArrayList<Byte>()
            // Add cover image : Not part of animation
            // region IDAT
            val idat = IDAT()
            idat.parseIDAT(toByteArray(cover!!))
            idat.IDATBody.forEach {
                val idatByteArray = ArrayList<Byte>()
                framesByte.addAll(to4Bytes(it.size).toList())
                idatByteArray.addAll(byteArrayOf(0x49, 0x44, 0x41, 0x54).toList())
                idatByteArray.addAll(it.toList())
                val crc1 = CRC32()
                crc1.update(idatByteArray.toByteArray(), 0, idatByteArray.size)
                framesByte.addAll(idatByteArray)
                framesByte.addAll(to4Bytes(crc1.value.toInt()).toList())
            }
            // endregion

            //region fcTL
            val fcTL = ArrayList<Byte>()
            // Add the length of the chunk body
            framesByte.addAll(byteArrayOf(0x00, 0x00, 0x00, 0x1A).toList())
            // Add fcTL
            fcTL.addAll(byteArrayOf(0x66, 0x63, 0x54, 0x4c).toList())

            // Add the frame number
            fcTL.addAll(to4Bytes(seq).toList())
            seq++

            // Add width and height
            fcTL.addAll(to4Bytes(frames[0].width).toList())
            fcTL.addAll(to4Bytes(frames[0].height).toList())

            // Calculate offset
            if (frames[0].x_offsets == null) {
                if (frames[0].width < maxWitdh) {
                    val xOffset = (maxWitdh / 2) - (frames[0].width / 2)
                    fcTL.addAll(to4Bytes(xOffset).toList())
                } else {
                    fcTL.addAll(to4Bytes(0).toList())
                }
                if (frames[0].height < maxHeight) {
                    val xOffset = (maxHeight / 2) - (frames[0].height / 2)
                    fcTL.addAll(to4Bytes(xOffset).toList())
                } else {
                    fcTL.addAll(to4Bytes(0).toList())
                }
            } else {
                fcTL.addAll(to4Bytes(frames[0].x_offsets!!).toList())
                fcTL.addAll(to4Bytes(frames[0].y_offsets!!).toList())
            }

            // Set frame delay
            fcTL.addAll(to2Bytes(frames[0].delay.toInt()).toList())
            fcTL.addAll(to2Bytes(1000).toList())

            // Add dispose_op and blend_op
            fcTL.add(getDispose_op(frames[0].dispose_op).toByte())
            fcTL.add(getBlend_op(frames[0].blend_op).toByte())

            // Generate CRC
            val crc = CRC32()
            crc.update(fcTL.toByteArray(), 0, fcTL.size)
            framesByte.addAll(fcTL)
            framesByte.addAll(to4Bytes(crc.value.toInt()).toList())
            // endregion

            // region fdat
            frames[0].idat.IDATBody.forEach {
                val fdat = ArrayList<Byte>()
                // Add the chunk body length
                framesByte.addAll(to4Bytes(it.size + 4).toList())
                // Add fdat
                fdat.addAll(byteArrayOf(0x66, 0x64, 0x41, 0x54).toList())
                fdat.addAll(to4Bytes(seq).toList())
                seq++
                // Add chunk body
                fdat.addAll(it.toList())
                // Generate CRC
                val crc1 = CRC32()
                crc1.update(fdat.toByteArray(), 0, fdat.size)
                framesByte.addAll(fdat)
                framesByte.addAll(to4Bytes(crc1.value.toInt()).toList())
            }
            // endregion
            res.addAll(framesByte)
        }

        for (i in 1 until frames.size) {
            // If it's the first frame
            val framesByte = ArrayList<Byte>()
            val fcTL = ArrayList<Byte>()
            // region fcTL
            framesByte.addAll(byteArrayOf(0x00, 0x00, 0x00, 0x1A).toList())

            fcTL.addAll(byteArrayOf(0x66, 0x63, 0x54, 0x4c).toList())

            // Frame number
            fcTL.addAll(to4Bytes(seq).toList())
            seq++
            // width and height
            fcTL.addAll(to4Bytes(frames[i].width).toList())
            fcTL.addAll(to4Bytes(frames[i].height).toList())

            if (frames[i].x_offsets == null) {
                if (frames[i].width < maxWitdh) {
                    val xOffset = (maxWitdh / 2) - (frames[i].width / 2)
                    fcTL.addAll(to4Bytes(xOffset).toList())
                } else {
                    fcTL.addAll(to4Bytes(0).toList())
                }
                if (frames[i].height < maxHeight) {
                    val xOffset = (maxHeight / 2) - (frames[i].height / 2)
                    fcTL.addAll(to4Bytes(xOffset).toList())
                } else {
                    fcTL.addAll(to4Bytes(0).toList())
                }
            } else {
                fcTL.addAll(to4Bytes(frames[i].x_offsets!!).toList())
                fcTL.addAll(to4Bytes(frames[i].y_offsets!!).toList())
            }

            // Set frame delay
            fcTL.addAll(to2Bytes(frames[i].delay.toInt()).toList())
            fcTL.addAll(to2Bytes(1000).toList())

            fcTL.add(getDispose_op(frames[i].dispose_op).toByte())
            fcTL.add(getBlend_op(frames[i].blend_op).toByte())

            val crc = CRC32()
            crc.update(fcTL.toByteArray(), 0, fcTL.size)
            framesByte.addAll(fcTL)
            framesByte.addAll(to4Bytes(crc.value.toInt()).toList())
            // endregion

            // region fdAT
            // Write fdAT
            frames[i].idat.IDATBody.forEach {
                val fdat = ArrayList<Byte>()
                // Add IDAT size of frame + 4 byte of the seq
                framesByte.addAll(to4Bytes(it.size + 4).toList())
                // Add fdAT
                fdat.addAll(byteArrayOf(0x66, 0x64, 0x41, 0x54).toList())
                // Add Sequence number
                // ! THIS IS NOT FRAME NUMBER
                fdat.addAll(to4Bytes(seq).toList())
                // Increase seq
                seq++
                fdat.addAll(it.toList())
                // Generate CRC
                val crc1 = CRC32()
                crc1.update(fdat.toByteArray(), 0, fdat.size)
                framesByte.addAll(fdat)
                framesByte.addAll(to4Bytes(crc1.value.toInt()).toList())
            }
            // endregion
            res.addAll(framesByte)
        }
        if (frames.isNotEmpty()) {

            // Add IEND body length : 0
            res.addAll(to4Bytes(0).toList())
            // Add IEND
            val iend = byteArrayOf(0x49, 0x45, 0x4E, 0x44)
            // Generate crc for IEND
            val crC32 = CRC32()
            crC32.update(iend, 0, iend.size)
            res.addAll(iend.toList())
            res.addAll(to4Bytes(crC32.value.toInt()).toList())
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
     * @return An image cover
     */
    fun generateCover(bitmap: Bitmap, maxWidth : Int, maxHeight : Int) : Bitmap {
        return Bitmap.createScaledBitmap(bitmap, maxWidth, maxHeight, false)
    }

    /**
     * Generate the IHDR chunks.
     * @return The byteArray generated
     */
    private fun generateIhdr(): ByteArray {
        val ihdr = ArrayList<Byte>()

        // We need a body var to know body length and generate crc
        val ihdr_body = ArrayList<Byte>()

        // Get max height and max width of all the frames
        maxHeight = frames.sortedByDescending { it.height }[0].height
        maxWidth = frames.sortedByDescending { it.width }[0].width

        if (((maxWidth != frames[0].width) && (maxHeight != frames[0].height)) && cover == null) {
            cover = generateCover(BitmapFactory.decodeByteArray(frames[0].byteArray, 0, frames[0].byteArray.size), maxWidth!!, maxHeight!!)
        }

        // Add chunk body length
        ihdr.addAll(to4Bytes(frames[0].ihdr.ihdrCorps.size).toList())
        // Add IHDR
        ihdr_body.addAll(byteArrayOf(0x49.toByte(), 0x48.toByte(), 0x44.toByte(), 0x52.toByte()).toList())

        // Add the max width and height
        ihdr_body.addAll(to4Bytes(maxWidth!!).toList())
        ihdr_body.addAll(to4Bytes(maxHeight!!).toList())

        // Add complicated stuff like depth color ...
        // If you want correct png you need same parameters. Good solution is to create new png.
        ihdr_body.addAll(frames[0].ihdr.ihdrCorps.copyOfRange(8, 13).toList())

        // Generate CRC
        val crC32 = CRC32()
        crC32.update(ihdr_body.toByteArray(), 0, ihdr_body.size)
        ihdr.addAll(ihdr_body)
        ihdr.addAll(to4Bytes(crC32.value.toInt()).toList())
        return ihdr.toByteArray()
    }

    /**
     * Generate the animation control chunk
     * @return The byteArray generated
     */
    private fun generateACTL(): ArrayList<Byte> {
        val res = ArrayList<Byte>()
        val actl = ArrayList<Byte>()

        // Add length bytes
        res.addAll(to4Bytes(8).toList())

        // Add acTL
        actl.addAll(byteArrayOf(0x61, 0x63, 0x54, 0x4c).toList())

        // Add number of frames
        actl.addAll(to4Bytes(frames.size).toList())

        // Number of repeat, 0 to infinite
        actl.addAll(to4Bytes(0).toList())
        res.addAll(actl)

        // generate crc
        val crc = CRC32()
        crc.update(actl.toByteArray(), 0, actl.size)
        res.addAll(to4Bytes(crc.value.toInt()).toList())
        return res
    }


    /**
     * Reduce the apng size
     * @param maxColor Max color you want in the image
     * @param keepCover Keep the cover
     * @param sizePercent Reduce image width/height by percents.
     */
    fun reduceSize( maxColor : Int, keepCover : Boolean? = null, sizePercent : Int? = null) {
        val apng = Apng()
        if (keepCover != false) {
            if (cover != null) {
                if (sizePercent != null) {
                    cover = Bitmap.createScaledBitmap(cover, (cover!!.width.toFloat() * sizePercent.toFloat() / 100f).toInt(), (cover!!.height.toFloat() * sizePercent.toFloat() / 100f).toInt(), false)
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
                apng.addFrames(btmOptimised, it.delay, ((it.x_offsets ?: 0).toFloat() * sizePercent.toFloat() / 100f).toInt(), ((it.y_offsets ?: 0).toFloat() * sizePercent.toFloat() / 100f).toInt(), it.dispose_op, it.blend_op)
            } else {
                apng.addFrames(btmOptimised, it.delay, it.x_offsets ?: 0, it.y_offsets ?: 0, it.dispose_op, it.blend_op)
            }
        }
        frames = apng.frames
    }
}