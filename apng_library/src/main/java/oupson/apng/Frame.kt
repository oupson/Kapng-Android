package oupson.apng

import oupson.apng.ApngFactory.Companion.isPng

/**
 * A frame for an animated png
 * @author oupson
 * @param byteArray The byte Array of the png
 * @param delay The delay in ms, default is 1000
 * @throws NotPngException
 */

class Frame {

    val byteArray : ByteArray

    val width : Int
    val height : Int

    val ihdr : IHDR

    var idat : IDAT

    val delay : Float

    val x_offsets : Int
    val y_offsets : Int

    val maxWidth : Int
    val maxHeight : Int

    val blend_op: Utils.Companion.blend_op
    val dispose_op : Utils.Companion.dispose_op

    constructor(byteArray: ByteArray) {
        if (isPng(byteArray)) {
            this.byteArray = byteArray
            // Get width and height for image
            ihdr = IHDR()
            ihdr.parseIHDR(byteArray)

            width = ihdr.pngWidth
            height = ihdr.pngHeight

            // Get image bytes
            idat = IDAT()
            idat.parseIDAT(byteArray)

            delay = 1000f

            x_offsets = 0
            y_offsets = 0

            maxHeight = -1
            maxWidth = -1
            blend_op = Utils.Companion.blend_op.APNG_BLEND_OP_SOURCE
            dispose_op = Utils.Companion.dispose_op.APNG_DISPOSE_OP_NONE
        } else {
            throw NotPngException()
        }
    }
    constructor(byteArray: ByteArray, delay : Float) {
        if (isPng(byteArray)) {
            this.byteArray = byteArray
            // Get width and height for image
            ihdr = IHDR()
            ihdr.parseIHDR(byteArray)

            width = ihdr.pngWidth
            height = ihdr.pngHeight

            // Get image bytes
            idat = IDAT()
            idat.parseIDAT(byteArray)

            this.delay = delay

            x_offsets = 0
            y_offsets = 0

            maxHeight = -1
            maxWidth = -1
            blend_op = Utils.Companion.blend_op.APNG_BLEND_OP_SOURCE
            dispose_op = Utils.Companion.dispose_op.APNG_DISPOSE_OP_NONE
        } else {
            throw NotPngException()
        }
    }

    constructor(byteArray: ByteArray, delay : Float, xOffsets : Int, yOffsets : Int, blend_op: Utils.Companion.blend_op, dispose_op: Utils.Companion.dispose_op) {
        if (isPng(byteArray)) {
            this.byteArray = byteArray
            // Get width and height for image
            ihdr = IHDR()
            ihdr.parseIHDR(byteArray)

            width = ihdr.pngWidth
            height = ihdr.pngHeight

            // Get image bytes
            idat = IDAT()
            idat.parseIDAT(byteArray)

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

            // Get image bytes
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