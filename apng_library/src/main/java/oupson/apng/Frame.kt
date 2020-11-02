package oupson.apng

import oupson.apng.chunks.IDAT
import oupson.apng.chunks.IHDR
import oupson.apng.exceptions.NotPngException
import oupson.apng.utils.Utils
import oupson.apng.utils.Utils.Companion.IDAT
import oupson.apng.utils.Utils.Companion.IHDR
import oupson.apng.utils.Utils.Companion.isPng

/**
 * A frame of the APNG
 * @param byteArray The bitmap to add
 * @param delay Delay of the frame
 * @param xOffsets The X offset where the frame should be rendered
 * @param yOffsets The Y offset where the frame should be rendered
 * @param disposeOp `DisposeOp` specifies how the output buffer should be changed at the end of the delay (before rendering the next frame).
 * @param blendOp `BlendOp` specifies whether the frame is to be alpha blended into the current output buffer content, or whether it should completely replace its region in the output buffer.
 * @param maxWidth The max width of the APNG
 * @param maxHeight The max height of the APNG
 */
class Frame // Get width and height for image
    (
    byteArray: ByteArray,
    delay: Float = 1000f,
    xOffsets: Int = 0,
    yOffsets: Int = 0,
    blendOp: Utils.Companion.BlendOp = Utils.Companion.BlendOp.APNG_BLEND_OP_SOURCE,
    disposeOp: Utils.Companion.DisposeOp = Utils.Companion.DisposeOp.APNG_DISPOSE_OP_NONE,
    maxWidth: Int? = null,
    maxHeight: Int? = null
) {

    var byteArray : ByteArray

    var width : Int = -1
    var height : Int = -1

    lateinit var ihdr : IHDR

    lateinit var idat : IDAT

    val delay : Float

    var xOffsets : Int = 0
    var yOffsets : Int = 0

    var maxWidth : Int? = null
    var maxHeight : Int? = null

    var blendOp: Utils.Companion.BlendOp
    var disposeOp : Utils.Companion.DisposeOp

    init {
        if (isPng(byteArray)) {
            this.byteArray = byteArray
            // Get width and height for image
            var cursor = 8
            while (cursor < byteArray.size) {
                val chunk = byteArray.copyOfRange(cursor, cursor + Utils.uIntFromBytesBigEndian(byteArray.copyOfRange(cursor, cursor + 4).map(Byte::toInt)) + 12)
                parseChunk(chunk)
                cursor += Utils.uIntFromBytesBigEndian(byteArray.copyOfRange(cursor, cursor + 4).map(Byte::toInt)) + 12
            }

            this.delay = delay

            this.xOffsets = xOffsets
            this.yOffsets = yOffsets

            this.maxWidth = maxWidth
            this.maxHeight = maxHeight
            this.blendOp = blendOp
            this.disposeOp = disposeOp
        } else {
            throw NotPngException()
        }
    }

    /**
     * Parse the Frame
     * @param byteArray The frame 
     */
    private fun parseChunk(byteArray: ByteArray) {
        val name = byteArray.copyOfRange(4, 8)
        if (name.contentEquals(IHDR)) {
            ihdr = IHDR()
            ihdr.parse(byteArray)
            width = ihdr.pngWidth
            height = ihdr.pngHeight
        } else if (name.contentEquals(IDAT)){
            // Get IDAT Bytes
            idat = IDAT()
            idat.parse(byteArray)
        }
    }
}