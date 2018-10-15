package compressor.models

/**
 * Compression parameters used by many classed (IO, Search, Compressor...) involved in our compression process.
 * The defaults are set to what I found to be the optimal settings (window and code length size) for most files
 * that still works within the bounds of our original spec.
 * */
data class CompressionParams(
    val MAX_BYTES_UNCODED_LITERAL: Int = 2, // most chars allowed without coding in bits
    val MAX_CODE_LENGTH_BYTES: Int = 64, // longest code allowed to copy, buffer size = 2^6 (6bit pointer)
    val WINDOW_BYTES_SIZE: Int = 65535 / 13, // max offset chars to lookback/bytes = 2^16 (16bit pointer)
    val UNENCODED_FLAG_BIT: Boolean = false, // 0 bit
    val ENCODED_FLAG_BIT: Boolean = true, // 1 bit
    val BITS_ALLOCATED_TO_OFFSET: Int = 16, // the maximum amount of space we can store our offset in
    val BITS_ALLOCATED_TO_LENGTH: Int = 6, // the maximum amount of space we can store our offset in
    val STOP_OFFSET: Int = 0, // Values at which we'll stop reading from our filestream.
    val STOP_LENGTH: Int = 0
)