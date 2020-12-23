package oupson.apng

import org.junit.Assert.assertEquals
import org.junit.Test
import oupson.apng.utils.Utils
import oupson.apng.utils.Utils.Companion.uIntToByteArray
import oupson.apng.utils.Utils.Companion.uShortToByteArray
import java.nio.ByteBuffer

class UtilsUnitTest {
    @Test
    fun encode_disposeOp()  {
        assertEquals(Utils.encodeDisposeOp(Utils.Companion.DisposeOp.APNG_DISPOSE_OP_NONE), 0)
        assertEquals(Utils.encodeDisposeOp(Utils.Companion.DisposeOp.APNG_DISPOSE_OP_BACKGROUND), 1)
        assertEquals(Utils.encodeDisposeOp(Utils.Companion.DisposeOp.APNG_DISPOSE_OP_PREVIOUS), 2)
    }

    @Test
    fun decode_disposeOp()  {
        assertEquals(Utils.decodeDisposeOp(0), Utils.Companion.DisposeOp.APNG_DISPOSE_OP_NONE)
        assertEquals(Utils.decodeDisposeOp(1), Utils.Companion.DisposeOp.APNG_DISPOSE_OP_BACKGROUND)
        assertEquals(Utils.decodeDisposeOp(2), Utils.Companion.DisposeOp.APNG_DISPOSE_OP_PREVIOUS)
    }

    @Test
    fun encode_blendOp()  {
        assertEquals(Utils.encodeBlendOp(Utils.Companion.BlendOp.APNG_BLEND_OP_SOURCE), 0)
        assertEquals(Utils.encodeBlendOp(Utils.Companion.BlendOp.APNG_BLEND_OP_OVER), 1)
    }

    @Test
    fun decode_blendOp()  {
        assertEquals(Utils.decodeBlendOp(0), Utils.Companion.BlendOp.APNG_BLEND_OP_SOURCE)
        assertEquals(Utils.decodeBlendOp(1), Utils.Companion.BlendOp.APNG_BLEND_OP_OVER)
    }

    @Test
    fun decode_u32() {
        for (i in 0..Int.MAX_VALUE) {
            val b = ByteBuffer.allocate(4)
            b.putInt(i)
            val array = b.array()
            assertEquals(i, Utils.uIntFromBytesBigEndian(array.map { it.toInt() }))
        }
    }

    @Test
    fun encode_u32() {
        for (i in 0..Int.MAX_VALUE) {
            val b = ByteBuffer.allocate(4)
            b.putInt(i)
            val array = b.array()
            val arrayTest = uIntToByteArray(i)
            for (y in 0 until 4)
                assertEquals(array[y], arrayTest[y])
        }
    }

    @Test
    fun decode_u16() {
        for (i in 0..Short.MAX_VALUE) {
            val b = ByteBuffer.allocate(2)
            b.putShort(i.toShort())
            val array = b.array()
            assertEquals(i, Utils.uShortFromBytesBigEndian(array.map { it.toInt() }))
        }
    }

    @Test
    fun encode_u16() {
        for (i in 0..Short.MAX_VALUE) {
            val b = ByteBuffer.allocate(2)
            b.putShort(i.toShort())
            val array = b.array()
            val arrayTest = uShortToByteArray(i)
            for (y in 0 until 2)
                assertEquals(array[y], arrayTest[y])
        }
    }
}