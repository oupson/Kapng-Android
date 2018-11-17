package oupson.apng

import android.graphics.BitmapFactory
import oupson.apng.utils.Utils.Companion.isApng
import oupson.apng.utils.Utils.Companion.pngSignature
import oupson.apng.utils.Utils.Companion.to4Bytes
import oupson.apng.chunks.IHDR
import oupson.apng.chunks.fcTL
import oupson.apng.exceptions.NotApngException
import oupson.apng.utils.Utils
import java.util.zip.CRC32

class APNGDisassembler {
    companion object {
        fun disassemble(byteArray: ByteArray) : Apng {
            val pngList = ArrayList<Frame>()
            var png: ArrayList<Byte>? = null
            var cover: ArrayList<Byte>? = null
            var delay = -1f
            var yOffset = -1
            var xOffset = -1
            var plte: ByteArray? = null
            var tnrs: ByteArray? = null
            var maxWidth = 0
            var maxHeight = 0
            var blend_op: Utils.Companion.blend_op = Utils.Companion.blend_op.APNG_BLEND_OP_SOURCE
            var dispose_op: Utils.Companion.dispose_op = Utils.Companion.dispose_op.APNG_DISPOSE_OP_NONE
            val apng: Apng
            if (isApng(byteArray)) {
                apng = Apng()
                val ihdr = IHDR()
                ihdr.parseIHDR(byteArray)
                maxWidth = ihdr.pngWidth
                maxHeight = ihdr.pngHeight
                for (i in 0 until byteArray.size) {
                    // find new Frame with fcTL
                    if (byteArray[i] == 0x66.toByte() && byteArray[i + 1] == 0x63.toByte() && byteArray[i + 2] == 0x54.toByte() && byteArray[i + 3] == 0x4C.toByte()) {
                        if (png == null) {
                            if (cover != null) {
                                cover.addAll(to4Bytes(0).toList())
                                // Add IEND
                                val iend = byteArrayOf(0x49, 0x45, 0x4E, 0x44)
                                // Generate crc for IEND
                                val crC32 = CRC32()
                                crC32.update(iend, 0, iend.size)
                                cover.addAll(iend.toList())
                                cover.addAll(to4Bytes(crC32.value.toInt()).toList())

                                apng.cover = BitmapFactory.decodeByteArray(cover.toByteArray(), 0, cover.size)
                            }
                            png = ArrayList()
                            val fcTL = fcTL(byteArray.copyOfRange(i - 4, i + 36))
                            delay = fcTL.delay
                            yOffset = fcTL.y_offset
                            xOffset = fcTL.x_offset
                            blend_op = fcTL.blend_op
                            dispose_op = fcTL.dispose_op
                            val width = fcTL.pngWidth
                            val height = fcTL.pngHeight
                            png.addAll(pngSignature.toList())
                            png.addAll(generate_ihdr(ihdr, width, height).toList())

                            if (plte != null) {
                                png.addAll(plte.toList())
                            }

                            if (tnrs != null) {
                                png.addAll(tnrs.toList())
                            }
                        } else {
                            // Add IEND body length : 0
                            png.addAll(to4Bytes(0).toList())
                            // Add IEND
                            val iend = byteArrayOf(0x49, 0x45, 0x4E, 0x44)
                            // Generate crc for IEND
                            val crC32 = CRC32()
                            crC32.update(iend, 0, iend.size)
                            png.addAll(iend.toList())
                            png.addAll(to4Bytes(crC32.value.toInt()).toList())
                            pngList.add(Frame(png.toByteArray(), delay, xOffset, yOffset, maxWidth, maxHeight, blend_op, dispose_op))

                            png = ArrayList()

                            val bodySize = {
                                var lengthString = ""
                                byteArray.copyOfRange(i - 4, i).forEach {
                                    lengthString += String.format("%02x", it)
                                }
                                lengthString.toLong(16).toInt()
                            }()
                            val newBytes = byteArray.copyOfRange(i - 4, i + 4 + bodySize)
                            val fcTL = fcTL(newBytes)
                            delay = fcTL.delay

                            yOffset = fcTL.y_offset
                            xOffset = fcTL.x_offset

                            blend_op = fcTL.blend_op
                            dispose_op = fcTL.dispose_op
                            val width = fcTL.pngWidth
                            val height = fcTL.pngHeight
                            png.addAll(pngSignature.toList())
                            png.addAll(generate_ihdr(ihdr, width, height).toList())
                            if (plte != null) {
                                png.addAll(plte.toList())
                            }

                            if (tnrs != null) {
                                png.addAll(tnrs.toList())
                            }
                        }
                    } else if (i == byteArray.size - 1) {
                        png!!.addAll(to4Bytes(0).toList())
                        // Add IEND
                        val iend = byteArrayOf(0x49, 0x45, 0x4E, 0x44)
                        // Generate crc for IEND
                        val crC32 = CRC32()
                        crC32.update(iend, 0, iend.size)
                        png.addAll(iend.toList())
                        png.addAll(to4Bytes(crC32.value.toInt()).toList())
                        pngList.add(Frame(png.toByteArray(), delay, xOffset, yOffset, maxWidth, maxHeight, blend_op, dispose_op))
                    }
                    // Check if is IDAT
                    else if (byteArray[i] == 0x49.toByte() && byteArray[i + 1] == 0x44.toByte() && byteArray[i + 2] == 0x41.toByte() && byteArray[i + 3] == 0x54.toByte()) {
                        if (png == null) {
                            if (cover == null) {
                                cover = ArrayList()
                                cover.addAll(pngSignature.toList())
                                cover.addAll(generate_ihdr(ihdr, maxWidth, maxHeight).toList())
                            }
                            // Find the chunk length
                            var lengthString = ""
                            byteArray.copyOfRange(i - 4, i).forEach {
                                lengthString += String.format("%02x", it)
                            }
                            val bodySize = lengthString.toLong(16).toInt()
                            cover.addAll(byteArray.copyOfRange(i - 4, i).toList())
                            val body = ArrayList<Byte>()
                            body.addAll(byteArrayOf(0x49, 0x44, 0x41, 0x54).toList())
                            // Get image bytes
                            for (j in i + 4 until i + 4 + bodySize) {
                                body.add(byteArray[j])
                            }
                            val crC32 = CRC32()
                            crC32.update(body.toByteArray(), 0, body.size)
                            cover.addAll(body)
                            cover.addAll(to4Bytes(crC32.value.toInt()).toList())
                        } else {
                            // Find the chunk length
                            var lengthString = ""
                            byteArray.copyOfRange(i - 4, i).forEach {
                                lengthString += String.format("%02x", it)
                            }
                            val bodySize = lengthString.toLong(16).toInt()
                            png.addAll(byteArray.copyOfRange(i - 4, i).toList())
                            val body = ArrayList<Byte>()
                            body.addAll(byteArrayOf(0x49, 0x44, 0x41, 0x54).toList())
                            // Get image bytes
                            for (j in i + 4 until i + 4 + bodySize) {
                                body.add(byteArray[j])
                            }
                            val crC32 = CRC32()
                            crC32.update(body.toByteArray(), 0, body.size)
                            png.addAll(body)
                            png.addAll(to4Bytes(crC32.value.toInt()).toList())
                        }
                    }
                    // Check if is fdAT
                    else if (byteArray[i] == 0x66.toByte() && byteArray[i + 1] == 0x64.toByte() && byteArray[i + 2] == 0x41.toByte() && byteArray[i + 3] == 0x54.toByte()) {
                        // Find the chunk length
                        var lengthString = ""
                        byteArray.copyOfRange(i - 4, i).forEach {
                            lengthString += String.format("%02x", it)
                        }
                        val bodySize = lengthString.toLong(16).toInt()
                        png!!.addAll(to4Bytes(bodySize - 4).toList())
                        val body = ArrayList<Byte>()
                        body.addAll(byteArrayOf(0x49, 0x44, 0x41, 0x54).toList())
                        // Get image bytes
                        for (j in i + 8 until i + 4 + bodySize) {
                            body.add(byteArray[j])
                        }
                        val crC32 = CRC32()
                        crC32.update(body.toByteArray(), 0, body.size)
                        png.addAll(body)
                        png.addAll(to4Bytes(crC32.value.toInt()).toList())
                    }
                    // Get plte chunks if exist. The PLTE chunk contains from 1 to 256 palette entries, each a three-byte series of the form:
                    else if (byteArray[i] == 0x50.toByte() && byteArray[i + 1] == 0x4C.toByte() && byteArray[i + 2] == 0x54.toByte() && byteArray[i + 3] == 0x45.toByte()) {
                        var lengthString = ""
                        byteArray.copyOfRange(i - 4, i).forEach {
                            lengthString += String.format("%02x", it)
                        }
                        val bodySize = lengthString.toLong(16).toInt()
                        plte = byteArray.copyOfRange(i - 4, i + 8 + bodySize)
                    }
                    // Get tnrs chunk if exist. Used for transparency
                    else if (byteArray[i] == 0x74.toByte() && byteArray[i + 1] == 0x52.toByte() && byteArray[i + 2] == 0x4E.toByte() && byteArray[i + 3] == 0x53.toByte()) {
                        var lengthString = ""
                        byteArray.copyOfRange(i - 4, i).forEach {
                            lengthString += String.format("%02x", it)
                        }
                        val bodySize = lengthString.toLong(16).toInt()
                        tnrs = byteArray.copyOfRange(i - 4, i + 8 + bodySize)
                    }
                }
                apng.frames = pngList
                return apng
            } else {
                throw NotApngException()
            }
        }
        private fun generate_ihdr(ihdrOfApng: IHDR, width : Int, height : Int) : ByteArray {
            val ihdr = ArrayList<Byte>()
            // We need a body var to know body length and generate crc
            val ihdr_body = ArrayList<Byte>()
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
    }
}