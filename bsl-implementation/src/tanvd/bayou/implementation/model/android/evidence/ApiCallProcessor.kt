package tanvd.bayou.implementation.model.android.evidence

import tanvd.bayou.implementation.core.evidence.EvidenceProcessor

object ApiCallProcessor : EvidenceProcessor<Array<Float>> {
    private val tfidf = TfIdfModel("tfidf_model_api.json", "tfidf_vocab_api.json")
    private val lda = LdaHiveModel("lda_model_api.json", 0.1f, 0.00390625f)

    override fun wrangle(evidences: List<String>): Array<Float> {
        return lda.getTopicDistribution(tfidf.transform(evidences).map { it.toString() }.toTypedArray()).toTypedArray()
    }
}