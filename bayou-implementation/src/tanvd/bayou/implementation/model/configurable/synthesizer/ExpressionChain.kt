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
package tanvd.bayou.implementation.model.configurable.synthesizer

import java.lang.reflect.Method
import java.util.ArrayList

internal class ExpressionChain {

    val `var`: Variable

    // these two lists are synchronized
    val methods: MutableList<Method>
    val types: MutableList<Type>

    val currentType: Type
        get() = if (types.isEmpty()) `var`.type else types[types.size - 1]

    constructor(`var`: Variable) {
        this.`var` = `var`
        methods = ArrayList()
        types = ArrayList()
    }

    constructor(chain: ExpressionChain) {
        `var` = chain.`var`
        methods = ArrayList(chain.methods)
        types = ArrayList(chain.types)
    }

    fun addMethod(m: Method) {
        types.add(currentType.getConcretization(m.genericReturnType))
        methods.add(m)
    }

    fun pop() {
        methods.removeAt(methods.size - 1)
        types.removeAt(types.size - 1)
    }

    fun structCost(): Int {
        val args = methods.stream().mapToInt { m -> m.parameterTypes.size }.sum()
        val chainLength = methods.size

        return chainLength + K * args // give some more weight to arguments because that involves further search
    }

    override fun equals(o: Any?): Boolean {
        return if (o == null || o !is ExpressionChain) false else methods == o.methods
    }

    override fun hashCode(): Int {
        return methods.hashCode()
    }

    companion object {
        val MAX_COMPOSE_LENGTH = 1 // a().b().c().d()...
        val MAX_ARGUMENT_DEPTH = 1 // a(b(c(d(...))))
        val K = 3 // number of arguments is given K times more weight than length of composition in cost
    }
}
