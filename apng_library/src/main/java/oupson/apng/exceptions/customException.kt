package oupson.apng.exceptions

class NoFrameException : Exception()
class NotPngException : Exception()
class NotApngException : Exception()
class BadCRCException : Exception()
class BadApngException(override val message: String? = null) : Exception()

class InvalidFrameSizeException(override val message: String?) : Exception()