package oupson.apng.chunks

interface Chunk {
    var body : ByteArray
    fun parse(byteArray: ByteArray)
}