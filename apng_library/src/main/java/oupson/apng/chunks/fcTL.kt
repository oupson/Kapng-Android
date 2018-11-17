package oupson.apng.chunks

import oupson.apng.utils.Utils
import oupson.apng.utils.Utils.Companion.getBlend_op
import oupson.apng.utils.Utils.Companion.getDispose_op

class fcTL(byteArray: ByteArray) {

    private var corpsSize = -1
    lateinit var fcTLBody : ByteArray

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
    init {
        for (i in 0 until byteArray.size) {
            // Find fcTL chunk
            if (byteArray[i] == 0x66.toByte() && byteArray[i + 1] == 0x63.toByte() && byteArray[ i + 2 ] == 0x54.toByte() && byteArray[ i + 3 ] == 0x4C.toByte()) {
                // Get length of the body of the chunk
                var lengthString = ""
                byteArray.copyOfRange(i - 4, i).forEach {
                    lengthString += String.format("%02x", it)
                }
                corpsSize = lengthString.toLong(16).toInt()

                // Get the width of the png
                var pngwidth = ""
                byteArray.copyOfRange(i + 8, i + 12).forEach {
                    pngwidth += String.format("%02x", it)
                }
                pngWidth = pngwidth.toLong(16).toInt()

                // Get the height of the png
                var pngheight = ""
                byteArray.copyOfRange(i + 12, i + 16).forEach {
                    pngheight += String.format("%02x", it)
                }
                pngHeight = pngheight.toLong(16).toInt()

                /**
                 * The `delay_num` and `delay_den` parameters together specify a fraction indicating the time to display the current frame, in seconds.
                 * If the the value of the numerator is 0 the decoder should render the next frame as quickly as possible, though viewers may impose a reasonable lower bound.
                 */

                // Get delay numerator
                var delayNum = ""
                byteArray.copyOfRange(i + 24, i+ 26).forEach {
                    delayNum += String.format("%02x", it)
                }
                val delay_num = delayNum.toLong(16).toFloat()

                // Get delay denominator
                var delayDen = ""
                byteArray.copyOfRange(i + 26, i+ 28).forEach {
                    delayDen += String.format("%02x", it)
                }
                var delay_den = delayDen.toLong(16).toFloat()

                /**
                 * If the denominator is 0, it is to be treated as if it were 100 (that is, `delay_num` then specifies 1/100ths of a second).
                 */
                if (delay_den == 0f) {
                    delay_den = 100f
                }

                delay = (delay_num / delay_den * 1000)


                // Get x and y offsets
                var xOffset = ""
                byteArray.copyOfRange(i + 16, i+ 20).forEach {
                    xOffset += String.format("%02x", it)
                }

                x_offset = xOffset.toLong(16).toInt()

                var yOffset = ""
                byteArray.copyOfRange(i + 20, i+ 24).forEach {
                    yOffset += String.format("%02x", it)
                }

                y_offset = yOffset.toLong(16).toInt()

                val _fcTLBody = ArrayList<Byte>()
                byteArray.copyOfRange(i + 4, i + corpsSize + 3 ).forEach {
                    _fcTLBody.add(it)
                }
                fcTLBody= _fcTLBody.toByteArray()

                blend_op = getBlend_op(String.format("%02x", byteArray[33]).toLong(16).toInt())

                dispose_op = getDispose_op(String.format("%02x", byteArray[32]).toLong(16).toInt())
            }
        }
    }
}