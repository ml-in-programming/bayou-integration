package tanvd.bayou.implementation.utils

import tanvd.bayou.implementation.facade.DownloadProgress
import tanvd.bayou.implementation.facade.DownloadProgressProvider
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.reflect.KClass
import java.io.FileOutputStream
import java.net.URL
import java.io.BufferedInputStream





data class RepositoryRecord(val target: String, val name: String, val printableName: String)

object Downloader {
    private val tmp: String = System.getProperty("java.io.tmpdir")
    private fun getRootPath(name: String) = Paths.get(tmp, "bayou_plugin", name)
    private fun getTargetPath(target: String, name: String) = Paths.get(tmp, "bayou_plugin", target, name)


    private val repositoryFile = getRootPath("repository.json").toFile()
    private var repository: MutableList<RepositoryRecord>

    init {
        if (repositoryFile.exists()) {
            repository = JsonUtils.readCollectionValue(repositoryFile.readText(),
                    MutableList::class as KClass<MutableList<RepositoryRecord>>, RepositoryRecord::class)
        } else {
            repository = ArrayList()
            repositoryFile.parentFile.mkdirs()
            saveRepository()
        }
    }

    fun getTargetFiles(target: String) : List<RepositoryRecord> {
        return repository.filter { it.target == target}
    }

    fun downloadFile(target: String, name: String, url: String, printableName: String = name): File {
        return repository.firstOrNull {it.name == name && it.target == target }?.let {
            getTargetPath(it.target, it.name).toFile()
        } ?: downloadTo(printableName, URL(url), getTargetPath(target, name), DownloadProgressProvider.getProgress()).also {
            repository.add(RepositoryRecord(target, name, printableName))
            saveRepository()
        }
    }

    fun downloadZip(target: String, name: String, url: String, printableName: String = name): File {
        return repository.firstOrNull {it.name == name && it.target == target }?.let {
            getTargetPath(it.target, it.name).toFile()
        } ?: downloadTo(printableName, URL(url), getTargetPath(target, "$name.zip"), DownloadProgressProvider.getProgress()).let {
            val zip = Zip.extractFolder(it, getTargetPath(target, name).toFile())
            repository.add(RepositoryRecord(target, name, printableName))
            saveRepository()
            zip
        }
    }

    private fun downloadTo(printableName: String, url: URL, path: Path, progress: DownloadProgress): File {
        progress.name = printableName
        progress.progress = 0.0

        path.toFile().parentFile.mkdirs()


        val conn = url.openConnection()
        val size = conn.contentLength

        BufferedInputStream(url.openStream()).use {
            val out = FileOutputStream(path.toFile())
            val data = ByteArray(1024)
            var totalCount = 0
            var count = it.read(data, 0, 1024)
            while (count != -1) {
                out.write(data, 0, count)
                totalCount += count
                progress.progress = if (size == 0 ) {
                    0.0
                } else {
                    totalCount.toDouble() / size
                }

                count = it.read(data, 0, 1024)
            }

        }

        progress.progress = 1.0
        return path.toFile()
    }

    private fun saveRepository() {
        repositoryFile.writeText(JsonUtils.writeValueAsString(repository))
    }
}