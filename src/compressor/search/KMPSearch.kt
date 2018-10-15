package compressor.search

import compressor.models.CompressionParams
import compressor.models.MatchResult

/**
 * A class which uses Knuth-Morris-Pratt table in order to search for longest shared substrings.
 * In practice, performance is generally no faster than our sliding window search.
 */
class KMPSearch(val compressionParams: CompressionParams) : SearchContract {

    override fun findMatch(windowBuffer: ArrayList<Byte>, lookaheadBuffer: ArrayList<Byte>): MatchResult {
        return search(windowBuffer.toByteArray(), lookaheadBuffer.toByteArray())
    }

    fun search(txt: ByteArray, pat: ByteArray): MatchResult {
        if (txt.isEmpty()) return MatchResult(0, 0)
        var bestLen = 0
        var bestOffset = 0
        val patSize = pat.size
        val txtSize = txt.size
        val lps = IntArray(patSize)
        var patIdx = 0

        createKMPTable(pat, patSize, lps)

        val lpsSearchStartTime = System.currentTimeMillis()
        var txtIdx = 0
        var loopsRun = 0
        while (txtIdx < txtSize) {
            if (pat[patIdx] == txt[txtIdx]) {
                patIdx++
                txtIdx++
                loopsRun++
            }
            if (patIdx == patSize) { // found the entire pattern, return.
                bestLen = patIdx
                bestOffset = txtIdx - patIdx
                patIdx = lps[patIdx - 1]
            } else if (txtIdx < txtSize && pat[patIdx] != txt[txtIdx]) {
                if (patIdx != 0) {

                    if (patIdx >= bestLen) {
                        bestLen = patIdx // if this is the best len thus far
                        bestOffset = txtIdx - patIdx
                    }
                    if (patIdx >= compressionParams.MAX_CODE_LENGTH_BYTES) {
                        bestLen = compressionParams.MAX_CODE_LENGTH_BYTES
                        break
                    }
                    patIdx = lps[patIdx - 1] // update where in the pattern we want to search from
                } else {
                    txtIdx += 1
                }
            }
        }

        totalLPSSearchTime += System.currentTimeMillis() - lpsSearchStartTime
        val bestMatch = MatchResult(bestLen, bestOffset)

        if (bestMatch.matchedLength < 1) {
            return MatchResult(0, 0)
        } else {
            return MatchResult(bestMatch.matchedLength, (txtSize - bestMatch.offset))
        }
    }

    internal fun createKMPTable(pat: ByteArray, M: Int, lps: IntArray) {
        val computeStartTime = System.currentTimeMillis()
        var len = 0
        var i = 1
        lps[0] = 0 // lps[0] is always 0
        while (i < M) {
            if (pat[i] == pat[len]) {
                len++
                lps[i] = len
                i++
            } else {
                if (len != 0) {
                    len = lps[len - 1]
                } else {
                    lps[i] = len
                    i++
                }
            }
        }
        totalComputeTime += System.currentTimeMillis() - computeStartTime
    }

    companion object {
        var totalComputeTime: Long = 0
        var totalLPSSearchTime: Long = 0
    }
}
