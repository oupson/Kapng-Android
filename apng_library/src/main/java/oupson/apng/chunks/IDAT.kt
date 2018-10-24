package oupson.apng.chunks

class IDAT {
    private var bodySize = -1
    var IDATBody: ArrayList<ByteArray> = ArrayList()

    fun parseIDAT(byteArray: ByteArray) {
        for (i in 0 until byteArray.size) {
            // Find IDAT chunk
            if (byteArray[i] == 0x49.toByte() && byteArray[i + 1] == 0x44.toByte() && byteArray[ i + 2 ] == 0x41.toByte() && byteArray[ i + 3 ] == 0x54.toByte()) {

                // Find the chunk length
                var lengthString = ""
                byteArray.copyOfRange( i - 4, i).forEach {
                    lengthString += String.format("%02x", it)
                }
                bodySize = lengthString.toLong(16).toInt()

                // Get image bytes
                val _IDATbody = ArrayList<Byte>()
                for (j in i +4 until i + 4 + bodySize) {
                    _IDATbody.add(byteArray[j])
                }
                IDATBody.add(_IDATbody.toByteArray())
            }
        }

    }

}