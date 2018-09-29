package oupson.apng

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
    }
}