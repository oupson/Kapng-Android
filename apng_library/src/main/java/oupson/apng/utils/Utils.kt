package oupson.apng.utils

class Utils {
    companion object {
        /**
         * @param byteArray The PNG
         * @return [Boolean] True if is a png
         */
        fun isPng(byteArray: ByteArray): Boolean {
            // return byteArray.copyOfRange(0, 8).contentToString() == pngSignature.contentToString()
            return if (byteArray.size == 8)
                byteArray.contentEquals(pngSignature)
            else
                byteArray.copyOfRange(0, 8).contentEquals(pngSignature)
        }

        /**
         * Know if file is an APNG
         * @param byteArray APNG
         * @return True if is an APNG
         */
        fun isApng(byteArray: ByteArray): Boolean {
            if (!isPng(byteArray)) return false
            try {
                val acTL = byteArrayOf(0x61, 0x63, 0x54, 0x4c)
                @Suppress("LocalVariableName") val IDAT = byteArrayOf(0x49, 0x44, 0x41, 0x54)
                for (i in 8 until byteArray.size) {
                    val it = byteArray.copyOfRange(i, i + 4)
                    // if byteArray contain acTL
                    if (it.contentEquals(acTL)) {
                        return true
                    } else if (it.contentEquals(IDAT)) {
                        return false
                    }
                }
                return false
            } catch (e: Exception) {
                return false
            }
        }

        /**
         * Signature for png / APNG files
         */
        val pngSignature: ByteArray by lazy {
            byteArrayOf(
                0x89.toByte(),
                0x50,
                0x4E,
                0x47,
                0x0D,
                0x0A,
                0x1A,
                0x0A
            )
        }

        /**
         * DisposeOp specifies how the output buffer should be changed at the end of the delay (before rendering the next frame).
         * Values :
         * - [APNG_DISPOSE_OP_NONE] : No disposal is done on this frame before rendering the next; the contents of the output buffer are left as is.
         * - [APNG_DISPOSE_OP_BACKGROUND] : The frame's region of the output buffer is to be cleared to fully transparent black before rendering the next frame.
         * - [APNG_DISPOSE_OP_PREVIOUS] : The frame's region of the output buffer is to be reverted to the previous contents before rendering the next frame.
         */
        enum class DisposeOp {
            APNG_DISPOSE_OP_NONE,
            APNG_DISPOSE_OP_BACKGROUND,
            APNG_DISPOSE_OP_PREVIOUS
        }

        /**
         * Get the int equivalent to the DisposeOp
         * @param disposeOp The DisposeOp
         * @return [Int] An int equivalent to the DisposeOp
         */
        fun getDisposeOp(disposeOp: DisposeOp): Int {
            return when (disposeOp) {
                Companion.DisposeOp.APNG_DISPOSE_OP_NONE -> 0
                Companion.DisposeOp.APNG_DISPOSE_OP_BACKGROUND -> 1
                Companion.DisposeOp.APNG_DISPOSE_OP_PREVIOUS -> 2
            }
        }

        /**
         * Get the DisposeOp enum equivalent to the int
         * @param int Int of the DisposeOp
         * @return [DisposeOp] A DisposeOp
         */
        fun getDisposeOp(int: Int): DisposeOp {
            return when (int) {
                0 -> DisposeOp.APNG_DISPOSE_OP_NONE
                1 -> DisposeOp.APNG_DISPOSE_OP_BACKGROUND
                2 -> DisposeOp.APNG_DISPOSE_OP_PREVIOUS
                else -> DisposeOp.APNG_DISPOSE_OP_NONE
            }
        }

        /**
         * BlendOp specifies whether the frame is to be alpha blended into the current output buffer content, or whether it should completely replace its region in the output buffer.
         * Values :
         * - [APNG_BLEND_OP_SOURCE] : All color components of the frame, including alpha, overwrite the current contents of the frame's output buffer region.
         * - [APNG_BLEND_OP_OVER] : The frame should be composited onto the output buffer based on its alpha, using a simple OVER operation as described in the Alpha Channel Processing section of the Extensions to the PNG Specification, Version 1.2.0.
         */
        enum class BlendOp {
            APNG_BLEND_OP_SOURCE,
            APNG_BLEND_OP_OVER
        }

        /**
         * Get the int equivalent to the BlendOp
         * @param blendOp The BlendOp
         * @return [Int] An int equivalent to the BlendOp
         */
        fun getBlendOp(blendOp: BlendOp): Int {
            return when (blendOp) {
                Companion.BlendOp.APNG_BLEND_OP_SOURCE -> 0
                Companion.BlendOp.APNG_BLEND_OP_OVER -> 1
            }
        }

        /**
         * Get the BlendOp enum equivalent to the int
         * @param int Int of the BlendOp
         * @return [BlendOp] A BlendOp
         */
        fun getBlendOp(int: Int): BlendOp {
            return when (int) {
                0 -> BlendOp.APNG_BLEND_OP_SOURCE
                1 -> BlendOp.APNG_BLEND_OP_OVER
                else -> BlendOp.APNG_BLEND_OP_SOURCE
            }
        }

        /**
         * Generate a 4 bytes array from an Int
         * @param i The int
         * @return [Array] 4 Bytes
         */
        fun to4Bytes(i: Int): Array<Byte> {
            return arrayOf((i shr 24).toByte(), (i shr 16).toByte(), (i shr 8).toByte(), i.toByte())
        }

        /**
         * Generate a 4 bytes array from an Int
         * @param i The int
         * @return [ByteArray] 4 Bytes
         */
        fun to4BytesArray(i: Int): ByteArray {
            return byteArrayOf(
                (i shr 24).toByte(),
                (i shr 16).toByte(),
                (i shr 8).toByte(),
                i.toByte()
            )
        }

        /**
         * Generate a 2 bytes array from an Int
         * @param i The int
         * @return [ByteArray] 2 Bytes
         */
        fun to2Bytes(i: Int): ByteArray {
            return byteArrayOf((i shr 8).toByte(), i /*>> 0*/.toByte())
        }

        /**
         * Parse the length of chunks
         * [byteArray] The beginning of the chunk, containing the length
         * [Int] The length of the chunk
         */
        fun parseLength(byteArray: ByteArray): Int {
            var lengthString = ""
            byteArray.forEach {
                lengthString += String.format("%02x", it)
            }

            return lengthString.toLong(16).toInt()
        }

        val fcTL: ByteArray by lazy { byteArrayOf(0x66, 0x63, 0x54, 0x4c) }
        val IEND: ByteArray by lazy { byteArrayOf(0x49, 0x45, 0x4e, 0x44) }
        val IDAT: ByteArray by lazy { byteArrayOf(0x49, 0x44, 0x41, 0x54) }
        val fdAT: ByteArray by lazy { byteArrayOf(0x66, 0x64, 0x41, 0x54) }
        val plte: ByteArray by lazy { byteArrayOf(0x50, 0x4c, 0x54, 0x45) }
        val tnrs: ByteArray by lazy { byteArrayOf(0x74, 0x52, 0x4e, 0x53) }
        val IHDR: ByteArray by lazy { byteArrayOf(0x49, 0x48, 0x44, 0x52) }
        val acTL: ByteArray by lazy { byteArrayOf(0x61, 0x63, 0x54, 0x4c) }
    }
}