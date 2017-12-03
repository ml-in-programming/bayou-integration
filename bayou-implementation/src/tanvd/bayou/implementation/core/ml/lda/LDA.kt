package tanvd.bayou.implementation.core.ml.lda

object LdaTypes : LdaHiveModel("lda_model_types.json", 0.1f, 0.015625f)
object LdaContexts : LdaHiveModel("lda_model_context.json", 0.1f, 0.03125f)
object LdaApi : LdaHiveModel("lda_model_api.json", 0.1f, 0.00390625f)