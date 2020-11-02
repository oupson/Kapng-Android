package oupson.apng.chunks

import oupson.apng.utils.Utils

class IHDR : Chunk {
    override var body = byteArrayOf()
    var pngWidth = -1
    var pngHeight = -1

    /**
     * Parse the chunk
     * @param byteArray The chunk with the length and the crc
     */
    override fun parse(byteArray: ByteArray) {
        for (i in byteArray.indices) {
            // Find IHDR chunk
            if (byteArray[i] == 0x49.toByte() && byteArray[i + 1] == 0x48.toByte() && byteArray[ i + 2 ] == 0x44.toByte() && byteArray[ i + 3 ] == 0x52.toByte()) {
                // Get length of the body of the chunk
                val bodySize = Utils.uIntFromBytesBigEndian(byteArray.copyOfRange(i - 4, i).map(Byte::toInt))
                // Get the width of the png
                pngWidth = Utils.uIntFromBytesBigEndian(byteArray.copyOfRange(i +4, i + 8).map(Byte::toInt))
                // Get the height of the png
                pngHeight = Utils.uIntFromBytesBigEndian(byteArray.copyOfRange(i +8, i +12).map(Byte::toInt))
                body = byteArray.copyOfRange(i + 4, i + bodySize + 4)
            }
        }
    }
}