package tanvd.bayou.implementation.utils

fun String.dropWhileIncluding(body: (Char) -> Boolean): String = dropWhile(body).drop(1)

fun String.dropLastWhileIncluding(body: (Char) -> Boolean): String = dropLastWhile(body).dropLast(1)