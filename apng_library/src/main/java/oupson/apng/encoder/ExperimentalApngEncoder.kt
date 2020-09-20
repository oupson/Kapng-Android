package oupson.apng.encoder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import oupson.apng.exceptions.InvalidFrameSize
import oupson.apng.utils.Utils
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import kotlin.math.max
import kotlin.math.min

// TODO ADD SUPPORT FOR FIRST FRAME NOT IN ANIM
// TODO OPTIMISE APNG
/**
 * A class to write APNG
 * @param outputStream The ouput stream.
 * @param width Width of the animation
 * @param height Height of the animation
 * @param numberOfFrames The number of frame the animation is composed.
 * @throws IOException If something failed when writing into the output stream.
 *
 * Usage :
 *  - Instantiate the class.
 *  - Call [writeFrame].
 *  - Call [writeEnd] at the end of the animation.
 */
class ExperimentalApngEncoder(
    private val outputStream: OutputStream,
    private val width: Int,
    private val height: Int,
    numberOfFrames: Int
) {
    companion object {
        private const val TAG = "ExperimentalApngEncoder"

        /** Constants for filter (NONE)  */
        const val FILTER_NONE = 0

        /** Constants for filter (SUB)  */
        const val FILTER_SUB = 1

        /** Constants for filter (UP)  */
        const val FILTER_UP = 2

        /** Constants for filter (LAST)  */
        const val FILTER_LAST = 2
    }

    /** Current Frame.  **/
    private var currentFrame = 0

    /**
     * Current sequence of the animation.
     * @see [https://developer.mozilla.org/en-US/docs/Mozilla/Tech/APNG#Chunk_sequence_numbers](Mozilla documentation.)
     **/
    private var currentSeq = 0

    /** CRC.  */
    private var crc = CRC32()

    /** The CRC value.  */
    private var crcValue: Long = 0

    /** The bytes-per-pixel.  */
    private var bytesPerPixel: Int = 0

    /** The compression level.  */
    private var compressionLevel: Int = 0

    /** The filter type.  */
    private var filter: Int = FILTER_NONE

    /** If the alpha channel must be encoded    */
    private var encodeAlpha: Boolean = true

    /** The prior row.  */
    private var priorRow: ByteArray? = null

    /** The left bytes.  */
    private var leftBytes: ByteArray? = null

    /** Number of loop of the animation, zero to infinite **/
    private var repetitionCount: Int = 0

    init {
        outputStream.write(Utils.pngSignature)
        writeHeader()
        writeACTL(numberOfFrames)
    }

    /**
     * Set if the alpha channel must be encoded.
     * @param encodeAlpha If the alpha channel must be encoded.
     * @return [ExperimentalApngEncoder] for chaining.
     */
    @Suppress("unused")
    fun encodeAlpha(encodeAlpha: Boolean): ExperimentalApngEncoder {
        this.encodeAlpha = encodeAlpha
        return this
    }

    /**
     * Set the filter.
     * @param filter The filter.
     * Values :
     * - [FILTER_NONE]
     * - [FILTER_SUB]
     * - [FILTER_UP]
     * - [FILTER_LAST]
     * @return [ExperimentalApngEncoder] for chaining.
     */
    @Suppress("unused")
    fun filter(filter: Int): ExperimentalApngEncoder {
        if (filter <= FILTER_LAST) {
            this.filter = filter
        } else {
            Log.e(TAG, "Invalid filter")
        }
        return this
    }

    /**
     * Set the repetition count.
     * @param repetitionCount The number of repetition, zero for infinite.
     * @return [ExperimentalApngEncoder] for chaining.
     */
    @Suppress("unused")
    fun repetitionCount(repetitionCount: Int): ExperimentalApngEncoder {
        this.repetitionCount = repetitionCount
        return this
    }

    /**
     * Set the compression level
     * @param compressionLevel A integer between 0 and 9 (not include)
     * @return [ExperimentalApngEncoder] for chaining
     */
    fun compressionLevel(compressionLevel: Int): ExperimentalApngEncoder {
        if (compressionLevel in 0..9) {
            this.compressionLevel = compressionLevel
        } else {
            Log.e(
                TAG,
                "Invalid compression level : $compressionLevel, expected a number in range 0..9"
            )
        }

        return this
    }

    /**
     * Write a frame into the output stream.
     * @param inputStream An input stream that will be decoded in order to be written in the animation. Not freed.
     * @param delay A delay in ms.
     * @param xOffsets The offset of the left bound of the frame in the animation.
     * @param yOffsets The offset of the top bound of the frame in the animation.
     * @param blendOp See [Utils.BlendOp].
     * @param disposeOp See [Utils.DisposeOp].
     * @throws NullPointerException If the bitmap failed to be decoded
     * @throws InvalidFrameSize If the frame size is bigger than the animation size, or the first frame size is not equal to the animation size.
     * @throws IOException If something failed when writing into the output stream.
     */
    @JvmOverloads
    @Throws(NullPointerException::class, InvalidFrameSize::class, InvalidFrameSize::class, IOException::class)
    fun writeFrame(
        inputStream: InputStream,
        delay: Float = 1000f,
        xOffsets: Int = 0,
        yOffsets: Int = 0,
        blendOp: Utils.Companion.BlendOp = Utils.Companion.BlendOp.APNG_BLEND_OP_SOURCE,
        disposeOp: Utils.Companion.DisposeOp = Utils.Companion.DisposeOp.APNG_DISPOSE_OP_NONE
    ) {
        val btm = BitmapFactory.decodeStream(inputStream)!!

        writeFrame(btm, delay, xOffsets, yOffsets, blendOp, disposeOp)
        btm.recycle()
    }

    /**
     * Write a frame into the output stream.
     * @param btm An bitmap that will be written in the animation.
     * @param delay A delay in ms.
     * @param xOffsets The offset of the left bound of the frame in the animation.
     * @param yOffsets The offset of the top bound of the frame in the animation.
     * @param blendOp See [Utils.BlendOp].
     * @param disposeOp See [Utils.DisposeOp].
     * @throws InvalidFrameSize If the frame size is bigger than the animation size, or the first frame size is not equal to the animation size.
     * @throws IOException If something failed when writing into the output stream.
     */
    @JvmOverloads
    @Throws(InvalidFrameSize::class, IOException::class)
    fun writeFrame(
        btm: Bitmap,
        delay: Float = 1000f,
        xOffsets: Int = 0,
        yOffsets: Int = 0,
        blendOp: Utils.Companion.BlendOp = Utils.Companion.BlendOp.APNG_BLEND_OP_SOURCE,
        disposeOp: Utils.Companion.DisposeOp = Utils.Companion.DisposeOp.APNG_DISPOSE_OP_NONE
    ) {
        if (currentFrame == 0) {
            if (btm.width != width)
                throw InvalidFrameSize("Width of first frame must be equal to width of APNG. (${btm.width} != $width)")
            if (btm.height != height)
                throw InvalidFrameSize("Height of first frame must be equal to height of APNG. (${btm.height} != $height)")
        }

        if (btm.width > width)
            throw InvalidFrameSize("Frame width must be inferior or equal at the animation width")
        else if (btm.height > height)
            throw InvalidFrameSize("Frame height must be inferior or equal at the animation height")

        writeFCTL(btm, delay, disposeOp, blendOp, xOffsets, yOffsets)
        writeImageData(btm)
        currentSeq++
    }

    /**
     * Write the end of the animation.
     * @throws IOException If something failed when writing into the output stream.
     */
    @Throws(IOException::class)
    fun writeEnd() {
        // Add IEND body length : 0
        outputStream.write(Utils.to4BytesArray(0))
        // Add IEND
        val iend = byteArrayOf(0x49, 0x45, 0x4E, 0x44)
        // Generate crc for IEND
        crc.reset()
        crc.update(iend, 0, iend.size)
        outputStream.write(iend)
        outputStream.write(Utils.to4BytesArray(crc.value.toInt()))
    }

    /**
     * Write the header into the outputStream.
     * @throws IOException If something failed when writing into the output stream.
     */
    @Throws(IOException::class)
    private fun writeHeader() {
        writeInt4(13)
        val header = arrayListOf<Byte>()
        header.addAll(Utils.IHDR.asList())
        header.addAll(Utils.to4Bytes(width))
        header.addAll(Utils.to4Bytes(height))
        header.add(8) // bit depth
        header.add(if (encodeAlpha) 6 else 2) // direct model
        header.add(0) // compression method
        header.add(0) // filter method
        header.add(0) // no interlace
        outputStream.write(
            header.toByteArray()
        )
        crc.reset()
        crc.update(header.toByteArray())
        crcValue = crc.value
        writeInt4(crcValue.toInt())
    }

    /**
     * Write a four-byte integer into the outputStream.
     * @param n The four-byte integer to write.
     * @throws IOException If something failed when writing into the output stream.
     */
    @Throws(IOException::class)
    private fun writeInt4(n: Int) {
        val temp = byteArrayOf(
            (n shr 24 and 0xff).toByte(),
            (n shr 16 and 0xff).toByte(),
            (n shr 8 and 0xff).toByte(),
            (n and 0xff).toByte()
        )
        outputStream.write(temp)
    }

    /**
     * Write the animation control chunk into the outputStream.
     * @param num The number of frame the animation contain.
     * @throws IOException If something failed when writing into the output stream.
     */
    @Throws(IOException::class)
    private fun writeACTL(num: Int) {
        val actl = ArrayList<Byte>()

        // Add length bytes
        outputStream.write(byteArrayOf(0, 0, 0, 0x08))

        // Add acTL
        actl.addAll(byteArrayOf(0x61, 0x63, 0x54, 0x4c).asList())

        // Add number of frames
        actl.addAll(Utils.to4Bytes(num).asList())

        // Number of repeat, 0 to infinite
        actl.addAll(Utils.to4Bytes(repetitionCount).asList())
        outputStream.write(actl.toByteArray())

        // generate crc
        crc.reset()
        crc.update(actl.toByteArray(), 0, actl.size)
        outputStream.write(Utils.to4BytesArray(crc.value.toInt()))
    }

    /**
     * Write the frame control chunk into the outputStream.
     * @throws IOException If something failed when writing into the output stream.
     */
    @Throws(IOException::class)
    private fun writeFCTL(
        btm: Bitmap,
        delay: Float,
        disposeOp: Utils.Companion.DisposeOp,
        blendOp: Utils.Companion.BlendOp,
        xOffsets: Int,
        yOffsets: Int
    ) {
        val fcTL = ArrayList<Byte>()

        // Add the length of the chunk body
        outputStream.write(byteArrayOf(0x00, 0x00, 0x00, 0x1A))

        // Add fcTL
        fcTL.addAll(Utils.fcTL.asList())

        // Add the frame number
        fcTL.addAll(Utils.to4Bytes(currentSeq++))

        // Add width and height
        fcTL.addAll(Utils.to4Bytes(btm.width))
        fcTL.addAll(Utils.to4Bytes(btm.height))

        // Add offsets
        fcTL.addAll(Utils.to4Bytes(xOffsets))
        fcTL.addAll(Utils.to4Bytes(yOffsets))

        // Set frame delay
        fcTL.addAll(Utils.to2Bytes(delay.toInt()).asList())
        fcTL.addAll(Utils.to2Bytes(1000).asList())

        // Add DisposeOp and BlendOp
        fcTL.add(Utils.getDisposeOp(disposeOp).toByte())
        fcTL.add(Utils.getBlendOp(blendOp).toByte())

        // Create CRC
        crc.reset()
        crc.update(fcTL.toByteArray(), 0, fcTL.size)

        // Write all
        outputStream.write(fcTL.toByteArray())
        outputStream.write(Utils.to4BytesArray(crc.value.toInt()))
    }

    /**
     * Write the image data into the ouputStrea.
     * This will write one or more PNG "IDAT"/"fdAT" chunks. In order
     * to conserve memory, this method grabs as many rows as will
     * fit into 32K bytes, or the whole image; whichever is less.
     *
     *
     * @return true if no errors; false if error grabbing pixels
     */
    private fun writeImageData(image: Bitmap): Boolean {
        var rowsLeft = height  // number of rows remaining to write
        var startRow = 0       // starting row to process this time through
        var nRows: Int              // how many rows to grab at a time

        var scanLines: ByteArray       // the scan lines to be compressed
        var scanPos: Int            // where we are in the scan lines
        var startPos: Int           // where this line's actual pixels start (used for filtering)

        val compressedLines: ByteArray // the resultant compressed lines
        val nCompressed: Int        // how big is the compressed area?

        //int depth;              // color depth ( handle only 8 or 32 )

        bytesPerPixel = if (encodeAlpha) 4 else 3

        val scrunch = Deflater(compressionLevel)
        val outBytes = ByteArrayOutputStream(1024)

        val compBytes = DeflaterOutputStream(outBytes, scrunch)
        try {
            while (rowsLeft > 0) {
                nRows = min(32767 / (width * (bytesPerPixel + 1)), rowsLeft)
                nRows = max(nRows, 1)

                val pixels = IntArray(width * nRows)

                //pg = new PixelGrabber(image, 0, startRow, width, nRows, pixels, 0, width);
                image.getPixels(pixels, 0, width, 0, startRow, width, nRows)

                /*
                * Create a data chunk. scanLines adds "nRows" for
                * the filter bytes.
                */
                scanLines = ByteArray(width * nRows * bytesPerPixel + nRows)

                if (filter == FILTER_SUB) {
                    leftBytes = ByteArray(16)
                }
                if (filter == FILTER_UP) {
                    priorRow = ByteArray(width * bytesPerPixel)
                }

                scanPos = 0
                startPos = 1
                for (i in 0 until width * nRows) {
                    if (i % width == 0) {
                        scanLines[scanPos++] = filter.toByte()
                        startPos = scanPos
                    }
                    scanLines[scanPos++] = (pixels[i] shr 16 and 0xff).toByte()
                    scanLines[scanPos++] = (pixels[i] shr 8 and 0xff).toByte()
                    scanLines[scanPos++] = (pixels[i] and 0xff).toByte()
                    if (encodeAlpha) {
                        scanLines[scanPos++] = (pixels[i] shr 24 and 0xff).toByte()
                    }
                    if (i % width == width - 1 && filter != FILTER_NONE) {
                        if (filter == FILTER_SUB) {
                            filterSub(scanLines, startPos, width)
                        }
                        if (filter == FILTER_UP) {
                            filterUp(scanLines, startPos, width)
                        }
                    }
                }

                /*
                * Write these lines to the output area
                */
                compBytes.write(scanLines, 0, scanPos)

                startRow += nRows
                rowsLeft -= nRows
            }
            compBytes.close()

            /*
            * Write the compressed bytes
            */
            compressedLines = outBytes.toByteArray()
            nCompressed = compressedLines.size

            crc.reset()
            // Add 4 bytes to the length, for the sequence number
            writeInt4(nCompressed + if (currentFrame == 0) 0 else 4)

            if (currentFrame == 0) {
                outputStream.write(Utils.IDAT)
                crc.update(Utils.IDAT)
            } else {
                val fdat = ArrayList<Byte>().also { fdat ->
                    // Add fdat
                    fdat.addAll(byteArrayOf(0x66, 0x64, 0x41, 0x54).asList())
                    // Add the sequence number
                    fdat.addAll(Utils.to4Bytes(currentSeq++).asList())
                }.toByteArray()

                outputStream.write(fdat)
                crc.update(fdat)
            }
            //bytePos = writeBytes(compressedLines, nCompressed, bytePos)
            outputStream.write(compressedLines)
            crc.update(compressedLines, 0, nCompressed)

            crcValue = crc.value
            writeInt4(crcValue.toInt())
            scrunch.finish()
            scrunch.end()
            return true
        } catch (e: IOException) {
            Log.e(TAG, "Error while writing IDAT/fdAT chunks", e)
            return false
        }
    }

    /**
     * Perform "sub" filtering on the given row.
     * Uses temporary array leftBytes to store the original values
     * of the previous pixels.  The array is 16 bytes long, which
     * will easily hold two-byte samples plus two-byte alpha.
     *
     * @param pixels The array holding the scan lines being built
     * @param startPos Starting position within pixels of bytes to be filtered.
     * @param width Width of a scanline in pixels.
     */
    private fun filterSub(pixels: ByteArray, startPos: Int, width: Int) {
        val offset = bytesPerPixel
        val actualStart = startPos + offset
        val nBytes = width * bytesPerPixel
        var leftInsert = offset
        var leftExtract = 0
        var i: Int = actualStart
        while (i < startPos + nBytes) {
            leftBytes!![leftInsert] = pixels[i]
            pixels[i] = ((pixels[i] - leftBytes!![leftExtract]) % 256).toByte()
            leftInsert = (leftInsert + 1) % 0x0f
            leftExtract = (leftExtract + 1) % 0x0f
            i++
        }
    }

    /**
     * Perform "up" filtering on the given row.
     * Side effect: refills the prior row with current row
     *
     * @param pixels The array holding the scan lines being built
     * @param startPos Starting position within pixels of bytes to be filtered.
     * @param width Width of a scanline in pixels.
     */
    private fun filterUp(pixels: ByteArray, startPos: Int, width: Int) {
        var i = 0
        val nBytes: Int = width * bytesPerPixel
        var currentByte: Byte
        while (i < nBytes) {
            currentByte = pixels[startPos + i]
            pixels[startPos + i] = ((pixels[startPos + i] - priorRow!![i]) % 256).toByte()
            priorRow!![i] = currentByte
            i++
        }
    }
}