package oupson.apng

import android.graphics.Bitmap
import android.graphics.Canvas
import java.io.ByteArrayOutputStream

class Utils {
    companion object {
        enum class dispose_op {
            APNG_DISPOSE_OP_NONE,
            APNG_DISPOSE_OP_BACKGROUND,
            APNG_DISPOSE_OP_PREVIOUS
        }

        fun getDispose_op(dispose_op: dispose_op) : Int {
            return when(dispose_op) {
                Companion.dispose_op.APNG_DISPOSE_OP_NONE -> 0
                Companion.dispose_op.APNG_DISPOSE_OP_BACKGROUND -> 1
                Companion.dispose_op.APNG_DISPOSE_OP_PREVIOUS -> 2
            }
        }

        fun getDispose_op(int: Int) : dispose_op {
            return when(int) {
                0 -> Companion.dispose_op.APNG_DISPOSE_OP_NONE
                1 -> Companion.dispose_op.APNG_DISPOSE_OP_BACKGROUND
                2 -> Companion.dispose_op.APNG_DISPOSE_OP_PREVIOUS
                else -> dispose_op.APNG_DISPOSE_OP_NONE
            }
        }

        enum class blend_op() {
            APNG_BLEND_OP_SOURCE,
            APNG_BLEND_OP_OVER
        }

        fun getBlend_op(blend_op: blend_op) : Int {
            return when(blend_op) {
                Companion.blend_op.APNG_BLEND_OP_SOURCE -> 0
                Companion.blend_op.APNG_BLEND_OP_OVER -> 1
            }
        }

        fun getBlend_op(int : Int) : blend_op{
            return when(int) {
                0 -> Companion.blend_op.APNG_BLEND_OP_SOURCE
                1 -> Companion.blend_op.APNG_BLEND_OP_OVER
                else -> blend_op.APNG_BLEND_OP_SOURCE
            }
        }

        fun toByteArray(bitmap: Bitmap) : ByteArray {
            val bos = ByteArrayOutputStream();
            convertImage(bitmap).compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
            return bos.toByteArray();
        }

        fun convertImage(bitmap: Bitmap) : Bitmap{
            val btm = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            val canvas = Canvas(btm)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            return btm
        }

        /**
         * Generate a 4 bytes array from an Int
         * @param i The int
         */
        fun to4Bytes(i: Int): ByteArray {
            val result = ByteArray(4)
            result[0] = (i shr 24).toByte()
            result[1] = (i shr 16).toByte()
            result[2] = (i shr 8).toByte()
            result[3] = i /*>> 0*/.toByte()
            return result
        }

        /**
         * Generate a 2 bytes array from an Int
         * @param i The int
         */
        fun to2Bytes(i: Int): ByteArray {
            val result = ByteArray(2)
            result[0] = (i shr 8).toByte()
            result[1] = i /*>> 0*/.toByte()
            return result
        }
    }
}