package tanvd.bayou.prototype.annotations

@Repeatable
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class ApiType(val name: String)