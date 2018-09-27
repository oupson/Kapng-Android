package oupson.apng

import android.graphics.Bitmap
import oupson.apng.APNG.Companion.isApng
import java.util.zip.CRC32
import android.graphics.BitmapFactory
import android.util.Log


class APNGDisassembler(val byteArray: ByteArray) {
    val pngList = ArrayList<ByteArray>()
    var png : ArrayList<Byte>? = null

    var delayList = ArrayList<Float>()

    var yOffset= ArrayList<Int>()

    var xOffset = ArrayList<Int>()

    var maxWidth = 0
    var maxHeight = 0
    init {
        if (isApng(byteArray)) {
            val ihdr = IHDR()
            ihdr.parseIHDR(byteArray)
            maxWidth = ihdr.pngWidth
            maxHeight = ihdr.pngHeight
            for(i in 0 until byteArray.size) {
                // find new Frame with fcTL
                if (byteArray[i] == 0x66.toByte() && byteArray[i + 1] == 0x63.toByte() && byteArray[ i + 2 ] == 0x54.toByte() && byteArray[ i + 3 ] == 0x4C.toByte() || i == byteArray.size - 1) {
                    if (png == null) {
                        png = ArrayList()

                        val fcTL = fcTL(byteArray.copyOfRange(i-4, i + 28))
                        delayList.add(fcTL.delay)

                        yOffset.add(fcTL.y_offset)
                        xOffset.add(fcTL.x_offset)
                        Log.e("APNG", "delay : + ${fcTL.delay}")
                        val width = fcTL.pngWidth
                        val height = fcTL.pngHeight
                        png!!.addAll(APNG.pngSignature.toList())
                        png!!.addAll(generate_ihdr(ihdr, width, height).toList())
                    } else {
                        // Add IEND body length : 0
                        png!!.addAll(to4Bytes(0).toList())
                        // Add IEND
                        val iend = byteArrayOf(0x49, 0x45, 0x4E, 0x44)
                        // Generate crc for IEND
                        val crC32 = CRC32()
                        crC32.update(iend, 0, iend.size)
                        png!!.addAll(iend.toList())
                        png!!.addAll(to4Bytes(crC32.value.toInt()).toList())

                        pngList.add(png!!.toByteArray())

                        png = ArrayList()

                        val fcTL = fcTL(byteArray.copyOfRange(i-4, i + 28))
                        delayList.add(fcTL.delay)

                        yOffset.add(fcTL.y_offset)
                        xOffset.add(fcTL.x_offset)

                        val width = fcTL.pngWidth
                        val height = fcTL.pngHeight
                        png!!.addAll(APNG.pngSignature.toList())
                        png!!.addAll(generate_ihdr(ihdr, width, height).toList())
                    }
                } else if (byteArray[i] == 0x49.toByte() && byteArray[i + 1] == 0x44.toByte() && byteArray[ i + 2 ] == 0x41.toByte() && byteArray[ i + 3 ] == 0x54.toByte()) {
                    // Find the chunk length
                    var lengthString = ""
                    byteArray.copyOfRange( i - 4, i).forEach {
                        lengthString += String.format("%02x", it)
                    }


                    var bodySize = lengthString.toLong(16).toInt()
                    png!!.addAll(byteArray.copyOfRange(i-4, i).toList())
                    val body = ArrayList<Byte>()
                    body.addAll(byteArrayOf(0x49, 0x44, 0x41, 0x54).toList())
                    // Get image bytes

                    for (j in i +4 until i + 4 + bodySize) {
                        body.add(byteArray[j])
                    }

                    val crC32 = CRC32()
                    crC32.update(body.toByteArray(), 0, body.size)
                    png!!.addAll(body)
                    png!!.addAll(to4Bytes(crC32.value.toInt()).toList())
                } else if (byteArray[i] == 0x66.toByte() && byteArray[i + 1] == 0x64.toByte() && byteArray[ i + 2 ] == 0x41.toByte() && byteArray[ i + 3 ] == 0x54.toByte()) {
                    // Find the chunk length
                    var lengthString = ""
                    byteArray.copyOfRange( i - 4, i).forEach {
                        lengthString += String.format("%02x", it)
                    }


                    var bodySize = lengthString.toLong(16).toInt()
                    png!!.addAll(to4Bytes(bodySize - 4).toList())
                    val body = ArrayList<Byte>()
                    body.addAll(byteArrayOf(0x49, 0x44, 0x41, 0x54).toList())
                    // Get image bytes

                    for (j in i + 8 until i + 4 + bodySize) {
                        body.add(byteArray[j])
                    }

                    val crC32 = CRC32()
                    crC32.update(body.toByteArray(), 0, body.size)
                    png!!.addAll(body)
                    png!!.addAll(to4Bytes(crC32.value.toInt()).toList())
                }
            }
        } else {
            throw NotApngException()
        }
    }

    fun getbitmapList() : ArrayList<Frame>
    {
        var res = ArrayList<Frame>()
        Log.e("APNG", "pngList : ${pngList.size}, delayList : ${delayList.size}")
        for (i in 0 until pngList.size) {
            res.add(Frame(pngList[i], delayList[i], xOffset[i], yOffset[i], maxWidth, maxHeight))
        }
        return res
    }

    private fun generate_ihdr(ihdrOfApng: IHDR, width : Int, height : Int) : ByteArray {
        val ihdr = ArrayList<Byte>()

        // We need a body var to know body length and generate crc
        val ihdr_body = ArrayList<Byte>()

        // Get max height and max width of all the frames

        // Add chunk body length
        ihdr.addAll(to4Bytes(ihdrOfApng.ihdrCorps.size).toList())
        // Add IHDR
        ihdr_body.addAll(byteArrayOf(0x49.toByte(), 0x48.toByte(), 0x44.toByte(), 0x52.toByte()).toList())

        // Add the max width and height
        ihdr_body.addAll(to4Bytes(width).toList())
        ihdr_body.addAll(to4Bytes(height).toList())

        // Add complicated stuff like depth color ...
        // If you want correct png you need same parameters. Good solution is to create new png.
        ihdr_body.addAll(ihdrOfApng.ihdrCorps.copyOfRange(8, 13).toList())

        // Generate CRC
        val crC32 = CRC32()
        crC32.update(ihdr_body.toByteArray(), 0, ihdr_body.size)
        ihdr.addAll(ihdr_body)
        ihdr.addAll(to4Bytes(crC32.value.toInt()).toList())
        return ihdr.toByteArray()
    }

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



}
class extractedFrame(val bitmap: Bitmap, val delay : Float, val xoffset : Int, val yoffset : Int, val maxWidth : Int, val maxHeight : Int) {
}