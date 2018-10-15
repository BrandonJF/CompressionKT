package compressor.io.binary

import compressor.io.CompressedIOStream
import compressor.lib.BinaryIn
import compressor.models.Block
import compressor.models.CompressionParams
import java.io.InputStream

/**
 * A [CompressedIOStream] input that will read in a binary file encoded in our Compressed format, converting it to
 * our 'universal' intermediary [Block] class.
 */
class BinaryCompressionInput(val compressedInputStream: InputStream, val params: CompressionParams) : CompressedIOStream.Input {
    private val binaryIn: BinaryIn by lazy { BinaryIn(compressedInputStream.buffered()) }

    override fun getBlockIterator(): Iterator<Block> {
        val blocks = ArrayList<Block>()
        loop@ while (!binaryIn.isEmpty && binaryIn.exists()) {
            val flag = binaryIn.readBoolean()
            when (flag) {
                params.UNENCODED_FLAG_BIT -> {
                    val literal = Block.Literal(binaryIn.readByte())
                    blocks.add(literal)
                }
                params.ENCODED_FLAG_BIT -> {
                    val offset = binaryIn.readInt(params.BITS_ALLOCATED_TO_OFFSET)
                    val length = binaryIn.readInt(params.BITS_ALLOCATED_TO_LENGTH)
                    val backReference = Block.BackReference(offset, length)
                    if (offset == params.STOP_OFFSET && length == params.STOP_LENGTH) {
                        break@loop // we've reached the end of the file.
                    }
                    blocks.add(backReference)
                }
            }
        }
        return blocks.iterator()
    }
}