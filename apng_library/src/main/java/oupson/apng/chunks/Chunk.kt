package oupson.apng.chunks

/**
 * An interface for the png chunks
 */
interface Chunk {
    var body : ByteArray

    /**
     * Parse the chunk
     * @param byteArray The chunk with the length and the crc
     */
    fun parse(byteArray: ByteArray)
}