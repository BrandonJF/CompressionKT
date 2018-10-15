package compressor.io.binary

import compressor.io.CompressedIOStream
import compressor.lib.BinaryOut
import compressor.models.CompressionParams
import java.io.OutputStream

/**
 * A [CompressedIOStream] output that will write in a binary file encoded in our Compressed format/
 */
class BinaryCompressionOutput(val compressedOutputStream: OutputStream, val params: CompressionParams) : CompressedIOStream.Output {
    private val binaryOut: BinaryOut by lazy { BinaryOut(compressedOutputStream) }

    override fun writeLiteral(byte: Byte) {
        binaryOut.write(params.UNENCODED_FLAG_BIT)
        binaryOut.write(byte)
    }

    override fun writeBackReference(offset: Int, length: Int) {
        binaryOut.write(params.ENCODED_FLAG_BIT)
        binaryOut.write(offset, params.BITS_ALLOCATED_TO_OFFSET)
        binaryOut.write(length, params.BITS_ALLOCATED_TO_LENGTH)
    }

    override fun finish() {
        binaryOut.close() // close out and flush our stream.
    }
}