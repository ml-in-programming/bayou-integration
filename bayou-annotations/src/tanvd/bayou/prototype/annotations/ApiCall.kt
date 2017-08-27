package tanvd.bayou.prototype.annotations

@Repeatable
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class ApiCall(val name: String)