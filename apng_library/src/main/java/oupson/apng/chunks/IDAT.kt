package oupson.apng.chunks

import oupson.apng.utils.Utils

class IDAT : Chunk {
    @Suppress("PropertyName")
    var IDATBody: ArrayList<ByteArray> = ArrayList()
    override var body = byteArrayOf()

    /**
     * Parse the chunk
     * @param byteArray The chunk with the length and the crc
     */
    override fun parse(byteArray: ByteArray) {
        val i = 4
        // Find IDAT chunk
        if (byteArray[i] == 0x49.toByte() && byteArray[i + 1] == 0x44.toByte() && byteArray[i + 2] == 0x41.toByte() && byteArray[i + 3] == 0x54.toByte()) {
            // Find the chunk length
            val bodySize = Utils.uIntFromBytesBigEndian(byteArray.copyOfRange(i - 4, i).map(Byte::toInt))
            // Get image bytes
            IDATBody.add(byteArray.copyOfRange(i + 4, i + 4 + bodySize))
        }
    }
}