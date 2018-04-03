package tanvd.bayou.implementation.model.configurable.wrangle

import tanvd.bayou.implementation.EvidenceConfig
import tanvd.bayou.implementation.WrangleType
import tanvd.bayou.implementation.utils.Downloader


object WrangleModelProvider {
    fun wrangle(name: String, config: EvidenceConfig, evidences: List<String>): Array<Float> {
        return when (config.wrangling.type) {
            WrangleType.LDA -> {
                val tfIdfVocabFile = Downloader.downloadFile(name, "tfidf_vocab_${config.type}", config.wrangling.config["tfidf_vocab_url"]!!,
                        "TF-IDF Vocabulary for ${config.type}")
                val tfIdfModelFile = Downloader.downloadFile(name, "tfidf_model_${config.type}", config.wrangling.config["tfidf_model_url"]!!,
                        "TF-IDF Model for ${config.type}")
                val ldaModelFile = Downloader.downloadFile(name, "lda_model_${config.type}", config.wrangling.config["lda_model_url"]!!,
                        "LDA Model for ${config.type}")
                val tfidf = TfIdfModel(tfIdfModelFile, tfIdfVocabFile)
                val lda = LdaHiveModel(ldaModelFile, config.wrangling.config["lda_alpha"]!!.toFloat(), config.wrangling.config["lda_eta"]!!.toFloat())
                lda.getTopicDistribution(tfidf.transform(evidences).map { it.toString() }.toTypedArray()).toTypedArray()
            }
            WrangleType.KHot -> {
                val kHotVocabFile = Downloader.downloadFile(name, "khot_vocab_${config.type}", config.wrangling.config["khot_vocab"]!!,
                        "KHot Vocabulary for ${config.type}")
                KHotModel(kHotVocabFile).transform(evidences)
            }
        }
    }

    fun download(name: String, config: EvidenceConfig) {
        when (config.wrangling.type) {
            WrangleType.LDA -> {
                Downloader.downloadFile(name, "tfidf_vocab_${config.type}", config.wrangling.config["tfidf_vocab_url"]!!,
                        "TF-IDF Vocabulary for ${config.type}")
                Downloader.downloadFile(name, "tfidf_model_${config.type}", config.wrangling.config["tfidf_model_url"]!!,
                        "TF-IDF Model for ${config.type}")
                Downloader.downloadFile(name, "lda_model_${config.type}", config.wrangling.config["lda_model_url"]!!,
                        "LDA Model for ${config.type}")
            }
            WrangleType.KHot -> {
                Downloader.downloadFile(name, "khot_vocab_${config.type}", config.wrangling.config["khot_vocab"]!!,
                        "KHot Vocabulary for ${config.type}")
            }
        }
    }
}