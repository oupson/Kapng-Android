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

    var blend_op: Utils.Companion.blend_op
    var dispose_op : Utils.Companion.dispose_op

    constructor(byteArray: ByteArray) {
        if (isPng(byteArray)) {
            this.byteArray = byteArray
            Log.e("tag", byteArray.size.toString())
            // Get width and height for image
            delay = 1000f
            blend_op = Utils.Companion.blend_op.APNG_BLEND_OP_SOURCE
            dispose_op = Utils.Companion.dispose_op.APNG_DISPOSE_OP_NONE
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
            blend_op = Utils.Companion.blend_op.APNG_BLEND_OP_SOURCE
            dispose_op = Utils.Companion.dispose_op.APNG_DISPOSE_OP_NONE
        } else {
            throw NotPngException()
        }
    }

    constructor(byteArray: ByteArray, delay : Float, blend_op: Utils.Companion.blend_op, dispose_op: Utils.Companion.dispose_op) {
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
            this.blend_op = blend_op
            this.dispose_op = dispose_op
        } else {
            throw NotPngException()
        }
    }

    constructor(byteArray: ByteArray, delay : Float, xOffsets : Int, yOffsets : Int, blend_op: Utils.Companion.blend_op, dispose_op: Utils.Companion.dispose_op) {
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
            this.blend_op = blend_op
            this.dispose_op = dispose_op
        } else {
            throw NotPngException()
        }
    }

    constructor(byteArray: ByteArray, delay : Float, xOffsets : Int, yOffsets : Int, maxWidth : Int, maxHeight : Int, blend_op: Utils.Companion.blend_op, dispose_op: Utils.Companion.dispose_op) {
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
            this.blend_op = blend_op
            this.dispose_op = dispose_op
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