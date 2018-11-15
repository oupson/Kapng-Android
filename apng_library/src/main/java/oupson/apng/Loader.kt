package oupson.apng

import android.content.Context
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.URL

class Loader {
    companion object {
        fun load(context: Context, url: URL): ByteArray {
            val currenDir = context.filesDir
            val fileTXT = File(currenDir, "apngLoader.txt")
            var filePNG = File(currenDir, "apngLoader.png")
            if (fileTXT.exists() && url.toString() == fileTXT.readText()) {
                return filePNG.readBytes()
            } else {
                try {
                    val connection = url.openConnection()
                    connection.connect()
                    val input = BufferedInputStream(connection.getInputStream())
                    val output = ByteArrayOutputStream()
                    val data = ByteArray(1024)
                    var count = 0
                    while ({ count = input.read(data); count }() != -1) {
                        output.write(data, 0, count)
                    }
                    output.flush()
                    output.close()
                    input.close()
                    fileTXT.writeText(url.toString())
                    filePNG.writeBytes(output.toByteArray())
                    return output.toByteArray()
                } catch (e: IOException) {
                    throw e
                }
            }
        }
    }
}