package compressor.models

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Creates a pair of In/Out Streams given paths. A convenience data class.
 * */
class InOutStreamPair(inputFilePath: String, outputFilePath: String) {
    val inStream: FileInputStream = File(inputFilePath).inputStream()
    val outStream: FileOutputStream = File(outputFilePath).outputStream()
}