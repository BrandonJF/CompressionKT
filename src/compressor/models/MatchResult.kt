package compressor.models

/**
 * A data class for holding referential data to other bytes used by
 * the compressor itself as well as the search algorithms
 */
data class MatchResult(val matchedLength: Int, val offset: Int)