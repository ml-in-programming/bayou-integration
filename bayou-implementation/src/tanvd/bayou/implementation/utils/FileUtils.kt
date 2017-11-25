package tanvd.bayou.implementation.utils

import java.io.*

object Resource {
    fun getLines(fileName: String): List<String> {
        return BufferedReader(InputStreamReader(FileInputStream(getFile(fileName)))).use {
            it.readLines()
        }
    }

    fun getInputStream(fileName: String): InputStream {
        return FileInputStream(getFile(fileName))
    }

    fun getFile(fileName: String): File {
        return File(getPath(fileName))
    }

    fun getPath(fileName: String): String {
        return javaClass.classLoader.getResource(fileName).file
    }
 }