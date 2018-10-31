package oupson.apng

import oupson.apng.Utils.Companion.pngSignature
import oupson.apng.exceptions.NoFrameException
import java.util.zip.CRC32


/**
 *  APNG is a class for create apng
 *
 *  Call .addFrame() to add a Frame
 *  Call .create to get the generated file
 *
 *  @author oupson
 *
 *  @throws NotPngException
 *  @throws NoFrameException
 *
 */
class ApngFactory {

    private var seq = 0

    var frames = ArrayList<Frame>()

    /**
     * @return a byte array of the generated png
     *
     * @throws NoFrameException
     */
    fun create(): ByteArray {

        if (frames.isNotEmpty()) {
            val res = ArrayList<Byte>()

            // Add PNG signature
            res.addAll(pngSignature.toList())

            // Add Image Header
            res.addAll(generate_ihdr().toList())

            // Add Animation Controller
            res.addAll(generateACTL())

            // Get max height and max width
            val maxHeight = frames.sortedByDescending { it.height }[0].height
            val maxWitdh = frames.sortedByDescending { it.width }[0].width

            for (i in 0 until frames.size) {

                // If it's the first frame
                if (i == 0) {
                    val framesByte = ArrayList<Byte>()
                    // region fcTL
                    // Create the fcTL
                    val fcTL = ArrayList<Byte>()
                    // Add the length of the chunk body
                    framesByte.addAll(byteArrayOf(0x00, 0x00, 0x00, 0x1A).toList())

                    // Add acTL
                    fcTL.addAll(byteArrayOf(0x66, 0x63, 0x54, 0x4c).toList())

                    // Add the frame number
                    fcTL.addAll(to4Bytes(seq).toList())
                    seq++

                    // Add width and height
                    fcTL.addAll(to4Bytes(frames[i].width).toList())
                    fcTL.addAll(to4Bytes(frames[i].height).toList())

                    // Calculate offset
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

                    // Set frame delay
                    fcTL.addAll(to2Bytes(frames[i].delay.toInt()).toList())
                    fcTL.addAll(to2Bytes(1000).toList())

                    fcTL.add(0x01)
                    fcTL.add(0x00)

                    val crc = CRC32()
                    crc.update(fcTL.toByteArray(), 0, fcTL.size)
                    framesByte.addAll(fcTL)
                    framesByte.addAll(to4Bytes(crc.value.toInt()).toList())
                    // endregion

                    // region idat
                    frames[i].idat.IDATBody.forEach {
                        val fdat = ArrayList<Byte>()
                        // Add the chunk body length
                        framesByte.addAll(to4Bytes(it.size).toList())
                        // Add IDAT
                        fdat.addAll(byteArrayOf(0x49, 0x44, 0x41, 0x54).toList())
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
                } else {
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

                    // Set frame delay
                    fcTL.addAll(to2Bytes(frames[i].delay.toInt()).toList())
                    fcTL.addAll(to2Bytes(1000).toList())

                    fcTL.add(0x01)
                    fcTL.add(0x00)

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
            }
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

    // Animation Control chunk
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
     * Generate a 4 bytes array from an Int
     * @param i The int
     */
    fun to4Bytes(i: Int): ByteArray {
        val result = ByteArray(4)
        result[0] = (i shr 24).toByte()
        result[1] = (i shr 16).toByte()
        result[2] = (i shr 8).toByte()
        result[3] = i /*>> 0*/.toByte()
        return result
    }

    /**
     * Generate a 2 bytes array from an Int
     * @param i The int
     */
    fun to2Bytes(i: Int): ByteArray {
        val result = ByteArray(2)
        result[0] = (i shr 8).toByte()
        result[1] = i /*>> 0*/.toByte()
        return result
    }


    /**
     * Add a frame to the Animated PNG
     *
     * @param byteArray It's the byteArray of a png
     * @param delay Delay in MS between this frame and the next
     */
    fun addFrame(byteArray: ByteArray, delay: Float = 1000f) {
        frames.add(Frame(byteArray, delay))
    }

    // Generate Image Header chunk
    private fun generate_ihdr(): ByteArray {
        val ihdr = ArrayList<Byte>()

        // We need a body var to know body length and generate crc
        val ihdr_body = ArrayList<Byte>()

        // Get max height and max width of all the frames
        val maxHeight = frames.sortedByDescending { it.height }[0].height
        val maxWitdh = frames.sortedByDescending { it.width }[0].width

        // Add chunk body length
        ihdr.addAll(to4Bytes(frames[0].ihdr.ihdrCorps.size).toList())
        // Add IHDR
        ihdr_body.addAll(byteArrayOf(0x49.toByte(), 0x48.toByte(), 0x44.toByte(), 0x52.toByte()).toList())

        // Add the max width and height
        ihdr_body.addAll(to4Bytes(maxWitdh).toList())
        ihdr_body.addAll(to4Bytes(maxHeight).toList())

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
}