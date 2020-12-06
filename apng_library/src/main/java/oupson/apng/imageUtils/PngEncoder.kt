package oupson.apng.imageUtils

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Taken from http://catcode.com/pngencoder/com/keypoint/PngEncoder.java
 */
class PngEncoder {
    companion object {
        /** Constants for filter (NONE)  */
        private const val FILTER_NONE = 0

        /** Constants for filter (SUB)  */
        private const val FILTER_SUB = 1

        /** Constants for filter (UP)  */
        private const val FILTER_UP = 2

        /** Constants for filter (LAST)  */
        private const val FILTER_LAST = 2
    }
    /** Encode alpha ?  */
    private var encodeAlpha = true

    /** IHDR tag.  */
    private val ihdr = byteArrayOf(73, 72, 68, 82)

    /** IDAT tag.  */
    private val idat = byteArrayOf(73, 68, 65, 84)

    /** IEND tag.  */
    private val iend = byteArrayOf(73, 69, 78, 68)

    /** The image.  */
    private var image: Bitmap? = null

    /** The png bytes.  */
    private var pngBytes: ByteArray? = null

    /** The prior row.  */
    private var priorRow: ByteArray? = null

    /** The left bytes.  */
    private var leftBytes: ByteArray? = null

    /** The width.  */
    private var width: Int = 0
    private var height: Int = 0

    /** The byte position.  */
    private var bytePos: Int = 0
    private var maxPos: Int = 0

    /** CRC.  */
    private var crc = CRC32()

    /** The CRC value.  */
    private var crcValue: Long = 0

    /** The filter type.  */
    private var filter: Int = 0

    /** The bytes-per-pixel.  */
    private var bytesPerPixel: Int = 0

    /** The compression level.  */
    private var compressionLevel: Int = 0

    /**
     * Encode a [Bitmap] into a png
     *
     * @param image Bitmap to encode
     * @param encodeAlpha Specify if the alpha should be encoded or not
     * @param filter 0=none, 1=sub, 2=up
     * @param compressionLevel ! Don't use it : It's buggy
     */
    fun encode(image: Bitmap, encodeAlpha: Boolean = false, filter: Int = 0, compressionLevel: Int = 0): ByteArray {
        this.filter = FILTER_NONE
        if (filter <= FILTER_LAST) {
            this.filter = filter
        }

        if (compressionLevel in 0..9) {
            this.compressionLevel = compressionLevel
        }

        this.encodeAlpha = encodeAlpha


        val pngIdBytes = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)
        width = image.width
        height = image.height
        this.image = image
        /*
        * start with an array that is big enough to hold all the pixels
        * (plus filter bytes), and an extra 200 bytes for header info
        */
        pngBytes = ByteArray((width + 1) * height * 3 + 200)
        /*
        * keep track of largest byte written to the array
        */
        maxPos = 0

        bytePos = writeBytes(pngIdBytes, 0)
        //hdrPos = bytePos;
        writeHeader()
        //dataPos = bytePos;
        if (writeImageData()) {
            writeEnd()
            pngBytes = resizeByteArray(pngBytes!!, maxPos)
        } else {
            throw Exception()
        }
        return pngBytes!!
    }

    /**
     * Increase or decrease the length of a byte array.
     *
     * @param array ByteArray to resize
     * @param newLength The length you wish the new array to have.
     * @return Array of newly desired length. If shorter than the
     * original, the trailing elements are truncated.
     */
    private fun resizeByteArray(array: ByteArray, newLength: Int): ByteArray {
        val newArray = ByteArray(newLength)
        val oldLength = array.size
        System.arraycopy(array, 0, newArray, 0, min(oldLength, newLength))
        return newArray
    }

    @Suppress("unused")
    fun release() {
        image?.recycle()
        image = null
        pngBytes = null
    }

    /**
     * Write an array of bytes into the pngBytes array.
     * Note: This routine has the side effect of updating
     * maxPos, the largest element written in the array.
     * The array is resized by 1000 bytes or the length
     * of the data to be written, whichever is larger.
     *
     * @param data The data to be written into pngBytes.
     * @param offset The starting point to write to.
     * @return The next place to be written to in the pngBytes array.
     */
    private fun writeBytes(data: ByteArray, offset: Int): Int {
        maxPos = max(maxPos, offset + data.size)
        if (data.size + offset > pngBytes!!.size) {
            pngBytes = resizeByteArray(pngBytes!!, pngBytes!!.size + max(1000, data.size))
        }
        System.arraycopy(data, 0, pngBytes!!, offset, data.size)
        return offset + data.size
    }

    /**
     * Write an array of bytes into the pngBytes array, specifying number of bytes to write.
     * Note: This routine has the side effect of updating
     * maxPos, the largest element written in the array.
     * The array is resized by 1000 bytes or the length
     * of the data to be written, whichever is larger.
     *
     * @param data The data to be written into pngBytes.
     * @param nBytes The number of bytes to be written.
     * @param offset The starting point to write to.
     * @return The next place to be written to in the pngBytes array.
     */
    private fun writeBytes(data: ByteArray, nBytes: Int, offset: Int): Int {
        maxPos = max(maxPos, offset + nBytes)
        if (nBytes + offset > pngBytes!!.size) {
            pngBytes = resizeByteArray(pngBytes!!, pngBytes!!.size + max(1000, nBytes))
        }
        System.arraycopy(data, 0, pngBytes!!, offset, nBytes)
        return offset + nBytes
    }

    /**
     * Write a two-byte integer into the pngBytes array at a given position.
     *
     * @param n The integer to be written into pngBytes.
     * @param offset The starting point to write to.
     * @return The next place to be written to in the pngBytes array.
     */
    @Suppress("unused")
    private fun writeInt2(n: Int, offset: Int): Int {
        val temp = byteArrayOf((n shr 8 and 0xff).toByte(), (n and 0xff).toByte())
        return writeBytes(temp, offset)
    }

    /**
     * Write a four-byte integer into the pngBytes array at a given position.
     *
     * @param n The integer to be written into pngBytes.
     * @param offset The starting point to write to.
     * @return The next place to be written to in the pngBytes array.
     */
    private fun writeInt4(n: Int, offset: Int): Int {
        val temp = byteArrayOf(
                (n shr 24 and 0xff).toByte(),
                (n shr 16 and 0xff).toByte(),
                (n shr 8 and 0xff).toByte(),
                (n and 0xff).toByte()
        )
        return writeBytes(temp, offset)
    }

    /**
     * Write a single byte into the pngBytes array at a given position.
     *
     * @param b The integer to be written into pngBytes.
     * @param offset The starting point to write to.
     * @return The next place to be written to in the pngBytes array.
     */
    private fun writeByte(b: Int, offset: Int): Int {
        val temp = byteArrayOf(b.toByte())

        return writeBytes(temp, offset)
    }

    /**
     * Write a PNG "IHDR" chunk into the pngBytes array.
     */
    private fun writeHeader() {
        bytePos = writeInt4(13, bytePos)
        val startPos: Int = bytePos
        bytePos = writeBytes(ihdr, bytePos)
        width = image!!.width
        height = image!!.height
        bytePos = writeInt4(width, bytePos)
        bytePos = writeInt4(height, bytePos)
        bytePos = writeByte(8, bytePos) // bit depth
        bytePos = writeByte(if (encodeAlpha) 6 else 2, bytePos) // direct model
        bytePos = writeByte(0, bytePos) // compression method
        bytePos = writeByte(0, bytePos) // filter method
        bytePos = writeByte(0, bytePos) // no interlace
        crc.reset()
        crc.update(pngBytes!!, startPos, bytePos - startPos)
        crcValue = crc.value
        bytePos = writeInt4(crcValue.toInt(), bytePos)
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

    /**
     * Write the image data into the pngBytes array.
     * This will write one or more PNG "IDAT" chunks. In order
     * to conserve memory, this method grabs as many rows as will
     * fit into 32K bytes, or the whole image; whichever is less.
     *
     *
     * @return true if no errors; false if error grabbing pixels
     */
    private fun writeImageData(): Boolean {
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
                image!!.getPixels(pixels, 0, width, 0, startRow, width, nRows)

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
            bytePos = writeInt4(nCompressed, bytePos)
            bytePos = writeBytes(idat, bytePos)
            crc.update(idat)
            bytePos = writeBytes(compressedLines, nCompressed, bytePos)
            crc.update(compressedLines, 0, nCompressed)

            crcValue = crc.value
            bytePos = writeInt4(crcValue.toInt(), bytePos)
            scrunch.finish()
            scrunch.end()
            return true
        } catch (e: IOException) {
            System.err.println(e.toString())
            return false
        }
    }

    /**
     * Write a PNG "IEND" chunk into the pngBytes array.
     */
    private fun writeEnd() {
        bytePos = writeInt4(0, bytePos)
        bytePos = writeBytes(iend, bytePos)
        crc.reset()
        crc.update(iend)
        crcValue = crc.value
        bytePos = writeInt4(crcValue.toInt(), bytePos)
    }
}