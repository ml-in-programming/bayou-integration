/*
Copyright 2017 Rice University

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package tanvd.bayou.implementation.application.dom

import org.apache.commons.lang3.StringUtils
import org.eclipse.jdt.core.dom.*

import java.util.ArrayList
import java.util.Arrays
import java.util.regex.Pattern
import java.util.stream.Collectors

class Utils private constructor() {
    init {
        throw AssertionError("Do not instantiate this class!")
    }

    companion object {

        fun isRelevantCall(binding: IMethodBinding?): Boolean {
            val cls: ITypeBinding? = binding?.declaringClass
            if (binding == null || cls == null)
                return false
            val pack = cls.`package`
            val packs = pack.nameComponents
            if (Visitor.V().options.API_MODULES.contains(packs[0]))
                return true
            if (Visitor.V().options.API_PACKAGES.contains(pack.name))
                return true
            var className = cls.qualifiedName
            if (className.contains("<"))
            /* be agnostic to generic versions */
                className = className.substring(0, className.indexOf("<"))
            return Visitor.V().options.API_CLASSES.contains(className)

        }

        fun checkAndGetLocalMethod(binding: IMethodBinding?): MethodDeclaration? {
            if (binding != null)
                for (method in Visitor.V().allMethods)
                    if (binding.isEqualTo(method.resolveBinding()))
                        return method
            return null
        }

        fun getJavadoc(method: MethodDeclaration, javadocType: String): String? {
            try {
                val doc = method.javadoc
                val fragments = (doc.tags()[0] as TagElement).fragments()
                val str = fragments.joinToString(" ") { f -> getJavadocText(f as IDocElement) }
                if (javadocType == "summary") {
                    val p = Pattern.compile("(.*?)\\.\\W.*")
                    val m = p.matcher(str)
                    return sanitizeJavadoc(if (m.matches()) m.group(1) else str)
                } else if (javadocType == "full") {
                    return str
                }
                return null
            } catch (e: Exception) {
                return null
            }

        }

        private fun getJavadocText(fragment: IDocElement): String {
            if (fragment is TextElement)
                return fragment.text
            if (fragment is Name)
                return fragment.fullyQualifiedName
            if (fragment is MemberRef)
                return fragment.name.identifier
            if (fragment is MethodRef)
                return fragment.name.identifier
            if (fragment is TagElement) {
                val fragments = fragment.fragments()
                return fragments.joinToString(" ") { f -> getJavadocText(f as IDocElement) }
            }
            throw RuntimeException()
        }

        private fun sanitizeJavadoc(str: String): String? {
            var str = str
            val stop_words = arrayOf("TODO", "FIXME", "NOTE", "HACK", "XXX")
            for (w in stop_words)
            // ignore the doc if it contains any stop word
                if (str.contains(w))
                    return null
            str = str.replace("<[^>]*>".toRegex(), "") // remove HTML tags
            val splits = str.split("[\\p{Punct}\\p{Space}]+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val ret = ArrayList<String>()
            for (s in splits) { // split by camel case and make everything lower
                var cc = Arrays.asList(*StringUtils.splitByCharacterTypeCamelCase(s))
                cc = cc.map { c -> c.toLowerCase() }
                ret.add(cc.joinToString(" "))
            }
            return ret.joinToString(" ")
        }
    }
}
