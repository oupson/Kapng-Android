package oupson.apng

import android.util.Log
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

class Frame {

    var byteArray : ByteArray

    var width : Int = -1
    var height : Int = -1

    lateinit var ihdr : IHDR

    lateinit var idat : IDAT

    val delay : Float

    var x_offsets : Int = 0
    var y_offsets : Int = 0

    var maxWidth : Int? = null
    var maxHeight : Int? = null

    var blendOp: Utils.Companion.BlendOp
    var disposeOp : Utils.Companion.DisposeOp

    constructor(byteArray: ByteArray) {
        if (isPng(byteArray)) {
            this.byteArray = byteArray
            Log.e("tag", byteArray.size.toString())
            // Get width and height for image
            delay = 1000f
            blendOp = Utils.Companion.BlendOp.APNG_BLEND_OP_SOURCE
            disposeOp = Utils.Companion.DisposeOp.APNG_DISPOSE_OP_NONE
            var cursor = 8
            while (cursor < byteArray.size) {
                val chunk = byteArray.copyOfRange(cursor, cursor + Utils.parseLength(byteArray.copyOfRange(cursor, cursor + 4)) + 12)
                parseChunk(chunk)
                cursor += Utils.parseLength(byteArray.copyOfRange(cursor, cursor + 4)) + 12
            }
        } else {
            throw NotPngException()
        }
    }
    constructor(byteArray: ByteArray, delay : Float) {
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
            blendOp = Utils.Companion.BlendOp.APNG_BLEND_OP_SOURCE
            disposeOp = Utils.Companion.DisposeOp.APNG_DISPOSE_OP_NONE
        } else {
            throw NotPngException()
        }
    }

    constructor(byteArray: ByteArray, delay : Float, blendOp: Utils.Companion.BlendOp, disposeOp: Utils.Companion.DisposeOp) {
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


            this.maxWidth = -1
            this.maxHeight = -1
            this.blendOp = blendOp
            this.disposeOp = disposeOp
        } else {
            throw NotPngException()
        }
    }

    constructor(byteArray: ByteArray, delay : Float, xOffsets : Int, yOffsets : Int, blendOp: Utils.Companion.BlendOp, disposeOp: Utils.Companion.DisposeOp) {
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

            x_offsets = xOffsets
            y_offsets = yOffsets

            this.maxWidth = -1
            this.maxHeight = -1
            this.blendOp = blendOp
            this.disposeOp = disposeOp
        } else {
            throw NotPngException()
        }
    }

    constructor(byteArray: ByteArray, delay : Float, xOffsets : Int, yOffsets : Int, maxWidth : Int, maxHeight : Int, blendOp: Utils.Companion.BlendOp, disposeOp: Utils.Companion.DisposeOp) {
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

            x_offsets = xOffsets
            y_offsets = yOffsets

            this.maxWidth = maxWidth
            this.maxHeight = maxHeight
            this.blendOp = blendOp
            this.disposeOp = disposeOp
        } else {
            throw NotPngException()
        }
    }

    fun parseChunk(byteArray: ByteArray) {
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