package compressor.search

import compressor.models.MatchResult

interface SearchContract {
    fun findMatch(windowBuffer: ArrayList<Byte>, lookaheadBuffer: ArrayList<Byte>): MatchResult
}