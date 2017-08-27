package tanvd.bayou.prototype.language

import com.intellij.lang.Language

class BayouLanguage private constructor() : Language("Bayou") {
    companion object {
        val INSTANCE = BayouLanguage()
    }
}