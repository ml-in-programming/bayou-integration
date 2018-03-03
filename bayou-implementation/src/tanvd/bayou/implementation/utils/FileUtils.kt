package tanvd.bayou.implementation.utils

import org.apache.logging.log4j.LogManager
import java.io.*
import java.nio.file.Paths
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.io.BufferedInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile



object Resource {
    private val preparedNames: HashMap<String, String> = HashMap()
    private var firstEntryHook: (String) -> String = { Paths.get(javaClass.classLoader.getResource(it).toURI()).toFile().absolutePath }

    fun setHook(hook: (String) -> String) {
        firstEntryHook = hook
    }

    fun getLines(fileName: String): List<String> {
        return BufferedReader(InputStreamReader(FileInputStream(getFile(fileName)))).use {
            it.readLines()
        }
    }

    fun getJarStream(fileName: String): InputStream {
        return javaClass.classLoader.getResource(fileName).openStream()
    }

    fun getInputStream(fileName: String): InputStream {
        return FileInputStream(getFile(fileName))
    }

    fun getFile(fileName: String): File {
        return File(getPath(fileName))
    }

    fun getPath(fileName: String): String {
        return preparedNames[fileName]?.let {
            it
        } ?: firstEntryHook(fileName).also {
            preparedNames.put(fileName, it)
        }
    }

    fun getClassPath(fileName: String): String {
        return getPath(fileName)
    }
}

object Zip {

    private val logger = LogManager.getLogger(Zip::class.java)

    fun extractFolder(zipFile: File, extractFolder: File): File {
        try {
            val zip = ZipFile(zipFile)

            extractFolder.mkdirs()
            val zipFileEntries = zip.entries()

            // Process each entry
            while (zipFileEntries.hasMoreElements()) {
                // grab a zip file entry
                val entry = zipFileEntries.nextElement() as ZipEntry
                val currentEntry = entry.name

                val destFile = File(extractFolder, currentEntry)
                //destFile = new File(newPath, destFile.getName());
                val destinationParent = destFile.parentFile

                // create the parent directory structure if needed
                destinationParent.mkdirs()

                if (!entry.isDirectory) {
                    val `is` = BufferedInputStream(zip.getInputStream(entry))
                    // establish buffer for writing file
                    val data = ByteArray(2048)

                    // write the current file to disk
                    val fos = FileOutputStream(destFile)
                    val dest = BufferedOutputStream(fos, 2048)

                    // read and write until last byte is encountered
                    var currentByte = `is`.read(data, 0, 2048)
                    while (currentByte != -1) {
                        dest.write(data, 0, currentByte)
                        currentByte = `is`.read(data, 0, 2048)
                    }
                    dest.flush()
                    dest.close()
                    `is`.close()
                }


            }
        } catch (e: Exception) {
            logger.error("Zipping error", e)
        }
        return extractFolder

    }
}