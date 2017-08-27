package tanvd.bayou.prototype.utils

object CodeUtils {
    fun qualifyWithImports(code: String, imports: List<String>): String {
        var resultCode = code
        for (import in imports) {
            val notPartOfLiteral = "[^\\d\\w]"
            val shortName = Regex(".*\\.(\\w+)").find(import)!!.first()
            resultCode = Regex("($notPartOfLiteral)($shortName)").replace(resultCode, "\$1$import")
            resultCode = Regex("(^$shortName)").replace(resultCode, import)
        }
        return resultCode
    }
}