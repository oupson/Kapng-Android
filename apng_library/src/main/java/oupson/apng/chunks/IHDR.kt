package oupson.apng.chunks

class IHDR {
    private var corpsSize = -1

    var ihdrCorps = byteArrayOf()

    var pngWidth = -1
    var pngHeight = -1

    fun parseIHDR(byteArray: ByteArray) {
        for (i in 0 until byteArray.size) {
            // Find IHDR chunk
            if (byteArray[i] == 0x49.toByte() && byteArray[i + 1] == 0x48.toByte() && byteArray[ i + 2 ] == 0x44.toByte() && byteArray[ i + 3 ] == 0x52.toByte()) {
                // Get length of the corps of the chunk
                var lengthString = ""
                byteArray.copyOfRange(i - 4, i).forEach {
                    lengthString += String.format("%02x", it)
                }
                corpsSize = lengthString.toLong(16).toInt()

                // Get the width of the png
                var pngwidth = ""
                byteArray.copyOfRange(i + 4, i + 8).forEach {
                    pngwidth += String.format("%02x", it)
                }
                pngWidth = pngwidth.toLong(16).toInt()

                // Get the height of the png
                var pngheight = ""
                byteArray.copyOfRange(i + 8, i + 12).forEach {
                    pngheight += String.format("%02x", it)
                }
                pngHeight = pngheight.toLong(16).toInt()

                val _ihdrCorps = ArrayList<Byte>()
                byteArray.copyOfRange(i + 4, i + corpsSize + 4).forEach {
                    _ihdrCorps.add(it)
                }
                ihdrCorps = _ihdrCorps.toByteArray()

            }
        }
    }
}