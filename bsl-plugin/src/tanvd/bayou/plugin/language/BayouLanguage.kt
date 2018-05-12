package tanvd.bayou.plugin.language

import com.intellij.lang.Language

class BayouLanguage private constructor() : Language("BSL") {
    companion object {
        val INSTANCE = BayouLanguage()
    }
}