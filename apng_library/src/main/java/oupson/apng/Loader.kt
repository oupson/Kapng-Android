package oupson.apng

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.net.URL

class Loader {
    companion object {
        /**
         * Download file from given url
         * @param context Context of app
         * @param url Url of the file to download
         * @return [ByteArray] of the file
         */
        @Suppress("RedundantSuspendModifier")
        @Throws(IOException::class)
        suspend fun load(context: Context, url: URL): File = withContext(Dispatchers.IO) {
            val currentDir = context.filesDir
            val fileTXT = File(currentDir, "apngLoader.txt")
            val filePNG = File(currentDir, "apngLoader.png")
            if (fileTXT.exists() && url.toString() == fileTXT.readText()) {
                filePNG
            } else {
                val connection = url.openConnection()
                connection.connect()
                val input = BufferedInputStream(connection.getInputStream())
                val bytes = input.readBytes()
                input.close()
                fileTXT.writeText(url.toString())
                filePNG.apply { writeBytes(bytes) }
            }
        }
    }
}