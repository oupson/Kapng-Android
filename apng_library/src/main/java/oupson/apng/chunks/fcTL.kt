package oupson.apng.chunks

import oupson.apng.utils.Utils
import oupson.apng.utils.Utils.Companion.getBlendOp
import oupson.apng.utils.Utils.Companion.getDisposeOp
import oupson.apng.utils.Utils.Companion.parseLength

@Suppress("ClassName")
class fcTL : Chunk {
    override var body : ByteArray = byteArrayOf()

    // Height and width of frame
    var pngWidth = -1
    var pngHeight = -1

    // Delay to wait after the frame
    var delay : Float = -1f

    // x and y offsets
    var xOffset : Int = 0
    var yOffset : Int = 0

    var blendOp : Utils.Companion.BlendOp = Utils.Companion.BlendOp.APNG_BLEND_OP_SOURCE
    var disposeOp : Utils.Companion.DisposeOp = Utils.Companion.DisposeOp.APNG_DISPOSE_OP_NONE

    /**
     * Parse the chunk
     * @param byteArray The chunk with the length and the crc
     */
    override fun parse(byteArray: ByteArray) {
        val i = 4
        // Find fcTL chunk
        if (byteArray[i] == 0x66.toByte() && byteArray[i + 1] == 0x63.toByte() && byteArray[i + 2] == 0x54.toByte() && byteArray[i + 3] == 0x4C.toByte()) {
            // Get length of the body of the chunk
            val bodySize = parseLength(byteArray.copyOfRange(i - 4, 1))
            // Get the width of the png
            pngWidth = parseLength(byteArray.copyOfRange(i + 8, i + 12))
            // Get the height of the png
            pngHeight = parseLength(byteArray.copyOfRange(i + 12, i + 16))
            /*
                 * The `delay_num` and `delay_den` parameters together specify a fraction indicating the time to display the current frame, in seconds.
                 * If the the value of the numerator is 0 the decoder should render the next frame as quickly as possible, though viewers may impose a reasonable lower bound.
                 */
            // Get delay numerator
            val delayNum = parseLength(byteArray.copyOfRange(i + 24, i + 26)).toFloat()
            // Get delay denominator
            var delayDen = parseLength(byteArray.copyOfRange(i + 26, i + 28)).toFloat()

            // If the denominator is 0, it is to be treated as if it were 100 (that is, `delay_num` then specifies 1/100ths of a second).
            if (delayDen == 0f) {
                delayDen = 100f
            }
            delay = (delayNum / delayDen * 1000)
            // Get x and y offsets
            xOffset = parseLength(byteArray.copyOfRange(i + 16, i + 20))
            yOffset = parseLength(byteArray.copyOfRange(i + 20, i + 24))
            body = byteArray.copyOfRange(i + 4, i + bodySize + 4)
            blendOp = getBlendOp(String.format("%02x", byteArray[33]).toLong(16).toInt())
            disposeOp = getDisposeOp(String.format("%02x", byteArray[32]).toLong(16).toInt())
        }
    }
}