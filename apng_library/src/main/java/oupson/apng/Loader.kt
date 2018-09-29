package oupson.apng

import android.os.Bundle
import android.widget.ImageView
import java.io.*
import java.net.URI
import java.net.URL
import java.net.URLConnection

class Loader {
    fun load(url : URL) : ByteArray {
        try {
            val connection = url.openConnection()
            connection.connect()
            // this will be useful so that you can show a typical 0-100% progress bar
            val fileLength = connection.contentLength
            // download the file
            val input = BufferedInputStream(connection.getInputStream())
            val output = ByteArrayOutputStream()

            val data = ByteArray(1024)
            var count: Int = 0
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
    fun load(file : File) : ByteArray {
        return file.readBytes()
    }
}