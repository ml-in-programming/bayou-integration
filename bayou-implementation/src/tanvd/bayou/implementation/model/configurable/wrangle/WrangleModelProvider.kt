package tanvd.bayou.implementation.model.configurable.wrangle

import tanvd.bayou.implementation.EvidenceConfig
import tanvd.bayou.implementation.WrangleType
import tanvd.bayou.implementation.utils.Downloader


object WrangleModelProvider {
    fun wrangle(name: String, config: EvidenceConfig, evidences: List<String>): Array<Float> {
        return when (config.wrangling.type) {
            WrangleType.LDA -> {
                val tfIdfVocabFile = Downloader.downloadFile(name, "tfidf_vocab_${config.type}", config.wrangling.config["tfidf_vocab_url"]!!)
                val tfIdfModelFile = Downloader.downloadFile(name, "tfidf_model_${config.type}", config.wrangling.config["tfidf_model_url"]!!)
                val ldaModelFile = Downloader.downloadFile(name, "lda_model_${config.type}", config.wrangling.config["lda_model_url"]!!)
                val tfidf = TfIdfModel(tfIdfModelFile, tfIdfVocabFile)
                val lda = LdaHiveModel(ldaModelFile, config.wrangling.config["lda_alpha"]!!.toFloat(), config.wrangling.config["lda_eta"]!!.toFloat())
                lda.getTopicDistribution(tfidf.transform(evidences).map { it.toString() }.toTypedArray()).toTypedArray()
            }
            WrangleType.KHot -> {
                val kHotVocabFile = Downloader.downloadFile(name, "tfidf_vocab_${config.type}", config.wrangling.config["khot_vocab"]!!)
                KHotModel(kHotVocabFile).transform(evidences)
            }
        }
    }
}