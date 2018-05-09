package tanvd.bayou.plugin.annotations.kotlin.android

@Repeatable
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class ApiCall(val name: AndroidFunctions)