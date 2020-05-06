package oupson.apng

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class Loader {
    companion object {
        /**
         * Download file from given url.
         * @param url Url of the file to download.
         * @return [ByteArray] of the file.
         */
        @Throws(IOException::class, Exception::class)
        suspend fun load(url: URL): ByteArray =
            withContext(Dispatchers.IO) {
                val connection = url.openConnection() as HttpURLConnection
                connection.useCaches = true
                connection.connect()
                if (connection.responseCode == 200) {
                    val input = BufferedInputStream(connection.inputStream)
                    val output = ByteArrayOutputStream()
                    var bytesRead: Int
                    val buffer = ByteArray(4096)
                    do {
                        bytesRead = input.read(buffer)
                        if (bytesRead > -1)
                            output.write(buffer, 0, bytesRead)
                    } while (bytesRead != -1)
                    input.close()
                    output.close()
                    connection.disconnect()
                    output.toByteArray()
                } else {
                    connection.disconnect()
                    throw Exception("Error when downloading file : ${connection.responseCode}")
                }
            }
    }
}