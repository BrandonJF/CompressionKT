package compressor.models

/**
 * Convenience data class used to setup file system configurations for the IO streams in our application.
 */
data class FileConfig(val dir: String, val extension: String = "") {
    fun makePathForName(name: String): String {
        return dir + name + extension
    }
}