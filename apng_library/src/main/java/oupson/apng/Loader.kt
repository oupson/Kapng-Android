package oupson.apng

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
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
        @Throws(IOException::class, java.io.FileNotFoundException::class)
        suspend fun load(context: Context, url: URL) =
            withContext(Dispatchers.IO) {
                val currentDir = context.filesDir
                val fileTXT = File(currentDir, "apngLoader.txt")
                val filePNG = File(currentDir, "apngLoader.png")
                if (fileTXT.exists() && url.toString() == fileTXT.readText()) {
                    filePNG
                } else {
                    val connection = url.openConnection()
                    connection.connect()
                    val input = BufferedInputStream(connection.getInputStream())
                    val output = FileOutputStream(filePNG)
                    var bytesRead : Int
                    val buffer = ByteArray(4096)
                    do {
                        bytesRead = input.read(buffer)
                        if (bytesRead > -1)
                            output.write(buffer, 0, bytesRead)
                    } while (bytesRead != -1)
                    input.close()
                    output.close()
                    fileTXT.writeText(url.toString())
                    filePNG
                }
            }
    }
}