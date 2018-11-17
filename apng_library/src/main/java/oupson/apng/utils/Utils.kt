package oupson.apng.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import java.io.ByteArrayOutputStream

class Utils {
    companion object {
        /**
         * @return True if is a png
         */
        fun isPng(byteArray: ByteArray): Boolean {
            return byteArray.copyOfRange(0, 8).contentToString() == pngSignature.contentToString()
        }

        /**
         * Know if file is an APNG
         * @return True if is an APNG
         */
        fun isApng(byteArray: ByteArray) : Boolean {
            if (!isPng(byteArray)) return false
            try {
                val acTL = byteArrayOf(0x61, 0x63, 0x54, 0x4c)
                val IDAT = byteArrayOf(0x49, 0x44, 0x41, 0x54)
                for (i in 0 until byteArray.size) {
                    val it = byteArray.copyOfRange(i, i + 4)
                    // if byteArray contain acTL
                    if (it.contentEquals(acTL)) {
                        return true
                    } else if (it.contentEquals(IDAT)){
                        return false
                    }
                }
                return false
            } catch (e : Exception) {
                return false
            }
        }

        /**
         * Signature for png / APNG files
         */
        val pngSignature: ByteArray = byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte())


        enum class dispose_op {
            APNG_DISPOSE_OP_NONE,
            APNG_DISPOSE_OP_BACKGROUND,
            APNG_DISPOSE_OP_PREVIOUS
        }

        /**
         * Get the int equivalent to the dispose_op
         * @param dispose_op The dispose_op
         * @return An int equivalent to the dispose_op
         */
        fun getDispose_op(dispose_op: dispose_op) : Int {
            return when(dispose_op) {
                Companion.dispose_op.APNG_DISPOSE_OP_NONE -> 0
                Companion.dispose_op.APNG_DISPOSE_OP_BACKGROUND -> 1
                Companion.dispose_op.APNG_DISPOSE_OP_PREVIOUS -> 2
            }
        }

        /**
         * Get the dispose_op enum equivalent to the int
         * @param int Int of the dispose_op
         * @return A dispose_op
         */
        fun getDispose_op(int: Int) : dispose_op {
            return when(int) {
                0 -> dispose_op.APNG_DISPOSE_OP_NONE
                1 -> dispose_op.APNG_DISPOSE_OP_BACKGROUND
                2 -> dispose_op.APNG_DISPOSE_OP_PREVIOUS
                else -> dispose_op.APNG_DISPOSE_OP_NONE
            }
        }

        enum class blend_op() {
            APNG_BLEND_OP_SOURCE,
            APNG_BLEND_OP_OVER
        }

        /**
         * Get the int equivalent to the blend_op
         * @param blend_op The blend_op
         * @return An int equivalent to the blend_op
         */
        fun getBlend_op(blend_op: blend_op) : Int {
            return when(blend_op) {
                Companion.blend_op.APNG_BLEND_OP_SOURCE -> 0
                Companion.blend_op.APNG_BLEND_OP_OVER -> 1
            }
        }

        /**
         * Get the blend_op enum equivalent to the int
         * @param int Int of the blend_op
         * @return A blend_op
         */
        fun getBlend_op(int : Int) : blend_op {
            return when(int) {
                0 -> blend_op.APNG_BLEND_OP_SOURCE
                1 -> blend_op.APNG_BLEND_OP_OVER
                else -> blend_op.APNG_BLEND_OP_SOURCE
            }
        }

        /**
         * Get PNG ByteArray of Bitmap
         * @param bitmap The bitmap to convert
         * @return PNG ByteArray of the Bitmap
         */
        fun toByteArray(bitmap: Bitmap) : ByteArray {
            val bos = ByteArrayOutputStream();
            convertImage(bitmap).compress(Bitmap.CompressFormat.PNG, 100 /*ignored for PNG*/, bos);
            return bos.toByteArray();
        }

        /**
         * Convert the image
         * @param bitmap Bitmap to convert
         * @return Bitmap converted
         */
        fun convertImage(bitmap: Bitmap) : Bitmap{
            val btm = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            btm.setHasAlpha(true)
            val canvas = Canvas(btm)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            return btm
        }

        /**
         * Generate a 4 bytes array from an Int
         * @param i The int
         * @return 2 Bytes
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
         * @return 2 Bytes
         */
        fun to2Bytes(i: Int): ByteArray {
            val result = ByteArray(2)
            result[0] = (i shr 8).toByte()
            result[1] = i /*>> 0*/.toByte()
            return result
        }
    }
}