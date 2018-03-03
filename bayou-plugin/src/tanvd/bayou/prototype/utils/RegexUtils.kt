package tanvd.bayou.prototype.utils

fun MatchResult.first(): String {
    return groups[1]!!.value
}