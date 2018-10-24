package oupson.apng

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URL

class Loader {
    fun load(url : URL) : ByteArray {
        try {
            val connection = url.openConnection()
            connection.connect()
            val input = BufferedInputStream(connection.getInputStream())
            val output = ByteArrayOutputStream()
            val data = ByteArray(1024)
            var count = 0
            while ({count = input.read(data); count}() != -1) {
                output.write(data, 0, count)
            }
            output.flush()
            output.close()
            input.close()
            return output.toByteArray()
        } catch (e: IOException) {
            throw e
        }
    }
}