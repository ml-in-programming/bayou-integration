package tanvd.bayou.implementation.model.android.evidence

import tanvd.bayou.implementation.core.evidence.EvidenceProcessor

object ContextTypeProcessor: EvidenceProcessor<Array<Float>> {
    private val tfidf = TfIdfModel("tfidf_model_context.json", "tfidf_vocab_context.json")
    private val lda = LdaHiveModel("lda_model_context.json", 0.1f, 0.03125f)

    override fun wrangle(evidences: List<String>): Array<Float> {
        return lda.getTopicDistribution(tfidf.transform(evidences).map { it.toString() }.toTypedArray()).toTypedArray()
    }
}