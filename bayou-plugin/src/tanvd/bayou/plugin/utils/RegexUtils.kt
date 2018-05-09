package tanvd.bayou.plugin.utils

fun MatchResult.first(): String {
    return groups[1]!!.value
}