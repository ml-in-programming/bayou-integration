package tanvd.bayou.implementation.utils

import com.github.kittinunf.fuel.httpGet
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.reflect.KClass

data class RepositoryRecord(val target: String, val name: String)

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


    fun downloadFile(target: String, name: String, url: String): File {
        return repository.firstOrNull {it.name == name && it.target == target }?.let {
            getTargetPath(it.target, it.name).toFile()
        } ?: downloadTo(url, getTargetPath(target, name)).also {
            repository.add(RepositoryRecord(target, name))
            saveRepository()
        }
    }

    fun downloadZip(target: String, name: String, url: String): File {
        return repository.firstOrNull {it.name == name && it.target == target }?.let {
            getTargetPath(it.target, it.name).toFile()
        } ?: downloadTo(url, getTargetPath(target, "$name.zip")).let {
            val zip = Zip.extractFolder(it, getTargetPath(target, name).toFile())
            repository.add(RepositoryRecord(target, name))
            saveRepository()
            zip
        }
    }

    private fun downloadTo(url: String, path: Path): File {
        val (req, resp, res) = url.httpGet().responseString()
        path.toFile().parentFile.mkdirs()
        path.toFile().writeBytes(resp.data)
        return path.toFile()
    }

    private fun saveRepository() {
        repositoryFile.writeText(JsonUtils.writeValueAsString(repository))
    }
}