package compressor

import compressor.io.binary.BinaryCompressionInput
import compressor.io.binary.BinaryCompressionOutput
import compressor.models.CompressionParams
import compressor.models.FileConfig
import compressor.models.InOutStreamPair
import compressor.search.SlidingWindowSearch
import getFiles
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import util.log
import java.io.File
import java.nio.file.Paths
import java.util.*
import kotlin.test.assertTrue

/**
 * Unit tests for Compression and Decompression mainly to assert:
 * - Files are not corrupted through the process
 * - Aggregate file size is diminished after compression
 */
internal class CompressorTest {
    private val rootPath = Paths.get("").toAbsolutePath().toString()
    private val pristineConfig = FileConfig(rootPath + "/src/samples/pristine/")
    private val compressedConfig = FileConfig(rootPath + "/src/samples/compressed/", ".compressed")
    private val uncompressedConfig = FileConfig(rootPath + "/src/samples/uncompressed/")


    @Test
    @DisplayName("Files contents uncorrupted and identical after compression and decompression")
    fun compressionDoesNotCorrupt() {
        val params = CompressionParams()
        var compressor = Compressor(params, SlidingWindowSearch(params))
        var pristineFileName: String? = null
        var pristineFile: File? = null
        var compressedFile: File? = null
        var compressedFilePath: String? = null
        var uncompressedFilePath: String? = null
        var uncompressedFile: File? = null
        var allFilesAreTheSame = true

        // get a pristine file
        getFiles(pristineConfig.dir)?.forEach {
            pristineFileName = it.name
            pristineFile = it


            // compress the pristine file to compressed file
            pristineFile?.let { pristineF ->
                compressedFilePath = compressedConfig.makePathForName(pristineF.name)
                with(InOutStreamPair(pristineF.path, compressedFilePath!!)) {
                    compressor.compress("${pristineF.name} Compression", inStream, BinaryCompressionOutput(outStream, params))
                }
            }

            // get the compressed version of the file
            getFiles(compressedConfig.dir)?.first { it.nameWithoutExtension == pristineFileName }?.let {
                compressedFile = it
            }

            // uncompress the compressed version of the file
            compressedFile?.let { compressedF ->
                uncompressedFilePath = uncompressedConfig.makePathForName(compressedF.nameWithoutExtension)
                with(InOutStreamPair(compressedF.path, uncompressedFilePath!!)) {
                    compressor.decompress("${compressedF.name} Decompression", BinaryCompressionInput(inStream, params), outStream)
                }
            }

            // get the uncompressed file
            getFiles(uncompressedConfig.dir)?.first { it.name == pristineFileName }?.let {
                uncompressedFile = it
            }
            val theseFilesAreTheSame = Arrays.equals(pristineFile?.readBytes(), uncompressedFile?.readBytes())
            log("Uncompressed file is uncorrupted from pristine state: $theseFilesAreTheSame")
            allFilesAreTheSame = allFilesAreTheSame && Arrays.equals(pristineFile?.readBytes(), uncompressedFile?.readBytes())
        }
        
        // assert that the pristine file and uncompressed file are now the same size
        assertTrue(allFilesAreTheSame)
    }

    @Test
    @DisplayName("Zero-length files do not break compressor.")
    fun compressEmptyFile() {
        val params = CompressionParams()
        var compressor = Compressor(params, SlidingWindowSearch(params))
        var pristineFileName: String? = null
        var pristineFile: File? = null
        var compressedFile: File? = null
        var compressedFilePath: String? = null
        var uncompressedFilePath: String? = null
        var uncompressedFile: File? = null

        // get the empty pristine file
        getFiles(pristineConfig.dir)?.first { it.name == "emptyfile" }?.let {
            pristineFileName = it.name
            pristineFile = it
        }

        // compress the pristine file to compressed file
        pristineFile?.let { pristineF ->
            compressedFilePath = compressedConfig.makePathForName(pristineF.name)
            with(InOutStreamPair(pristineF.path, compressedFilePath!!)) {
                compressor.compress("${pristineF.name} Compression", inStream, BinaryCompressionOutput(outStream, params))
            }
        }

        // get the compressed version of the file
        getFiles(compressedConfig.dir)?.first { it.nameWithoutExtension == pristineFileName }?.let {
            compressedFile = it
        }

        // uncompress the compressed version of the file
        compressedFile?.let { compressedF ->
            uncompressedFilePath = uncompressedConfig.makePathForName(compressedF.nameWithoutExtension)
            with(InOutStreamPair(compressedF.path, uncompressedFilePath!!)) {
                compressor.decompress("${compressedF.name} Decompression", BinaryCompressionInput(inStream, params), outStream)
            }
        }

        // get the uncompressed file
        getFiles(uncompressedConfig.dir)?.first { it.name == pristineFileName }?.let {
            uncompressedFile = it
        }

        val filesAreEqual = Arrays.equals(pristineFile?.readBytes(), uncompressedFile?.readBytes())
        val isStillZeroSize = uncompressedFile?.readBytes()?.size == 0
        // assert that the pristine file and uncompressed file are now the same size
        assertTrue(filesAreEqual && isStillZeroSize)
    }

    @Test
    @DisplayName("Files contents are smaller after compression")
    fun fileAggregateSizeIsSmallerAfterCompression() {

        /**
         * NOTE: This test compresses all of the files, but does not decompress them all again. So the uncompressed folder
         * will likely have less files in it.
         * */
        val params = CompressionParams()
        var compressor = Compressor(params, SlidingWindowSearch(params))
        var pristineFileName: String? = null
        var pristineFile: File? = null
        var compressedFile: File? = null
        var compressedFilePath: String? = null
        var uncompressedFilePath: String? = null
        var uncompressedFile: File? = null

        var totalPristineSize = 0L
        var totalCompressedSize = 0L

        // get a pristine file
        getFiles(pristineConfig.dir)?.forEach {
            pristineFileName = it.name
            pristineFile = it

            // compress the pristine file to compressed file
            pristineFile?.let { pristineF ->
                totalPristineSize += pristineF.length()
                compressedFilePath = compressedConfig.makePathForName(pristineF.name)
                with(InOutStreamPair(pristineF.path, compressedFilePath!!)) {
                    compressor.compress("${pristineF.name} Compression", inStream, BinaryCompressionOutput(outStream, params))
                }
            }

            // get the compressed version of the file
            getFiles(compressedConfig.dir)?.first { it.nameWithoutExtension == pristineFileName }?.let {
                compressedFile = it
                totalCompressedSize += it.length()
            }
        }

        val diff = totalPristineSize - totalCompressedSize
        val percentDiff = (diff.toFloat() / totalPristineSize) * 100
        log("Pristine aggregate size: $totalCompressedSize vs Compressed aggregate size: $totalCompressedSize")
        log("Files compressed by %$percentDiff")

        // assert that the pristine file and uncompressed file are now the same size
        assertTrue(totalCompressedSize < totalPristineSize)
    }

    fun getFiles(dirPath: String): Collection<File>? = File(dirPath).listFiles().filterNot { it.name[0] == '.' }
}