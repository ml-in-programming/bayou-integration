package tanvd.bayou.plugin.annotations.kotlin.android

import kotlin.reflect.KClass

@Repeatable
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class ContextClass(val klass: KClass<*>)