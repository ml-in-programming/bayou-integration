package tanvd.bayou.implementation.model.android.evidence

import tanvd.bayou.implementation.core.evidence.EvidenceProcessor

object ApiTypeProcessor : EvidenceProcessor<Array<Float>> {
    private val tfidf = TfIdfModel("tfidf_model_types.json", "tfidf_vocab_types.json")
    private val lda = LdaHiveModel("lda_model_types.json", 0.1f, 0.015625f)

    override fun wrangle(evidences: List<String>): Array<Float> {
        return lda.getTopicDistribution(tfidf.transform(evidences).map { it.toString() }.toTypedArray()).toTypedArray()
    }
}