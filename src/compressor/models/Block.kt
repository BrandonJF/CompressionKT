package compressor.models

sealed class Block {
    class Literal(val byte: Byte) : Block() {
        override fun toString(): String {
            return "${byte.toChar()}"
        }
    }

    class BackReference(val distanceBack: Int, val runLength: Int) : Block() {
        override fun toString(): String {
            return "(ref, $distanceBack, $runLength)"
        }
    }
}