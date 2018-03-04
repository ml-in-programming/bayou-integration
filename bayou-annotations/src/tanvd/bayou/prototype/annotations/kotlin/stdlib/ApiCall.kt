package tanvd.bayou.prototype.annotations.kotlin.stdlib

@Repeatable
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class ApiCall(val name: String)