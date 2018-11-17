package oupson.apng

import android.graphics.BitmapFactory
import oupson.apng.utils.Utils.Companion.isPng
import oupson.apng.utils.Utils.Companion.toByteArray
import oupson.apng.chunks.IDAT
import oupson.apng.chunks.IHDR
import oupson.apng.exceptions.NotPngException
import oupson.apng.utils.Utils

/**
 * A frame for an animated png
 * @author oupson
 * @param byteArray The byte Array of the png
 * @param delay The delay in ms, default is 1000
 * @throws NotPngException
 */

class Frame {

    var byteArray : ByteArray

    var width : Int
    var height : Int

    var ihdr : IHDR

    var idat : IDAT

    val delay : Float

    var x_offsets : Int? = null
    var y_offsets : Int? = null

    var maxWidth : Int? = null
    var maxHeight : Int? = null

    var blend_op: Utils.Companion.blend_op
    var dispose_op : Utils.Companion.dispose_op

    constructor(byteArray: ByteArray) {
        if (isPng(byteArray)) {
            val btm = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            val bytes = toByteArray(btm)
            this.byteArray = bytes
            // Get width and height for image
            ihdr = IHDR()
            ihdr.parseIHDR(bytes)

            width = ihdr.pngWidth
            height = ihdr.pngHeight

            // Get IDAT Bytes
            idat = IDAT()
            idat.parseIDAT(bytes)

            delay = 1000f

            blend_op = Utils.Companion.blend_op.APNG_BLEND_OP_SOURCE
            dispose_op = Utils.Companion.dispose_op.APNG_DISPOSE_OP_NONE
        } else {
            throw NotPngException()
        }
    }
    constructor(byteArray: ByteArray, delay : Float) {
        if (isPng(byteArray)) {
            val btm = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            val bytes = toByteArray(btm)
            this.byteArray = bytes
            // Get width and height for image
            ihdr = IHDR()
            ihdr.parseIHDR(bytes)

            width = ihdr.pngWidth
            height = ihdr.pngHeight

            // Get IDAT Bytes
            idat = IDAT()
            idat.parseIDAT(bytes)

            this.delay = delay
            blend_op = Utils.Companion.blend_op.APNG_BLEND_OP_SOURCE
            dispose_op = Utils.Companion.dispose_op.APNG_DISPOSE_OP_NONE
        } else {
            throw NotPngException()
        }
    }

    constructor(byteArray: ByteArray, delay : Float, blend_op: Utils.Companion.blend_op, dispose_op: Utils.Companion.dispose_op) {
        if (isPng(byteArray)) {
            val btm = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            val bytes = toByteArray(btm)
            this.byteArray = bytes
            // Get width and height for image
            ihdr = IHDR()
            ihdr.parseIHDR(bytes)

            width = ihdr.pngWidth
            height = ihdr.pngHeight

            // Get IDAT Bytes
            idat = IDAT()
            idat.parseIDAT(bytes)

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
            val btm = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            val bytes = toByteArray(btm)
            this.byteArray = bytes
            // Get width and height for image
            ihdr = IHDR()
            ihdr.parseIHDR(bytes)

            width = ihdr.pngWidth
            height = ihdr.pngHeight

            // Get IDAT Bytes
            idat = IDAT()
            idat.parseIDAT(bytes)

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
            ihdr = IHDR()
            ihdr.parseIHDR(byteArray)

            width = ihdr.pngWidth
            height = ihdr.pngHeight

            // Get IDAT Bytes
            idat = IDAT()
            idat.parseIDAT(byteArray)

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
}