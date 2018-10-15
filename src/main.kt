import compressor.Compressor
import compressor.io.binary.BinaryCompressionInput
import compressor.io.binary.BinaryCompressionOutput
import compressor.models.CompressionParams
import compressor.models.FileConfig
import compressor.models.InOutStreamPair
import compressor.search.SlidingWindowSearch
import java.io.File
import java.nio.file.Paths

private val rootPath = Paths.get("").toAbsolutePath().toString()
private val pristineConfig = FileConfig(rootPath + "/src/samples/pristine/")
private val compressedConfig = FileConfig(rootPath + "/src/samples/compressed/", ".compressed")
private val uncompressedConfig = FileConfig(rootPath + "/src/samples/uncompressed/")

/**
* Driver code for our compressor.
* Will compress all files in the "/pristine" folder.
 *
 * Tests can be found in the [CompressorTests] file.
*/
fun main(args: Array<String>) {

    val params = CompressionParams()
    val compressor = Compressor(params, SlidingWindowSearch(params))

    compressFiles(compressor, params)

    decompressFiles(compressor, params)
}

fun compressFiles(compressor: Compressor, params: CompressionParams) {
    getFiles(pristineConfig.dir)?.forEach { pristineFile ->
        val compressedFilePath = compressedConfig.makePathForName(pristineFile.name)
        with(InOutStreamPair(pristineFile.path, compressedFilePath)) {
            compressor.compress("${pristineFile.name} Compression", inStream, BinaryCompressionOutput(outStream, params))
        }
    }
}

fun decompressFiles(compressor: Compressor, params: CompressionParams) {
    getFiles(compressedConfig.dir)?.forEach { compressedFile ->
        with(InOutStreamPair(compressedFile.path, uncompressedConfig.makePathForName(compressedFile.nameWithoutExtension))) {
            compressor.decompress("${compressedFile.name} Decompression", BinaryCompressionInput(inStream, params), outStream)
        }
    }
}

fun getFiles(dirPath: String): Collection<File>? = File(dirPath).listFiles().filterNot { it.name[0] == '.' }
