package oupson.apng.chunks

import oupson.apng.utils.Utils
import oupson.apng.utils.Utils.Companion.getBlend_op
import oupson.apng.utils.Utils.Companion.getDispose_op
import oupson.apng.utils.Utils.Companion.parseLength

class fcTL : Chunk {
    override var body : ByteArray = byteArrayOf()

    // Height and width of frame
    var pngWidth = -1
    var pngHeight = -1

    // Delay to wait after the frame
    var delay : Float = -1f

    // x and y offsets
    var x_offset : Int = 0
    var y_offset : Int = 0

    var blend_op : Utils.Companion.blend_op = Utils.Companion.blend_op.APNG_BLEND_OP_SOURCE
    var dispose_op : Utils.Companion.dispose_op = Utils.Companion.dispose_op.APNG_DISPOSE_OP_NONE

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
            val delay_num = parseLength(byteArray.copyOfRange(i + 24, i + 26)).toFloat()
            // Get delay denominator
            var delay_den = parseLength(byteArray.copyOfRange(i + 26, i + 28)).toFloat()

            // If the denominator is 0, it is to be treated as if it were 100 (that is, `delay_num` then specifies 1/100ths of a second).
            if (delay_den == 0f) {
                delay_den = 100f
            }
            delay = (delay_num / delay_den * 1000)
            // Get x and y offsets
            x_offset = parseLength(byteArray.copyOfRange(i + 16, i + 20))
            y_offset = parseLength(byteArray.copyOfRange(i + 20, i + 24))
            body = byteArray.copyOfRange(i + 4, i + bodySize + 4)
            blend_op = getBlend_op(String.format("%02x", byteArray[33]).toLong(16).toInt())
            dispose_op = getDispose_op(String.format("%02x", byteArray[32]).toLong(16).toInt())
        }
    }
}