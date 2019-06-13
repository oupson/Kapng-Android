@file:Suppress("unused")

package oupson.apng.exceptions

class NoFrameException : Exception()
class NotPngException : Exception()
class NotApngException : Exception()
class NoFcTL : Exception()
class BadCRC : Exception()
class BadApng(override val message: String? = null) : Exception()