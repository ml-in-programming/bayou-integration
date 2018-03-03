package tanvd.bayou.implementation.utils

import java.io.*
import java.nio.file.Paths

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
        val file = getPath(fileName)
//        return if (!file.startsWith("file:")) {
//            "file:$file"
//        } else {
            return file
//        }
    }
}