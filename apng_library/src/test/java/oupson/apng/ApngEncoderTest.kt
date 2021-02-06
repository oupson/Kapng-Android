package oupson.apng

import junit.framework.TestCase.*
import org.junit.Test
import oupson.apng.encoder.ApngEncoder
import oupson.apng.exceptions.BadParameterException
import java.io.ByteArrayOutputStream

class ApngEncoderTest {
    @Test
    fun testOptimiseApngGetterSetter() {
        val outputStream = ByteArrayOutputStream()
        val encoder = ApngEncoder(outputStream, 500, 500, 10)
        assertTrue(encoder.isAlphaEncoded())
        assertTrue(encoder.isOptimisingApng())

        encoder.setOptimiseApng(false)
        encoder.setEncodeAlpha(false)

        assertFalse(encoder.isOptimisingApng())

        try {
            encoder.setOptimiseApng(true)
            fail("setOptimiseApng(true) must throw an exception when encode alpha is false")
        } catch (e : BadParameterException) {
            // Good behavior
        }

        encoder.setEncodeAlpha(true)
        encoder.setOptimiseApng(true)

        assertTrue(encoder.isAlphaEncoded())
        outputStream.close()
    }

    @Test
    fun testEncodeAlphaGetterSetter() {
        val outputStream = ByteArrayOutputStream()
        val encoder = ApngEncoder(outputStream, 500, 500, 10)
        assertTrue(encoder.isAlphaEncoded())
        assertTrue(encoder.isOptimisingApng())

        try {
            encoder.setEncodeAlpha(false)
            fail("setEncodeAlpha(false) must throw an exception when optimise apng is true")
        } catch (e : BadParameterException) {
            // Good behavior
        }

        encoder.setOptimiseApng(false)
        encoder.setEncodeAlpha(false)
        assertFalse(encoder.isAlphaEncoded())
        outputStream.close()
    }

    @Test
    fun testFilters() {
        val outputStream = ByteArrayOutputStream()
        val encoder = ApngEncoder(outputStream, 500, 500, 10)

        for (filters in arrayListOf(ApngEncoder.FILTER_LAST, ApngEncoder.FILTER_NONE, ApngEncoder.FILTER_SUB, ApngEncoder.FILTER_UP)) {
            encoder.setFilter(filters)
            assertEquals(filters, encoder.getFilter())
        }

        try {
            encoder.setFilter(999)
            fail("Invalid filter must throw and exception")
        } catch (e : BadParameterException) {
            // Good behavior
        }
        outputStream.close()
    }

    @Test
    fun testCompressionLevel() {
        val outputStream = ByteArrayOutputStream()
        val encoder = ApngEncoder(outputStream, 500, 500, 10)
        for (i in 0..9) {
            encoder.setCompressionLevel(i)
            assertEquals(encoder.getCompressionLevel(), i)
        }

        try {
            encoder.setCompressionLevel(999)
            fail("Invalid compression level must throw and exception")
        } catch (e : BadParameterException) {
            // Good behavior
        }

        outputStream.close()
    }
}