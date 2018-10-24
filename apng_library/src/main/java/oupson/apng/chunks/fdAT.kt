package oupson.apng.chunks

class fdAT {
    private var bodySize = -1
    var fdATBody: ArrayList<ByteArray> = ArrayList()

    fun parsefdAT(byteArray: ByteArray, position: Int) {
        for (i in position until byteArray.size) {
            // Find fdAT chunk
            if (byteArray[i] == 0x66.toByte() && byteArray[i + 1] == 0x64.toByte() && byteArray[ i + 2 ] == 0x41.toByte() && byteArray[ i + 3 ] == 0x54.toByte()) {

                // Find the chunk length
                var lengthString = ""
                byteArray.copyOfRange( i - 4, i).forEach {
                    lengthString += String.format("%02x", it)
                }
                bodySize = lengthString.toLong(16).toInt()

                // Get image bytes
                val _fdATbody = ArrayList<Byte>()
                for (j in i +4 until i + 4 + bodySize) {
                    _fdATbody.add(byteArray[j])
                }
                fdATBody.add(_fdATbody.toByteArray())
            }
            // Find idat chunk
            else if (byteArray[i] == 0x49.toByte() && byteArray[i + 1] == 0x44.toByte() && byteArray[ i + 2 ] == 0x41.toByte() && byteArray[ i + 3 ] == 0x54.toByte()) {
                // Find the chunk length
                var lengthString = ""
                byteArray.copyOfRange( i - 4, i).forEach {
                    lengthString += String.format("%02x", it)
                }
                bodySize = lengthString.toLong(16).toInt()

                // Get image bytes
                val _fdATbody = ArrayList<Byte>()
                for (j in i +4 until i + 4 + bodySize) {
                    _fdATbody.add(byteArray[j])
                }
                fdATBody.add(_fdATbody.toByteArray())
            }
        }

    }
}