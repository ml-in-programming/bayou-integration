package tanvd.bayou.prototype.annotations.kotlin.android

@Repeatable
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class ApiCall(val name: AndroidFunctions)