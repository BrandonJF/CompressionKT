package compressor

import compressor.io.CompressedIOStream
import compressor.models.Block
import compressor.models.CompressionJobStats
import compressor.models.CompressionParams
import compressor.search.SearchContract
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.experimental.EmptyCoroutineContext.plus
import kotlin.system.measureTimeMillis

/**
 * The main compression class of our app. The main entry points being [Compressor.compress] and [Compressor.decompress]
 */

class Compressor(val params: CompressionParams, val searchEngine: SearchContract) {

    fun compress(tag: String, uncompressedInputStream: InputStream, compressedOutputStream: CompressedIOStream.Output) {
        measureTimeMillis { doCompress(uncompressedInputStream, compressedOutputStream) }.apply {
            CompressionJobStats(tag, this).display()
        }
    }
    fun decompress(tag: String, compressedInputStream: CompressedIOStream.Input, uncompressedOutputStream: OutputStream) {
        measureTimeMillis { doDecompress(compressedInputStream, uncompressedOutputStream) }.apply {
            CompressionJobStats(tag, this).display()
        }
    }

    /**
     * Compresses our IO input stream according to our CompressedIO stream contract.
     */
    private fun doCompress(uncompressedInputStream: InputStream, compressedOutputStream: CompressedIOStream.Output) {
        val uncompressedBytes: ByteArray = uncompressedInputStream.readBytes()
        val windowBuffer = ArrayList<Byte>(params.WINDOW_BYTES_SIZE + 1)
        val lookaheadBuffer = ArrayList<Byte>(params.MAX_BYTES_UNCODED_LITERAL + 1)
        val byteIterator = uncompressedBytes.iterator()

        fillBuffer(lookaheadBuffer, params.MAX_CODE_LENGTH_BYTES, byteIterator)

        while (lookaheadBuffer.isNotEmpty()) {
            val searchResults = searchEngine.findMatch(windowBuffer, lookaheadBuffer)
            if (searchResults.matchedLength <= params.MAX_BYTES_UNCODED_LITERAL) {
                compressedOutputStream.writeLiteral(lookaheadBuffer[0])
                transferBuffers(1, fromBuffer = lookaheadBuffer, toBuffer = windowBuffer)
            } else {
                val adjustedLen = searchResults.matchedLength - (params.MAX_BYTES_UNCODED_LITERAL + 1) // fit len into fewer bits
                compressedOutputStream.writeBackReference(searchResults.offset, adjustedLen)
                transferBuffers(searchResults.matchedLength, lookaheadBuffer, windowBuffer)
            }
            fillBuffer(lookaheadBuffer, params.MAX_CODE_LENGTH_BYTES, byteIterator)
            trimBuffer(windowBuffer, maxSize = params.WINDOW_BYTES_SIZE)
        }
        compressedOutputStream.writeBackReference(params.STOP_OFFSET, params.STOP_LENGTH)
        compressedOutputStream.finish()
    }

    /**
     * Decompresses our data stream according to our CompressedIO stream contract.
     */
    private fun doDecompress(compressedInputStream: CompressedIOStream.Input, uncompressedOutputStream: OutputStream) {

        val windowBuffer = ArrayList<Byte>(params.WINDOW_BYTES_SIZE + 1)
        val outputStream = ArrayList<Byte>()
        val blockIterator = compressedInputStream.getBlockIterator()

        uncompressedOutputStream.buffered().use {
            while (blockIterator.hasNext()) {
                val block = blockIterator.next()
                when (block) {
                    is Block.Literal -> {
                        outputStream.add(block.byte)
                        windowBuffer.add(block.byte)
                    }
                    is Block.BackReference -> {
                        val offset = block.distanceBack
                        val length = block.runLength + (params.MAX_BYTES_UNCODED_LITERAL + 1) // len was encoded smaller
                        val decodeBuffer = windowBuffer.subList(windowBuffer.size - offset, windowBuffer.size - offset + length)
                        decodeBuffer.forEach { outputStream.add(it) }
                        windowBuffer.addAll(decodeBuffer)
                        trimBuffer(windowBuffer, maxSize = params.WINDOW_BYTES_SIZE)
                    }
                }
            }
            uncompressedOutputStream.write(outputStream.toByteArray())
        }
    }

    /**
     * Fill a buffer using data from the source iterator.
     */
    private fun fillBuffer(buffer: ArrayList<Byte>, maxSize: Int, source: ByteIterator) {
        while (buffer.size < maxSize && source.hasNext()) {
            buffer.add(source.nextByte()) // if the buffer is not filled, fill with file bytes first
        }
    }

    /**
     * Transfer n bytes of data from one buffer to another.
     */
    private fun transferBuffers(nBytesToTransfer: Int, fromBuffer: ArrayList<Byte>, toBuffer: ArrayList<Byte>) {
        (1..nBytesToTransfer).forEach { toBuffer.add(fromBuffer.removeAt(0)) }
    }

    /**
     * Trims a buffer so that it does not go beyond the max size.
     */
    private fun trimBuffer(buffer: ArrayList<Byte>, maxSize: Int) {
        while (buffer.size > maxSize) {
            buffer.removeAt(0) // should remove the number of matched items
        }
    }
}