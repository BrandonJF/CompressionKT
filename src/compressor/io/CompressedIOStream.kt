package compressor.io

import compressor.models.Block

/**
 * Compressed format contract for writing data out and reading it in using our compressed format.
 */
interface CompressedIOStream {

    /**
     * Output should be capable of writing both literals and back references. Implementing a finish method when
     * it's
     */
    interface Output {
        fun writeLiteral(byte: Byte)
        fun writeBackReference(offset: Int, length: Int)
        fun finish()
    }

    /**
     * An input is expected to rended data to the compressor in the intermediary [Block] format.
     */
    interface Input {
        fun getBlockIterator(): Iterator<Block>
    }
}