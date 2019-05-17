package oupson.apng

import oupson.apng.chunks.IDAT
import oupson.apng.chunks.IHDR
import oupson.apng.exceptions.NotPngException
import oupson.apng.utils.Utils
import oupson.apng.utils.Utils.Companion.IDAT
import oupson.apng.utils.Utils.Companion.IHDR
import oupson.apng.utils.Utils.Companion.isPng
import java.util.*

/**
 * A frame for an animated png
 * @author oupson
 * @param byteArray The byte Array of the png
 * @param delay The delay in ms, default is 1000
 * @throws NotPngException
 */

class Frame// Get width and height for image
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
                val chunk = byteArray.copyOfRange(cursor, cursor + Utils.parseLength(byteArray.copyOfRange(cursor, cursor + 4)) + 12)
                parseChunk(chunk)
                cursor += Utils.parseLength(byteArray.copyOfRange(cursor, cursor + 4)) + 12
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

    private fun parseChunk(byteArray: ByteArray) {
        when(Arrays.toString(byteArray.copyOfRange(4, 8))) {
            IHDR -> {
                ihdr = IHDR()
                ihdr.parse(byteArray)
                width = ihdr.pngWidth
                height = ihdr.pngHeight
            }
            IDAT -> {
                // Get IDAT Bytes
                idat = IDAT()
                idat.parse(byteArray)
            }
        }
    }
}