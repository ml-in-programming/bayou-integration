package tanvd.bayou.implementation

import tanvd.bayou.implementation.facade.DownloadProgress
import tanvd.bayou.implementation.facade.DownloadProgressProvider
import tanvd.bayou.implementation.model.SynthesizingModel
import tanvd.bayou.implementation.model.configurable.ConfigurableSynthesizingModel
import tanvd.bayou.implementation.model.configurable.wrangle.WrangleModelProvider
import tanvd.bayou.implementation.utils.Downloader
import tanvd.bayou.implementation.utils.JsonUtils

object BayouClient {
    fun getConfigurableModel(configStr: String): SynthesizingModel {
        return ConfigurableSynthesizingModel(JsonUtils.readValue(configStr, Config::class))
    }

    fun existsModel(model: String): Boolean {
        return Downloader.getTargetFiles(model).isNotEmpty()
    }

    fun downloadModel(configStr: String, progress: DownloadProgress) {
        val progressFuncLast = DownloadProgressProvider.getProgress
        DownloadProgressProvider.getProgress = { progress }


        val config = JsonUtils.readValue(configStr, Config::class)

        progress.phase = "Download classpath artifacts"
        config.classpath.forEach {
            Downloader.downloadFile(config.name, it.lib_name, it.lib_url)
        }

        progress.phase = "Download embedding artifacts"
        config.evidences.forEach {
            WrangleModelProvider.download(config.name, it)
        }

        progress.phase = "Download model artifacts"
        Downloader.downloadFile(config.name, "synth_config", config.synthesizer.config_url, "Synthesizing Config")
        Downloader.downloadZip(config.name, "synth_model", config.synthesizer.model_url, "Synthesizing Model")

        DownloadProgressProvider.getProgress = progressFuncLast
    }

    fun setDownloadProgress(func: () -> DownloadProgress) {
        DownloadProgressProvider.getProgress = func
    }
}