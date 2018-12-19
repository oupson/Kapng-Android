package oupson.apng

import android.graphics.BitmapFactory
import oupson.apng.chunks.IHDR
import oupson.apng.chunks.fcTL
import oupson.apng.exceptions.BadCRC
import oupson.apng.exceptions.NotApngException
import oupson.apng.utils.Utils
import oupson.apng.utils.Utils.Companion.isApng
import oupson.apng.utils.Utils.Companion.parseLength
import oupson.apng.utils.Utils.Companion.pngSignature
import oupson.apng.utils.Utils.Companion.to4Bytes
import java.util.*
import java.util.zip.CRC32

class APNGDisassembler {
    companion object {
        private var png: ArrayList<Byte>? = null
        private var cover: ArrayList<Byte>? = null
        private var delay = -1f
        private var yOffset = -1
        private var xOffset = -1
        private var plte: ByteArray? = null
        private var tnrs: ByteArray? = null
        private var maxWidth = 0
        private var maxHeight = 0
        private var blend_op: Utils.Companion.blend_op = Utils.Companion.blend_op.APNG_BLEND_OP_SOURCE
        private var dispose_op: Utils.Companion.dispose_op = Utils.Companion.dispose_op.APNG_DISPOSE_OP_NONE
        private var ihdr = IHDR()

        var apng: Apng = Apng()

        /**
         * Disassemble an Apng file
         * @param byteArray The Byte Array of the file
         * @return The apng decoded
         */
        fun disassemble(byteArray: ByteArray) : Apng {
            reset()
            if (isApng(byteArray)) {
                var cursor = 8
                while (cursor < byteArray.size) {
                    val chunk = byteArray.copyOfRange(cursor, cursor + parseLength(byteArray.copyOfRange(cursor, cursor + 4)) + 12)
                    parseChunk(chunk)
                    cursor += parseLength(byteArray.copyOfRange(cursor, cursor + 4)) + 12
                }
                return apng
            } else {
                throw NotApngException()
            }
        }

        private fun generateIhdr(ihdrOfApng: IHDR, width : Int, height : Int) : ByteArray {
            val ihdr = ArrayList<Byte>()
            // We need a body var to know body length and generate crc
            val ihdrBody = ArrayList<Byte>()
            // Add chunk body length
            ihdr.addAll(to4Bytes(ihdrOfApng.body.size).toList())
            // Add IHDR
            ihdrBody.addAll(byteArrayOf(0x49.toByte(), 0x48.toByte(), 0x44.toByte(), 0x52.toByte()).toList())
            // Add the max width and height
            ihdrBody.addAll(to4Bytes(width).toList())
            ihdrBody.addAll(to4Bytes(height).toList())
            // Add complicated stuff like depth color ...
            // If you want correct png you need same parameters. Good solution is to create new png.
            ihdrBody.addAll(ihdrOfApng.body.copyOfRange(8, 13).toList())
            // Generate CRC
            val crC32 = CRC32()
            crC32.update(ihdrBody.toByteArray(), 0, ihdrBody.size)
            ihdr.addAll(ihdrBody)
            ihdr.addAll(to4Bytes(crC32.value.toInt()).toList())
            return ihdr.toByteArray()
        }

        private fun parseChunk(byteArray: ByteArray) {
            val i = 4
            val chunkCRC = parseLength(byteArray.copyOfRange(byteArray.size - 4, byteArray.size))
            val crc = CRC32();
            crc.update(byteArray.copyOfRange(i, byteArray.size - 4))
            if (chunkCRC == crc.value.toInt()) {
                val name = Arrays.toString(byteArray.copyOfRange(i, i + 4))
                when (name) {
                    Utils.fcTL -> {
                        if (png == null) {
                            cover?.let {
                                it.addAll(to4Bytes(0).toList())
                                // Add IEND
                                val iend = byteArrayOf(0x49, 0x45, 0x4E, 0x44)
                                // Generate crc for IEND
                                val crC32 = CRC32()
                                crC32.update(iend, 0, iend.size)
                                it.addAll(iend.toList())
                                it.addAll(to4Bytes(crC32.value.toInt()).toList())
                                apng.cover = BitmapFactory.decodeByteArray(it.toByteArray(), 0, it.size)
                            }
                            png = ArrayList()
                            val fcTL = fcTL()
                            fcTL.parse(byteArray)
                            delay = fcTL.delay
                            yOffset = fcTL.y_offset
                            xOffset = fcTL.x_offset
                            blend_op = fcTL.blend_op
                            dispose_op = fcTL.dispose_op
                            val width = fcTL.pngWidth
                            val height = fcTL.pngHeight
                            png!!.addAll(pngSignature.toList())
                            png!!.addAll(generateIhdr(ihdr, width, height).toList())
                            plte?.let {
                                png!!.addAll(it.toList())
                            }
                            tnrs?.let {
                                png!!.addAll(it.toList())
                            }
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
                            apng.frames.add(Frame(png!!.toByteArray(), delay, xOffset, yOffset, maxWidth, maxHeight, blend_op, dispose_op))
                            png = ArrayList()
                            val fcTL = fcTL()
                            fcTL.parse(byteArray)
                            delay = fcTL.delay
                            yOffset = fcTL.y_offset
                            xOffset = fcTL.x_offset
                            blend_op = fcTL.blend_op
                            dispose_op = fcTL.dispose_op
                            val width = fcTL.pngWidth
                            val height = fcTL.pngHeight
                            png!!.addAll(pngSignature.toList())
                            png!!.addAll(generateIhdr(ihdr, width, height).toList())
                            plte?.let {
                                png!!.addAll(it.toList())
                            }
                            tnrs?.let {
                                png!!.addAll(it.toList())
                            }
                        }
                    }
                    Utils.IEND -> {
                        png!!.addAll(to4Bytes(0).toList())
                        // Add IEND
                        val iend = byteArrayOf(0x49, 0x45, 0x4E, 0x44)
                        // Generate crc for IEND
                        val crC32 = CRC32()
                        crC32.update(iend, 0, iend.size)
                        png!!.addAll(iend.toList())
                        png!!.addAll(to4Bytes(crC32.value.toInt()).toList())
                        apng.frames.add(Frame(png!!.toByteArray(), delay, xOffset, yOffset, maxWidth, maxHeight, blend_op, dispose_op))
                    }
                    Utils.IDAT -> {
                        if (png == null) {
                            if (cover == null) {
                                cover = ArrayList()
                                cover!!.addAll(pngSignature.toList())
                                cover!!.addAll(generateIhdr(ihdr, maxWidth, maxHeight).toList())
                            }
                            // Find the chunk length
                            val bodySize = parseLength(byteArray.copyOfRange(i - 4, i))
                            cover!!.addAll(byteArray.copyOfRange(i - 4, i).toList())
                            val body = ArrayList<Byte>()
                            body.addAll(byteArrayOf(0x49, 0x44, 0x41, 0x54).toList())
                            // Get image bytes
                            body.addAll(byteArray.copyOfRange(i + 4, i + 4 + bodySize).toList())
                            val crC32 = CRC32()
                            crC32.update(body.toByteArray(), 0, body.size)
                            cover!!.addAll(body)
                            cover!!.addAll(to4Bytes(crC32.value.toInt()).toList())
                        } else {
                            // Find the chunk length
                            val bodySize = parseLength(byteArray.copyOfRange(i - 4, i))
                            png!!.addAll(byteArray.copyOfRange(i - 4, i).toList())
                            val body = ArrayList<Byte>()
                            body.addAll(byteArrayOf(0x49, 0x44, 0x41, 0x54).toList())
                            // Get image bytes
                            body.addAll(byteArray.copyOfRange(i + 4, i + 4 + bodySize).toList())
                            val crC32 = CRC32()
                            crC32.update(body.toByteArray(), 0, body.size)
                            png!!.addAll(body)
                            png!!.addAll(to4Bytes(crC32.value.toInt()).toList())
                        }
                    }
                    Utils.fdAT -> {
                        // Find the chunk length
                        val bodySize = parseLength(byteArray.copyOfRange(i - 4, i))
                        png!!.addAll(to4Bytes(bodySize - 4).toList())
                        val body = ArrayList<Byte>()
                        body.addAll(byteArrayOf(0x49, 0x44, 0x41, 0x54).toList())
                        // Get image bytes
                        body.addAll(byteArray.copyOfRange(i + 8, i + 4 + bodySize).toList())
                        val crC32 = CRC32()
                        crC32.update(body.toByteArray(), 0, body.size)
                        png!!.addAll(body)
                        png!!.addAll(to4Bytes(crC32.value.toInt()).toList())
                    }
                    Utils.plte -> {
                        plte = byteArray
                    }
                    Utils.tnrs -> {
                        tnrs = byteArray
                    }
                    Utils.IHDR -> {
                        ihdr.parse(byteArray)
                        maxWidth = ihdr.pngWidth
                        maxHeight = ihdr.pngHeight
                    }
                }
            } else throw BadCRC()
        }

        private fun reset() {
            png = null
            cover = null
            delay = -1f
            yOffset = -1
            xOffset = -1
            plte = null
            tnrs = null
            maxWidth = 0
            maxHeight = 0
            ihdr = IHDR()
            apng = Apng()
        }
    }
}