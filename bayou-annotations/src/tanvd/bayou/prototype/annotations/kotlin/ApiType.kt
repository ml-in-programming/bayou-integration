package tanvd.bayou.prototype.annotations.kotlin

import kotlin.reflect.KClass

@Repeatable
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class ApiType(val name: KClass<*>)